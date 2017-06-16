package org.jaredstevens.interviews.aem;

import org.apache.tika.Tika;
import org.jaredstevens.interviews.aem.httppojos.BadRequestException;
import org.jaredstevens.interviews.aem.httppojos.HttpRequestHeader;
import org.jaredstevens.interviews.aem.httppojos.HttpResponseHeader;
import org.jaredstevens.interviews.aem.httppojos.InternalResourceTooLargeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Copyright Jared Stevens 2017 All Rights Reserved
 *
 */
public class RequestHandler implements Runnable {
	private static final String PROTOCOL = "HTTP/1.1";
	// Used for identifying file mime-types
	private static final Tika tika = new Tika();
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);
	// This limits the size of internal resources. That way, the server doesn't try to serve some 3 GB file.
	private static final int INTERNAL_RESOURCE_MAX_SIZE = 1048576;

	private Socket clientSocket;
	private String documentRoot;

	public RequestHandler(final Socket inSocket, final String documentRoot) {
		RequestHandler.LOGGER.debug("Initializing the thread...");
		this.clientSocket = inSocket;
		this.documentRoot = documentRoot;
	}

	/**
	 * Kicks of the server's thread. This does a few things:
	 * 1. Connects to the browser's output stream (for sending data to the browser)
	 * 2. Connects to the browser's input stream (for receiving data from the browser)
	 * 3. Reads the request from the browser and parses it into an HttpRequestHeader
	 * 4. Processes the request using the processRequest method
	 * This method reads headers from the same connection until there the socket disconnects.
	 * This behavior provides keep-alive functionality.
	 */
	public void run() {
		RequestHandler.LOGGER.debug("Thread started. Streaming input data from socket.");
		OutputStream outputStream = null;
		try {
			outputStream = this.clientSocket.getOutputStream();
		} catch(IOException e) {
			RequestHandler.LOGGER.warn("There was a problem opening the output stream to the client.", e);
		}

		HttpRequestHeader header;
		try(BufferedReader inputFromClient = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()))) {
			// Loop over and process requests until the client stops sending them. Provides keep-alive functionality.
			while ((header = this.processHeader(inputFromClient)) != null) {
				this.processRequest(header, outputStream);
			}
		} catch(IOException e) {
			if(e.getMessage().equals("Connection reset")) {
				RequestHandler.LOGGER.debug("Its likely the client closed the connection.");
			} else {
				RequestHandler.LOGGER.warn("There was a problem getting an input stream from the client.", e);
			}
		} catch(BadRequestException e) {
			// If we can't parse the request, send a 400 error back.
			RequestHandler.LOGGER.warn("Request was malformed: ", e);
			final HttpResponseHeader responseHeader = new HttpResponseHeader(RequestHandler.PROTOCOL, 400, "Bad Request", new HashMap<>());
			this.sendInternalResponse(responseHeader, "/error_pages/400.html", outputStream);
		}
		RequestHandler.LOGGER.debug("Processed all requests... freeing up the thread.");
	}

	/**
	 * Reads the next request header
	 * @param inputFromClient The input stream from the browser
	 * @return An HttpRequestHeader containing the request information. If there are no headers
	 * 	left to read, null is returned.
	 * @throws BadRequestException Thrown if the method is bad, or if the header is malformed.
	 */
	HttpRequestHeader processHeader(final BufferedReader inputFromClient) throws BadRequestException {
		HttpRequestHeader requestHeader = null;
		List<String> requestLines = new ArrayList<>();
		try {
			RequestHandler.LOGGER.debug("Reading data from the client's header...");
			String line;
			while((line = inputFromClient.readLine()) != null) {
				RequestHandler.LOGGER.debug(line);
				if(line.length() <= 0) {
					break;
				} else {
					requestLines.add(line);
				}
			}
			requestHeader = HttpRequestHeader.fromList(requestLines);
		} catch(SocketTimeoutException e) {
			RequestHandler.LOGGER.debug("Got tired of waiting for data. Returning what I've got.");
		} catch (IOException e) {
			if(e.getMessage().equals("Connection reset")) {
				RequestHandler.LOGGER.debug("Its likely the client closed the connection.");
			} else {
				RequestHandler.LOGGER.debug("There was a problem reading data from the client.", e);
			}
		}
		return requestHeader;
	}

	/**
	 * Processes a request from a browser. This writes data directly to the browsers output stream.
	 * If a resource isn't found (typically a 404), the server checks to see if the requested resource
	 * is an "internal" resource. If so, it serves it from data stored in the JAR. This makes it possible
	 * to ship pretty looking 400/401/404/500/etc error pages with the server.
	 * NOTE: Currently, this only supports GET requests.
	 * @param requestHeader The request header from the browser
	 * @param outputStream The browser's output stream (used to send data to the browser)
	 */
	 void processRequest(HttpRequestHeader requestHeader, OutputStream outputStream) {
		switch(requestHeader.getMethod()) {
			// We only support GET for now
			case GET:
				// Let's get a file object. This can be used in reading the file and determining file size.
				final File file = this.getFileObject(requestHeader.getResource());
				try(final FileInputStream inputStream = RequestHandler.getInputStream(file)) {
					HashMap<String,String> headers = new HashMap<>();
					headers.put("Content-Length", String.valueOf(file.length()));
					headers.put("Content-Type", RequestHandler.tika.detect(file));
					final HttpResponseHeader header = new HttpResponseHeader(RequestHandler.PROTOCOL, 200, "OK", headers);
					RequestHandler.sendResponse(header, inputStream, outputStream);
				} catch(AccessDeniedException e) {
					// Looks like the client is requesting a resource that is read only or that the server doesn't have access to serve.
					final HttpResponseHeader header = new HttpResponseHeader(RequestHandler.PROTOCOL, 401, "Unauthorized", new HashMap<>());
					this.sendInternalResponse(header, "/error_pages/401.html", outputStream);
					RequestHandler.LOGGER.warn("Access denied: {}", file.getAbsoluteFile());
				} catch(FileNotFoundException e) {
					HttpResponseHeader header;
					// Check to see if this request is for internal resources (i.e., background images for a 404 error page)
					if(!this.serveInternalResource(requestHeader.getResource(), outputStream)) {
						// Looks like its a legit 404. Send the 404 error page.
						header = new HttpResponseHeader(RequestHandler.PROTOCOL, 404, "Not Found", new HashMap<>());
						RequestHandler.LOGGER.warn("File not found: {}", file.getAbsoluteFile());
						this.sendInternalResponse(header, "/error_pages/404.html", outputStream);
					}
				} catch(IOException e) {
					// There was some kind of problem reading/sending data to the client
					final HttpResponseHeader header = new HttpResponseHeader(RequestHandler.PROTOCOL, 500, "Internal Server Error", new HashMap<>());
					RequestHandler.LOGGER.warn("Unexpected error when reading file: {}", file.getAbsoluteFile());
					this.sendInternalResponse(header, "/error_pages/500.html", outputStream);
				}
				break;
			default:
				// The request used an unsupported request method (only GET is supported currently)
				final HttpResponseHeader header = new HttpResponseHeader(RequestHandler.PROTOCOL, 400, "Bad Request", new HashMap<>());
				this.sendInternalResponse(header, "/error_pages/400.html", outputStream);
				break;
		}
		RequestHandler.LOGGER.debug("Served the request. I'm done.");
	}

	/**
	 * Sometimes, a request will be for a file that doesn't appear in the document root. I wanted
	 * to provide some built in images for use in error pages.
	 * This method handles requests for these types of internal resources and serves the data directly.
	 * If the request doesn't match any internal resources, nothing is served and false is returned.
	 * @param resource The name of the resource being requested
	 * @param outputStream The output stream that data should be written to (this is usually the browser's
	 *                     output stream).
	 * @return True if data was sent to the output stream, false if nothing was sent.
	 */
	boolean serveInternalResource(String resource, OutputStream outputStream) {
		HttpResponseHeader header;
		boolean retVal = false;
		switch(resource) {
			case "/400-background.jpg":
				header = new HttpResponseHeader(RequestHandler.PROTOCOL, 200, "OK", new HashMap<>());
				this.sendInternalResponse(header, "/error_pages/400-background.jpg", outputStream);
				retVal = true;
				break;
			case "/401-background.jpg":
				header = new HttpResponseHeader(RequestHandler.PROTOCOL, 200, "OK", new HashMap<>());
				this.sendInternalResponse(header, "/error_pages/401-background.jpg", outputStream);
				retVal = true;
				break;
			case "/404-background.jpg":
				header = new HttpResponseHeader(RequestHandler.PROTOCOL, 200, "OK", new HashMap<>());
				this.sendInternalResponse(header, "/error_pages/404-background.jpg", outputStream);
				retVal = true;
				break;
			case "/500-background.jpg":
				header = new HttpResponseHeader(RequestHandler.PROTOCOL, 200, "OK", new HashMap<>());
				this.sendInternalResponse(header, "/error_pages/500-background.jpg", outputStream);
				retVal = true;
				break;
			default:
				break;
		}
		return retVal;
	}

	/**
	 * Streams data from a file to the browser using the relevant streams.
	 * @param header The header data to send to the browser
	 * @param inStream A FileInputStream that's connected to the file on disk that you want to send.
	 * @param outStream A stream that can be used to send data to the browser.
	 */
	static void sendResponse(HttpResponseHeader header, FileInputStream inStream, OutputStream outStream) {
		if(outStream == null) {
			RequestHandler.LOGGER.warn("There is an unexpected problem writing data to the client.");
			return;
		}

		if(inStream == null) {
			RequestHandler.LOGGER.warn("There is an unexpected error reading data from disk.");
			return;
		}

		// Write the header
		try {
			outStream.write(header.getRawResponse().getBytes());
		} catch(IOException e) {
			RequestHandler.LOGGER.warn("There was a problem streaming the header to the client.", e);
		}

		// Stream the file to the client.
		byte[] buffer = new byte[1024];
		int bytesRead;
		try {
			while((bytesRead = inStream.read(buffer)) != -1) {
				outStream.write(buffer, 0, bytesRead);
			}
			// Flush the buffer
			outStream.flush();
		} catch(IOException e) {
			RequestHandler.LOGGER.warn("There was a problem streaming the file to the client.", e);
		}
	}

	/**
	 * Locates an internal resource (text or image) and streams it to the browser
	 * @param header The response header to use
	 * @param internalFilename The name of the internal resource to use
	 * @param outStream The browsers stream
	 */
	 void sendInternalResponse(final HttpResponseHeader header, final String internalFilename, final OutputStream outStream) {
		if(outStream == null) {
			RequestHandler.LOGGER.warn("There is an unexpected problem writing data to the client.");
			return;
		}

		// Write the header
		byte[] resourceData;
		try {
			resourceData = this.readInternalResource(internalFilename);
		} catch(InternalResourceTooLargeException e) {
			RequestHandler.LOGGER.warn("Just tried to read a resource that was too big!", e);
			resourceData = "Internal Server Error -- error loading internal error page.".getBytes();
		} catch(IOException e) {
			RequestHandler.LOGGER.warn("Couldn't read an internal resource!", e);
			resourceData = "Internal Server Error -- error loading internal error page.".getBytes();
		}
		header.add("Content-Length", String.valueOf(resourceData.length));
		header.add("Content-Type", RequestHandler.tika.detect(resourceData));
		try {
			outStream.write(header.getRawResponse().getBytes());
			outStream.write(resourceData);
			outStream.flush();
		} catch(IOException e) {
			RequestHandler.LOGGER.warn("There was a problem streaming the header to the client.", e);
		}
	}

	/**
	 * Fetches packaged data for an internal resource. This is data for packaged error pages.
	 * @param name The name of the internal resource. For example:
	 *             /error_pages/404.html
	 * @return A byte array containing the resource's data
	 * @throws InternalResourceTooLargeException Thrown if the resources data exceeds the
	 * 	max defined in INTERNAL_RESOURCE_MAX_SIZE.
	 */
	 byte[] readInternalResource(String name) throws InternalResourceTooLargeException, IOException {
		RequestHandler.LOGGER.debug("Reading internal resource {}", name);
		try(InputStream inStream = this.getClass().getResourceAsStream(name)) {
			if(inStream == null) {
				throw new IOException("Unable to read resource " + name);
			}
			int availableBytes = inStream.available();
			RequestHandler.LOGGER.debug("Stream reporting {} bytes available.", availableBytes);
			if(availableBytes > INTERNAL_RESOURCE_MAX_SIZE) {
				throw new InternalResourceTooLargeException(String.format("Internal resource larger than limit: %d - %s", availableBytes, name));
			}
			// Let's create a buffer for reading and a block of memory for storing the entire resource
			byte[] buffer = new byte[32768];
			ByteBuffer output = ByteBuffer.allocate(INTERNAL_RESOURCE_MAX_SIZE);
			int bytesRead;
			int resourceSize = 0;
			// You're probably wondering why I'm doing this. It turns out that when reading data
			// from an internal JAR resource, it has to do a few reads, even though the buffer
			// is big enough for the data. For that reason, I have to read whatever its got,
			// append it to the 'output' buffer, then try and read the next chunk until I read everything.
			while((bytesRead = inStream.read(buffer)) > 0) {
				RequestHandler.LOGGER.debug("Read {} bytes from internal resource", bytesRead);
				resourceSize += bytesRead;
				output.put(Arrays.copyOfRange(buffer, 0, bytesRead));
				if(resourceSize > INTERNAL_RESOURCE_MAX_SIZE) {
					throw new InternalResourceTooLargeException(String.format("Internal resource larger than 1 MB: %d - %s", resourceSize, name));
				}
			}
			RequestHandler.LOGGER.debug("Read {} bytes.", resourceSize);
			return Arrays.copyOfRange(output.array(), 0, resourceSize);
		} catch(IOException e) {
			// Pass this up to be processed by the calling method
			throw e;
		}
	}

	/**
	 * Creates a File object for the specified resource.
	 * @param resource The resource name. Usually a filename.
	 * @return A File object that represents the resource being requested.
	 */
	 File getFileObject(String resource) {
		RequestHandler.LOGGER.debug("Processing resource {}", resource);
		File file;

		// Replace '..' strings so that we never leave the document root.
		resource = resource.replace("..", "");

		// Decode the URL for processing
		try {
			resource = URLDecoder.decode(resource, "UTF-8");
		} catch(UnsupportedEncodingException e) {
			RequestHandler.LOGGER.error("An unexpected decoding error has occurred. {}", e);
		}

		// Provide a default if no specific file is requested
		switch(resource) {
			// Create a default where if no file is requested, we serve index.html
			case "/":
				resource = "/index.html";
				file = new File(this.getDocumentRoot() + resource);
				break;
			default:
				file = new File(this.getDocumentRoot() + resource);
				break;
		}
		RequestHandler.LOGGER.debug("Resolved to {}", file.getPath());
		return file;
	}

	/**
	 * Creates a FileInputStream object for the provided File object.
	 * @param file The File object of the file you want to read from.
	 * @return A FileInputStream connected to the specified file resource.
	 * @throws FileNotFoundException Thrown if the file can't be found.
	 * @throws AccessDeniedException Thrown if the file can't be read.
	 */
	 static FileInputStream getInputStream(File file) throws FileNotFoundException, AccessDeniedException {
		RequestHandler.LOGGER.debug("Opening file: {}", file.getAbsoluteFile());
		// Get rid of anything that could pull us out of the document root

		if(!file.exists()) {
			throw new FileNotFoundException("Unable to locate file: "+file.getAbsoluteFile());
		}
		if(!file.canRead()) {
			throw new AccessDeniedException("Unable to read file: "+file.getAbsoluteFile());
		}
		return new FileInputStream(file);
	}

	public Socket getClientSocket() {
		return clientSocket;
	}

	public void setClientSocket(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	public String getDocumentRoot() {
		return documentRoot;
	}

	public void setDocumentRoot(String documentRoot) {
		this.documentRoot = documentRoot;
	}
}
