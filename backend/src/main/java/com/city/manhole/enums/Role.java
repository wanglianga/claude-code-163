package com.city.manhole.enums;

public enum Role {
    CITIZEN("市民"),
    STREET_DUTY("街道值班员"),
    OWNER_UNIT("权属单位"),
    REPAIR_TEAM("维修队"),
    INSPECTOR("城管巡查员"),
    COMPENSATION("赔付专员"),
    ADMIN("管理员");

    private final String label;

    Role(String label) { this.label = label; }

    public String getLabel() { return label; }
}
