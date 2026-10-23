package com.deadlocksimulator.services;

import com.deadlocksimulator.dto.response.DashboardResponse;
import com.deadlocksimulator.dto.response.HistoryItemResponse;
import com.deadlocksimulator.dto.response.RecoveryResultResponse;
import com.deadlocksimulator.entities.RecoveryResultEntity;
import com.deadlocksimulator.entities.SimulationRunEntity;
import com.deadlocksimulator.enums.RecoveryStatus;
import com.deadlocksimulator.enums.RunStatus;
import com.deadlocksimulator.enums.StrategyType;
import com.deadlocksimulator.repositories.RecoveryResultRepository;
import com.deadlocksimulator.repositories.ScenarioRepository;
import com.deadlocksimulator.repositories.SimulationRunRepository;
import com.deadlocksimulator.security.AppUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Metric retrieval, history search, dashboard statistics and CSV export — all from persisted data. */
@Service
public class MetricsService {

    /** History filters; null fields are ignored. */
    public record HistoryFilter(String query, Boolean deadlocked, RunStatus status, StrategyType recommended,
                                Instant from, Instant to) {
    }

    private final AccessService access;
    private final ScenarioRepository scenarios;
    private final SimulationRunRepository runs;
    private final RecoveryResultRepository results;
    private final ResultMapper mapper;

    public MetricsService(AccessService access, ScenarioRepository scenarios, SimulationRunRepository runs,
                          RecoveryResultRepository results, ResultMapper mapper) {
        this.access = access;
        this.scenarios = scenarios;
        this.runs = runs;
        this.results = results;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<RecoveryResultResponse> metrics(Long runId, AppUserPrincipal user) {
        access.run(runId, user);
        return results.findByRunIdOrderByStrategy(runId).stream().map(mapper::result).toList();
    }

    @Transactional(readOnly = true)
    public List<HistoryItemResponse> history(AppUserPrincipal user, HistoryFilter f) {
        List<SimulationRunEntity> all = user.isAdmin() ? runs.findAllByOrderByCreatedAtDesc()
                : runs.findByOwnerIdOrderByCreatedAtDesc(user.id());
        List<SimulationRunEntity> filtered = all.stream().filter(r -> matches(r, f)).toList();
        return withResults(filtered);
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(AppUserPrincipal user) {
        Long uid = user.id();
        List<SimulationRunEntity> mine = runs.findByOwnerIdOrderByCreatedAtDesc(uid);
        List<DashboardResponse.StrategyStats> stats = results.strategySummary(uid).stream().map(row -> {
            StrategyType s = (StrategyType) row[0];
            return new DashboardResponse.StrategyStats(s, s.getDisplayName(), ((Number) row[1]).longValue(),
                    round(((Number) row[2]).doubleValue()), round(((Number) row[3]).doubleValue()),
                    ((Number) row[4]).longValue(), ((Number) row[5]).longValue());
        }).toList();
        Long latestDeadlocked = mine.stream().filter(SimulationRunEntity::isDeadlocked).findFirst()
                .map(SimulationRunEntity::getId).orElse(null);
        return new DashboardResponse(
                scenarios.countByOwnerId(uid),
                mine.size(),
                mine.stream().filter(SimulationRunEntity::isDeadlocked).count(),
                mine.stream().filter(r -> r.getStatus() == RunStatus.RECOVERY_EXECUTED).count(),
                results.countByRunOwnerIdAndStatus(uid, RecoveryStatus.RECOVERED),
                results.countByRunOwnerIdAndStatusIn(uid, List.of(RecoveryStatus.FAILED, RecoveryStatus.PARTIAL)),
                stats,
                withResults(mine.stream().limit(6).toList()),
                latestDeadlocked);
    }

    /** CSV of the comparative metrics of one run (one row per strategy). */
    @Transactional(readOnly = true)
    public String runCsv(Long runId, AppUserPrincipal user) {
        SimulationRunEntity run = access.run(runId, user);
        StringBuilder sb = new StringBuilder(header());
        results.findByRunIdOrderByStrategy(runId).forEach(r -> sb.append(row(run, r)));
        return sb.toString();
    }

    /** CSV of every (filtered) run in the user's history. */
    @Transactional(readOnly = true)
    public String historyCsv(AppUserPrincipal user, HistoryFilter f) {
        List<SimulationRunEntity> all = (user.isAdmin() ? runs.findAllByOrderByCreatedAtDesc()
                : runs.findByOwnerIdOrderByCreatedAtDesc(user.id())).stream().filter(r -> matches(r, f)).toList();
        Map<Long, List<RecoveryResultEntity>> byRun = resultsByRun(all);
        StringBuilder sb = new StringBuilder(header());
        for (SimulationRunEntity run : all) {
            byRun.getOrDefault(run.getId(), List.of()).stream()
                    .sorted((a, b) -> a.getStrategy().compareTo(b.getStrategy()))
                    .forEach(r -> sb.append(row(run, r)));
        }
        return sb.toString();
    }

    private List<HistoryItemResponse> withResults(List<SimulationRunEntity> list) {
        Map<Long, List<RecoveryResultEntity>> byRun = resultsByRun(list);
        return list.stream().map(r -> mapper.history(r, byRun.getOrDefault(r.getId(), List.of()))).toList();
    }

    private Map<Long, List<RecoveryResultEntity>> resultsByRun(List<SimulationRunEntity> list) {
        if (list.isEmpty()) {
            return Map.of();
        }
        return results.findByRunIdIn(list.stream().map(SimulationRunEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(r -> r.getRun().getId()));
    }

    private static boolean matches(SimulationRunEntity r, HistoryFilter f) {
        if (f == null) {
            return true;
        }
        if (f.query() != null && !f.query().isBlank()
                && !r.getScenarioName().toLowerCase(Locale.ROOT).contains(f.query().toLowerCase(Locale.ROOT).trim())) {
            return false;
        }
        if (f.deadlocked() != null && r.isDeadlocked() != f.deadlocked()) {
            return false;
        }
        if (f.status() != null && r.getStatus() != f.status()) {
            return false;
        }
        if (f.recommended() != null && r.getRecommendedStrategy() != f.recommended()) {
            return false;
        }
        if (f.from() != null && r.getCreatedAt().isBefore(f.from())) {
            return false;
        }
        return f.to() == null || !r.getCreatedAt().isAfter(f.to());
    }

    private static String header() {
        return "run_id,scenario,created_at,strategy,status,verified,rank,work_lost,processes_terminated,"
                + "processes_rolled_back,resources_reclaimed,recovery_steps,starvation_risk,aggregate_score\n";
    }

    private static String row(SimulationRunEntity run, RecoveryResultEntity r) {
        return String.join(",", String.valueOf(run.getId()), csv(run.getScenarioName()),
                String.valueOf(run.getCreatedAt()), r.getStrategy().name(), r.getStatus().name(),
                String.valueOf(r.isVerified()), r.getRankPosition() == null ? "" : String.valueOf(r.getRankPosition()),
                String.valueOf(r.getWorkLost()), String.valueOf(r.getProcessesTerminated()),
                String.valueOf(r.getProcessesRolledBack()), String.valueOf(r.getResourcesReclaimed()),
                String.valueOf(r.getRecoverySteps()), String.valueOf(r.getStarvationRisk()),
                String.format(Locale.ROOT, "%.6f", r.getAggregateScore())) + "\n";
    }

    /** RFC 4180 quoting, and neutralises spreadsheet formula injection. */
    static String csv(String value) {
        String v = value == null ? "" : value;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static double round(double v) {
        return Math.round(v * 10_000d) / 10_000d;
    }
}
