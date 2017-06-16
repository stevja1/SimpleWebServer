package org.jaredstevens.interviews.aem;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Copyright Jared Stevens 2017 All Rights Reserved
 */
public class ServerConfig {
	private final static Logger LOGGER = LoggerFactory.getLogger(ServerConfig.class);
	private String hostname;
	private String documentRoot;
	private int port;
	private int socketTimeout;
	private int backlog;
	private int threads;

	/**
	 * Parses a JSON configuration file that defines the parameters for this web server
	 *
	 * @param filename The filename containing the configuration
	 * @return A ServerConfig object containing the configuration information
	 * @throws IOException         Thrown if the file can't be found or read.
	 * @throws JsonSyntaxException Thrown if the file can't be parsed.
	 */
	public static ServerConfig parseConfiguration(final String filename) throws IOException, JsonSyntaxException {
		ServerConfig retVal;
		final File file = new File(filename);
		ServerConfig.LOGGER.info("Loading configuration {}", file.getAbsoluteFile());
		String line;
		final StringBuilder rawConfig = new StringBuilder();
		final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

		while ((line = br.readLine()) != null) {
			rawConfig.append(line);
		}
		ServerConfig.LOGGER.debug("Read {} bytes from config file.", rawConfig.length());
		Gson parser = new GsonBuilder()
						.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
						.create();
		retVal = parser.fromJson(rawConfig.toString(), ServerConfig.class);

		return retVal;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getDocumentRoot() {
		return documentRoot;
	}

	public void setDocumentRoot(String documentRoot) {
		this.documentRoot = documentRoot;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public int getBacklog() {
		return backlog;
	}

	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}

	public int getThreads() {
		return threads;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}
}
