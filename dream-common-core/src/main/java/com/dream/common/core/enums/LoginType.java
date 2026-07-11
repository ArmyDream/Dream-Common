package com.dream.common.core.enums;

/**
 * 登录方式枚举
 *
 */
public enum LoginType implements BaseEnum<String> {

    /**
     * 账号密码登录
     */
    PASSWORD("password", "账号密码"),

    /**
     * 短信验证码登录
     */
    SMS("sms", "短信验证码"),

    /**
     * 邮箱验证码登录
     */
    EMAIL("email", "邮箱验证码"),

    /**
     * 微信扫码登录
     */
    WECHAT("wechat", "微信登录"),

    /**
     * 支付宝扫码登录
     */
    ALIPAY("alipay", "支付宝登录"),

    /**
     * GitHub OAuth 登录
     */
    GITHUB("github", "GitHub登录"),

    /**
     * Gitee OAuth 登录
     */
    GITEE("gitee", "Gitee登录"),

    /**
     * 企业微信登录
     */
    WECHAT_WORK("wechatWork", "企业微信登录"),

    /**
     * 钉钉登录
     */
    DINGTALK("dingtalk", "钉钉登录"),

    /**
     * 飞书登录
     */
    FEISHU("feishu", "飞书登录"),

    /**
     * 人脸识别登录
     */
    FACE("face", "人脸识别"),

    /**
     * 指纹登录
     */
    FINGERPRINT("fingerprint", "指纹登录"),

    /**
     * 第三方 SSO 单点登录
     */
    SSO("sso", "单点登录"),

    /**
     * 扫码登录（通用）
     */
    QR_CODE("qrCode", "扫码登录"),

    /**
     * 免密登录（信任设备）
     */
    TRUSTED("trusted", "免密登录");

    private final String value;
    private final String label;

    LoginType(String value, String label) {
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
     * 是否为第三方 OAuth 登录
     */
    public boolean isOAuth() {
        return this == GITHUB || this == GITEE || this == WECHAT
                || this == ALIPAY || this == WECHAT_WORK
                || this == DINGTALK || this == FEISHU;
    }

    /**
     * 是否为验证码登录
     */
    public boolean isCaptchaLogin() {
        return this == SMS || this == EMAIL;
    }

    /**
     * 是否为生物识别登录
     */
    public boolean isBiometric() {
        return this == FACE || this == FINGERPRINT;
    }

    @Override
    public String getColor() {
        switch (this) {
            case PASSWORD:
                return "#1890FF";
            case SMS:
            case EMAIL:
                return "#52C41A";
            case WECHAT:
                return "#07C160";
            case ALIPAY:
                return "#1677FF";
            case GITHUB:
                return "#24292F";
            case GITEE:
                return "#C71D23";
            case WECHAT_WORK:
                return "#2B7D3A";
            case DINGTALK:
                return "#1E8BFF";
            case FEISHU:
                return "#00A3E0";
            case FACE:
            case FINGERPRINT:
                return "#722ED1";
            case SSO:
                return "#FAAD14";
            case QR_CODE:
                return "#13C2C2";
            case TRUSTED:
                return "#8C8C8C";
            default:
                return "#8C8C8C";
        }
    }

    public static LoginType fromValue(String value) {
        return BaseEnum.fromValue(LoginType.class, value);
    }
}