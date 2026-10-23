package com.deadlocksimulator.algorithms;

import com.deadlocksimulator.algorithms.VictimSelector.Candidate;
import com.deadlocksimulator.algorithms.VictimSelector.ScoredCandidate;
import com.deadlocksimulator.models.VictimWeights;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class VictimSelectorTest {

    private final VictimSelector selector = new VictimSelector();
    private static final VictimWeights WORK_ONLY = new VictimWeights(1, 0, 0, 0);
    private static final VictimWeights PRIORITY_ONLY = new VictimWeights(0, 1, 0, 0);
    private static final VictimWeights RESOURCES_ONLY = new VictimWeights(0, 0, 1, 0);
    private static final VictimWeights HISTORY_ONLY = new VictimWeights(0, 0, 0, 1);

    @Test
    void lessWorkAtRiskIsPreferred() {
        var chosen = selector.select(List.of(new Candidate("P1", 90, 5, 1, 0), new Candidate("P2", 10, 5, 1, 0)), WORK_ONLY);
        assertThat(chosen.candidate().processId()).isEqualTo("P2");
        assertThat(chosen.cost()).isZero();
    }

    @Test
    void lowerPriorityIsPreferred() {
        var chosen = selector.select(List.of(new Candidate("P1", 50, 9, 1, 0), new Candidate("P2", 50, 2, 1, 0)), PRIORITY_ONLY);
        assertThat(chosen.candidate().processId()).isEqualTo("P2");
    }

    @Test
    void holdingMoreResourcesMakesAProcessCheaperBecauseMoreIsReclaimed() {
        var chosen = selector.select(List.of(new Candidate("P1", 50, 5, 1, 0), new Candidate("P2", 50, 5, 4, 0)), RESOURCES_ONLY);
        assertThat(chosen.candidate().processId()).isEqualTo("P2");
    }

    @Test
    void previousVictimsBecomeLessAttractiveNotMore() {
        var chosen = selector.select(List.of(new Candidate("P1", 50, 5, 1, 3), new Candidate("P2", 50, 5, 1, 0)), HISTORY_ONLY);
        assertThat(chosen.candidate().processId()).isEqualTo("P2");
        List<ScoredCandidate> ranked = selector.rank(List.of(new Candidate("P1", 50, 5, 1, 3),
                new Candidate("P2", 50, 5, 1, 0)), HISTORY_ONLY);
        assertThat(ranked.get(1).historyTerm()).isEqualTo(1.0);
    }

    @Test
    void historyCanOutweighWorkWhenWeightedSo() {
        List<Candidate> cs = List.of(new Candidate("P1", 10, 5, 1, 2), new Candidate("P2", 30, 5, 1, 0));
        assertThat(selector.select(cs, new VictimWeights(0.4, 0, 0, 0.2)).candidate().processId()).isEqualTo("P1");
        assertThat(selector.select(cs, new VictimWeights(0.4, 0, 0, 0.6)).candidate().processId()).isEqualTo("P2");
    }

    @Test
    void inputsAreMinMaxNormalisedSoScalesDoNotDominate() {
        // Work differs by 1000 units, history by 1. With equal weights both terms are worth at most 1 * weight.
        List<ScoredCandidate> ranked = selector.rank(List.of(new Candidate("P1", 1000, 5, 1, 0),
                new Candidate("P2", 0, 5, 1, 1)), new VictimWeights(0.5, 0, 0, 0.5));
        assertThat(ranked).allSatisfy(s -> assertThat(s.cost()).isCloseTo(0.5, within(1e-9)));
    }

    @Test
    void combinedCostMatchesHandComputation() {
        List<Candidate> cs = List.of(
                new Candidate("P1", 30, 5, 1, 0),
                new Candidate("P2", 20, 8, 2, 1),
                new Candidate("P3", 10, 2, 3, 2));
        List<ScoredCandidate> ranked = selector.rank(cs, VictimWeights.defaults());
        // P1: work 1.0*.4 + prio (5-2)/6*.2=0.1 + res (1-0)*.2 + hist 0           = 0.7
        // P2: work .5*.4=.2 + prio 1*.2 + res (1-.5)*.2=.1 + hist .5*.2=.1        = 0.6
        // P3: work 0 + prio 0 + res 0 + hist 1*.2                                 = 0.2
        assertThat(ranked).extracting(s -> s.candidate().processId()).containsExactly("P3", "P2", "P1");
        assertThat(ranked).extracting(ScoredCandidate::cost).containsExactly(0.2, 0.6, 0.7);
    }

    @Test
    void tiesAreBrokenDeterministicallyByNaturalIdOrder() {
        List<Candidate> cs = List.of(new Candidate("P10", 5, 5, 1, 0), new Candidate("P2", 5, 5, 1, 0));
        assertThat(selector.select(cs, VictimWeights.defaults()).candidate().processId()).isEqualTo("P2");
    }
}
