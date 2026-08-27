package com.zed.user_service.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final UserDetailsService userDetailsService;
	private final Duration expiration;

	public RefreshTokenService(
			RefreshTokenRepository refreshTokenRepository,
			UserDetailsService userDetailsService,
			@Value("${app.security.refresh-token.expiration:P30D}") Duration expiration) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.userDetailsService = userDetailsService;
		this.expiration = expiration;
	}

	@Transactional
	public IssuedRefreshToken issue(String email) {
		IssuedEntity issued = issueEntity(email);
		refreshTokenRepository.save(issued.entity());
		return new IssuedRefreshToken(issued.rawToken(), expiration.toSeconds());
	}

	@Transactional
	public RotatedRefreshToken rotate(String rawToken) {
		RefreshToken current = findValid(rawToken);
		UserDetails user = userDetailsService.loadUserByUsername(current.getUserEmail());
		current.setRevokedAt(Instant.now());
		IssuedEntity issued = issueEntity(user.getUsername());
		refreshTokenRepository.save(current);
		refreshTokenRepository.save(issued.entity());
		return new RotatedRefreshToken(user, issued.rawToken());
	}

	@Transactional
	public void revoke(String rawToken) {
		refreshTokenRepository.findByTokenHash(hash(rawToken))
				.ifPresent(token -> {
					if (token.getRevokedAt() == null) {
						token.setRevokedAt(Instant.now());
						refreshTokenRepository.save(token);
					}
				});
	}

	public long getExpirationSeconds() {
		return expiration.toSeconds();
	}

	private RefreshToken findValid(String rawToken) {
		RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
				.orElseThrow(InvalidRefreshTokenException::new);
		if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
			throw new InvalidRefreshTokenException();
		}
		return token;
	}

	private IssuedEntity issueEntity(String email) {
		String rawToken = UUID.randomUUID() + "." + UUID.randomUUID();
		RefreshToken token = new RefreshToken();
		token.setTokenHash(hash(rawToken));
		token.setUserEmail(email);
		token.setExpiresAt(Instant.now().plus(expiration));
		return new IssuedEntity(rawToken, token);
	}

	private String hash(String rawToken) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	public record IssuedRefreshToken(String value, long expiresIn) {
	}

	public record RotatedRefreshToken(UserDetails user, String value) {
	}

	private record IssuedEntity(String rawToken, RefreshToken entity) {
	}
}
