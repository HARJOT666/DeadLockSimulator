package com.deadlocksimulator.controllers;

import com.deadlocksimulator.dto.request.RecoveryRequest;
import com.deadlocksimulator.dto.response.ComparisonResponse;
import com.deadlocksimulator.dto.response.RecoveryResultResponse;
import com.deadlocksimulator.enums.StrategyType;
import com.deadlocksimulator.security.AppUserPrincipal;
import com.deadlocksimulator.services.RecoveryService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recovery/{simulationId}")
public class RecoveryController {

    private final RecoveryService recoveryService;

    public RecoveryController(RecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @PostMapping("/terminate")
    public RecoveryResultResponse terminate(@PathVariable Long simulationId,
                                            @Valid @RequestBody(required = false) RecoveryRequest req,
                                            @AuthenticationPrincipal AppUserPrincipal user) {
        return recoveryService.execute(simulationId, StrategyType.TERMINATION, orDefault(req), user);
    }

    @PostMapping("/rollback")
    public RecoveryResultResponse rollback(@PathVariable Long simulationId,
                                           @Valid @RequestBody(required = false) RecoveryRequest req,
                                           @AuthenticationPrincipal AppUserPrincipal user) {
        return recoveryService.execute(simulationId, StrategyType.ROLLBACK, orDefault(req), user);
    }

    @PostMapping("/abort")
    public RecoveryResultResponse abort(@PathVariable Long simulationId,
                                        @Valid @RequestBody(required = false) RecoveryRequest req,
                                        @AuthenticationPrincipal AppUserPrincipal user) {
        return recoveryService.execute(simulationId, StrategyType.TOTAL_ABORT, orDefault(req), user);
    }

    /** Runs all three strategies on independent copies of the same snapshot and ranks them. */
    @PostMapping("/compare")
    public ComparisonResponse compare(@PathVariable Long simulationId,
                                      @Valid @RequestBody(required = false) RecoveryRequest req,
                                      @AuthenticationPrincipal AppUserPrincipal user) {
        return recoveryService.compare(simulationId, orDefault(req), user);
    }

    /** Latest stored comparison without recomputing. */
    @GetMapping("/comparison")
    public ComparisonResponse comparison(@PathVariable Long simulationId,
                                         @AuthenticationPrincipal AppUserPrincipal user) {
        return recoveryService.latestComparison(simulationId, user);
    }

    private static RecoveryRequest orDefault(RecoveryRequest req) {
        return req == null ? RecoveryRequest.defaults() : req;
    }
}
