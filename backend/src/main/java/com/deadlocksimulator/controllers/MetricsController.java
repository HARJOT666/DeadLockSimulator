package com.deadlocksimulator.controllers;

import com.deadlocksimulator.dto.response.DashboardResponse;
import com.deadlocksimulator.dto.response.HistoryItemResponse;
import com.deadlocksimulator.dto.response.RecoveryResultResponse;
import com.deadlocksimulator.enums.RunStatus;
import com.deadlocksimulator.enums.StrategyType;
import com.deadlocksimulator.security.AppUserPrincipal;
import com.deadlocksimulator.services.MetricsService;
import com.deadlocksimulator.services.MetricsService.HistoryFilter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MetricsController {

    private static final MediaType CSV = MediaType.parseMediaType("text/csv; charset=UTF-8");
    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/metrics/{simulationId}")
    public List<RecoveryResultResponse> metrics(@PathVariable Long simulationId,
                                                @AuthenticationPrincipal AppUserPrincipal user) {
        return metricsService.metrics(simulationId, user);
    }

    @GetMapping("/metrics/{simulationId}/csv")
    public ResponseEntity<String> metricsCsv(@PathVariable Long simulationId,
                                             @AuthenticationPrincipal AppUserPrincipal user) {
        return csv("run-" + simulationId + "-metrics.csv", metricsService.runCsv(simulationId, user));
    }

    @GetMapping("/history")
    public List<HistoryItemResponse> history(@AuthenticationPrincipal AppUserPrincipal user,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) Boolean deadlocked,
                                             @RequestParam(required = false) RunStatus status,
                                             @RequestParam(required = false) StrategyType recommended,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return metricsService.history(user, new HistoryFilter(q, deadlocked, status, recommended, from, to));
    }

    @GetMapping("/history/csv")
    public ResponseEntity<String> historyCsv(@AuthenticationPrincipal AppUserPrincipal user,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) Boolean deadlocked,
                                             @RequestParam(required = false) RunStatus status,
                                             @RequestParam(required = false) StrategyType recommended) {
        return csv("simulation-history.csv",
                metricsService.historyCsv(user, new HistoryFilter(q, deadlocked, status, recommended, null, null)));
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal AppUserPrincipal user) {
        return metricsService.dashboard(user);
    }

    private static ResponseEntity<String> csv(String filename, String body) {
        return ResponseEntity.ok().contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"").body(body);
    }
}
