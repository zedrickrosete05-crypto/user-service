package com.zed.user_service.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final SecretKey signingKey;
	private final Duration expiration;

	public JwtService(
			@Value("${app.security.jwt.secret}") String secret,
			@Value("${app.security.jwt.expiration:PT1H}") Duration expiration) {
		this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
		this.expiration = expiration;
	}

	public String generateToken(UserDetails userDetails) {
		Date issuedAt = new Date();
		Date expiresAt = new Date(issuedAt.getTime() + expiration.toMillis());
		return Jwts.builder()
				.subject(userDetails.getUsername())
				.issuedAt(issuedAt)
				.expiration(expiresAt)
				.signWith(signingKey)
				.compact();
	}

	public String extractUsername(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getSubject();
	}

	public long getExpirationSeconds() {
		return expiration.toSeconds();
	}
}
