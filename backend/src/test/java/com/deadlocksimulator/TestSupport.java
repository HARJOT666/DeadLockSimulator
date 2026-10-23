package com.deadlocksimulator;

import com.deadlocksimulator.algorithms.DfsCycleDetector;
import com.deadlocksimulator.algorithms.MetricsCalculator;
import com.deadlocksimulator.algorithms.RecoveryVerifier;
import com.deadlocksimulator.algorithms.StateCloner;
import com.deadlocksimulator.algorithms.StateConsistencyChecker;
import com.deadlocksimulator.algorithms.StrategyRanker;
import com.deadlocksimulator.algorithms.VictimSelector;
import com.deadlocksimulator.algorithms.WaitForGraphBuilder;
import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.ScenarioConfig;
import com.deadlocksimulator.models.SystemState;
import com.deadlocksimulator.simulation.RecoveryEngine;
import com.deadlocksimulator.simulation.ScenarioValidator;
import com.deadlocksimulator.simulation.SimulationEngine;
import com.deadlocksimulator.strategies.RollbackStrategy;
import com.deadlocksimulator.strategies.TerminationStrategy;
import com.deadlocksimulator.strategies.TotalAbortStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/** Wires the pure algorithm components without a Spring context. */
public final class TestSupport {

    public static final WaitForGraphBuilder GRAPH = new WaitForGraphBuilder();
    public static final DfsCycleDetector DFS = new DfsCycleDetector();
    public static final StateCloner CLONER = new StateCloner();
    public static final StateConsistencyChecker CONSISTENCY = new StateConsistencyChecker();
    public static final VictimSelector SELECTOR = new VictimSelector();
    public static final SimulationEngine SIMULATOR = new SimulationEngine(new ScenarioValidator());
    public static final TerminationStrategy TERMINATION = new TerminationStrategy(GRAPH, DFS, SELECTOR);
    public static final RollbackStrategy ROLLBACK = new RollbackStrategy(GRAPH, DFS, SELECTOR);
    public static final TotalAbortStrategy TOTAL_ABORT = new TotalAbortStrategy(GRAPH, DFS);
    public static final RecoveryVerifier VERIFIER = new RecoveryVerifier(GRAPH, DFS, CONSISTENCY);
    public static final MetricsCalculator METRICS = new MetricsCalculator();
    public static final ObjectMapper MAPPER = new ObjectMapper();
    public static final RecoveryEngine ENGINE = new RecoveryEngine(List.of(TERMINATION, ROLLBACK, TOTAL_ABORT),
            CLONER, GRAPH, DFS, VERIFIER, METRICS, new StrategyRanker(), MAPPER);

    private TestSupport() {
    }

    public static SystemState simulate(ScenarioConfig config) {
        return SIMULATOR.run(config);
    }

    public static DetectionResult detect(SystemState state) {
        return DFS.detect(GRAPH.build(state));
    }
}
