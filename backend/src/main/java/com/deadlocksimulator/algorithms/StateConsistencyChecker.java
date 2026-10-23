package com.deadlocksimulator.algorithms;

import com.deadlocksimulator.enums.ProcessState;
import com.deadlocksimulator.models.Process;
import com.deadlocksimulator.models.Resource;
import com.deadlocksimulator.models.SystemState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks the resource-ownership and wait-queue invariants documented on {@link SystemState}.
 * Returns human-readable violations (empty list = consistent).
 */
@Component
public class StateConsistencyChecker {

    public List<String> check(SystemState state) {
        List<String> v = new ArrayList<>();
        for (Resource r : state.getResources().values()) {
            if (r.getHolder() != null) {
                Process holder = state.getProcesses().get(r.getHolder());
                if (holder == null) {
                    v.add(r.getId() + " held by unknown process " + r.getHolder());
                } else if (!holder.holds(r.getId())) {
                    v.add(r.getId() + " lists holder " + holder.getId() + " but the process does not hold it");
                } else if (!holder.isLive()) {
                    v.add(r.getId() + " is held by " + holder.getState() + " process " + holder.getId());
                }
            } else if (!r.getWaitQueue().isEmpty()) {
                v.add(r.getId() + " is free but has waiters " + r.getWaitQueue());
            }
            for (String w : r.getWaitQueue()) {
                Process waiter = state.getProcesses().get(w);
                if (waiter == null || !waiter.getWaitingFor().contains(r.getId())) {
                    v.add(w + " is queued on " + r.getId() + " without a pending request");
                }
            }
        }
        for (Process p : state.getProcesses().values()) {
            for (String rid : p.getHeldResources()) {
                Resource r = state.getResources().get(rid);
                if (r == null || !p.getId().equals(r.getHolder())) {
                    v.add(p.getId() + " claims " + rid + " but is not its holder");
                }
            }
            for (String rid : p.getWaitingFor()) {
                Resource r = state.getResources().get(rid);
                if (r == null || !r.getWaitQueue().contains(p.getId())) {
                    v.add(p.getId() + " waits for " + rid + " but is not in its queue");
                }
                if (p.holds(rid)) {
                    v.add(p.getId() + " both holds and waits for " + rid);
                }
            }
            boolean waiting = !p.getWaitingFor().isEmpty();
            if (waiting != (p.getState() == ProcessState.WAITING)) {
                v.add(p.getId() + " is " + p.getState() + " but has " + p.getWaitingFor().size() + " pending requests");
            }
            if (!p.isLive() && (!p.getHeldResources().isEmpty() || waiting)) {
                v.add(p.getId() + " is " + p.getState() + " but still holds or waits for resources");
            }
            if (p.getWorkDone() < 0 || p.getWorkDone() > p.getTotalWork()) {
                v.add(p.getId() + " work counter " + p.getWorkDone() + " outside [0, " + p.getTotalWork() + "]");
            }
        }
        return v;
    }
}
