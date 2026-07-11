package com.dream.common.core.enums;

/**
 * 订单状态枚举
 * <p>
 * 对应字典类型：{@link DictType#SYS_ORDER_STATUS}
 */
public enum OrderStatus implements BaseEnum<String> {

    /**
     * 待支付
     */
    PENDING("pending", "待支付", "warning"),

    /**
     * 已支付（待发货）
     */
    PAID("paid", "已支付", "primary"),

    /**
     * 已发货（待收货）
     */
    SHIPPED("shipped", "已发货", "primary"),

    /**
     * 已收货（待评价）
     */
    RECEIVED("received", "已收货", "success"),

    /**
     * 已完成
     */
    COMPLETED("completed", "已完成", "success"),

    /**
     * 已取消
     */
    CANCELLED("cancelled", "已取消", "default"),

    /**
     * 已关闭（超时/异常）
     */
    CLOSED("closed", "已关闭", "default"),

    /**
     * 退款中
     */
    REFUNDING("refunding", "退款中", "warning"),

    /**
     * 已退款
     */
    REFUNDED("refunded", "已退款", "purple"),

    /**
     * 售后中
     */
    AFTER_SALE("afterSale", "售后中", "danger");

    private final String value;
    private final String label;
    private final String color;

    OrderStatus(String value, String label, String color) {
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
     * 是否为终态（不可再变更）
     */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED || this == CLOSED || this == REFUNDED;
    }

    /**
     * 是否可以发货
     */
    public boolean canShip() {
        return this == PAID;
    }

    /**
     * 是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING || this == PAID;
    }

    /**
     * 是否可以申请退款
     */
    public boolean canRefund() {
        return this == PAID || this == SHIPPED || this == RECEIVED;
    }

    /**
     * 是否为正常流程中（非异常、非终态）
     */
    public boolean isInProgress() {
        return this == PENDING || this == PAID || this == SHIPPED || this == RECEIVED;
    }

    public static OrderStatus fromValue(String value) {
        return BaseEnum.fromValue(OrderStatus.class, value);
    }
}
