package com.dream.common.tool.utils;

import com.dream.common.core.enums.DateFormat;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 日期时间工具类
 *
 * @author dream
 * @see org.apache.commons.lang3.time.DateUtils
 * @see DateFormat
 */
@Slf4j
@SuppressWarnings({"unused", "WeakerAccess"})
public final class DateUtils extends org.apache.commons.lang3.time.DateUtils {

    // ==================== 格式常量 ====================

    /**
     * 默认日期时间格式：yyyy-MM-dd HH:mm:ss
     */
    public static final String DATE_TIME_PATTERN = DateFormat.DATE_TIME.getPattern();

    /**
     * 默认日期格式：yyyy-MM-dd
     */
    public static final String DATE_PATTERN = DateFormat.DATE.getPattern();

    /**
     * 默认时间格式：HH:mm:ss
     */
    public static final String TIME_PATTERN = DateFormat.TIME.getPattern();

    // ==================== 线程安全的 DateTimeFormatter ====================

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_PATTERN);

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern(TIME_PATTERN);

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    /**
     * 获取指定模式对应的格式化器。
     * <p>
     * {@code yyyy-MM-dd HH:mm:ss}、{@code yyyy-MM-dd}、{@code HH:mm:ss} 三种默认模式
     * 复用上方线程安全的静态实例，避免每次调用重复构建；其余模式按需创建。
     * </p>
     *
     * @param pattern 日期/时间模式，调用方保证非空白
     * @return 格式化器
     */
    private static DateTimeFormatter formatterOf(String pattern) {
        if (DATE_TIME_PATTERN.equals(pattern)) {
            return DATE_TIME_FORMATTER;
        }
        if (DATE_PATTERN.equals(pattern)) {
            return DATE_FORMATTER;
        }
        if (TIME_PATTERN.equals(pattern)) {
            return TIME_FORMATTER;
        }
        return DateTimeFormatter.ofPattern(pattern);
    }

    // ==================== 私有构造器 ====================

    private DateUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 当前时间获取 ====================

    /**
     * 获取当前系统时间（Date）
     *
     * @return 当前 Date
     */
    public static Date nowDate() {
        return new Date();
    }

    /**
     * 获取当前系统时间（LocalDateTime）
     *
     * @return 当前 LocalDateTime
     */
    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前系统日期（LocalDate）
     *
     * @return 当前 LocalDate
     */
    public static LocalDate nowLocalDate() {
        return LocalDate.now();
    }

    /**
     * 获取当前时间（LocalTime）
     *
     * @return 当前 LocalTime
     */
    public static LocalTime nowTime() {
        return LocalTime.now();
    }

    /**
     * 获取当前时间戳（毫秒）
     *
     * @return 当前毫秒级时间戳
     */
    public static long nowMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前日期时间字符串，格式：yyyy-MM-dd HH:mm:ss
     *
     * @return 当前日期时间字符串
     */
    public static String nowDateTimeStr() {
        return nowDateTimeStr(DATE_TIME_PATTERN);
    }

    /**
     * 获取当前日期时间字符串（指定格式）
     *
     * @param pattern 日期格式，如 "yyyy/MM/dd HH:mm:ss"
     * @return 当前日期时间字符串
     */
    public static String nowDateTimeStr(String pattern) {
        if (pattern == null) {
            pattern = DATE_TIME_PATTERN;
        }
        try {
            String result = LocalDateTime.now().format(formatterOf(pattern));
            if (log.isDebugEnabled()) {
                log.debug("nowDateTimeStr: {}", result);
            }
            return result;
        } catch (Exception e) {
            log.error("nowDateTimeStr error: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取当前日期字符串，格式：yyyy-MM-dd
     *
     * @return 当前日期字符串
     */
    public static String nowLocalDateStr() {
        return nowLocalDateStr(DATE_PATTERN);
    }

    /**
     * 获取当前日期字符串（指定格式）
     *
     * @param pattern 日期格式，如 "yyyy/MM/dd"
     * @return 当前日期字符串；
     */
    public static String nowLocalDateStr(String pattern) {
        if (pattern == null) {
            pattern = DATE_PATTERN;
        }
        try {
            String result = LocalDate.now().format(formatterOf(pattern));
            if (log.isDebugEnabled()) {
                log.debug("nowLocalDateStr: {}", result);
            }
            return result;
        } catch (Exception e) {
            log.error("nowLocalDateStr error: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取当前时间字符串，格式：HH:mm:ss
     *
     * @return 当前时间字符串
     */
    public static String nowTimeStr() {
        return nowTimeStr(TIME_PATTERN);
    }

    /**
     * 获取当前时间字符串，格式：HH:mm:ss
     *
     * @return 当前时间字符串
     */
    public static String nowTimeStr(String pattern) {
        if (pattern == null) {
            pattern = TIME_PATTERN;
        }
        try {
            String result = LocalTime.now().format(formatterOf(pattern));
            if (log.isDebugEnabled()) {
                log.debug("nowTimeStr: {}", result);
            }
            return result;
        } catch (Exception e) {
            log.error("nowTimeStr error: {}", e.getMessage());
        }
        return null;
    }

    // ==================== 解析（String → 对象） ====================

    /**
     * 解析日期时间字符串为 LocalDateTime（格式：yyyy-MM-dd HH:mm:ss）
     *
     * @param dateStr 日期时间字符串，如 "2026-07-18 10:30:00"
     * @return LocalDateTime；dateStr 为 null/空白 返回 null；解析失败返回 null
     */
    public static LocalDateTime parseDateTime(String dateStr) {
        return parseDateTime(dateStr, DATE_TIME_PATTERN);
    }

    /**
     * 解析日期时间字符串为 LocalDateTime（指定格式）
     *
     * @param dateStr 日期时间字符串
     * @param pattern 日期格式，如 "yyyy/MM/dd HH:mm:ss"
     * @return LocalDateTime；dateStr 为 null/空白 返回 null；解析失败返回 null
     */
    public static LocalDateTime parseDateTime(String dateStr, String pattern) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        if (StringUtils.isBlank(pattern)) {
            pattern = DATE_TIME_PATTERN;
        }
        try {
            LocalDateTime result = LocalDateTime.parse(dateStr.trim(), formatterOf(pattern));
            if (log.isDebugEnabled()) {
                log.debug("parseDateTime: [{}] pattern=[{}] → {}", dateStr, pattern, result);
            }
            return result;
        } catch (Exception e) {
            log.error("parseDateTime failed: dateStr={}, pattern={}", dateStr, pattern, e);
            return null;
        }
    }

    /**
     * 解析日期字符串为 LocalDate（格式：yyyy-MM-dd）
     *
     * @param dateStr 日期字符串，如 "2026-07-18"
     * @return LocalDate；dateStr 为 null/空白 返回 null；解析失败返回 null
     */
    public static LocalDate parseLocalDate(String dateStr) {
        return parseLocalDate(dateStr, DATE_PATTERN);
    }

    /**
     * 解析日期字符串为 LocalDate（指定格式）
     *
     * @param dateStr 日期字符串
     * @param pattern 日期格式，如 "yyyy/MM/dd"
     * @return LocalDate；dateStr 为 null/空白 返回 null；解析失败返回 null
     */
    public static LocalDate parseLocalDate(String dateStr, String pattern) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        if (StringUtils.isBlank(pattern)) {
            pattern = DATE_PATTERN;
        }
        try {
            LocalDate result = LocalDate.parse(dateStr.trim(), formatterOf(pattern));
            if (log.isDebugEnabled()) {
                log.debug("parseLocalDate: [{}] pattern=[{}] → {}", dateStr, pattern, result);
            }
            return result;
        } catch (Exception e) {
            log.error("parseLocalDate failed: dateStr={}, pattern={}", dateStr, pattern, e);
            return null;
        }
    }

    /**
     * 解析时间字符串为 LocalTime（格式：HH:mm:ss）
     *
     * @param dateStr 时间字符串，如 "10:30:00"
     * @return LocalTime；dateStr 为 null/空白 返回 null；解析失败返回 null
     */
    public static LocalTime parseTime(String dateStr) {
        return parseTime(dateStr, TIME_PATTERN);
    }

    /**
     * 解析时间字符串为 LocalTime（格式：HH:mm:ss）
     *
     * @param dateStr 时间字符串
     * @param pattern 时间格式，如 "HH:mm:ss"
     * @return LocalTime；dateStr 为 null/空白 返回 null；解析失败返回 null
     */
    public static LocalTime parseTime(String dateStr, String pattern) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        if (StringUtils.isBlank(pattern)) {
            pattern = TIME_PATTERN;
        }
        try {
            LocalTime result = LocalTime.parse(dateStr.trim(), formatterOf(pattern));
            if (log.isDebugEnabled()) {
                log.debug("parseTime: [{}] pattern=[{}] → {}", dateStr, pattern, result);
            }
            return result;
        } catch (Exception e) {
            log.error("parseTime failed: dateStr={}, pattern={}", dateStr, pattern, e);
            return null;
        }
    }

    /**
     * 解析日期字符串为 Date（指定格式）
     * <p>
     * 与父类 {@link #parseDate(String, String...)} 区分，避免方法签名冲突。
     * </p>
     *
     * @param dateStr 日期字符串
     * @param pattern 日期格式
     * @return Date；dateStr 为 null/空白 返回 null；解析失败返回 null
     */
    public static Date parseDateByPattern(String dateStr, String pattern) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        if (StringUtils.isBlank(pattern)) {
            pattern = DATE_TIME_PATTERN;
        }
        try {
            Date result = parseDate(dateStr, pattern);
            if (log.isDebugEnabled()) {
                log.debug("parseDateByPattern: [{}] pattern=[{}] → {}", dateStr, pattern, result);
            }
            return result;
        } catch (ParseException e) {
            log.error("parseDateByPattern failed: dateStr={}, pattern={}", dateStr, pattern, e);
            return null;
        }
    }

    /**
     * 解析为 Date
     *
     * @param dateStr 日期字符串
     * @return Date；无法匹配或解析失败返回 null  yyyy-MM-dd HH:mm:ss
     */
    public static Date parseDateByPattern(String dateStr) {
        return parseDateByPattern(dateStr, DATE_TIME_PATTERN);
    }

    /**
     * 自动匹配格式并解析为 Date
     * <p>
     * 尝试 {@link DateFormat} 枚举中定义的所有格式进行匹配。
     * </p>
     *
     * @param dateStr 日期字符串
     * @return Date；无法匹配或解析失败返回 null
     */
    public static Date parseAuto(String dateStr) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        DateFormat matched = DateFormat.matchPattern(dateStr);
        if (matched == null) {
            log.warn("parseAuto: cannot match pattern, dateStr={}", dateStr);
            return null;
        }
        try {
            Date result = parseDate(dateStr, matched.getValue());
            if (log.isDebugEnabled()) {
                log.debug("parseAuto: [{}] → {} (matched={})", dateStr, result, matched);
            }
            return result;
        } catch (ParseException e) {
            log.error("parseAuto failed: dateStr={}, matched={}", dateStr, matched, e);
            return null;
        }
    }

    // ==================== 格式化（对象 → String） ====================

    /**
     * 格式化 LocalDateTime 为字符串（格式：yyyy-MM-dd HH:mm:ss）
     *
     * @param dateTime LocalDateTime
     * @return 日期时间字符串；dateTime 为 null 返回 null
     */
    public static String format(LocalDateTime dateTime) {
        return format(dateTime, DATE_TIME_PATTERN);
    }

    /**
     * 格式化 LocalDateTime 为指定格式字符串
     *
     * @param dateTime LocalDateTime
     * @param pattern  日期格式
     * @return 日期时间字符串；dateTime 为 null 返回 null
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        try {
            if (StringUtils.isBlank(pattern)) {
                pattern = DATE_TIME_PATTERN;
            }
            return dateTime.format(formatterOf(pattern));
        } catch (Exception e) {
            log.error("format failed: dateTime={}, pattern={}", dateTime, pattern, e);
            return null;
        }
    }

    /**
     * 格式化 LocalDate 为字符串（格式：yyyy-MM-dd）
     *
     * @param date LocalDate
     * @return 日期字符串；date 为 null 返回 null
     */
    public static String format(LocalDate date) {
        return format(date, DATE_PATTERN);
    }

    /**
     * 格式化 LocalDate 为指定格式字符串
     *
     * @param date    LocalDate
     * @param pattern 日期格式
     * @return 日期字符串；date 为 null 返回 null
     */
    public static String format(LocalDate date, String pattern) {
        if (date == null) {
            return null;
        }
        try {
            if (StringUtils.isBlank(pattern)) {
                pattern = DATE_PATTERN;
            }
            return date.format(formatterOf(pattern));
        } catch (Exception e) {
            log.error("format failed: date={}, pattern={}", date, pattern, e);
            return null;
        }

    }

    /**
     * 格式化 Date 为字符串（格式：yyyy-MM-dd HH:mm:ss）
     *
     * @param date Date
     * @return 日期时间字符串；date 为 null 返回 null
     */
    public static String format(Date date) {
        return format(date, DATE_TIME_PATTERN);
    }

    /**
     * 格式化 Date 为指定格式字符串
     *
     * @param date    Date
     * @param pattern 日期格式
     * @return 日期时间字符串；date 为 null 返回 null
     */
    public static String format(Date date, String pattern) {
        if (date == null) {
            return null;
        }
        if (StringUtils.isBlank(pattern)) {
            pattern = DATE_TIME_PATTERN;
        }
        try {
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            return df.format(date);
        } catch (Exception e) {
            log.warn("format failed: date={}, pattern={}", date, pattern, e);
        }
        return null;
    }

    // ==================== 类型转换 ====================

    /**
     * Date → LocalDateTime
     *
     * @param date Date
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(date.toInstant(), SYSTEM_ZONE);
        } catch (Exception e) {
            log.error("toLocalDateTime failed: date={}", date, e);
            return null;
        }
    }

    /**
     * LocalDateTime → Date
     *
     * @param dateTime LocalDateTime
     * @return Date；dateTime 为 null 返回 null
     */
    public static Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        try {
            return Date.from(dateTime.atZone(SYSTEM_ZONE).toInstant());
        } catch (Exception e) {
            log.error("toDate failed: dateTime={}", dateTime, e);
            return null;
        }
    }

    /**
     * LocalDate → Date（当天 00:00:00）
     *
     * @param date LocalDate
     * @return Date；date 为 null 返回 null
     */
    public static Date toDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        try {
            return Date.from(date.atStartOfDay(SYSTEM_ZONE).toInstant());
        } catch (Exception e) {
            log.error("toDate failed: date={}", date, e);
            return null;
        }
    }

    /**
     * Date → LocalDate
     *
     * @param date Date
     * @return LocalDate；date 为 null 返回 null
     */
    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        try {
            return date.toInstant().atZone(SYSTEM_ZONE).toLocalDate();
        } catch (Exception e) {
            log.error("toLocalDate failed: date={}", date, e);
            return null;
        }
    }

    /**
     * 时间戳（毫秒）→ LocalDateTime
     *
     * @param epochMillis 毫秒级时间戳
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(long epochMillis) {
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), SYSTEM_ZONE);
        } catch (Exception e) {
            log.error("toLocalDateTime failed: epochMillis={}", epochMillis, e);
            return null;
        }
    }

    /**
     * LocalDateTime → 时间戳（毫秒）
     *
     * @param dateTime LocalDateTime
     * @return 毫秒级时间戳；dateTime 为 null 返回 null
     */
    public static Long toEpochMilli(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        try {
            return dateTime.atZone(SYSTEM_ZONE).toInstant().toEpochMilli();
        } catch (Exception e) {
            log.error("toEpochMilli failed: dateTime={}", dateTime, e);
            return null;
        }
    }

    /**
     * 日期字符串 → 时间戳（毫秒），格式：yyyy-MM-dd HH:mm:ss
     *
     * @param dateStr 日期时间字符串
     * @return 时间戳（毫秒）；解析失败返回 null
     */
    public static Long toEpochMilli(String dateStr) {
        LocalDateTime dt = parseDateTime(dateStr);
        return dt != null ? toEpochMilli(dt) : null;
    }

    // ==================== 日期加减 ====================

    /**
     * 通用日期加减（别名方法，对应父类 addXxx）
     *
     * @param dateTime 基准时间
     * @param amount   偏移量（正数=加，负数=减）
     * @param unit     时间单位（如 {@link ChronoUnit#DAYS}）
     * @return 偏移后的 LocalDateTime；dateTime 为 null 返回 null
     */
    public static LocalDateTime add(LocalDateTime dateTime, long amount, TemporalUnit unit) {
        if (dateTime == null || unit == null) {
            return dateTime;
        }
        return dateTime.plus(amount, unit);
    }

    /**
     * 加减天数（正数=加，负数=减）
     *
     * @param dateTime 基准时间
     * @param days     天数（正数=加，负数=减）
     * @return 偏移后的 LocalDateTime
     */
    public static LocalDateTime addDays(LocalDateTime dateTime, long days) {
        return add(dateTime, days, ChronoUnit.DAYS);
    }

    /**
     * 加减月数（正数=加，负数=减）
     *
     * @param dateTime 基准时间
     * @param months   月数（正数=加，负数=减）
     * @return 偏移后的 LocalDateTime
     */
    public static LocalDateTime addMonths(LocalDateTime dateTime, long months) {
        return add(dateTime, months, ChronoUnit.MONTHS);
    }

    /**
     * 加减年数（正数=加，负数=减）
     *
     * @param dateTime 基准时间
     * @param years    年数（正数=加，负数=减）
     * @return 偏移后的 LocalDateTime
     */
    public static LocalDateTime addYears(LocalDateTime dateTime, long years) {
        return add(dateTime, years, ChronoUnit.YEARS);
    }

    /**
     * 加减小时数（正数=加，负数=减）
     *
     * @param dateTime 基准时间
     * @param hours    小时数（正数=加，负数=减）
     * @return 偏移后的 LocalDateTime
     */
    public static LocalDateTime addHours(LocalDateTime dateTime, long hours) {
        return add(dateTime, hours, ChronoUnit.HOURS);
    }

    /**
     * 加减分钟数（正数=加，负数=减）
     *
     * @param dateTime 基准时间
     * @param minutes  分钟数（正数=加，负数=减）
     * @return 偏移后的 LocalDateTime
     */
    public static LocalDateTime addMinutes(LocalDateTime dateTime, long minutes) {
        return add(dateTime, minutes, ChronoUnit.MINUTES);
    }

    /**
     * 加减秒数（正数=加，负数=减）
     *
     * @param dateTime 基准时间
     * @param seconds  秒数（正数=加，负数=减）
     * @return 偏移后的 LocalDateTime
     */
    public static LocalDateTime addSeconds(LocalDateTime dateTime, long seconds) {
        return add(dateTime, seconds, ChronoUnit.SECONDS);
    }

    // ==================== 日期差值 ====================

    /**
     * 计算两个时间之间的差值（指定单位）
     * <p>
     * 结果 = start.until(end, unit)，即 end - start
     * </p>
     *
     * @param start 起始时间
     * @param end   结束时间
     * @param unit  时间单位
     * @return 差值（end - start）；任一参数为 null 返回 0
     */
    public static long diff(LocalDateTime start, LocalDateTime end, TemporalUnit unit) {
        if (start == null || end == null || unit == null) {
            return 0L;
        }
        return start.until(end, unit);
    }

    /**
     * 计算两个时间相差的天数（end - start）
     *
     * @param start 起始时间
     * @param end   结束时间
     * @return 相差天数
     */
    public static long diffDays(LocalDateTime start, LocalDateTime end) {
        return diff(start, end, ChronoUnit.DAYS);
    }

    /**
     * 计算两个时间相差的月数（end - start）
     *
     * @param start 起始时间
     * @param end   结束时间
     * @return 相差月数
     */
    public static long diffMonths(LocalDateTime start, LocalDateTime end) {
        return diff(start, end, ChronoUnit.MONTHS);
    }

    /**
     * 计算两个时间相差的年数（end - start）
     *
     * @param start 起始时间
     * @param end   结束时间
     * @return 相差年数
     */
    public static long diffYears(LocalDateTime start, LocalDateTime end) {
        return diff(start, end, ChronoUnit.YEARS);
    }

    /**
     * 计算两个时间相差的秒数（end - start）
     *
     * @param start 起始时间
     * @param end   结束时间
     * @return 相差秒数
     */
    public static long diffSeconds(LocalDateTime start, LocalDateTime end) {
        return diff(start, end, ChronoUnit.SECONDS);
    }

    /**
     * 计算指定时间与当前时间相差的天数（当前时间 - dateTime）
     *
     * @param dateTime 指定时间
     * @return 相差天数
     */
    public static long diffDaysToNow(LocalDateTime dateTime) {
        return diffDays(dateTime, LocalDateTime.now());
    }

    // ==================== 日边界（起止时间） ====================

    /**
     * 获取指定日期的开始时间（00:00:00）
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getDayStart(LocalDate date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.of(date, LocalTime.MIN);
    }

    /**
     * 获取指定日期的结束时间（23:59:59.999999999）
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getDayEnd(LocalDate date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.of(date, LocalTime.MAX);
    }

    /**
     * 获取当前日期的开始时间（00:00:00）
     *
     * @return LocalDateTime
     */
    public static LocalDateTime getTodayStart() {
        return getDayStart(LocalDate.now());
    }

    /**
     * 获取当前日期的结束时间（23:59:59.999999999）
     *
     * @return LocalDateTime
     */
    public static LocalDateTime getTodayEnd() {
        return getDayEnd(LocalDate.now());
    }

    // ==================== 周边界（起止时间） ====================

    /**
     * 获取指定日期所在周的开始时间（周一 00:00:00）
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getWeekStart(LocalDate date) {
        if (date == null) {
            return null;
        }
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return LocalDateTime.of(monday, LocalTime.MIN);
    }

    /**
     * 获取指定日期所在周的结束时间（周日 23:59:59.999999999）
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getWeekEnd(LocalDate date) {
        if (date == null) {
            return null;
        }
        LocalDate sunday = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return LocalDateTime.of(sunday, LocalTime.MAX);
    }

    /**
     * 获取当前周的开始时间（周一 00:00:00）
     *
     * @return LocalDateTime
     */
    public static LocalDateTime getThisWeekStart() {
        return getWeekStart(LocalDate.now());
    }

    /**
     * 获取当前周的结束时间（周日 23:59:59.999999999）
     *
     * @return LocalDateTime
     */
    public static LocalDateTime getThisWeekEnd() {
        return getWeekEnd(LocalDate.now());
    }

    /**
     * 获取上周的开始时间（周一 00:00:00）
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getLastWeekStart(LocalDate date) {
        if (date == null) {
            return null;
        }
        return getWeekStart(date.minusWeeks(1));
    }

    /**
     * 获取上周的结束时间（周日 23:59:59.999999999）
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getLastWeekEnd(LocalDate date) {
        if (date == null) {
            return null;
        }
        return getWeekEnd(date.minusWeeks(1));
    }

    // ==================== 月边界（起止时间） ====================

    /**
     * 获取指定日期所在月的开始时间（1日 00:00:00）
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getMonthStart(LocalDate date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.of(date.with(TemporalAdjusters.firstDayOfMonth()), LocalTime.MIN);
    }

    /**
     * 获取指定日期所在月的结束时间（月末 23:59:59.999999999）
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getMonthEnd(LocalDate date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.of(date.with(TemporalAdjusters.lastDayOfMonth()), LocalTime.MAX);
    }

    /**
     * 获取当前月的开始时间（1日 00:00:00）
     *
     * @return LocalDateTime
     */
    public static LocalDateTime getThisMonthStart() {
        return getMonthStart(LocalDate.now());
    }

    /**
     * 获取当前月的结束时间（月末 23:59:59.999999999）
     *
     * @return LocalDateTime
     */
    public static LocalDateTime getThisMonthEnd() {
        return getMonthEnd(LocalDate.now());
    }

    /**
     * 获取上月的开始时间（1日 00:00:00）
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getLastMonthStart(LocalDate date) {
        if (date == null) {
            return null;
        }
        return getMonthStart(date.minusMonths(1));
    }

    /**
     * 获取上月的结束时间（月末 23:59:59.999999999）
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getLastMonthEnd(LocalDate date) {
        if (date == null) {
            return null;
        }
        return getMonthEnd(date.minusMonths(1));
    }

    // ==================== 季度边界（起止时间） ====================

    /**
     * 获取指定日期所在季度的开始时间（季度首日 00:00:00）
     * <p>
     * 季度划分：Q1(1-3月)、Q2(4-6月)、Q3(7-9月)、Q4(10-12月)
     * </p>
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getQuarterStart(LocalDate date) {
        if (date == null) {
            return null;
        }
        int month = ((date.getMonthValue() - 1) / 3) * 3 + 1;
        return LocalDateTime.of(LocalDate.of(date.getYear(), month, 1), LocalTime.MIN);
    }

    /**
     * 获取指定日期所在季度的结束时间（季度末日 23:59:59.999999999）
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getQuarterEnd(LocalDate date) {
        if (date == null) {
            return null;
        }
        int quarter = (date.getMonthValue() - 1) / 3 + 1;
        int month = quarter * 3;
        Month lastMonth = Month.of(month);
        return LocalDateTime.of(
                LocalDate.of(date.getYear(), month, lastMonth.length(date.isLeapYear())),
                LocalTime.MAX);
    }

    /**
     * 获取当前季度的开始时间
     *
     * @return LocalDateTime
     */
    public static LocalDateTime getThisQuarterStart() {
        return getQuarterStart(LocalDate.now());
    }

    /**
     * 获取当前季度的结束时间
     *
     * @return LocalDateTime
     */
    public static LocalDateTime getThisQuarterEnd() {
        return getQuarterEnd(LocalDate.now());
    }

    // ==================== 半年边界（起止时间） ====================

    /**
     * 获取指定日期所在半年的开始时间
     * <p>
     * 上半年：1月1日 00:00:00，下半年：7月1日 00:00:00
     * </p>
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getHalfYearStart(LocalDate date) {
        if (date == null) {
            return null;
        }
        int month = date.getMonthValue() > 6 ? 7 : 1;
        return LocalDateTime.of(LocalDate.of(date.getYear(), month, 1), LocalTime.MIN);
    }

    /**
     * 获取指定日期所在半年的结束时间
     * <p>
     * 上半年：6月30日 23:59:59，下半年：12月31日 23:59:59
     * </p>
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getHalfYearEnd(LocalDate date) {
        if (date == null) {
            return null;
        }
        int month = date.getMonthValue() > 6 ? 12 : 6;
        Month lastMonth = Month.of(month);
        return LocalDateTime.of(
                LocalDate.of(date.getYear(), month, lastMonth.length(date.isLeapYear())),
                LocalTime.MAX);
    }

    /**
     * 获取当前半年的开始时间
     *
     * @return LocalDateTime
     */
    public static LocalDateTime getThisHalfYearStart() {
        return getHalfYearStart(LocalDate.now());
    }

    /**
     * 获取当前半年的结束时间
     *
     * @return LocalDateTime
     */
    public static LocalDateTime getThisHalfYearEnd() {
        return getHalfYearEnd(LocalDate.now());
    }

    // ==================== 年边界（起止时间） ====================

    /**
     * 获取指定日期所在年的开始时间（1月1日 00:00:00）
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getYearStart(LocalDate date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.of(date.with(TemporalAdjusters.firstDayOfYear()), LocalTime.MIN);
    }

    /**
     * 获取指定日期所在年的结束时间（12月31日 23:59:59.999999999）
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getYearEnd(LocalDate date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.of(date.with(TemporalAdjusters.lastDayOfYear()), LocalTime.MAX);
    }

    /**
     * 获取当前年的开始时间
     *
     * @return LocalDateTime
     */
    public static LocalDateTime getThisYearStart() {
        return getYearStart(LocalDate.now());
    }

    /**
     * 获取当前年的结束时间
     *
     * @return LocalDateTime
     */
    public static LocalDateTime getThisYearEnd() {
        return getYearEnd(LocalDate.now());
    }

    /**
     * 获取近一年的开始时间（去年同日+1天 00:00:00）
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getLastYearStart(LocalDate date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.of(date.minusYears(1).plusDays(1), LocalTime.MIN);
    }

    /**
     * 获取近一年的结束时间（当天 23:59:59）
     *
     * @param date LocalDate
     * @return LocalDateTime；date 为 null 返回 null
     */
    public static LocalDateTime getLastYearEnd(LocalDate date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.of(date, LocalTime.MAX);
    }

    // ==================== 年月列表 ====================

    /**
     * 获取指定日期前后 N 个月的年月列表（格式：yyyy-MM-dd HH:mm:ss）
     *
     * @param dateTime 基准时间
     * @param months   月数（正数=向后，负数=向前，0=仅当月）
     * @return 年月字符串列表；dateTime 为 null 返回空列表
     */
    public static List<String> listYearMonths(LocalDateTime dateTime, long months) {
        List<String> result = new ArrayList<>();
        if (dateTime == null) {
            return result;
        }
        if (months == 0) {
            result.add(format(dateTime));
            return result;
        }
        int count = (int) Math.abs(months);
        for (int i = 0; i <= count; i++) {
            LocalDateTime current = months > 0
                    ? dateTime.plusMonths(i)
                    : dateTime.plusMonths(months + i);
            result.add(format(current));
        }
        return result;
    }

    /**
     * 获取当前时间前后 N 个月的年月列表
     *
     * @param months 月数（正数=向后，负数=向前）
     * @return 年月字符串列表
     * @see #listYearMonths(LocalDateTime, long)
     */
    public static List<String> listYearMonths(long months) {
        return listYearMonths(LocalDateTime.now(), months);
    }

    // ==================== 日期比较 ====================

    /**
     * 判断两个时间是否相等
     *
     * @param a LocalDateTime
     * @param b LocalDateTime
     * @return true=相等；任一为 null 返回 false
     */
    public static boolean isEqual(LocalDateTime a, LocalDateTime b) {
        if (a == null || b == null) {
            return false;
        }
        return a.isEqual(b);
    }

    /**
     * 判断 a 是否在 b 之前
     *
     * @param a LocalDateTime
     * @param b LocalDateTime
     * @return true=a 在 b 之前；任一为 null 返回 false
     */
    public static boolean isBefore(LocalDateTime a, LocalDateTime b) {
        if (a == null || b == null) {
            return false;
        }
        return a.isBefore(b);
    }

    /**
     * 判断 a 是否在 b 之后
     *
     * @param a LocalDateTime
     * @param b LocalDateTime
     * @return true=a 在 b 之后；任一为 null 返回 false
     */
    public static boolean isAfter(LocalDateTime a, LocalDateTime b) {
        if (a == null || b == null) {
            return false;
        }
        return a.isAfter(b);
    }

    /**
     * 判断两个日期是否为同一天
     *
     * @param a LocalDateTime
     * @param b LocalDateTime
     * @return true=同一天；任一为 null 返回 false
     */
    public static boolean isSameDay(LocalDateTime a, LocalDateTime b) {
        if (a == null || b == null) {
            return false;
        }
        return a.toLocalDate().isEqual(b.toLocalDate());
    }

    // ==================== 日期校验 ====================

    /**
     * 判断字符串是否为有效的日期时间格式（yyyy-MM-dd HH:mm:ss）
     *
     * @param dateStr 日期字符串
     * @return true=格式有效
     */
    public static boolean isValidDateTime(String dateStr) {
        return parseDateTime(dateStr) != null;
    }

    /**
     * 判断字符串是否为有效的日期格式（yyyy-MM-dd）
     *
     * @param dateStr 日期字符串
     * @return true=格式有效
     */
    public static boolean isValidDate(String dateStr) {
        return parseLocalDate(dateStr) != null;
    }

    /**
     * 判断字符串是否为指定格式的有效日期
     *
     * @param dateStr 日期字符串
     * @param pattern 日期格式
     * @return true=格式有效
     */
    public static boolean isValidDate(String dateStr, String pattern) {
        return parseLocalDate(dateStr, pattern) != null;
    }

    /**
     * 判断是否为闰年
     *
     * @param year 年份
     * @return true=闰年
     */
    public static boolean isLeapYear(int year) {
        return LocalDate.of(year, 1, 1).isLeapYear();
    }

    /**
     * 获取指定日期的星期值（1=周一，7=周日）
     *
     * @param date LocalDate
     * @return 星期值（1-7）；date 为 null 返回 0
     */
    public static int getDayOfWeek(LocalDate date) {
        if (date == null) {
            return 0;
        }
        return date.getDayOfWeek().getValue();
    }

    /**
     * 获取指定日期是一年中的第几天
     *
     * @param date LocalDate
     * @return 天序号（1-366）；date 为 null 返回 0
     */
    public static int getDayOfYear(LocalDate date) {
        if (date == null) {
            return 0;
        }
        return date.getDayOfYear();
    }

    /**
     * 获取指定月份的天数
     *
     * @param year  年份
     * @param month 月份（1-12）
     * @return 该月天数
     */
    public static int getDaysOfMonth(int year, int month) {
        return YearMonth.of(year, month).lengthOfMonth();
    }
}