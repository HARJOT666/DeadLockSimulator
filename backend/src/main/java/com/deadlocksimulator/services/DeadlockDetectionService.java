package com.deadlocksimulator.services;

import com.deadlocksimulator.algorithms.DfsCycleDetector;
import com.deadlocksimulator.algorithms.WaitForGraphBuilder;
import com.deadlocksimulator.dto.response.GraphResponse;
import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.RecoveryAction;
import com.deadlocksimulator.models.SystemState;
import com.deadlocksimulator.models.WaitForGraph;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** WFG construction + DFS detection, and conversion of a state into a visualisable graph. */
@Service
public class DeadlockDetectionService {

    private final WaitForGraphBuilder graphBuilder;
    private final DfsCycleDetector detector;

    public DeadlockDetectionService(WaitForGraphBuilder graphBuilder, DfsCycleDetector detector) {
        this.graphBuilder = graphBuilder;
        this.detector = detector;
    }

    public DetectionResult detect(SystemState state) {
        return detector.detect(graphBuilder.build(state));
    }

    /**
     * @param view    label of the graph ("snapshot" or a strategy name)
     * @param actions recovery actions applied to reach this state (empty for the snapshot); used to
     *                mark victims on the graph
     */
    public GraphResponse graph(String view, SystemState state, List<RecoveryAction> actions) {
        WaitForGraph wfg = graphBuilder.build(state);
        DetectionResult d = detector.detect(wfg);
        Set<String> deadlocked = new HashSet<>(d.deadlockedProcesses());
        Set<String> blocked = new HashSet<>(d.blockedProcesses());
        Set<String> cycleEdges = new HashSet<>();
        Set<String> inCycle = new HashSet<>();
        for (DetectionResult.DetectedCycle c : d.cycles()) {
            inCycle.addAll(c.processes());
            c.edges().forEach(e -> cycleEdges.add(e.from() + ">" + e.to() + ">" + e.resource()));
        }
        Map<String, String> lastAction = new HashMap<>();
        actions.forEach(a -> lastAction.put(a.processId(), a.type().name()));

        List<GraphResponse.Node> nodes = state.getProcesses().values().stream().map(p -> new GraphResponse.Node(
                p.getId(), p.getState().name(), p.getPriority(), p.getWorkDone(), p.getTotalWork(), p.getVictimCount(),
                List.copyOf(p.getHeldResources()), List.copyOf(p.getWaitingFor()), p.getCheckpoints().size(),
                p.latestCheckpoint().map(c -> c.workDone()).orElse(null),
                inCycle.contains(p.getId()), deadlocked.contains(p.getId()), blocked.contains(p.getId()),
                lastAction.get(p.getId()))).toList();

        List<GraphResponse.Edge> edges = wfg.getEdges().stream().map(e -> new GraphResponse.Edge(
                e.from() + "-" + e.to() + "-" + e.resource(), e.from(), e.to(), e.resource(),
                cycleEdges.contains(e.from() + ">" + e.to() + ">" + e.resource()),
                e.from() + " is waiting for " + e.resource() + ", which is currently held by " + e.to() + "."))
                .toList();

        List<GraphResponse.ResourceView> resources = state.getResources().values().stream()
                .map(r -> new GraphResponse.ResourceView(r.getId(), r.getHolder(), List.copyOf(r.getWaitQueue())))
                .toList();

        return new GraphResponse(view, nodes, edges,
                d.cycles().stream().map(DetectionResult.DetectedCycle::processes).toList(),
                d.deadlockedProcesses(), d.blockedProcesses(), resources, d.deadlocked());
    }

}
