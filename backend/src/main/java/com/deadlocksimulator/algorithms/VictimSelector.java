package com.deadlocksimulator.algorithms;

import com.deadlocksimulator.models.VictimWeights;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Multi-criteria victim selection. See {@link VictimWeights} for the cost convention.
 *
 * <p>Relation to the case study: the case study combines work performed, resources held,
 * priority and previous victim count in a weighted expression, but as written the victim-history
 * term carries a sign that would make a frequently chosen process <em>cheaper</em> to choose again
 * when the minimum cost is selected — contradicting the stated anti-starvation intent. This
 * implementation minimises cost and <em>adds</em> the history term, so every previous selection
 * makes a process less attractive. Inputs are min-max normalised so that, e.g., work units (100s)
 * cannot drown out victim counts (0-3). This is a heuristic, not an optimal policy.
 *
 * <p>Ties are broken deterministically: lower work at risk, then lower priority, then natural id order.
 */
@Component
public class VictimSelector {

    /** Raw factor values for one candidate. {@code workAtRisk} is the work this victim would lose. */
    public record Candidate(String processId, int workAtRisk, int priority, int resourcesHeld, int victimCount) {
    }

    /** Cost breakdown for one candidate (terms already multiplied by their weights). */
    public record ScoredCandidate(Candidate candidate, double cost, double workTerm, double priorityTerm,
                                  double resourceTerm, double historyTerm) {
    }

    /** Returns candidates sorted from most to least preferred victim. */
    public List<ScoredCandidate> rank(List<Candidate> candidates, VictimWeights w) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        Normaliser work = Normaliser.of(candidates, Candidate::workAtRisk);
        Normaliser prio = Normaliser.of(candidates, Candidate::priority);
        Normaliser res = Normaliser.of(candidates, Candidate::resourcesHeld);
        Normaliser hist = Normaliser.of(candidates, Candidate::victimCount);

        List<ScoredCandidate> scored = new ArrayList<>();
        for (Candidate c : candidates) {
            double wt = w.work() * work.apply(c.workAtRisk());
            double pt = w.priority() * prio.apply(c.priority());
            double rt = w.resources() * (1.0 - res.apply(c.resourcesHeld()));
            double ht = w.history() * hist.apply(c.victimCount());
            scored.add(new ScoredCandidate(c, round(wt + pt + rt + ht), round(wt), round(pt), round(rt), round(ht)));
        }
        scored.sort(Comparator.comparingDouble(ScoredCandidate::cost)
                .thenComparingInt(s -> s.candidate().workAtRisk())
                .thenComparingInt(s -> s.candidate().priority())
                .thenComparing(s -> s.candidate().processId(), ProcessIdComparator.INSTANCE));
        return scored;
    }

    public ScoredCandidate select(List<Candidate> candidates, VictimWeights w) {
        List<ScoredCandidate> ranked = rank(candidates, w);
        if (ranked.isEmpty()) {
            throw new IllegalArgumentException("No victim candidates");
        }
        return ranked.get(0);
    }

    private static double round(double v) {
        return Math.round(v * 1_000_000d) / 1_000_000d;
    }

    /** Min-max normalisation to [0,1]; degenerate range maps to 0. */
    private record Normaliser(int min, int max) {
        static Normaliser of(List<Candidate> cs, ToIntFunction<Candidate> f) {
            int min = cs.stream().mapToInt(f).min().orElse(0);
            int max = cs.stream().mapToInt(f).max().orElse(0);
            return new Normaliser(min, max);
        }

        double apply(int value) {
            return max == min ? 0.0 : (value - min) / (double) (max - min);
        }
    }
}
