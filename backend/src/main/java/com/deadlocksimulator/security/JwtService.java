package com.deadlocksimulator.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and validates HS256-signed JWTs (jjwt 0.12).
 *
 * <p>The signing key comes from {@code app.jwt.secret} (env {@code JWT_SECRET}, base64, ≥ 256 bits).
 * No key is hard-coded: if none is configured, a random ephemeral key is generated and a warning is
 * logged — fine for a local demo, but every restart invalidates all tokens.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String ISSUER = "deadlock-simulator";

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.expiration-minutes:120}") long expirationMinutes) {
        if (secret == null || secret.isBlank()) {
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            this.key = Keys.hmacShaKeyFor(random);
            log.warn("JWT_SECRET is not set: using an EPHEMERAL signing key. Tokens will be invalid after restart. "
                    + "Set JWT_SECRET (base64, >= 32 bytes) outside local development.");
        } else {
            this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        }
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public String issue(AppUserPrincipal user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(user.id()))
                .claim("email", user.email())
                .claim("role", user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /** Returns the user id if the token is well-formed, correctly signed, from us and not expired. */
    public Optional<Long> validate(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).requireIssuer(ISSUER).build()
                    .parseSignedClaims(token).getPayload();
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Duration getExpiration() {
        return expiration;
    }
}
