package com.city.manhole.enums;

public enum CompensationStatus {
    PENDING("待判定"),
    DECIDED("已判定"),
    PAID("已赔付"),
    REJECTED("已驳回");

    private final String label;

    CompensationStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
