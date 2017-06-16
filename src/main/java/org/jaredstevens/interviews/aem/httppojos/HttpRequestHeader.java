package org.jaredstevens.interviews.aem.httppojos;

import java.util.*;

/**
 * Copyright Jared Stevens 2017 All Rights Reserved
 */
public class HttpRequestHeader {
	private HttpRequestMethod method;
	private String resource;
	private String protocol;
	private String requestString;
	private Map<String, String> headers;
	private String body;

	/**
	 * Parses a list of strings containing header info into an HttpRequestHeader object
	 * @param requestLines A List<String> object containing request header information
	 * @return An HttpRequestHeader that reflects the header info passed in the parameter
	 * @throws BadRequestException Thrown if this method is unable to parse the header.
	 */
	public static HttpRequestHeader fromList(List<String> requestLines) throws BadRequestException {
		HttpRequestHeader response = new HttpRequestHeader();
		response.setHeaders(new HashMap<>());

		// Parse out the request method
		String method = requestLines.remove(0);
		String methodParts[] = method.split(" ");
		if(methodParts == null || methodParts.length != 3) {
			throw new BadRequestException("Expecting Method, Resource and Protocol. Found: "+method);
		}
		response.setRequestString(method);
		try {
			response.method = HttpRequestMethod.getMethod(methodParts[0]);
		} catch(InvalidHttpMethodException e) {
			throw new BadRequestException("Unknown request method: "+method, e);
		}

		// Parse out the requested resource
		response.setResource(methodParts[1]);

		// Parse out the protocol
		response.setProtocol(methodParts[2]);

		// Parse out the headers
		String parts[];
		for(String header : requestLines) {
			parts = header.split(":");
			response.getHeaders().put(parts[0].trim(), parts[1].trim());
		}

		return response;
	}

	/**
	 * Parses string containing header info into an HttpRequestHeader object
	 * @param rawRequest The raw request data that was received from the client.
	 * @return An HttpRequestHeader that reflects the header info passed in the parameter
	 * @throws BadRequestException Thrown if this method is unable to parse the header.
	 */
	public static HttpRequestHeader fromString(String rawRequest) throws BadRequestException {
		String[] lines = rawRequest.split("\n");
		List<String> requestLines = new ArrayList<>(Arrays.asList(lines));
		return HttpRequestHeader.fromList(requestLines);
	}

	public HttpRequestMethod getMethod() {
		return method;
	}

	public void setMethod(HttpRequestMethod method) {
		this.method = method;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getRequestString() {
		return requestString;
	}

	public void setRequestString(String requestString) {
		this.requestString = requestString;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
}
