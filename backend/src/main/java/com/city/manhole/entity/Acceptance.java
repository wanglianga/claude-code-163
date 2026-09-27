package com.city.manhole.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 城管巡查员验收记录 */
@Data
@Entity
@Table(name = "acceptance")
public class Acceptance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long workOrderId;
    private Long eventId;
    private Long inspectorId;
    private String inspectorName;

    /** 照片核验 */
    private Boolean photoOk = false;
    /** 定位核验 */
    private Boolean locationOk = false;
    /** 路面平整度 */
    private Boolean flatnessOk = false;
    /** 车辆碾压测试 */
    private Boolean rollingTestOk = false;

    /** PASS / FAIL */
    @Column(nullable = false, length = 8)
    private String result;

    @Column(length = 500)
    private String comment;

    private LocalDateTime createdAt = LocalDateTime.now();
}
