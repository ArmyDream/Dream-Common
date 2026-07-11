package com.dream.common.core.enums;

/**
 * 菜单类型枚举
 */
public enum MenuType implements BaseEnum<String> {

    DIRECTORY("D", "目录", "primary"),
    MENU("M", "菜单", "success"),
    BUTTON("B", "按钮", "warning"),
    LINK("L", "外链", "danger");

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


    public boolean isDirectory() {
        return this == DIRECTORY;
    }

    public boolean isMenu() {
        return this == MENU;
    }

    public boolean isButton() {
        return this == BUTTON;
    }

    public boolean isLink() {
        return this == LINK;
    }

    public static MenuType fromValue(String value) {
        return BaseEnum.fromValue(MenuType.class, value);
    }

}
