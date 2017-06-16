package org.jaredstevens.interviews.aem.httppojos;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright Jared Stevens 2017 All Rights Reserved
 */
public class HttpResponseHeader {
	private String protocol;
	private int statusCode;
	private String status;
	private Map<String, String> headers;

	public HttpResponseHeader() {
		this.setProtocol("HTTP/1.1");
		this.setStatusCode(200);
		this.setStatus("OK");
		this.setHeaders(new HashMap<>());
	}

	public HttpResponseHeader(final String protocol, final int statusCode, final String status, final Map<String, String> headers) {
		this.setProtocol(protocol);
		this.setStatusCode(statusCode);
		this.setStatus(status);
		this.setHeaders(headers);
	}

	/**
	 * Returns the fields of this object in the form of an HTTP response header.
	 * @return A String containing an HTTP response header represented by this object.
	 */
	public String getRawResponse() {
		StringBuilder response = new StringBuilder();
		// Add the status
		response
						.append(this.getProtocol())
						.append(" ")
						.append(this.getStatusCode())
						.append(" ")
						.append(this.getStatus()).append("\n");

		if(this.getHeaders() != null) {
			// Add the headers
			for (Map.Entry<String, String> entry : this.getHeaders().entrySet()) {
				response.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
			}
		}
		// Append the newline to end the header block
		response.append("\n");
		return response.toString();
	}

	public String add(final String key, final String value) {
		return this.getHeaders().put(key, value);
	}

	public String get(final String key) {
		return this.getHeaders().get(key);
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
}
