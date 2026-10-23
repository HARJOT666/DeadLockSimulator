package com.deadlocksimulator.models;

import java.util.List;

/**
 * Independent post-recovery check: rebuild the WFG, rerun DFS, check state invariants.
 *
 * @param deadlockFree        DFS found no cycle in the rebuilt WFG
 * @param remainingCycles     cycles still present (empty if deadlock-free)
 * @param remainingDeadlocked processes still deadlocked
 * @param consistent          ownership / wait-queue invariants hold
 * @param violations          invariant violations, if any
 * @param graphVertices       |V| of the rebuilt WFG
 * @param graphEdges          |E| of the rebuilt WFG
 */
public record VerificationResult(
        boolean deadlockFree,
        List<List<String>> remainingCycles,
        List<String> remainingDeadlocked,
        boolean consistent,
        List<String> violations,
        int graphVertices,
        int graphEdges) {

    public boolean verified() {
        return deadlockFree && consistent;
    }
}
