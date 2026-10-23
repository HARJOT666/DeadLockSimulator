package com.deadlocksimulator.models;

/**
 * Structured trace entry produced by the simulator and by recovery actions.
 *
 * @param step       logical clock value
 * @param type       e.g. GRANT, BLOCK, WORK, CHECKPOINT, RELEASE, DEFER, FINISH, TERMINATE, ROLLBACK
 * @param processId  process involved (may be null)
 * @param resourceId resource involved (may be null)
 * @param message    human-readable description
 */
public record SimulationEvent(int step, String type, String processId, String resourceId, String message) {
}
