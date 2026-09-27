package com.city.manhole.enums;

/** 事件主流程状态（权属追责线） */
public enum EventStatus {
    REPORTED("已上报"),
    INVESTIGATING("权属排查中"),
    OWNER_CONFIRMED("权属已确认"),
    OWNER_UNDETERMINED("权属待定"),
    ORDER_ISSUED("工单已派发"),
    REPAIRING("维修中"),
    REPAIRED("待验收"),
    ACCEPTED("验收通过"),
    CLOSED("已结案");

    private final String label;

    EventStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
