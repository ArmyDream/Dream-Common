package com.dream.common.core.enums;

/**
 * 排序方向枚举
 */
public enum SortDirection implements BaseEnum<String> {

    ASC("asc", "升序", "primary"),
    DESC("desc", "降序", "danger");

    private final String value;

    private final String label;

    private final String color;

    SortDirection(String value, String label, String color) {
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

    public boolean isAsc() {
        return this == ASC;
    }

    public boolean isDesc() {
        return this == DESC;
    }

    public static SortDirection fromValue(String value) {
        return BaseEnum.fromValue(SortDirection.class, value);
    }
}
