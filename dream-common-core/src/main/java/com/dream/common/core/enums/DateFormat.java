package com.dream.common.core.enums;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * 日期格式枚举
 *
 * @author dream
 */
public enum DateFormat implements BaseEnum<String> {

    // ==================== 默认格式 ====================

    DATE_TIME("yyyy-MM-dd HH:mm:ss", "日期时间",
            "^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}$"),

    DATE("yyyy-MM-dd", "日期",
            "^\\d{4}-\\d{1,2}-\\d{1,2}$"),

    TIME("HH:mm:ss", "时间",
            "^\\d{1,2}:\\d{1,2}:\\d{1,2}$"),

    // ==================== 完整日期时间 ====================

    DATE_TIME_MILLIS("yyyy-MM-dd HH:mm:ss:SSS", "日期时间(毫秒)",
            "^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d{1,3}$"),

    DATE_TIME_SLASH("yyyy/MM/dd HH:mm:ss", "日期时间(斜杠)",
            "^\\d{4}/\\d{1,2}/\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}$"),

    DATE_TIME_DOT("yyyy.MM.dd HH:mm:ss", "日期时间(点)",
            "^\\d{4}\\.\\d{1,2}\\.\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}$"),

    DATE_TIME_CHINESE("yyyy年MM月dd日 HH:mm:ss", "日期时间(中文)",
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

    DATE_TIME_COMPACT("yyyyMMddHHmmss", "紧凑日期时间",
            "^\\d{14}$"),

    DATE_TIME_COMPACT_MILLIS("yyyyMMddHHmmssSSS", "紧凑日期时间(毫秒)",
            "^\\d{17}$"),

    DATE_COMPACT("yyyyMMdd", "紧凑日期",
            "^\\d{8}$"),

    TIME_COMPACT("HHmmss", "紧凑时间",
            "^\\d{6}$");

    // 注意：TIMESTAMP_MILLIS 与 DATE_TIME_COMPACT_MILLIS 的 pattern 和 regex 完全相同（^\d{17}$）
    // TIMESTAMP_SECOND 与 DATE_TIME_COMPACT 的 pattern 和 regex 完全相同（^\d{14}$）
    // 为避免 matchPattern() 歧义，不再重复定义，请直接引用对应的紧凑格式枚举
    //
    // 也不收录数据库侧格式串（Oracle 的 'YYYY-MM-DD HH24:MI:SS'、MySQL/MyBatis 的 '%Y-%m-%d %H:%i:%s'）：
    // 本枚举的 pattern 一律交给 SimpleDateFormat / DateTimeFormatter 使用，上面两种写法不是合法的
    // Java pattern（'%Y' 会直接抛 IllegalArgumentException），只应出现在 SQL 语句里。

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
            case DATE_TIME:
            case DATE_TIME_MILLIS:
            case DATE_TIME_SLASH:
            case DATE_TIME_DOT:
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
            case DATE_TIME_COMPACT:
            case DATE_TIME_COMPACT_MILLIS:
            case DATE_COMPACT:
            case TIME_COMPACT:
                return "#FF7A45";
            case DATE_TIME_CHINESE:
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
     * 获取所有的 Pattern，用于校验
     */
    public static String[] getAllPatterns() {
        return Arrays.stream(values()).map(BaseEnum::getValue).toArray(String[]::new);
    }

    public static List<String> getWeekList(Language lang) {
        if (lang == null) {
            lang = Language.ZH_CN;
        }
        if (lang == Language.ZH_CN || lang == Language.ZH_TW || lang == Language.ZH_HK) {
            return Arrays.asList("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六");
        }
        return Arrays.asList("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");
    }

    public static List<String> getMonthList(Language lang) {
        if (lang == null) {
            lang = Language.ZH_CN;
        }
        if (lang == Language.ZH_CN || lang == Language.ZH_TW || lang == Language.ZH_HK) {
            return Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12");
        }
        return Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
    }

    /**
     * start 00:00:00
     */
    public static String getStartOfDay() {
        return "00:00:00";
    }

    /**
     * end 23:59:59
     */
    public static String getEndOfDay() {
        return "23:59:59";
    }

}