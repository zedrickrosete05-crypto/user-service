package com.zed.user_service.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

class JwtServiceTest {

	@Test
	void generatedTokenContainsUsernameAndExpiration() {
		JwtService jwtService = new JwtService(
				"VGhpc0lzQURldmVsb3BtZW50U2VjcmV0S2V5VGhhdElzQXQ",
				java.time.Duration.ofHours(1));
		var user = User.withUsername("user@example.com").password("hash").roles("USER").build();

		String token = jwtService.generateToken(user);

		assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
		assertThat(jwtService.getExpirationSeconds()).isEqualTo(3600);
	}
}
