package com.deadlocksimulator.dto.response;

import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.ScenarioConfig;

/** A built-in example scenario plus a preview of the deadlock it produces. */
public record TemplateResponse(String key, String name, String description, ScenarioConfig config,
                               DetectionResult preview) {
}
