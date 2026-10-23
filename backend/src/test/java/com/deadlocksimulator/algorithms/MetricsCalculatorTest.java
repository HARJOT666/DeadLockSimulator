package com.deadlocksimulator.algorithms;

import com.deadlocksimulator.TestSupport;
import com.deadlocksimulator.models.RecoveryContext;
import com.deadlocksimulator.models.RecoveryMetrics;
import com.deadlocksimulator.models.RecoveryMetrics.ScoreComponent;
import com.deadlocksimulator.models.RecoveryOutcome;
import com.deadlocksimulator.models.ScoreWeights;
import com.deadlocksimulator.models.SystemState;
import com.deadlocksimulator.simulation.ScenarioBuilder;
import com.deadlocksimulator.simulation.ScenarioTemplates;
import com.deadlocksimulator.strategies.RecoveryStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MetricsCalculatorTest {

    private static RecoveryMetrics metrics(RecoveryStrategy strategy, SystemState snapshot, ScoreWeights w) {
        var norm = MetricsCalculator.Normalisers.of(snapshot, TestSupport.detect(snapshot));
        SystemState copy = TestSupport.CLONER.deepCopy(snapshot);
        RecoveryOutcome o = strategy.recover(copy, RecoveryContext.defaults(copy));
        return TestSupport.METRICS.calculate(o, norm, w);
    }

    @Test
    void classicScenarioMetricsMatchHandComputation() {
        SystemState snap = TestSupport.simulate(ScenarioTemplates.classicThreeCycle().config());
        ScoreWeights w = ScoreWeights.defaults();

        RecoveryMetrics t = metrics(TestSupport.TERMINATION, snap, w);
        assertThat(t.workLost()).isEqualTo(10);
        assertThat(t.processesTerminated()).isEqualTo(1);
        assertThat(t.processesRolledBack()).isZero();
        assertThat(t.resourcesReclaimed()).isEqualTo(1);
        assertThat(t.recoverySteps()).isEqualTo(1);
        assertThat(t.starvationRisk()).isZero();
        // .35*(10/60) + .25*(1/3) + .10*(1/3) = 0.058333 + 0.083333 + 0.033333
        assertThat(t.aggregateScore()).isCloseTo(0.175, within(1e-5));

        RecoveryMetrics r = metrics(TestSupport.ROLLBACK, snap, w);
        assertThat(r.workLost()).isEqualTo(10);
        assertThat(r.processesTerminated()).isZero();
        assertThat(r.processesRolledBack()).isEqualTo(1);
        assertThat(r.starvationRisk()).isEqualTo(1);        // rolled-back P3 survives with victim count 1
        // .35/6 + .10/3 (rollback) + .10/3 (steps) + .20/3 (starvation)
        assertThat(r.aggregateScore()).isCloseTo(0.191667, within(1e-5));

        RecoveryMetrics a = metrics(TestSupport.TOTAL_ABORT, snap, w);
        assertThat(a.workLost()).isEqualTo(60);
        assertThat(a.processesTerminated()).isEqualTo(3);
        assertThat(a.resourcesReclaimed()).isEqualTo(3);
        assertThat(a.recoverySteps()).isEqualTo(1);
        assertThat(a.recoveryActions()).isEqualTo(3);
        assertThat(a.starvationRisk()).isZero();
        assertThat(a.aggregateScore()).isCloseTo(0.633333, within(1e-5));
    }

    @Test
    void breakdownContributionsSumToTheScoreAndUseIntrinsicNormalisers() {
        SystemState snap = TestSupport.simulate(ScenarioTemplates.twoCyclesWithDependent().config());
        RecoveryMetrics m = metrics(TestSupport.TOTAL_ABORT, snap, ScoreWeights.defaults());
        double sum = m.scoreBreakdown().stream().mapToDouble(ScoreComponent::contribution).sum();
        assertThat(sum).isCloseTo(m.aggregateScore(), within(1e-5));
        ScoreComponent work = m.scoreBreakdown().get(0);
        assertThat(work.metric()).isEqualTo("workLost");
        // Live work in snapshot: P1 45 + P2 60 + P3 35 + P4 50 + P5 65 + P6 25 = 280 (P7 completed).
        assertThat(work.normaliser()).isEqualTo(280);
        assertThat(work.rawValue()).isEqualTo(255);
    }

    @Test
    void starvationRiskIncludesVictimHistoryCarriedIntoTheScenario() {
        SystemState snap = TestSupport.simulate(new ScenarioBuilder()
                .process("P1", 5, 100, 0, 0).process("P2", 5, 100, 0, 2).process("P3", 5, 100, 0, 0)
                .resources("R1", "R2", "R3").checkpointInterval(10)
                .request("P1", "R1").request("P2", "R2").work("P1", 50).work("P2", 5)
                .request("P3", "R3").request("P1", "R2").request("P2", "R1").build());
        RecoveryMetrics t = metrics(TestSupport.TERMINATION, snap, ScoreWeights.defaults());
        // P2 has less work but a history of 2; with default weights P2 still costs less (.2*... ) — either
        // way the surviving maximum must reflect the carried-over history of survivors.
        SystemState copy = TestSupport.CLONER.deepCopy(snap);
        TestSupport.TERMINATION.recover(copy, RecoveryContext.defaults(copy));
        int expected = copy.getProcesses().values().stream()
                .filter(p -> p.getState() != com.deadlocksimulator.enums.ProcessState.TERMINATED)
                .mapToInt(p -> p.getVictimCount()).max().orElse(0);
        assertThat(t.starvationRisk()).isEqualTo(expected);
        assertThat(snap.process("P2").getVictimCount()).isEqualTo(2);
    }

    @Test
    void zeroWeightsGiveAZeroScoreInsteadOfDividingByZero() {
        SystemState snap = TestSupport.simulate(ScenarioTemplates.classicThreeCycle().config());
        RecoveryMetrics m = metrics(TestSupport.TOTAL_ABORT, snap, new ScoreWeights(0, 0, 0, 0, 0, 0, 3));
        assertThat(m.aggregateScore()).isZero();
    }

    @Test
    void weightsOnlyMatterThroughTheirRatios() {
        SystemState snap = TestSupport.simulate(ScenarioTemplates.overlappingCycles().config());
        double a = metrics(TestSupport.ROLLBACK, snap, ScoreWeights.defaults()).aggregateScore();
        double b = metrics(TestSupport.ROLLBACK, snap, new ScoreWeights(3.5, 2.5, 1.0, 0, 1.0, 2.0, 3)).aggregateScore();
        assertThat(a).isCloseTo(b, within(1e-5));
    }
}
