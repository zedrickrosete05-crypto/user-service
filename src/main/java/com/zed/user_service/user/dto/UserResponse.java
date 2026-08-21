package com.zed.user_service.user.dto;

import java.time.Instant;

public record UserResponse(
		Long id,
		String email,
		String name,
		Instant createdAt,
		Instant updatedAt) {
}
