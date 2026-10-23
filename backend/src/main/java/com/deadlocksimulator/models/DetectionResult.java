package com.deadlocksimulator.models;

import java.util.List;

/**
 * Output of DFS deadlock detection on one Wait-For Graph.
 *
 * @param deadlocked          true iff at least one directed cycle exists
 * @param cycles              one witness cycle per DFS back edge (see {@link DetectedCycle})
 * @param deadlockedProcesses every process that lies on some cycle (members of cyclic strongly
 *                            connected components) — the "deadlocked set" used by total abort
 * @param blockedProcesses    live processes NOT on a cycle that transitively wait for a deadlocked
 *                            process; they are stuck as a consequence, but are not themselves part
 *                            of a circular wait
 * @param vertexCount         |V|
 * @param edgeCount           |E| (labelled edges)
 * @param edgesExamined       de-duplicated adjacency entries examined by the DFS (proves O(V+E))
 */
public record DetectionResult(
        boolean deadlocked,
        List<DetectedCycle> cycles,
        List<String> deadlockedProcesses,
        List<String> blockedProcesses,
        int vertexCount,
        int edgeCount,
        int edgesExamined) {

    /**
     * A directed cycle {@code processes[0] -> processes[1] -> ... -> processes[k-1] -> processes[0]}.
     * {@code edges} lists the labelled WFG edges (with resources) along the cycle.
     */
    public record DetectedCycle(List<String> processes, List<WaitForGraph.Edge> edges) {
        public int length() {
            return processes.size();
        }
    }
}
