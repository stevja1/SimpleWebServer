package org.jaredstevens.interviews.aem;

import org.jaredstevens.interviews.aem.httppojos.BadRequestException;
import org.jaredstevens.interviews.aem.httppojos.HttpRequestHeader;
import org.jaredstevens.interviews.aem.httppojos.HttpRequestMethod;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
/**
 * Copyright Jared Stevens 2017 All Rights Reserved
 */
public class HttpRequestHeaderTest {
	@Test
	public void fromListTest() throws BadRequestException {
		List<String> rawHeaderInfo = new ArrayList<>();
		final String requestString = "GET /index.html HTTP/1.1";
		rawHeaderInfo.add(requestString);
		rawHeaderInfo.add("Cache-control: no-cache");
		rawHeaderInfo.add("Connection: keep-alive");
		rawHeaderInfo.add("Accept-Encoding: compress, gzip");
		rawHeaderInfo.add("User-Agent: The greatest and best browser in the world");
		HttpRequestHeader header = HttpRequestHeader.fromList(rawHeaderInfo);
		assertEquals("Request string wasn't stored properly.", requestString, header.getRequestString());
		assertEquals("Protocol wasn't parsed correctly.", "HTTP/1.1", header.getProtocol());
		assertEquals("Resource wasn't parsed correctly.", "/index.html", header.getResource());
		assertEquals("Method wasn't parsed correctly.", HttpRequestMethod.GET, header.getMethod());
		assertEquals("Cache-control header wasn't parsed correctly.", "no-cache", header.getHeaders().get("Cache-control"));
		assertEquals("Connection header wasn't parsed correctly.", "keep-alive", header.getHeaders().get("Connection"));
		assertEquals("Accept-Encoding header wasn't parsed correctly.", "compress, gzip", header.getHeaders().get("Accept-Encoding"));
		assertEquals("Connection header wasn't parsed correctly.", "The greatest and best browser in the world", header.getHeaders().get("User-Agent"));
		assertNull("Didn't expect a body.", header.getBody());
	}

	@Test
	public void fromStringTest() throws BadRequestException {
		final String requestString = "GET /index.html HTTP/1.1\nCache-control: no-cache\nConnection: keep-alive\nAccept-Encoding: compress, gzip\nUser-Agent: The greatest and best browser in the world\n\n";
		HttpRequestHeader header = HttpRequestHeader.fromString(requestString);
		assertEquals("Request string wasn't stored properly.", "GET /index.html HTTP/1.1", header.getRequestString());
		assertEquals("Protocol wasn't parsed correctly.", "HTTP/1.1", header.getProtocol());
		assertEquals("Resource wasn't parsed correctly.", "/index.html", header.getResource());
		assertEquals("Method wasn't parsed correctly.", HttpRequestMethod.GET, header.getMethod());
		assertEquals("Cache-control header wasn't parsed correctly.", "no-cache", header.getHeaders().get("Cache-control"));
		assertEquals("Connection header wasn't parsed correctly.", "keep-alive", header.getHeaders().get("Connection"));
		assertEquals("Accept-Encoding header wasn't parsed correctly.", "compress, gzip", header.getHeaders().get("Accept-Encoding"));
		assertEquals("Connection header wasn't parsed correctly.", "The greatest and best browser in the world", header.getHeaders().get("User-Agent"));
		assertNull("Didn't expect a body.", header.getBody());
	}
}
