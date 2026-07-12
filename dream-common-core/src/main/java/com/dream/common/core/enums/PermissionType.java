package com.dream.common.core.enums;

/**
 * 权限类型枚举
 *
 * @author dream
 */
public enum PermissionType implements BaseEnum<String> {

    VIEW("view", "查看", "primary"),
    LIST("list", "查询", "primary"),
    ADD("add", "新增", "success"),
    EDIT("edit", "编辑", "warning"),
    DELETE("delete", "删除", "danger"),
    EXPORT("export", "导出", "warning"),
    IMPORT("import", "导入", "danger"),
    AUDIT("audit", "审核", "warning"),
    GRANT("grant", "授权", "danger");

    private final String value;

    private final String label;

    private final String color;

    PermissionType(String value, String label, String color) {
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

    public static PermissionType fromValue(String value) {
        return BaseEnum.fromValue(PermissionType.class, value);
    }
}
