package com.deadlocksimulator.security;

import com.deadlocksimulator.entities.User;
import com.deadlocksimulator.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Authenticated user as seen by controllers ({@code @AuthenticationPrincipal}). */
public record AppUserPrincipal(Long id, String name, String email, String passwordHash, Role role)
        implements UserDetails {

    public static AppUserPrincipal of(User u) {
        return new AppUserPrincipal(u.getId(), u.getName(), u.getEmail(), u.getPasswordHash(), u.getRole());
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
