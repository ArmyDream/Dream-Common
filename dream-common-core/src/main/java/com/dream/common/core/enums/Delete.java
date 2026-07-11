package com.dream.common.core.enums;

/**
 * 未删除  已删除 枚举
 */
public enum Delete implements BaseEnum<String> {

    NOT_DELETED("0", "未删除", "success"),
    DELETED("1", "已删除", "danger");

    private final String value;

    private final String label;

    private final String color;

    Delete(String value, String label, String color) {
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

    public static Delete fromValue(String value) {
        return BaseEnum.fromValue(Delete.class, value);
    }

}
