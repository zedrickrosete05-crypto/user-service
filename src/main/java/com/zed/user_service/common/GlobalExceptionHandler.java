package com.zed.user_service.common;

import com.zed.user_service.user.DuplicateUserEmailException;
import com.zed.user_service.user.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import com.zed.user_service.auth.InvalidRefreshTokenException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ApiError> handleUserNotFound(
			UserNotFoundException exception,
			HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
	}

	@ExceptionHandler(DuplicateUserEmailException.class)
	public ResponseEntity<ApiError> handleDuplicateEmail(
			DuplicateUserEmailException exception,
			HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, exception.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(
			MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return error(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleMalformedRequest(
			HttpMessageNotReadableException exception,
			HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, "Request body must contain valid JSON", request);
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiError> handleBadCredentials(
			BadCredentialsException exception,
			HttpServletRequest request) {
		return error(HttpStatus.UNAUTHORIZED, "Invalid email or password", request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(
			AccessDeniedException exception,
			HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN, exception.getMessage(), request);
	}

	@ExceptionHandler(InvalidRefreshTokenException.class)
	public ResponseEntity<ApiError> handleInvalidRefreshToken(
			InvalidRefreshTokenException exception,
			HttpServletRequest request) {
		return error(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
	}

	private ResponseEntity<ApiError> error(
			HttpStatus status,
			String message,
			HttpServletRequest request) {
		ApiError body = new ApiError(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getRequestURI());
		return ResponseEntity.status(status).body(body);
	}
}
