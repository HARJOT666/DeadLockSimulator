package com.deadlocksimulator.security;

import com.deadlocksimulator.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public AppUserPrincipal loadUserByUsername(String email) {
        return users.findByEmailIgnoreCase(email).map(AppUserPrincipal::of)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public AppUserPrincipal loadById(Long id) {
        return users.findById(id).map(AppUserPrincipal::of)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
