package com.deadlocksimulator.services;

import com.deadlocksimulator.dto.response.GraphResponse;
import com.deadlocksimulator.dto.response.SimulationRunResponse;
import com.deadlocksimulator.entities.RecoveryResultEntity;
import com.deadlocksimulator.entities.ScenarioEntity;
import com.deadlocksimulator.entities.SimulationRunEntity;
import com.deadlocksimulator.enums.RunStatus;
import com.deadlocksimulator.enums.StrategyType;
import com.deadlocksimulator.exceptions.ResourceNotFoundException;
import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.RecoveryAction;
import com.deadlocksimulator.models.RoundTrace;
import com.deadlocksimulator.models.ScenarioConfig;
import com.deadlocksimulator.models.SystemState;
import com.deadlocksimulator.repositories.RecoveryResultRepository;
import com.deadlocksimulator.repositories.SimulationRunRepository;
import com.deadlocksimulator.security.AppUserPrincipal;
import com.deadlocksimulator.simulation.RecoveryEngine;
import com.deadlocksimulator.simulation.SimulationEngine;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Runs a scenario through the deterministic simulator, detects deadlock on the resulting state and
 * persists the snapshot (with its SHA-256 fingerprint) as the immutable starting point for recovery.
 */
@Service
public class SimulationService {

    private final AccessService access;
    private final SimulationRunRepository runs;
    private final RecoveryResultRepository results;
    private final SimulationEngine engine;
    private final RecoveryEngine recoveryEngine;
    private final DeadlockDetectionService detection;
    private final ResultMapper mapper;
    private final JsonCodec json;

    public SimulationService(AccessService access, SimulationRunRepository runs, RecoveryResultRepository results,
                             SimulationEngine engine, RecoveryEngine recoveryEngine,
                             DeadlockDetectionService detection, ResultMapper mapper, JsonCodec json) {
        this.access = access;
        this.runs = runs;
        this.results = results;
        this.engine = engine;
        this.recoveryEngine = recoveryEngine;
        this.detection = detection;
        this.mapper = mapper;
        this.json = json;
    }

    @Transactional
    public SimulationRunResponse start(Long scenarioId, AppUserPrincipal user) {
        ScenarioEntity scenario = access.scenario(scenarioId, user);
        ScenarioConfig config = json.read(scenario.getConfig(), ScenarioConfig.class);
        SystemState snapshot = engine.run(config);
        DetectionResult d = detection.detect(snapshot);

        SimulationRunEntity run = new SimulationRunEntity(scenario.getOwner(), scenario);
        run.setScenarioName(scenario.getName());
        run.setScenarioConfig(scenario.getConfig());
        run.setInitialState(json.write(snapshot));
        run.setSnapshotFingerprint(recoveryEngine.fingerprint(snapshot));
        run.setDetection(json.write(d));
        run.setDeadlocked(d.deadlocked());
        run.setCycleCount(d.cycles().size());
        run.setDeadlockedProcessCount(d.deadlockedProcesses().size());
        run.setStatus(d.deadlocked() ? RunStatus.DEADLOCKED : RunStatus.NO_DEADLOCK);
        return mapper.run(runs.save(run), List.of());
    }

    @Transactional(readOnly = true)
    public SimulationRunResponse get(Long runId, AppUserPrincipal user) {
        SimulationRunEntity run = access.run(runId, user);
        return mapper.run(run, results.findByRunIdOrderByStrategy(runId));
    }

    /** The deadlocked snapshot, or a strategy's final state if {@code strategy} is given. */
    @Transactional(readOnly = true)
    public SystemState state(Long runId, StrategyType strategy, AppUserPrincipal user) {
        SimulationRunEntity run = access.run(runId, user);
        if (strategy == null) {
            return json.read(run.getInitialState(), SystemState.class);
        }
        return json.read(result(runId, strategy).getFinalState(), SystemState.class);
    }

    @Transactional(readOnly = true)
    public GraphResponse graph(Long runId, StrategyType strategy, AppUserPrincipal user) {
        SystemState state = state(runId, strategy, user);
        if (strategy == null) {
            return detection.graph("snapshot", state, List.of());
        }
        List<RoundTrace> rounds = json.read(result(runId, strategy).getRounds(), new TypeReference<>() { });
        List<RecoveryAction> actions = rounds.stream().flatMap(r -> r.actions().stream()).toList();
        return detection.graph(strategy.name(), state, actions);
    }

    @Transactional(readOnly = true)
    public DetectionResult detection(Long runId, AppUserPrincipal user) {
        return json.read(access.run(runId, user).getDetection(), DetectionResult.class);
    }

    private RecoveryResultEntity result(Long runId, StrategyType strategy) {
        return results.findByRunIdAndStrategy(runId, strategy)
                .orElseThrow(() -> new ResourceNotFoundException(strategy + " result for run", runId));
    }
}
