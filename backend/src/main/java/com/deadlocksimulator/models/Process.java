package com.deadlocksimulator.models;

import com.deadlocksimulator.enums.ProcessState;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Mutable simulated process. Every collection is owned exclusively by this instance;
 * {@link com.deadlocksimulator.algorithms.StateCloner} deep-copies all of them.
 */
public class Process {

    private String id;
    /** Higher number = more important = more expensive to pick as a victim. */
    private int priority;
    private int totalWork;
    private int workDone;
    private ProcessState state = ProcessState.RUNNING;
    /** Resources currently held, in acquisition order. */
    private List<String> heldResources = new ArrayList<>();
    /** Resources requested but not yet granted (AND-request semantics). */
    private Set<String> waitingFor = new LinkedHashSet<>();
    /** How many times this process has been chosen as a recovery victim (incl. prior history). */
    private int victimCount;
    private List<Checkpoint> checkpoints = new ArrayList<>();
    /** Operations issued while the process was blocked; executed when it is unblocked. */
    private List<Operation> deferredOperations = new ArrayList<>();

    public Process() {
    }

    public Process(String id, int priority, int totalWork, int workDone) {
        this.id = id;
        this.priority = priority;
        this.totalWork = totalWork;
        this.workDone = workDone;
    }

    public boolean holds(String resourceId) {
        return heldResources.contains(resourceId);
    }

    @JsonIgnore
    public boolean isLive() {
        return state.isLive();
    }

    public Optional<Checkpoint> latestCheckpoint() {
        return checkpoints.isEmpty() ? Optional.empty() : Optional.of(checkpoints.get(checkpoints.size() - 1));
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public int getTotalWork() { return totalWork; }
    public void setTotalWork(int totalWork) { this.totalWork = totalWork; }
    public int getWorkDone() { return workDone; }
    public void setWorkDone(int workDone) { this.workDone = workDone; }
    public ProcessState getState() { return state; }
    public void setState(ProcessState state) { this.state = state; }
    public List<String> getHeldResources() { return heldResources; }
    public void setHeldResources(List<String> heldResources) { this.heldResources = new ArrayList<>(heldResources); }
    public Set<String> getWaitingFor() { return waitingFor; }
    @JsonDeserialize(as = LinkedHashSet.class)
    public void setWaitingFor(Set<String> waitingFor) { this.waitingFor = new LinkedHashSet<>(waitingFor); }
    public int getVictimCount() { return victimCount; }
    public void setVictimCount(int victimCount) { this.victimCount = victimCount; }
    public List<Checkpoint> getCheckpoints() { return checkpoints; }
    public void setCheckpoints(List<Checkpoint> checkpoints) { this.checkpoints = new ArrayList<>(checkpoints); }
    public List<Operation> getDeferredOperations() { return deferredOperations; }
    public void setDeferredOperations(List<Operation> ops) { this.deferredOperations = new ArrayList<>(ops); }
}
