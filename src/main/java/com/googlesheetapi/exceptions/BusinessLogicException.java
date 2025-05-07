package com.googlesheetapi.exceptions;

import org.springframework.http.HttpStatus;

public class BusinessLogicException extends RuntimeException {
	private final int statusCode;

	public BusinessLogicException(String message, int statusCode) {
		super(message);
		this.statusCode = statusCode;
	}

	public BusinessLogicException(String message) {
		super(message);
		this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value(); // Default for unknown error
	}

	public BusinessLogicException(String message, Throwable cause, int statusCode) {
		super(message, cause);
		this.statusCode = statusCode;
	}

	public BusinessLogicException(String message, Throwable cause) {
		super(message, cause);
		this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value(); // Default for unknown error
	}

	public int getStatusCode() {
		return statusCode;
	}
}
