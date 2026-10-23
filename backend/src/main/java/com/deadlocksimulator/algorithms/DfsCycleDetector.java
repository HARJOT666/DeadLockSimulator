package com.deadlocksimulator.algorithms;

import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.DetectionResult.DetectedCycle;
import com.deadlocksimulator.models.WaitForGraph;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Three-colour depth-first search for directed cycles in a Wait-For Graph.
 *
 * <ul>
 *   <li>WHITE — not yet visited;</li>
 *   <li>GRAY  — on the current DFS path (being explored);</li>
 *   <li>BLACK — fully explored.</li>
 * </ul>
 * An edge u -> v with v GRAY is a back edge and closes the cycle v -> ... -> u -> v, which is
 * read off the current DFS path. The outer loop starts a DFS from every still-WHITE vertex,
 * so disconnected components are covered. A self-loop u -> u is reported as a 1-cycle.
 *
 * <p>Complexity: every vertex is coloured once and every adjacency entry examined once, so the
 * search is O(V + E). Extracting a cycle costs O(length) per back edge.
 *
 * <p>Multiple / overlapping cycles: one witness cycle is reported per back edge. A graph can
 * contain exponentially many elementary cycles, so these witnesses are not an enumeration of all
 * cycles. The complete deadlocked set is therefore computed separately as the union of all
 * strongly connected components that contain a cycle (every such component contains at least one
 * back edge, so each is seeded by a reported witness).
 */
@Component
public class DfsCycleDetector {

    private enum Colour { WHITE, GRAY, BLACK }

    public DetectionResult detect(WaitForGraph graph) {
        Map<String, Colour> colour = new HashMap<>();
        graph.getVertices().forEach(v -> colour.put(v, Colour.WHITE));

        List<String> path = new ArrayList<>();          // current DFS path (GRAY vertices in order)
        Map<String, Integer> pathIndex = new HashMap<>(); // vertex -> position on path
        List<DetectedCycle> cycles = new ArrayList<>();
        int[] edgesExamined = {0};

        for (String start : graph.getVertices()) {
            if (colour.get(start) == Colour.WHITE) {
                visit(start, graph, colour, path, pathIndex, cycles, edgesExamined);
            }
        }

        List<String> deadlocked = deadlockedSet(graph, cycles);
        List<String> blocked = blockedByDeadlock(graph, deadlocked);
        return new DetectionResult(!cycles.isEmpty(), List.copyOf(cycles), deadlocked, blocked,
                graph.getVertices().size(), graph.edgeCount(), edgesExamined[0]);
    }

    private void visit(String u, WaitForGraph graph, Map<String, Colour> colour, List<String> path,
                       Map<String, Integer> pathIndex, List<DetectedCycle> cycles, int[] edgesExamined) {
        colour.put(u, Colour.GRAY);
        pathIndex.put(u, path.size());
        path.add(u);

        for (String v : graph.successors(u)) {
            edgesExamined[0]++;
            switch (colour.get(v)) {
                case WHITE -> visit(v, graph, colour, path, pathIndex, cycles, edgesExamined);
                case GRAY -> cycles.add(extractCycle(graph, path, pathIndex.get(v)));
                case BLACK -> { /* cross / forward edge: already fully explored, no new cycle */ }
            }
        }

        path.remove(path.size() - 1);
        pathIndex.remove(u);
        colour.put(u, Colour.BLACK);
    }

    /** The cycle is path[from..end] closed by the back edge end -> path[from]. */
    private DetectedCycle extractCycle(WaitForGraph graph, List<String> path, int from) {
        List<String> members = List.copyOf(path.subList(from, path.size()));
        List<WaitForGraph.Edge> edges = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            String a = members.get(i);
            String b = members.get((i + 1) % members.size());
            for (String r : graph.resourcesBetween(a, b)) {
                edges.add(new WaitForGraph.Edge(a, b, r));
            }
        }
        return new DetectedCycle(members, List.copyOf(edges));
    }

    /**
     * Union of the cyclic strongly connected components, seeded by the DFS witness cycles:
     * SCC(c) = forwardReach(c) ∩ backwardReach(c). Returned in graph vertex order.
     */
    private List<String> deadlockedSet(WaitForGraph graph, List<DetectedCycle> cycles) {
        if (cycles.isEmpty()) {
            return List.of();
        }
        Map<String, List<String>> reverse = graph.reverseAdjacency();
        Set<String> result = new HashSet<>();
        for (DetectedCycle cycle : cycles) {
            String seed = cycle.processes().get(0);
            if (result.contains(seed)) {
                continue;
            }
            Set<String> forward = reach(seed, graph::successors);
            Set<String> backward = reach(seed, reverse::get);
            forward.retainAll(backward);
            result.addAll(forward);
        }
        return graph.getVertices().stream().filter(result::contains).toList();
    }

    /** Live processes that can reach the deadlocked set but are not part of it. */
    private List<String> blockedByDeadlock(WaitForGraph graph, List<String> deadlocked) {
        if (deadlocked.isEmpty()) {
            return List.of();
        }
        Map<String, List<String>> reverse = graph.reverseAdjacency();
        Set<String> reached = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>(deadlocked);
        Set<String> core = new HashSet<>(deadlocked);
        while (!queue.isEmpty()) {
            for (String waiter : reverse.get(queue.poll())) {
                if (!core.contains(waiter) && reached.add(waiter)) {
                    queue.add(waiter);
                }
            }
        }
        return graph.getVertices().stream().filter(reached::contains).toList();
    }

    private Set<String> reach(String start, java.util.function.Function<String, List<String>> next) {
        Set<String> seen = new LinkedHashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push(start);
        seen.add(start);
        while (!stack.isEmpty()) {
            for (String n : next.apply(stack.pop())) {
                if (seen.add(n)) {
                    stack.push(n);
                }
            }
        }
        return seen;
    }
}
