package org.jaredstevens.interviews.aem.httppojos;

/**
 * Copyright Jared Stevens 2017 All Rights Reserved
 */
public enum HttpRequestMethod {
	GET("GET"), POST("POST"), PUT("PUT"), HEAD("HEAD"), DELETE("DELETE"), PATCH("PATCH");
	private String method;
	HttpRequestMethod(final String method) {
		this.method = method;
	}

	@Override
	public String toString() {
		return this.method;
	}

	public static HttpRequestMethod getMethod(String rawMethod) throws InvalidHttpMethodException {
		for(HttpRequestMethod method : HttpRequestMethod.values()) {
			if(rawMethod.equals(method.toString())) {
				return method;
			}
		}
		throw new InvalidHttpMethodException(rawMethod);
	}
}
