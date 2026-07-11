package com.dream.common.core.enums;

/**
 * 成功状态 枚举
 */
public enum LogStatus implements BaseEnum<String> {

    FAIL("0", "失败", "danger"),
    SUCCESS("1", "成功", "success"),
    TIMEOUT("2", "超时", "danger"),
    UNKNOWN("3", "未知", "warning"),
    ;

    private final String value;

    private final String label;

    private final String color;

    LogStatus(String value, String label, String color) {
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

    public static LogStatus fromValue(String value) {
        return BaseEnum.fromValue(LogStatus.class, value);
    }
}
