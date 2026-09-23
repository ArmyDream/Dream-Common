package com.dream.common.core.enums;

/**
 * 角色类型枚举
 *
 * @author dream
 */
public enum RoleType implements BaseEnum<String> {

    ADMIN("admin", "超级管理员", "danger"),
    SYSTEM("system", "系统管理员", "warning"),
    ORG_ADMIN("org_admin", "机构管理员", "info"),
    DEPT_ADMIN("dept_admin", "部门管理员", "info"),
    AREA_ADMIN("area_admin", "区域管理员", "info"),
    STORE_ADMIN("store_admin", "门店管理员", "info"),
    OPERATOR("operator", "运维人员", "primary"),
    USER("user", "普通用户", "success"),
    GUEST("guest", "游客", "danger"),
    COMMON("common", "公共", "warning"),
    ANONYMOUS("anonymous", "匿名", "info"),
    NONE("none", "无权限", "info"),
    NO_MENU("no_menu", "无菜单", "info"),
    THIRD_PARTY("third_party", "第三方", "info"),
    TEST("test", "测试", "info"),
    OTHER("other", "其他", "info"),
    ;

    private final String value;

    private final String label;

    private final String color;

    RoleType(String value, String label, String color) {
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
