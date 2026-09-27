package com.city.manhole.repository;

import com.city.manhole.entity.CompensationCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompensationCaseRepository extends JpaRepository<CompensationCase, Long> {
    List<CompensationCase> findByEventIdOrderByCreatedAtDesc(Long eventId);
    List<CompensationCase> findAllByOrderByCreatedAtDesc();
}
