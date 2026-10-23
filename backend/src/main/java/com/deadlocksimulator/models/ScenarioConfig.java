package com.deadlocksimulator.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * Complete, reproducible description of an experiment's initial conditions.
 * Persisted as JSONB so a run can always be replayed exactly.
 *
 * @param processes          process definitions
 * @param resources          single-instance resource ids
 * @param initialAllocations resources held before the sequence starts
 * @param requestSequence    deterministic, globally ordered operation trace
 * @param checkpointInterval a checkpoint is taken when a WORK operation makes the work counter cross
 *                           a multiple of this value — at the highest multiple reached (0 = only
 *                           the initial checkpoint)
 * @param seed               seed used by the generator (informational for hand-built scenarios)
 * @param parameters         experimental parameters (cycle length, density, ...) for reporting
 */
public record ScenarioConfig(
        @NotEmpty @Size(max = 100) @Valid List<ProcessSpec> processes,
        @NotEmpty @Size(max = 200) List<String> resources,
        @Valid List<InitialAllocation> initialAllocations,
        @NotEmpty @Size(max = 5000) @Valid List<Operation> requestSequence,
        @Min(0) @Max(100_000) int checkpointInterval,
        Long seed,
        Map<String, Object> parameters) {

    public ScenarioConfig {
        initialAllocations = initialAllocations == null ? List.of() : List.copyOf(initialAllocations);
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
