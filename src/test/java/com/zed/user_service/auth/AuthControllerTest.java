package com.zed.user_service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.zed.user_service.auth.dto.LoginRequest;
import com.zed.user_service.auth.dto.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

	@Mock
	private UserDetailsService userDetailsService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtService jwtService;

	@Mock
	private RefreshTokenService refreshTokenService;

	private AuthController authController;

	@BeforeEach
	void setUp() {
		authController = new AuthController(
				userDetailsService, passwordEncoder, jwtService, refreshTokenService);
	}

	@Test
	void loginReturnsAccessToken() {
		var user = User.withUsername("user@example.com").password("hash").roles("USER").build();
		when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(user);
		when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
		when(jwtService.generateToken(user)).thenReturn("token");
		when(jwtService.getExpirationSeconds()).thenReturn(3600L);
		when(refreshTokenService.issue("user@example.com"))
				.thenReturn(new RefreshTokenService.IssuedRefreshToken("refresh", 2592000L));

		LoginResponse response = authController.login(
				new LoginRequest(" USER@example.com ", "password123"));

		assertThat(response).isEqualTo(
				new LoginResponse("token", "Bearer", 3600L, "refresh", 2592000L));
	}

	@Test
	void loginRejectsInvalidPassword() {
		var user = User.withUsername("user@example.com").password("hash").roles("USER").build();
		when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(user);
		when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

		assertThatThrownBy(() -> authController.login(
				new LoginRequest("user@example.com", "wrong")))
				.isInstanceOf(BadCredentialsException.class);
	}
}
