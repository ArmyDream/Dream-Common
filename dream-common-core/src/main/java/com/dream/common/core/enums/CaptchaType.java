package com.dream.common.core.enums;

/**
 * 验证码类型枚举
 *
 * @author dream
 */
public enum CaptchaType implements BaseEnum<String> {

    /**
     * 图形验证码（字母数字组合）
     */
    IMAGE("image", "图形验证码"),

    /**
     * 滑块验证码
     */
    SLIDER("slider", "滑块验证码"),

    /**
     * 拼图验证码
     */
    PUZZLE("puzzle", "拼图验证码"),

    /**
     * 文字点选验证码
     */
    TEXT("text", "文字点选"),

    /**
     * 算术验证码
     */
    ARITHMETIC("arithmetic", "算术验证码"),

    /**
     * 短信验证码
     */
    SMS("sms", "短信验证码"),

    /**
     * 邮箱验证码
     */
    EMAIL("email", "邮箱验证码"),

    /**
     * 语音验证码
     */
    VOICE("voice", "语音验证码");

    private final String value;
    private final String label;

    CaptchaType(String value, String label) {
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

    /**
     * 是否为图片类验证码
     */
    public boolean isImageCaptcha() {
        return this == IMAGE || this == SLIDER || this == PUZZLE || this == TEXT || this == ARITHMETIC;
    }

    /**
     * 是否为数字类验证码（短信/邮箱）
     */
    public boolean isDigitalCaptcha() {
        return this == SMS || this == EMAIL || this == VOICE;
    }

    @Override
    public String getColor() {
        switch (this) {
            case IMAGE:
                return "#1890FF";
            case SLIDER:
                return "#52C41A";
            case PUZZLE:
                return "#722ED1";
            case TEXT:
                return "#FAAD14";
            case ARITHMETIC:
                return "#FF7A45";
            case SMS:
                return "#13C2C2";
            case EMAIL:
                return "#2F54EB";
            case VOICE:
                return "#FF4D4F";
            default:
                return "#8C8C8C";
        }
    }

}