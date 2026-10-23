package com.deadlocksimulator.simulation;

import com.deadlocksimulator.enums.ProcessState;
import com.deadlocksimulator.exceptions.InvalidScenarioException;
import com.deadlocksimulator.models.Checkpoint;
import com.deadlocksimulator.models.InitialAllocation;
import com.deadlocksimulator.models.Operation;
import com.deadlocksimulator.models.Process;
import com.deadlocksimulator.models.ProcessSpec;
import com.deadlocksimulator.models.Resource;
import com.deadlocksimulator.models.ScenarioConfig;
import com.deadlocksimulator.models.SystemState;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Deterministic discrete simulator.
 *
 * <p>The scenario's request sequence is a single, globally ordered trace. Each operation
 * advances the logical clock by one. When an operation belongs to a process that is blocked
 * (WAITING), it is <em>deferred</em>: appended to that process's program queue and executed, in
 * order, as soon as the process is unblocked. A deadlock therefore leaves the remaining operations
 * of the deadlocked processes deferred forever — exactly as a real blocked thread would never
 * reach its next instruction.
 *
 * <p>Checkpoints: every process gets checkpoint #0 at start (its initial work, no resources).
 * Initial allocations are acquired after checkpoint #0. Afterwards, whenever a WORK operation makes
 * the work counter cross at least one multiple of {@code checkpointInterval}, one checkpoint is
 * recorded at the highest multiple reached, with the resources held at that moment. (Intermediate
 * multiples inside the same WORK operation would hold exactly the same resources at a lower work
 * value, so rollback — which always restores the latest valid checkpoint — could never prefer them.)
 *
 * <p>The same {@link ScenarioConfig} always yields the same final state.
 */
@Component
public class SimulationEngine {

    private final ScenarioValidator validator;

    public SimulationEngine(ScenarioValidator validator) {
        this.validator = validator;
    }

    public SystemState run(ScenarioConfig config) {
        validator.validate(config);
        SystemState state = initialise(config);
        List<Operation> sequence = config.requestSequence();
        for (int i = 0; i < sequence.size(); i++) {
            Operation op = sequence.get(i);
            try {
                dispatch(state, op);
                drainUnblocked(state);
            } catch (IllegalStateException e) {
                throw new InvalidScenarioException("Step " + (i + 1) + " (" + op.describe() + "): " + e.getMessage());
            }
        }
        state.log("END", null, null, "Request sequence exhausted after " + sequence.size() + " operations");
        return state;
    }

    private SystemState initialise(ScenarioConfig config) {
        SystemState state = new SystemState();
        state.setCheckpointInterval(config.checkpointInterval());
        for (ProcessSpec spec : config.processes()) {
            Process p = new Process(spec.id(), spec.priority(), spec.totalWork(), spec.initialWork());
            p.setVictimCount(spec.previousVictimCount());
            p.getCheckpoints().add(new Checkpoint(0, spec.initialWork(), List.of(), 0));
            state.getProcesses().put(p.getId(), p);
        }
        for (String rid : config.resources()) {
            state.getResources().put(rid, new Resource(rid));
        }
        for (InitialAllocation a : config.initialAllocations()) {
            state.request(a.processId(), List.of(a.resourceId()));
        }
        state.log("INIT", null, null, config.processes().size() + " processes, "
                + config.resources().size() + " single-instance resources, checkpoint interval "
                + config.checkpointInterval());
        return state;
    }

    private void dispatch(SystemState state, Operation op) {
        Process p = state.process(op.processId());
        if (!p.isLive()) {
            throw new IllegalStateException(p.getId() + " is " + p.getState() + " and cannot issue operations");
        }
        if (p.getState() == ProcessState.WAITING) {
            p.getDeferredOperations().add(op);
            state.tick();
            state.log("DEFER", p.getId(), null, p.getId() + " is blocked; deferred: " + op.describe());
            return;
        }
        execute(state, p, op);
    }

    private void execute(SystemState state, Process p, Operation op) {
        state.tick();
        switch (op.type()) {
            case REQUEST -> {
                state.log("REQUEST", p.getId(), null, op.describe());
                state.request(p.getId(), op.resources());
            }
            case WORK -> doWork(state, p, op.amount());
            case RELEASE -> op.resources().forEach(r -> state.release(p.getId(), r));
            case FINISH -> {
                state.releaseAll(p.getId());
                p.setState(ProcessState.COMPLETED);
                state.log("FINISH", p.getId(), null, p.getId() + " completed");
            }
        }
    }

    private void doWork(SystemState state, Process p, int amount) {
        int before = p.getWorkDone();
        int after = Math.min(p.getTotalWork(), before + amount);
        p.setWorkDone(after);
        state.log("WORK", p.getId(), null, p.getId() + " work " + before + " -> " + after);
        int interval = state.getCheckpointInterval();
        if (interval > 0 && after / interval > before / interval) {
            int checkpointWork = (after / interval) * interval;
            Checkpoint cp = new Checkpoint(p.getCheckpoints().size(), checkpointWork,
                    List.copyOf(p.getHeldResources()), state.getClock());
            p.getCheckpoints().add(cp);
            state.log("CHECKPOINT", p.getId(), null, p.getId() + " checkpoint #" + cp.sequence()
                    + " at work " + checkpointWork + " holding " + cp.heldResources());
        }
    }

    /** Runs deferred operations of unblocked processes until no further progress is possible. */
    private void drainUnblocked(SystemState state) {
        boolean progress = true;
        while (progress) {
            progress = false;
            for (Process p : state.getProcesses().values()) {
                if (p.getState() == ProcessState.RUNNING && !p.getDeferredOperations().isEmpty()) {
                    Operation next = p.getDeferredOperations().remove(0);
                    execute(state, p, next);
                    progress = true;
                    break; // restart scan in deterministic order
                }
            }
        }
    }
}
