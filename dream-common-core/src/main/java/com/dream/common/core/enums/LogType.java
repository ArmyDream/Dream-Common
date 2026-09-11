package com.dream.common.core.enums;

/**
 * 日志类型枚举
 *
 * @author dream
 */
public enum LogType implements BaseEnum<String> {

    LOGIN("login", "Login", "登录日志"),
    OPERATION("operation", "Operation", "操作日志"),
    EXCEPTION("exception", "Exception", "异常日志"),
    SYSTEM("system", "System", "系统日志"),
    SCHEDULE("schedule", "Schedule", "定时任务日志"),
    SUCCESS("success", "Success", "成功日志"),
    FAIL("fail", "Fail", "失败日志"),
    ACCESS("access", "Access", "访问日志");

    private final String value;

    private final String labelEn;

    private final String labelZh;

    LogType(String value, String labelEn, String labelZh) {
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

    public String getLabel(String lang) {
        if ("en".equalsIgnoreCase(lang)) {
            return labelEn;
        }
        return labelZh;
    }

    @Override
    public String getColor() {
        switch (this) {
            case LOGIN:
                return "#1890FF";
            case OPERATION:
                return "#52C41A";
            case EXCEPTION:
                return "#FF4D4F";
            case SYSTEM:
                return "#8C8C8C";
            case SCHEDULE:
                return "#722ED1";
            case SUCCESS:
                return "#52C41A";
            case FAIL:
                return "#FAAD14";
            case ACCESS:
                return "#13C2C2";
            default:
                return "#8C8C8C";
        }
    }

}
