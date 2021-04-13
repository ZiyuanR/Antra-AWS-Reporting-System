package com.antra.report.client.exception;

public class RequestNotFoundException extends RuntimeException{
    private String errorMessage;

	public String getErrorMessage() {
		return errorMessage;
	}

	public RequestNotFoundException(String errorMessage) {
		super(errorMessage);
		this.errorMessage = errorMessage;
	}

	public RequestNotFoundException() {
		super();
	}
}