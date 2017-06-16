package org.jaredstevens.interviews.aem;

import org.jaredstevens.interviews.aem.httppojos.HttpResponseHeader;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Copyright Jared Stevens 2017 All Rights Reserved
 */
public class HttpResponseHeaderTest {
	@Test
	public void testGetRawResponse() {
		final HttpResponseHeader header = new HttpResponseHeader("HTTP/1.1", 200, "OK", new HashMap<>());
		final String rawResponse = header.getRawResponse();
		assertEquals("HTTP/1.1 200 OK\n\n", rawResponse);
	}

	@Test
	public void testConstructor() {
		HttpResponseHeader header;
		header = new HttpResponseHeader("HTTP/1.1", 404, "Not Found", new HashMap<>());
		assertEquals("HTTP/1.1 404 Not Found\n\n", header.getRawResponse());

		header = new HttpResponseHeader();
		assertEquals("HTTP/1.1 200 OK\n\n", header.getRawResponse());
	}
}
