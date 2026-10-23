package com.deadlocksimulator.enums;

/** Operations a scenario's deterministic request sequence may contain. */
public enum OperationType {
    /** Request one or more resources at once (AND-request; blocks until all are granted). */
    REQUEST,
    /** Perform {@code amount} units of work (may trigger periodic checkpoints). */
    WORK,
    /** Release resources currently held. */
    RELEASE,
    /** Finish: release everything and complete. */
    FINISH
}
