package com.deadlocksimulator.models;

import com.deadlocksimulator.enums.OperationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * One step of a scenario's deterministic request sequence.
 *
 * @param type      REQUEST, WORK, RELEASE or FINISH
 * @param processId process issuing the operation
 * @param resources resources for REQUEST / RELEASE (ignored otherwise)
 * @param amount    work units for WORK (ignored otherwise)
 */
public record Operation(
        @NotNull OperationType type,
        @NotBlank String processId,
        List<String> resources,
        Integer amount) {

    public Operation {
        resources = resources == null ? List.of() : List.copyOf(resources);
    }

    public static Operation request(String processId, String... resources) {
        return new Operation(OperationType.REQUEST, processId, List.of(resources), null);
    }

    public static Operation work(String processId, int amount) {
        return new Operation(OperationType.WORK, processId, List.of(), amount);
    }

    public static Operation release(String processId, String... resources) {
        return new Operation(OperationType.RELEASE, processId, List.of(resources), null);
    }

    public static Operation finish(String processId) {
        return new Operation(OperationType.FINISH, processId, List.of(), null);
    }

    public String describe() {
        return switch (type) {
            case REQUEST -> processId + " requests " + String.join(", ", resources);
            case WORK -> processId + " works " + amount + " units";
            case RELEASE -> processId + " releases " + String.join(", ", resources);
            case FINISH -> processId + " finishes";
        };
    }
}
