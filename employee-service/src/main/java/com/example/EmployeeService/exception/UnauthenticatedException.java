package com.example.EmployeeService.exception;

public class UnauthenticatedException extends RuntimeException {

	public UnauthenticatedException() {
		super("A valid Authorization bearer token is required");
	}
}
