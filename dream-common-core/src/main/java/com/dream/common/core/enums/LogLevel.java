package com.dream.common.core.enums;

/**
 * 错误级别枚举
 *
 * @author dream
 */
public enum LogLevel implements BaseEnum<String> {

    /**
     * 信息
     */
    INFO("info", "信息", "#1890FF"),

    /**
     * 警告
     */
    WARN("warn", "警告", "#FAAD14"),

    /**
     * 错误
     */
    ERROR("error", "错误", "#FF4D4F"),

    /**
     * 调试
     */
    DEBUG("debug", "调试", "#52C41A"),

    /**
     * 跟踪
     */
    TRACE("trace", "跟踪", "#8C8C8C"),

    /**
     * 运行时
     */
    RUNTIME("runtime", "运行时", "#FF7A45"),

    /**
     * 其他
     */
    OTHER("other", "其他", "#8C8C8C"),

    /**
     * 无
     */
    NONE("none", "无", "#8C8C8C"),

    /**
     * 致命错误
     */
    FATAL("fatal", "致命", "#8C0000");

    private final String value;
    private final String label;
    private final String color;

    LogLevel(String value, String label, String color) {
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

    public static LogLevel fromValue(String value) {
        return BaseEnum.fromValue(LogLevel.class, value);
    }
}