package com.deadlocksimulator.algorithms;

import com.deadlocksimulator.models.Process;
import com.deadlocksimulator.models.Resource;
import com.deadlocksimulator.models.SystemState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Produces fully independent deep copies of a {@link SystemState}.
 *
 * <p>Every mutable object (processes, resources) and every mutable collection (held resources,
 * pending requests, wait queues, checkpoint lists, deferred operations, event log, victim history)
 * is re-created. Only immutable values are shared: Strings, enums and Java records whose
 * components are immutable ({@code Checkpoint}, {@code Operation}, {@code SimulationEvent},
 * {@code VictimRecord} — all of which defensively copy their lists with {@code List.copyOf}).
 */
@Component
public class StateCloner {

    public SystemState deepCopy(SystemState source) {
        SystemState copy = new SystemState();
        copy.setCheckpointInterval(source.getCheckpointInterval());
        copy.setClock(source.getClock());

        Map<String, Process> processes = new LinkedHashMap<>();
        source.getProcesses().forEach((id, p) -> processes.put(id, copyProcess(p)));
        copy.setProcesses(processes);

        Map<String, Resource> resources = new LinkedHashMap<>();
        source.getResources().forEach((id, r) -> resources.put(id, copyResource(r)));
        copy.setResources(resources);

        copy.setEvents(new ArrayList<>(source.getEvents()));
        copy.setVictimHistory(new ArrayList<>(source.getVictimHistory()));
        return copy;
    }

    private Process copyProcess(Process p) {
        Process c = new Process(p.getId(), p.getPriority(), p.getTotalWork(), p.getWorkDone());
        c.setState(p.getState());
        c.setVictimCount(p.getVictimCount());
        c.setHeldResources(new ArrayList<>(p.getHeldResources()));
        c.setWaitingFor(new LinkedHashSet<>(p.getWaitingFor()));
        c.setCheckpoints(new ArrayList<>(p.getCheckpoints()));
        c.setDeferredOperations(new ArrayList<>(p.getDeferredOperations()));
        return c;
    }

    private Resource copyResource(Resource r) {
        Resource c = new Resource(r.getId());
        c.setHolder(r.getHolder());
        c.setWaitQueue(new ArrayList<>(r.getWaitQueue()));
        return c;
    }
}
