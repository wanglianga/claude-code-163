package com.city.manhole.repository;

import com.city.manhole.entity.ManholeEvent;
import com.city.manhole.enums.EventStatus;
import com.city.manhole.enums.OwnershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManholeEventRepository extends JpaRepository<ManholeEvent, Long> {
    List<ManholeEvent> findAllByOrderByCreatedAtDesc();
    List<ManholeEvent> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
    List<ManholeEvent> findByOwnerUnitIdOrderByCreatedAtDesc(Long ownerUnitId);
    List<ManholeEvent> findByOwnershipStatusOrderByCreatedAtDesc(OwnershipStatus status);
    long countByStatus(EventStatus status);
}
