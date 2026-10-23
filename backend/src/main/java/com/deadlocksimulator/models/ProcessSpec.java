package com.deadlocksimulator.models;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Static definition of a process inside a scenario.
 *
 * @param id                   unique id, e.g. "P1"
 * @param priority             1 (least important) .. 10 (most important)
 * @param totalWork            work units needed to complete
 * @param initialWork          work already performed when the simulation starts
 * @param previousVictimCount  victim history carried into the scenario (models earlier recoveries)
 */
public record ProcessSpec(
        @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]{0,15}") String id,
        @Min(1) @Max(10) int priority,
        @Min(1) @Max(1_000_000) int totalWork,
        @Min(0) int initialWork,
        @Min(0) @Max(100) int previousVictimCount) {
}
