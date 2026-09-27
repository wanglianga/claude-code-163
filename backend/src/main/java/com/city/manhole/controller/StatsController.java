package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.common.AuthContext;
import com.city.manhole.entity.*;
import com.city.manhole.enums.*;
import com.city.manhole.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 统计分析：高发路段、权属争议单位、维修超时、赔付金额 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {
    private final ManholeEventRepository eventRepo;
    private final EmergencyMeasureRepository measureRepo;
    private final OwnershipInvestigationRepository invRepo;
    private final WorkOrderRepository orderRepo;
    private final CompensationCaseRepository caseRepo;
    private final CostRecordRepository costRepo;
    private final OwnerUnitRepository unitRepo;

    @Value("${app.repair-sla-hours:48}")
    private int repairSlaHours;

    @GetMapping("/overview")
    public ApiResponse<?> overview() {
        AuthContext.require();
        List<ManholeEvent> events = eventRepo.findAll();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalEvents", events.size());
        Map<String, Long> byStatus = events.stream().collect(Collectors.groupingBy(e -> e.getStatus().name(), LinkedHashMap::new, Collectors.counting()));
        r.put("byStatus", byStatus);
        Map<String, Long> byDanger = events.stream().collect(Collectors.groupingBy(ManholeEvent::getDangerLevel, Collectors.counting()));
        r.put("byDanger", byDanger);
        r.put("pendingEmergency", events.stream().filter(e -> e.getEmergencyStatus() == EmergencyStatus.PENDING).count());
        r.put("undetermined", events.stream().filter(e -> e.getOwnershipStatus() == OwnershipStatus.UNDETERMINED).count());
        // 平均应急到场时效（分钟）
        var measures = measureRepo.findAll().stream()
                .filter(m -> m.getDispatchedAt() != null && m.getArrivedAt() != null).toList();
        double avgMinutes = measures.stream()
                .mapToLong(m -> Duration.between(m.getDispatchedAt(), m.getArrivedAt()).toMinutes())
                .average().orElse(0);
        r.put("avgArrivalMinutes", Math.round(avgMinutes * 10) / 10.0);
        r.put("repairTimeouts", timeoutOrders().size());
        BigDecimal totalComp = caseRepo.findAll().stream()
                .filter(c -> c.getDecidedAmount() != null && c.getStatus() != CompensationStatus.REJECTED)
                .map(CompensationCase::getDecidedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        r.put("totalCompensation", totalComp);
        r.put("pendingCosts", costRepo.findByBearerStatusOrderByCreatedAtDesc(CostBearerStatus.PENDING).size());
        return ApiResponse.ok(r);
    }

    /** 高发路段 */
    @GetMapping("/hot-roads")
    public ApiResponse<?> hotRoads() {
        AuthContext.require();
        Map<String, List<ManholeEvent>> byRoad = eventRepo.findAll().stream()
                .filter(e -> e.getRoadName() != null && !e.getRoadName().isBlank())
                .collect(Collectors.groupingBy(ManholeEvent::getRoadName));
        List<Map<String, Object>> rows = byRoad.entrySet().stream().map(en -> {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("roadName", en.getKey());
            row.put("count", (long) en.getValue().size());
            row.put("highDanger", en.getValue().stream().filter(e -> "高".equals(e.getDangerLevel())).count());
            row.put("damageCount", en.getValue().stream().filter(e -> Boolean.TRUE.equals(e.getDamageCaused())).count());
            return row;
        }).sorted((a, b) -> Long.compare((long) b.get("count"), (long) a.get("count"))).toList();
        return ApiResponse.ok(rows);
    }

    /** 权属争议单位：待定次数、平均确认时长、涉及赔付 */
    @GetMapping("/disputed-units")
    public ApiResponse<?> disputedUnits() {
        AuthContext.require();
        List<ManholeEvent> events = eventRepo.findAll();
        List<OwnershipInvestigation> invs = invRepo.findAll();
        List<CompensationCase> cases = caseRepo.findAll();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (OwnerUnit unit : unitRepo.findAll()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("unitId", unit.getId());
            row.put("unitName", unit.getName());
            row.put("unitType", unit.getType());
            // 被作为候选但结论为待定的次数（争议指标）
            long disputes = invs.stream().filter(i -> Objects.equals(i.getCandidateUnitId(), unit.getId())
                    && "UNDETERMINED".equals(i.getConclusion())).count();
            row.put("disputeCount", disputes);
            List<ManholeEvent> owned = events.stream().filter(e -> Objects.equals(e.getOwnerUnitId(), unit.getId())).toList();
            row.put("eventCount", owned.size());
            double avgConfirm = owned.stream().filter(e -> e.getOwnerConfirmedAt() != null)
                    .mapToLong(e -> Duration.between(e.getCreatedAt(), e.getOwnerConfirmedAt()).toMinutes())
                    .average().orElse(0) / 60.0;
            row.put("avgConfirmHours", Math.round(avgConfirm * 10) / 10.0);
            BigDecimal comp = cases.stream()
                    .filter(c -> Objects.equals(c.getBearerUnitId(), unit.getId()) && c.getDecidedAmount() != null
                            && c.getStatus() != CompensationStatus.REJECTED)
                    .map(CompensationCase::getDecidedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            row.put("compensationTotal", comp);
            rows.add(row);
        }
        rows.sort((a, b) -> Long.compare((long) b.get("disputeCount"), (long) a.get("disputeCount")));
        return ApiResponse.ok(rows);
    }

    private List<Map<String, Object>> timeoutOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (WorkOrder o : orderRepo.findAll()) {
            LocalDateTime end = o.getFinishedAt() != null ? o.getFinishedAt() : now;
            if (o.getClaimedAt() == null) continue;
            double hours = Duration.between(o.getClaimedAt(), end).toMinutes() / 60.0;
            if (hours > repairSlaHours && o.getStatus() != WorkOrderStatus.VERIFIED) {
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("orderNo", o.getOrderNo());
                row.put("eventId", o.getEventId());
                row.put("ownerUnitName", o.getOwnerUnitName());
                row.put("repairTeamName", o.getRepairTeamName());
                row.put("status", o.getStatus().name());
                row.put("hours", Math.round(hours * 10) / 10.0);
                row.put("finished", o.getFinishedAt() != null);
                rows.add(row);
            }
        }
        rows.sort((a, b) -> Double.compare((double) b.get("hours"), (double) a.get("hours")));
        return rows;
    }

    /** 维修超时工单（SLA 48小时） */
    @GetMapping("/repair-timeouts")
    public ApiResponse<?> repairTimeouts() {
        AuthContext.require();
        return ApiResponse.ok(timeoutOrders());
    }

    /** 赔付金额统计：按责任单位与状态汇总 */
    @GetMapping("/compensations")
    public ApiResponse<?> compensations() {
        AuthContext.require();
        List<CompensationCase> cases = caseRepo.findAll();
        Map<String, BigDecimal> byUnit = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CompensationCase c : cases) {
            if (c.getDecidedAmount() == null || c.getStatus() == CompensationStatus.REJECTED) continue;
            String key = c.getBearerUnitName() != null ? c.getBearerUnitName()
                    : switch (c.getResponsibility() == null ? "" : c.getResponsibility()) {
                        case "街道应急不力" -> "街道";
                        case "多方分摊" -> "多方分摊";
                        default -> "待定";
                    };
            byUnit.merge(key, c.getDecidedAmount(), BigDecimal::add);
            total = total.add(c.getDecidedAmount());
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", total);
        r.put("caseCount", cases.size());
        r.put("byUnit", byUnit);
        return ApiResponse.ok(r);
    }
}
