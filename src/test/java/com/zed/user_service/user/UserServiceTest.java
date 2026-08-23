package com.zed.user_service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zed.user_service.user.dto.CreateUserRequest;
import com.zed.user_service.user.dto.UpdateUserRequest;
import com.zed.user_service.user.dto.UserResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserMapper userMapper;

	@Mock
	private PasswordEncoder passwordEncoder;

	private UserService userService;

	@BeforeEach
	void setUp() {
		userService = new UserService(userRepository, userMapper, passwordEncoder);
	}

	@Test
	void createUserNormalizesAndHashesPassword() {
		CreateUserRequest request = new CreateUserRequest(" TEST@Example.COM ", " Jane ", "password123");
		User user = new User();
		UserResponse response = response(1L);
		when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
		when(userMapper.toEntity(any(CreateUserRequest.class), any())).thenReturn(user);
		when(userRepository.save(user)).thenReturn(user);
		when(userMapper.toResponse(user)).thenReturn(response);

		UserResponse result = userService.createUser(request);

		assertThat(result).isEqualTo(response);
		ArgumentCaptor<CreateUserRequest> requestCaptor = ArgumentCaptor.forClass(CreateUserRequest.class);
		verify(userMapper).toEntity(requestCaptor.capture(), org.mockito.ArgumentMatchers.eq("hashed-password"));
		assertThat(requestCaptor.getValue().email()).isEqualTo("test@example.com");
		assertThat(requestCaptor.getValue().name()).isEqualTo("Jane");
	}

	@Test
	void createUserRejectsDuplicateEmail() {
		CreateUserRequest request = new CreateUserRequest("test@example.com", "Jane", "password123");
		when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

		assertThatThrownBy(() -> userService.createUser(request))
				.isInstanceOf(DuplicateUserEmailException.class);
		verify(passwordEncoder, never()).encode(any());
	}

	@Test
	void getUserReturnsMappedUser() {
		User user = new User();
		UserResponse response = response(1L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userMapper.toResponse(user)).thenReturn(response);

		assertThat(userService.getUser(1L)).isEqualTo(response);
	}

	@Test
	void getUserRejectsUnknownId() {
		when(userRepository.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getUser(1L))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void getUsersReturnsMappedUsers() {
		User first = new User();
		User second = new User();
		when(userRepository.findAll()).thenReturn(List.of(first, second));
		when(userMapper.toResponse(first)).thenReturn(response(1L));
		when(userMapper.toResponse(second)).thenReturn(response(2L));

		assertThat(userService.getUsers()).containsExactly(response(1L), response(2L));
	}

	@Test
	void updateUserUpdatesFieldsAndHashesPassword() {
		User user = new User();
		user.setEmail("old@example.com");
		UpdateUserRequest request = new UpdateUserRequest(" NEW@Example.COM ", " Jane ", "password123");
		UserResponse response = response(1L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(passwordEncoder.encode("password123")).thenReturn("new-hash");
		when(userRepository.save(user)).thenReturn(user);
		when(userMapper.toResponse(user)).thenReturn(response);

		assertThat(userService.updateUser(1L, request)).isEqualTo(response);
		assertThat(user.getEmail()).isEqualTo("new@example.com");
		assertThat(user.getName()).isEqualTo("Jane");
		assertThat(user.getPasswordHash()).isEqualTo("new-hash");
		verify(userRepository).existsByEmail("new@example.com");
	}

	@Test
	void updateUserRejectsDuplicateEmail() {
		User user = new User();
		user.setEmail("old@example.com");
		UpdateUserRequest request = new UpdateUserRequest("new@example.com", "Jane", "password123");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

		assertThatThrownBy(() -> userService.updateUser(1L, request))
				.isInstanceOf(DuplicateUserEmailException.class);
		verify(userRepository, never()).save(any());
	}

	@Test
	void deleteUserDeletesExistingUser() {
		User user = new User();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		userService.deleteUser(1L);

		verify(userRepository).delete(user);
	}

	@Test
	void deleteUserRejectsUnknownId() {
		when(userRepository.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.deleteUser(1L))
				.isInstanceOf(UserNotFoundException.class);
		verify(userRepository, never()).delete(any());
	}

	private UserResponse response(Long id) {
		Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
		return new UserResponse(id, "user" + id + "@example.com", "User " + id, timestamp, timestamp);
	}
}
