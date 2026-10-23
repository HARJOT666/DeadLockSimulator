package com.deadlocksimulator.controllers;

import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.security.AppUserPrincipal;
import com.deadlocksimulator.services.SimulationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deadlock")
public class DeadlockController {

    private final SimulationService simulationService;

    public DeadlockController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    /** DFS detection result of the run's snapshot: cycles, deadlocked set, blocked processes. */
    @GetMapping("/{simulationId}")
    public DetectionResult detection(@PathVariable Long simulationId, @AuthenticationPrincipal AppUserPrincipal user) {
        return simulationService.detection(simulationId, user);
    }
}
