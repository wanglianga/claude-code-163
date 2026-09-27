package com.city.manhole.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 事件时间线 */
@Data
@Entity
@Table(name = "event_log")
public class EventLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventId;
    private Long actorId;
    private String actorName;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(length = 1000)
    private String detail;

    private LocalDateTime createdAt = LocalDateTime.now();

    public EventLog() {}

    public EventLog(Long eventId, Long actorId, String actorName, String action, String detail) {
        this.eventId = eventId;
        this.actorId = actorId;
        this.actorName = actorName;
        this.action = action;
        this.detail = detail;
        this.createdAt = LocalDateTime.now();
    }
}
