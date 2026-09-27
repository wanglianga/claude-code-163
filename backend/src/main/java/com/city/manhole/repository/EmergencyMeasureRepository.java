package com.city.manhole.repository;

import com.city.manhole.entity.EmergencyMeasure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmergencyMeasureRepository extends JpaRepository<EmergencyMeasure, Long> {
    List<EmergencyMeasure> findByEventIdOrderByCreatedAtDesc(Long eventId);
    Optional<EmergencyMeasure> findFirstByEventIdOrderByCreatedAtDesc(Long eventId);
}
