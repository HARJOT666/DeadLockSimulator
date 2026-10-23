package com.deadlocksimulator.algorithms;

import com.deadlocksimulator.TestSupport;
import com.deadlocksimulator.models.SystemState;
import com.deadlocksimulator.models.WaitForGraph;
import com.deadlocksimulator.models.WaitForGraph.Edge;
import com.deadlocksimulator.simulation.ScenarioBuilder;
import com.deadlocksimulator.simulation.ScenarioTemplates;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WaitForGraphBuilderTest {

    private final WaitForGraphBuilder builder = new WaitForGraphBuilder();

    @Test
    void edgePointsFromWaiterToHolderAndIsLabelledWithTheResource() {
        SystemState s = TestSupport.simulate(ScenarioTemplates.classicThreeCycle().config());
        WaitForGraph g = builder.build(s);
        assertThat(g.getVertices()).containsExactly("P1", "P2", "P3");
        assertThat(g.getEdges()).containsExactly(
                new Edge("P1", "P2", "R2"), new Edge("P2", "P3", "R3"), new Edge("P3", "P1", "R1"));
    }

    @Test
    void andRequestProducesOneEdgePerBusyResource() {
        SystemState s = TestSupport.simulate(ScenarioTemplates.overlappingCycles().config());
        WaitForGraph g = builder.build(s);
        assertThat(g.getEdges()).contains(new Edge("P1", "P2", "R2"), new Edge("P1", "P3", "R3"));
        assertThat(g.successors("P1")).containsExactly("P2", "P3");
    }

    @Test
    void grantedRequestsAndCompletedProcessesCreateNoEdges() {
        SystemState s = TestSupport.simulate(ScenarioTemplates.noDeadlock().config());
        WaitForGraph g = builder.build(s);
        assertThat(g.getVertices()).isEmpty(); // all three processes completed
        assertThat(g.getEdges()).isEmpty();
    }

    @Test
    void terminatedProcessesAreRemovedTogetherWithTheirEdges() {
        SystemState s = TestSupport.simulate(ScenarioTemplates.classicThreeCycle().config());
        s.terminate("P2");
        WaitForGraph g = builder.build(s);
        assertThat(g.getVertices()).containsExactly("P1", "P3");
        // P1 now holds R2 (handed over), P3 still waits for R1 held by P1.
        assertThat(g.getEdges()).containsExactly(new Edge("P3", "P1", "R1"));
    }

    @Test
    void waitingForAFreeResourceIsImpossibleBecauseReleaseHandsOver() {
        SystemState s = TestSupport.simulate(new ScenarioBuilder()
                .process("P1", 1, 10).process("P2", 1, 10).resources("R1")
                .request("P1", "R1").request("P2", "R1").release("P1", "R1").build());
        assertThat(s.resource("R1").getHolder()).isEqualTo("P2");
        assertThat(builder.build(s).getEdges()).isEmpty();
    }
}
