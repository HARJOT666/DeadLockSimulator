package com.deadlocksimulator.strategies;

import com.deadlocksimulator.enums.StrategyType;
import com.deadlocksimulator.models.RecoveryContext;
import com.deadlocksimulator.models.RecoveryOutcome;
import com.deadlocksimulator.models.SystemState;

/**
 * A deadlock recovery strategy. Implementations MUTATE the state they are given, so callers
 * must pass an independent copy (see {@link com.deadlocksimulator.algorithms.StateCloner}).
 */
public interface RecoveryStrategy {

    StrategyType type();

    RecoveryOutcome recover(SystemState state, RecoveryContext context);
}
