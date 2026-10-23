package com.deadlocksimulator.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DataIsolationIntegrationTest extends IntegrationTestBase {

    @Test
    void usersCannotSeeOrModifyEachOthersData() throws Exception {
        String alice = register("Alice", "alice@example.com", "secret123");
        String bob = register("Bob", "bob@example.com", "secret123");
        long scenario = createScenarioFromTemplate(alice, "classic-three-cycle");
        long run = startRun(alice, scenario);
        mvc.perform(post("/api/recovery/" + run + "/compare").header("Authorization", bearer(alice))).andExpect(status().isOk());

        String b = bearer(bob);
        mvc.perform(get("/api/scenarios/" + scenario).header("Authorization", b)).andExpect(status().isNotFound());
        mvc.perform(put("/api/scenarios/" + scenario).header("Authorization", b).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "x", "config", Map.of())))).andExpect(status().is4xxClientError());
        mvc.perform(delete("/api/scenarios/" + scenario).header("Authorization", b)).andExpect(status().isNotFound());
        mvc.perform(post("/api/simulation/start").header("Authorization", b).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("scenarioId", scenario)))).andExpect(status().isNotFound());
        for (String path : new String[]{"/api/simulation/" + run, "/api/simulation/" + run + "/state",
                "/api/simulation/" + run + "/graph", "/api/simulation/" + run + "/results", "/api/deadlock/" + run,
                "/api/metrics/" + run, "/api/metrics/" + run + "/csv", "/api/recovery/" + run + "/comparison"}) {
            mvc.perform(get(path).header("Authorization", b)).andExpect(status().isNotFound());
        }
        for (String action : new String[]{"terminate", "rollback", "abort", "compare"}) {
            mvc.perform(post("/api/recovery/" + run + "/" + action).header("Authorization", b))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/scenarios").header("Authorization", b)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/history").header("Authorization", b)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/dashboard").header("Authorization", b)).andExpect(jsonPath("$.totalRuns").value(0))
                .andExpect(jsonPath("$.strategies.length()").value(0));

        // Alice still sees everything, unchanged.
        mvc.perform(get("/api/history").header("Authorization", bearer(alice))).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void adminsCanInspectOtherUsersRuns() throws Exception {
        String alice = register("Alice", "alice@example.com", "secret123");
        long run = startRun(alice, createScenarioFromTemplate(alice, "classic-three-cycle"));
        register("Root", "root@example.com", "secret123");
        jdbc.update("update users set role = 'ADMIN' where email = 'root@example.com'");
        // Role is read from the database on every request, so the existing token picks up the promotion.
        String admin = body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", "root@example.com", "password", "secret123")))).andReturn())
                .get("token").asText();
        mvc.perform(get("/api/simulation/" + run).header("Authorization", bearer(admin))).andExpect(status().isOk());
        mvc.perform(get("/api/history").header("Authorization", bearer(admin))).andExpect(jsonPath("$.length()").value(1));
    }
}
