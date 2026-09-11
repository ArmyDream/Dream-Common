package com.dream.common.core.enums;

/**
 * 支付状态枚举
 * <p>
 * 对应字典类型：{@link DictType#SYS_PAY_STATUS}
 *
 * @author dream
 */
public enum PayStatus implements BaseEnum<String> {

    /**
     * 待支付
     */
    PENDING("pending", "待支付", "warning"),

    /**
     * 支付中（处理中）
     */
    PAYING("paying", "支付中", "primary"),

    /**
     * 支付成功
     */
    PAID("paid", "支付成功", "success"),

    /**
     * 支付失败
     */
    FAILED("failed", "支付失败", "danger"),

    /**
     * 已退款
     */
    REFUNDED("refunded", "已退款", "purple"),

    /**
     * 部分退款
     */
    PARTIAL_REFUND("partialRefund", "部分退款", "warning"),

    /**
     * 已关闭（超时未支付）
     */
    CLOSED("closed", "已关闭", "default"),

    /**
     * 已取消
     */
    CANCELLED("cancelled", "已取消", "default");

    private final String value;
    private final String label;
    private final String color;

    PayStatus(String value, String label, String color) {
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
     * 是否已支付成功
     */
    public boolean isPaid() {
        return this == PAID;
    }

    /**
     * 是否已退款（含部分退款）
     */
    public boolean isRefunded() {
        return this == REFUNDED || this == PARTIAL_REFUND;
    }

    /**
     * 是否为终态（不可再变更）
     */
    public boolean isFinal() {
        return this == PAID || this == REFUNDED || this == CLOSED || this == CANCELLED;
    }

    /**
     * 是否等待支付
     */
    public boolean isWaiting() {
        return this == PENDING || this == PAYING;
    }

}
