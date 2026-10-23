package com.deadlocksimulator.dto.request;

import jakarta.validation.constraints.NotNull;

public record StartSimulationRequest(@NotNull Long scenarioId) {
}
