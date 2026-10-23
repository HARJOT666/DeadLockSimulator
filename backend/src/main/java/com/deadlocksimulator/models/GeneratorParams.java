package com.deadlocksimulator.models;

import com.deadlocksimulator.enums.WorkDistribution;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Parameters of the seeded scenario generator — the experimental factors of the case study.
 *
 * @param seed                 random seed; same parameters + seed = identical scenario
 * @param cycleCount           number of disjoint circular waits
 * @param cycleLength          processes per cycle
 * @param blockedDependents    extra processes that wait on a cycle member (blocked, not deadlocked)
 * @param independentProcesses extra processes that run to completion
 * @param workDistribution     how work done is spread over cycle members
 * @param minWork              lower bound of work done before deadlock
 * @param maxWork              upper bound of work done before deadlock
 * @param checkpointInterval   periodic checkpoint interval (0 = only the initial checkpoint)
 * @param resourceDensity      0..1: fraction of {@code maxExtraResources} each cycle member also holds
 * @param maxExtraResources    extra (non-contended) resources per member at density 1
 * @param overlapping          if true, the first member of each cycle (length ≥ 3) issues an AND-request
 *                             for two successors' resources, creating overlapping cycles
 * @param maxPreviousVictims   victim history drawn uniformly from [0, maxPreviousVictims]
 */
public record GeneratorParams(
        long seed,
        @Min(1) @Max(5) int cycleCount,
        @Min(2) @Max(10) int cycleLength,
        @Min(0) @Max(10) int blockedDependents,
        @Min(0) @Max(10) int independentProcesses,
        @NotNull WorkDistribution workDistribution,
        @Min(0) @Max(10_000) int minWork,
        @Min(1) @Max(10_000) int maxWork,
        @Min(0) @Max(10_000) int checkpointInterval,
        @DecimalMin("0.0") @DecimalMax("1.0") double resourceDensity,
        @Min(0) @Max(5) int maxExtraResources,
        boolean overlapping,
        @Min(0) @Max(10) int maxPreviousVictims) {

    @AssertTrue(message = "minWork must not exceed maxWork")
    public boolean isWorkRangeValid() {
        return minWork <= maxWork;
    }

    public static GeneratorParams defaults() {
        return new GeneratorParams(42L, 1, 4, 1, 1, WorkDistribution.UNIFORM, 20, 120, 10, 0.5, 2, false, 0);
    }
}
