package com.deadlocksimulator.enums;

public enum RecoveryStatus {
    /** Verified: no cycles remain and the state is internally consistent. */
    RECOVERED,
    /** Some actions were taken but cycles remain (e.g. safety limit reached). */
    PARTIAL,
    /** The strategy could not act or the final state failed verification. */
    FAILED,
    /** The snapshot was not deadlocked, so nothing had to be done. */
    NOT_REQUIRED
}
