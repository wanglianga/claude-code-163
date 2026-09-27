package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.common.AuthContext;
import com.city.manhole.common.BizException;
import com.city.manhole.entity.*;
import com.city.manhole.enums.*;
import com.city.manhole.repository.*;
import com.city.manhole.service.CoreService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class WorkOrderController {
    private final WorkOrderRepository orderRepo;
    private final ManholeEventRepository eventRepo;
    private final UserRepository userRepo;
    private final CostRecordRepository costRepo;
    private final CoreService core;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public record CreateOrderReq(@NotNull(message = "请选择维修队") Long repairTeamId, String note) {}

    /** 权属单位认领后派发维修工单 */
    @PostMapping("/api/events/{id}/work-orders")
    public ApiResponse<?> create(@PathVariable Long id, @RequestBody CreateOrderReq req) {
        User u = AuthContext.requireRole(Role.OWNER_UNIT, Role.ADMIN);
        ManholeEvent e = core.event(id);
        if (e.getOwnershipStatus() != OwnershipStatus.CONFIRMED) {
            throw new BizException("权属未确认，无法派发工单");
        }
        if (u.getRole() == Role.OWNER_UNIT && !Objects.equals(u.getUnitId(), e.getOwnerUnitId())) {
            throw new BizException("仅确认的权属单位可派发工单");
        }
        boolean hasOpen = orderRepo.findByEventIdOrderByCreatedAtDesc(id).stream()
                .anyMatch(o -> o.getStatus() != WorkOrderStatus.VERIFIED);
        if (hasOpen) throw new BizException("该事件已有进行中的工单");

        User team = userRepo.findById(req.repairTeamId())
                .filter(t -> t.getRole() == Role.REPAIR_TEAM)
                .orElseThrow(() -> new BizException("维修队不存在"));

        WorkOrder o = new WorkOrder();
        o.setOrderNo(core.nextNo("WO"));
        o.setEventId(id);
        o.setOwnerUnitId(e.getOwnerUnitId());
        o.setOwnerUnitName(e.getOwnerUnitName());
        o.setRepairTeamId(team.getId());
        o.setRepairTeamName(team.getOrgName() != null ? team.getOrgName() : team.getName());
        o.setClaimedAt(LocalDateTime.now());
        orderRepo.save(o);

        e.setStatus(EventStatus.ORDER_ISSUED);
        eventRepo.save(e);
        core.log(id, u, "工单派发", "权属单位 " + e.getOwnerUnitName() + " 派单给 " + o.getRepairTeamName()
                + (req.note() != null ? "，备注:" + req.note() : ""));
        return ApiResponse.ok(o);
    }

    /** 工单列表：维修队看指派给自己的，权属单位看本单位的 */
    @GetMapping("/api/work-orders")
    public ApiResponse<?> list() {
        User u = AuthContext.require();
        List<WorkOrder> list = switch (u.getRole()) {
            case REPAIR_TEAM -> orderRepo.findByRepairTeamIdOrderByCreatedAtDesc(u.getId());
            case OWNER_UNIT -> orderRepo.findByOwnerUnitIdOrderByCreatedAtDesc(u.getUnitId());
            default -> orderRepo.findAllByOrderByCreatedAtDesc();
        };
        return ApiResponse.ok(list);
    }

    private WorkOrder myOrder(Long id) {
        User u = AuthContext.requireRole(Role.REPAIR_TEAM);
        WorkOrder o = orderRepo.findById(id).orElseThrow(() -> new BizException("工单不存在"));
        if (!Objects.equals(o.getRepairTeamId(), u.getId())) throw new BizException("该工单未指派给当前维修队");
        return o;
    }

    /** 维修队接收工单 */
    @PostMapping("/api/work-orders/{id}/accept")
    public ApiResponse<?> accept(@PathVariable Long id) {
        User u = AuthContext.requireRole(Role.REPAIR_TEAM);
        WorkOrder o = myOrder(id);
        if (o.getStatus() != WorkOrderStatus.ISSUED) throw new BizException("工单状态不允许接收");
        o.setStatus(WorkOrderStatus.ACCEPTED);
        o.setAcceptedAt(LocalDateTime.now());
        orderRepo.save(o);
        ManholeEvent e = core.event(o.getEventId());
        e.setStatus(EventStatus.REPAIRING);
        eventRepo.save(e);
        core.log(o.getEventId(), u, "工单接收", o.getRepairTeamName() + " 已接收工单 " + o.getOrderNo());
        return ApiResponse.ok(o);
    }

    public record RepairReportReq(String arrivedAt, String chamberCondition, String coverSpec,
                                  Boolean roadClosure, String professionalUnits,
                                  String repairDesc, BigDecimal repairCost) {}

    /** 维修队补充维修信息：到场时间、井室状况、井盖规格、封路、专业协同 */
    @PostMapping("/api/work-orders/{id}/report")
    public ApiResponse<?> report(@PathVariable Long id, @RequestBody RepairReportReq req) {
        User u = AuthContext.requireRole(Role.REPAIR_TEAM);
        WorkOrder o = myOrder(id);
        if (o.getStatus() != WorkOrderStatus.ACCEPTED && o.getStatus() != WorkOrderStatus.RETURNED) {
            throw new BizException("请先接收工单再补充维修信息");
        }
        if (req.arrivedAt() != null && !req.arrivedAt().isBlank()) {
            o.setArrivedAt(LocalDateTime.parse(req.arrivedAt(), FMT));
        }
        o.setChamberCondition(req.chamberCondition());
        o.setCoverSpec(req.coverSpec());
        o.setRoadClosure(Boolean.TRUE.equals(req.roadClosure()));
        o.setProfessionalUnits(req.professionalUnits());
        o.setRepairDesc(req.repairDesc());
        if (req.repairCost() != null) o.setRepairCost(req.repairCost());
        orderRepo.save(o);
        core.log(o.getEventId(), u, "维修信息登记",
                "井盖规格:" + req.coverSpec() + "，封路:" + (o.getRoadClosure() ? "是" : "否")
                        + (req.professionalUnits() != null && !req.professionalUnits().isBlank()
                        ? "，专业协同:" + req.professionalUnits() : ""));
        return ApiResponse.ok(o);
    }

    /** 维修完成 */
    @PostMapping("/api/work-orders/{id}/finish")
    public ApiResponse<?> finish(@PathVariable Long id) {
        User u = AuthContext.requireRole(Role.REPAIR_TEAM);
        WorkOrder o = myOrder(id);
        if (o.getStatus() != WorkOrderStatus.ACCEPTED && o.getStatus() != WorkOrderStatus.RETURNED) {
            throw new BizException("工单状态不允许完工");
        }
        if (o.getArrivedAt() == null || o.getCoverSpec() == null || o.getChamberCondition() == null) {
            throw new BizException("请先补充到场时间、井室状况、井盖规格等维修信息");
        }
        o.setStatus(WorkOrderStatus.FINISHED);
        o.setFinishedAt(LocalDateTime.now());
        orderRepo.save(o);
        ManholeEvent e = core.event(o.getEventId());
        e.setStatus(EventStatus.REPAIRED);
        eventRepo.save(e);
        // 维修费用由权属单位承担，直接入账
        var cost = core.addCost(o.getEventId(), "维修施工", o.getRepairCost(), "WORK_ORDER", o.getId(),
                "维修队 " + o.getRepairTeamName() + " 施工费用");
        cost.setBearerStatus(CostBearerStatus.ALLOCATED);
        cost.setBearerType("OWNER");
        cost.setBearerUnitId(o.getOwnerUnitId());
        cost.setBearerUnitName(o.getOwnerUnitName());
        cost.setAllocatedAt(LocalDateTime.now());
        costRepo.save(cost);
        core.log(o.getEventId(), u, "维修完成", "工单 " + o.getOrderNo() + " 完工，等待城管巡查验收");
        return ApiResponse.ok(o);
    }

    @GetMapping("/api/work-orders/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        AuthContext.require();
        return ApiResponse.ok(orderRepo.findById(id).orElseThrow(() -> new BizException("工单不存在")));
    }
}
