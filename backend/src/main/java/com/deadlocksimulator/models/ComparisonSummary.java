package com.deadlocksimulator.models;

import java.util.List;

/** The comparison-level part of a {@link ComparisonReport} that is persisted on the run. */
public record ComparisonSummary(
        String snapshotFingerprint,
        boolean snapshotUnchanged,
        boolean copiesIdentical,
        boolean persistedSnapshotIntact,
        ScoreWeights weights,
        VictimWeights victimWeights,
        int maxRounds,
        List<ComparisonReport.RankingEntry> ranking,
        ComparisonReport.Recommendation recommendation,
        List<ComparisonReport.SensitivityProfile> sensitivity) {
}
