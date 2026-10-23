package com.deadlocksimulator.simulation;

import com.deadlocksimulator.algorithms.DfsCycleDetector;
import com.deadlocksimulator.algorithms.MetricsCalculator;
import com.deadlocksimulator.algorithms.RecoveryVerifier;
import com.deadlocksimulator.algorithms.StateCloner;
import com.deadlocksimulator.algorithms.StrategyRanker;
import com.deadlocksimulator.algorithms.WaitForGraphBuilder;
import com.deadlocksimulator.enums.HaltReason;
import com.deadlocksimulator.enums.RecoveryStatus;
import com.deadlocksimulator.enums.StrategyType;
import com.deadlocksimulator.models.ComparisonReport;
import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.RecoveryContext;
import com.deadlocksimulator.models.RecoveryMetrics;
import com.deadlocksimulator.models.RecoveryOutcome;
import com.deadlocksimulator.models.ScoreWeights;
import com.deadlocksimulator.models.StrategyEvaluation;
import com.deadlocksimulator.models.SystemState;
import com.deadlocksimulator.models.VerificationResult;
import com.deadlocksimulator.strategies.RecoveryStrategy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Pure (database-free) recovery pipeline:
 * <pre>
 * deadlocked snapshot ──deepCopy──► copy A ──► TerminationStrategy ─┐
 *                     ──deepCopy──► copy B ──► RollbackStrategy    ─┼─► verify (rebuild WFG + DFS
 *                     ──deepCopy──► copy C ──► TotalAbortStrategy  ─┘    + invariants) ─► metrics ─► rank
 * </pre>
 * The snapshot itself is never handed to a strategy. Its SHA-256 fingerprint is taken before and
 * after, and each copy's fingerprint is checked before execution, to prove state isolation.
 */
@Component
public class RecoveryEngine {

    private final Map<StrategyType, RecoveryStrategy> strategies = new EnumMap<>(StrategyType.class);
    private final StateCloner cloner;
    private final WaitForGraphBuilder graphBuilder;
    private final DfsCycleDetector detector;
    private final RecoveryVerifier verifier;
    private final MetricsCalculator metricsCalculator;
    private final StrategyRanker ranker;
    private final ObjectMapper objectMapper;

    public RecoveryEngine(List<RecoveryStrategy> strategyBeans, StateCloner cloner, WaitForGraphBuilder graphBuilder,
                          DfsCycleDetector detector, RecoveryVerifier verifier, MetricsCalculator metricsCalculator,
                          StrategyRanker ranker, ObjectMapper objectMapper) {
        strategyBeans.forEach(s -> strategies.put(s.type(), s));
        this.cloner = cloner;
        this.graphBuilder = graphBuilder;
        this.detector = detector;
        this.verifier = verifier;
        this.metricsCalculator = metricsCalculator;
        this.ranker = ranker;
        this.objectMapper = objectMapper;
    }

    /** Runs one strategy on its own deep copy of {@code snapshot}. */
    public StrategyEvaluation evaluate(SystemState snapshot, StrategyType type, RecoveryContext context,
                                       ScoreWeights weights) {
        DetectionResult initial = detector.detect(graphBuilder.build(snapshot));
        MetricsCalculator.Normalisers norm = MetricsCalculator.Normalisers.of(snapshot, initial);
        return run(cloner.deepCopy(snapshot), type, initial, norm, context, weights);
    }

    /** Runs all three strategies on independent copies and ranks them. */
    public ComparisonReport compare(SystemState snapshot, RecoveryContext context, ScoreWeights weights) {
        String before = fingerprint(snapshot);
        DetectionResult initial = detector.detect(graphBuilder.build(snapshot));
        MetricsCalculator.Normalisers norm = MetricsCalculator.Normalisers.of(snapshot, initial);

        // Create all copies up front so every strategy demonstrably starts from identical data.
        Map<StrategyType, SystemState> copies = new EnumMap<>(StrategyType.class);
        for (StrategyType t : StrategyType.values()) {
            copies.put(t, cloner.deepCopy(snapshot));
        }
        boolean copiesIdentical = copies.values().stream().allMatch(c -> fingerprint(c).equals(before));

        List<StrategyEvaluation> evaluations = new ArrayList<>();
        for (StrategyType t : StrategyType.values()) {
            evaluations.add(run(copies.get(t), t, initial, norm, context, weights));
        }
        boolean unchanged = fingerprint(snapshot).equals(before);

        var sensitivity = ranker.sensitivity(evaluations, w -> rescore(evaluations, norm, w));
        return new ComparisonReport(before, unchanged, copiesIdentical, weights, context.victimWeights(),
                evaluations, ranker.rank(evaluations), ranker.recommend(evaluations, sensitivity), sensitivity);
    }

    private StrategyEvaluation run(SystemState copy, StrategyType type, DetectionResult initial,
                                   MetricsCalculator.Normalisers norm, RecoveryContext context, ScoreWeights weights) {
        long start = System.nanoTime();
        RecoveryOutcome outcome = strategies.get(type).recover(copy, context);
        long micros = (System.nanoTime() - start) / 1000;
        VerificationResult verification = verifier.verify(outcome.finalState());
        RecoveryStatus status = status(initial, outcome, verification);
        RecoveryMetrics metrics = metricsCalculator.calculate(outcome, norm, weights);
        return new StrategyEvaluation(type, status, outcome.haltReason(), outcome.rounds(), verification, metrics,
                outcome.finalState(), micros);
    }

    /** Success is declared only from the independent verification, never from "no exception". */
    static RecoveryStatus status(DetectionResult initial, RecoveryOutcome outcome, VerificationResult v) {
        if (!initial.deadlocked()) {
            return v.consistent() ? RecoveryStatus.NOT_REQUIRED : RecoveryStatus.FAILED;
        }
        if (v.verified() && outcome.haltReason() == HaltReason.DEADLOCK_RESOLVED) {
            return RecoveryStatus.RECOVERED;
        }
        if (!v.consistent() || outcome.rounds().isEmpty()) {
            return RecoveryStatus.FAILED;
        }
        return RecoveryStatus.PARTIAL;
    }

    private Map<StrategyType, Double> rescore(List<StrategyEvaluation> evaluations, MetricsCalculator.Normalisers norm,
                                              ScoreWeights w) {
        Map<StrategyType, Double> scores = new EnumMap<>(StrategyType.class);
        for (StrategyEvaluation e : evaluations) {
            RecoveryOutcome o = new RecoveryOutcome(e.strategy(), e.finalState(), e.rounds(), e.haltReason());
            scores.put(e.strategy(), metricsCalculator.calculate(o, norm, w).aggregateScore());
        }
        return scores;
    }

    public String fingerprint(SystemState state) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(state);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot fingerprint state", e);
        }
    }

    public DetectionResult detect(SystemState state) {
        return detector.detect(graphBuilder.build(state));
    }
}
