package com.deadlocksimulator.dto.request;

import com.deadlocksimulator.models.ScenarioConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScenarioRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2000) String description,
        @NotNull @Valid ScenarioConfig config) {
}
