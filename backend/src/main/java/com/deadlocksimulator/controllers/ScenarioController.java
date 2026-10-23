package com.deadlocksimulator.controllers;

import com.deadlocksimulator.dto.request.ScenarioRequest;
import com.deadlocksimulator.dto.response.ScenarioResponse;
import com.deadlocksimulator.dto.response.TemplateResponse;
import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.GeneratorParams;
import com.deadlocksimulator.models.ScenarioConfig;
import com.deadlocksimulator.security.AppUserPrincipal;
import com.deadlocksimulator.services.ScenarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioService scenarioService;

    public ScenarioController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @PostMapping
    public ResponseEntity<ScenarioResponse> create(@Valid @RequestBody ScenarioRequest req,
                                                   @AuthenticationPrincipal AppUserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scenarioService.create(req, user));
    }

    @GetMapping
    public List<ScenarioResponse> list(@AuthenticationPrincipal AppUserPrincipal user) {
        return scenarioService.list(user);
    }

    /** Built-in reproducible examples, each with a detection preview. */
    @GetMapping("/templates")
    public List<TemplateResponse> templates() {
        return scenarioService.templates();
    }

    /** Seeded generator preview (not persisted). */
    @PostMapping("/generate")
    public TemplateResponse generate(@Valid @RequestBody GeneratorParams params) {
        return scenarioService.generate(params);
    }

    /** Dry run of an unsaved configuration: simulate + detect, nothing persisted. */
    @PostMapping("/preview")
    public DetectionResult preview(@Valid @RequestBody ScenarioConfig config) {
        return scenarioService.preview(config);
    }

    @GetMapping("/{id}")
    public ScenarioResponse get(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        return scenarioService.get(id, user);
    }

    @PutMapping("/{id}")
    public ScenarioResponse update(@PathVariable Long id, @Valid @RequestBody ScenarioRequest req,
                                   @AuthenticationPrincipal AppUserPrincipal user) {
        return scenarioService.update(id, req, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        scenarioService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
