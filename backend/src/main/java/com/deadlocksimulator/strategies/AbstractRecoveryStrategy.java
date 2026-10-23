package com.deadlocksimulator.strategies;

import com.deadlocksimulator.algorithms.DfsCycleDetector;
import com.deadlocksimulator.algorithms.WaitForGraphBuilder;
import com.deadlocksimulator.enums.HaltReason;
import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.RecoveryAction;
import com.deadlocksimulator.models.RecoveryContext;
import com.deadlocksimulator.models.RecoveryOutcome;
import com.deadlocksimulator.models.RoundTrace;
import com.deadlocksimulator.models.SystemState;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared detect → act → re-detect loop. After every round the Wait-For Graph is rebuilt from the
 * mutated state and DFS is rerun; the loop continues until no cycle remains, the safety limit is
 * reached, or the strategy reports that no admissible action exists.
 */
public abstract class AbstractRecoveryStrategy implements RecoveryStrategy {

    /** What one round did: the actions applied and the candidate ranking that justified them. */
    protected record RoundResult(List<RecoveryAction> actions, List<RoundTrace.CandidateScore> candidates) {
        static RoundResult none() {
            return new RoundResult(List.of(), List.of());
        }
    }

    private final WaitForGraphBuilder graphBuilder;
    private final DfsCycleDetector detector;

    protected AbstractRecoveryStrategy(WaitForGraphBuilder graphBuilder, DfsCycleDetector detector) {
        this.graphBuilder = graphBuilder;
        this.detector = detector;
    }

    @Override
    public final RecoveryOutcome recover(SystemState state, RecoveryContext context) {
        List<RoundTrace> rounds = new ArrayList<>();
        DetectionResult detection = detect(state);
        HaltReason halt;
        while (true) {
            if (!detection.deadlocked()) {
                halt = HaltReason.DEADLOCK_RESOLVED;
                break;
            }
            if (rounds.size() >= context.maxRounds()) {
                halt = HaltReason.SAFETY_LIMIT_REACHED;
                break;
            }
            int round = rounds.size() + 1;
            RoundResult result = performRound(state, detection, round, context);
            if (result.actions().isEmpty()) {
                halt = HaltReason.NO_ADMISSIBLE_ACTION;
                break;
            }
            DetectionResult after = detect(state);
            rounds.add(new RoundTrace(round, cycleLists(detection), detection.deadlockedProcesses(),
                    result.candidates(), result.actions(), cycleLists(after), after.deadlockedProcesses()));
            detection = after;
        }
        state.log("RECOVERY_END", null, null, type() + " stopped after " + rounds.size() + " round(s): " + halt);
        return new RecoveryOutcome(type(), state, List.copyOf(rounds), halt);
    }

    /**
     * Applies one round of recovery to {@code state}.
     *
     * @return the actions taken; an empty list means no admissible action exists
     */
    protected abstract RoundResult performRound(SystemState state, DetectionResult detection, int round,
                                                RecoveryContext context);

    protected DetectionResult detect(SystemState state) {
        return detector.detect(graphBuilder.build(state));
    }

    /** "R1 -> P3" / "R1 -> free" for each released resource, read from the state after the action. */
    protected static List<String> handovers(SystemState state, List<String> released) {
        return released.stream().map(r -> {
            String holder = state.resource(r).getHolder();
            return r + " -> " + (holder == null ? "free" : holder);
        }).toList();
    }

    private static List<List<String>> cycleLists(DetectionResult d) {
        return d.cycles().stream().map(DetectionResult.DetectedCycle::processes).toList();
    }
}
