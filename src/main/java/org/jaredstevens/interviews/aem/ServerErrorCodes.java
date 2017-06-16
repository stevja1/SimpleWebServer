package org.jaredstevens.interviews.aem;

/**
 * Copyright Jared Stevens 2017 All Rights Reserved
 */
public enum ServerErrorCodes {
	CONFIG_FILE_PARSE_ERROR(1), PORT_IN_USE(2), INVALID_HOST_NAME(3), SERVER_SOCKET_ERROR(4);
	private int code;

	ServerErrorCodes(int code) {
		this.code = code;
	}

	public int getCode() {
		return this.code;
	}
}
