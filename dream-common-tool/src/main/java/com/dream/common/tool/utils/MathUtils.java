package com.dream.common.tool.utils;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;

/**
 * 高精度数学运算工具类
 * <p>
 * 基于 {@link BigDecimal} 封装，解决 float / double 精度丢失问题。
 * 适用于金融计算、统计汇总、百分比运算等对精度要求严格的场景。
 * <p>
 * 核心能力：
 * <ul>
 *   <li>{@link #toBigDecimal(Object)} —— 安全转换为 BigDecimal（支持各种常见类型）</li>
 *   <li>{@link #add(Object, Object...)} / {@link #subtract} —— 加减运算</li>
 *   <li>{@link #multiply(Object, Object...)} / {@link #divide} —— 乘除运算（含精度控制）</li>
 *   <li>{@link #round(Object, int)} —— 四舍五入精度控制</li>
 *   <li>{@link #gt(Object, Object)} / {@link #eq} / {@link #lt} —— 安全比较</li>
 *   <li>{@link #sum(Object...)} / {@link #avg} —— 汇总统计</li>
 *   <li>{@link #percent(Object, Object, int)} —— 百分比计算</li>
 *   <li>{@link #format(Object, String)} —— 格式化输出</li>
 * </ul>
 *
 * @author dream
 */
@Slf4j
@SuppressWarnings("all")
public final class MathUtils {

    // ==================== 常量 ====================

    /** 默认除法运算精度（小数后 10 位） */
    private static final int DEF_DIV_SCALE = 10;

    /** 默认精度（小数后 2 位） */
    private static final int DEF_SCALE = 2;

    /** 默认舍入模式：四舍五入 */
    private static final RoundingMode DEF_ROUND_MODE = RoundingMode.HALF_UP;

    /** 比较结果常量 */
    private static final int GT = 1;
    private static final int EQ = 0;
    private static final int LT = -1;

    /** 常用数值常量 */
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal ONE_THOUSAND = new BigDecimal("1000");

    /** 格式化器缓存（ThreadLocal 隔离，NumberFormat 非线程安全） */
    private static final ThreadLocal<NumberFormat> MONEY_FORMAT = ThreadLocal.withInitial(() -> {
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        nf.setMaximumFractionDigits(DEF_SCALE);
        nf.setMinimumFractionDigits(DEF_SCALE);
        return nf;
    });
    private static final ThreadLocal<NumberFormat> PERCENT_FORMAT = ThreadLocal.withInitial(() -> {
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMaximumFractionDigits(DEF_SCALE);
        nf.setMinimumFractionDigits(DEF_SCALE);
        return nf;
    });

    private MathUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 安全转换 ====================

    /**
     * 安全转换为 BigDecimal
     * <pre>
     * toBigDecimal(null)        → BigDecimal.ZERO
     * toBigDecimal("123.45")    → 123.45
     * toBigDecimal(123.45d)     → 123.45
     * toBigDecimal(123)         → 123
     * </pre>
     *
     * @param value 待转换值
     * @return BigDecimal 实例；无法转换返回 BigDecimal.ZERO
     */
    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            if (log.isDebugEnabled()) {
                log.debug("toBigDecimal: value 为 null，返回 BigDecimal.ZERO");
            }
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof String) {
            String str = ((String) value).trim();
            if (StringUtils.isBlank(str)) {
                if (log.isDebugEnabled()) {
                    log.debug("toBigDecimal: 字符串为空，返回 BigDecimal.ZERO");
                }
                return BigDecimal.ZERO;
            }
            try {
                return new BigDecimal(str);
            } catch (NumberFormatException e) {
                log.warn("toBigDecimal: 字符串转 BigDecimal 失败: {}", str, e);
                return BigDecimal.ZERO;
            }
        }
        if (value instanceof Number) {
            // 整数类型精确转换
            if (value instanceof Integer || value instanceof Long
                    || value instanceof Short || value instanceof Byte) {
                return BigDecimal.valueOf(((Number) value).longValue());
            }
            // 浮点类型用 Double.toString 避免精度丢失
            return new BigDecimal(Double.toString(((Number) value).doubleValue()));
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        // 尝试 toString 后解析
        try {
            String str = value.toString().trim();
            if (StringUtils.isBlank(str)) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(str);
        } catch (NumberFormatException e) {
            log.warn("toBigDecimal: 对象转 BigDecimal 失败: {}", value, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 安全转换为 BigDecimal，支持自定义默认值
     *
     * @param value        待转换值
     * @param defaultValue 转换失败时的默认值
     * @return BigDecimal 实例
     */
    public static BigDecimal toBigDecimal(Object value, BigDecimal defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return toBigDecimal(value);
        } catch (Exception e) {
            log.warn("toBigDecimal 转换失败，使用默认值: {}", defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * 判断字符串是否为合法数值
     *
     * @param str 待判断字符串
     * @return true 表示合法数值
     */
    public static boolean isNumber(String str) {
        if (StringUtils.isBlank(str)) {
            return false;
        }
        try {
            new BigDecimal(str.trim());
            return true;
        } catch (NumberFormatException e) {
            if (log.isDebugEnabled()) {
                log.debug("isNumber: 非法数值: {}", str);
            }
            return false;
        }
    }

    // ==================== 加法 ====================

    /**
     * 加法运算（精确）
     * <pre>
     * add("1.23", "4.56") → 5.79
     * </pre>
     *
     * @param v1     第一个加数
     * @param values 其余加数
     * @return 累加和
     */
    public static BigDecimal add(Object v1, Object... values) {
        BigDecimal sum = toBigDecimal(v1);
        if (values != null) {
            for (Object v : values) {
                sum = sum.add(toBigDecimal(v));
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("add: {} + {} = {}", v1, values, sum);
        }
        return sum;
    }

    /**
     * BigDecimal 加法
     *
     * @param v1     第一个加数
     * @param values 其余加数
     * @return 累加和
     */
    public static BigDecimal add(BigDecimal v1, BigDecimal... values) {
        BigDecimal sum = v1 == null ? BigDecimal.ZERO : v1;
        if (values != null) {
            for (BigDecimal v : values) {
                if (v != null) {
                    sum = sum.add(v);
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("add: {} + {} = {}", v1, values, sum);
        }
        return sum;
    }

    // ==================== 减法 ====================

    /**
     * 减法运算（精确）
     * <pre>
     * subtract("10.00", "3.50") → 6.50
     * </pre>
     *
     * @param v1     被减数
     * @param values 减数（依次减去）
     * @return 差值
     */
    public static BigDecimal subtract(Object v1, Object... values) {
        BigDecimal result = toBigDecimal(v1);
        if (values != null) {
            for (Object v : values) {
                result = result.subtract(toBigDecimal(v));
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("subtract: {} - {} = {}", v1, values, result);
        }
        return result;
    }

    /**
     * BigDecimal 减法
     *
     * @param v1     被减数
     * @param values 减数（依次减去）
     * @return 差值
     */
    public static BigDecimal subtract(BigDecimal v1, BigDecimal... values) {
        BigDecimal result = v1 == null ? BigDecimal.ZERO : v1;
        if (values != null) {
            for (BigDecimal v : values) {
                if (v != null) {
                    result = result.subtract(v);
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("subtract: {} - {} = {}", v1, values, result);
        }
        return result;
    }

    // ==================== 乘法 ====================

    /**
     * 乘法运算（精确）
     * <pre>
     * multiply("2.5", "4") → 10.00
     * </pre>
     *
     * @param v1     第一个乘数
     * @param values 其余乘数
     * @return 乘积
     */
    public static BigDecimal multiply(Object v1, Object... values) {
        BigDecimal result = toBigDecimal(v1);
        if (values != null) {
            for (Object v : values) {
                result = result.multiply(toBigDecimal(v));
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("multiply: {} * {} = {}", v1, values, result);
        }
        return result;
    }

    /**
     * BigDecimal 乘法
     *
     * @param v1     第一个乘数
     * @param values 其余乘数
     * @return 乘积
     */
    public static BigDecimal multiply(BigDecimal v1, BigDecimal... values) {
        BigDecimal result = v1 == null ? BigDecimal.ZERO : v1;
        if (values != null) {
            for (BigDecimal v : values) {
                if (v != null) {
                    result = result.multiply(v);
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("multiply: {} * {} = {}", v1, values, result);
        }
        return result;
    }

    // ==================== 除法 ====================

    /**
     * 除法运算（默认精度 10 位，四舍五入）
     * <pre>
     * divide("10", "3") → 3.3333333333
     * </pre>
     *
     * @param v1 被除数
     * @param v2 除数
     * @return 商
     * @throws IllegalArgumentException 除数为零时抛出
     */
    public static BigDecimal divide(Object v1, Object v2) {
        return divide(v1, v2, DEF_DIV_SCALE, DEF_ROUND_MODE);
    }

    /**
     * 除法运算（指定小数位数，四舍五入）
     *
     * @param v1    被除数
     * @param v2    除数
     * @param scale 小数位数
     * @return 商
     */
    public static BigDecimal divide(Object v1, Object v2, int scale) {
        return divide(v1, v2, scale, DEF_ROUND_MODE);
    }

    /**
     * 除法运算（完全控制精度和舍入模式）
     *
     * @param v1           被除数
     * @param v2           除数
     * @param scale        小数位数
     * @param roundingMode 舍入模式
     * @return 商
     * @throws IllegalArgumentException 除数为零时抛出
     */
    public static BigDecimal divide(Object v1, Object v2, int scale, RoundingMode roundingMode) {
        BigDecimal divisor = toBigDecimal(v2);
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            log.error("除法运算失败：除数为零");
            throw new IllegalArgumentException("Divisor cannot be zero");
        }
        BigDecimal result = toBigDecimal(v1).divide(divisor, scale, roundingMode);
        if (log.isDebugEnabled()) {
            log.debug("divide: {} / {} = {} (scale={}, roundingMode={})", v1, v2, result, scale, roundingMode);
        }
        return result;
    }

    // ==================== 精度控制 ====================

    /**
     * 四舍五入保留指定位数小数
     *
     * @param value 待舍入值
     * @param scale 小数位数
     * @return 舍入后的值
     */
    public static BigDecimal round(Object value, int scale) {
        return round(value, scale, DEF_ROUND_MODE);
    }

    /**
     * 按指定舍入模式保留指定位数小数
     *
     * @param value        待舍入值
     * @param scale        小数位数
     * @param roundingMode 舍入模式
     * @return 舍入后的值
     */
    public static BigDecimal round(Object value, int scale, RoundingMode roundingMode) {
        BigDecimal result = toBigDecimal(value).setScale(scale, roundingMode);
        if (log.isDebugEnabled()) {
            log.debug("round: {} → {} (scale={}, roundingMode={})", value, result, scale, roundingMode);
        }
        return result;
    }

    /**
     * 默认两位小数四舍五入
     *
     * @param value 待舍入值
     * @return 保留两位小数的值
     */
    public static BigDecimal round(Object value) {
        return round(value, DEF_SCALE, DEF_ROUND_MODE);
    }

    /**
     * 截断小数位（直接舍弃指定位数后的部分）
     *
     * @param value 待截取值
     * @param scale 小数位数
     * @return 截断后的值
     */
    public static BigDecimal truncate(Object value, int scale) {
        BigDecimal result = toBigDecimal(value).setScale(scale, RoundingMode.DOWN);
        if (log.isDebugEnabled()) {
            log.debug("truncate: {} → {} (scale={})", value, result, scale);
        }
        return result;
    }

    /**
     * 向上取整（远离零方向）
     *
     * @param value 待取值
     * @param scale 小数位数
     * @return 向上取整后的值
     */
    public static BigDecimal ceil(Object value, int scale) {
        BigDecimal result = toBigDecimal(value).setScale(scale, RoundingMode.UP);
        if (log.isDebugEnabled()) {
            log.debug("ceil: {} → {} (scale={})", value, result, scale);
        }
        return result;
    }

    /**
     * 向下取整（靠近零方向）
     *
     * @param value 待取值
     * @param scale 小数位数
     * @return 向下取整后的值
     */
    public static BigDecimal floor(Object value, int scale) {
        BigDecimal result = toBigDecimal(value).setScale(scale, RoundingMode.DOWN);
        if (log.isDebugEnabled()) {
            log.debug("floor: {} → {} (scale={})", value, result, scale);
        }
        return result;
    }

    // ==================== 比较运算 ====================

    /**
     * 比较两个值的大小
     *
     * @param v1 第一个值
     * @param v2 第二个值
     * @return 1: v1 &gt; v2, 0: v1 == v2, -1: v1 &lt; v2
     */
    public static int compare(Object v1, Object v2) {
        int result = toBigDecimal(v1).compareTo(toBigDecimal(v2));
        if (log.isDebugEnabled()) {
            log.debug("compare: {} vs {} = {}", v1, v2, result);
        }
        return result;
    }

    public static boolean eq(Object v1, Object v2) {
        boolean result = compare(v1, v2) == EQ;
        if (log.isDebugEnabled()) {
            log.debug("eq: {} == {} = {}", v1, v2, result);
        }
        return result;
    }

    public static boolean neq(Object v1, Object v2) {
        boolean result = compare(v1, v2) != EQ;
        if (log.isDebugEnabled()) {
            log.debug("neq: {} != {} = {}", v1, v2, result);
        }
        return result;
    }

    public static boolean gt(Object v1, Object v2) {
        boolean result = compare(v1, v2) == GT;
        if (log.isDebugEnabled()) {
            log.debug("gt: {} > {} = {}", v1, v2, result);
        }
        return result;
    }

    public static boolean gte(Object v1, Object v2) {
        boolean result = compare(v1, v2) >= EQ;
        if (log.isDebugEnabled()) {
            log.debug("gte: {} >= {} = {}", v1, v2, result);
        }
        return result;
    }

    public static boolean lt(Object v1, Object v2) {
        boolean result = compare(v1, v2) == LT;
        if (log.isDebugEnabled()) {
            log.debug("lt: {} < {} = {}", v1, v2, result);
        }
        return result;
    }

    public static boolean lte(Object v1, Object v2) {
        boolean result = compare(v1, v2) <= EQ;
        if (log.isDebugEnabled()) {
            log.debug("lte: {} <= {} = {}", v1, v2, result);
        }
        return result;
    }

    public static boolean isZero(Object value) {
        boolean result = compare(value, 0) == EQ;
        if (log.isDebugEnabled()) {
            log.debug("isZero: {} = {}", value, result);
        }
        return result;
    }

    public static boolean isPositive(Object value) {
        boolean result = compare(value, 0) == GT;
        if (log.isDebugEnabled()) {
            log.debug("isPositive: {} = {}", value, result);
        }
        return result;
    }

    public static boolean isNegative(Object value) {
        boolean result = compare(value, 0) == LT;
        if (log.isDebugEnabled()) {
            log.debug("isNegative: {} = {}", value, result);
        }
        return result;
    }

    // ==================== 聚合统计 ====================

    /**
     * 求和
     * <pre>
     * sum("1.1", "2.2", "3.3") → 6.6
     * </pre>
     */
    public static BigDecimal sum(Object... values) {
        if (values == null || values.length == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Object v : values) {
            total = total.add(toBigDecimal(v));
        }
        if (log.isDebugEnabled()) {
            log.debug("sum: {} = {}", values, total);
        }
        return total;
    }

    public static BigDecimal sum(Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Object v : values) {
            total = total.add(toBigDecimal(v));
        }
        if (log.isDebugEnabled()) {
            log.debug("sum: {} 个元素求和 = {}", values.size(), total);
        }
        return total;
    }

    /**
     * 求平均值
     * <pre>
     * avg("1.0", "2.0", "3.0") → 2.0
     * </pre>
     */
    public static BigDecimal avg(Object... values) {
        if (values == null || values.length == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = sum(values);
        BigDecimal result = divide(sum, values.length, DEF_DIV_SCALE, DEF_ROUND_MODE);
        if (log.isDebugEnabled()) {
            log.debug("avg: {} 个元素平均值 = {}", values.length, result);
        }
        return result;
    }

    public static BigDecimal avg(Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = sum(values);
        BigDecimal result = divide(sum, values.size(), DEF_DIV_SCALE, DEF_ROUND_MODE);
        if (log.isDebugEnabled()) {
            log.debug("avg: {} 个元素平均值 = {}", values.size(), result);
        }
        return result;
    }

    /**
     * 最大值
     */
    public static BigDecimal max(Object... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        BigDecimal max = toBigDecimal(values[0]);
        for (int i = 1; i < values.length; i++) {
            BigDecimal v = toBigDecimal(values[i]);
            if (compare(v, max) == GT) {
                max = v;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("max: 最大值 = {}", max);
        }
        return max;
    }

    /**
     * 最小值
     */
    public static BigDecimal min(Object... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        BigDecimal min = toBigDecimal(values[0]);
        for (int i = 1; i < values.length; i++) {
            BigDecimal v = toBigDecimal(values[i]);
            if (compare(v, min) == LT) {
                min = v;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("min: 最小值 = {}", min);
        }
        return min;
    }

    public static BigDecimal abs(Object value) {
        BigDecimal result = toBigDecimal(value).abs();
        if (log.isDebugEnabled()) {
            log.debug("abs: {} → {}", value, result);
        }
        return result;
    }

    public static BigDecimal negate(Object value) {
        BigDecimal result = toBigDecimal(value).negate();
        if (log.isDebugEnabled()) {
            log.debug("negate: {} → {}", value, result);
        }
        return result;
    }

    // ==================== 百分比 ====================

    /**
     * 计算百分比（除数 / 被除数 × 100）
     * <pre>
     * percent(20, 100, 2) → 20.00
     * </pre>
     */
    public static BigDecimal percent(Object divisor, Object dividend, int scale) {
        BigDecimal dvd = toBigDecimal(dividend);
        if (isZero(dvd)) {
            log.warn("percent: 被除数为零，返回 BigDecimal.ZERO");
            return BigDecimal.ZERO;
        }
        BigDecimal result = divide(toBigDecimal(divisor), dvd, scale + DEF_SCALE, DEF_ROUND_MODE)
                .multiply(ONE_HUNDRED)
                .setScale(scale, DEF_ROUND_MODE);
        if (log.isDebugEnabled()) {
            log.debug("percent: {} / {} * 100 = {}% (scale={})", divisor, dividend, result, scale);
        }
        return result;
    }

    public static BigDecimal percent(Object divisor, Object dividend) {
        return percent(divisor, dividend, DEF_SCALE);
    }

    /**
     * 计算千分比
     */
    public static BigDecimal permillage(Object divisor, Object dividend, int scale) {
        BigDecimal dvd = toBigDecimal(dividend);
        if (isZero(dvd)) {
            log.warn("permillage: 被除数为零，返回 BigDecimal.ZERO");
            return BigDecimal.ZERO;
        }
        BigDecimal result = divide(toBigDecimal(divisor), dvd, scale + 3, DEF_ROUND_MODE)
                .multiply(ONE_THOUSAND)
                .setScale(scale, DEF_ROUND_MODE);
        if (log.isDebugEnabled()) {
            log.debug("permillage: {} / {} * 1000 = {} (scale={})", divisor, dividend, result, scale);
        }
        return result;
    }

    /**
     * 增长率计算：(新值 - 旧值) / 旧值 × 100%
     * <pre>
     * growthRate(120, 100, 2) → 20.00
     * growthRate(80, 100, 2)  → -20.00
     * </pre>
     */
    public static BigDecimal growthRate(Object newValue, Object oldValue, int scale) {
        BigDecimal old = toBigDecimal(oldValue);
        if (isZero(old)) {
            log.warn("growthRate: 旧值为零，返回 BigDecimal.ZERO");
            return BigDecimal.ZERO;
        }
        BigDecimal result = subtract(newValue, old)
                .divide(old, scale + DEF_SCALE, DEF_ROUND_MODE)
                .multiply(ONE_HUNDRED)
                .setScale(scale, DEF_ROUND_MODE);
        if (log.isDebugEnabled()) {
            log.debug("growthRate: ({}) / {} * 100% = {}%", subtract(newValue, old), old, result);
        }
        return result;
    }

    // ==================== 格式化 ====================

    /**
     * 格式化数值
     * <pre>
     * format(12345.6789, "#,##0.00") → "12,345.68"
     * format(0.056, "0.00%")         → "5.60%"
     * </pre>
     */
    public static String format(Object value, String format) {
        if (value == null) {
            return "";
        }
        DecimalFormat df = new DecimalFormat(format);
        df.setRoundingMode(DEF_ROUND_MODE);
        String result = df.format(toBigDecimal(value));
        if (log.isDebugEnabled()) {
            log.debug("format: {} 格式化为 {} (pattern={})", value, result, format);
        }
        return result;
    }

    /**
     * 格式化金额（带千分位，两位小数）
     * <pre>
     * formatMoney(1234567.89) → "1,234,567.89"
     * </pre>
     */
    public static String formatMoney(Object value) {
        return format(value, "#,##0.00");
    }

    /**
     * 格式化金额（含货币符号）
     * <pre>
     * formatCurrency(1234567.89) → "¥1,234,567.89"
     * </pre>
     */
    public static String formatCurrency(Object value) {
        if (value == null) {
            return "";
        }
        String result = MONEY_FORMAT.get().format(toBigDecimal(value));
        if (log.isDebugEnabled()) {
            log.debug("formatCurrency: {} → {}", value, result);
        }
        return result;
    }

    /**
     * 格式化百分比
     * <pre>
     * formatPercent(0.256) → "25.60%"
     * </pre>
     */
    public static String formatPercent(Object value) {
        if (value == null) {
            return "";
        }
        String result = PERCENT_FORMAT.get().format(toBigDecimal(value));
        if (log.isDebugEnabled()) {
            log.debug("formatPercent: {} → {}", value, result);
        }
        return result;
    }

    /**
     * 去掉小数尾随零，返回普通字符串（避免科学计数法）
     * <pre>
     * toPlainString("100.000") → "100"
     * toPlainString("0.00100") → "0.001"
     * </pre>
     */
    public static String toPlainString(Object value) {
        String result = toBigDecimal(value).stripTrailingZeros().toPlainString();
        if (log.isDebugEnabled()) {
            log.debug("toPlainString: {} → {}", value, result);
        }
        return result;
    }

    // ==================== 单位换算 ====================

    /**
     * 分转元
     * <pre>
     * centsToYuan(12345) → 123.45
     * </pre>
     */
    public static BigDecimal centsToYuan(Object cents) {
        BigDecimal result = divide(cents, ONE_HUNDRED, DEF_SCALE, DEF_ROUND_MODE);
        if (log.isDebugEnabled()) {
            log.debug("centsToYuan: {} 分 → {} 元", cents, result);
        }
        return result;
    }

    /**
     * 元转分
     * <pre>
     * yuanToCents(123.45) → 12345
     * </pre>
     */
    public static long yuanToCents(Object yuan) {
        long result = multiply(toBigDecimal(yuan), ONE_HUNDRED)
                .setScale(0, DEF_ROUND_MODE)
                .longValue();
        if (log.isDebugEnabled()) {
            log.debug("yuanToCents: {} 元 → {} 分", yuan, result);
        }
        return result;
    }

    // ==================== 范围限定 ====================

    /**
     * 将值限定在 [min, max] 范围内
     * <pre>
     * clamp(150, 0, 100) → 100
     * clamp(-10, 0, 100) → 0
     * clamp(50, 0, 100)  → 50
     * </pre>
     */
    public static BigDecimal clamp(Object value, Object min, Object max) {
        BigDecimal v = toBigDecimal(value);
        BigDecimal mn = toBigDecimal(min);
        BigDecimal mx = toBigDecimal(max);
        BigDecimal result;
        if (compare(v, mn) == LT) {
            result = mn;
        } else if (compare(v, mx) == GT) {
            result = mx;
        } else {
            result = v;
        }
        if (log.isDebugEnabled()) {
            log.debug("clamp: {} 限定在 [{} , {}] → {}", value, min, max, result);
        }
        return result;
    }

    // ==================== 其他 ====================

    /**
     * 获取小数位数
     */
    public static int scaleOf(Object value) {
        return toBigDecimal(value).scale();
    }

    /**
     * 获取精度（总位数）
     */
    public static int precisionOf(Object value) {
        return toBigDecimal(value).precision();
    }

    public static int toInt(Object value) {
        return toBigDecimal(value).intValue();
    }

    public static long toLong(Object value) {
        return toBigDecimal(value).longValue();
    }

    public static double toDouble(Object value) {
        return toBigDecimal(value).doubleValue();
    }

    /**
     * 返回字符串（普通字符串，非科学计数法）
     */
    public static String toString(Object value) {
        if (value == null) {
            return "0";
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).stripTrailingZeros().toPlainString();
        }
        return toBigDecimal(value).stripTrailingZeros().toPlainString();
    }
}