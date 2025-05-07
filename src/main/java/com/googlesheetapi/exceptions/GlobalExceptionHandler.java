package com.googlesheetapi.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.googlesheetapi.common.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessLogicException.class)
	public ResponseEntity<ErrorResponse> handleBusinessLogicException(BusinessLogicException ex) {
		int statusCode = ex.getStatusCode() > 0 ? ex.getStatusCode() : HttpStatus.BAD_REQUEST.value();

		ErrorResponse error = new ErrorResponse(statusCode, ex.getMessage());
		return new ResponseEntity<>(error, HttpStatus.valueOf(statusCode));
	}
}