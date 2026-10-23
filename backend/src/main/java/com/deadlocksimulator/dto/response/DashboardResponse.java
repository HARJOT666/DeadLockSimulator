package com.deadlocksimulator.dto.response;

import com.deadlocksimulator.enums.StrategyType;

import java.util.List;

/** Every number is computed from the caller's persisted data. */
public record DashboardResponse(long totalScenarios, long totalRuns, long deadlockedRuns, long comparedRuns,
                                long recoverySuccesses, long recoveryFailures,
                                List<StrategyStats> strategies, List<HistoryItemResponse> recentRuns,
                                Long latestDeadlockedRunId) {

    public record StrategyStats(StrategyType strategy, String displayName, long executions, double averageScore,
                                double averageWorkLost, long timesRankedFirst, long recovered) {
    }
}
