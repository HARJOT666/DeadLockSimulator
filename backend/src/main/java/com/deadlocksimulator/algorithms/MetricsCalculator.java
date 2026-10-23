package com.deadlocksimulator.algorithms;

import com.deadlocksimulator.enums.ProcessState;
import com.deadlocksimulator.enums.RecoveryActionType;
import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.Process;
import com.deadlocksimulator.models.RecoveryAction;
import com.deadlocksimulator.models.RecoveryMetrics;
import com.deadlocksimulator.models.RecoveryMetrics.ScoreComponent;
import com.deadlocksimulator.models.RecoveryOutcome;
import com.deadlocksimulator.models.ScoreWeights;
import com.deadlocksimulator.models.SystemState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the seven recovery metrics from a strategy's actions and final state, and the
 * aggregate score from scenario-intrinsic normalisers (see {@link RecoveryMetrics} for definitions).
 *
 * <p>Aggregate score = Σ wᵢ · min(rawᵢ / normaliserᵢ, 1) / Σ wᵢ, in [0, 1], lower is better.
 * Normalisers depend only on the deadlocked snapshot, never on the other strategies, so a score
 * does not change when another strategy is added or removed:
 * <ul>
 *   <li>work lost / total work done by live processes in the snapshot</li>
 *   <li>terminated, rolled back / number of live processes in the snapshot</li>
 *   <li>resources reclaimed / number of resources</li>
 *   <li>recovery steps / size of the deadlocked set (an upper bound for one-victim-per-round)</li>
 *   <li>starvation risk / starvation threshold</li>
 * </ul>
 */
@Component
public class MetricsCalculator {

    /** Scenario-intrinsic denominators derived from the deadlocked snapshot. */
    public record Normalisers(int liveWork, int liveProcesses, int resources, int deadlockedProcesses) {

        public static Normalisers of(SystemState snapshot, DetectionResult detection) {
            int liveWork = snapshot.liveProcesses().stream().mapToInt(Process::getWorkDone).sum();
            return new Normalisers(liveWork, snapshot.liveProcesses().size(), snapshot.getResources().size(),
                    detection.deadlockedProcesses().size());
        }
    }

    public RecoveryMetrics calculate(RecoveryOutcome outcome, Normalisers norm, ScoreWeights weights) {
        List<RecoveryAction> actions = outcome.actions();
        int workLost = actions.stream().mapToInt(RecoveryAction::workLost).sum();
        int terminated = (int) actions.stream()
                .filter(a -> a.type() == RecoveryActionType.TERMINATE || a.type() == RecoveryActionType.ABORT).count();
        int rollbacks = (int) actions.stream().filter(a -> a.type() == RecoveryActionType.ROLLBACK).count();
        int distinctRolledBack = (int) actions.stream().filter(a -> a.type() == RecoveryActionType.ROLLBACK)
                .map(RecoveryAction::processId).distinct().count();
        int reclaimed = actions.stream().mapToInt(a -> a.resourcesReleased().size()).sum();
        int steps = outcome.rounds().size();
        int starvation = outcome.finalState().getProcesses().values().stream()
                .filter(p -> p.getState() != ProcessState.TERMINATED)
                .mapToInt(Process::getVictimCount).max().orElse(0);

        List<ScoreComponent> parts = new ArrayList<>();
        double wsum = weights.sum();
        parts.add(component("workLost", workLost, norm.liveWork(), weights.workLost(), wsum));
        parts.add(component("processesTerminated", terminated, norm.liveProcesses(), weights.processesTerminated(), wsum));
        parts.add(component("processesRolledBack", rollbacks, norm.liveProcesses(), weights.processesRolledBack(), wsum));
        parts.add(component("resourcesReclaimed", reclaimed, norm.resources(), weights.resourcesReclaimed(), wsum));
        parts.add(component("recoverySteps", steps, norm.deadlockedProcesses(), weights.recoverySteps(), wsum));
        parts.add(component("starvationRisk", starvation, weights.starvationThreshold(), weights.starvationRisk(), wsum));
        double score = round(parts.stream().mapToDouble(ScoreComponent::contribution).sum());

        return new RecoveryMetrics(workLost, terminated, rollbacks, distinctRolledBack, reclaimed, steps,
                actions.size(), starvation, score, parts);
    }

    private static ScoreComponent component(String name, double raw, double normaliser, double weight, double wsum) {
        double normalised = normaliser <= 0 ? 0.0 : Math.min(raw / normaliser, 1.0);
        double contribution = wsum <= 0 ? 0.0 : weight * normalised / wsum;
        return new ScoreComponent(name, raw, normaliser, round(normalised), weight, round(contribution));
    }

    static double round(double v) {
        return Math.round(v * 1_000_000d) / 1_000_000d;
    }
}
