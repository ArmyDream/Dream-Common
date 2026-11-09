package com.dream.common.tool.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 中国法定节假日工具类
 *
 * @author dream
 */
@Slf4j
@SuppressWarnings({"unused", "WeakerAccess"})
public final class ChinaHolidayUtils {

    // ==================== 常量 ====================

    /**
     * 日期格式化器
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 缓存各年份数据，由 {@link #registerYearData(YearHolidayData)} 填充
     */
    private static final Map<Integer, YearHolidayData> CACHE = new ConcurrentHashMap<>();

    // ==================== 私有构造器 ====================

    private ChinaHolidayUtils() {
        throw new UnsupportedOperationException("ChinaHolidayUtils is a utility class");
    }

    // ==================== 枚举定义 ====================

    /**
     * 星期几枚举
     */
    @Getter
    public enum WeekDay {
        /**
         * 周一
         */
        MONDAY(1, "周一", "Monday"),
        /**
         * 周二
         */
        TUESDAY(2, "周二", "Tuesday"),
        /**
         * 周三
         */
        WEDNESDAY(3, "周三", "Wednesday"),
        /**
         * 周四
         */
        THURSDAY(4, "周四", "Thursday"),
        /**
         * 周五
         */
        FRIDAY(5, "周五", "Friday"),
        /**
         * 周六
         */
        SATURDAY(6, "周六", "Saturday"),
        /**
         * 周日
         */
        SUNDAY(7, "周日", "Sunday");

        private final int value;
        private final String chineseName;
        private final String englishName;

        WeekDay(int value, String chineseName, String englishName) {
            this.value = value;
            this.chineseName = chineseName;
            this.englishName = englishName;
        }

        public boolean isWeekend() {
            return this == SATURDAY || this == SUNDAY;
        }

        /**
         * 判断是否为周六
         *
         * @return true=周六
         */
        public boolean isSaturday() {
            return this == SATURDAY;
        }

        /**
         * 判断是否为周日
         *
         * @return true=周日
         */
        public boolean isSunday() {
            return this == SUNDAY;
        }

        public static WeekDay fromValue(int value) {
            for (WeekDay day : values()) {
                if (day.value == value) {
                    return day;
                }
            }
            return null;
        }

        public static WeekDay fromLocalDate(LocalDate date) {
            return fromValue(date.getDayOfWeek().getValue());
        }
    }

    /**
     * 日期类型枚举
     */
    @Getter
    public enum DateType {
        /**
         * 工作日：正常周一至周五，需要上班
         */
        WORKDAY("workday", "工作日", true),
        /**
         * 周末休息：周六或周日正常休息
         */
        WEEKEND_REST("weekend_rest", "周末休息", false),
        /**
         * 法定节假日：国家法定假日，休息
         */
        LEGAL_HOLIDAY("legal_holiday", "法定节假日", false),
        /**
         * 调休工作日：周末需要调休上班
         */
        WORK_WEEKEND("work_weekend", "调休工作日", true);

        private final String code;
        private final String desc;
        private final boolean needWork;

        DateType(String code, String desc, boolean needWork) {
            this.code = code;
            this.desc = desc;
            this.needWork = needWork;
        }

    }

    // ==================== 数据类 ====================

    /**
     * 年度节假日数据
     */
    @Getter
    public static class YearHolidayData {
        private final int year;
        private final Map<String, Integer> wageMap = new HashMap<>();
        private final Map<String, String> holidayNameMap = new HashMap<>();
        private final Set<String> legalHolidays = new HashSet<>();
        private final Set<String> workWeekends = new HashSet<>();

        public YearHolidayData(int year) {
            this.year = year;
        }

        public boolean isLegalHoliday(String date) {
            return legalHolidays.contains(date);
        }

        public boolean isWorkWeekend(String date) {
            return workWeekends.contains(date);
        }

        public Integer getWage(String date) {
            return wageMap.get(date);
        }

        public String getHolidayName(String date) {
            return holidayNameMap.get(date);
        }
    }

    /**
     * 日期信息类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateInfo {
        /**
         * 日期字符串，格式 yyyy-MM-dd
         */
        private String date;

        /**
         * 年份
         */
        private Integer year;

        /**
         * 月份（1-12）
         */
        private Integer month;

        /**
         * 日期（1-31）
         */
        private Integer day;

        /**
         * 星期几数字（1-7，1=周一，7=周日）
         */
        private Integer dayOfWeek;

        /**
         * 中文星期名称
         */
        private String weekName;

        /**
         * 英文星期名称
         */
        private String weekNameEn;

        /**
         * 是否为周末
         */
        private Boolean isWeekend;

        /**
         * 日期类型代码
         */
        private String dateTypeCode;

        /**
         * 日期类型描述
         */
        private String dateTypeDesc;

        /**
         * 是否需要上班
         */
        private Boolean needWork;

        /**
         * 节假日名称（非节假日为 null）
         */
        private String holidayName;

        /**
         * 上班状态描述（如：正常上班、周六休息、元旦放假）
         */
        private String workStatusDesc;

        /**
         * 构造方法（用于内部构建）
         */
        public DateInfo(LocalDate date, DateType type, String holidayName, WeekDay weekDay) {
            this.date = date.format(DATE_FORMATTER);
            this.year = date.getYear();
            this.month = date.getMonthValue();
            this.day = date.getDayOfMonth();
            this.dayOfWeek = weekDay.getValue();
            this.weekName = weekDay.getChineseName();
            this.weekNameEn = weekDay.getEnglishName();
            this.isWeekend = weekDay.isWeekend();
            this.dateTypeCode = type.getCode();
            this.dateTypeDesc = type.getDesc();
            this.needWork = type.isNeedWork();
            this.holidayName = holidayName;
            this.workStatusDesc = generateWorkStatusDesc(type, weekDay, holidayName);
        }

        private String generateWorkStatusDesc(DateType type, WeekDay weekDay, String holidayName) {
            switch (type) {
                case WORKDAY:
                    return "正常上班";
                case WEEKEND_REST:
                    return weekDay.isSaturday() ? "周六休息" : "周日休息";
                case LEGAL_HOLIDAY:
                    return (holidayName != null ? holidayName : "法定假日") + "放假";
                case WORK_WEEKEND:
                    return (holidayName != null ? holidayName : "调休") + "调休上班";
                default:
                    return "未知";
            }
        }

        @Override
        public String toString() {
            return String.format("DateInfo{date=%s, weekName=%s, dateTypeDesc=%s, workStatusDesc=%s, holidayName=%s}",
                    date, weekName, dateTypeDesc, workStatusDesc,
                    holidayName == null ? "无" : holidayName);
        }
    }

    // ==================== 核心判断方法 ====================

    /**
     * 判断指定日期的类型
     *
     * @param date        日期
     * @param holidayData 年度节假日数据
     * @return 日期类型
     */
    private static DateType getDateType(LocalDate date, YearHolidayData holidayData) {
        String dateStr = date.format(DATE_FORMATTER);

        if (holidayData.isWorkWeekend(dateStr)) {
            return DateType.WORK_WEEKEND;
        }

        if (holidayData.isLegalHoliday(dateStr)) {
            return DateType.LEGAL_HOLIDAY;
        }

        WeekDay weekDay = WeekDay.fromLocalDate(date);
        return weekDay.isWeekend() ? DateType.WEEKEND_REST : DateType.WORKDAY;
    }

    /**
     * 判断指定日期是否为周末（周六或周日）
     *
     * @param date 日期
     * @return true=周末
     */
    public static boolean isWeekend(LocalDate date) {
        if (date == null) {
            return false;
        }
        WeekDay weekDay = WeekDay.fromLocalDate(date);
        return weekDay.isWeekend();
    }

    // ==================== 缓存管理 ====================

    /**
     * 注册某一年度的节假日数据。
     * <p>
     * 框架不内置外部 HTTP 依赖，节假日与调休数据由业务从国务院公告、第三方 API 或数据库获取后，
     * 组装为 {@link YearHolidayData} 注册进来（建议应用启动时加载、定期刷新）。
     * 未注册年份的日期判断会退化为「仅按周末」，见 {@link #getDateType(LocalDate)}。
     * </p>
     * <pre>{@code
     * YearHolidayData data = new YearHolidayData(2026);
     * data.getLegalHolidays().add("2026-01-01");
     * data.getWorkWeekends().add("2026-02-14");
     * ChinaHolidayUtils.registerYearData(data);
     * }</pre>
     *
     * @param data 年度节假日数据，为 null 时忽略
     */
    public static void registerYearData(YearHolidayData data) {
        if (data == null) {
            return;
        }
        CACHE.put(data.getYear(), data);
    }

    /**
     * 判断指定日期的类型。
     * <p>已调用 {@link #registerYearData(YearHolidayData)} 注册该年份数据时，
     * 会区分法定节假日、调休工作日；未注册时退化为仅按周末判断，并输出一次告警提示。</p>
     *
     * @param date 日期，为 null 时返回 null
     * @return 日期类型
     */
    public static DateType getDateType(LocalDate date) {
        if (date == null) {
            return null;
        }
        YearHolidayData holidayData = CACHE.get(date.getYear());
        if (holidayData == null) {
            log.warn("{} 年节假日数据未注册，日期类型仅按周末判断，请先调用 registerYearData", date.getYear());
            WeekDay weekDay = WeekDay.fromLocalDate(date);
            return weekDay.isWeekend() ? DateType.WEEKEND_REST : DateType.WORKDAY;
        }
        return getDateType(date, holidayData);
    }

    /**
     * 清除指定年份的缓存
     *
     * @param year 年份
     */
    public static void clearCache(int year) {
        CACHE.remove(year);
        log.info("清除 {} 年缓存数据", year);
    }

    /**
     * 清除所有缓存
     */
    public static void clearAllCache() {
        CACHE.clear();
        log.info("清除所有缓存数据");
    }

    /**
     * 获取当前缓存年份列表
     *
     * @return 已缓存的年份列表
     */
    public static Set<Integer> getCachedYears() {
        return Collections.unmodifiableSet(CACHE.keySet());
    }
}