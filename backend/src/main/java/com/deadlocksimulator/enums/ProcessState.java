package com.deadlocksimulator.enums;

/**
 * Lifecycle of a simulated process.
 *
 * RUNNING    -> executing its program; may REQUEST / WORK / RELEASE / FINISH.
 * WAITING    -> blocked on at least one pending resource request.
 * READY      -> rolled back to a checkpoint by recovery; holds only the checkpoint's
 *               resources, has no pending requests and must re-execute from there.
 * COMPLETED  -> finished normally and released everything.
 * TERMINATED -> killed by a recovery strategy; holds nothing.
 */
public enum ProcessState {
    RUNNING,
    WAITING,
    READY,
    COMPLETED,
    TERMINATED;

    /** Live processes still participate in the Wait-For Graph. */
    public boolean isLive() {
        return this != COMPLETED && this != TERMINATED;
    }
}
