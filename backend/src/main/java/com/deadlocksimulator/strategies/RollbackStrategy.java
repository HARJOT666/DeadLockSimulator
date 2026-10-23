package com.deadlocksimulator.strategies;

import com.deadlocksimulator.algorithms.DfsCycleDetector;
import com.deadlocksimulator.algorithms.VictimSelector;
import com.deadlocksimulator.algorithms.VictimSelector.Candidate;
import com.deadlocksimulator.algorithms.VictimSelector.ScoredCandidate;
import com.deadlocksimulator.algorithms.WaitForGraphBuilder;
import com.deadlocksimulator.enums.ProcessState;
import com.deadlocksimulator.enums.RecoveryActionType;
import com.deadlocksimulator.enums.StrategyType;
import com.deadlocksimulator.models.Checkpoint;
import com.deadlocksimulator.models.DetectionResult;
import com.deadlocksimulator.models.Process;
import com.deadlocksimulator.models.RecoveryAction;
import com.deadlocksimulator.models.RecoveryContext;
import com.deadlocksimulator.models.RoundTrace.CandidateScore;
import com.deadlocksimulator.models.SystemState;
import com.deadlocksimulator.models.VictimRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Strategy B — resource preemption with checkpoint-based rollback.
 *
 * <p>Rollback semantics. A process cannot simply lose a resource and keep running: its state after
 * acquiring the resource may depend on it. To preempt resource R from victim V we therefore restore
 * V to a checkpoint C that is <em>valid</em> for this preemption:
 * <ol>
 *   <li>V did not hold R at C (so after restoring, V legitimately does not own R);</li>
 *   <li>every resource V held at C is still held now (so restoring C does not need to re-acquire
 *       anything that may now belong to someone else).</li>
 * </ol>
 * Checkpoint #0 (start, no resources) is always valid, so a rollback is always possible; the latest
 * valid checkpoint minimises lost work. Restoring C means: withdraw V's pending requests (they were
 * issued after C), release every resource acquired after C (handed to the next FIFO waiter), reset
 * the work counter to C's value (work lost = current − C), discard checkpoints newer than C, and put V
 * in state READY (it will re-execute from C). V's victim count is incremented.
 *
 * <p>Victim choice. Candidates are deadlocked processes holding at least one resource that another
 * deadlocked process waits for. For each candidate the cheapest preemption option (smallest work
 * loss) is evaluated, and the multi-criteria cost uses that loss as "work at risk".
 */
@Component
public class RollbackStrategy extends AbstractRecoveryStrategy {

    /** Cheapest way to preempt a demanded resource from one candidate. */
    record PreemptionOption(String resource, Checkpoint checkpoint, int workLost) {
    }

    private final VictimSelector victimSelector;

    public RollbackStrategy(WaitForGraphBuilder graphBuilder, DfsCycleDetector detector,
                            VictimSelector victimSelector) {
        super(graphBuilder, detector);
        this.victimSelector = victimSelector;
    }

    @Override
    public StrategyType type() {
        return StrategyType.ROLLBACK;
    }

    @Override
    protected RoundResult performRound(SystemState state, DetectionResult detection, int round,
                                       RecoveryContext context) {
        Set<String> deadlocked = new HashSet<>(detection.deadlockedProcesses());
        Map<String, PreemptionOption> options = new HashMap<>();
        List<Candidate> candidates = new ArrayList<>();
        for (String pid : detection.deadlockedProcesses()) {
            Process p = state.process(pid);
            bestOption(state, p, deadlocked).ifPresent(opt -> {
                options.put(pid, opt);
                candidates.add(new Candidate(pid, opt.workLost(), p.getPriority(),
                        p.getHeldResources().size(), p.getVictimCount()));
            });
        }
        if (candidates.isEmpty()) {
            return RoundResult.none();
        }
        List<ScoredCandidate> ranked = victimSelector.rank(candidates, context.victimWeights());
        ScoredCandidate chosen = ranked.get(0);
        Process victim = state.process(chosen.candidate().processId());
        PreemptionOption option = options.get(victim.getId());

        RecoveryAction action = rollback(state, victim, option, round, chosen.cost());
        List<CandidateScore> scores = ranked.stream().map(s -> {
            PreemptionOption o = options.get(s.candidate().processId());
            return TerminationStrategy.toView(s, "preempt " + o.resource() + " via checkpoint #"
                    + o.checkpoint().sequence() + " (work " + o.checkpoint().workDone() + ")");
        }).toList();
        return new RoundResult(List.of(action), scores);
    }

    /** Resources held by {@code p} that some other deadlocked process is waiting for. */
    static List<String> demandedResources(SystemState state, Process p, Set<String> deadlocked) {
        Set<String> demanded = new LinkedHashSet<>();
        for (String rid : p.getHeldResources()) {
            for (String waiter : state.resource(rid).getWaitQueue()) {
                if (deadlocked.contains(waiter) && !waiter.equals(p.getId())) {
                    demanded.add(rid);
                }
            }
        }
        return List.copyOf(demanded);
    }

    /** Latest checkpoint valid for preempting {@code resource} from {@code p} (see class doc). */
    static Optional<Checkpoint> latestValidCheckpoint(Process p, String resource) {
        List<Checkpoint> cps = p.getCheckpoints();
        for (int i = cps.size() - 1; i >= 0; i--) {
            Checkpoint c = cps.get(i);
            if (!c.heldResources().contains(resource) && p.getHeldResources().containsAll(c.heldResources())) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    private Optional<PreemptionOption> bestOption(SystemState state, Process p, Set<String> deadlocked) {
        PreemptionOption best = null;
        for (String rid : demandedResources(state, p, deadlocked)) {
            Optional<Checkpoint> cp = latestValidCheckpoint(p, rid);
            if (cp.isPresent()) {
                int lost = p.getWorkDone() - cp.get().workDone();
                if (best == null || lost < best.workLost()) {
                    best = new PreemptionOption(rid, cp.get(), lost);
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private RecoveryAction rollback(SystemState state, Process victim, PreemptionOption option, int round,
                                    double cost) {
        Checkpoint cp = option.checkpoint();
        int workBefore = victim.getWorkDone();
        state.tick();
        state.log("ROLLBACK", victim.getId(), option.resource(), "Round " + round + ": preempting "
                + option.resource() + " from " + victim.getId() + ", restoring checkpoint #" + cp.sequence());

        // 1. Requests issued after the checkpoint no longer exist in the restored execution.
        state.cancelPendingRequests(victim.getId());
        // 2. Release everything acquired after the checkpoint (includes the preempted resource).
        List<String> toRelease = victim.getHeldResources().stream()
                .filter(r -> !cp.heldResources().contains(r)).toList();
        List<String> released = new ArrayList<>();
        for (String rid : toRelease) {
            state.release(victim.getId(), rid);
            released.add(rid);
        }
        // 3. Restore the execution state recorded by the checkpoint.
        victim.setWorkDone(cp.workDone());
        victim.setCheckpoints(victim.getCheckpoints().subList(0, cp.sequence() + 1));
        victim.getDeferredOperations().clear();
        victim.setState(ProcessState.READY);
        victim.setVictimCount(victim.getVictimCount() + 1);
        state.getVictimHistory().add(new VictimRecord(round, "ROLLBACK", victim.getId(), cost));

        return new RecoveryAction(round, RecoveryActionType.ROLLBACK, victim.getId(), cost, workBefore,
                cp.workDone(), option.workLost(), option.resource(), cp.sequence(), released,
                handovers(state, released),
                "Rolled " + victim.getId() + " back to checkpoint #" + cp.sequence() + " (work " + workBefore
                        + " -> " + cp.workDone() + ") to preempt " + option.resource() + "; released "
                        + released + ", lost " + option.workLost() + " work units.");
    }
}
