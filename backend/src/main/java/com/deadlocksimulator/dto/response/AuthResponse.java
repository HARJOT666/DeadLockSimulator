package com.deadlocksimulator.dto.response;

/**
 * Login / registration result. The browser app relies on the HttpOnly cookie set alongside this
 * response and ignores {@code token}; the token is returned for non-browser API clients.
 */
public record AuthResponse(UserResponse user, String token, long expiresInSeconds) {
}
