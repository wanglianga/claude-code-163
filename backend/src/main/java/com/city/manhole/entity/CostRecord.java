package com.city.manhole.entity;

import com.city.manhole.enums.CostBearerStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 费用台账：应急处置/维修施工/赔付费用，支持责任待定与复核分摊 */
@Data
@Entity
@Table(name = "cost_record")
public class CostRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventId;

    /** 费用类型：应急处置/维修施工/赔付 */
    @Column(nullable = false, length = 32)
    private String costType;

    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private CostBearerStatus bearerStatus = CostBearerStatus.PENDING;

    /** 承担方类型：OWNER 权属单位 / STREET 街道 / SHARED 多方分摊 */
    @Column(length = 16)
    private String bearerType;

    private Long bearerUnitId;
    private String bearerUnitName;

    /** 关联来源：应急记录/工单/赔付案件 ID */
    private String refType;
    private Long refId;

    @Column(length = 500)
    private String note;

    private LocalDateTime allocatedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
}
