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

/**
 * 权属追责线：排查（井盖标识/地下管线/历史施工/周边商户/城管台账）、确认、待定、认领。
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class OwnershipController {
    private final ManholeEventRepository eventRepo;
    private final OwnershipInvestigationRepository invRepo;
    private final OwnerUnitRepository unitRepo;
    private final CoreService core;

    public record InvestigateReq(String coverMarking, String pipelineInfo, String constructionHistory,
                                 String merchantInfo, String urbanRecords,
                                 Long candidateUnitId,
                                 @NotBlank(message = "排查结论不能为空") String conclusion,
                                 String note) {}

    /** 街道/管理员提交权属排查记录 */
    @PostMapping("/{id}/investigations")
    public ApiResponse<?> investigate(@PathVariable Long id, @RequestBody InvestigateReq req) {
        User u = AuthContext.requireRole(Role.STREET_DUTY, Role.ADMIN);
        ManholeEvent e = core.event(id);
        if (e.getStatus() == EventStatus.CLOSED || e.getStatus() == EventStatus.ACCEPTED) {
            throw new BizException("事件已验收或结案，无法再排查");
        }
        OwnershipInvestigation inv = new OwnershipInvestigation();
        inv.setEventId(id);
        inv.setInvestigatorId(u.getId());
        inv.setInvestigatorName(u.getName());
        inv.setCoverMarking(req.coverMarking());
        inv.setPipelineInfo(req.pipelineInfo());
        inv.setConstructionHistory(req.constructionHistory());
        inv.setMerchantInfo(req.merchantInfo());
        inv.setUrbanRecords(req.urbanRecords());
        inv.setConclusion(req.conclusion());
        inv.setNote(req.note());
        if (req.candidateUnitId() != null) {
            OwnerUnit unit = unitRepo.findById(req.candidateUnitId())
                    .orElseThrow(() -> new BizException("候选权属单位不存在"));
            inv.setCandidateUnitId(unit.getId());
            inv.setCandidateUnitName(unit.getName());
        }
        invRepo.save(inv);

        switch (req.conclusion()) {
            case "CONFIRMED" -> {
                if (inv.getCandidateUnitId() == null) throw new BizException("确认权属时必须选择权属单位");
                e.setOwnerUnitId(inv.getCandidateUnitId());
                e.setOwnerUnitName(inv.getCandidateUnitName());
                e.setOwnerConfirmedAt(LocalDateTime.now());
                e.setOwnershipStatus(OwnershipStatus.CONFIRMED);
                e.setStatus(EventStatus.OWNER_CONFIRMED);
                core.log(id, u, "权属确认", "经排查确认权属单位:" + inv.getCandidateUnitName());
            }
            case "UNDETERMINED" -> {
                e.setOwnershipStatus(OwnershipStatus.UNDETERMINED);
                e.setStatus(EventStatus.OWNER_UNDETERMINED);
                core.log(id, u, "权属待定", "短时间无法确认归属，街道先行处置，责任待定");
            }
            default -> {
                if (e.getStatus() == EventStatus.REPORTED) e.setStatus(EventStatus.INVESTIGATING);
                core.log(id, u, "权属排查", "提交排查线索，继续排查");
            }
        }
        eventRepo.save(e);
        return ApiResponse.ok(inv);
    }

    /** 权属单位认领事件（责任待定或待排查时） */
    @PostMapping("/{id}/claim")
    public ApiResponse<?> claim(@PathVariable Long id) {
        User u = AuthContext.requireRole(Role.OWNER_UNIT);
        if (u.getUnitId() == null) throw new BizException("当前账号未关联权属单位");
        ManholeEvent e = core.event(id);
        if (e.getOwnershipStatus() == OwnershipStatus.CONFIRMED) {
            throw new BizException("该事件权属已确认，无需认领");
        }
        OwnerUnit unit = unitRepo.findById(u.getUnitId())
                .orElseThrow(() -> new BizException("关联的权属单位不存在"));
        e.setOwnerUnitId(unit.getId());
        e.setOwnerUnitName(unit.getName());
        e.setOwnerConfirmedAt(LocalDateTime.now());
        e.setOwnershipStatus(OwnershipStatus.CONFIRMED);
        e.setStatus(EventStatus.OWNER_CONFIRMED);
        eventRepo.save(e);
        core.log(id, u, "权属单位认领", unit.getName() + " 主动认领该井盖事件");
        return ApiResponse.ok(e);
    }
}
