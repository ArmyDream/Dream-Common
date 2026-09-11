package com.dream.common.core.enums;

/**
 * 启用、停用 枚举
 *
 * @author dream
 */
public enum EnableStatus implements BaseEnum<String> {

    ENABLE("0", "启用", "success"),
    DISABLE("1", "停用", "danger");

    private final String value;

    private final String label;

    private final String color;

    EnableStatus(String value, String label, String color) {
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
