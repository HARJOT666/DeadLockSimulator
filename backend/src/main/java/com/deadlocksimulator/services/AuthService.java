package com.deadlocksimulator.services;

import com.deadlocksimulator.dto.request.LoginRequest;
import com.deadlocksimulator.dto.request.RegisterRequest;
import com.deadlocksimulator.dto.response.AuthResponse;
import com.deadlocksimulator.dto.response.UserResponse;
import com.deadlocksimulator.entities.User;
import com.deadlocksimulator.enums.Role;
import com.deadlocksimulator.exceptions.ConflictException;
import com.deadlocksimulator.exceptions.ResourceNotFoundException;
import com.deadlocksimulator.repositories.UserRepository;
import com.deadlocksimulator.security.AppUserPrincipal;
import com.deadlocksimulator.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Hash compared against when the email is unknown, so response time does not reveal registered emails. */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("timing-equaliser-not-a-password");
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalise(req.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user;
        try {
            user = users.saveAndFlush(new User(req.name().trim(), email, passwordEncoder.encode(req.password()), Role.USER));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("An account with this email already exists");
        }
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        Optional<User> user = users.findByEmailIgnoreCase(normalise(req.email()));
        String hash = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(req.password(), hash);
        if (user.isEmpty() || !matches) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return authResponse(user.get());
    }

    @Transactional(readOnly = true)
    public UserResponse me(AppUserPrincipal principal) {
        return users.findById(principal.id()).map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.id()));
    }

    private AuthResponse authResponse(User user) {
        String token = jwtService.issue(AppUserPrincipal.of(user));
        return new AuthResponse(UserResponse.from(user), token, jwtService.getExpiration().toSeconds());
    }

    private static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
