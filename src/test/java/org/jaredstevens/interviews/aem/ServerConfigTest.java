package org.jaredstevens.interviews.aem;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;

/**
 * Copyright Jared Stevens 2017 All Rights Reserved
 */
public class ServerConfigTest {
	private final static String testConfigFilename = "src/test/resources/test_files/server_config.json";
	@Test
	public void parseConfigurationTest() throws IOException {
		ServerConfig config = ServerConfig.parseConfiguration(ServerConfigTest.testConfigFilename);
		assertEquals(config.getHostname(), "localhost");
		assertEquals(config.getBacklog(), 5);
		assertEquals(config.getPort(), 4444);
		assertEquals(config.getSocketTimeout(), 500);
		assertEquals(config.getDocumentRoot(), "./test_docroot/");
		assertEquals(config.getThreads(), 2);
	}
}
