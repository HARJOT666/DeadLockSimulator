package com.deadlocksimulator.algorithms;

import com.deadlocksimulator.models.Process;
import com.deadlocksimulator.models.Resource;
import com.deadlocksimulator.models.SystemState;
import com.deadlocksimulator.models.WaitForGraph;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the Wait-For Graph from the resource-allocation state.
 *
 * <p>For every live process Pi and every resource R in Pi's pending requests, if R is held by
 * Pj then the edge Pi -> Pj (label R) is added. Resources that are free do not create edges
 * (a free resource with waiters cannot persist because releases immediately hand over).
 * Runs in O(P + pending requests).
 */
@Component
public class WaitForGraphBuilder {

    public WaitForGraph build(SystemState state) {
        List<String> vertices = new ArrayList<>();
        List<WaitForGraph.Edge> edges = new ArrayList<>();
        for (Process p : state.getProcesses().values()) {
            if (p.isLive()) {
                vertices.add(p.getId());
            }
        }
        for (Process p : state.getProcesses().values()) {
            if (!p.isLive()) {
                continue;
            }
            for (String rid : p.getWaitingFor()) {
                Resource r = state.resource(rid);
                String holder = r.getHolder();
                if (holder != null && state.process(holder).isLive()) {
                    edges.add(new WaitForGraph.Edge(p.getId(), holder, rid));
                }
            }
        }
        return new WaitForGraph(vertices, edges);
    }
}
