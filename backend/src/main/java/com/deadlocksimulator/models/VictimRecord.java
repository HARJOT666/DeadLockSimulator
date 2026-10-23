package com.deadlocksimulator.models;

/**
 * Entry in a state's victim history.
 *
 * @param round     recovery round in which the victim was chosen
 * @param action    TERMINATE / ROLLBACK / ABORT
 * @param processId victim
 * @param cost      victim-selection cost (null when no selection was made, e.g. total abort)
 */
public record VictimRecord(int round, String action, String processId, Double cost) {
}
