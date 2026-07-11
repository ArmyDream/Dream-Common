package com.dream.common.core.enums;

/**
 * 字典类型枚举
 * <p>
 * 用于系统字典管理，定义所有字典的分类
 *
 */
public enum DictType implements BaseEnum<String> {

    /**
     * 系统状态：启用/禁用
     */
    SYS_STATUS("sys_status", "系统状态"),

    /**
     * 是/否
     */
    SYS_YES_NO("sys_yes_no", "是/否"),

    /**
     * 性别
     */
    SYS_USER_SEX("sys_user_sex", "用户性别"),

    /**
     * 用户类型
     */
    SYS_USER_TYPE("sys_user_type", "用户类型"),

    /**
     * 菜单类型
     */
    SYS_MENU_TYPE("sys_menu_type", "菜单类型"),

    /**
     * 操作类型
     */
    SYS_OPER_TYPE("sys_oper_type", "操作类型"),

    /**
     * 日志类型
     */
    SYS_LOG_TYPE("sys_log_type", "日志类型"),

    /**
     * 通知类型
     */
    SYS_NOTIFY_TYPE("sys_notify_type", "通知类型"),

    /**
     * 通知状态
     */
    SYS_NOTIFY_STATUS("sys_notify_status", "通知状态"),

    /**
     * 支付方式
     */
    SYS_PAY_TYPE("sys_pay_type", "支付方式"),

    /**
     * 支付状态
     */
    SYS_PAY_STATUS("sys_pay_status", "支付状态"),

    /**
     * 订单状态
     */
    SYS_ORDER_STATUS("sys_order_status", "订单状态"),

    /**
     * 文件类型
     */
    SYS_FILE_TYPE("sys_file_type", "文件类型"),

    /**
     * 存储方式
     */
    SYS_STORAGE_TYPE("sys_storage_type", "存储方式"),

    /**
     * 平台
     */
    SYS_PLATFORM("sys_platform", "平台"),

    /**
     * 语言
     */
    SYS_LANGUAGE("sys_language", "语言"),

    /**
     * 优先级
     */
    SYS_PRIORITY("sys_priority", "优先级"),

    /**
     * 审核状态
     */
    SYS_AUDIT_STATUS("sys_audit_status", "审核状态"),

    /**
     * 验证码类型
     */
    SYS_CAPTCHA_TYPE("sys_captcha_type", "验证码类型"),

    /**
     * 数据脱敏类型
     */
    SYS_DESENSITIZE_TYPE("sys_desensitize_type", "数据脱敏类型"),

    /**
     * 限流类型
     */
    SYS_RATE_LIMITER_TYPE("sys_rate_limiter_type", "限流类型");

    private final String value;
    private final String label;

    DictType(String value, String label) {
        this.value = value;
        this.label = label;
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
        switch (this) {
            case SYS_STATUS:
            case SYS_YES_NO:
                return "#1890FF";
            case SYS_USER_SEX:
            case SYS_USER_TYPE:
                return "#52C41A";
            case SYS_MENU_TYPE:
            case SYS_OPER_TYPE:
            case SYS_LOG_TYPE:
                return "#FAAD14";
            case SYS_NOTIFY_TYPE:
            case SYS_NOTIFY_STATUS:
                return "#722ED1";
            case SYS_PAY_TYPE:
            case SYS_PAY_STATUS:
            case SYS_ORDER_STATUS:
                return "#FF7A45";
            case SYS_FILE_TYPE:
            case SYS_STORAGE_TYPE:
                return "#13C2C2";
            default:
                return "#8C8C8C";
        }
    }

    public static DictType fromValue(String value) {
        return BaseEnum.fromValue(DictType.class, value);
    }
}