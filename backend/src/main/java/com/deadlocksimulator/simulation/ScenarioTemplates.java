package com.deadlocksimulator.simulation;

import com.deadlocksimulator.models.ScenarioConfig;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hand-written, reproducible example scenarios. Each documents the deadlock it creates so users
 * can build one without understanding the internal data structures.
 */
@Component
public class ScenarioTemplates {

    public record Template(String key, String name, String description, ScenarioConfig config) {
    }

    public List<Template> all() {
        return List.of(classicThreeCycle(), twoCyclesWithDependent(), overlappingCycles(), cascadingDeadlock(),
                rollbackFriendly(), noDeadlock());
    }

    public Template byKey(String key) {
        return all().stream().filter(t -> t.key().equals(key)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown template " + key));
    }

    /** P1 -> P2 -> P3 -> P1 circular wait; each process holds one resource and requests the next. */
    public static Template classicThreeCycle() {
        ScenarioConfig c = new ScenarioBuilder()
                .process("P1", 5, 100).process("P2", 5, 100).process("P3", 5, 100)
                .resources("R1", "R2", "R3")
                .checkpointInterval(10)
                .request("P1", "R1").request("P2", "R2").request("P3", "R3")
                .work("P1", 30).work("P2", 20).work("P3", 10)
                .request("P1", "R2").request("P2", "R3").request("P3", "R1")
                .parameter("cycleLength", 3).parameter("cycleCount", 1)
                .build();
        return new Template("classic-three-cycle", "Classic three-process cycle",
                "Each process holds one resource and requests the next one: P1 → P2 → P3 → P1. "
                        + "Resources are acquired before any work, so rollback must restore checkpoint #0.", c);
    }

    /** Two disjoint cycles plus a process blocked behind one of them and an independent one. */
    public static Template twoCyclesWithDependent() {
        ScenarioConfig c = new ScenarioBuilder()
                .process("P1", 3, 120).process("P2", 7, 120)
                .process("P3", 4, 150).process("P4", 6, 150).process("P5", 2, 150)
                .process("P6", 5, 80).process("P7", 5, 60)
                .resources("R1", "R2", "R3", "R4", "R5", "R6")
                .checkpointInterval(15)
                .work("P1", 20).request("P1", "R1").work("P1", 25)
                .request("P2", "R2").work("P2", 60)
                .request("P3", "R3").work("P3", 35)
                .work("P4", 40).request("P4", "R4").work("P4", 10)
                .work("P5", 15).request("P5", "R5").work("P5", 50)
                .request("P7", "R6").work("P7", 30).finish("P7")
                .work("P6", 25)
                .request("P1", "R2").request("P2", "R1")
                .request("P3", "R4").request("P4", "R5").request("P5", "R3")
                .request("P6", "R1")
                .parameter("cycleCount", 2).parameter("cycleLength", "2 and 3")
                .build();
        return new Template("two-cycles-dependent", "Two independent cycles + blocked dependent",
                "Cycle A: P1 ↔ P2. Cycle B: P3 → P4 → P5 → P3. P6 waits for R1 (held by P1) so it is blocked "
                        + "but not deadlocked; P7 runs to completion. Total abort aborts only the five cycle members.", c);
    }

    /** AND-request makes P1 wait for two holders, creating two cycles that share P1 and P3. */
    public static Template overlappingCycles() {
        ScenarioConfig c = new ScenarioBuilder()
                .process("P1", 4, 100).process("P2", 6, 100).process("P3", 5, 100)
                .resources("R1", "R2", "R3")
                .checkpointInterval(10)
                .request("P1", "R1").work("P1", 45)
                .work("P2", 20).request("P2", "R2").work("P2", 15)
                .request("P3", "R3").work("P3", 25)
                .request("P2", "R3").request("P3", "R1").request("P1", "R2", "R3")
                .parameter("cycleCount", 2).parameter("overlapping", true)
                .build();
        return new Template("overlapping-cycles", "Overlapping cycles (AND request)",
                "P1 requests R2 and R3 at once, so it waits for both P2 and P3. Cycles P1 → P2 → P3 → P1 and "
                        + "P1 → P3 → P1 share vertices; removing one process may leave the other cycle intact.", c);
    }

    /** Recovering the first cycle hands a resource to a blocked process, which closes a new cycle. */
    public static Template cascadingDeadlock() {
        ScenarioConfig c = new ScenarioBuilder()
                .process("P1", 2, 100).process("P2", 8, 100).process("P3", 5, 100)
                .resources("R1", "R2", "R3")
                .checkpointInterval(10)
                .request("P1", "R1").request("P2", "R2").request("P3", "R3")
                .work("P1", 5).work("P2", 40).work("P3", 30)
                .request("P3", "R1", "R2")
                .request("P2", "R1").request("P1", "R2")
                .parameter("cycleCount", 1).parameter("cascading", true)
                .build();
        return new Template("cascading-deadlock", "Cascading deadlock (re-detection required)",
                "P1 ↔ P2 deadlock, and P3 is queued first on both R1 and R2. Breaking the cycle at P1 hands R1 "
                        + "to P3, which then waits for R2 (held by P2) while P2 waits for R1 (now held by P3): "
                        + "a new cycle that only re-running detection reveals.", c);
    }

    /** Contended resources are acquired late, after checkpoints, so rollback loses little work. */
    public static Template rollbackFriendly() {
        ScenarioConfig c = new ScenarioBuilder()
                .process("P1", 5, 200).process("P2", 5, 200).process("P3", 5, 200).process("P4", 5, 200)
                .resources("R1", "R2", "R3", "R4")
                .checkpointInterval(10)
                .work("P1", 80).request("P1", "R1").work("P1", 4)
                .work("P2", 90).request("P2", "R2").work("P2", 6)
                .work("P3", 70).request("P3", "R3").work("P3", 3)
                .work("P4", 100).request("P4", "R4").work("P4", 8)
                .request("P1", "R2").request("P2", "R3").request("P3", "R4").request("P4", "R1")
                .parameter("cycleLength", 4).parameter("checkpointInterval", 10)
                .build();
        return new Template("rollback-friendly", "Late acquisition, frequent checkpoints",
                "Four processes do most of their work before acquiring their resource, so a checkpoint just before "
                        + "each acquisition exists. Rollback loses only a few units while termination loses 70–108.", c);
    }

    /** Same processes and resources, but a global acquisition order prevents circular wait. */
    public static Template noDeadlock() {
        ScenarioConfig c = new ScenarioBuilder()
                .process("P1", 5, 60).process("P2", 5, 60).process("P3", 5, 60)
                .resources("R1", "R2", "R3")
                .checkpointInterval(10)
                .request("P1", "R1").request("P2", "R1").request("P3", "R2")
                .work("P1", 20).request("P1", "R2").work("P1", 20).finish("P1")
                .work("P2", 30).request("P2", "R3").finish("P2")
                .work("P3", 15).finish("P3")
                .parameter("cycleCount", 0)
                .build();
        return new Template("no-deadlock", "Ordered acquisition (no deadlock)",
                "Resources are always requested in ascending order, so circular wait is impossible. Detection "
                        + "finds no cycle and recovery is not required.", c);
    }
}
