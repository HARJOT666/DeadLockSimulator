package com.deadlocksimulator.enums;

public enum StrategyType {
    TERMINATION("Iterative process termination"),
    ROLLBACK("Resource preemption with checkpoint rollback"),
    TOTAL_ABORT("Total abort of the deadlocked set");

    private final String displayName;

    StrategyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
