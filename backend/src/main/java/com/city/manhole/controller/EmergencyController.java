package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.common.AuthContext;
import com.city.manhole.common.BizException;
import com.city.manhole.entity.*;
import com.city.manhole.enums.*;
import com.city.manhole.repository.*;
import com.city.manhole.service.CoreService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 应急处置线：只看现场风险与到场时效。
 * 街道值班员可先处置危险点，费用记为责任待定，后续复核分摊。
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EmergencyController {
    private final ManholeEventRepository eventRepo;
    private final EmergencyMeasureRepository measureRepo;
    private final CoreService core;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 街道值班员受理并派发应急处置 */
    @PostMapping("/{id}/dispatch")
    public ApiResponse<?> dispatch(@PathVariable Long id) {
        User u = AuthContext.requireRole(Role.STREET_DUTY, Role.ADMIN);
        ManholeEvent e = core.event(id);
        if (e.getEmergencyStatus() != EmergencyStatus.PENDING) {
            throw new BizException("该事件已派发或已处置");
        }
        EmergencyMeasure m = new EmergencyMeasure();
        m.setEventId(id);
        m.setOperatorId(u.getId());
        m.setOperatorName(u.getName());
        m.setDispatchedAt(LocalDateTime.now());
        measureRepo.save(m);
        e.setEmergencyStatus(EmergencyStatus.DISPATCHED);
        eventRepo.save(e);
        core.log(id, u, "应急处置派发", "街道值班员受理，危险程度:" + e.getDangerLevel() + "，安排临时围护");
        return ApiResponse.ok(m);
    }

    public record SecureReq(@NotBlank(message = "请选择围护措施") String measureTypes,
                            String arrivedAt, BigDecimal cost, String note) {}

    /** 记录临时围护完成（警示锥/临时盖板/夜间反光标识），费用责任待定 */
    @PostMapping("/{id}/secure")
    public ApiResponse<?> secure(@PathVariable Long id, @RequestBody SecureReq req) {
        User u = AuthContext.requireRole(Role.STREET_DUTY, Role.ADMIN);
        ManholeEvent e = core.event(id);
        if (e.getEmergencyStatus() == EmergencyStatus.SECURED) {
            throw new BizException("该事件已完成临时围护");
        }
        EmergencyMeasure m = measureRepo.findFirstByEventIdOrderByCreatedAtDesc(id)
                .orElseThrow(() -> new BizException("请先受理派发应急处置"));
        LocalDateTime arrived = (req.arrivedAt() == null || req.arrivedAt().isBlank())
                ? LocalDateTime.now() : LocalDateTime.parse(req.arrivedAt(), FMT);
        m.setArrivedAt(arrived);
        m.setCompletedAt(LocalDateTime.now());
        m.setMeasureTypes(req.measureTypes());
        m.setCost(req.cost() == null ? BigDecimal.ZERO : req.cost());
        m.setNote(req.note());
        measureRepo.save(m);

        e.setEmergencyStatus(EmergencyStatus.SECURED);
        eventRepo.save(e);

        // 费用先记为责任待定，待权属复核后分摊
        core.addCost(id, "应急处置", m.getCost(), "EMERGENCY", m.getId(),
                "街道先行处置（" + req.measureTypes() + "），费用责任待定");

        long minutes = java.time.Duration.between(m.getDispatchedAt(), arrived).toMinutes();
        core.log(id, u, "临时围护完成",
                "措施:" + req.measureTypes() + "，到场时效:" + minutes + "分钟，费用:" + m.getCost() + "元（责任待定）");
        return ApiResponse.ok(m);
    }
}
