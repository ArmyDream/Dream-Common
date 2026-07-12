package com.dream.common.core.enums;

/**
 * 审核状态枚举
 * <p>
 * 对应字典类型：{@link DictType#SYS_AUDIT_STATUS}
 *
 * @author dream
 */
public enum AuditStatus implements BaseEnum<String> {

    /**
     * 待审核
     */
    PENDING("pending", "待审核", "warning"),

    /**
     * 审核中
     */
    AUDITING("auditing", "审核中", "primary"),

    /**
     * 审核通过
     */
    APPROVED("approved", "审核通过", "success"),

    /**
     * 审核拒绝
     */
    REJECTED("rejected", "审核拒绝", "danger"),

    /**
     * 已撤回
     */
    WITHDRAWN("withdrawn", "已撤回", "default"),

    /**
     * 已退回（退回修改）
     */
    RETURNED("returned", "已退回", "warning");

    private final String value;
    private final String label;
    private final String color;

    AuditStatus(String value, String label, String color) {
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
     * 是否已完成审核（通过或拒绝）
     */
    public boolean isCompleted() {
        return this == APPROVED || this == REJECTED;
    }

    /**
     * 是否等待审核
     */
    public boolean isWaiting() {
        return this == PENDING || this == AUDITING;
    }

    public static AuditStatus fromValue(String value) {
        return BaseEnum.fromValue(AuditStatus.class, value);
    }
}
