package com.example.EmployeeService.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

	@ExceptionHandler(EventPublicationException.class)
	public ResponseEntity<ErrorResponse> handleEventPublicationException(EventPublicationException ex) {
		return ResponseEntity
				.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(UnauthenticatedException.class)
	public ResponseEntity<ErrorResponse> handleUnauthenticatedException(UnauthenticatedException ex) {
		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(new ErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getDefaultMessage())
				.orElse("Validation failed");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
	}
}
