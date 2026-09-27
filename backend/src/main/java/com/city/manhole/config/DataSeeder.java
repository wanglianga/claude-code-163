package com.city.manhole.config;

import com.city.manhole.common.Passwords;
import com.city.manhole.entity.*;
import com.city.manhole.enums.*;
import com.city.manhole.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 初始化测试账号、权属单位与各状态示例事件 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepo;
    private final OwnerUnitRepository unitRepo;
    private final ManholeEventRepository eventRepo;
    private final EmergencyMeasureRepository measureRepo;
    private final OwnershipInvestigationRepository invRepo;
    private final WorkOrderRepository orderRepo;
    private final AcceptanceRepository acceptanceRepo;
    private final CompensationCaseRepository caseRepo;
    private final CostRecordRepository costRepo;
    private final EventLogRepository logRepo;

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) return;

        // ===== 权属单位 =====
        OwnerUnit drain = unit("市政排水处", "排水", "刘工", "0311-87010001");
        OwnerUnit water = unit("水务集团", "供水", "陈工", "0311-87010002");
        OwnerUnit gas = unit("燃气公司", "燃气", "赵工", "0311-87010003");
        OwnerUnit power = unit("电力公司", "电力", "钱工", "0311-87010004");
        OwnerUnit telecom = unit("通信运营商", "通信", "孙工", "0311-87010005");

        // ===== 测试账号 =====
        user("admin", "admin123", "平台管理员", Role.ADMIN, "市城市管理局", null);
        User citizen = user("citizen1", "123456", "张三", Role.CITIZEN, null, null);
        User street = user("street1", "123456", "李值班", Role.STREET_DUTY, "朝阳街道办事处", null);
        User inspector = user("inspector1", "123456", "王巡查", Role.INSPECTOR, "城管执法大队", null);
        User comp = user("comp1", "123456", "赵赔付", Role.COMPENSATION, "市赔付中心", null);
        User repair1 = user("repair1", "123456", "周队长", Role.REPAIR_TEAM, "市政维修一队", null);
        user("repair2", "123456", "吴队长", Role.REPAIR_TEAM, "燃气抢修队", null);
        user("drain", "123456", "排水处联络员", Role.OWNER_UNIT, "市政排水处", drain.getId());
        user("water", "123456", "水务联络员", Role.OWNER_UNIT, "水务集团", water.getId());
        user("gas", "123456", "燃气联络员", Role.OWNER_UNIT, "燃气公司", gas.getId());
        user("power", "123456", "电力联络员", Role.OWNER_UNIT, "电力公司", power.getId());
        user("telecom", "123456", "通信联络员", Role.OWNER_UNIT, "通信运营商", telecom.getId());

        LocalDateTime now = LocalDateTime.now();

        // ===== 事件A：人民路塌陷，全流程已结案，含赔付 =====
        ManholeEvent a = event("MH-SEED-0001", citizen, "人民路与建设大街交口东南角", "人民路", "主干道",
                "塌陷", "高", true, "轿车右前轮陷入井口，车辆受损", EventStatus.CLOSED,
                EmergencyStatus.SECURED, OwnershipStatus.CONFIRMED, drain, now.minusDays(20));
        EmergencyMeasure ma = measure(a, street, now.minusDays(20).plusMinutes(30), now.minusDays(20).plusMinutes(95),
                "警示锥,临时盖板,夜间反光标识", new BigDecimal("800"));
        inv(a, street, "井圈铸有'排水'字样", "排水管线DN600", "2024年道路改造由市政施工", "周边商户证实雨后异响", "台账登记为排水检查井",
                drain, "CONFIRMED", now.minusDays(19));
        a.setOwnerConfirmedAt(now.minusDays(19));
        eventRepo.save(a);
        WorkOrder oa = order(a, drain, repair1, now.minusDays(19).plusHours(2), now.minusDays(18), now.minusDays(17),
                "井室完好，井圈破损", "Φ700重型球墨铸铁井盖", true, null, new BigDecimal("3500"), WorkOrderStatus.VERIFIED);
        accept(oa, a, inspector, true, true, true, true, "PASS", "路面平整，碾压无异响");
        CompensationCase ca = comp("CP-SEED-0001", a, comp, "刘先生", "车辆受损", new BigDecimal("8000"),
                new BigDecimal("6000"), "权属单位", drain,
                "围护及时(95分钟)，权属单位日常巡检缺失，承担主要责任", CompensationStatus.PAID);
        cost(a, "应急处置", "800", "OWNER", drain, "EMERGENCY", ma.getId());
        cost(a, "维修施工", "3500", "OWNER", drain, "WORK_ORDER", oa.getId());
        cost(a, "赔付", "6000", "OWNER", drain, "COMPENSATION", ca.getId());
        log(a, "系统", "示例数据", "已结案示例：全流程演示");

        // ===== 事件B：和平路缺失，维修中且已超时 =====
        ManholeEvent b = event("MH-SEED-0002", citizen, "和平路100号门前", "和平路", "次干道",
                "缺失", "高", false, null, EventStatus.REPAIRING,
                EmergencyStatus.SECURED, OwnershipStatus.CONFIRMED, water, now.minusDays(4));
        measure(b, street, now.minusDays(4).plusMinutes(20), now.minusDays(4).plusMinutes(150),
                "警示锥,临时盖板", new BigDecimal("500"));
        inv(b, street, "井壁标识模糊", "供水管线DN300", "无近期施工记录", "商户反映为自来水井", "台账待更新",
                water, "CONFIRMED", now.minusDays(4).plusHours(20));
        b.setOwnerConfirmedAt(now.minusDays(4).plusHours(20));
        eventRepo.save(b);
        order(b, water, repair1, now.minusDays(3), now.minusDays(3).plusHours(5), null,
                "井室淤积", "Φ600复合井盖", false, null, new BigDecimal("2200"), WorkOrderStatus.ACCEPTED);
        costPending(b, "应急处置", "500", "EMERGENCY", 2L);

        // ===== 事件C：解放大街松动，权属待定 =====
        ManholeEvent c = event("MH-SEED-0003", citizen, "解放大街与自由路交口", "解放大街", "支路",
                "松动", "中", false, null, EventStatus.OWNER_UNDETERMINED,
                EmergencyStatus.SECURED, OwnershipStatus.UNDETERMINED, null, now.minusDays(2));
        measure(c, street, now.minusDays(2).plusMinutes(15), now.minusDays(2).plusMinutes(80),
                "警示锥,夜间反光标识", new BigDecimal("300"));
        inv(c, street, "井盖无标识", "管线资料缺失", "2023年多家单位施工", "商户称曾为燃气施工", "台账无记录",
                gas, "UNDETERMINED", now.minusDays(2).plusHours(6));

        // ===== 事件D：中山路异响，新上报待应急 =====
        event("MH-SEED-0004", citizen, "中山路88号便道", "中山路", "人行道",
                "异响", "低", false, null, EventStatus.REPORTED,
                EmergencyStatus.PENDING, OwnershipStatus.PENDING, null, now.minusHours(3));

        // ===== 事件E：人民路又一缺失，权属已确认待派单 =====
        ManholeEvent e = event("MH-SEED-0005", citizen, "人民路200号公交站旁", "人民路", "主干道",
                "缺失", "高", false, null, EventStatus.OWNER_CONFIRMED,
                EmergencyStatus.SECURED, OwnershipStatus.CONFIRMED, gas, now.minusDays(1));
        measure(e, street, now.minusDays(1).plusMinutes(25), now.minusDays(1).plusMinutes(110),
                "警示锥,临时盖板,夜间反光标识", new BigDecimal("600"));
        inv(e, street, "井圈铸有'燃气'字样", "燃气管线DN200", "2025年燃气改造施工", "商户证实", "台账登记为燃气阀井",
                gas, "CONFIRMED", now.minusDays(1).plusHours(8));
        e.setOwnerConfirmedAt(now.minusDays(1).plusHours(8));
        eventRepo.save(e);

        // ===== 事件F：胜利北街塌陷，验收通过待结案 =====
        ManholeEvent f = event("MH-SEED-0006", citizen, "胜利北街15号小区内", "胜利北街", "小区道路",
                "塌陷", "中", false, null, EventStatus.ACCEPTED,
                EmergencyStatus.SECURED, OwnershipStatus.CONFIRMED, power, now.minusDays(6));
        EmergencyMeasure mf = measure(f, street, now.minusDays(6).plusMinutes(40), now.minusDays(6).plusMinutes(130),
                "警示锥,临时盖板", new BigDecimal("450"));
        inv(f, street, "井盖印有'电力'标识", "电缆沟", "无", "无", "台账登记为电力井",
                power, "CONFIRMED", now.minusDays(5));
        f.setOwnerConfirmedAt(now.minusDays(5));
        eventRepo.save(f);
        WorkOrder of = order(f, power, repair1, now.minusDays(5).plusHours(3), now.minusDays(5).plusHours(8), now.minusDays(4),
                "井室轻微渗水", "Φ700重型井盖", false, "电力", new BigDecimal("2800"), WorkOrderStatus.VERIFIED);
        accept(of, f, inspector, true, true, true, true, "PASS", "验收合格");
        cost(f, "应急处置", "450", "OWNER", power, "EMERGENCY", mf.getId());
        cost(f, "维修施工", "2800", "OWNER", power, "WORK_ORDER", of.getId());
    }

    private OwnerUnit unit(String name, String type, String person, String phone) {
        OwnerUnit u = new OwnerUnit();
        u.setName(name);
        u.setType(type);
        u.setContactPerson(person);
        u.setContactPhone(phone);
        return unitRepo.save(u);
    }

    private User user(String username, String pwd, String name, Role role, String org, Long unitId) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(Passwords.sha256(pwd));
        u.setName(name);
        u.setRole(role);
        u.setOrgName(org);
        u.setUnitId(unitId);
        return userRepo.save(u);
    }

    private ManholeEvent event(String no, User reporter, String location, String road, String roadType,
                               String problem, String danger, boolean damage, String damageDesc,
                               EventStatus status, EmergencyStatus em, OwnershipStatus os,
                               OwnerUnit unit, LocalDateTime createdAt) {
        ManholeEvent e = new ManholeEvent();
        e.setEventNo(no);
        e.setReporterId(reporter.getId());
        e.setReporterName(reporter.getName());
        e.setLocation(location);
        e.setRoadName(road);
        e.setRoadType(roadType);
        e.setLng(114.51 + Math.random() * 0.01);
        e.setLat(38.04 + Math.random() * 0.01);
        e.setProblemType(problem);
        e.setDamageCaused(damage);
        e.setDamageDesc(damageDesc);
        e.setDangerLevel(danger);
        e.setStatus(status);
        e.setEmergencyStatus(em);
        e.setOwnershipStatus(os);
        if (unit != null) {
            e.setOwnerUnitId(unit.getId());
            e.setOwnerUnitName(unit.getName());
        }
        e.setCreatedAt(createdAt);
        e.setUpdatedAt(createdAt);
        return eventRepo.save(e);
    }

    private EmergencyMeasure measure(ManholeEvent e, User op, LocalDateTime dispatched, LocalDateTime arrived,
                                     String types, BigDecimal cost) {
        EmergencyMeasure m = new EmergencyMeasure();
        m.setEventId(e.getId());
        m.setOperatorId(op.getId());
        m.setOperatorName(op.getName());
        m.setDispatchedAt(dispatched);
        m.setArrivedAt(arrived);
        m.setCompletedAt(arrived.plusMinutes(20));
        m.setMeasureTypes(types);
        m.setCost(cost);
        m.setCreatedAt(dispatched);
        return measureRepo.save(m);
    }

    private void inv(ManholeEvent e, User inv, String marking, String pipeline, String construction,
                     String merchant, String records, OwnerUnit candidate, String conclusion, LocalDateTime at) {
        OwnershipInvestigation i = new OwnershipInvestigation();
        i.setEventId(e.getId());
        i.setInvestigatorId(inv.getId());
        i.setInvestigatorName(inv.getName());
        i.setCoverMarking(marking);
        i.setPipelineInfo(pipeline);
        i.setConstructionHistory(construction);
        i.setMerchantInfo(merchant);
        i.setUrbanRecords(records);
        i.setCandidateUnitId(candidate.getId());
        i.setCandidateUnitName(candidate.getName());
        i.setConclusion(conclusion);
        i.setCreatedAt(at);
        invRepo.save(i);
    }

    private WorkOrder order(ManholeEvent e, OwnerUnit unit, User team, LocalDateTime claimed,
                            LocalDateTime accepted, LocalDateTime finished, String chamber, String spec,
                            boolean closure, String pros, BigDecimal cost, WorkOrderStatus status) {
        WorkOrder o = new WorkOrder();
        o.setOrderNo("WO-" + e.getEventNo());
        o.setEventId(e.getId());
        o.setOwnerUnitId(unit.getId());
        o.setOwnerUnitName(unit.getName());
        o.setRepairTeamId(team.getId());
        o.setRepairTeamName(team.getOrgName());
        o.setClaimedAt(claimed);
        o.setAcceptedAt(accepted);
        o.setArrivedAt(accepted == null ? null : accepted.plusHours(2));
        o.setFinishedAt(finished);
        o.setChamberCondition(chamber);
        o.setCoverSpec(spec);
        o.setRoadClosure(closure);
        o.setProfessionalUnits(pros);
        o.setRepairDesc("更换井盖，修复井圈");
        o.setRepairCost(cost);
        o.setStatus(status);
        o.setCreatedAt(claimed);
        return orderRepo.save(o);
    }

    private void accept(WorkOrder o, ManholeEvent e, User inspector, boolean photo, boolean loc,
                        boolean flat, boolean roll, String result, String comment) {
        Acceptance a = new Acceptance();
        a.setWorkOrderId(o.getId());
        a.setEventId(e.getId());
        a.setInspectorId(inspector.getId());
        a.setInspectorName(inspector.getName());
        a.setPhotoOk(photo);
        a.setLocationOk(loc);
        a.setFlatnessOk(flat);
        a.setRollingTestOk(roll);
        a.setResult(result);
        a.setComment(comment);
        a.setCreatedAt(o.getFinishedAt() == null ? LocalDateTime.now() : o.getFinishedAt().plusHours(6));
        acceptanceRepo.save(a);
    }

    private CompensationCase comp(String no, ManholeEvent e, User sp, String claimant, String type,
                                  BigDecimal claim, BigDecimal decided, String resp, OwnerUnit bearer,
                                  String note, CompensationStatus status) {
        CompensationCase c = new CompensationCase();
        c.setCaseNo(no);
        c.setEventId(e.getId());
        c.setSpecialistId(sp.getId());
        c.setSpecialistName(sp.getName());
        c.setClaimantName(claimant);
        c.setClaimantType(type);
        c.setClaimAmount(claim);
        c.setAccidentProof("交警事故认定书、医院诊断证明");
        c.setDecidedAmount(decided);
        c.setResponsibility(resp);
        c.setResponsibilityNote(note);
        c.setBearerUnitId(bearer.getId());
        c.setBearerUnitName(bearer.getName());
        c.setStatus(status);
        c.setDecidedAt(LocalDateTime.now().minusDays(15));
        c.setCreatedAt(LocalDateTime.now().minusDays(18));
        return caseRepo.save(c);
    }

    private void cost(ManholeEvent e, String type, String amount, String bearerType, OwnerUnit unit,
                      String refType, Long refId) {
        CostRecord c = new CostRecord();
        c.setEventId(e.getId());
        c.setCostType(type);
        c.setAmount(new BigDecimal(amount));
        c.setBearerStatus(CostBearerStatus.ALLOCATED);
        c.setBearerType(bearerType);
        c.setBearerUnitId(unit.getId());
        c.setBearerUnitName(unit.getName());
        c.setRefType(refType);
        c.setRefId(refId);
        c.setAllocatedAt(LocalDateTime.now().minusDays(10));
        costRepo.save(c);
    }

    private void costPending(ManholeEvent e, String type, String amount, String refType, Long refId) {
        CostRecord c = new CostRecord();
        c.setEventId(e.getId());
        c.setCostType(type);
        c.setAmount(new BigDecimal(amount));
        c.setRefType(refType);
        c.setRefId(refId);
        c.setNote("街道先行处置，费用责任待定");
        costRepo.save(c);
    }

    private void log(ManholeEvent e, String actor, String action, String detail) {
        logRepo.save(new EventLog(e.getId(), null, actor, action, detail));
    }
}
