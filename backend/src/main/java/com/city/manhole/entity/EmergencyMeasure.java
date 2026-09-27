package com.city.manhole.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 应急处置记录（街道值班员临时围护） */
@Data
@Entity
@Table(name = "emergency_measure")
public class EmergencyMeasure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventId;
    private Long operatorId;
    private String operatorName;

    /** 派发时间 */
    private LocalDateTime dispatchedAt;
    /** 到场时间 */
    private LocalDateTime arrivedAt;
    /** 围护完成时间 */
    private LocalDateTime completedAt;

    /** 措施：警示锥/临时盖板/夜间反光标识，逗号分隔 */
    @Column(length = 255)
    private String measureTypes;

    /** 应急处置费用 */
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(length = 500)
    private String note;

    private LocalDateTime createdAt = LocalDateTime.now();
}
