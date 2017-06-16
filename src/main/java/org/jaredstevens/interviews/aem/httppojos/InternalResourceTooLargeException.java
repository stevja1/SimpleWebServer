package org.jaredstevens.interviews.aem.httppojos;

/**
 * Copyright Jared Stevens 2017 All Rights Reserved
 */
public class InternalResourceTooLargeException extends Exception {
	public InternalResourceTooLargeException() {
		super();
	}

	public InternalResourceTooLargeException(String message) {
		super(message);
	}

	public InternalResourceTooLargeException(String message, Throwable cause) {
		super(message, cause);
	}

	public InternalResourceTooLargeException(Throwable cause) {
		super(cause);
	}

	public InternalResourceTooLargeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
