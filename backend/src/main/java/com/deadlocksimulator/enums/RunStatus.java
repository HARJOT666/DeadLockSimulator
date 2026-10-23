package com.deadlocksimulator.enums;

public enum RunStatus {
    /** Simulation finished without a deadlock. */
    NO_DEADLOCK,
    /** Simulation reached a deadlocked state; recovery not yet executed. */
    DEADLOCKED,
    /** At least one recovery strategy has been executed on the snapshot. */
    RECOVERY_EXECUTED
}
