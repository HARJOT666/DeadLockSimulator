package com.deadlocksimulator.dto.response;

import com.deadlocksimulator.enums.RecoveryStatus;
import com.deadlocksimulator.enums.RunStatus;
import com.deadlocksimulator.enums.StrategyType;

import java.time.Instant;
import java.util.List;

public record HistoryItemResponse(Long runId, Long scenarioId, String scenarioName, Instant createdAt,
                                  RunStatus status, boolean deadlocked, int cycleCount, int deadlockedProcessCount,
                                  StrategyType recommendedStrategy, List<StrategySummary> results) {

    public record StrategySummary(StrategyType strategy, RecoveryStatus status, Integer rank, double aggregateScore,
                                  int workLost, int processesTerminated, int processesRolledBack,
                                  int resourcesReclaimed, int recoverySteps, int starvationRisk) {
    }
}
