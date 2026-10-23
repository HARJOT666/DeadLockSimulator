package com.deadlocksimulator.models;

import java.util.List;

/**
 * The seven evaluation metrics (plus two supporting counts).
 *
 * <ol>
 *   <li>{@code workLost} — work units discarded: full work counter of terminated/aborted
 *       processes, work since the restored checkpoint for rollbacks. Each action counted once.</li>
 *   <li>{@code processesTerminated} — processes killed (termination victims or aborted members).</li>
 *   <li>{@code processesRolledBack} — rollback <em>operations</em> performed; a process rolled back
 *       twice counts twice. {@code distinctProcessesRolledBack} gives the number of processes.</li>
 *   <li>{@code resourcesReclaimed} — resource release events caused by recovery (single-instance,
 *       so one event = one resource instance; a resource reclaimed in two rounds counts twice).</li>
 *   <li>{@code recoverySteps} — detect→act→re-detect rounds. Termination and rollback act on one
 *       victim per round; total abort acts on the whole deadlocked set in one round.
 *       {@code recoveryActions} gives the number of per-process actions.</li>
 *   <li>{@code starvationRisk} — maximum victim count among surviving (non-terminated) processes,
 *       including victim history carried into the scenario.</li>
 *   <li>{@code aggregateScore} — weighted, normalised composite in [0,1]; lower is better.
 *       {@code scoreBreakdown} makes every term inspectable.</li>
 * </ol>
 */
public record RecoveryMetrics(
        int workLost,
        int processesTerminated,
        int processesRolledBack,
        int distinctProcessesRolledBack,
        int resourcesReclaimed,
        int recoverySteps,
        int recoveryActions,
        int starvationRisk,
        double aggregateScore,
        List<ScoreComponent> scoreBreakdown) {

    /**
     * One term of the aggregate score.
     *
     * @param metric       metric name
     * @param rawValue     raw metric value
     * @param normaliser   scenario-intrinsic denominator
     * @param normalised   min(raw / normaliser, 1)
     * @param weight       configured weight
     * @param contribution weight * normalised / sum(weights)
     */
    public record ScoreComponent(String metric, double rawValue, double normaliser, double normalised,
                                 double weight, double contribution) {
    }
}
