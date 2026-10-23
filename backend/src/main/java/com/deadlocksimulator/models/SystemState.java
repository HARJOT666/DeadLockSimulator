package com.deadlocksimulator.models;

import com.deadlocksimulator.enums.ProcessState;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete mutable state of the simulated system (single-instance resource model).
 *
 * <p>All ownership / wait-queue mutations go through the methods of this class so that
 * the simulator and every recovery strategy keep the same invariants:
 * <ul>
 *   <li>{@code resource.holder == p} iff {@code p.heldResources} contains the resource;</li>
 *   <li>{@code p} is in {@code resource.waitQueue} iff {@code p.waitingFor} contains the resource;</li>
 *   <li>a process is WAITING iff it has at least one pending request;</li>
 *   <li>COMPLETED / TERMINATED processes hold nothing and wait for nothing.</li>
 * </ul>
 */
public class SystemState {

    private Map<String, Process> processes = new LinkedHashMap<>();
    private Map<String, Resource> resources = new LinkedHashMap<>();
    private int checkpointInterval;
    private int clock;
    private List<SimulationEvent> events = new ArrayList<>();
    private List<VictimRecord> victimHistory = new ArrayList<>();

    // ------------------------------------------------------------------ lookups

    public Process process(String id) {
        Process p = processes.get(id);
        if (p == null) {
            throw new IllegalArgumentException("Unknown process " + id);
        }
        return p;
    }

    public Resource resource(String id) {
        Resource r = resources.get(id);
        if (r == null) {
            throw new IllegalArgumentException("Unknown resource " + id);
        }
        return r;
    }

    @JsonIgnore
    public List<Process> liveProcesses() {
        return processes.values().stream().filter(Process::isLive).toList();
    }

    // ------------------------------------------------------------------ mutations

    public int tick() {
        return ++clock;
    }

    public void log(String type, String processId, String resourceId, String message) {
        events.add(new SimulationEvent(clock, type, processId, resourceId, message));
    }

    /**
     * AND-request: every free resource is granted immediately, every busy one is queued.
     * The process blocks (WAITING) while at least one request is pending.
     */
    public void request(String processId, List<String> resourceIds) {
        Process p = process(processId);
        for (String rid : resourceIds) {
            Resource r = resource(rid);
            if (processId.equals(r.getHolder())) {
                throw new IllegalStateException(processId + " requested " + rid + " which it already holds");
            }
            if (p.getWaitingFor().contains(rid)) {
                continue;
            }
            if (r.isFree() && r.getWaitQueue().isEmpty()) {
                grant(p, r);
            } else {
                r.getWaitQueue().add(processId);
                p.getWaitingFor().add(rid);
                log("BLOCK", processId, rid, processId + " waits for " + rid
                        + (r.getHolder() != null ? " (held by " + r.getHolder() + ")" : ""));
            }
        }
        if (!p.getWaitingFor().isEmpty()) {
            p.setState(ProcessState.WAITING);
        }
    }

    /**
     * Releases one resource held by {@code processId} and hands it to the first live waiter.
     *
     * @return id of the process that received the resource, or null if it became free
     */
    public String release(String processId, String resourceId) {
        Process p = process(processId);
        Resource r = resource(resourceId);
        if (!processId.equals(r.getHolder())) {
            throw new IllegalStateException(processId + " cannot release " + resourceId + " (not the holder)");
        }
        p.getHeldResources().remove(resourceId);
        r.setHolder(null);
        log("RELEASE", processId, resourceId, processId + " releases " + resourceId);
        return grantToNextWaiter(r);
    }

    /** Releases everything the process holds, in acquisition order. Returns released ids. */
    public List<String> releaseAll(String processId) {
        List<String> released = new ArrayList<>(process(processId).getHeldResources());
        for (String rid : released) {
            release(processId, rid);
        }
        return released;
    }

    /** Withdraws all pending requests of a process (removes it from every wait queue). */
    public void cancelPendingRequests(String processId) {
        Process p = process(processId);
        for (String rid : p.getWaitingFor()) {
            resource(rid).getWaitQueue().remove(processId);
        }
        p.getWaitingFor().clear();
    }

    /**
     * Kills a process: pending requests are withdrawn first (so it can never be granted
     * anything again), then every held resource is released to the next waiter.
     *
     * @return resources released by the termination
     */
    public List<String> terminate(String processId) {
        Process p = process(processId);
        cancelPendingRequests(processId);
        p.setState(ProcessState.TERMINATED);
        p.getDeferredOperations().clear();
        return releaseAll(processId);
    }

    /**
     * Terminates a set of processes atomically. Phase 1 withdraws every member's pending requests
     * and marks it TERMINATED; phase 2 releases their resources. Doing it in two phases guarantees
     * that a resource freed by one member is never handed to another member of the same set.
     *
     * @return released resources per process, in the given order
     */
    public Map<String, List<String>> terminateAll(List<String> processIds) {
        for (String id : processIds) {
            cancelPendingRequests(id);
            process(id).setState(ProcessState.TERMINATED);
            process(id).getDeferredOperations().clear();
        }
        Map<String, List<String>> released = new LinkedHashMap<>();
        for (String id : processIds) {
            released.put(id, releaseAll(id));
        }
        return released;
    }

    private void grant(Process p, Resource r) {
        r.setHolder(p.getId());
        p.getHeldResources().add(r.getId());
        log("GRANT", p.getId(), r.getId(), r.getId() + " granted to " + p.getId());
    }

    private String grantToNextWaiter(Resource r) {
        while (!r.getWaitQueue().isEmpty()) {
            String next = r.getWaitQueue().remove(0);
            Process waiter = process(next);
            waiter.getWaitingFor().remove(r.getId());
            if (!waiter.isLive()) {
                continue; // defensive: dead processes are normally removed from queues already
            }
            grant(waiter, r);
            if (waiter.getWaitingFor().isEmpty() && waiter.getState() == ProcessState.WAITING) {
                waiter.setState(ProcessState.RUNNING);
                log("UNBLOCK", next, r.getId(), next + " is no longer blocked");
            }
            return next;
        }
        return null;
    }

    // ------------------------------------------------------------------ accessors

    public Map<String, Process> getProcesses() { return processes; }
    public void setProcesses(Map<String, Process> processes) { this.processes = new LinkedHashMap<>(processes); }
    public Map<String, Resource> getResources() { return resources; }
    public void setResources(Map<String, Resource> resources) { this.resources = new LinkedHashMap<>(resources); }
    public int getCheckpointInterval() { return checkpointInterval; }
    public void setCheckpointInterval(int checkpointInterval) { this.checkpointInterval = checkpointInterval; }
    public int getClock() { return clock; }
    public void setClock(int clock) { this.clock = clock; }
    public List<SimulationEvent> getEvents() { return events; }
    public void setEvents(List<SimulationEvent> events) { this.events = new ArrayList<>(events); }
    public List<VictimRecord> getVictimHistory() { return victimHistory; }
    public void setVictimHistory(List<VictimRecord> victimHistory) { this.victimHistory = new ArrayList<>(victimHistory); }
}
