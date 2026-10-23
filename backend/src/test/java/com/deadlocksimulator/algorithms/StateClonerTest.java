package com.deadlocksimulator.algorithms;

import com.deadlocksimulator.TestSupport;
import com.deadlocksimulator.enums.ProcessState;
import com.deadlocksimulator.models.Checkpoint;
import com.deadlocksimulator.models.Operation;
import com.deadlocksimulator.models.Process;
import com.deadlocksimulator.models.SystemState;
import com.deadlocksimulator.models.VictimRecord;
import com.deadlocksimulator.simulation.ScenarioTemplates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StateClonerTest {

    private SystemState original;
    private String originalFingerprint;

    @BeforeEach
    void setUp() {
        original = TestSupport.simulate(ScenarioTemplates.twoCyclesWithDependent().config());
        originalFingerprint = TestSupport.ENGINE.fingerprint(original);
    }

    @Test
    void copyIsEqualInContentButSharesNoMutableObject() {
        SystemState copy = TestSupport.CLONER.deepCopy(original);
        assertThat(TestSupport.ENGINE.fingerprint(copy)).isEqualTo(originalFingerprint);
        assertThat(copy.getProcesses()).isNotSameAs(original.getProcesses());
        assertThat(copy.getResources()).isNotSameAs(original.getResources());
        assertThat(copy.getEvents()).isNotSameAs(original.getEvents());
        assertThat(copy.getVictimHistory()).isNotSameAs(original.getVictimHistory());
        for (String id : original.getProcesses().keySet()) {
            Process a = original.process(id);
            Process b = copy.process(id);
            assertThat(b).isNotSameAs(a);
            assertThat(b.getHeldResources()).isNotSameAs(a.getHeldResources());
            assertThat(b.getWaitingFor()).isNotSameAs(a.getWaitingFor());
            assertThat(b.getCheckpoints()).isNotSameAs(a.getCheckpoints());
            assertThat(b.getDeferredOperations()).isNotSameAs(a.getDeferredOperations());
        }
        for (String id : original.getResources().keySet()) {
            assertThat(copy.resource(id)).isNotSameAs(original.resource(id));
            assertThat(copy.resource(id).getWaitQueue()).isNotSameAs(original.resource(id).getWaitQueue());
        }
    }

    @Test
    void mutatingOneCopyLeavesTheSnapshotAndOtherCopiesUnchanged() {
        SystemState a = TestSupport.CLONER.deepCopy(original);
        SystemState b = TestSupport.CLONER.deepCopy(original);
        SystemState c = TestSupport.CLONER.deepCopy(original);

        // Mutate every kind of mutable component of copy A directly...
        Process p1 = a.process("P1");
        p1.setWorkDone(0);
        p1.setVictimCount(9);
        p1.getHeldResources().add("R6");
        p1.getWaitingFor().clear();
        p1.getCheckpoints().add(new Checkpoint(99, 1, List.of("R1"), 1));
        p1.getDeferredOperations().add(Operation.work("P1", 1));
        a.resource("R1").getWaitQueue().clear();
        a.resource("R2").setHolder(null);
        a.getEvents().clear();
        a.getVictimHistory().add(new VictimRecord(1, "TERMINATE", "P1", 0.5));
        // ...and run real recovery operations on copy B.
        b.terminate("P2");
        b.terminateAll(List.of("P3", "P4", "P5"));

        assertThat(TestSupport.ENGINE.fingerprint(original)).isEqualTo(originalFingerprint);
        assertThat(TestSupport.ENGINE.fingerprint(c)).isEqualTo(originalFingerprint);
        assertThat(original.process("P2").getState()).isEqualTo(ProcessState.WAITING);
        assertThat(c.process("P1").getHeldResources()).containsExactly("R1");
        assertThat(c.resource("R1").getWaitQueue()).containsExactly("P2", "P6");
        assertThat(TestSupport.CONSISTENCY.check(c)).isEmpty();
    }

    @Test
    void allThreeStrategiesStartFromIdenticalData() {
        var report = TestSupport.ENGINE.compare(original,
                com.deadlocksimulator.models.RecoveryContext.defaults(original),
                com.deadlocksimulator.models.ScoreWeights.defaults());
        assertThat(report.copiesIdentical()).isTrue();
        assertThat(report.snapshotUnchanged()).isTrue();
        assertThat(report.snapshotFingerprint()).isEqualTo(originalFingerprint);
        assertThat(TestSupport.ENGINE.fingerprint(original)).isEqualTo(originalFingerprint);
    }

    @Test
    void jsonRoundTripPreservesTheStateExactly() throws Exception {
        String json = TestSupport.MAPPER.writeValueAsString(original);
        SystemState restored = TestSupport.MAPPER.readValue(json, SystemState.class);
        assertThat(TestSupport.ENGINE.fingerprint(restored)).isEqualTo(originalFingerprint);
    }
}
