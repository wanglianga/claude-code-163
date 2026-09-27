package com.city.manhole.entity;

import com.city.manhole.enums.CompensationStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 赔付案件 */
@Data
@Entity
@Table(name = "compensation_case")
public class CompensationCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String caseNo;

    private Long eventId;
    private Long specialistId;
    private String specialistName;

    private String claimantName;
    /** 索赔类型：人员受伤/车辆受损/财产受损 */
    @Column(length = 32)
    private String claimantType;
    private BigDecimal claimAmount = BigDecimal.ZERO;

    /** 事故证明（材料描述/附件URL） */
    @Column(length = 1000)
    private String accidentProof;

    /** 核定赔付金额 */
    private BigDecimal decidedAmount;

    /** 责任判定：权属单位/街道应急不力/多方分摊/不予赔付 */
    @Column(length = 32)
    private String responsibility;

    /** 判定理由（依据上报时间、围护时效、权属确认、维修时长、事故证明） */
    @Column(length = 1000)
    private String responsibilityNote;

    /** 赔付承担单位 */
    private Long bearerUnitId;
    private String bearerUnitName;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private CompensationStatus status = CompensationStatus.PENDING;

    private LocalDateTime decidedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
}
