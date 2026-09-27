package com.city.manhole.repository;

import com.city.manhole.entity.Acceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcceptanceRepository extends JpaRepository<Acceptance, Long> {
    List<Acceptance> findByEventIdOrderByCreatedAtDesc(Long eventId);
    List<Acceptance> findByWorkOrderIdOrderByCreatedAtDesc(Long workOrderId);
}
