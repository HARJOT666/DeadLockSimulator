package com.deadlocksimulator.enums;

/** Why a recovery loop stopped. */
public enum HaltReason {
    /** Detection found no remaining cycle. */
    DEADLOCK_RESOLVED,
    /** Configured maximum number of recovery rounds was reached. */
    SAFETY_LIMIT_REACHED,
    /** No admissible recovery action existed for the remaining cycle. */
    NO_ADMISSIBLE_ACTION
}
