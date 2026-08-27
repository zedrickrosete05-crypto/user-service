package com.zed.user_service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	@Mock
	private RefreshTokenRepository repository;

	@Mock
	private UserDetailsService userDetailsService;

	private RefreshTokenService service;

	@BeforeEach
	void setUp() {
		service = new RefreshTokenService(repository, userDetailsService, Duration.ofDays(30));
	}

	@Test
	void issueStoresHashedToken() {
		RefreshTokenService.IssuedRefreshToken issued = service.issue("user@example.com");

		assertThat(issued.value()).isNotBlank();
		assertThat(issued.expiresIn()).isEqualTo(30L * 24 * 60 * 60);
		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getTokenHash()).hasSize(64);
		assertThat(captor.getValue().getUserEmail()).isEqualTo("user@example.com");
	}

	@Test
	void rotateRevokesCurrentAndIssuesReplacement() {
		RefreshToken current = token("user@example.com");
		when(repository.findByTokenHash(any(String.class))).thenReturn(Optional.of(current));
		when(userDetailsService.loadUserByUsername("user@example.com"))
				.thenReturn(User.withUsername("user@example.com").password("hash").roles("USER").build());

		RefreshTokenService.RotatedRefreshToken rotated = service.rotate("refresh-token");

		assertThat(rotated.value()).isNotBlank();
		assertThat(current.getRevokedAt()).isNotNull();
		verify(repository, org.mockito.Mockito.times(2)).save(any(RefreshToken.class));
	}

	@Test
	void rotateRejectsExpiredToken() {
		RefreshToken current = token("user@example.com");
		current.setExpiresAt(Instant.now().minusSeconds(1));
		when(repository.findByTokenHash(any(String.class))).thenReturn(Optional.of(current));

		assertThatThrownBy(() -> service.rotate("refresh-token"))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void revokeMarksExistingToken() {
		RefreshToken current = token("user@example.com");
		when(repository.findByTokenHash(any(String.class))).thenReturn(Optional.of(current));

		service.revoke("refresh-token");

		assertThat(current.getRevokedAt()).isNotNull();
		verify(repository).save(current);
	}

	private RefreshToken token(String email) {
		RefreshToken token = new RefreshToken();
		token.setTokenHash("hash");
		token.setUserEmail(email);
		token.setExpiresAt(Instant.now().plus(Duration.ofDays(1)));
		return token;
	}
}
