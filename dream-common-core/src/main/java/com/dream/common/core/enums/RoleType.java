package com.dream.common.core.enums;

/**
 * 角色类型枚举
 *
 * @author dream
 */
public enum RoleType implements BaseEnum<String> {

    ADMIN("admin", "超级管理员", "danger"),
    SYSTEM("system", "系统管理员", "warning"),
    OPERATOR("operator", "运维人员", "primary"),
    USER("user", "普通用户", "success"),
    GUEST("guest", "访客", "danger");

    private final String value;

    private final String label;

    private final String color;

    RoleType(String value, String label, String color) {
        this.value = value;
        this.label = label;
        this.color = color;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getColor() {
        return color;
    }

}
