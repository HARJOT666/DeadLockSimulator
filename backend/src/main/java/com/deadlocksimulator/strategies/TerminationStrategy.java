package com.deadlocksimulator.strategies;

import com.deadlocksimulator.algorithms.DfsCycleDetector;
import com.deadlocksimulator.algorithms.VictimSelector;
import com.deadlocksimulator.algorithms.VictimSelector.Candidate;
import com.deadlocksimulator.algorithms.VictimSelector.ScoredCandidate;
import com.deadlocksimulator.algorithms.WaitForGraphBuilder;
import com.deadlocksimulator.enums.RecoveryActionType;
import com.deadlocksimulator.enums.StrategyType;
import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.Process;
import com.deadlocksimulator.models.RecoveryAction;
import com.deadlocksimulator.models.RecoveryContext;
import com.deadlocksimulator.models.RoundTrace.CandidateScore;
import com.deadlocksimulator.models.SystemState;
import com.deadlocksimulator.models.VictimRecord;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy A — iterative process termination.
 *
 * <p>Each round selects ONE victim among the currently deadlocked processes using the
 * multi-criteria cost (work at risk = the victim's whole work counter), terminates it, releases
 * all its resources to the next FIFO waiters, then the WFG is rebuilt and DFS rerun by the base
 * class. Repeats while a cycle remains.
 */
@Component
public class TerminationStrategy extends AbstractRecoveryStrategy {

    private final VictimSelector victimSelector;

    public TerminationStrategy(WaitForGraphBuilder graphBuilder, DfsCycleDetector detector,
                               VictimSelector victimSelector) {
        super(graphBuilder, detector);
        this.victimSelector = victimSelector;
    }

    @Override
    public StrategyType type() {
        return StrategyType.TERMINATION;
    }

    @Override
    protected RoundResult performRound(SystemState state, DetectionResult detection, int round,
                                       RecoveryContext context) {
        List<Candidate> candidates = detection.deadlockedProcesses().stream()
                .map(state::process)
                .map(p -> new Candidate(p.getId(), p.getWorkDone(), p.getPriority(),
                        p.getHeldResources().size(), p.getVictimCount()))
                .toList();
        if (candidates.isEmpty()) {
            return RoundResult.none();
        }
        List<ScoredCandidate> ranked = victimSelector.rank(candidates, context.victimWeights());
        ScoredCandidate chosen = ranked.get(0);
        Process victim = state.process(chosen.candidate().processId());

        int workBefore = victim.getWorkDone();
        victim.setVictimCount(victim.getVictimCount() + 1);
        state.tick();
        state.log("TERMINATE", victim.getId(), null, "Round " + round + ": terminating " + victim.getId()
                + " (cost " + chosen.cost() + ")");
        List<String> released = state.terminate(victim.getId());
        state.getVictimHistory().add(new VictimRecord(round, "TERMINATE", victim.getId(), chosen.cost()));

        RecoveryAction action = new RecoveryAction(round, RecoveryActionType.TERMINATE, victim.getId(),
                chosen.cost(), workBefore, workBefore, workBefore, null, null, released,
                handovers(state, released),
                "Terminated " + victim.getId() + " (lowest cost " + chosen.cost() + " among "
                        + candidates.size() + " deadlocked processes); lost all " + workBefore
                        + " work units and released " + released.size() + " resource(s).");
        List<CandidateScore> scores = ranked.stream().map(s -> toView(s, "terminate")).toList();
        return new RoundResult(List.of(action), scores);
    }

    static CandidateScore toView(ScoredCandidate s, String option) {
        Candidate c = s.candidate();
        return new CandidateScore(c.processId(), c.workAtRisk(), c.priority(), c.resourcesHeld(),
                c.victimCount(), s.cost(), s.workTerm(), s.priorityTerm(), s.resourceTerm(), s.historyTerm(), option);
    }
}
