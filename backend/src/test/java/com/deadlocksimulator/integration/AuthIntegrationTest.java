package com.deadlocksimulator.integration;

import com.deadlocksimulator.security.AppUserPrincipal;
import com.deadlocksimulator.security.JwtService;
import com.deadlocksimulator.enums.Role;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    @Autowired
    PasswordEncoder passwordEncoder;
    @Value("${app.jwt.secret}")
    String secret;

    @Test
    void registrationStoresABcryptHashAndNeverReturnsIt() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Ada", "email", "Ada@Example.com", "password", "secret123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("ada@example.com"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
                .andReturn();
        assertThat(r.getResponse().getContentAsString()).doesNotContain("secret123").doesNotContain("$2a$");
        String hash = jdbc.queryForObject("select password_hash from users where email = 'ada@example.com'", String.class);
        assertThat(hash).startsWith("$2a$").isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", hash)).isTrue();
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        register("Ada", "ada@example.com", "secret123");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Other", "email", "ADA@example.com", "password", "secret123"))))
                .andExpect(status().isConflict());
    }

    @Test
    void registrationValidatesInput() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "", "email", "not-an-email", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("email"))))
                .andExpect(jsonPath("$.details", hasItem(containsString("password"))))
                .andExpect(jsonPath("$.details", hasItem(containsString("name"))));
    }

    @Test
    void loginIssuesAWorkingTokenAndCookie() throws Exception {
        register("Ada", "ada@example.com", "secret123");
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "ada@example.com", "password", "secret123"))))
                .andExpect(status().isOk()).andReturn();
        String token = body(r).get("token").asText();
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("ada@example.com"));
        Cookie cookie = r.getResponse().getCookie("dls_token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        mvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isOk());
    }

    @Test
    void invalidCredentialsAreRejectedWithoutRevealingWhichPartWasWrong() throws Exception {
        register("Ada", "ada@example.com", "secret123");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "ada@example.com", "password", "wrong1234"))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid email or password"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "nobody@example.com", "password", "secret123"))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void protectedEndpointsRejectMissingTamperedAndExpiredTokens() throws Exception {
        String token = register("Ada", "ada@example.com", "secret123");
        for (String path : new String[]{"/api/auth/me", "/api/scenarios", "/api/history", "/api/dashboard"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        String tampered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(tampered))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", bearer("not.a.jwt"))).andExpect(status().isUnauthorized());

        Long id = jdbc.queryForObject("select id from users", Long.class);
        String expired = new JwtService(secret, -1)
                .issue(new AppUserPrincipal(id, "Ada", "ada@example.com", "", Role.USER));
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(expired))).andExpect(status().isUnauthorized());

        String otherKey = new JwtService("b3RoZXIta2V5LW90aGVyLWtleS1vdGhlci1rZXktMzJieXRlcw==", 10)
                .issue(new AppUserPrincipal(id, "Ada", "ada@example.com", "", Role.USER));
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(otherKey))).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutClearsTheCookie() throws Exception {
        mvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }
}
