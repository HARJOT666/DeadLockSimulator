package com.deadlocksimulator.repositories;

import com.deadlocksimulator.entities.RecoveryResultEntity;
import com.deadlocksimulator.enums.RecoveryStatus;
import com.deadlocksimulator.enums.StrategyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecoveryResultRepository extends JpaRepository<RecoveryResultEntity, Long> {

    List<RecoveryResultEntity> findByRunIdOrderByStrategy(Long runId);

    List<RecoveryResultEntity> findByRunIdIn(Collection<Long> runIds);

    Optional<RecoveryResultEntity> findByRunIdAndStrategy(Long runId, StrategyType strategy);

    long countByRunOwnerIdAndStatus(Long ownerId, RecoveryStatus status);

    long countByRunOwnerIdAndStatusIn(Long ownerId, Collection<RecoveryStatus> statuses);

    /** Per-strategy aggregates for the dashboard: strategy, runs, avg score, avg work lost, wins, recovered. */
    @Query("""
            select r.strategy, count(r), avg(r.aggregateScore), avg(r.workLost),
                   sum(case when r.rankPosition = 1 then 1 else 0 end),
                   sum(case when r.status = com.deadlocksimulator.enums.RecoveryStatus.RECOVERED then 1 else 0 end)
            from RecoveryResultEntity r
            where r.run.owner.id = :ownerId
            group by r.strategy
            order by r.strategy
            """)
    List<Object[]> strategySummary(@Param("ownerId") Long ownerId);
}
