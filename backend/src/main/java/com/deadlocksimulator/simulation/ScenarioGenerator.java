package com.deadlocksimulator.simulation;

import com.deadlocksimulator.models.GeneratorParams;
import com.deadlocksimulator.models.Operation;
import com.deadlocksimulator.models.ScenarioConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Seeded, deterministic generator of deadlocking scenarios.
 *
 * <p>For each cycle of length L, member i owns a "ring" resource and, depending on the resource
 * density, some extra resources. Its program interleaves WORK segments with acquisitions in a
 * seeded random order (so the contended resource may be acquired early — rollback is expensive —
 * or late — rollback is cheap). After every member has run its program, member i requests the ring
 * resource of member (i+1) mod L, closing the cycle. Blocked dependents then request a cycle
 * member's resource. Same parameters + seed always produce the identical {@link ScenarioConfig}.
 */
@Component
public class ScenarioGenerator {

    public ScenarioConfig generate(GeneratorParams g) {
        Random rnd = new Random(g.seed());
        ScenarioBuilder b = new ScenarioBuilder().checkpointInterval(g.checkpointInterval()).seed(g.seed());
        int pid = 0;
        int rid = 0;
        List<List<Operation>> setupOps = new ArrayList<>();
        List<Operation> closingOps = new ArrayList<>();
        List<String> ringResources = new ArrayList<>();
        int extrasPerMember = (int) Math.round(g.resourceDensity() * g.maxExtraResources());

        for (int c = 0; c < g.cycleCount(); c++) {
            List<String> members = new ArrayList<>();
            List<String> rings = new ArrayList<>();
            int heavy = rnd.nextInt(g.cycleLength());
            for (int i = 0; i < g.cycleLength(); i++) {
                String p = "P" + (++pid);
                String ring = "R" + (++rid);
                members.add(p);
                rings.add(ring);
                b.resources(ring);
                List<String> acquisitions = new ArrayList<>();
                acquisitions.add(ring);
                for (int e = 0; e < extrasPerMember; e++) {
                    String extra = "R" + (++rid);
                    b.resources(extra);
                    acquisitions.add(extra);
                }
                Collections.shuffle(acquisitions, rnd);
                int work = drawWork(g, rnd, i == heavy);
                int total = Math.max(work + 1, (int) Math.ceil(work * 1.5) + 10);
                b.process(p, 1 + rnd.nextInt(10), total, 0, g.maxPreviousVictims() == 0 ? 0 : rnd.nextInt(g.maxPreviousVictims() + 1));
                setupOps.add(interleave(p, acquisitions, work, rnd));
            }
            for (int i = 0; i < members.size(); i++) {
                String next = rings.get((i + 1) % members.size());
                if (g.overlapping() && i == 0 && members.size() >= 3) {
                    closingOps.add(Operation.request(members.get(i), next, rings.get(2)));
                } else {
                    closingOps.add(Operation.request(members.get(i), next));
                }
            }
            // Overlap request of member 0 must come last so both requested resources are already held.
            if (g.overlapping() && members.size() >= 3) {
                int idx = closingOps.size() - members.size();
                closingOps.add(closingOps.remove(idx));
            }
            ringResources.addAll(rings);
        }

        for (int d = 0; d < g.blockedDependents(); d++) {
            String p = "P" + (++pid);
            int work = drawWork(g, rnd, false);
            b.process(p, 1 + rnd.nextInt(10), work * 2 + 10, 0, 0);
            List<Operation> ops = new ArrayList<>();
            if (work > 0) {
                ops.add(Operation.work(p, work));
            }
            setupOps.add(ops);
            closingOps.add(Operation.request(p, ringResources.get(rnd.nextInt(ringResources.size()))));
        }
        for (int k = 0; k < g.independentProcesses(); k++) {
            String p = "P" + (++pid);
            String r = "R" + (++rid);
            b.resources(r);
            int work = Math.max(1, drawWork(g, rnd, false));
            b.process(p, 1 + rnd.nextInt(10), work, 0, 0);
            setupOps.add(List.of(Operation.request(p, r), Operation.work(p, work), Operation.finish(p)));
        }

        // Round-robin interleaving of the setup programs keeps the trace realistic yet deterministic.
        int longest = setupOps.stream().mapToInt(List::size).max().orElse(0);
        for (int step = 0; step < longest; step++) {
            for (List<Operation> program : setupOps) {
                if (step < program.size()) {
                    b.op(program.get(step));
                }
            }
        }
        closingOps.forEach(b::op);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("generator", true);
        params.put("seed", g.seed());
        params.put("cycleCount", g.cycleCount());
        params.put("cycleLength", g.cycleLength());
        params.put("workDistribution", g.workDistribution().name());
        params.put("minWork", g.minWork());
        params.put("maxWork", g.maxWork());
        params.put("checkpointInterval", g.checkpointInterval());
        params.put("resourceDensity", g.resourceDensity());
        params.put("extraResourcesPerMember", extrasPerMember);
        params.put("blockedDependents", g.blockedDependents());
        params.put("independentProcesses", g.independentProcesses());
        params.put("overlapping", g.overlapping());
        params.put("maxPreviousVictims", g.maxPreviousVictims());
        params.forEach(b::parameter);
        return b.build();
    }

    private int drawWork(GeneratorParams g, Random rnd, boolean heavy) {
        int span = g.maxWork() - g.minWork();
        return switch (g.workDistribution()) {
            case EQUAL -> g.minWork() + span / 2;
            case UNIFORM -> g.minWork() + (span == 0 ? 0 : rnd.nextInt(span + 1));
            case SKEWED -> heavy ? g.maxWork() : g.minWork() + (span == 0 ? 0 : rnd.nextInt(span / 6 + 1));
        };
    }

    /** Splits {@code work} into |acquisitions|+1 random segments placed around the acquisitions. */
    private List<Operation> interleave(String p, List<String> acquisitions, int work, Random rnd) {
        int segments = acquisitions.size() + 1;
        int[] parts = new int[segments];
        int remaining = work;
        for (int s = 0; s < segments - 1; s++) {
            parts[s] = remaining == 0 ? 0 : rnd.nextInt(remaining + 1);
            remaining -= parts[s];
        }
        parts[segments - 1] = remaining;
        List<Operation> ops = new ArrayList<>();
        for (int s = 0; s < segments; s++) {
            if (parts[s] > 0) {
                ops.add(Operation.work(p, parts[s]));
            }
            if (s < acquisitions.size()) {
                ops.add(Operation.request(p, acquisitions.get(s)));
            }
        }
        return ops;
    }
}
