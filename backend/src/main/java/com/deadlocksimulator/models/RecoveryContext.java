package com.deadlocksimulator.models;

/**
 * Parameters shared by all strategies in one comparison.
 *
 * @param victimWeights victim-selection weights
 * @param maxRounds     safety limit on detect→act→re-detect rounds
 */
public record RecoveryContext(VictimWeights victimWeights, int maxRounds) {

    public static RecoveryContext defaults(SystemState state) {
        return new RecoveryContext(VictimWeights.defaults(), defaultMaxRounds(state));
    }

    /** Termination/rollback need at most a few rounds per process; 2|P| + 5 is a generous bound. */
    public static int defaultMaxRounds(SystemState state) {
        return 2 * state.getProcesses().size() + 5;
    }
}
