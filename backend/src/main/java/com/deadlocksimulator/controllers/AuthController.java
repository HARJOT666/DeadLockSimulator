package com.deadlocksimulator.controllers;

import com.deadlocksimulator.dto.request.LoginRequest;
import com.deadlocksimulator.dto.request.RegisterRequest;
import com.deadlocksimulator.dto.response.AuthResponse;
import com.deadlocksimulator.dto.response.UserResponse;
import com.deadlocksimulator.security.AppUserPrincipal;
import com.deadlocksimulator.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Authentication endpoints. On success the JWT is set as an HttpOnly, SameSite=Strict cookie so
 * that JavaScript (and therefore an XSS payload) cannot read it from storage.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final String cookieName;
    private final boolean cookieSecure;

    public AuthController(AuthService authService, @Value("${app.jwt.cookie-name:dls_token}") String cookieName,
                          @Value("${app.jwt.cookie-secure:false}") boolean cookieSecure) {
        this.authService = authService;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        AuthResponse res = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie(res.token(), Duration.ofSeconds(res.expiresInSeconds())))
                .body(res);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse res = authService.login(req);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(res.token(), Duration.ofSeconds(res.expiresInSeconds())))
                .body(res);
    }

    /** Clears the cookie. JWTs are stateless, so a copied token stays valid until it expires. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO)).build();
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUserPrincipal user) {
        return authService.me(user);
    }

    private String cookie(String value, Duration maxAge) {
        return ResponseCookie.from(cookieName, value).httpOnly(true).secure(cookieSecure).sameSite("Strict")
                .path("/api").maxAge(maxAge).build().toString();
    }
}
