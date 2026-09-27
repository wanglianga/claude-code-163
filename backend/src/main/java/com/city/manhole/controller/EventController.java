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

import java.util.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final ManholeEventRepository eventRepo;
    private final EmergencyMeasureRepository measureRepo;
    private final OwnershipInvestigationRepository invRepo;
    private final WorkOrderRepository orderRepo;
    private final AcceptanceRepository acceptanceRepo;
    private final CompensationCaseRepository caseRepo;
    private final CostRecordRepository costRepo;
    private final EventLogRepository logRepo;
    private final CoreService core;

    public record ReportReq(
            @NotBlank(message = "位置不能为空") String location,
            String roadName,
            @NotBlank(message = "道路类型不能为空") String roadType,
            Double lng, Double lat,
            @NotBlank(message = "问题类型不能为空") String problemType,
            String photos,
            Boolean damageCaused,
            String damageDesc,
            @NotBlank(message = "危险程度不能为空") String dangerLevel) {}

    /** 市民上报 */
    @PostMapping
    public ApiResponse<?> report(@RequestBody ReportReq req) {
        User u = AuthContext.requireRole(Role.CITIZEN, Role.ADMIN);
        ManholeEvent e = new ManholeEvent();
        e.setEventNo(core.nextNo("MH"));
        e.setReporterId(u.getId());
        e.setReporterName(u.getName());
        e.setLocation(req.location());
        e.setRoadName(req.roadName());
        e.setRoadType(req.roadType());
        e.setLng(req.lng());
        e.setLat(req.lat());
        e.setProblemType(req.problemType());
        e.setPhotos(req.photos());
        e.setDamageCaused(Boolean.TRUE.equals(req.damageCaused()));
        e.setDamageDesc(req.damageDesc());
        e.setDangerLevel(req.dangerLevel());
        eventRepo.save(e);
        core.log(e.getId(), u, "市民上报",
                "问题类型:" + req.problemType() + "，危险程度:" + req.dangerLevel()
                        + (e.getDamageCaused() ? "，已造成人车损伤" : ""));
        return ApiResponse.ok(e);
    }

    /** 事件列表：按角色过滤 */
    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String status,
                               @RequestParam(required = false) String emergencyStatus,
                               @RequestParam(required = false) String ownershipStatus,
                               @RequestParam(required = false) String roadName,
                               @RequestParam(required = false) String keyword) {
        User u = AuthContext.require();
        List<ManholeEvent> base = switch (u.getRole()) {
            case CITIZEN -> eventRepo.findByReporterIdOrderByCreatedAtDesc(u.getId());
            case OWNER_UNIT -> {
                // 权属单位：已确认给本单位的 + 责任待定可认领的
                List<ManholeEvent> mine = new ArrayList<>(eventRepo.findByOwnerUnitIdOrderByCreatedAtDesc(u.getUnitId()));
                Set<Long> ids = new HashSet<>();
                mine.forEach(e -> ids.add(e.getId()));
                for (ManholeEvent e : eventRepo.findByOwnershipStatusOrderByCreatedAtDesc(OwnershipStatus.UNDETERMINED)) {
                    if (ids.add(e.getId())) mine.add(e);
                }
                mine.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                yield mine;
            }
            default -> eventRepo.findAllByOrderByCreatedAtDesc();
        };
        List<ManholeEvent> filtered = base.stream()
                .filter(e -> status == null || status.isBlank() || e.getStatus().name().equals(status))
                .filter(e -> emergencyStatus == null || emergencyStatus.isBlank() || e.getEmergencyStatus().name().equals(emergencyStatus))
                .filter(e -> ownershipStatus == null || ownershipStatus.isBlank() || e.getOwnershipStatus().name().equals(ownershipStatus))
                .filter(e -> roadName == null || roadName.isBlank() || (e.getRoadName() != null && e.getRoadName().contains(roadName)))
                .filter(e -> keyword == null || keyword.isBlank()
                        || e.getEventNo().contains(keyword)
                        || (e.getLocation() != null && e.getLocation().contains(keyword)))
                .toList();
        return ApiResponse.ok(filtered);
    }

    /** 事件详情：含两条线的全部记录 */
    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        User u = AuthContext.require();
        ManholeEvent e = core.event(id);
        if (u.getRole() == Role.CITIZEN && !Objects.equals(e.getReporterId(), u.getId())) {
            throw new BizException("无权查看他人事件");
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("event", e);
        detail.put("measures", measureRepo.findByEventIdOrderByCreatedAtDesc(id));
        detail.put("investigations", invRepo.findByEventIdOrderByCreatedAtDesc(id));
        List<WorkOrder> orders = orderRepo.findByEventIdOrderByCreatedAtDesc(id);
        detail.put("workOrders", orders);
        detail.put("acceptances", acceptanceRepo.findByEventIdOrderByCreatedAtDesc(id));
        detail.put("compensations", caseRepo.findByEventIdOrderByCreatedAtDesc(id));
        detail.put("costs", costRepo.findByEventIdOrderByCreatedAtDesc(id));
        detail.put("logs", logRepo.findByEventIdOrderByCreatedAtAsc(id));
        return ApiResponse.ok(detail);
    }

    /** 结案（验收通过后） */
    @PostMapping("/{id}/close")
    public ApiResponse<?> close(@PathVariable Long id) {
        User u = AuthContext.requireRole(Role.ADMIN, Role.STREET_DUTY);
        ManholeEvent e = core.event(id);
        if (e.getStatus() != EventStatus.ACCEPTED) {
            throw new BizException("仅验收通过的事件可结案");
        }
        e.setStatus(EventStatus.CLOSED);
        eventRepo.save(e);
        core.log(id, u, "事件结案", "验收通过，归档结案");
        return ApiResponse.ok(e);
    }
}
