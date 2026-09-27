package com.city.manhole.repository;

import com.city.manhole.entity.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventLogRepository extends JpaRepository<EventLog, Long> {
    List<EventLog> findByEventIdOrderByCreatedAtAsc(Long eventId);
}
