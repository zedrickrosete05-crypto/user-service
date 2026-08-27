package com.zed.user_service.auth.dto;

public record LoginResponse(
		String accessToken,
		String tokenType,
		long expiresIn,
		String refreshToken,
		long refreshExpiresIn) {
}
