package com.zed.user_service.auth;

public class InvalidRefreshTokenException extends RuntimeException {

	public InvalidRefreshTokenException() {
		super("Invalid or expired refresh token");
	}
}
