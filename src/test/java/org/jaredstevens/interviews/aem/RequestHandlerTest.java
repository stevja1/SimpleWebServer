package org.jaredstevens.interviews.aem;

import org.jaredstevens.interviews.aem.httppojos.*;
import org.jaredstevens.interviews.aem.httppojos.HttpResponseHeader;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Copyright Jared Stevens 2017 All Rights Reserved
 */
public class RequestHandlerTest {
	private final static String documentRoot = "src/test/resources/test_files/";

	@Test
	public void processStringHeaderTest() throws IOException, BadRequestException {
		final StringBuilder testStream = new StringBuilder("GET /index.html HTTP/1.1\n")
						.append("Host: localhost:4444\n")
						.append("User-Agent: curl/7.51.0\n")
						.append("Accept: */*\n");

		final Socket inputSocket = null;
		final InputStream streamFromBrowser = new ByteArrayInputStream(testStream.toString().getBytes());
		final BufferedReader reader = new BufferedReader(new InputStreamReader(streamFromBrowser));
		final RequestHandler thread = new RequestHandler(inputSocket, RequestHandlerTest.documentRoot);
		final HttpRequestHeader header = thread.processHeader(reader);
		assertEquals("Request method doesn't match expected result.", HttpRequestMethod.GET, header.getMethod());
		assertEquals("Request protocol doesn't match expected result.", "HTTP/1.1", header.getProtocol());
		assertEquals("Request resource doesn't match expected result.", "/index.html", header.getResource());
		assertEquals("Unexpected number of request headers", 3, header.getHeaders().size());
	}

	@Test(expected = BadRequestException.class)
	public void processBadHeaderTest() throws IOException, BadRequestException {
		final StringBuilder testStream = new StringBuilder("GERT /index.html HTTP/1.1\n")
						.append("Host: localhost:4444\n")
						.append("User-Agent: curl/7.51.0\n")
						.append("Accept: */*\n");

		final Socket inputSocket = null;
		final InputStream streamFromBrowser = new ByteArrayInputStream(testStream.toString().getBytes());
		final BufferedReader reader = new BufferedReader(new InputStreamReader(streamFromBrowser));
		final RequestHandler thread = new RequestHandler(inputSocket, RequestHandlerTest.documentRoot);
		thread.processHeader(reader);
	}

	@Test
	public void processMultipleHeaderTest() throws IOException, BadRequestException {
		final StringBuilder testStream = new StringBuilder("GET /index.html HTTP/1.1\n")
						.append("Host: localhost:4444\n")
						.append("User-Agent: curl/7.51.0\n")
						.append("Accept: */*\n\n");

		testStream.append("GET /test_image.jpg HTTP/1.1\n")
						.append("Host: localhost:4444\n")
						.append("User-Agent: curl/7.51.0\n")
						.append("Accept: */*\n\n");

		final Socket inputSocket = null;
		final InputStream streamFromBrowser = new ByteArrayInputStream(testStream.toString().getBytes());
		final BufferedReader reader = new BufferedReader(new InputStreamReader(streamFromBrowser));
		final RequestHandler thread = new RequestHandler(inputSocket, RequestHandlerTest.documentRoot);

		HttpRequestHeader header;
		header = thread.processHeader(reader);
		assertEquals("Request method doesn't match expected result.", HttpRequestMethod.GET, header.getMethod());
		assertEquals("Request protocol doesn't match expected result.", "HTTP/1.1", header.getProtocol());
		assertEquals("Request resource doesn't match expected result.", "/index.html", header.getResource());
		assertEquals("Unexpected number of request headers", 3, header.getHeaders().size());

		header = thread.processHeader(reader);
		assertEquals("Request method doesn't match expected result.", HttpRequestMethod.GET, header.getMethod());
		assertEquals("Request protocol doesn't match expected result.", "HTTP/1.1", header.getProtocol());
		assertEquals("Request resource doesn't match expected result.", "/test_image.jpg", header.getResource());
		assertEquals("Unexpected number of request headers", 3, header.getHeaders().size());
	}

	@Test
	public void processRequestTest() throws BadRequestException {
		final Socket inputSocket = null;
		final RequestHandler thread = new RequestHandler(inputSocket, RequestHandlerTest.documentRoot);
		final StringBuilder rawHeader = new StringBuilder("GET /index.html HTTP/1.1\n")
						.append("Host: localhost:4444\n")
						.append("User-Agent: curl/7.51.0\n")
						.append("Accept: */*\n\n");
		final HttpRequestHeader requestHeader = HttpRequestHeader.fromString(rawHeader.toString());
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		thread.processRequest(requestHeader, outputStream);
		assertEquals("Output didn't match expected value.", "HTTP/1.1 200 OK\n" +
						"Content-Length: 51\n" +
						"Content-Type: text/html\n\n" +
						"<!DOCTYPE html>\n" +
						"<html><body>hi there.</body></html>", outputStream.toString());
	}

	@Test
	public void processInternalRequestTest() throws BadRequestException {
		final Socket inputSocket = null;
		final RequestHandler thread = new RequestHandler(inputSocket, RequestHandlerTest.documentRoot);
		final StringBuilder rawHeader = new StringBuilder("GET /404.html HTTP/1.1\n")
						.append("Host: localhost:4444\n")
						.append("User-Agent: curl/7.51.0\n")
						.append("Accept: */*\n\n");
		final HttpRequestHeader requestHeader = HttpRequestHeader.fromString(rawHeader.toString());
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		thread.processRequest(requestHeader, outputStream);
		assertEquals("Output didn't match expected value.", "HTTP/1.1 404 Not Found\n" +
						"Content-Length: 50\n" +
						"Content-Type: text/html\n\n" +
						"<!DOCTYPE html><html><body>404 Error</body></html>", outputStream.toString());
	}

	@Test
	public void processRequestUnsupportedMethodTest() throws BadRequestException {
		final Socket inputSocket = null;
		final RequestHandler thread = new RequestHandler(inputSocket, RequestHandlerTest.documentRoot);
		final StringBuilder rawHeader = new StringBuilder("POST /index.html HTTP/1.1\n")
						.append("Host: localhost:4444\n")
						.append("User-Agent: curl/7.51.0\n")
						.append("Accept: */*\n\n");
		final HttpRequestHeader requestHeader = HttpRequestHeader.fromString(rawHeader.toString());
		requestHeader.setBody("{\"test\":\"value\"}");
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		thread.processRequest(requestHeader, outputStream);
		assertEquals("Output didn't match expected value.", "HTTP/1.1 400 Bad Request\n" +
						"Content-Length: 50\n" +
						"Content-Type: text/html\n\n" +
						"<!DOCTYPE html><html><body>400 Error</body></html>", outputStream.toString());
	}

	@Test
	public void serveInternalResourceTest() {
		final Socket inputSocket = null;
		final RequestHandler thread = new RequestHandler(inputSocket, RequestHandlerTest.documentRoot);
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		assertTrue("Couldn't find expected internal resource", thread.serveInternalResource("/404-background.jpg", outputStream));
		assertFalse("An internal resource that shouldn't exist was apparently served.", thread.serveInternalResource("/900-background.jpg", outputStream));
		// Verify that the headers are correct -- not sure if it makes sense to compare binary data here...
		assertEquals("HTTP/1.1 200 OK\n" +
						"Content-Length: 1657\n" +
						"Content-Type: image/jpeg\n\n", outputStream.toString().substring(0, 63));
	}

	@Test
	public void sendResponseTest() throws IOException {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "text/html");
		headers.put("Content-Length", "12");
		final HttpResponseHeader responseHeader = new HttpResponseHeader("HTTP/1.1", 200, "OK", headers);
		FileInputStream fileStream = Mockito.mock(FileInputStream.class);
		Mockito.when(fileStream.read(Mockito.any())).thenAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
				byte[] inputData = invocationOnMock.getArgumentAt(0, byte[].class);
				if(inputData[0] == 'H') {
					return -1;
				} else {
					inputData[0] = 'H';
					inputData[1] = 'e';
					inputData[2] = 'l';
					inputData[3] = 'l';
					inputData[4] = 'o';
					inputData[5] = ' ';
					inputData[6] = 't';
					inputData[7] = 'h';
					inputData[8] = 'e';
					inputData[9] = 'r';
					inputData[10] = 'e';
					inputData[11] = '.';
					return 12;
				}
			}
		});
		OutputStream browserStream = new ByteArrayOutputStream();
		RequestHandler.sendResponse(responseHeader, fileStream, browserStream);
		assertEquals("Output doesn't match expected value", "HTTP/1.1 200 OK\n" +
						"Content-Length: 12\n" +
						"Content-Type: text/html\n\n" +
						"Hello there.", browserStream.toString());
	}

	@Test
	public void sendInternalResponseTest() {
		final Socket inputSocket = null;
		final RequestHandler thread = new RequestHandler(inputSocket, RequestHandlerTest.documentRoot);
		final HttpResponseHeader responseHeader = new HttpResponseHeader("HTTP/1.1", 404, "Not Found", new HashMap<>());
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		thread.sendInternalResponse(responseHeader, "/error_pages/404.html", outputStream);
		assertEquals("Output doesn't match expected value", "HTTP/1.1 404 Not Found\n" +
						"Content-Length: 50\n" +
						"Content-Type: text/html\n\n" +
						"<!DOCTYPE html><html><body>404 Error</body></html>", outputStream.toString());
	}

	@Test
	public void sendInternalResponseMissingResourceTest() {
		final Socket inputSocket = null;
		final RequestHandler thread = new RequestHandler(inputSocket, RequestHandlerTest.documentRoot);
		final HttpResponseHeader responseHeader = new HttpResponseHeader("HTTP/1.1", 404, "Not Found", new HashMap<>());
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		thread.sendInternalResponse(responseHeader, "/error_pages/04.html", outputStream);
		assertEquals("Output doesn't match expected value", "HTTP/1.1 404 Not Found\n" +
						"Content-Length: 59\n" +
						"Content-Type: text/plain\n\n" +
						"Internal Server Error -- error loading internal error page.", outputStream.toString());
	}

	@Test
	public void readInternalResourceTest() throws InternalResourceTooLargeException, IOException {
		final Socket inputSocket = null;
		final RequestHandler thread = new RequestHandler(inputSocket, RequestHandlerTest.documentRoot);
		final byte[] data = thread.readInternalResource("/error_pages/404.html");
		assertEquals("Output doesn't match expected value", "<!DOCTYPE html><html><body>404 Error</body></html>", new String(data));
	}

	@Test(expected = IOException.class)
	public void readInternalResourceMissingTest() throws InternalResourceTooLargeException, IOException {
		final Socket inputSocket = null;
		final RequestHandler thread = new RequestHandler(inputSocket, RequestHandlerTest.documentRoot);
		thread.readInternalResource("/error_pages/44.html");
	}

	@Test(expected = InternalResourceTooLargeException.class)
	public void readInternalResourceLargeTest() throws InternalResourceTooLargeException, IOException {
		final Socket inputSocket = null;
		final RequestHandler thread = new RequestHandler(inputSocket, RequestHandlerTest.documentRoot);
		thread.readInternalResource("/error_pages/large_file.bin");
	}

	@Test
	public void getFileObjectTest() {
		final Socket inputSocket = null;
		final RequestHandler thread = new RequestHandler(inputSocket, RequestHandlerTest.documentRoot);

		// Normal Test
		String resource = "test_file.txt";
		File file = thread.getFileObject(resource);
		assertEquals("Didn't get the expected File object", documentRoot+"test_file.txt", file.getPath());

		// Test with a '..' in the path to make sure its removed
		resource = "../test_file.txt";
		file = thread.getFileObject(resource);
		assertEquals("Method failed condition that sanitizes '..' patterns.", documentRoot+"test_file.txt", file.getPath());

		// Test by appending something with a / at the beginning to make sure its handled properly
		resource = "//test_file.txt";
		file = thread.getFileObject(resource);
		assertEquals("Method failed condition that handles extra '/' characters.", documentRoot+"test_file.txt", file.getPath());
	}

	@Test
	public void getInputStreamTest() throws IOException {
		File file = new File(RequestHandlerTest.documentRoot+"index.html");
		try(final InputStream stream = RequestHandler.getInputStream(file)) {
			// We don't really need to do anything here. Just making sure we get a stream for the file we're testing.
		} catch(IOException e) {
			throw e;
		}
	}

	@Test(expected = FileNotFoundException.class)
	public void getInputStreamNotFoundTest() throws IOException {
		File file = new File(RequestHandlerTest.documentRoot+"index.php");
		try(final InputStream stream = RequestHandler.getInputStream(file)) {
			// We don't really need to do anything here. Just testing to see if the FileNotFound functionality works.
		} catch(IOException e) {
			throw e;
		}
	}
}
