package com.deadlocksimulator.repositories;

import com.deadlocksimulator.entities.ScenarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<ScenarioEntity, Long> {

    List<ScenarioEntity> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<ScenarioEntity> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerId(Long ownerId);
}
