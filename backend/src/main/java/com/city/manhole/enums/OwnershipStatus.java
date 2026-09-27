package com.city.manhole.enums;

public enum OwnershipStatus {
    PENDING("待排查"),
    CONFIRMED("已确认"),
    UNDETERMINED("责任待定");

    private final String label;

    OwnershipStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
