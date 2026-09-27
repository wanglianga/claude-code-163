package com.city.manhole.enums;

public enum WorkOrderStatus {
    ISSUED("已派发"),
    ACCEPTED("维修队已接收"),
    FINISHED("维修完成"),
    VERIFIED("验收通过"),
    RETURNED("验收退回");

    private final String label;

    WorkOrderStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
