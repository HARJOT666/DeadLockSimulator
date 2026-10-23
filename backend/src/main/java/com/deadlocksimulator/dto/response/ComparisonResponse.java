package com.deadlocksimulator.dto.response;

import com.deadlocksimulator.models.ComparisonSummary;

import java.util.List;

public record ComparisonResponse(Long runId, String scenarioName, ComparisonSummary summary,
                                 List<RecoveryResultResponse> results) {
}
