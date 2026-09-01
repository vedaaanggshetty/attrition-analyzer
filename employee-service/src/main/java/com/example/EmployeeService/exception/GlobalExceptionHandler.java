package com.example.EmployeeService.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.EmployeeService.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(SurveyApiException.class)
	public ResponseEntity<ErrorResponse> handleSurveyApiException(SurveyApiException ex) {
		return ResponseEntity
				.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ErrorResponse(ex.getMessage()));
	}
}
