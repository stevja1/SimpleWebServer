package org.jaredstevens.interviews.aem.httppojos;

/**
 * Copyright Jared Stevens 2017 All Rights Reserved
 */
public class BadRequestException extends Exception {
	public BadRequestException(String message) {
		super(message);
	}

	public BadRequestException(String message, Throwable cause) {
		super(message, cause);
	}

	public BadRequestException(Throwable cause) {
		super(cause);
	}

	public BadRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {

		super(message, cause, enableSuppression, writableStackTrace);
	}
}
