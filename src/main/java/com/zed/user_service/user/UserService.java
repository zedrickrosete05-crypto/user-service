package com.zed.user_service.user;

import com.zed.user_service.user.dto.CreateUserRequest;
import com.zed.user_service.user.dto.UpdateUserRequest;
import com.zed.user_service.user.dto.UserResponse;
import java.util.Locale;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	public UserService(
			UserRepository userRepository,
			UserMapper userMapper,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UserResponse createUser(CreateUserRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new DuplicateUserEmailException(email);
		}

		CreateUserRequest normalizedRequest = new CreateUserRequest(
				email,
				request.name().trim(),
				request.password());
		User user = userMapper.toEntity(
				normalizedRequest,
				passwordEncoder.encode(normalizedRequest.password()));

		return userMapper.toResponse(userRepository.save(user));
	}

	@Transactional(readOnly = true)
	public UserResponse getUser(Long id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new UserNotFoundException(id));
		return userMapper.toResponse(user);
	}

	@Transactional(readOnly = true)
	public UserResponse getUser(Long id, String authenticatedEmail, boolean admin) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new UserNotFoundException(id));
		if (!admin && !user.getEmail().equals(authenticatedEmail)) {
			throw new AccessDeniedException("You can only access your own user profile");
		}
		return userMapper.toResponse(user);
	}

	@Transactional(readOnly = true)
	public List<UserResponse> getUsers() {
		return userRepository.findAll().stream()
				.map(userMapper::toResponse)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<UserResponse> getUsers(boolean admin) {
		if (!admin) {
			throw new AccessDeniedException("Only administrators can list users");
		}
		return getUsers();
	}

	@Transactional
	public UserResponse updateUser(Long id, UpdateUserRequest request) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new UserNotFoundException(id));
		String email = normalizeEmail(request.email());
		if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
			throw new DuplicateUserEmailException(email);
		}

		user.setEmail(email);
		user.setName(request.name().trim());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		return userMapper.toResponse(userRepository.save(user));
	}

	@Transactional
	public UserResponse updateUser(
			Long id,
			UpdateUserRequest request,
			String authenticatedEmail,
			boolean admin) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new UserNotFoundException(id));
		if (!admin && !user.getEmail().equals(authenticatedEmail)) {
			throw new AccessDeniedException("You can only update your own user profile");
		}
		return updateUser(id, request);
	}

	@Transactional
	public void deleteUser(Long id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new UserNotFoundException(id));
		userRepository.delete(user);
	}

	@Transactional
	public void deleteUser(Long id, String authenticatedEmail, boolean admin) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new UserNotFoundException(id));
		if (!admin && !user.getEmail().equals(authenticatedEmail)) {
			throw new AccessDeniedException("You can only delete your own user profile");
		}
		userRepository.delete(user);
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
