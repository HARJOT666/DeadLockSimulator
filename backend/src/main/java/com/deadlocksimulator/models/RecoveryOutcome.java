package com.deadlocksimulator.models;

import com.deadlocksimulator.enums.HaltReason;
import com.deadlocksimulator.enums.StrategyType;

import java.util.List;

/**
 * Raw result of running one strategy on its own state copy (before verification and scoring).
 *
 * @param strategy   which strategy ran
 * @param finalState the (mutated) copy the strategy worked on
 * @param rounds     evidence for every detect → act → re-detect round
 * @param haltReason why the loop stopped
 */
public record RecoveryOutcome(StrategyType strategy, SystemState finalState, List<RoundTrace> rounds,
                              HaltReason haltReason) {

    public List<RecoveryAction> actions() {
        return rounds.stream().flatMap(r -> r.actions().stream()).toList();
    }
}
