package com.deadlocksimulator.strategies;

import com.deadlocksimulator.TestSupport;
import com.deadlocksimulator.enums.HaltReason;
import com.deadlocksimulator.enums.ProcessState;
import com.deadlocksimulator.enums.RecoveryActionType;
import com.deadlocksimulator.models.RecoveryAction;
import com.deadlocksimulator.models.RecoveryContext;
import com.deadlocksimulator.models.RecoveryOutcome;
import com.deadlocksimulator.models.SystemState;
import com.deadlocksimulator.models.VerificationResult;
import com.deadlocksimulator.models.VictimWeights;
import com.deadlocksimulator.simulation.ScenarioTemplates;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecoveryStrategiesTest {

    private static SystemState deadlocked(ScenarioTemplates.Template t) {
        return TestSupport.simulate(t.config());
    }

    private static RecoveryOutcome run(RecoveryStrategy strategy, SystemState s) {
        return strategy.recover(s, RecoveryContext.defaults(s));
    }

    private static void assertVerifiedAndConsistent(RecoveryOutcome o) {
        VerificationResult v = TestSupport.VERIFIER.verify(o.finalState());
        assertThat(v.violations()).isEmpty();
        assertThat(v.deadlockFree()).isTrue();
        assertThat(o.haltReason()).isEqualTo(HaltReason.DEADLOCK_RESOLVED);
    }

    @Nested
    class Termination {

        @Test
        void terminatesTheCheapestVictimAndHandsItsResourceToTheNextWaiter() {
            RecoveryOutcome o = run(TestSupport.TERMINATION, deadlocked(ScenarioTemplates.classicThreeCycle()));
            assertVerifiedAndConsistent(o);
            assertThat(o.rounds()).hasSize(1);
            RecoveryAction a = o.actions().get(0);
            assertThat(a.processId()).isEqualTo("P3");               // least work (10)
            assertThat(a.workLost()).isEqualTo(10);
            assertThat(a.resourcesReleased()).containsExactly("R3");
            assertThat(a.handovers()).containsExactly("R3 -> P2");
            SystemState s = o.finalState();
            assertThat(s.process("P3").getState()).isEqualTo(ProcessState.TERMINATED);
            assertThat(s.process("P3").getHeldResources()).isEmpty();
            assertThat(s.process("P2").getState()).isEqualTo(ProcessState.RUNNING);
            assertThat(s.process("P2").getHeldResources()).containsExactly("R2", "R3");
            assertThat(s.resource("R1").getWaitQueue()).isEmpty();   // P3's pending request withdrawn
        }

        @Test
        void iteratesUntilAnIndirectlyCreatedCycleIsAlsoBroken() {
            RecoveryOutcome o = run(TestSupport.TERMINATION, deadlocked(ScenarioTemplates.cascadingDeadlock()));
            assertVerifiedAndConsistent(o);
            assertThat(o.rounds()).hasSize(2);
            assertThat(o.rounds().get(0).cyclesBefore()).containsExactly(List.of("P1", "P2"));
            assertThat(o.rounds().get(0).cyclesAfter()).containsExactly(List.of("P2", "P3"));
            assertThat(o.actions()).extracting(RecoveryAction::processId).containsExactly("P1", "P3");
            assertThat(o.actions()).extracting(RecoveryAction::workLost).containsExactly(5, 30);
        }

        @Test
        void breaksAllIndependentCycles() {
            RecoveryOutcome o = run(TestSupport.TERMINATION, deadlocked(ScenarioTemplates.twoCyclesWithDependent()));
            assertVerifiedAndConsistent(o);
            assertThat(o.rounds()).hasSize(2);
            assertThat(o.actions()).allMatch(a -> a.type() == RecoveryActionType.TERMINATE);
        }

        @Test
        void resolvesOverlappingCyclesWithOneWellChosenVictim() {
            RecoveryOutcome o = run(TestSupport.TERMINATION, deadlocked(ScenarioTemplates.overlappingCycles()));
            assertVerifiedAndConsistent(o);
            assertThat(o.actions()).extracting(RecoveryAction::processId).containsExactly("P3");
        }

        @Test
        void reportsTheSafetyLimitInsteadOfClaimingSuccess() {
            SystemState s = deadlocked(ScenarioTemplates.twoCyclesWithDependent());
            RecoveryOutcome o = TestSupport.TERMINATION.recover(s, new RecoveryContext(VictimWeights.defaults(), 1));
            assertThat(o.haltReason()).isEqualTo(HaltReason.SAFETY_LIMIT_REACHED);
            assertThat(o.rounds()).hasSize(1);
            assertThat(TestSupport.VERIFIER.verify(o.finalState()).deadlockFree()).isFalse();
        }
    }

    @Nested
    class Rollback {

        @Test
        void restoresTheLatestCheckpointThatPrecedesTheResourceAcquisition() {
            RecoveryOutcome o = run(TestSupport.ROLLBACK, deadlocked(ScenarioTemplates.rollbackFriendly()));
            assertVerifiedAndConsistent(o);
            RecoveryAction a = o.actions().get(0);
            assertThat(a.type()).isEqualTo(RecoveryActionType.ROLLBACK);
            assertThat(a.processId()).isEqualTo("P3");           // only 3 units since its checkpoint at 70
            assertThat(a.preemptedResource()).isEqualTo("R3");
            assertThat(a.restoredCheckpoint()).isEqualTo(1);  // checkpoint #1 at work 70
            assertThat(a.workBefore()).isEqualTo(73);
            assertThat(a.workAfter()).isEqualTo(70);
            assertThat(a.workLost()).isEqualTo(3);
            SystemState s = o.finalState();
            assertThat(s.process("P3").getState()).isEqualTo(ProcessState.READY);
            assertThat(s.process("P3").getWorkDone()).isEqualTo(70);
            assertThat(s.process("P3").getHeldResources()).isEmpty();
            assertThat(s.process("P3").getWaitingFor()).isEmpty();
            assertThat(s.process("P3").getVictimCount()).isEqualTo(1);
            assertThat(s.resource("R3").getHolder()).isEqualTo("P2");
            assertThat(s.resource("R4").getWaitQueue()).doesNotContain("P3");
        }

        @Test
        void fallsBackToTheInitialCheckpointWhenTheResourceWasHeldAtEveryLaterCheckpoint() {
            RecoveryOutcome o = run(TestSupport.ROLLBACK, deadlocked(ScenarioTemplates.classicThreeCycle()));
            assertVerifiedAndConsistent(o);
            RecoveryAction a = o.actions().get(0);
            assertThat(a.processId()).isEqualTo("P3");
            assertThat(a.restoredCheckpoint()).isZero();
            assertThat(a.workLost()).isEqualTo(10);
            assertThat(o.finalState().process("P3").getCheckpoints()).hasSize(1); // newer checkpoints discarded
        }

        @Test
        void keepsResourcesHeldAtTheCheckpointAndOnlyReleasesLaterAcquisitions() {
            RecoveryOutcome o = run(TestSupport.ROLLBACK, deadlocked(ScenarioTemplates.cascadingDeadlock()));
            assertVerifiedAndConsistent(o);
            assertThat(o.rounds()).hasSize(2);
            RecoveryAction second = o.actions().get(1);
            assertThat(second.processId()).isEqualTo("P3");
            assertThat(second.resourcesReleased()).containsExactly("R1");   // R3 was held at the checkpoint
            assertThat(second.workLost()).isZero();                         // checkpoint at exactly 30
            assertThat(o.finalState().process("P3").getHeldResources()).containsExactly("R3");
        }

        @Test
        void needsSeveralRoundsForOverlappingCycles() {
            RecoveryOutcome o = run(TestSupport.ROLLBACK, deadlocked(ScenarioTemplates.overlappingCycles()));
            assertVerifiedAndConsistent(o);
            assertThat(o.actions()).extracting(RecoveryAction::processId).containsExactly("P2", "P3");
            assertThat(o.actions()).extracting(RecoveryAction::workLost).containsExactly(15, 25);
            assertThat(o.rounds().get(0).cyclesAfter()).isNotEmpty();
        }

        @Test
        void latestValidCheckpointRejectsCheckpointsNeedingResourcesNoLongerHeld() {
            SystemState s = deadlocked(ScenarioTemplates.cascadingDeadlock());
            var p3 = s.process("P3");
            assertThat(RollbackStrategy.latestValidCheckpoint(p3, "R3")).get()
                    .extracting(c -> c.sequence()).isEqualTo(0);
            p3.getHeldResources().remove("R3"); // pretend R3 had been released after the checkpoints
            assertThat(RollbackStrategy.latestValidCheckpoint(p3, "R1")).get()
                    .extracting(c -> c.sequence()).isEqualTo(0);
        }
    }

    @Nested
    class TotalAbort {

        @Test
        void abortsOnlyTheDeadlockedSetNotBlockedOrIndependentProcesses() {
            RecoveryOutcome o = run(TestSupport.TOTAL_ABORT, deadlocked(ScenarioTemplates.twoCyclesWithDependent()));
            assertVerifiedAndConsistent(o);
            assertThat(o.rounds()).hasSize(1);
            assertThat(o.actions()).extracting(RecoveryAction::processId)
                    .containsExactly("P1", "P2", "P3", "P4", "P5");
            SystemState s = o.finalState();
            assertThat(s.process("P6").getState()).isEqualTo(ProcessState.RUNNING); // got R1
            assertThat(s.process("P6").getHeldResources()).containsExactly("R1");
            assertThat(s.process("P7").getState()).isEqualTo(ProcessState.COMPLETED);
        }

        @Test
        void neverHandsAFreedResourceToAnotherMemberOfTheAbortedSet() {
            RecoveryOutcome o = run(TestSupport.TOTAL_ABORT, deadlocked(ScenarioTemplates.classicThreeCycle()));
            assertVerifiedAndConsistent(o);
            assertThat(o.actions()).allMatch(a -> a.handovers().stream().allMatch(h -> h.endsWith("free")));
            assertThat(o.finalState().getResources().values()).allMatch(r -> r.getHolder() == null);
            assertThat(o.actions()).extracting(RecoveryAction::workLost).containsExactly(30, 20, 10);
        }

        @Test
        void resolvesTheCascadingScenarioInOneRound() {
            RecoveryOutcome o = run(TestSupport.TOTAL_ABORT, deadlocked(ScenarioTemplates.cascadingDeadlock()));
            assertVerifiedAndConsistent(o);
            assertThat(o.rounds()).hasSize(1);
            assertThat(o.finalState().process("P3").getHeldResources()).containsExactly("R3", "R1", "R2");
        }
    }

    @Test
    void strategiesDoNothingOnADeadlockFreeState() {
        for (RecoveryStrategy s : List.of(TestSupport.TERMINATION, TestSupport.ROLLBACK, TestSupport.TOTAL_ABORT)) {
            RecoveryOutcome o = run(s, deadlocked(ScenarioTemplates.noDeadlock()));
            assertThat(o.rounds()).isEmpty();
            assertThat(o.haltReason()).isEqualTo(HaltReason.DEADLOCK_RESOLVED);
        }
    }
}
