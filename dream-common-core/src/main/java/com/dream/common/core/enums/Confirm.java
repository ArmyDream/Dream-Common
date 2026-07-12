package com.dream.common.core.enums;

/**
 * 是否确认 枚举
 *
 * @author dream
 */
public enum Confirm implements BaseEnum<String> {

    UNCONFIRMED("0", "未确认", "danger"),
    CONFIRMED("1", "已确认", "success");

    private final String value;

    private final String label;

    private final String color;

    Confirm(String value, String label, String color) {
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

    public static Confirm fromValue(String value) {
        return BaseEnum.fromValue(Confirm.class, value);
    }
}
