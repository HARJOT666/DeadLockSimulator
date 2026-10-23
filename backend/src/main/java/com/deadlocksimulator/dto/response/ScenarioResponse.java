package com.deadlocksimulator.dto.response;

import com.deadlocksimulator.models.ScenarioConfig;

import java.time.Instant;

/** Scenario with its configuration; {@code config} is null in list views. */
public record ScenarioResponse(Long id, String name, String description, int processCount, int resourceCount,
                               int operationCount, int checkpointInterval, Long seed, long runCount,
                               Instant createdAt, Instant updatedAt, ScenarioConfig config) {
}
