package com.deadlocksimulator.dto.response;

import com.deadlocksimulator.enums.HaltReason;
import com.deadlocksimulator.enums.RecoveryStatus;
import com.deadlocksimulator.enums.StrategyType;
import com.deadlocksimulator.models.RecoveryMetrics;
import com.deadlocksimulator.models.RoundTrace;
import com.deadlocksimulator.models.ScoreWeights;
import com.deadlocksimulator.models.VerificationResult;
import com.deadlocksimulator.models.VictimWeights;

import java.time.Instant;
import java.util.List;

public record RecoveryResultResponse(
        Long id,
        StrategyType strategy,
        String displayName,
        RecoveryStatus status,
        HaltReason haltReason,
        boolean verified,
        Integer rank,
        List<RoundTrace> rounds,
        VerificationResult verification,
        RecoveryMetrics metrics,
        ScoreWeights scoreWeights,
        VictimWeights victimWeights,
        long executionMicros,
        Instant executedAt) {
}
