package com.deadlocksimulator.controllers;

import com.deadlocksimulator.dto.request.StartSimulationRequest;
import com.deadlocksimulator.dto.response.GraphResponse;
import com.deadlocksimulator.dto.response.RecoveryResultResponse;
import com.deadlocksimulator.dto.response.SimulationRunResponse;
import com.deadlocksimulator.enums.StrategyType;
import com.deadlocksimulator.models.SystemState;
import com.deadlocksimulator.security.AppUserPrincipal;
import com.deadlocksimulator.services.MetricsService;
import com.deadlocksimulator.services.SimulationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationService simulationService;
    private final MetricsService metricsService;

    public SimulationController(SimulationService simulationService, MetricsService metricsService) {
        this.simulationService = simulationService;
        this.metricsService = metricsService;
    }

    /** Runs the scenario, detects deadlock and persists the snapshot. */
    @PostMapping("/start")
    public ResponseEntity<SimulationRunResponse> start(@Valid @RequestBody StartSimulationRequest req,
                                                       @AuthenticationPrincipal AppUserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(simulationService.start(req.scenarioId(), user));
    }

    @GetMapping("/{id}")
    public SimulationRunResponse get(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        return simulationService.get(id, user);
    }

    /** Deadlocked snapshot, or the final state of {@code strategy}. */
    @GetMapping("/{id}/state")
    public SystemState state(@PathVariable Long id, @RequestParam(required = false) StrategyType strategy,
                             @AuthenticationPrincipal AppUserPrincipal user) {
        return simulationService.state(id, strategy, user);
    }

    /** Wait-For Graph of the snapshot, or of {@code strategy}'s final state (before/after comparison). */
    @GetMapping("/{id}/graph")
    public GraphResponse graph(@PathVariable Long id, @RequestParam(required = false) StrategyType strategy,
                               @AuthenticationPrincipal AppUserPrincipal user) {
        return simulationService.graph(id, strategy, user);
    }

    @GetMapping("/{id}/results")
    public List<RecoveryResultResponse> results(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        return metricsService.metrics(id, user);
    }
}
