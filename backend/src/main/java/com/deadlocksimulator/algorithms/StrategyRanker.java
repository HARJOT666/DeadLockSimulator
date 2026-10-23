package com.deadlocksimulator.algorithms;

import com.deadlocksimulator.enums.RecoveryStatus;
import com.deadlocksimulator.enums.StrategyType;
import com.deadlocksimulator.models.ComparisonReport.RankingEntry;
import com.deadlocksimulator.models.ComparisonReport.Recommendation;
import com.deadlocksimulator.models.ComparisonReport.SensitivityProfile;
import com.deadlocksimulator.models.RecoveryMetrics;
import com.deadlocksimulator.models.ScoreWeights;
import com.deadlocksimulator.models.StrategyEvaluation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Ranks verified strategy results by aggregate score and explains the trade-offs.
 *
 * <p>Only strategies whose final state passed verification (RECOVERED) are eligible; failed or
 * partial recoveries are always ranked after them regardless of score. Ties on score are broken
 * by lower work lost, then fewer terminations, then enum order.
 *
 * <p>The recommendation is explicitly conditional: it reflects the configured weights and this
 * workload only. A sensitivity analysis re-scores the same metrics under alternative weightings.
 */
@Component
public class StrategyRanker {

    /** Named alternative weightings used for the sensitivity analysis. */
    public static final Map<String, ScoreWeights> PROFILES = Map.of(
            "Work preservation", new ScoreWeights(0.70, 0.10, 0.05, 0.0, 0.05, 0.10, 3),
            "Process survival", new ScoreWeights(0.20, 0.60, 0.05, 0.0, 0.05, 0.10, 3),
            "Fairness (anti-starvation)", new ScoreWeights(0.20, 0.10, 0.05, 0.0, 0.05, 0.60, 3),
            "Fast recovery", new ScoreWeights(0.20, 0.10, 0.10, 0.0, 0.50, 0.10, 3));

    private static final List<String> PROFILE_ORDER =
            List.of("Work preservation", "Process survival", "Fairness (anti-starvation)", "Fast recovery");

    public List<RankingEntry> rank(List<StrategyEvaluation> evaluations) {
        List<StrategyEvaluation> sorted = sort(evaluations, e -> e.metrics().aggregateScore());
        List<RankingEntry> ranking = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            StrategyEvaluation e = sorted.get(i);
            ranking.add(new RankingEntry(i + 1, e.strategy(), e.status(), eligible(e),
                    e.metrics().aggregateScore(), explain(e)));
        }
        return ranking;
    }

    public Recommendation recommend(List<StrategyEvaluation> evaluations, List<SensitivityProfile> sensitivity) {
        List<StrategyEvaluation> sorted = sort(evaluations, e -> e.metrics().aggregateScore());
        String caveat = "This recommendation is conditional on the configured weights and on this scenario's "
                + "cycle structure, work distribution, checkpoint interval and resource density. It is a "
                + "heuristic ranking, not a claim that one strategy is optimal in general.";
        if (sorted.isEmpty() || !eligible(sorted.get(0))) {
            return new Recommendation(null, "No strategy produced a verified deadlock-free state.",
                    List.of(), caveat, false);
        }
        StrategyEvaluation best = sorted.get(0);
        if (best.status() == RecoveryStatus.NOT_REQUIRED) {
            return new Recommendation(null, "The snapshot is not deadlocked, so no recovery is required.",
                    List.of(), caveat, false);
        }
        List<String> tradeoffs = new ArrayList<>();
        for (StrategyEvaluation other : sorted.subList(1, sorted.size())) {
            tradeoffs.add(compare(best, other));
        }
        Set<StrategyType> winners = new LinkedHashSet<>();
        winners.add(best.strategy());
        sensitivity.forEach(s -> { if (s.winner() != null) winners.add(s.winner()); });
        boolean sensitive = winners.size() > 1;
        String headline = best.strategy().getDisplayName() + " has the lowest aggregate recovery cost ("
                + fmt(best.metrics().aggregateScore()) + ") under the current weights"
                + (sensitive ? ", but other weightings favour " + String.join(" or ", winners.stream()
                .filter(w -> w != best.strategy()).map(t -> t.getDisplayName().toLowerCase(java.util.Locale.ROOT)).toList()) + "."
                : "; it also wins under every alternative weighting tested.");
        return new Recommendation(best.strategy(), headline, tradeoffs, caveat, sensitive);
    }

    public List<SensitivityProfile> sensitivity(List<StrategyEvaluation> evaluations,
                                                Function<ScoreWeights, Map<StrategyType, Double>> rescore) {
        List<SensitivityProfile> result = new ArrayList<>();
        for (String name : PROFILE_ORDER) {
            ScoreWeights w = PROFILES.get(name);
            Map<StrategyType, Double> scores = rescore.apply(w);
            StrategyType winner = sort(evaluations, e -> scores.get(e.strategy())).stream()
                    .filter(StrategyRanker::eligible)
                    .filter(e -> e.status() != RecoveryStatus.NOT_REQUIRED)
                    .map(StrategyEvaluation::strategy).findFirst().orElse(null);
            result.add(new SensitivityProfile(name, w, winner, new EnumMap<>(scores)));
        }
        return result;
    }

    private static List<StrategyEvaluation> sort(List<StrategyEvaluation> evaluations,
                                                 Function<StrategyEvaluation, Double> score) {
        return evaluations.stream().sorted(Comparator
                .comparing((StrategyEvaluation e) -> !eligible(e))
                .thenComparing(score::apply)
                .thenComparingInt(e -> e.metrics().workLost())
                .thenComparingInt(e -> e.metrics().processesTerminated())
                .thenComparing(StrategyEvaluation::strategy)).toList();
    }

    static boolean eligible(StrategyEvaluation e) {
        return e.status() == RecoveryStatus.RECOVERED || e.status() == RecoveryStatus.NOT_REQUIRED;
    }

    private String explain(StrategyEvaluation e) {
        RecoveryMetrics m = e.metrics();
        if (!eligible(e)) {
            return "Not eligible: status " + e.status() + " (" + e.haltReason() + "), "
                    + e.verification().remainingCycles().size() + " cycle(s) remain.";
        }
        var top = m.scoreBreakdown().stream().max(Comparator.comparingDouble(c -> c.contribution())).orElseThrow();
        return "Score " + fmt(m.aggregateScore()) + "; largest cost component: " + top.metric() + " ("
                + fmt(top.contribution()) + "). Lost " + m.workLost() + " work units, terminated "
                + m.processesTerminated() + ", rolled back " + m.processesRolledBack() + " in "
                + m.recoverySteps() + " round(s).";
    }

    private String compare(StrategyEvaluation best, StrategyEvaluation other) {
        RecoveryMetrics a = best.metrics();
        RecoveryMetrics b = other.metrics();
        if (!eligible(other)) {
            return other.strategy().getDisplayName() + " did not reach a verified deadlock-free state ("
                    + other.status() + ").";
        }
        List<String> better = new ArrayList<>();
        List<String> worse = new ArrayList<>();
        diff("work lost", a.workLost(), b.workLost(), better, worse);
        diff("processes terminated", a.processesTerminated(), b.processesTerminated(), better, worse);
        diff("rollbacks", a.processesRolledBack(), b.processesRolledBack(), better, worse);
        diff("recovery rounds", a.recoverySteps(), b.recoverySteps(), better, worse);
        diff("starvation risk", a.starvationRisk(), b.starvationRisk(), better, worse);
        String s = "vs " + other.strategy().getDisplayName() + " (score " + fmt(b.aggregateScore()) + "): ";
        s += better.isEmpty() ? "no metric is strictly better" : "better on " + String.join(", ", better);
        if (!worse.isEmpty()) {
            s += "; worse on " + String.join(", ", worse);
        }
        return s + ".";
    }

    private static void diff(String name, int mine, int theirs, List<String> better, List<String> worse) {
        if (mine < theirs) {
            better.add(name + " (" + mine + " vs " + theirs + ")");
        } else if (mine > theirs) {
            worse.add(name + " (" + mine + " vs " + theirs + ")");
        }
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.3f", v);
    }
}
