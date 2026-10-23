package com.deadlocksimulator.entities;

import com.deadlocksimulator.enums.RunStatus;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "simulation_runs")
public class SimulationRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /** Null if the scenario was deleted later; the run stays reproducible via {@link #scenarioConfig}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id")
    private ScenarioEntity scenario;

    @Column(name = "scenario_name", nullable = false, length = 120)
    private String scenarioName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scenario_config", nullable = false, columnDefinition = "jsonb")
    private String scenarioConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "initial_state", nullable = false, columnDefinition = "jsonb")
    private String initialState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String detection;

    @Column(name = "snapshot_fingerprint", nullable = false, length = 64)
    private String snapshotFingerprint;

    @Column(nullable = false)
    private boolean deadlocked;

    @Column(name = "cycle_count", nullable = false)
    private int cycleCount;

    @Column(name = "deadlocked_process_count", nullable = false)
    private int deadlockedProcessCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RunStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String comparison;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_strategy", length = 20)
    private StrategyType recommendedStrategy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SimulationRunEntity() {
    }

    public SimulationRunEntity(User owner, ScenarioEntity scenario) {
        this.owner = owner;
        this.scenario = scenario;
    }

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public ScenarioEntity getScenario() { return scenario; }
    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    public String getScenarioConfig() { return scenarioConfig; }
    public void setScenarioConfig(String scenarioConfig) { this.scenarioConfig = scenarioConfig; }
    public String getInitialState() { return initialState; }
    public void setInitialState(String initialState) { this.initialState = initialState; }
    public String getDetection() { return detection; }
    public void setDetection(String detection) { this.detection = detection; }
    public String getSnapshotFingerprint() { return snapshotFingerprint; }
    public void setSnapshotFingerprint(String snapshotFingerprint) { this.snapshotFingerprint = snapshotFingerprint; }
    public boolean isDeadlocked() { return deadlocked; }
    public void setDeadlocked(boolean deadlocked) { this.deadlocked = deadlocked; }
    public int getCycleCount() { return cycleCount; }
    public void setCycleCount(int cycleCount) { this.cycleCount = cycleCount; }
    public int getDeadlockedProcessCount() { return deadlockedProcessCount; }
    public void setDeadlockedProcessCount(int n) { this.deadlockedProcessCount = n; }
    public RunStatus getStatus() { return status; }
    public void setStatus(RunStatus status) { this.status = status; }
    public String getComparison() { return comparison; }
    public void setComparison(String comparison) { this.comparison = comparison; }
    public StrategyType getRecommendedStrategy() { return recommendedStrategy; }
    public void setRecommendedStrategy(StrategyType s) { this.recommendedStrategy = s; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
