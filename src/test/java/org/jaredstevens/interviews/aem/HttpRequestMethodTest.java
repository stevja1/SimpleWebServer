package org.jaredstevens.interviews.aem;

import org.jaredstevens.interviews.aem.httppojos.HttpRequestMethod;
import org.jaredstevens.interviews.aem.httppojos.InvalidHttpMethodException;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 * Copyright Jared Stevens 2017 All Rights Reserved
 */
public class HttpRequestMethodTest {
	@Test
	public void getMethodTest() throws InvalidHttpMethodException {
		String method;
		method = "GET";
		assertEquals(HttpRequestMethod.GET, HttpRequestMethod.getMethod(method));
		method = "POST";
		assertEquals(HttpRequestMethod.POST, HttpRequestMethod.getMethod(method));
		method = "PUT";
		assertEquals(HttpRequestMethod.PUT, HttpRequestMethod.getMethod(method));
		method = "HEAD";
		assertEquals(HttpRequestMethod.HEAD, HttpRequestMethod.getMethod(method));
		method = "DELETE";
		assertEquals(HttpRequestMethod.DELETE, HttpRequestMethod.getMethod(method));
		method = "PATCH";
		assertEquals(HttpRequestMethod.PATCH, HttpRequestMethod.getMethod(method));
	}

	@Test(expected = InvalidHttpMethodException.class)
	public void getMethodInvalidTest() throws InvalidHttpMethodException {
		String method;
		method = "GERT";
		HttpRequestMethod.getMethod(method);
	}
}
