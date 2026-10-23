package com.deadlocksimulator.entities;

import com.deadlocksimulator.enums.HaltReason;
import com.deadlocksimulator.enums.RecoveryStatus;
import com.deadlocksimulator.enums.StrategyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "recovery_results")
public class RecoveryResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private SimulationRunEntity run;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StrategyType strategy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecoveryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "halt_reason", nullable = false, length = 30)
    private HaltReason haltReason;

    @Column(nullable = false)
    private boolean verified;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "final_state", nullable = false, columnDefinition = "jsonb")
    private String finalState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String rounds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String verification;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String metrics;

    @Column(name = "work_lost", nullable = false)
    private int workLost;
    @Column(name = "processes_terminated", nullable = false)
    private int processesTerminated;
    @Column(name = "processes_rolled_back", nullable = false)
    private int processesRolledBack;
    @Column(name = "resources_reclaimed", nullable = false)
    private int resourcesReclaimed;
    @Column(name = "recovery_steps", nullable = false)
    private int recoverySteps;
    @Column(name = "starvation_risk", nullable = false)
    private int starvationRisk;
    @Column(name = "aggregate_score", nullable = false)
    private double aggregateScore;
    @Column(name = "rank_position")
    private Integer rankPosition;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "score_weights", nullable = false, columnDefinition = "jsonb")
    private String scoreWeights;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "victim_weights", nullable = false, columnDefinition = "jsonb")
    private String victimWeights;

    @Column(name = "execution_micros", nullable = false)
    private long executionMicros;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    protected RecoveryResultEntity() {
    }

    public RecoveryResultEntity(SimulationRunEntity run, StrategyType strategy) {
        this.run = run;
        this.strategy = strategy;
    }

    public Long getId() { return id; }
    public SimulationRunEntity getRun() { return run; }
    public StrategyType getStrategy() { return strategy; }
    public RecoveryStatus getStatus() { return status; }
    public void setStatus(RecoveryStatus status) { this.status = status; }
    public HaltReason getHaltReason() { return haltReason; }
    public void setHaltReason(HaltReason haltReason) { this.haltReason = haltReason; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public String getFinalState() { return finalState; }
    public void setFinalState(String finalState) { this.finalState = finalState; }
    public String getRounds() { return rounds; }
    public void setRounds(String rounds) { this.rounds = rounds; }
    public String getVerification() { return verification; }
    public void setVerification(String verification) { this.verification = verification; }
    public String getMetrics() { return metrics; }
    public void setMetrics(String metrics) { this.metrics = metrics; }
    public int getWorkLost() { return workLost; }
    public void setWorkLost(int workLost) { this.workLost = workLost; }
    public int getProcessesTerminated() { return processesTerminated; }
    public void setProcessesTerminated(int v) { this.processesTerminated = v; }
    public int getProcessesRolledBack() { return processesRolledBack; }
    public void setProcessesRolledBack(int v) { this.processesRolledBack = v; }
    public int getResourcesReclaimed() { return resourcesReclaimed; }
    public void setResourcesReclaimed(int v) { this.resourcesReclaimed = v; }
    public int getRecoverySteps() { return recoverySteps; }
    public void setRecoverySteps(int v) { this.recoverySteps = v; }
    public int getStarvationRisk() { return starvationRisk; }
    public void setStarvationRisk(int v) { this.starvationRisk = v; }
    public double getAggregateScore() { return aggregateScore; }
    public void setAggregateScore(double v) { this.aggregateScore = v; }
    public Integer getRankPosition() { return rankPosition; }
    public void setRankPosition(Integer rankPosition) { this.rankPosition = rankPosition; }
    public String getScoreWeights() { return scoreWeights; }
    public void setScoreWeights(String scoreWeights) { this.scoreWeights = scoreWeights; }
    public String getVictimWeights() { return victimWeights; }
    public void setVictimWeights(String victimWeights) { this.victimWeights = victimWeights; }
    public long getExecutionMicros() { return executionMicros; }
    public void setExecutionMicros(long executionMicros) { this.executionMicros = executionMicros; }
    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }
}
