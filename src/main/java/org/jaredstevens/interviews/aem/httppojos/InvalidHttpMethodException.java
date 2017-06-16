package org.jaredstevens.interviews.aem.httppojos;

/**
 * Copyright Jared Stevens 2017 All Rights Reserved
 */
public class InvalidHttpMethodException extends Exception {
	public InvalidHttpMethodException(String message) {
		super(message);
	}

	public InvalidHttpMethodException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidHttpMethodException(Throwable cause) {
		super(cause);
	}

	public InvalidHttpMethodException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
