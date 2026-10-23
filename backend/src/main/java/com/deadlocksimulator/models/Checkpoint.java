package com.deadlocksimulator.models;

import java.util.List;

/**
 * Immutable snapshot of a process's restorable execution state.
 *
 * @param sequence      0 for the initial checkpoint, then 1, 2, ... in the order taken
 * @param workDone      work counter value at the checkpoint
 * @param heldResources resources held at the checkpoint (acquisition order)
 * @param takenAtStep   simulation clock value when the checkpoint was taken
 */
public record Checkpoint(int sequence, int workDone, List<String> heldResources, int takenAtStep) {

    public Checkpoint {
        // Defensive immutable copy: checkpoints may be shared safely between state copies.
        heldResources = List.copyOf(heldResources);
    }
}
