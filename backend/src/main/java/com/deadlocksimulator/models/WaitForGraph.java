package com.deadlocksimulator.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wait-For Graph. An edge {@code Pi -> Pj} labelled with resource R means
 * "Pi is waiting for R, which is currently held by Pj".
 *
 * <p>Vertices are the live processes. Two different resources can produce the same
 * process pair, so labelled edges are kept in {@link #getEdges()} while
 * {@link #successors(String)} returns the de-duplicated adjacency used by DFS.
 */
public class WaitForGraph {

    /** Labelled edge explaining why the dependency exists. */
    public record Edge(String from, String to, String resource) {
    }

    private final List<String> vertices;
    private final List<Edge> edges;
    private final Map<String, List<String>> adjacency = new LinkedHashMap<>();

    public WaitForGraph(List<String> vertices, List<Edge> edges) {
        this.vertices = List.copyOf(vertices);
        this.edges = List.copyOf(edges);
        Map<String, Set<String>> tmp = new LinkedHashMap<>();
        for (String v : this.vertices) {
            tmp.put(v, new LinkedHashSet<>());
        }
        for (Edge e : this.edges) {
            if (!tmp.containsKey(e.from()) || !tmp.containsKey(e.to())) {
                throw new IllegalArgumentException("Edge references unknown vertex: " + e);
            }
            tmp.get(e.from()).add(e.to());
        }
        tmp.forEach((v, succ) -> adjacency.put(v, List.copyOf(succ)));
    }

    public List<String> getVertices() {
        return vertices;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public List<String> successors(String vertex) {
        return adjacency.getOrDefault(vertex, List.of());
    }

    /** Reverse adjacency (who waits for {@code vertex}); built on demand. */
    public Map<String, List<String>> reverseAdjacency() {
        Map<String, List<String>> rev = new LinkedHashMap<>();
        vertices.forEach(v -> rev.put(v, new ArrayList<>()));
        adjacency.forEach((from, succ) -> succ.forEach(to -> rev.get(to).add(from)));
        rev.replaceAll((k, v) -> Collections.unmodifiableList(v));
        return rev;
    }

    /** Resources that explain the dependency {@code from -> to}. */
    public List<String> resourcesBetween(String from, String to) {
        return edges.stream().filter(e -> e.from().equals(from) && e.to().equals(to)).map(Edge::resource).toList();
    }

    public int edgeCount() {
        return edges.size();
    }
}
