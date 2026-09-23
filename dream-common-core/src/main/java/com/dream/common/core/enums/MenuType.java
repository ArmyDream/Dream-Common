package com.dream.common.core.enums;

/**
 * 菜单类型：M 目录 / C 菜单 / F 按钮
 * 枚举
 *
 * @author dream
 */
public enum MenuType implements BaseEnum<String> {

    M("M", "目录", "primary"),
    C("C", "菜单", "warning"),
    F("F", "按钮", "danger");

    private final String value;

    private final String label;

    private final String color;

    MenuType(String value, String label, String color) {
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
