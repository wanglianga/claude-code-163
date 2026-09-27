package com.city.manhole.entity;

import com.city.manhole.enums.EmergencyStatus;
import com.city.manhole.enums.EventStatus;
import com.city.manhole.enums.OwnershipStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 井盖事件（市民上报） */
@Data
@Entity
@Table(name = "manhole_event")
public class ManholeEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String eventNo;

    private Long reporterId;
    private String reporterName;

    /** 位置描述 */
    @Column(nullable = false, length = 255)
    private String location;

    /** 路段名称（用于高发路段统计） */
    @Column(length = 128)
    private String roadName;

    /** 道路类型：主干道/次干道/支路/人行道/小区道路 */
    @Column(length = 32)
    private String roadType;

    private Double lng;
    private Double lat;

    /** 问题类型：缺失/松动/异响/塌陷 */
    @Column(nullable = false, length = 32)
    private String problemType;

    /** 照片URL，逗号分隔 */
    @Column(length = 2000)
    private String photos;

    /** 是否已造成人车损伤 */
    private Boolean damageCaused = false;

    @Column(length = 1000)
    private String damageDesc;

    /** 现场危险程度：高/中/低 */
    @Column(nullable = false, length = 8)
    private String dangerLevel;

    /** 主流程状态（权属追责线） */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private EventStatus status = EventStatus.REPORTED;

    /** 应急处置线状态 */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private EmergencyStatus emergencyStatus = EmergencyStatus.PENDING;

    /** 权属状态 */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private OwnershipStatus ownershipStatus = OwnershipStatus.PENDING;

    private Long ownerUnitId;
    private String ownerUnitName;
    private LocalDateTime ownerConfirmedAt;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
