package com.deadlocksimulator.models;

import com.deadlocksimulator.enums.HaltReason;
import com.deadlocksimulator.enums.RecoveryStatus;
import com.deadlocksimulator.enums.StrategyType;

import java.util.List;

/** One strategy's complete, verified and scored result. */
public record StrategyEvaluation(
        StrategyType strategy,
        RecoveryStatus status,
        HaltReason haltReason,
        List<RoundTrace> rounds,
        VerificationResult verification,
        RecoveryMetrics metrics,
        SystemState finalState,
        long executionMicros) {
}
