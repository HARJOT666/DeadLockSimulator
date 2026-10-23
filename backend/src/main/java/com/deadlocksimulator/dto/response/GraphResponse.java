package com.deadlocksimulator.dto.response;

import java.util.List;

/**
 * Wait-For Graph prepared for visualisation. Nodes include every process (also completed or
 * terminated ones, flagged as such) so before/after graphs can be laid out identically; edges
 * are only the live WFG dependencies.
 *
 * @param view "snapshot" or the strategy whose final state is shown
 */
public record GraphResponse(String view, List<Node> nodes, List<Edge> edges, List<List<String>> cycles,
                            List<String> deadlockedProcesses, List<String> blockedProcesses,
                            List<ResourceView> resources, boolean deadlocked) {

    public record Node(String id, String state, int priority, int workDone, int totalWork, int victimCount,
                       List<String> heldResources, List<String> waitingFor, int checkpointCount,
                       Integer lastCheckpointWork, boolean inCycle, boolean deadlocked, boolean blocked,
                       String recoveryAction) {
    }

    public record Edge(String id, String from, String to, String resource, boolean inCycle, String explanation) {
    }

    public record ResourceView(String id, String holder, List<String> waitQueue) {
    }
}
