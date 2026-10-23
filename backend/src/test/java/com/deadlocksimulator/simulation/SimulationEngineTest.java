package com.deadlocksimulator.simulation;

import com.deadlocksimulator.TestSupport;
import com.deadlocksimulator.enums.OperationType;
import com.deadlocksimulator.enums.ProcessState;
import com.deadlocksimulator.exceptions.InvalidScenarioException;
import com.deadlocksimulator.models.Checkpoint;
import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.GeneratorParams;
import com.deadlocksimulator.models.ScenarioConfig;
import com.deadlocksimulator.models.SystemState;
import com.deadlocksimulator.enums.WorkDistribution;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulationEngineTest {

    @Test
    void classicScenarioReachesTheDocumentedDeadlock() {
        SystemState s = TestSupport.simulate(ScenarioTemplates.classicThreeCycle().config());
        assertThat(s.getProcesses().values()).allMatch(p -> p.getState() == ProcessState.WAITING);
        assertThat(s.process("P1").getHeldResources()).containsExactly("R1");
        assertThat(s.process("P1").getWaitingFor()).containsExactly("R2");
        assertThat(s.resource("R1").getWaitQueue()).containsExactly("P3");
        assertThat(s.process("P1").getWorkDone()).isEqualTo(30);
        DetectionResult d = TestSupport.detect(s);
        assertThat(d.deadlocked()).isTrue();
        assertThat(d.cycles().get(0).processes()).containsExactly("P1", "P2", "P3");
        assertThat(TestSupport.CONSISTENCY.check(s)).isEmpty();
    }

    @Test
    void checkpointsAreTakenAtIntervalMultiplesWithTheResourcesHeldAtThatTime() {
        SystemState s = TestSupport.simulate(ScenarioTemplates.rollbackFriendly().config());
        // P1: one WORK 80 (no resources) -> one checkpoint at the highest multiple reached (80);
        // then acquire R1, work 4 -> 84 (no new multiple crossed).
        List<Checkpoint> cps = s.process("P1").getCheckpoints();
        assertThat(cps).extracting(Checkpoint::workDone).containsExactly(0, 80);
        assertThat(cps).allMatch(c -> c.heldResources().isEmpty());
        // P4: work 100 then R4 then work 8.
        assertThat(s.process("P4").getCheckpoints()).last().extracting(Checkpoint::workDone).isEqualTo(100);
        // Classic: P1 acquires R1 before WORK 30, so its checkpoint at 30 includes R1.
        SystemState c = TestSupport.simulate(ScenarioTemplates.classicThreeCycle().config());
        assertThat(c.process("P1").getCheckpoints()).extracting(Checkpoint::workDone).containsExactly(0, 30);
        assertThat(c.process("P1").getCheckpoints()).extracting(Checkpoint::heldResources)
                .containsExactly(List.of(), List.of("R1"));
    }

    @Test
    void separateWorkOperationsEachCrossingAMultipleProduceSeparateCheckpoints() {
        SystemState s = TestSupport.simulate(new ScenarioBuilder().process("P1", 1, 100).resources("R1")
                .checkpointInterval(10).work("P1", 12).request("P1", "R1").work("P1", 5).work("P1", 4)
                .work("P1", 30).build());
        assertThat(s.process("P1").getCheckpoints()).extracting(Checkpoint::workDone).containsExactly(0, 10, 20, 50);
        assertThat(s.process("P1").getCheckpoints()).extracting(Checkpoint::heldResources)
                .containsExactly(List.of(), List.of(), List.of("R1"), List.of("R1"));
    }

    @Test
    void operationsOfABlockedProcessAreDeferredAndRunWhenItIsUnblocked() {
        SystemState s = TestSupport.simulate(ScenarioTemplates.noDeadlock().config());
        assertThat(s.getProcesses().values()).allMatch(p -> p.getState() == ProcessState.COMPLETED);
        assertThat(s.process("P2").getWorkDone()).isEqualTo(30);
        assertThat(s.getResources().values()).allMatch(r -> r.getHolder() == null && r.getWaitQueue().isEmpty());
        assertThat(s.getEvents()).anyMatch(e -> e.type().equals("DEFER") && "P2".equals(e.processId()));
        assertThat(TestSupport.detect(s).deadlocked()).isFalse();
    }

    @Test
    void deadlockedProcessesKeepTheirRemainingProgramDeferred() {
        ScenarioConfig cfg = new ScenarioBuilder().process("P1", 1, 50).process("P2", 1, 50)
                .resources("R1", "R2")
                .request("P1", "R1").request("P2", "R2").request("P1", "R2").request("P2", "R1")
                .work("P1", 10).finish("P1").build();
        SystemState s = TestSupport.simulate(cfg);
        assertThat(s.process("P1").getDeferredOperations()).extracting(o -> o.type())
                .containsExactly(OperationType.WORK, OperationType.FINISH);
        assertThat(s.process("P1").getWorkDone()).isZero();
    }

    @Test
    void sameScenarioAlwaysProducesTheSameState() {
        ScenarioConfig cfg = ScenarioTemplates.twoCyclesWithDependent().config();
        String a = TestSupport.ENGINE.fingerprint(TestSupport.simulate(cfg));
        String b = TestSupport.ENGINE.fingerprint(TestSupport.simulate(cfg));
        assertThat(a).isEqualTo(b);
    }

    @Test
    void workCounterIsCappedAtTotalWork() {
        SystemState s = TestSupport.simulate(new ScenarioBuilder().process("P1", 1, 25).resources("R1")
                .work("P1", 100).build());
        assertThat(s.process("P1").getWorkDone()).isEqualTo(25);
    }

    @Test
    void releasingAResourceThatIsNotHeldIsRejectedWithItsStep() {
        ScenarioConfig cfg = new ScenarioBuilder().process("P1", 1, 10).resources("R1").release("P1", "R1").build();
        assertThatThrownBy(() -> TestSupport.simulate(cfg)).isInstanceOf(InvalidScenarioException.class)
                .hasMessageContaining("Step 1");
    }

    @Test
    void requestingAnAlreadyHeldResourceIsRejected() {
        ScenarioConfig cfg = new ScenarioBuilder().process("P1", 1, 10).resources("R1")
                .request("P1", "R1").request("P1", "R1").build();
        assertThatThrownBy(() -> TestSupport.simulate(cfg)).isInstanceOf(InvalidScenarioException.class)
                .hasMessageContaining("already holds");
    }

    @Test
    void validatorReportsAllStructuralProblems() {
        ScenarioConfig cfg = new ScenarioBuilder().process("P1", 1, 10).process("P1", 1, 10)
                .resources("R1", "R1").allocate("R9", "P1").request("P7", "R1").work("P1", 0).build();
        assertThatThrownBy(() -> TestSupport.simulate(cfg)).isInstanceOf(InvalidScenarioException.class)
                .satisfies(e -> assertThat(((InvalidScenarioException) e).getProblems()).hasSizeGreaterThanOrEqualTo(5));
    }

    @Test
    void initialAllocationsAreHeldAfterTheInitialCheckpoint() {
        SystemState s = TestSupport.simulate(new ScenarioBuilder().process("P1", 1, 10).resources("R1")
                .allocate("R1", "P1").work("P1", 1).build());
        assertThat(s.process("P1").getHeldResources()).containsExactly("R1");
        assertThat(s.process("P1").getCheckpoints().get(0).heldResources()).isEmpty();
    }

    @Test
    void generatorIsDeterministicAndProducesTheRequestedDeadlockStructure() {
        GeneratorParams g = new GeneratorParams(7L, 2, 4, 2, 1, WorkDistribution.UNIFORM, 10, 90, 10, 0.5, 2,
                false, 1);
        ScenarioGenerator gen = new ScenarioGenerator();
        ScenarioConfig a = gen.generate(g);
        ScenarioConfig b = gen.generate(g);
        assertThat(a).isEqualTo(b);
        SystemState s = TestSupport.simulate(a);
        DetectionResult d = TestSupport.detect(s);
        assertThat(d.cycles()).hasSize(2).allMatch(c -> c.length() == 4);
        assertThat(d.deadlockedProcesses()).hasSize(8);
        assertThat(d.blockedProcesses()).hasSize(2);
        assertThat(s.process("P11").getState()).isEqualTo(ProcessState.COMPLETED);
        assertThat(TestSupport.CONSISTENCY.check(s)).isEmpty();
    }

    @Test
    void generatorOverlapOptionCreatesOverlappingCycles() {
        GeneratorParams g = new GeneratorParams(3L, 1, 4, 0, 0, WorkDistribution.EQUAL, 30, 30, 10, 0.0, 0,
                true, 0);
        DetectionResult d = TestSupport.detect(TestSupport.simulate(new ScenarioGenerator().generate(g)));
        assertThat(d.deadlockedProcesses()).containsExactly("P1", "P2", "P3", "P4");
        assertThat(d.vertexCount()).isEqualTo(4);
        assertThat(d.edgeCount()).isEqualTo(5); // ring (4) + extra AND-request edge
    }
}
