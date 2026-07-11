package com.dream.common.core.enums;

/**
 * 用户类型枚举
 * <p>
 * 对应字典类型：{@link DictType#SYS_USER_TYPE}
 */
public enum UserType implements BaseEnum<String> {

    /**
     * 系统管理员
     */
    ADMIN("admin", "系统管理员", "danger"),

    /**
     * 后台用户
     */
    BACKEND("backend", "后台用户", "warning"),

    /**
     * 前台用户
     */
    FRONTEND("frontend", "前台用户", "primary"),

    /**
     * 会员用户
     */
    MEMBER("member", "会员用户", "success"),

    /**
     * VIP 用户
     */
    VIP("vip", "VIP用户", "warning"),

    /**
     * SVIP 用户
     */
    SVIP("svip", "SVIP用户", "danger"),

    /**
     * 游客
     */
    GUEST("guest", "游客", "info"),

    /**
     * 测试用户
     */
    TEST("test", "测试用户", "info");

    private final String value;
    private final String label;
    private final String color;

    UserType(String value, String label, String color) {
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

    /**
     * 是否为管理员
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * 是否为正式用户（非游客、非测试）
     */
    public boolean isFormal() {
        return this != GUEST && this != TEST;
    }

    public static UserType fromValue(String value) {
        return BaseEnum.fromValue(UserType.class, value);
    }
}
