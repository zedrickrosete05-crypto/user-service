package com.zed.user_service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtService jwtService;

	@Mock
	private UserDetailsService userDetailsService;

	@Mock
	private FilterChain filterChain;

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void validBearerTokenAuthenticatesRequest() throws Exception {
		when(jwtService.extractUsername("token")).thenReturn("user@example.com");
		when(userDetailsService.loadUserByUsername("user@example.com"))
				.thenReturn(User.withUsername("user@example.com").password("hash").roles("USER").build());

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer token");
		new JwtAuthenticationFilter(jwtService, userDetailsService)
				.doFilter(request, new MockHttpServletResponse(), filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
				.isEqualTo("user@example.com");
	}

	@Test
	void invalidBearerTokenDoesNotAuthenticateRequest() throws Exception {
		when(jwtService.extractUsername("token"))
				.thenThrow(new io.jsonwebtoken.JwtException("invalid"));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer token");
		new JwtAuthenticationFilter(jwtService, userDetailsService)
				.doFilter(request, new MockHttpServletResponse(), filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}
}
