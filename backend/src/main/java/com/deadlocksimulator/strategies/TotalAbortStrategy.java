package com.deadlocksimulator.strategies;

import com.deadlocksimulator.algorithms.DfsCycleDetector;
import com.deadlocksimulator.algorithms.WaitForGraphBuilder;
import com.deadlocksimulator.enums.RecoveryActionType;
import com.deadlocksimulator.enums.StrategyType;
import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.Process;
import com.deadlocksimulator.models.RecoveryAction;
import com.deadlocksimulator.models.RecoveryContext;
import com.deadlocksimulator.models.SystemState;
import com.deadlocksimulator.models.VictimRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy C — total abort of the deadlocked set.
 *
 * <p>The deadlocked set is every process lying on a WFG cycle (members of cyclic strongly connected
 * components). Processes that are merely blocked behind the deadlock, and processes unrelated to it,
 * are NOT aborted — this is abort of the deadlocked set, not of the whole system. All members are
 * terminated atomically in one round (no victim selection), their resources are released, and the
 * base class rebuilds the WFG and reruns DFS. If releasing resources lets blocked processes form a
 * new cycle, the next round aborts that set too.
 */
@Component
public class TotalAbortStrategy extends AbstractRecoveryStrategy {

    public TotalAbortStrategy(WaitForGraphBuilder graphBuilder, DfsCycleDetector detector) {
        super(graphBuilder, detector);
    }

    @Override
    public StrategyType type() {
        return StrategyType.TOTAL_ABORT;
    }

    @Override
    protected RoundResult performRound(SystemState state, DetectionResult detection, int round,
                                       RecoveryContext context) {
        List<String> set = detection.deadlockedProcesses();
        if (set.isEmpty()) {
            return RoundResult.none();
        }
        Map<String, Integer> workBefore = new LinkedHashMap<>();
        for (String pid : set) {
            Process p = state.process(pid);
            workBefore.put(pid, p.getWorkDone());
            p.setVictimCount(p.getVictimCount() + 1);
        }
        state.tick();
        state.log("ABORT", null, null, "Round " + round + ": aborting deadlocked set " + set);
        Map<String, List<String>> released = state.terminateAll(set);

        List<RecoveryAction> actions = new ArrayList<>();
        for (String pid : set) {
            int work = workBefore.get(pid);
            List<String> rel = released.get(pid);
            state.getVictimHistory().add(new VictimRecord(round, "ABORT", pid, null));
            actions.add(new RecoveryAction(round, RecoveryActionType.ABORT, pid, null, work, work, work, null,
                    null, rel, handovers(state, rel),
                    "Aborted " + pid + " as a member of the deadlocked set " + set + "; lost " + work
                            + " work units, released " + rel + "."));
        }
        return new RoundResult(actions, List.of());
    }
}
