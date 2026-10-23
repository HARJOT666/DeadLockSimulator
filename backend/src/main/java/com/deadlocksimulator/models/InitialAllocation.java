package com.deadlocksimulator.models;

import jakarta.validation.constraints.NotBlank;

/** A resource held by a process before the request sequence starts. */
public record InitialAllocation(@NotBlank String resourceId, @NotBlank String processId) {
}
