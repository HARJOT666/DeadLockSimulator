package com.deadlocksimulator.simulation;

import com.deadlocksimulator.exceptions.InvalidScenarioException;
import com.deadlocksimulator.models.InitialAllocation;
import com.deadlocksimulator.models.Operation;
import com.deadlocksimulator.models.ProcessSpec;
import com.deadlocksimulator.models.ScenarioConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Structural validation that Bean Validation cannot express (cross references, uniqueness).
 * Runtime problems (e.g. releasing a resource that is not held) are reported by the engine.
 */
@Component
public class ScenarioValidator {

    public void validate(ScenarioConfig config) {
        List<String> problems = new ArrayList<>();
        Set<String> pids = new HashSet<>();
        for (ProcessSpec p : config.processes()) {
            if (!pids.add(p.id())) {
                problems.add("Duplicate process id " + p.id());
            }
            if (p.initialWork() > p.totalWork()) {
                problems.add(p.id() + ": initialWork exceeds totalWork");
            }
        }
        Set<String> rids = new HashSet<>();
        for (String r : config.resources()) {
            if (r == null || !r.matches("[A-Za-z][A-Za-z0-9_-]{0,15}")) {
                problems.add("Invalid resource id '" + r + "'");
            } else if (!rids.add(r)) {
                problems.add("Duplicate resource id " + r);
            } else if (pids.contains(r)) {
                problems.add("Id " + r + " is used for both a process and a resource");
            }
        }
        Set<String> allocated = new HashSet<>();
        for (InitialAllocation a : config.initialAllocations()) {
            if (!rids.contains(a.resourceId())) {
                problems.add("Initial allocation references unknown resource " + a.resourceId());
            }
            if (!pids.contains(a.processId())) {
                problems.add("Initial allocation references unknown process " + a.processId());
            }
            if (!allocated.add(a.resourceId())) {
                problems.add("Resource " + a.resourceId() + " is initially allocated more than once (single-instance)");
            }
        }
        for (int i = 0; i < config.requestSequence().size(); i++) {
            Operation op = config.requestSequence().get(i);
            String at = "Step " + (i + 1) + " (" + op.type() + "): ";
            if (!pids.contains(op.processId())) {
                problems.add(at + "unknown process " + op.processId());
            }
            switch (op.type()) {
                case REQUEST, RELEASE -> {
                    if (op.resources().isEmpty()) {
                        problems.add(at + "at least one resource is required");
                    }
                    if (new HashSet<>(op.resources()).size() != op.resources().size()) {
                        problems.add(at + "duplicate resource in the same operation");
                    }
                    op.resources().stream().filter(r -> !rids.contains(r))
                            .forEach(r -> problems.add(at + "unknown resource " + r));
                }
                case WORK -> {
                    if (op.amount() == null || op.amount() <= 0) {
                        problems.add(at + "amount must be a positive integer");
                    }
                }
                case FINISH -> { }
            }
        }
        if (!problems.isEmpty()) {
            throw new InvalidScenarioException(problems);
        }
    }
}
