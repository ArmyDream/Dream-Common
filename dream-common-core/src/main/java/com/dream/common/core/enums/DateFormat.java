package com.dream.common.core.enums;

/**
 * 日期格式枚举
 *
 * @author dream
 */
public enum DateFormat implements BaseEnum<String> {

    // ==================== 默认格式 ====================

    DATETIME("yyyy-MM-dd HH:mm:ss", "日期时间",
            "^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}$"),

    DATE("yyyy-MM-dd", "日期",
            "^\\d{4}-\\d{1,2}-\\d{1,2}$"),

    TIME("HH:mm:ss", "时间",
            "^\\d{1,2}:\\d{1,2}:\\d{1,2}$"),

    // ==================== 完整日期时间 ====================

    DATETIME_MILLIS("yyyy-MM-dd HH:mm:ss:SSS", "日期时间(毫秒)",
            "^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d{1,3}$"),

    DATETIME_SLASH("yyyy/MM/dd HH:mm:ss", "日期时间(斜杠)",
            "^\\d{4}/\\d{1,2}/\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}$"),

    DATETIME_DOT("yyyy.MM.dd HH:mm:ss", "日期时间(点)",
            "^\\d{4}\\.\\d{1,2}\\.\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}$"),

    DATETIME_CHINESE("yyyy年MM月dd日 HH:mm:ss", "日期时间(中文)",
            "^\\d{4}年\\d{1,2}月\\d{1,2}日 \\d{1,2}:\\d{1,2}:\\d{1,2}$"),

    // ==================== 日期（不含时间） ====================

    DATE_SLASH("yyyy/MM/dd", "日期(斜杠)",
            "^\\d{4}/\\d{1,2}/\\d{1,2}$"),

    DATE_CHINESE("yyyy年MM月dd日", "日期(中文)",
            "^\\d{4}年\\d{1,2}月\\d{1,2}日$"),

    YEAR_MONTH("yyyy-MM", "年月",
            "^\\d{4}-\\d{1,2}$"),

    YEAR_MONTH_SLASH("yyyy/MM", "年月(斜杠)",
            "^\\d{4}/\\d{1,2}$"),

    MONTH_DAY("MM-dd", "月日",
            "^\\d{1,2}-\\d{1,2}$"),

    MONTH_DAY_SLASH("MM/dd", "月日(斜杠)",
            "^\\d{1,2}/\\d{1,2}$"),

    // ==================== 时间（不含日期） ====================

    TIME_MILLIS("HH:mm:ss:SSS", "时间(毫秒)",
            "^\\d{1,2}:\\d{1,2}:\\d{1,2}:\\d{1,3}$"),

    TIME_HOUR_MINUTE("HH:mm", "时分",
            "^\\d{1,2}:\\d{1,2}$"),

    // ==================== 紧凑格式（无分隔符） ====================

    DATETIME_COMPACT("yyyyMMddHHmmss", "紧凑日期时间",
            "^\\d{14}$"),

    DATETIME_COMPACT_MILLIS("yyyyMMddHHmmssSSS", "紧凑日期时间(毫秒)",
            "^\\d{17}$"),

    DATE_COMPACT("yyyyMMdd", "紧凑日期",
            "^\\d{8}$"),

    TIME_COMPACT("HHmmss", "紧凑时间",
            "^\\d{6}$"),

    // ==================== 数据库专用 ====================

    ORACLE_DB("YYYY-MM-DD HH24:MI:SS", "Oracle数据库", null),

    MYBATIS_DB("%Y-%m-%d %H:%i:%s", "MyBatis", null);

    // 注意：TIMESTAMP_MILLIS 与 DATETIME_COMPACT_MILLIS 的 pattern 和 regex 完全相同（^\d{17}$）
    // TIMESTAMP_SECOND 与 DATETIME_COMPACT 的 pattern 和 regex 完全相同（^\d{14}$）
    // 为避免 matchPattern() 歧义，不再重复定义，请直接引用对应的紧凑格式枚举

    private final String pattern;
    private final String label;
    private final String regex;

    DateFormat(String pattern, String label, String regex) {
        this.pattern = pattern;
        this.label = label;
        this.regex = regex;
    }

    @Override
    public String getValue() {
        return pattern;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public String getPattern() {
        return pattern;
    }

    public String getRegex() {
        return regex;
    }

    @Override
    public String getColor() {
        switch (this) {
            case DATETIME:
            case DATETIME_MILLIS:
            case DATETIME_SLASH:
            case DATETIME_DOT:
                return "#1890FF";
            case DATE:
            case DATE_SLASH:
                return "#52C41A";
            case YEAR_MONTH:
            case YEAR_MONTH_SLASH:
            case MONTH_DAY:
            case MONTH_DAY_SLASH:
                return "#FAAD14";
            case TIME:
            case TIME_MILLIS:
            case TIME_HOUR_MINUTE:
                return "#722ED1";
            case DATETIME_COMPACT:
            case DATETIME_COMPACT_MILLIS:
            case DATE_COMPACT:
            case TIME_COMPACT:
                return "#FF7A45";
            case ORACLE_DB:
            case MYBATIS_DB:
                return "#8C8C8C";
            case DATETIME_CHINESE:
            case DATE_CHINESE:
                return "#FF4D4F";
            default:
                return "#8C8C8C";
        }
    }

    // ==================== 静态方法 ====================

    /**
     * 根据日期字符串自动匹配对应的格式枚举
     *
     * @param dateStr 日期字符串
     * @return 匹配的格式枚举，未匹配返回 null
     */
    public static DateFormat matchPattern(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        String trimmed = dateStr.trim();

        for (DateFormat format : values()) {
            String regex = format.getRegex();
            if (regex != null && trimmed.matches(regex)) {
                return format;
            }
        }

        return null;
    }

    /**
     * 根据日期字符串自动匹配对应的格式，返回格式字符串
     *
     * @param dateStr 日期字符串
     * @return 匹配的格式字符串，未匹配返回 null
     */
    public static String matchPatternStr(String dateStr) {
        DateFormat format = matchPattern(dateStr);
        return format != null ? format.getPattern() : null;
    }

    /**
     * 根据日期字符串自动匹配对应的格式，带默认值
     *
     * @param dateStr       日期字符串
     * @param defaultFormat 未匹配时的默认格式
     * @return 匹配的格式枚举
     */
    public static DateFormat matchPatternOrDefault(String dateStr, DateFormat defaultFormat) {
        DateFormat matched = matchPattern(dateStr);
        return matched != null ? matched : defaultFormat;
    }

    public static DateFormat fromPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return null;
        }
        for (DateFormat format : values()) {
            if (format.pattern.equals(pattern)) {
                return format;
            }
        }
        return null;
    }

    public static DateFormat fromValue(String value) {
        return BaseEnum.fromValue(DateFormat.class, value);
    }
}