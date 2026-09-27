package com.city.manhole.enums;

/** 应急处置线状态：只看现场风险与到场时效 */
public enum EmergencyStatus {
    PENDING("待应急处置"),
    DISPATCHED("已派发街道"),
    SECURED("临时围护完成");

    private final String label;

    EmergencyStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
