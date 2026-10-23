package com.deadlocksimulator.dto.response;

import com.deadlocksimulator.entities.User;
import com.deadlocksimulator.enums.Role;

import java.time.Instant;

/** Public view of a user — never includes the password hash. */
public record UserResponse(Long id, String name, String email, Role role, Instant createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getCreatedAt());
    }
}
