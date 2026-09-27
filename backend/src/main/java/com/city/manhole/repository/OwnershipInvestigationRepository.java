package com.city.manhole.repository;

import com.city.manhole.entity.OwnershipInvestigation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OwnershipInvestigationRepository extends JpaRepository<OwnershipInvestigation, Long> {
    List<OwnershipInvestigation> findByEventIdOrderByCreatedAtDesc(Long eventId);
}
