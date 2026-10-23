package com.deadlocksimulator.algorithms;

import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.SystemState;
import com.deadlocksimulator.models.VerificationResult;
import com.deadlocksimulator.models.WaitForGraph;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Independent verification of a recovered state. It does not trust the strategy's own loop:
 * it rebuilds the WFG from scratch, reruns DFS and checks every ownership/queue invariant.
 */
@Component
public class RecoveryVerifier {

    private final WaitForGraphBuilder graphBuilder;
    private final DfsCycleDetector detector;
    private final StateConsistencyChecker consistencyChecker;

    public RecoveryVerifier(WaitForGraphBuilder graphBuilder, DfsCycleDetector detector,
                            StateConsistencyChecker consistencyChecker) {
        this.graphBuilder = graphBuilder;
        this.detector = detector;
        this.consistencyChecker = consistencyChecker;
    }

    public VerificationResult verify(SystemState state) {
        WaitForGraph graph = graphBuilder.build(state);
        DetectionResult detection = detector.detect(graph);
        List<String> violations = consistencyChecker.check(state);
        return new VerificationResult(!detection.deadlocked(),
                detection.cycles().stream().map(DetectionResult.DetectedCycle::processes).toList(),
                detection.deadlockedProcesses(), violations.isEmpty(), violations,
                graph.getVertices().size(), graph.edgeCount());
    }
}
