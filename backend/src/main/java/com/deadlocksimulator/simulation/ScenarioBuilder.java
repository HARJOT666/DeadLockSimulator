package com.deadlocksimulator.simulation;

import com.deadlocksimulator.models.InitialAllocation;
import com.deadlocksimulator.models.Operation;
import com.deadlocksimulator.models.ProcessSpec;
import com.deadlocksimulator.models.ScenarioConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small fluent helper for writing scenarios in code (templates, generator, tests). */
public class ScenarioBuilder {

    private final List<ProcessSpec> processes = new ArrayList<>();
    private final List<String> resources = new ArrayList<>();
    private final List<InitialAllocation> allocations = new ArrayList<>();
    private final List<Operation> ops = new ArrayList<>();
    private final Map<String, Object> parameters = new LinkedHashMap<>();
    private int checkpointInterval;
    private Long seed;

    public ScenarioBuilder process(String id, int priority, int totalWork) {
        return process(id, priority, totalWork, 0, 0);
    }

    public ScenarioBuilder process(String id, int priority, int totalWork, int initialWork, int previousVictims) {
        processes.add(new ProcessSpec(id, priority, totalWork, initialWork, previousVictims));
        return this;
    }

    public ScenarioBuilder resources(String... ids) {
        resources.addAll(List.of(ids));
        return this;
    }

    public ScenarioBuilder allocate(String resource, String process) {
        allocations.add(new InitialAllocation(resource, process));
        return this;
    }

    public ScenarioBuilder request(String process, String... res) {
        ops.add(Operation.request(process, res));
        return this;
    }

    public ScenarioBuilder work(String process, int amount) {
        ops.add(Operation.work(process, amount));
        return this;
    }

    public ScenarioBuilder release(String process, String... res) {
        ops.add(Operation.release(process, res));
        return this;
    }

    public ScenarioBuilder finish(String process) {
        ops.add(Operation.finish(process));
        return this;
    }

    public ScenarioBuilder op(Operation op) {
        ops.add(op);
        return this;
    }

    public ScenarioBuilder checkpointInterval(int interval) {
        this.checkpointInterval = interval;
        return this;
    }

    public ScenarioBuilder seed(Long seed) {
        this.seed = seed;
        return this;
    }

    public ScenarioBuilder parameter(String key, Object value) {
        parameters.put(key, value);
        return this;
    }

    public ScenarioConfig build() {
        return new ScenarioConfig(processes, resources, allocations, ops, checkpointInterval, seed, parameters);
    }
}
