package com.deadlocksimulator.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SimulationWorkflowIntegrationTest extends IntegrationTestBase {

    @Test
    void scenarioIsPersistedAsJsonbAndCanBeUpdatedAndDeleted() throws Exception {
        String token = register("Ada", "ada@example.com", "secret123");
        long id = createScenarioFromTemplate(token, "classic-three-cycle");
        assertThat(jdbc.queryForObject("select jsonb_array_length(config->'requestSequence') from scenarios where id = ?",
                Integer.class, id)).isEqualTo(9);
        assertThat(jdbc.queryForObject("select process_count from scenarios where id = ?", Integer.class, id)).isEqualTo(3);

        JsonNode scenario = body(mvc.perform(get("/api/scenarios/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn());
        var update = Map.of("name", "Renamed", "description", "x", "config", scenario.get("config"));
        mvc.perform(put("/api/scenarios/" + id).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Renamed"));
        mvc.perform(get("/api/scenarios").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$[0].name").value("Renamed")).andExpect(jsonPath("$[0].config").doesNotExist());
        mvc.perform(delete("/api/scenarios/" + id).header("Authorization", bearer(token))).andExpect(status().isNoContent());
        mvc.perform(get("/api/scenarios/" + id).header("Authorization", bearer(token))).andExpect(status().isNotFound());
    }

    @Test
    void unexecutableScenariosAreRejectedBeforeBeingStored() throws Exception {
        String token = register("Ada", "ada@example.com", "secret123");
        var config = Map.of(
                "processes", List.of(Map.of("id", "P1", "priority", 5, "totalWork", 10, "initialWork", 0,
                        "previousVictimCount", 0)),
                "resources", List.of("R1"),
                "requestSequence", List.of(Map.of("type", "RELEASE", "processId", "P1", "resources", List.of("R1"))),
                "checkpointInterval", 5);
        mvc.perform(post("/api/scenarios").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "bad", "config", config))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details", hasItem(containsString("not the holder"))));
        mvc.perform(post("/api/scenarios").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "bad", "config", Map.of("processes", List.of())))))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from scenarios", Long.class)).isZero();
    }

    @Test
    void completeWorkflowDetectsRecoversVerifiesAndPersists() throws Exception {
        String token = register("Ada", "ada@example.com", "secret123");
        long scenarioId = createScenarioFromTemplate(token, "cascading-deadlock");

        // Simulation + detection
        JsonNode run = body(mvc.perform(post("/api/simulation/start").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("scenarioId", scenarioId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DEADLOCKED"))
                .andExpect(jsonPath("$.detection.cycles[0].processes").value(org.hamcrest.Matchers.contains("P1", "P2")))
                .andExpect(jsonPath("$.detection.blockedProcesses[0]").value("P3"))
                .andReturn());
        long runId = run.get("id").asLong();

        mvc.perform(get("/api/deadlock/" + runId).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.deadlocked").value(true));
        JsonNode graph = body(mvc.perform(get("/api/simulation/" + runId + "/graph").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes.length()").value(3))
                .andExpect(jsonPath("$.edges.length()").value(4)).andReturn());
        List<String> cycleEdges = new java.util.ArrayList<>();
        graph.get("edges").forEach(e -> { if (e.get("inCycle").asBoolean()) cycleEdges.add(e.get("id").asText()); });
        assertThat(cycleEdges).containsExactlyInAnyOrder("P1-P2-R2", "P2-P1-R1");

        // Comparison on isolated copies
        JsonNode cmp = body(mvc.perform(post("/api/recovery/" + runId + "/compare").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.snapshotUnchanged").value(true))
                .andExpect(jsonPath("$.summary.copiesIdentical").value(true))
                .andExpect(jsonPath("$.summary.persistedSnapshotIntact").value(true))
                .andExpect(jsonPath("$.summary.ranking[0].strategy").value("ROLLBACK"))
                .andExpect(jsonPath("$.results.length()").value(3))
                .andReturn());
        for (JsonNode r : cmp.get("results")) {
            assertThat(r.get("status").asText()).isEqualTo("RECOVERED");
            assertThat(r.get("verified").asBoolean()).isTrue();
            assertThat(r.get("verification").get("remainingCycles")).isEmpty();
            JsonNode m = r.get("metrics");
            for (String metric : List.of("workLost", "processesTerminated", "processesRolledBack", "resourcesReclaimed",
                    "recoverySteps", "starvationRisk", "aggregateScore")) {
                assertThat(m.has(metric)).as(metric).isTrue();
            }
        }

        // Persistence of all seven metrics in typed columns
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select strategy, status, verified, work_lost, processes_terminated, processes_rolled_back,
                       resources_reclaimed, recovery_steps, starvation_risk, aggregate_score, rank_position
                from recovery_results where run_id = ? order by strategy""", runId);
        assertThat(rows).hasSize(3);
        Map<String, Object> rollback = rows.stream().filter(r -> r.get("strategy").equals("ROLLBACK")).findFirst().orElseThrow();
        assertThat(rollback).containsEntry("work_lost", 5).containsEntry("processes_rolled_back", 2)
                .containsEntry("processes_terminated", 0).containsEntry("recovery_steps", 2)
                .containsEntry("starvation_risk", 1).containsEntry("rank_position", 1).containsEntry("verified", true);
        assertThat(jdbc.queryForObject("select status from simulation_runs where id = ?", String.class, runId))
                .isEqualTo("RECOVERY_EXECUTED");

        // After-state graph, stored comparison, metrics, CSV
        mvc.perform(get("/api/simulation/" + runId + "/graph").param("strategy", "TERMINATION").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.deadlocked").value(false))
                .andExpect(jsonPath("$.nodes[?(@.id == 'P1')].recoveryAction").value(hasItem("TERMINATE")));
        mvc.perform(get("/api/recovery/" + runId + "/comparison").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.summary.recommendation.strategy").value("ROLLBACK"));
        mvc.perform(get("/api/metrics/" + runId).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/metrics/" + runId + "/csv").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("ROLLBACK,RECOVERED,true,1,5,0,2")));
        mvc.perform(get("/api/simulation/" + runId + "/state").param("strategy", "ROLLBACK").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.processes.P3.state").value("READY"));
    }

    @Test
    void singleStrategyEndpointsPersistIndividually() throws Exception {
        String token = register("Ada", "ada@example.com", "secret123");
        long runId = startRun(token, createScenarioFromTemplate(token, "classic-three-cycle"));
        mvc.perform(post("/api/recovery/" + runId + "/terminate").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.strategy").value("TERMINATION"))
                .andExpect(jsonPath("$.metrics.workLost").value(10));
        mvc.perform(post("/api/recovery/" + runId + "/rollback").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("maxRounds", 3))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.metrics.processesRolledBack").value(1));
        mvc.perform(post("/api/recovery/" + runId + "/abort").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.metrics.processesTerminated").value(3));
        assertThat(jdbc.queryForObject("select count(*) from recovery_results where run_id = ?", Long.class, runId)).isEqualTo(3);
        // Re-running a strategy replaces its row (unique per run + strategy).
        mvc.perform(post("/api/recovery/" + runId + "/abort").header("Authorization", bearer(token))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from recovery_results where run_id = ?", Long.class, runId)).isEqualTo(3);
    }

    @Test
    void customWeightsChangeTheRecommendationAndInvalidWeightsAreRejected() throws Exception {
        String token = register("Ada", "ada@example.com", "secret123");
        long runId = startRun(token, createScenarioFromTemplate(token, "classic-three-cycle"));
        mvc.perform(post("/api/recovery/" + runId + "/compare").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.summary.recommendation.strategy").value("TERMINATION"));
        var survival = Map.of("scoreWeights", Map.of("workLost", 0.2, "processesTerminated", 0.6,
                "processesRolledBack", 0.05, "resourcesReclaimed", 0, "recoverySteps", 0.05, "starvationRisk", 0.1,
                "starvationThreshold", 3));
        mvc.perform(post("/api/recovery/" + runId + "/compare").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(survival)))
                .andExpect(jsonPath("$.summary.recommendation.strategy").value("ROLLBACK"));
        var zero = Map.of("scoreWeights", Map.of("workLost", 0, "processesTerminated", 0, "processesRolledBack", 0,
                "resourcesReclaimed", 0, "recoverySteps", 0, "starvationRisk", 0, "starvationThreshold", 3));
        mvc.perform(post("/api/recovery/" + runId + "/compare").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(zero)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void previewSimulatesAnUnsavedConfigurationWithoutPersistingIt() throws Exception {
        String token = register("Ada", "ada@example.com", "secret123");
        var config = Map.of(
                "processes", List.of(Map.of("id", "P1", "priority", 5, "totalWork", 10, "initialWork", 0, "previousVictimCount", 0),
                        Map.of("id", "P2", "priority", 5, "totalWork", 10, "initialWork", 0, "previousVictimCount", 0)),
                "resources", List.of("R1", "R2"),
                "requestSequence", List.of(
                        Map.of("type", "REQUEST", "processId", "P1", "resources", List.of("R1")),
                        Map.of("type", "REQUEST", "processId", "P2", "resources", List.of("R2")),
                        Map.of("type", "REQUEST", "processId", "P1", "resources", List.of("R2")),
                        Map.of("type", "REQUEST", "processId", "P2", "resources", List.of("R1"))),
                "checkpointInterval", 5);
        mvc.perform(post("/api/scenarios/preview").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(config)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.deadlocked").value(true))
                .andExpect(jsonPath("$.deadlockedProcesses.length()").value(2));
        assertThat(jdbc.queryForObject("select count(*) from scenarios", Long.class)).isZero();
    }

    @Test
    void noDeadlockRunNeedsNoRecovery() throws Exception {
        String token = register("Ada", "ada@example.com", "secret123");
        long runId = startRun(token, createScenarioFromTemplate(token, "no-deadlock"));
        mvc.perform(get("/api/simulation/" + runId).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.status").value("NO_DEADLOCK")).andExpect(jsonPath("$.deadlocked").value(false));
        mvc.perform(post("/api/recovery/" + runId + "/compare").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.results[*].status").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is("NOT_REQUIRED"))));
    }

    @Test
    void generatorPreviewIsDeterministicAndHistoryAndDashboardUseRealData() throws Exception {
        String token = register("Ada", "ada@example.com", "secret123");
        mvc.perform(get("/api/dashboard").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.totalScenarios").value(0)).andExpect(jsonPath("$.totalRuns").value(0))
                .andExpect(jsonPath("$.recentRuns.length()").value(0));

        var params = Map.ofEntries(Map.entry("seed", 11), Map.entry("cycleCount", 2), Map.entry("cycleLength", 3),
                Map.entry("blockedDependents", 1), Map.entry("independentProcesses", 1),
                Map.entry("workDistribution", "SKEWED"), Map.entry("minWork", 10), Map.entry("maxWork", 100),
                Map.entry("checkpointInterval", 10), Map.entry("resourceDensity", 0.5),
                Map.entry("maxExtraResources", 2), Map.entry("overlapping", true), Map.entry("maxPreviousVictims", 1));
        String a = mvc.perform(post("/api/scenarios/generate").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json(params))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String b = mvc.perform(post("/api/scenarios/generate").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json(params))).andReturn().getResponse().getContentAsString();
        assertThat(a).isEqualTo(b);
        assertThat(mapper.readTree(a).get("preview").get("deadlockedProcesses")).hasSize(6);

        long runId = startRun(token, createScenarioFromTemplate(token, "two-cycles-dependent"));
        mvc.perform(post("/api/recovery/" + runId + "/compare").header("Authorization", bearer(token))).andExpect(status().isOk());
        startRun(token, createScenarioFromTemplate(token, "no-deadlock"));

        mvc.perform(get("/api/dashboard").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.totalScenarios").value(2)).andExpect(jsonPath("$.totalRuns").value(2))
                .andExpect(jsonPath("$.deadlockedRuns").value(1)).andExpect(jsonPath("$.comparedRuns").value(1))
                .andExpect(jsonPath("$.recoverySuccesses").value(3)).andExpect(jsonPath("$.recoveryFailures").value(0))
                .andExpect(jsonPath("$.strategies.length()").value(3))
                .andExpect(jsonPath("$.latestDeadlockedRunId").value(runId));
        mvc.perform(get("/api/history").param("deadlocked", "true").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].results.length()").value(3));
        mvc.perform(get("/api/history").param("q", "ORDERED").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].status").value("NO_DEADLOCK"));
        mvc.perform(get("/api/history/csv").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    void runsSurviveScenarioDeletionBecauseTheyCopyTheConfiguration() throws Exception {
        String token = register("Ada", "ada@example.com", "secret123");
        long scenarioId = createScenarioFromTemplate(token, "classic-three-cycle");
        long runId = startRun(token, scenarioId);
        mvc.perform(delete("/api/scenarios/" + scenarioId).header("Authorization", bearer(token))).andExpect(status().isNoContent());
        mvc.perform(get("/api/simulation/" + runId).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.scenarioId").doesNotExist())
                .andExpect(jsonPath("$.scenarioConfig.processes.length()").value(3));
        mvc.perform(post("/api/recovery/" + runId + "/compare").header("Authorization", bearer(token))).andExpect(status().isOk());
    }
}
