package com.deadlocksimulator.models;

import jakarta.validation.constraints.DecimalMin;

/**
 * Weights of the victim-selection cost (lower cost = preferred victim).
 *
 * <pre>
 * cost(P) = wWork     * norm(workAtRisk)            more work at stake   -> costlier
 *         + wPriority * norm(priority)              more important       -> costlier
 *         + wResources* (1 - norm(resourcesHeld))   holds more resources -> cheaper (frees more)
 *         + wHistory  * norm(victimCount)           victimised before    -> costlier (anti-starvation)
 * </pre>
 * norm() is min-max normalisation over the current candidate set (0 if all candidates are equal).
 */
public record VictimWeights(
        @DecimalMin("0.0") double work,
        @DecimalMin("0.0") double priority,
        @DecimalMin("0.0") double resources,
        @DecimalMin("0.0") double history) {

    public static VictimWeights defaults() {
        return new VictimWeights(0.4, 0.2, 0.2, 0.2);
    }
}
