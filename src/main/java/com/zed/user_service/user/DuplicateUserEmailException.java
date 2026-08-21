package com.zed.user_service.user;

public class DuplicateUserEmailException extends RuntimeException {

	public DuplicateUserEmailException(String email) {
		super("A user with email already exists: " + email);
	}
}
