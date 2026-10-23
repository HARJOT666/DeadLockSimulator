package com.deadlocksimulator.services;

import com.deadlocksimulator.dto.response.HistoryItemResponse;
import com.deadlocksimulator.dto.response.RecoveryResultResponse;
import com.deadlocksimulator.dto.response.SimulationRunResponse;
import com.deadlocksimulator.entities.RecoveryResultEntity;
import com.deadlocksimulator.entities.SimulationRunEntity;
import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.RecoveryMetrics;
import com.deadlocksimulator.models.RoundTrace;
import com.deadlocksimulator.models.ScenarioConfig;
import com.deadlocksimulator.models.ScoreWeights;
import com.deadlocksimulator.models.VerificationResult;
import com.deadlocksimulator.models.VictimWeights;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

/** Entity → response DTO conversion (shared by several services). */
@Component
public class ResultMapper {

    private static final TypeReference<List<RoundTrace>> ROUNDS = new TypeReference<>() { };
    private final JsonCodec json;

    public ResultMapper(JsonCodec json) {
        this.json = json;
    }

    public RecoveryResultResponse result(RecoveryResultEntity e) {
        return new RecoveryResultResponse(e.getId(), e.getStrategy(), e.getStrategy().getDisplayName(), e.getStatus(),
                e.getHaltReason(), e.isVerified(), e.getRankPosition(), json.read(e.getRounds(), ROUNDS),
                json.read(e.getVerification(), VerificationResult.class), json.read(e.getMetrics(), RecoveryMetrics.class),
                json.read(e.getScoreWeights(), ScoreWeights.class), json.read(e.getVictimWeights(), VictimWeights.class),
                e.getExecutionMicros(), e.getExecutedAt());
    }

    public SimulationRunResponse run(SimulationRunEntity r, List<RecoveryResultEntity> results) {
        return new SimulationRunResponse(r.getId(), r.getScenario() == null ? null : r.getScenario().getId(),
                r.getScenarioName(), r.getStatus(), r.isDeadlocked(), r.getCycleCount(), r.getDeadlockedProcessCount(),
                r.getSnapshotFingerprint(), json.read(r.getDetection(), DetectionResult.class),
                r.getRecommendedStrategy(), results.stream().map(RecoveryResultEntity::getStrategy).sorted().toList(),
                json.read(r.getScenarioConfig(), ScenarioConfig.class), r.getCreatedAt());
    }

    public HistoryItemResponse history(SimulationRunEntity r, List<RecoveryResultEntity> results) {
        return new HistoryItemResponse(r.getId(), r.getScenario() == null ? null : r.getScenario().getId(),
                r.getScenarioName(), r.getCreatedAt(), r.getStatus(), r.isDeadlocked(), r.getCycleCount(),
                r.getDeadlockedProcessCount(), r.getRecommendedStrategy(),
                results.stream().sorted((a, b) -> a.getStrategy().compareTo(b.getStrategy()))
                        .map(e -> new HistoryItemResponse.StrategySummary(e.getStrategy(), e.getStatus(),
                                e.getRankPosition(), e.getAggregateScore(), e.getWorkLost(), e.getProcessesTerminated(),
                                e.getProcessesRolledBack(), e.getResourcesReclaimed(), e.getRecoverySteps(),
                                e.getStarvationRisk())).toList());
    }
}
