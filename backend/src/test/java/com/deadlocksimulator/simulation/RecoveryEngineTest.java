package com.deadlocksimulator.simulation;

import com.deadlocksimulator.TestSupport;
import com.deadlocksimulator.enums.RecoveryStatus;
import com.deadlocksimulator.enums.StrategyType;
import com.deadlocksimulator.enums.WorkDistribution;
import com.deadlocksimulator.models.ComparisonReport;
import com.deadlocksimulator.models.GeneratorParams;
import com.deadlocksimulator.models.RecoveryContext;
import com.deadlocksimulator.models.ScoreWeights;
import com.deadlocksimulator.models.StrategyEvaluation;
import com.deadlocksimulator.models.SystemState;
import com.deadlocksimulator.models.VictimWeights;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RecoveryEngineTest {

    private static ComparisonReport compare(SystemState s) {
        return TestSupport.ENGINE.compare(s, RecoveryContext.defaults(s), ScoreWeights.defaults());
    }

    @Test
    void allStrategiesRecoverEveryDeadlockingTemplateAndAreVerified() {
        for (var t : new ScenarioTemplates().all()) {
            if (t.key().equals("no-deadlock")) {
                continue;
            }
            ComparisonReport r = compare(TestSupport.simulate(t.config()));
            assertThat(r.snapshotUnchanged()).as(t.key()).isTrue();
            assertThat(r.copiesIdentical()).as(t.key()).isTrue();
            assertThat(r.evaluations()).as(t.key()).hasSize(3).allSatisfy(e -> {
                assertThat(e.status()).isEqualTo(RecoveryStatus.RECOVERED);
                assertThat(e.verification().verified()).isTrue();
            });
            assertThat(r.ranking()).extracting(ComparisonReport.RankingEntry::rank).containsExactly(1, 2, 3);
        }
    }

    @Test
    void classicScenarioRankingAndWeightSensitivity() {
        ComparisonReport r = compare(TestSupport.simulate(ScenarioTemplates.classicThreeCycle().config()));
        assertThat(r.ranking()).extracting(ComparisonReport.RankingEntry::strategy)
                .containsExactly(StrategyType.TERMINATION, StrategyType.ROLLBACK, StrategyType.TOTAL_ABORT);
        assertThat(r.recommendation().strategy()).isEqualTo(StrategyType.TERMINATION);
        // Under "Process survival" weights rollback (no termination) must win instead.
        var survival = r.sensitivity().stream().filter(p -> p.name().equals("Process survival")).findFirst().orElseThrow();
        assertThat(survival.winner()).isEqualTo(StrategyType.ROLLBACK);
        assertThat(r.recommendation().weightSensitive()).isTrue();
        assertThat(r.recommendation().caveat()).contains("conditional");
    }

    @Test
    void noDeadlockMeansNoRecoveryRequired() {
        ComparisonReport r = compare(TestSupport.simulate(ScenarioTemplates.noDeadlock().config()));
        assertThat(r.evaluations()).allMatch(e -> e.status() == RecoveryStatus.NOT_REQUIRED);
        assertThat(r.recommendation().strategy()).isNull();
    }

    @Test
    void safetyLimitProducesPartialStatusAndIneligibleRanking() {
        SystemState s = TestSupport.simulate(ScenarioTemplates.twoCyclesWithDependent().config());
        ComparisonReport r = TestSupport.ENGINE.compare(s, new RecoveryContext(VictimWeights.defaults(), 1),
                ScoreWeights.defaults());
        StrategyEvaluation term = r.evaluations().stream().filter(e -> e.strategy() == StrategyType.TERMINATION)
                .findFirst().orElseThrow();
        assertThat(term.status()).isEqualTo(RecoveryStatus.PARTIAL);
        assertThat(term.verification().remainingCycles()).hasSize(1);
        // total abort needs one round, so it is the only verified strategy and ranks first.
        assertThat(r.ranking().get(0).strategy()).isEqualTo(StrategyType.TOTAL_ABORT);
        assertThat(r.ranking()).filteredOn(e -> e.strategy() == StrategyType.TERMINATION)
                .allMatch(e -> !e.eligible() && e.rank() > 1);
    }

    @Test
    void noStrategyWinsEverywhereAcrossGeneratedWorkloads() {
        // Research hypothesis check: vary checkpoint interval, work distribution and density.
        Set<StrategyType> winners = new HashSet<>();
        int[] intervals = {0, 5, 25};
        for (int interval : intervals) {
            for (WorkDistribution wd : WorkDistribution.values()) {
                for (double density : new double[]{0.0, 1.0}) {
                    for (long seed = 1; seed <= 3; seed++) {
                        GeneratorParams g = new GeneratorParams(seed, 1, 4, 1, 0, wd, 10, 150, interval, density, 2,
                                false, 2);
                        ComparisonReport r = compare(TestSupport.simulate(new ScenarioGenerator().generate(g)));
                        assertThat(r.evaluations()).allMatch(e -> e.status() == RecoveryStatus.RECOVERED);
                        winners.add(r.recommendation().strategy());
                    }
                }
            }
        }
        assertThat(winners).hasSizeGreaterThanOrEqualTo(2);
    }
}
