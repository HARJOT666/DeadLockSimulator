package com.deadlocksimulator.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "scenarios")
public class ScenarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    /** Serialized {@link com.deadlocksimulator.models.ScenarioConfig}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String config;

    @Column(name = "process_count", nullable = false)
    private int processCount;

    @Column(name = "resource_count", nullable = false)
    private int resourceCount;

    @Column(name = "operation_count", nullable = false)
    private int operationCount;

    @Column(name = "checkpoint_interval", nullable = false)
    private int checkpointInterval;

    private Long seed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ScenarioEntity() {
    }

    public ScenarioEntity(User owner) {
        this.owner = owner;
    }

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
    public int getProcessCount() { return processCount; }
    public void setProcessCount(int processCount) { this.processCount = processCount; }
    public int getResourceCount() { return resourceCount; }
    public void setResourceCount(int resourceCount) { this.resourceCount = resourceCount; }
    public int getOperationCount() { return operationCount; }
    public void setOperationCount(int operationCount) { this.operationCount = operationCount; }
    public int getCheckpointInterval() { return checkpointInterval; }
    public void setCheckpointInterval(int checkpointInterval) { this.checkpointInterval = checkpointInterval; }
    public Long getSeed() { return seed; }
    public void setSeed(Long seed) { this.seed = seed; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
