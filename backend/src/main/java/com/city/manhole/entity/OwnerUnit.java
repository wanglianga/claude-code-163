package com.city.manhole.entity;

import jakarta.persistence.*;
import lombok.Data;

/** 权属单位：市政排水、水务、燃气、电力、通信等 */
@Data
@Entity
@Table(name = "owner_unit")
public class OwnerUnit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    /** 单位类型：排水/供水/燃气/电力/通信/市政/其他 */
    @Column(length = 32)
    private String type;

    private String contactPerson;
    private String contactPhone;
}
