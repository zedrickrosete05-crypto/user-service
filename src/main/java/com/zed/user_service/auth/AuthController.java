package com.zed.user_service.auth;

import com.zed.user_service.auth.dto.LoginRequest;
import com.zed.user_service.auth.dto.LoginResponse;
import com.zed.user_service.auth.dto.RefreshTokenRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final UserDetailsService userDetailsService;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;

	public AuthController(
			UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			RefreshTokenService refreshTokenService) {
		this.userDetailsService = userDetailsService;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.refreshTokenService = refreshTokenService;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		UserDetails user;
		try {
			user = userDetailsService.loadUserByUsername(
					request.email().trim().toLowerCase(Locale.ROOT));
		} catch (UsernameNotFoundException exception) {
			throw new BadCredentialsException("Invalid email or password");
		}
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new BadCredentialsException("Invalid email or password");
		}
		RefreshTokenService.IssuedRefreshToken refreshToken =
				refreshTokenService.issue(user.getUsername());
		return new LoginResponse(
				jwtService.generateToken(user),
				"Bearer",
				jwtService.getExpirationSeconds(),
				refreshToken.value(),
				refreshToken.expiresIn());
	}

	@PostMapping("/refresh")
	public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
		RefreshTokenService.RotatedRefreshToken rotated =
				refreshTokenService.rotate(request.refreshToken());
		return new LoginResponse(
				jwtService.generateToken(rotated.user()),
				"Bearer",
				jwtService.getExpirationSeconds(),
				rotated.value(),
				refreshTokenService.getExpirationSeconds());
	}

	@PostMapping("/logout")
	public void logout(@Valid @RequestBody RefreshTokenRequest request) {
		refreshTokenService.revoke(request.refreshToken());
	}
}
