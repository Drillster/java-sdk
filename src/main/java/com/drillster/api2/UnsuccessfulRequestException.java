package com.drillster.api2;

import com.drillster.api2.general.ErrorResponse;

@SuppressWarnings("serial")
public class UnsuccessfulRequestException extends ApiException {

	private ErrorResponse error;
	
	public UnsuccessfulRequestException(ErrorResponse error) {
		super(error.getDescription());
		this.error = error;
	}
	
	public ErrorResponse getError() {
		return this.error;
	}
}
