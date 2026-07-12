package com.dream.common.core.enums;

/**
 * 平台类型枚举
 *
 * @author dream
 */
public enum Platform implements BaseEnum<String> {

    PC("pc", "PC端", "primary"),
    MOBILE("mobile", "移动端", "warning"),
    APP("app", "App", "danger"),
    WECHAT("wechat", "微信小程序", "success"),
    ALIPAY("alipay", "支付宝小程序", "primary"),
    H5("h5", "H5", "primary");

    private final String value;

    private final String label;

    private final String color;

    Platform(String value, String label, String color) {
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

    public static Platform fromValue(String value) {
        return BaseEnum.fromValue(Platform.class, value);
    }
}
