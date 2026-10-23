package com.deadlocksimulator.dto.response;

import com.deadlocksimulator.enums.RunStatus;
import com.deadlocksimulator.enums.StrategyType;
import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.ScenarioConfig;

import java.time.Instant;
import java.util.List;

public record SimulationRunResponse(
        Long id,
        Long scenarioId,
        String scenarioName,
        RunStatus status,
        boolean deadlocked,
        int cycleCount,
        int deadlockedProcessCount,
        String snapshotFingerprint,
        DetectionResult detection,
        StrategyType recommendedStrategy,
        List<StrategyType> strategiesExecuted,
        ScenarioConfig scenarioConfig,
        Instant createdAt) {
}
