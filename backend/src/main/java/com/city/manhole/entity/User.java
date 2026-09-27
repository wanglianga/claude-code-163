package com.city.manhole.entity;

import com.city.manhole.enums.Role;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 128)
    private String password;

    @Column(nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    /** 所属部门/单位名称 */
    private String orgName;

    /** 权属单位用户关联的 owner_unit.id */
    private Long unitId;

    private String phone;

    private LocalDateTime createdAt = LocalDateTime.now();
}
