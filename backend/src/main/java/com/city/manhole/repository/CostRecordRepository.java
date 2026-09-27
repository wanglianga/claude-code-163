package com.city.manhole.repository;

import com.city.manhole.entity.CostRecord;
import com.city.manhole.enums.CostBearerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CostRecordRepository extends JpaRepository<CostRecord, Long> {
    List<CostRecord> findByEventIdOrderByCreatedAtDesc(Long eventId);
    List<CostRecord> findByBearerStatusOrderByCreatedAtDesc(CostBearerStatus status);
}
