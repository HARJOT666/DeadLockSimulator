package com.deadlocksimulator.services;

import com.deadlocksimulator.entities.ScenarioEntity;
import com.deadlocksimulator.entities.SimulationRunEntity;
import com.deadlocksimulator.exceptions.ResourceNotFoundException;
import com.deadlocksimulator.repositories.ScenarioRepository;
import com.deadlocksimulator.repositories.SimulationRunRepository;
import com.deadlocksimulator.security.AppUserPrincipal;
import org.springframework.stereotype.Service;

/**
 * Central ownership enforcement. Users can only load their own scenarios and runs; ADMINs can load
 * any. Foreign objects produce 404 (not 403) so their existence is not disclosed.
 */
@Service
public class AccessService {

    private final ScenarioRepository scenarios;
    private final SimulationRunRepository runs;

    public AccessService(ScenarioRepository scenarios, SimulationRunRepository runs) {
        this.scenarios = scenarios;
        this.runs = runs;
    }

    public ScenarioEntity scenario(Long id, AppUserPrincipal user) {
        return (user.isAdmin() ? scenarios.findById(id) : scenarios.findByIdAndOwnerId(id, user.id()))
                .orElseThrow(() -> new ResourceNotFoundException("Scenario", id));
    }

    public SimulationRunEntity run(Long id, AppUserPrincipal user) {
        return (user.isAdmin() ? runs.findById(id) : runs.findByIdAndOwnerId(id, user.id()))
                .orElseThrow(() -> new ResourceNotFoundException("Simulation run", id));
    }
}
