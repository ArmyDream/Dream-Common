package com.dream.common.core.enums;

/**
 * 是否枚举
 *
 * @author dream
 */
public enum YesNo implements BaseEnum<String> {

    NO("0", "否", "danger"),
    YES("1", "是", "success");

    private final String value;

    private final String label;

    private final String color;

    YesNo(String value, String label, String color) {
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

    public boolean isYes() {
        return this == YES;
    }

    public boolean isNo() {
        return this == NO;
    }

    public static YesNo fromValue(String value) {
        return BaseEnum.fromValue(YesNo.class, value);
    }
}
