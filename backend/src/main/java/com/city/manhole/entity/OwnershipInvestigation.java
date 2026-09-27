package com.city.manhole.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 权属排查记录：井盖标识/地下管线/历史施工/周边商户/城管台账 */
@Data
@Entity
@Table(name = "ownership_investigation")
public class OwnershipInvestigation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventId;
    private Long investigatorId;
    private String investigatorName;

    @Column(length = 500)
    private String coverMarking;      // 井盖标识
    @Column(length = 500)
    private String pipelineInfo;      // 地下管线
    @Column(length = 500)
    private String constructionHistory; // 历史施工
    @Column(length = 500)
    private String merchantInfo;      // 周边商户
    @Column(length = 500)
    private String urbanRecords;      // 城管台账

    /** 候选权属单位 */
    private Long candidateUnitId;
    private String candidateUnitName;

    /** 结论：CONFIRMED 确认归属 / UNDETERMINED 责任待定 / CONTINUE 继续排查 */
    @Column(nullable = false, length = 16)
    private String conclusion;

    @Column(length = 500)
    private String note;

    private LocalDateTime createdAt = LocalDateTime.now();
}
