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

/** 城管巡查员验收：照片、定位、路面平整度、车辆碾压测试 */
@RestController
@RequiredArgsConstructor
public class AcceptanceController {
    private final AcceptanceRepository acceptanceRepo;
    private final WorkOrderRepository orderRepo;
    private final ManholeEventRepository eventRepo;
    private final CoreService core;

    public record AcceptanceReq(Boolean photoOk, Boolean locationOk, Boolean flatnessOk,
                                Boolean rollingTestOk,
                                @NotBlank(message = "验收结论不能为空") String result,
                                String comment) {}

    @PostMapping("/api/work-orders/{id}/acceptance")
    public ApiResponse<?> accept(@PathVariable Long id, @RequestBody AcceptanceReq req) {
        User u = AuthContext.requireRole(Role.INSPECTOR, Role.ADMIN);
        WorkOrder o = orderRepo.findById(id).orElseThrow(() -> new BizException("工单不存在"));
        if (o.getStatus() != WorkOrderStatus.FINISHED) {
            throw new BizException("工单未完工，无法验收");
        }
        Acceptance a = new Acceptance();
        a.setWorkOrderId(id);
        a.setEventId(o.getEventId());
        a.setInspectorId(u.getId());
        a.setInspectorName(u.getName());
        a.setPhotoOk(Boolean.TRUE.equals(req.photoOk()));
        a.setLocationOk(Boolean.TRUE.equals(req.locationOk()));
        a.setFlatnessOk(Boolean.TRUE.equals(req.flatnessOk()));
        a.setRollingTestOk(Boolean.TRUE.equals(req.rollingTestOk()));
        a.setResult(req.result());
        a.setComment(req.comment());
        acceptanceRepo.save(a);

        ManholeEvent e = core.event(o.getEventId());
        if ("PASS".equals(req.result())) {
            o.setStatus(WorkOrderStatus.VERIFIED);
            e.setStatus(EventStatus.ACCEPTED);
            core.log(e.getId(), u, "验收通过", "照片/定位/平整度/碾压测试核验通过");
        } else {
            o.setStatus(WorkOrderStatus.RETURNED);
            e.setStatus(EventStatus.REPAIRING);
            core.log(e.getId(), u, "验收不通过", "退回维修队返工，原因:" + req.comment());
        }
        orderRepo.save(o);
        eventRepo.save(e);
        return ApiResponse.ok(a);
    }
}
