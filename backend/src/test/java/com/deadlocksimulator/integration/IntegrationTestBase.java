package com.deadlocksimulator.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full Spring context against the real PostgreSQL test database (see application-test.yml).
 * Flyway migrates it on startup; every test starts from empty tables.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper mapper;
    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE recovery_results, simulation_runs, scenarios, users RESTART IDENTITY CASCADE");
    }

    protected String json(Object o) throws Exception {
        return mapper.writeValueAsString(o);
    }

    protected JsonNode body(MvcResult r) throws Exception {
        return mapper.readTree(r.getResponse().getContentAsString());
    }

    /** Registers a user and returns its bearer token. */
    protected String register(String name, String email, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name, "email", email, "password", password))))
                .andExpect(status().isCreated()).andReturn();
        return body(r).get("token").asText();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    /** Creates a scenario from a built-in template and returns its id. */
    protected long createScenarioFromTemplate(String token, String key) throws Exception {
        JsonNode templates = body(mvc.perform(get("/api/scenarios/templates").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn());
        JsonNode t = null;
        for (JsonNode n : templates) {
            if (n.get("key").asText().equals(key)) {
                t = n;
            }
        }
        var req = Map.of("name", t.get("name").asText(), "description", t.get("description").asText(),
                "config", t.get("config"));
        return body(mvc.perform(post("/api/scenarios").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    protected long startRun(String token, long scenarioId) throws Exception {
        return body(mvc.perform(post("/api/simulation/start").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("scenarioId", scenarioId))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }
}
