package com.deadlocksimulator.services;

import com.deadlocksimulator.dto.request.RecoveryRequest;
import com.deadlocksimulator.dto.response.ComparisonResponse;
import com.deadlocksimulator.dto.response.RecoveryResultResponse;
import com.deadlocksimulator.entities.RecoveryResultEntity;
import com.deadlocksimulator.entities.SimulationRunEntity;
import com.deadlocksimulator.enums.RunStatus;
import com.deadlocksimulator.enums.StrategyType;
import com.deadlocksimulator.models.ComparisonReport;
import com.deadlocksimulator.models.ComparisonSummary;
import com.deadlocksimulator.models.RecoveryContext;
import com.deadlocksimulator.models.ScoreWeights;
import com.deadlocksimulator.models.StrategyEvaluation;
import com.deadlocksimulator.models.SystemState;
import com.deadlocksimulator.models.VictimWeights;
import com.deadlocksimulator.repositories.RecoveryResultRepository;
import com.deadlocksimulator.security.AppUserPrincipal;
import com.deadlocksimulator.simulation.RecoveryEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Loads a run's persisted deadlocked snapshot, checks it against its stored fingerprint, runs the
 * recovery engine on isolated copies and persists every result (one row per run and strategy).
 */
@Service
public class RecoveryService {

    private final AccessService access;
    private final RecoveryResultRepository results;
    private final RecoveryEngine engine;
    private final ResultMapper mapper;
    private final JsonCodec json;

    public RecoveryService(AccessService access, RecoveryResultRepository results, RecoveryEngine engine,
                           ResultMapper mapper, JsonCodec json) {
        this.access = access;
        this.results = results;
        this.engine = engine;
        this.mapper = mapper;
        this.json = json;
    }

    /** Runs a single strategy. Any earlier comparison of this run becomes stale and is cleared. */
    @Transactional
    public RecoveryResultResponse execute(Long runId, StrategyType strategy, RecoveryRequest req,
                                          AppUserPrincipal user) {
        SimulationRunEntity run = access.run(runId, user);
        SystemState snapshot = loadSnapshot(run);
        RecoveryContext ctx = context(snapshot, req);
        ScoreWeights weights = scoreWeights(req);
        StrategyEvaluation eval = engine.evaluate(snapshot, strategy, ctx, weights);
        RecoveryResultEntity saved = persist(run, eval, null, weights, ctx.victimWeights());
        results.findByRunIdOrderByStrategy(runId).forEach(r -> r.setRankPosition(null));
        run.setComparison(null);
        run.setRecommendedStrategy(null);
        run.setStatus(RunStatus.RECOVERY_EXECUTED);
        return mapper.result(saved);
    }

    /** Runs all three strategies on independent deep copies of the same snapshot and ranks them. */
    @Transactional
    public ComparisonResponse compare(Long runId, RecoveryRequest req, AppUserPrincipal user) {
        SimulationRunEntity run = access.run(runId, user);
        SystemState snapshot = loadSnapshot(run);
        RecoveryContext ctx = context(snapshot, req);
        ScoreWeights weights = scoreWeights(req);
        ComparisonReport report = engine.compare(snapshot, ctx, weights);

        Map<StrategyType, Integer> ranks = report.ranking().stream()
                .collect(Collectors.toMap(ComparisonReport.RankingEntry::strategy, ComparisonReport.RankingEntry::rank));
        List<RecoveryResultEntity> saved = report.evaluations().stream()
                .map(e -> persist(run, e, ranks.get(e.strategy()), weights, ctx.victimWeights())).toList();

        ComparisonSummary summary = new ComparisonSummary(report.snapshotFingerprint(), report.snapshotUnchanged(),
                report.copiesIdentical(), report.snapshotFingerprint().equals(run.getSnapshotFingerprint()), weights,
                ctx.victimWeights(), ctx.maxRounds(), report.ranking(), report.recommendation(), report.sensitivity());
        run.setComparison(json.write(summary));
        run.setRecommendedStrategy(report.recommendation().strategy());
        run.setStatus(RunStatus.RECOVERY_EXECUTED);
        return new ComparisonResponse(run.getId(), run.getScenarioName(), summary,
                saved.stream().map(mapper::result).toList());
    }

    /** The latest stored comparison (no recomputation). */
    @Transactional(readOnly = true)
    public ComparisonResponse latestComparison(Long runId, AppUserPrincipal user) {
        SimulationRunEntity run = access.run(runId, user);
        ComparisonSummary summary = json.read(run.getComparison(), ComparisonSummary.class);
        return new ComparisonResponse(run.getId(), run.getScenarioName(), summary,
                results.findByRunIdOrderByStrategy(runId).stream().map(mapper::result).toList());
    }

    private SystemState loadSnapshot(SimulationRunEntity run) {
        SystemState snapshot = json.read(run.getInitialState(), SystemState.class);
        String fp = engine.fingerprint(snapshot);
        if (!fp.equals(run.getSnapshotFingerprint())) {
            throw new IllegalStateException("Stored snapshot of run " + run.getId() + " does not match its fingerprint");
        }
        return snapshot;
    }

    private RecoveryResultEntity persist(SimulationRunEntity run, StrategyEvaluation e, Integer rank,
                                         ScoreWeights weights, VictimWeights victimWeights) {
        RecoveryResultEntity r = results.findByRunIdAndStrategy(run.getId(), e.strategy())
                .orElseGet(() -> new RecoveryResultEntity(run, e.strategy()));
        r.setStatus(e.status());
        r.setHaltReason(e.haltReason());
        r.setVerified(e.verification().verified());
        r.setFinalState(json.write(e.finalState()));
        r.setRounds(json.write(e.rounds()));
        r.setVerification(json.write(e.verification()));
        r.setMetrics(json.write(e.metrics()));
        r.setWorkLost(e.metrics().workLost());
        r.setProcessesTerminated(e.metrics().processesTerminated());
        r.setProcessesRolledBack(e.metrics().processesRolledBack());
        r.setResourcesReclaimed(e.metrics().resourcesReclaimed());
        r.setRecoverySteps(e.metrics().recoverySteps());
        r.setStarvationRisk(e.metrics().starvationRisk());
        r.setAggregateScore(e.metrics().aggregateScore());
        r.setRankPosition(rank);
        r.setScoreWeights(json.write(weights));
        r.setVictimWeights(json.write(victimWeights));
        r.setExecutionMicros(e.executionMicros());
        r.setExecutedAt(Instant.now());
        return results.save(r);
    }

    private static RecoveryContext context(SystemState snapshot, RecoveryRequest req) {
        VictimWeights vw = req.victimWeights() != null ? req.victimWeights() : VictimWeights.defaults();
        int maxRounds = req.maxRounds() != null ? req.maxRounds() : RecoveryContext.defaultMaxRounds(snapshot);
        return new RecoveryContext(vw, maxRounds);
    }

    private static ScoreWeights scoreWeights(RecoveryRequest req) {
        return req.scoreWeights() != null ? req.scoreWeights() : ScoreWeights.defaults();
    }

}
