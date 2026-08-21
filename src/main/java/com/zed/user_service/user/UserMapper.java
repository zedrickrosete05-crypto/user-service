package com.zed.user_service.user;

import com.zed.user_service.user.dto.CreateUserRequest;
import com.zed.user_service.user.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

	public User toEntity(CreateUserRequest request, String passwordHash) {
		User user = new User();
		user.setEmail(request.email());
		user.setName(request.name());
		user.setPasswordHash(passwordHash);
		return user;
	}

	public UserResponse toResponse(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getCreatedAt(),
				user.getUpdatedAt());
	}
}
