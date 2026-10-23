package com.deadlocksimulator.algorithms;

import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.WaitForGraph;
import com.deadlocksimulator.models.WaitForGraph.Edge;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DfsCycleDetectorTest {

    private final DfsCycleDetector detector = new DfsCycleDetector();

    private static WaitForGraph graph(List<String> vertices, String... edges) {
        List<Edge> list = new ArrayList<>();
        int i = 0;
        for (String e : edges) {
            String[] parts = e.split("->");
            list.add(new Edge(parts[0], parts[1], "R" + (++i)));
        }
        return new WaitForGraph(vertices, list);
    }

    @Test
    void emptyGraphHasNoCycle() {
        DetectionResult r = detector.detect(new WaitForGraph(List.of(), List.of()));
        assertThat(r.deadlocked()).isFalse();
        assertThat(r.cycles()).isEmpty();
        assertThat(r.vertexCount()).isZero();
    }

    @Test
    void acyclicChainHasNoCycle() {
        DetectionResult r = detector.detect(graph(List.of("P1", "P2", "P3", "P4"), "P1->P2", "P2->P3", "P1->P3", "P4->P3"));
        assertThat(r.deadlocked()).isFalse();
        assertThat(r.deadlockedProcesses()).isEmpty();
        assertThat(r.blockedProcesses()).isEmpty();
    }

    @Test
    void detectsSimpleCycleAndReturnsItsOrderAndResources() {
        DetectionResult r = detector.detect(graph(List.of("P1", "P2", "P3"), "P1->P2", "P2->P3", "P3->P1"));
        assertThat(r.deadlocked()).isTrue();
        assertThat(r.cycles()).hasSize(1);
        assertThat(r.cycles().get(0).processes()).containsExactly("P1", "P2", "P3");
        assertThat(r.cycles().get(0).edges()).extracting(Edge::resource).containsExactly("R1", "R2", "R3");
        assertThat(r.deadlockedProcesses()).containsExactly("P1", "P2", "P3");
    }

    @Test
    void selfLoopIsAOneVertexCycle() {
        DetectionResult r = detector.detect(graph(List.of("P1", "P2"), "P1->P1", "P2->P1"));
        assertThat(r.cycles()).hasSize(1);
        assertThat(r.cycles().get(0).processes()).containsExactly("P1");
        assertThat(r.deadlockedProcesses()).containsExactly("P1");
        assertThat(r.blockedProcesses()).containsExactly("P2");
    }

    @Test
    void handlesDisconnectedComponentsAndFindsEveryIndependentCycle() {
        DetectionResult r = detector.detect(graph(List.of("P1", "P2", "P3", "P4", "P5", "P6", "P7"),
                "P1->P2", "P2->P1", "P3->P4", "P4->P5", "P5->P3", "P6->P7"));
        assertThat(r.cycles()).extracting(DetectionResult.DetectedCycle::processes)
                .containsExactly(List.of("P1", "P2"), List.of("P3", "P4", "P5"));
        assertThat(r.deadlockedProcesses()).containsExactly("P1", "P2", "P3", "P4", "P5");
        assertThat(r.blockedProcesses()).isEmpty();
    }

    @Test
    void overlappingCyclesSharingAVertexYieldOneWitnessPerBackEdge() {
        // P1 <-> P2 and P2 <-> P3 share P2; DFS sees two back edges (P2->P1 and P3->P2).
        DetectionResult r = detector.detect(graph(List.of("P1", "P2", "P3"), "P1->P2", "P2->P1", "P2->P3", "P3->P2"));
        Set<List<String>> cycles = new HashSet<>();
        r.cycles().forEach(c -> cycles.add(c.processes()));
        assertThat(cycles).containsExactlyInAnyOrder(List.of("P1", "P2"), List.of("P2", "P3"));
        assertThat(r.deadlockedProcesses()).containsExactly("P1", "P2", "P3");
    }

    @Test
    void overlappingCyclesWithASingleBackEdgeStillProduceTheCompleteDeadlockedSet() {
        // Cycles P1->P2->P3->P1 and P1->P3->P1 overlap, but P3->P1 is the only back edge, so DFS
        // reports one witness. The SCC-based deadlocked set still contains every member.
        DetectionResult r = detector.detect(graph(List.of("P1", "P2", "P3"), "P1->P2", "P1->P3", "P2->P3", "P3->P1"));
        assertThat(r.deadlocked()).isTrue();
        assertThat(r.cycles()).hasSize(1);
        assertThat(r.cycles().get(0).processes()).containsExactly("P1", "P2", "P3");
        assertThat(r.deadlockedProcesses()).containsExactly("P1", "P2", "P3");
    }

    @Test
    void deadlockedSetIncludesSccMembersMissedByWitnessCycles() {
        // a->b, b->a, a->c, c->b : DFS reports only [a,b] (c->b is a cross edge), yet c is on cycle a->c->b->a.
        DetectionResult r = detector.detect(graph(List.of("a", "b", "c"), "a->b", "b->a", "a->c", "c->b"));
        assertThat(r.cycles()).extracting(DetectionResult.DetectedCycle::processes).containsExactly(List.of("a", "b"));
        assertThat(r.deadlockedProcesses()).containsExactly("a", "b", "c");
    }

    @Test
    void processesWaitingOnACycleAreBlockedButNotDeadlocked() {
        DetectionResult r = detector.detect(graph(List.of("P1", "P2", "P3", "P4", "P5"),
                "P1->P2", "P2->P1", "P3->P1", "P4->P3", "P5->P4"));
        assertThat(r.deadlockedProcesses()).containsExactly("P1", "P2");
        assertThat(r.blockedProcesses()).containsExactly("P3", "P4", "P5");
    }

    @Test
    void examinesEachAdjacencyEntryExactlyOnce() {
        WaitForGraph g = graph(List.of("P1", "P2", "P3", "P4"), "P1->P2", "P2->P3", "P3->P1", "P4->P1", "P4->P2");
        DetectionResult r = detector.detect(g);
        assertThat(r.edgesExamined()).isEqualTo(5);
        assertThat(r.vertexCount()).isEqualTo(4);
    }

    @Test
    void duplicateLabelledEdgesBetweenTheSamePairAreReportedOnTheCycle() {
        WaitForGraph g = new WaitForGraph(List.of("P1", "P2"), List.of(
                new Edge("P1", "P2", "R1"), new Edge("P1", "P2", "R2"), new Edge("P2", "P1", "R3")));
        DetectionResult r = detector.detect(g);
        assertThat(r.cycles()).hasSize(1);
        assertThat(r.cycles().get(0).edges()).extracting(Edge::resource).containsExactly("R1", "R2", "R3");
        assertThat(r.edgesExamined()).isEqualTo(2);
    }

    @Test
    void largeRingDoesNotOverflowAndIsDetected() {
        int n = 2000;
        List<String> v = new ArrayList<>();
        List<Edge> e = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            v.add("P" + i);
            e.add(new Edge("P" + i, "P" + ((i + 1) % n), "R" + i));
        }
        DetectionResult r = detector.detect(new WaitForGraph(v, e));
        assertThat(r.cycles()).hasSize(1);
        assertThat(r.cycles().get(0).length()).isEqualTo(n);
    }
}
