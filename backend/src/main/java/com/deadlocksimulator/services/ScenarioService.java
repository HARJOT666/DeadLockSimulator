package com.deadlocksimulator.services;

import com.deadlocksimulator.dto.request.ScenarioRequest;
import com.deadlocksimulator.dto.response.ScenarioResponse;
import com.deadlocksimulator.dto.response.TemplateResponse;
import com.deadlocksimulator.entities.ScenarioEntity;
import com.deadlocksimulator.entities.User;
import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.GeneratorParams;
import com.deadlocksimulator.models.ScenarioConfig;
import com.deadlocksimulator.repositories.ScenarioRepository;
import com.deadlocksimulator.repositories.SimulationRunRepository;
import com.deadlocksimulator.repositories.UserRepository;
import com.deadlocksimulator.security.AppUserPrincipal;
import com.deadlocksimulator.simulation.ScenarioGenerator;
import com.deadlocksimulator.simulation.ScenarioTemplates;
import com.deadlocksimulator.simulation.SimulationEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScenarioService {

    private final ScenarioRepository scenarios;
    private final SimulationRunRepository runs;
    private final UserRepository users;
    private final AccessService access;
    private final SimulationEngine engine;
    private final ScenarioTemplates templates;
    private final ScenarioGenerator generator;
    private final DeadlockDetectionService detection;
    private final JsonCodec json;

    public ScenarioService(ScenarioRepository scenarios, SimulationRunRepository runs, UserRepository users,
                           AccessService access, SimulationEngine engine, ScenarioTemplates templates,
                           ScenarioGenerator generator, DeadlockDetectionService detection, JsonCodec json) {
        this.scenarios = scenarios;
        this.runs = runs;
        this.users = users;
        this.access = access;
        this.engine = engine;
        this.templates = templates;
        this.generator = generator;
        this.detection = detection;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public List<ScenarioResponse> list(AppUserPrincipal user) {
        return scenarios.findByOwnerIdOrderByCreatedAtDesc(user.id()).stream().map(s -> toResponse(s, false)).toList();
    }

    @Transactional(readOnly = true)
    public ScenarioResponse get(Long id, AppUserPrincipal user) {
        return toResponse(access.scenario(id, user), true);
    }

    @Transactional
    public ScenarioResponse create(ScenarioRequest req, AppUserPrincipal principal) {
        User owner = users.getReferenceById(principal.id());
        ScenarioEntity entity = new ScenarioEntity(owner);
        apply(entity, req);
        return toResponse(scenarios.save(entity), true);
    }

    @Transactional
    public ScenarioResponse update(Long id, ScenarioRequest req, AppUserPrincipal user) {
        ScenarioEntity entity = access.scenario(id, user);
        apply(entity, req);
        return toResponse(scenarios.saveAndFlush(entity), true);
    }

    @Transactional
    public void delete(Long id, AppUserPrincipal user) {
        scenarios.delete(access.scenario(id, user));
    }

    public List<TemplateResponse> templates() {
        return templates.all().stream().map(t -> new TemplateResponse(t.key(), t.name(), t.description(), t.config(),
                detection.detect(engine.run(t.config())))).toList();
    }

    /** Deterministic preview; nothing is persisted until the user saves the scenario. */
    public TemplateResponse generate(GeneratorParams params) {
        ScenarioConfig config = generator.generate(params);
        return new TemplateResponse("generated", "Generated scenario (seed " + params.seed() + ")",
                "Seeded scenario: " + params.cycleCount() + " cycle(s) of length " + params.cycleLength() + ", "
                        + params.workDistribution() + " work in [" + params.minWork() + ", " + params.maxWork()
                        + "], checkpoint interval " + params.checkpointInterval() + ", resource density "
                        + params.resourceDensity() + ".",
                config, detection.detect(engine.run(config)));
    }

    /** Runs an unsaved configuration and returns its detection result (nothing is persisted). */
    public DetectionResult preview(ScenarioConfig config) {
        return detection.detect(engine.run(config));
    }

    /** Validates structurally AND by a dry run, so only executable scenarios are ever stored. */
    private void apply(ScenarioEntity entity, ScenarioRequest req) {
        ScenarioConfig config = req.config();
        engine.run(config);
        entity.setName(req.name().trim());
        entity.setDescription(req.description());
        entity.setConfig(json.write(config));
        entity.setProcessCount(config.processes().size());
        entity.setResourceCount(config.resources().size());
        entity.setOperationCount(config.requestSequence().size());
        entity.setCheckpointInterval(config.checkpointInterval());
        entity.setSeed(config.seed());
    }

    private ScenarioResponse toResponse(ScenarioEntity s, boolean withConfig) {
        long runCount = s.getId() == null ? 0 : runs.countByScenarioId(s.getId());
        return new ScenarioResponse(s.getId(), s.getName(), s.getDescription(), s.getProcessCount(),
                s.getResourceCount(), s.getOperationCount(), s.getCheckpointInterval(), s.getSeed(), runCount,
                s.getCreatedAt(), s.getUpdatedAt(), withConfig ? json.read(s.getConfig(), ScenarioConfig.class) : null);
    }
}
