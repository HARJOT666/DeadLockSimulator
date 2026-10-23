package com.deadlocksimulator.exceptions;

import java.util.List;

/** Thrown when a scenario is structurally invalid or its request sequence cannot be executed. */
public class InvalidScenarioException extends RuntimeException {

    private final List<String> problems;

    public InvalidScenarioException(List<String> problems) {
        super("Invalid scenario: " + String.join("; ", problems));
        this.problems = List.copyOf(problems);
    }

    public InvalidScenarioException(String problem) {
        this(List.of(problem));
    }

    public List<String> getProblems() {
        return problems;
    }
}
