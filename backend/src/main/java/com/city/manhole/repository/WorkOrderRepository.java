package com.city.manhole.repository;

import com.city.manhole.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findByEventIdOrderByCreatedAtDesc(Long eventId);
    List<WorkOrder> findByRepairTeamIdOrderByCreatedAtDesc(Long repairTeamId);
    List<WorkOrder> findByOwnerUnitIdOrderByCreatedAtDesc(Long ownerUnitId);
    List<WorkOrder> findAllByOrderByCreatedAtDesc();
}
