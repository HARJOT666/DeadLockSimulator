package com.deadlocksimulator.repositories;

import com.deadlocksimulator.entities.SimulationRunEntity;
import com.deadlocksimulator.enums.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SimulationRunRepository extends JpaRepository<SimulationRunEntity, Long> {

    Optional<SimulationRunEntity> findByIdAndOwnerId(Long id, Long ownerId);

    List<SimulationRunEntity> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<SimulationRunEntity> findAllByOrderByCreatedAtDesc();

    long countByOwnerId(Long ownerId);

    long countByScenarioId(Long scenarioId);

    long countByOwnerIdAndDeadlockedTrue(Long ownerId);

    long countByOwnerIdAndStatus(Long ownerId, RunStatus status);
}
