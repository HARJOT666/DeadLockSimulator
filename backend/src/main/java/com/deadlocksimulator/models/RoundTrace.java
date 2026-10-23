package com.deadlocksimulator.models;

import java.util.List;

/**
 * Evidence for one detect → act → re-detect iteration.
 *
 * @param round              1-based round number
 * @param cyclesBefore       cycles detected before acting
 * @param deadlockedBefore   deadlocked set before acting
 * @param candidates         victim candidates with cost breakdown (empty for total abort)
 * @param actions            actions applied in this round
 * @param cyclesAfter        cycles found when the WFG was rebuilt and DFS rerun afterwards
 * @param deadlockedAfter    deadlocked set afterwards
 */
public record RoundTrace(
        int round,
        List<List<String>> cyclesBefore,
        List<String> deadlockedBefore,
        List<CandidateScore> candidates,
        List<RecoveryAction> actions,
        List<List<String>> cyclesAfter,
        List<String> deadlockedAfter) {

    /** Serializable view of a victim candidate's cost breakdown. */
    public record CandidateScore(String processId, int workAtRisk, int priority, int resourcesHeld,
                                 int victimCount, double cost, double workTerm, double priorityTerm,
                                 double resourceTerm, double historyTerm, String option) {
    }
}
