package com.city.manhole.enums;

public enum CostBearerStatus {
    PENDING("责任待定"),
    ALLOCATED("已分摊");

    private final String label;

    CostBearerStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
