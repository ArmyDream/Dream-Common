package com.dream.common.core.enums;

/**
 * 性别 枚举
 *
 * @author dream
 */
public enum Gender implements BaseEnum<String> {

    MAN("0", "男", "primary"),
    WOMAN("1", "女", "danger"),
    SECRET("2", "保密", "warning"),
    UNKNOWN("3", "未知", "danger");

    private final String value;

    private final String label;

    private final String color;

    Gender(String value, String label, String color) {
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
