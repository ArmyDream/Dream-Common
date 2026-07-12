package com.dream.common.core.enums;

/**
 * 通知类型 枚举
 *
 * @author dream
 */
public enum NotifyType implements BaseEnum<String> {

    EMAIL("email", "Email", "邮件通知"),
    SMS("sms", "SMS", "短信通知"),
    WECHAT("wechat", "WeChat", "微信通知"),
    DINGTALK("dingtalk", "DingTalk", "钉钉通知"),
    FEISHU("feishu", "Feishu", "飞书通知"),
    SYSTEM("system", "System", "系统通知"),
    TEMPLATE("template", "Template", "模板通知"),
    ALL("all", "All", "所有通知"),
    NONE("none", "None", "无通知"),

    ;

    private final String value;

    private final String labelEn;

    private final String labelZh;

    NotifyType(String value, String labelEn, String labelZh) {
        this.value = value;
        this.labelEn = labelEn;
        this.labelZh = labelZh;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return labelZh;
    }

    public String getLabelEn() {
        return labelEn;
    }

    public String getLabelZh() {
        return labelZh;
    }

    public static NotifyType fromValue(String value) {
        return BaseEnum.fromValue(NotifyType.class, value);
    }

    @Override
    public String getColor() {
        switch (this) {
            case EMAIL:
                return "#1890FF";
            case SMS:
                return "#52C41A";
            case WECHAT:
                return "#07C160";
            case DINGTALK:
                return "#1E8BFF";
            case FEISHU:
                return "#00A3E0";
            case SYSTEM:
                return "#722ED1";
            case TEMPLATE:
                return "#FAAD14";
            case ALL:
                return "#13C2C2";
            case NONE:
                return "#8C8C8C";
            default:
                return "#8C8C8C";
        }
    }
}
