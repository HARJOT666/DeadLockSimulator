package com.deadlocksimulator.dto.request;

import com.deadlocksimulator.models.ScoreWeights;
import com.deadlocksimulator.models.VictimWeights;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Optional tuning for a recovery execution; every field falls back to its documented default.
 *
 * @param scoreWeights  aggregate-score weights
 * @param victimWeights victim-selection weights
 * @param maxRounds     safety limit on recovery rounds (default 2|P| + 5)
 */
public record RecoveryRequest(@Valid ScoreWeights scoreWeights, @Valid VictimWeights victimWeights,
                              @Min(1) @Max(500) Integer maxRounds) {

    public static RecoveryRequest defaults() {
        return new RecoveryRequest(null, null, null);
    }

    @AssertTrue(message = "at least one aggregate-score weight must be positive")
    public boolean isScoreWeightsUsable() {
        return scoreWeights == null || scoreWeights.sum() > 0;
    }

    @AssertTrue(message = "at least one victim-selection weight must be positive")
    public boolean isVictimWeightsUsable() {
        return victimWeights == null || victimWeights.work() + victimWeights.priority()
                + victimWeights.resources() + victimWeights.history() > 0;
    }
}
