package com.deadlocksimulator.models;

import com.deadlocksimulator.enums.RecoveryStatus;
import com.deadlocksimulator.enums.StrategyType;

import java.util.List;
import java.util.Map;

/**
 * Result of running all strategies on independent copies of one deadlocked snapshot.
 *
 * @param snapshotFingerprint SHA-256 of the serialised snapshot before recovery
 * @param snapshotUnchanged   the snapshot's fingerprint is identical after all strategies ran
 *                            (proves the strategies worked on isolated copies)
 * @param copiesIdentical     all strategy copies had the snapshot's fingerprint before execution
 */
public record ComparisonReport(
        String snapshotFingerprint,
        boolean snapshotUnchanged,
        boolean copiesIdentical,
        ScoreWeights weights,
        VictimWeights victimWeights,
        List<StrategyEvaluation> evaluations,
        List<RankingEntry> ranking,
        Recommendation recommendation,
        List<SensitivityProfile> sensitivity) {

    public record RankingEntry(int rank, StrategyType strategy, RecoveryStatus status, boolean eligible,
                               double score, String explanation) {
    }

    /**
     * @param strategy        recommended strategy (null if none recovered successfully)
     * @param headline        one-sentence recommendation
     * @param tradeoffs       bullet-point trade-offs versus the alternatives
     * @param caveat          why this is conditional on weights and workload
     * @param weightSensitive true if a different weight profile would pick another winner
     */
    public record Recommendation(StrategyType strategy, String headline, List<String> tradeoffs, String caveat,
                                 boolean weightSensitive) {
    }

    /** Winner under an alternative, named weighting. */
    public record SensitivityProfile(String name, ScoreWeights weights, StrategyType winner,
                                     Map<StrategyType, Double> scores) {
    }
}
