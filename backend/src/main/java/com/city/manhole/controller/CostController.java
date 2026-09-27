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

import java.time.LocalDateTime;
import java.util.List;

/** 费用台账与复核分摊：先处置、后分摊，避免归属不清导致风险滞留 */
@RestController
@RequiredArgsConstructor
public class CostController {
    private final CostRecordRepository costRepo;
    private final OwnerUnitRepository unitRepo;
    private final CoreService core;

    @GetMapping("/api/costs")
    public ApiResponse<?> list(@RequestParam(required = false) Long eventId,
                               @RequestParam(required = false) String bearerStatus) {
        AuthContext.require();
        List<CostRecord> list;
        if (eventId != null) {
            list = costRepo.findByEventIdOrderByCreatedAtDesc(eventId);
        } else if ("PENDING".equals(bearerStatus)) {
            list = costRepo.findByBearerStatusOrderByCreatedAtDesc(CostBearerStatus.PENDING);
        } else {
            list = costRepo.findAll();
        }
        return ApiResponse.ok(list);
    }

    public record AllocateReq(@NotBlank(message = "请选择承担方类型") String bearerType,
                              Long bearerUnitId, String note) {}

    /** 复核分摊：将责任待定的费用分摊给权属单位/街道/多方 */
    @PostMapping("/api/costs/{id}/allocate")
    public ApiResponse<?> allocate(@PathVariable Long id, @RequestBody AllocateReq req) {
        User u = AuthContext.requireRole(Role.ADMIN, Role.STREET_DUTY);
        CostRecord c = costRepo.findById(id).orElseThrow(() -> new BizException("费用记录不存在"));
        if (c.getBearerStatus() == CostBearerStatus.ALLOCATED) {
            throw new BizException("该费用已分摊");
        }
        c.setBearerType(req.bearerType());
        if ("OWNER".equals(req.bearerType())) {
            if (req.bearerUnitId() == null) throw new BizException("请选择承担的权属单位");
            OwnerUnit unit = unitRepo.findById(req.bearerUnitId())
                    .orElseThrow(() -> new BizException("权属单位不存在"));
            c.setBearerUnitId(unit.getId());
            c.setBearerUnitName(unit.getName());
        }
        c.setBearerStatus(CostBearerStatus.ALLOCATED);
        c.setAllocatedAt(LocalDateTime.now());
        if (req.note() != null && !req.note().isBlank()) {
            c.setNote((c.getNote() == null ? "" : c.getNote() + "；") + "复核分摊:" + req.note());
        }
        costRepo.save(c);
        String bearer = switch (req.bearerType()) {
            case "OWNER" -> "权属单位 " + c.getBearerUnitName();
            case "STREET" -> "街道";
            default -> "多方分摊";
        };
        core.log(c.getEventId(), u, "费用复核分摊",
                c.getCostType() + "费用 " + c.getAmount() + "元 分摊给 " + bearer);
        return ApiResponse.ok(c);
    }
}
