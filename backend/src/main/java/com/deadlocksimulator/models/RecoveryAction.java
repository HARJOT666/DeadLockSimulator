package com.deadlocksimulator.models;

import com.deadlocksimulator.enums.RecoveryActionType;

import java.util.List;

/**
 * One recovery action applied to one process.
 *
 * @param round               recovery round (1-based)
 * @param type                TERMINATE, ROLLBACK or ABORT
 * @param processId           victim
 * @param selectionCost       victim-selection cost (null for total abort, which does not select)
 * @param workBefore          victim's work counter before the action
 * @param workAfter           work counter after (0-progress is kept for terminated processes)
 * @param workLost            work units lost by this action
 * @param preemptedResource   resource whose preemption motivated a rollback (null otherwise)
 * @param restoredCheckpoint  sequence number of the checkpoint restored (rollback only)
 * @param resourcesReleased   resources taken from the victim, in release order
 * @param handovers           where each released resource went, e.g. "R2 -> P3" or "R2 -> free"
 * @param explanation         human-readable summary
 */
public record RecoveryAction(
        int round,
        RecoveryActionType type,
        String processId,
        Double selectionCost,
        int workBefore,
        int workAfter,
        int workLost,
        String preemptedResource,
        Integer restoredCheckpoint,
        List<String> resourcesReleased,
        List<String> handovers,
        String explanation) {
}
