package com.city.manhole.entity;

import com.city.manhole.enums.WorkOrderStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 维修工单 */
@Data
@Entity
@Table(name = "work_order")
public class WorkOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String orderNo;

    private Long eventId;
    private Long ownerUnitId;
    private String ownerUnitName;

    private Long repairTeamId;
    private String repairTeamName;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private WorkOrderStatus status = WorkOrderStatus.ISSUED;

    /** 权属单位认领/派单时间 */
    private LocalDateTime claimedAt;
    /** 维修队接收时间 */
    private LocalDateTime acceptedAt;
    /** 维修队到场时间 */
    private LocalDateTime arrivedAt;

    /** 井室状况 */
    @Column(length = 500)
    private String chamberCondition;
    /** 井盖规格 */
    @Column(length = 128)
    private String coverSpec;
    /** 是否需要封路 */
    private Boolean roadClosure = false;
    /** 专业协同单位：燃气/电力/通信等，逗号分隔 */
    @Column(length = 255)
    private String professionalUnits;

    @Column(length = 1000)
    private String repairDesc;

    private BigDecimal repairCost = BigDecimal.ZERO;

    private LocalDateTime finishedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
}
