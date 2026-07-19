package com.dream.common.tool.utils;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 超期计算工具类
 * <p>
 * 提供基于<strong>工作日</strong>的超期判断与计算能力，自动排除节假日（周末 + 法定假日 + 自定义假日）。
 * 支持两种精度模式：
 * </p>
 * <ul>
 *   <li><strong>天级模式</strong>（yyyy-MM-dd）：按工作日天数计算，适用于合同到期、还款日、审批流程等业务</li>
 *   <li><strong>毫秒级模式</strong>（yyyy-MM-dd HH:mm:ss:SSS）：按实际毫秒差计算，排除节假日后折算，适用于 SLA 计时、工单超时等</li>
 * </ul>
 * <p>
 * <b>重要说明：</b>
 * <br>
 * 工作日计算规则：
 * <ul>
 *   <li><b>天级模式</b>：开始日期当天计入第一个工作日。应完成日期 = 从开始日期起连续数够 limitDays 个工作日。</li>
 *   <li><b>毫秒级模式</b>：从开始时刻起算，有效毫秒 = 总毫秒 - 期间非工作日整天占用的毫秒数。</li>
 * </ul>
 * </p>
 *
 * <h2>节假日配置</h2>
 * <p>
 * 通过 {@link OverdueConfig} 配置节假日数据源，支持三种方式：
 * <ol>
 *   <li>静态集合：直接传入假日 Set 和调休工作日 Set</li>
 *   <li>动态 Supplier：每次调用时实时获取（适用于从数据库/API 动态加载）</li>
 *   <li>默认配置：仅排除周末，不排除法定假日</li>
 * </ol>
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>
 * // 1. 创建配置（使用 ChinaHolidayUtils 的静态数据）
 * OverdueConfig config = OverdueConfig.builder()
 *     .holidays(ChinaHolidayUtils.getHolidayDates())
 *     .adjustmentWorkdays(ChinaHolidayUtils.getWorkdayDates())
 *     .build();
 *
 * // 2. 检查超期（返回完整结果）
 * // 2026-07-13（周一）开始，限制 2 个工作日，应完成日期为 2026-07-14（周二）
 * OverdueResult result = OverdueUtils.checkOverdue(
 *     LocalDate.of(2026, 7, 13),
 *     LocalDate.of(2026, 7, 15),
 *     2, config
 * );
 * // 实际完成 2026-07-15，比应完成日期晚 1 个工作日，超期 1 天
 *
 * // 3. 判断是否超期
 * if (result.isOverdue()) {
 *     System.out.println("超期天数：" + result.getOverdueDays());
 *     System.out.println("非工作日：" + result.getNonWorkdayDates());
 * }
 *
 * // 4. 计算到期日期
 * // 从 2026-07-13（周一）开始，3 个工作日后到期：2026-07-15（周三）
 * LocalDate dueDate = OverdueUtils.calcDueDate(
 *     LocalDate.of(2026, 7, 13), 3, config
 * );
 *
 * // 5. 毫秒级超期计算
 * long millis = OverdueUtils.calcEffectiveMillis(
 *     LocalDateTime.of(2026, 7, 13, 9, 0, 0),
 *     LocalDateTime.of(2026, 7, 15, 18, 0, 0),
 *     config
 * );
 * </pre>
 *
 * @author dream
 * @see ChinaHolidayUtils
 * @see DateUtils
 */
@Slf4j
@SuppressWarnings({"unused", "WeakerAccess"})
public final class OverdueUtils {

    // ==================== 常量 ====================

    /** 天级格式：yyyy-MM-dd */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    /** 毫秒级格式：yyyy-MM-dd HH:mm:ss:SSS */
    public static final String MILLIS_PATTERN = "yyyy-MM-dd HH:mm:ss:SSS";

    /** 日期格式化器（线程安全） */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    /** 毫秒级格式化器（线程安全） */
    private static final DateTimeFormatter MILLIS_FORMATTER = DateTimeFormatter.ofPattern(MILLIS_PATTERN);

    /** 最大循环次数（防止死循环，365天 * 10年） */
    private static final int MAX_LOOP = 3650;

    // ==================== 私有构造器 ====================

    private OverdueUtils() {
        throw new UnsupportedOperationException("OverdueUtils is a utility class");
    }

    // ==================== 配置类 ====================

    /**
     * 超期计算配置
     * <p>
     * 封装节假日和调休工作日数据源，支持静态集合和动态 Supplier 两种方式。
     * </p>
     */
    @Data
    @Builder
    public static class OverdueConfig {

        /** 假日集合（法定假日 + 自定义假日，这些天不计入工作日） */
        @Builder.Default
        private final Set<LocalDate> holidays = Collections.emptySet();

        /** 调休工作日集合（周末需上班的日期，这些天计入工作日） */
        @Builder.Default
        private final Set<LocalDate> adjustmentWorkdays = Collections.emptySet();

        /** 动态假日数据源（优先级高于静态 holidays） */
        private final Supplier<Set<LocalDate>> holidaySupplier;

        /** 动态调休工作日数据源（优先级高于静态 adjustmentWorkdays） */
        private final Supplier<Set<LocalDate>> adjustmentWorkdaySupplier;

        /**
         * 创建默认配置（仅排除周末）
         */
        public static OverdueConfig defaultConfig() {
            return OverdueConfig.builder().build();
        }

        /**
         * 创建仅含静态集合的配置
         *
         * @param holidays           假日集合
         * @param adjustmentWorkdays 调休工作日集合
         * @return OverdueConfig
         */
        public static OverdueConfig of(Set<LocalDate> holidays, Set<LocalDate> adjustmentWorkdays) {
            return OverdueConfig.builder()
                    .holidays(holidays != null ? holidays : Collections.emptySet())
                    .adjustmentWorkdays(adjustmentWorkdays != null ? adjustmentWorkdays : Collections.emptySet())
                    .build();
        }

        /**
         * 创建仅含动态 Supplier 的配置
         *
         * @param holidaySupplier           假日数据源
         * @param adjustmentWorkdaySupplier 调休工作日数据源
         * @return OverdueConfig
         */
        public static OverdueConfig ofSupplier(Supplier<Set<LocalDate>> holidaySupplier,
                                               Supplier<Set<LocalDate>> adjustmentWorkdaySupplier) {
            return OverdueConfig.builder()
                    .holidaySupplier(holidaySupplier)
                    .adjustmentWorkdaySupplier(adjustmentWorkdaySupplier)
                    .build();
        }

        /**
         * 获取当前有效的假日集合
         * <p>优先使用 Supplier，其次使用静态集合，都没有返回空集合</p>
         *
         * @return 假日集合
         */
        public Set<LocalDate> resolveHolidays() {
            if (holidaySupplier != null) {
                Set<LocalDate> result = holidaySupplier.get();
                return result != null ? result : Collections.emptySet();
            }
            return holidays != null ? holidays : Collections.emptySet();
        }

        /**
         * 获取当前有效的调休工作日集合
         *
         * @return 调休工作日集合
         */
        public Set<LocalDate> resolveAdjustmentWorkdays() {
            if (adjustmentWorkdaySupplier != null) {
                Set<LocalDate> result = adjustmentWorkdaySupplier.get();
                return result != null ? result : Collections.emptySet();
            }
            return adjustmentWorkdays != null ? adjustmentWorkdays : Collections.emptySet();
        }

        /**
         * 判断当前配置是否为空（无任何节假日配置）
         */
        public boolean isEmpty() {
            return resolveHolidays().isEmpty() && resolveAdjustmentWorkdays().isEmpty();
        }
    }

    // ==================== DTO 类 ====================

    /**
     * 非工作日记录
     */
    @Data
    public static class NonWorkdayRecord {
        /** 非工作日日期 */
        private final LocalDate date;
        /** 格式化日期字符串（yyyy-MM-dd） */
        private final String dateStr;

        public NonWorkdayRecord(LocalDate date) {
            this.date = date;
            this.dateStr = date != null ? date.format(DATE_FORMATTER) : "";
        }
    }

    /**
     * 超期计算结果
     */
    @Data
    @Builder
    public static class OverdueResult {
        /** 是否超期 */
        private final boolean overdue;

        /** 超期天数（工作日，正数=已超期） */
        private final long overdueDays;

        /** 阶段开始日期 */
        private final LocalDate startDate;

        /** 阶段实际完成日期 */
        private final LocalDate actualCompleteDate;

        /** 阶段应完成日期 */
        private final LocalDate expectedCompleteDate;

        /** 阶段限制天数（工作日） */
        private final int limitDays;

        /** 非工作日列表 */
        @Builder.Default
        private final List<NonWorkdayRecord> nonWorkdays = Collections.emptyList();

        /**
         * 获取格式化的超期描述
         */
        public String getFormattedDescription() {
            if (!overdue) {
                long remaining = -overdueDays;
                return "未超期，剩余" + remaining + "个工作日";
            }
            return "超期" + overdueDays + "天";
        }

        /**
         * 获取非工作日统计摘要
         */
        public String getNonWorkdaySummary() {
            if (nonWorkdays.isEmpty()) {
                return "无非工作日";
            }
            return "共" + nonWorkdays.size() + "个非工作日";
        }

        /**
         * 获取非工作日日期列表（逗号分隔）
         */
        public String getNonWorkdayDates() {
            if (nonWorkdays.isEmpty()) {
                return "";
            }
            return nonWorkdays.stream()
                    .map(NonWorkdayRecord::getDateStr)
                    .collect(Collectors.joining(", "));
        }
    }

    // ==================== 核心方法：检查超期 ====================

    /**
     * 检查超期（返回完整结果）
     * <p>
     * <b>工作日计算规则：</b>
     * <br>
     * 从 startDate 当天开始计数，每遇到一个工作日计数 +1，跳过非工作日。
     * 当计数达到 limitDays 时，当前日期即为应完成日期。
     * </p>
     * <p>
     * <b>超期天数计算：</b>
     * <br>
     * 超期天数 = 从应完成日期的下一天到实际完成日期之间的工作日数。
     * 如果实际完成日期等于应完成日期，则超期天数为 0。
     * </p>
     *
     * @param startDate     开始日期（当天计入第一个工作日）
     * @param actualEndDate 实际完成日期（null 则使用当前日期）
     * @param limitDays     限制工作日天数（必须 > 0）
     * @param config        节假日配置
     * @return 超期结果
     */
    public static OverdueResult checkOverdue(LocalDate startDate,
                                             LocalDate actualEndDate,
                                             int limitDays,
                                             OverdueConfig config) {
        if (startDate == null || config == null) {
            log.warn("checkOverdue: startDate or config is null");
            return OverdueResult.builder().build();
        }

        if (limitDays <= 0) {
            log.warn("checkOverdue: limitDays={} <= 0, treated as 1", limitDays);
            limitDays = 1;
        }

        // 实际完成日期
        LocalDate endDate = actualEndDate != null ? actualEndDate : LocalDate.now();

        // 计算应完成日期
        Set<LocalDate> holidays = config.resolveHolidays();
        Set<LocalDate> adjWorkdays = config.resolveAdjustmentWorkdays();

        LocalDate current = startDate;
        int counted = 0;
        List<NonWorkdayRecord> nonWorkdays = new ArrayList<>();

        // 从 startDate 开始计数，先判断当前日期是否为工作日
        while (counted < limitDays) {
            if (isWorkingDay(current, holidays, adjWorkdays)) {
                counted++;
            } else {
                nonWorkdays.add(new NonWorkdayRecord(current));
            }
            if (counted < limitDays) {
                current = current.plusDays(1);
            }
        }
        LocalDate expectedDate = current;

        // 计算超期天数
        long overdueDays = 0L;
        boolean isOverdue = false;
        if (endDate.isAfter(expectedDate)) {
            isOverdue = true;
            // 超期天数 = 从 expectedDate 的下一天到 endDate 之间的工作日数
            overdueDays = calcWorkDaysBetween(expectedDate.plusDays(1), endDate, config);
        }

        if (log.isDebugEnabled()) {
            log.debug("checkOverdue: start={}, end={}, expected={}, limit={}, overdue={}, days={}",
                    startDate, endDate, expectedDate, limitDays, isOverdue, overdueDays);
        }

        return OverdueResult.builder()
                .overdue(isOverdue)
                .overdueDays(overdueDays)
                .startDate(startDate)
                .actualCompleteDate(endDate)
                .expectedCompleteDate(expectedDate)
                .limitDays(limitDays)
                .nonWorkdays(nonWorkdays)
                .build();
    }

    /**
     * 检查超期（使用默认配置）
     */
    public static OverdueResult checkOverdue(LocalDate startDate, LocalDate actualEndDate, int limitDays) {
        return checkOverdue(startDate, actualEndDate, limitDays, OverdueConfig.defaultConfig());
    }

    /**
     * 检查超期（字符串入参）
     */
    public static OverdueResult checkOverdue(String startDateStr,
                                             String actualEndDateStr,
                                             int limitDays,
                                             OverdueConfig config) {
        LocalDate startDate = parseDate(startDateStr);
        LocalDate endDate = parseDate(actualEndDateStr);
        if (startDate == null || config == null) {
            log.warn("checkOverdue: parse failed, start={}", startDateStr);
            return OverdueResult.builder().build();
        }
        return checkOverdue(startDate, endDate, limitDays, config);
    }

    // ==================== 天级计算 ====================

    /**
     * 计算两个日期之间的工作日天数（含 start 当天，含 end 当天）
     * <p>
     * <b>示例：</b>
     * <br>
     * calcWorkDaysBetween(2026-07-13, 2026-07-15, config) = 3
     * <br>
     * 因为 7-13（周一）、7-14（周二）、7-15（周三）都是工作日
     * </p>
     *
     * @param start  起始日期（含当天）
     * @param end    截止日期（含当天）
     * @param config 节假日配置
     * @return 工作日天数差值（end - start，可为负数）
     */
    public static long calcWorkDaysBetween(LocalDate start, LocalDate end, OverdueConfig config) {
        if (start == null || end == null || config == null) {
            return 0L;
        }

        Set<LocalDate> holidays = config.resolveHolidays();
        Set<LocalDate> adjWorkdays = config.resolveAdjustmentWorkdays();

        if (start.isEqual(end)) {
            return isWorkingDay(start, holidays, adjWorkdays) ? 1L : 0L;
        }

        boolean forward = end.isAfter(start);
        LocalDate from = forward ? start : end;
        LocalDate to = forward ? end : start;

        long count = 0L;
        LocalDate current = from;
        int loop = 0;

        while (!current.isAfter(to) && loop < MAX_LOOP) {
            if (isWorkingDay(current, holidays, adjWorkdays)) {
                count++;
            }
            current = current.plusDays(1);
            loop++;
        }

        if (loop >= MAX_LOOP) {
            log.warn("calcWorkDaysBetween: reached MAX_LOOP({})", MAX_LOOP);
        }

        return forward ? count : -count;
    }

    /**
     * 判断两个日期之间是否超期（天级）
     *
     * @param start     开始日期（当天计入第一个工作日）
     * @param end       截止日期
     * @param limitDays 限制工作日天数
     * @param config    节假日配置
     * @return true=已超期
     */
    public static boolean isOverdueDays(LocalDate start, LocalDate end, int limitDays, OverdueConfig config) {
        if (start == null || end == null || config == null) {
            return false;
        }
        long workDays = calcWorkDaysBetween(start, end, config);
        return workDays > limitDays;
    }

    /**
     * 计算超期天数（天级）
     * <p>
     * 超期天数 = 从 start 到 end 的工作日天数 - limitDays
     * 如果差值 ≤ 0 则返回 0
     * </p>
     *
     * @param start     开始日期
     * @param end       截止日期
     * @param limitDays 限制工作日天数
     * @param config    节假日配置
     * @return 超期工作日天数（正数=已超期）
     */
    public static long calcOverdueDays(LocalDate start, LocalDate end, int limitDays, OverdueConfig config) {
        if (start == null || end == null || config == null) {
            return 0L;
        }
        long workDays = calcWorkDaysBetween(start, end, config);
        return Math.max(0, workDays - limitDays);
    }

    /**
     * 计算到期日期（从起始日加 N 个工作日）
     * <p>
     * <b>示例：</b>
     * <br>
     * calcDueDate(2026-07-13, 3, config) = 2026-07-15
     * <br>
     * 因为 7-13（周一）第1个工作日，7-14（周二）第2个，7-15（周三）第3个
     * </p>
     *
     * @param startDate 开始日期（当天计入第一个工作日）
     * @param workDays  工作日天数（必须 > 0）
     * @param config    节假日配置
     * @return 到期日期
     */
    public static LocalDate calcDueDate(LocalDate startDate, int workDays, OverdueConfig config) {
        if (startDate == null || config == null) {
            return startDate;
        }

        if (workDays <= 0) {
            log.warn("calcDueDate: workDays={} <= 0, treated as 1", workDays);
            workDays = 1;
        }

        Set<LocalDate> holidays = config.resolveHolidays();
        Set<LocalDate> adjWorkdays = config.resolveAdjustmentWorkdays();

        LocalDate current = startDate;
        int counted = 0;
        int loop = 0;

        // 从 startDate 开始计数，先判断当前日期是否为工作日
        while (counted < workDays && loop < MAX_LOOP) {
            if (isWorkingDay(current, holidays, adjWorkdays)) {
                counted++;
            }
            if (counted < workDays) {
                current = current.plusDays(1);
            }
            loop++;
        }

        if (loop >= MAX_LOOP) {
            log.warn("calcDueDate: reached MAX_LOOP({})", MAX_LOOP);
        }

        if (log.isDebugEnabled()) {
            log.debug("calcDueDate: start={}, workDays={}, due={}", startDate, workDays, current);
        }

        return current;
    }

    // ==================== 毫秒级计算 ====================

    /**
     * 计算两个时间之间的有效毫秒数（排除节假日整天）
     * <p>
     * 从 start 时刻开始计算，排除非工作日整天占用的毫秒数。
     * 对于部分天（如开始日或结束日的部分时间），仅扣除实际交集部分。
     * </p>
     *
     * @param start  起始时间（含该时刻）
     * @param end    截止时间
     * @param config 节假日配置
     * @return 有效毫秒数
     */
    public static long calcEffectiveMillis(LocalDateTime start, LocalDateTime end, OverdueConfig config) {
        if (start == null || end == null || config == null) {
            return 0L;
        }

        if (start.isAfter(end)) {
            return -calcEffectiveMillis(end, start, config);
        }

        long totalMillis = ChronoUnit.MILLIS.between(start, end);
        long holidayMillis = calcHolidayMillisBetween(start, end, config);
        long effectiveMillis = totalMillis - holidayMillis;

        if (log.isDebugEnabled()) {
            log.debug("calcEffectiveMillis: start={}, end={}, total={}, holiday={}, effective={}",
                    start, end, totalMillis, holidayMillis, effectiveMillis);
        }

        return effectiveMillis;
    }

    /**
     * 判断是否超期（毫秒级）
     *
     * @param start          开始时间
     * @param end            截止时间
     * @param deadlineMillis 截止毫秒阈值
     * @param config         节假日配置
     * @return true=已超期
     */
    public static boolean isOverdueMillis(LocalDateTime start, LocalDateTime end, long deadlineMillis, OverdueConfig config) {
        long effective = calcEffectiveMillis(start, end, config);
        return effective > deadlineMillis;
    }

    /**
     * 计算超期毫秒数（毫秒级）
     *
     * @param start          开始时间
     * @param end            截止时间
     * @param deadlineMillis 截止毫秒阈值
     * @param config         节假日配置
     * @return 超期毫秒数（正数=已超期）
     */
    public static long calcOverdueMillis(LocalDateTime start, LocalDateTime end, long deadlineMillis, OverdueConfig config) {
        long effective = calcEffectiveMillis(start, end, config);
        return Math.max(0, effective - deadlineMillis);
    }

    // ==================== 内部工具方法 ====================

    /**
     * 判断指定日期是否为工作日
     * <p>
     * 判断逻辑（按优先级）：
     * <ol>
     *   <li>调休工作日（周末补班）→ 工作日</li>
     *   <li>法定假日 / 自定义假日 → 非工作日</li>
     *   <li>周一至周五 → 工作日</li>
     *   <li>周六周日 → 非工作日</li>
     * </ol>
     * </p>
     */
    private static boolean isWorkingDay(LocalDate date, Set<LocalDate> holidays, Set<LocalDate> adjWorkdays) {
        // 调休工作日（周末补班）
        if (adjWorkdays != null && adjWorkdays.contains(date)) {
            return true;
        }
        // 法定假日 / 自定义假日
        if (holidays != null && holidays.contains(date)) {
            return false;
        }
        // 自然周末（周六=6，周日=7）
        int dow = date.getDayOfWeek().getValue();
        return dow <= 5;
    }

    /**
     * 计算两个时间之间节假日占用的毫秒数
     */
    private static long calcHolidayMillisBetween(LocalDateTime start, LocalDateTime end, OverdueConfig config) {
        Set<LocalDate> holidays = config.resolveHolidays();
        Set<LocalDate> adjWorkdays = config.resolveAdjustmentWorkdays();

        if (config.isEmpty() || (holidays.isEmpty() && adjWorkdays.isEmpty())) {
            return 0L;
        }

        long holidayMillis = 0L;
        LocalDate currentDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        while (!currentDate.isAfter(endDate)) {
            if (!isWorkingDay(currentDate, holidays, adjWorkdays)) {
                LocalDateTime dayStart = currentDate.atStartOfDay();
                LocalDateTime dayEnd = currentDate.plusDays(1).atStartOfDay();

                LocalDateTime effectiveStart = start.isAfter(dayStart) ? start : dayStart;
                LocalDateTime effectiveEnd = end.isBefore(dayEnd) ? end : dayEnd;

                if (effectiveStart.isBefore(effectiveEnd)) {
                    holidayMillis += ChronoUnit.MILLIS.between(effectiveStart, effectiveEnd);
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        return holidayMillis;
    }

    // ==================== 解析方法 ====================

    /**
     * 解析天级日期字符串
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("parseDate failed: {}", dateStr, e);
            return null;
        }
    }

    /**
     * 解析毫秒级日期时间字符串
     */
    public static LocalDateTime parseMillis(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr.trim(), MILLIS_FORMATTER);
        } catch (Exception e) {
            log.warn("parseMillis failed: {}", dateTimeStr, e);
            return null;
        }
    }

    /**
     * 格式化 LocalDate 为字符串
     */
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    /**
     * 格式化 LocalDateTime 为毫秒级字符串
     */
    public static String formatMillis(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(MILLIS_FORMATTER) : "";
    }
}