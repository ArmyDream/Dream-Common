package com.dream.common.core.enums;

/**
 * 日期时间类别枚举
 * <p>
 * 用于按类别处理日期格式，而非具体格式字符串
 *
 * @author dream
 */
public enum DateTimeCategory implements BaseEnum<String> {

    /**
     * 仅日期（不含时间）
     */
    DATE("date", "日期"),
    /**
     * 仅时间（不含日期）
     */
    TIME("time", "时间"),
    /**
     * 日期 + 时间
     */
    DATETIME("datetime", "日期时间"),
    /**
     * 紧凑格式（无分隔符）
     */
    COMPACT("compact", "紧凑格式"),
    /**
     * 数据库专用
     */
    DB("db", "数据库专用"),
    /**
     * 其他
     */
    OTHER("other", "其他");
    ;

    private final String value;
    private final String label;

    DateTimeCategory(String value, String label) {
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
     * 判断是否包含日期部分
     */
    public boolean hasDate() {
        return this == DATE || this == DATETIME || this == COMPACT;
    }

    /**
     * 判断是否包含时间部分
     */
    public boolean hasTime() {
        return this == TIME || this == DATETIME || this == COMPACT;
    }

    public static DateTimeCategory fromValue(String value) {
        return BaseEnum.fromValue(DateTimeCategory.class, value);
    }
}