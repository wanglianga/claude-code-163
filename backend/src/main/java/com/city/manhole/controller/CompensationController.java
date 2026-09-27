package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.common.AuthContext;
import com.city.manhole.common.BizException;
import com.city.manhole.entity.*;
import com.city.manhole.enums.*;
import com.city.manhole.repository.*;
import com.city.manhole.service.CoreService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/** 赔付专员：依据上报时间、临时围护、权属确认、维修时长和事故证明判定责任 */
@RestController
@RequiredArgsConstructor
public class CompensationController {
    private final CompensationCaseRepository caseRepo;
    private final ManholeEventRepository eventRepo;
    private final EmergencyMeasureRepository measureRepo;
    private final WorkOrderRepository orderRepo;
    private final OwnerUnitRepository unitRepo;
    private final CoreService core;

    @Value("${app.emergency-sla-minutes:120}")
    private int emergencySlaMinutes;
    @Value("${app.repair-sla-hours:48}")
    private int repairSlaHours;

    public record CreateCaseReq(@NotBlank String claimantName, @NotBlank String claimantType,
                                @NotNull BigDecimal claimAmount, String accidentProof) {}

    /** 对已造成人车损伤的事件立案 */
    @PostMapping("/api/events/{id}/compensations")
    public ApiResponse<?> create(@PathVariable Long id, @RequestBody CreateCaseReq req) {
        User u = AuthContext.requireRole(Role.COMPENSATION, Role.ADMIN);
        ManholeEvent e = core.event(id);
        if (!Boolean.TRUE.equals(e.getDamageCaused())) {
            throw new BizException("该事件未登记人车损伤，无需赔付立案");
        }
        boolean hasOpen = caseRepo.findByEventIdOrderByCreatedAtDesc(id).stream()
                .anyMatch(c -> c.getStatus() == CompensationStatus.PENDING || c.getStatus() == CompensationStatus.DECIDED);
        if (hasOpen) throw new BizException("该事件已有进行中的赔付案件");

        CompensationCase c = new CompensationCase();
        c.setCaseNo(core.nextNo("CP"));
        c.setEventId(id);
        c.setSpecialistId(u.getId());
        c.setSpecialistName(u.getName());
        c.setClaimantName(req.claimantName());
        c.setClaimantType(req.claimantType());
        c.setClaimAmount(req.claimAmount());
        c.setAccidentProof(req.accidentProof());
        caseRepo.save(c);
        core.log(id, u, "赔付立案", "索赔人:" + req.claimantName() + "，类型:" + req.claimantType()
                + "，索赔金额:" + req.claimAmount() + "元");
        return ApiResponse.ok(c);
    }

    @GetMapping("/api/compensations")
    public ApiResponse<?> list() {
        User u = AuthContext.require();
        List<CompensationCase> all = caseRepo.findAllByOrderByCreatedAtDesc();
        if (u.getRole() == Role.CITIZEN) {
            Set<Long> myEventIds = new HashSet<>();
            eventRepo.findByReporterIdOrderByCreatedAtDesc(u.getId()).forEach(e -> myEventIds.add(e.getId()));
            all = all.stream().filter(c -> myEventIds.contains(c.getEventId())).toList();
        }
        return ApiResponse.ok(all);
    }

    /** 案件详情 + 自动汇总的责任判定证据链 */
    @GetMapping("/api/compensations/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        AuthContext.require();
        CompensationCase c = caseRepo.findById(id).orElseThrow(() -> new BizException("案件不存在"));
        ManholeEvent e = core.event(c.getEventId());
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("上报时间", e.getCreatedAt());
        measureRepo.findFirstByEventIdOrderByCreatedAtDesc(e.getId()).ifPresentOrElse(m -> {
            evidence.put("应急派发时间", m.getDispatchedAt());
            evidence.put("围护到场时间", m.getArrivedAt());
            if (m.getDispatchedAt() != null && m.getArrivedAt() != null) {
                long minutes = Duration.between(m.getDispatchedAt(), m.getArrivedAt()).toMinutes();
                evidence.put("围护到场时效(分钟)", minutes);
                evidence.put("围护是否及时", minutes <= emergencySlaMinutes ? "及时" : "超时(SLA " + emergencySlaMinutes + "分钟)");
            }
        }, () -> evidence.put("临时围护", "未实施"));
        if (e.getOwnerConfirmedAt() != null) {
            evidence.put("权属确认时间", e.getOwnerConfirmedAt());
            evidence.put("权属确认耗时(小时)", Math.round(Duration.between(e.getCreatedAt(), e.getOwnerConfirmedAt()).toMinutes() / 60.0 * 10) / 10.0);
        } else {
            evidence.put("权属确认", "未确认（责任待定）");
        }
        orderRepo.findByEventIdOrderByCreatedAtDesc(e.getId()).stream().findFirst().ifPresent(o -> {
            if (o.getClaimedAt() != null && o.getFinishedAt() != null) {
                double hours = Duration.between(o.getClaimedAt(), o.getFinishedAt()).toMinutes() / 60.0;
                evidence.put("维修时长(小时)", Math.round(hours * 10) / 10.0);
                evidence.put("维修是否超时", hours <= repairSlaHours ? "未超时" : "超时(SLA " + repairSlaHours + "小时)");
            } else {
                evidence.put("维修时长", "未完工");
            }
        });
        evidence.put("事故证明", c.getAccidentProof());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("case", c);
        result.put("event", e);
        result.put("evidence", evidence);
        return ApiResponse.ok(result);
    }

    public record DecideReq(@NotBlank(message = "请选择责任判定") String responsibility,
                            @NotNull(message = "请填写核定金额") BigDecimal decidedAmount,
                            Long bearerUnitId,
                            String responsibilityNote) {}

    /** 判定责任与赔付金额 */
    @PostMapping("/api/compensations/{id}/decide")
    public ApiResponse<?> decide(@PathVariable Long id, @RequestBody DecideReq req) {
        User u = AuthContext.requireRole(Role.COMPENSATION, Role.ADMIN);
        CompensationCase c = caseRepo.findById(id).orElseThrow(() -> new BizException("案件不存在"));
        if (c.getStatus() != CompensationStatus.PENDING) throw new BizException("案件已判定");

        c.setResponsibility(req.responsibility());
        c.setDecidedAmount(req.decidedAmount());
        c.setResponsibilityNote(req.responsibilityNote());
        c.setDecidedAt(LocalDateTime.now());

        if ("不予赔付".equals(req.responsibility())) {
            c.setStatus(CompensationStatus.REJECTED);
        } else {
            c.setStatus(CompensationStatus.DECIDED);
            // 赔付费用入账：按责任判定确定承担方
            var cost = core.addCost(c.getEventId(), "赔付", req.decidedAmount(), "COMPENSATION", c.getId(),
                    "赔付案件 " + c.getCaseNo() + " 核定金额");
            switch (req.responsibility()) {
                case "权属单位" -> {
                    ManholeEvent e = core.event(c.getEventId());
                    Long unitId = req.bearerUnitId() != null ? req.bearerUnitId() : e.getOwnerUnitId();
                    if (unitId != null) {
                        unitRepo.findById(unitId).ifPresent(unit -> {
                            c.setBearerUnitId(unit.getId());
                            c.setBearerUnitName(unit.getName());
                            cost.setBearerUnitId(unit.getId());
                            cost.setBearerUnitName(unit.getName());
                        });
                    }
                    cost.setBearerType("OWNER");
                }
                case "街道应急不力" -> cost.setBearerType("STREET");
                case "多方分摊" -> cost.setBearerType("SHARED");
                default -> throw new BizException("未知的责任判定类型");
            }
            cost.setBearerStatus(CostBearerStatus.ALLOCATED);
            cost.setAllocatedAt(LocalDateTime.now());
            core.saveCost(cost);
        }
        caseRepo.save(c);
        core.log(c.getEventId(), u, "赔付判定",
                "责任判定:" + req.responsibility() + "，核定金额:" + req.decidedAmount() + "元");
        return ApiResponse.ok(c);
    }

    /** 确认赔付完成 */
    @PostMapping("/api/compensations/{id}/pay")
    public ApiResponse<?> pay(@PathVariable Long id) {
        User u = AuthContext.requireRole(Role.COMPENSATION, Role.ADMIN);
        CompensationCase c = caseRepo.findById(id).orElseThrow(() -> new BizException("案件不存在"));
        if (c.getStatus() != CompensationStatus.DECIDED) throw new BizException("仅已判定案件可确认赔付");
        c.setStatus(CompensationStatus.PAID);
        caseRepo.save(c);
        core.log(c.getEventId(), u, "赔付完成", "案件 " + c.getCaseNo() + " 赔付 " + c.getDecidedAmount() + "元");
        return ApiResponse.ok(c);
    }
}
