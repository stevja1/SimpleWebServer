package org.jaredstevens.interviews.aem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.file.AccessDeniedException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Spins up a small, multi-threaded HTTP web server.
 * This server was written for a programming test interview. I've tried to balance framework code
 * and home brew code in order to provide a good sample of my programming style.
 */
public class Server {
	private final static Logger LOGGER = LoggerFactory.getLogger(Server.class);

	public static void main(String[] args) {
		ServerConfig config = null;

		// Ensure there is a configuration file provided.
		if (args.length != 1) {
			System.out.println("Usage: java -jar WebServer.jar config.json");
			System.exit(0);
		}

		// Try to read in and parse the configuration
		try {
			final String configFilename = args[0];
			config = ServerConfig.parseConfiguration(configFilename);
		} catch(FileNotFoundException e) {
			System.err.println("Config file wasn't found: " + args[0]);
			System.exit(ServerErrorCodes.CONFIG_FILE_PARSE_ERROR.getCode());
		} catch(AccessDeniedException e) {
			System.err.println("Config file access denied: " + args[0]);
			System.exit(ServerErrorCodes.CONFIG_FILE_PARSE_ERROR.getCode());
		} catch(IOException e) {
			System.err.println("Couldn't read configuration file: " + args[0]);
			System.exit(ServerErrorCodes.CONFIG_FILE_PARSE_ERROR.getCode());
		}

		// Create our thread pool
		ExecutorService threadPool = Executors.newFixedThreadPool(config.getThreads());

		try {
			final InetAddress host = InetAddress.getByName(config.getHostname());
			Server.LOGGER.debug("Creating a server socket. Binding to {}:{}", config.getHostname(), config.getPort());
			Server.LOGGER.debug("{} resolves to IP {}", config.getHostname(), host.getHostAddress());
			final ServerSocket serverSocket = new ServerSocket(config.getPort(), config.getBacklog(), host);

			Server.LOGGER.info("Waiting for connections to {} on port {}", host.getHostName(), config.getPort());
			Socket clientSocket;
			while (true) {
				clientSocket = serverSocket.accept();
				clientSocket.setSoTimeout(config.getSocketTimeout());
				Server.LOGGER.debug("Received request. Assigning to thread.");
				threadPool.submit(new RequestHandler(clientSocket, config.getDocumentRoot()));
			}
		} catch (BindException e) {
			if(e.getMessage().equals("Permission denied (Bind failed)")) {
				System.err.println("It looks like you may not have permissions to bind to port "+config.getPort());
			} else if(e.getMessage().equals("Cannot assign requested address (Bind failed)")) {
				System.err.println("Can't resolve the hostname/IP for this host. Check your server configuration.");
			} else if(e.getMessage().equals("Address already in use (Bind failed)")) {
				System.err.println("Something else is already using port "+config.getPort());
			} else {
				System.err.println("There was a problem binding to port " + config.getPort());
			}
			System.err.println("Message: "+e.getMessage());
			e.printStackTrace();
			System.exit(ServerErrorCodes.PORT_IN_USE.getCode());
		} catch (UnknownHostException e) {
			System.err.println("The hostname provided looks invalid: "+config.getHostname());
			e.printStackTrace();
			System.exit(ServerErrorCodes.INVALID_HOST_NAME.getCode());
		} catch (IOException e) {
			System.err.println("There was a problem while initializing and listening for requests.");
			e.printStackTrace();
			System.exit(ServerErrorCodes.SERVER_SOCKET_ERROR.getCode());
		}
	}
}
