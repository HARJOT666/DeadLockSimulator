package com.deadlocksimulator.models;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Weights of the aggregate recovery-cost score (lower score = cheaper recovery).
 * Weights are re-normalised by their sum, so only their ratios matter.
 *
 * <p>Resources reclaimed defaults to weight 0: in this model it is almost entirely a consequence
 * of terminations / rollbacks, so weighting it by default would double-count the same disruption.
 *
 * @param starvationThreshold victim count at which starvation risk is considered maximal (normaliser)
 */
public record ScoreWeights(
        @DecimalMin("0.0") double workLost,
        @DecimalMin("0.0") double processesTerminated,
        @DecimalMin("0.0") double processesRolledBack,
        @DecimalMin("0.0") double resourcesReclaimed,
        @DecimalMin("0.0") double recoverySteps,
        @DecimalMin("0.0") double starvationRisk,
        @Min(1) @Max(100) int starvationThreshold) {

    public static ScoreWeights defaults() {
        return new ScoreWeights(0.35, 0.25, 0.10, 0.0, 0.10, 0.20, 3);
    }

    public double sum() {
        return workLost + processesTerminated + processesRolledBack + resourcesReclaimed + recoverySteps + starvationRisk;
    }
}
