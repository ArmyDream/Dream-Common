package com.dream.common.core.enums;

/**
 * 支付方式枚举
 * <p>
 * 对应字典类型：{@link DictType#SYS_PAY_TYPE}
 */
public enum PayType implements BaseEnum<String> {

    /**
     * 余额支付
     */
    BALANCE("balance", "余额支付", "primary"),

    /**
     * 微信支付
     */
    WECHAT("wechat", "微信支付", "#07C160"),

    /**
     * 支付宝
     */
    ALIPAY("alipay", "支付宝", "#1677FF"),

    /**
     * 银行卡支付
     */
    BANK_CARD("bankCard", "银行卡支付", "warning"),

    /**
     * 信用卡支付
     */
    CREDIT_CARD("creditCard", "信用卡支付", "warning"),

    /**
     * 银联支付
     */
    UNIONPAY("unionpay", "银联支付", "#E60012"),

    /**
     * Apple Pay
     */
    APPLE_PAY("applePay", "Apple Pay", "default"),

    /**
     * PayPal
     */
    PAYPAL("paypal", "PayPal", "#003087"),

    /**
     * 现金支付
     */
    CASH("cash", "现金支付", "default"),

    /**
     * 积分支付
     */
    POINTS("points", "积分支付", "info"),

    /**
     * 混合支付
     */
    MIXED("mixed", "混合支付", "purple"),

    /**
     * 其他
     */
    OTHER("other", "其他方式", "default");

    private final String value;
    private final String label;
    private final String color;

    PayType(String value, String label, String color) {
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
     * 是否为在线支付
     */
    public boolean isOnline() {
        return this == WECHAT || this == ALIPAY || this == UNIONPAY
                || this == APPLE_PAY || this == PAYPAL;
    }

    /**
     * 是否为第三方平台支付
     */
    public boolean isThirdParty() {
        return this == WECHAT || this == ALIPAY || this == APPLE_PAY || this == PAYPAL;
    }

    public static PayType fromValue(String value) {
        return BaseEnum.fromValue(PayType.class, value);
    }
}
