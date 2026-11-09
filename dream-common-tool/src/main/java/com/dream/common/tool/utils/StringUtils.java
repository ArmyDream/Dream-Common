package com.dream.common.tool.utils;

import com.dream.common.core.constant.Business;
import com.dream.common.core.enums.StringPool;
import com.dream.common.tool.utils.id.UuidGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 字符串工具类
 * <p>
 * 继承自 {@link org.apache.commons.lang3.StringUtils}，扩展以下能力：
 * <ul>
 *   <li>{@link #isEmpty(Object)} —— Object 深度判空（null/空串/空集合/空Map/空数组/空Optional）</li>
 *   <li>{@link #existEmpty(Collection)} —— 判断集合/数组中是否存在空元素</li>
 *   <li>{@link #ofNullable(Object, Object)} —— 为空时返回默认值</li>
 *   <li>{@link #joinComma(String...)} —— 基于 {@link StringPool} 的快捷拼接与分割</li>
 *   <li>{@link #truncate(String, int)} —— 截断、命名转换、填充等处理</li>
 *   <li>{@link #toStr(Object)} / {@link #toInt(Object, Integer)} —— 类型安全转换</li>
 *   <li>{@link #buildCacheKey(String...)} / {@link #buildRedisKey(String, String, Object...)} —— 缓存 Key 构建</li>
 * </ul>
 * <p>
 * 注意：父类 {@link org.apache.commons.lang3.StringUtils} 已提供的常用方法（如 isBlank、equals、contains 等）
 * 本类不再重复定义，请直接调用父类方法。
 *
 * @author dream
 */
@Slf4j
@SuppressWarnings("all")
public final class StringUtils extends org.apache.commons.lang3.StringUtils {

    // ==================== 常量 ====================

    /** 验证码等安全随机串的来源（{@link java.util.Random} 子类，线程安全） */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 数字验证码长度 */
    private static final int VERIFY_CODE_LENGTH = 6;

    // ==================== 金额大写转换常量 ====================

    /** 数字金额大写转换常量 */
    private static final String[] FRACTION = {"角", "分"};
    private static final String[] DIGIT = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
    private static final String[][] UNIT = {{"元", "万", "亿"}, {"", "拾", "佰", "仟"}};
    /**
     * 金额大写转换的上下限（亿元）。
     * <p>上限由 {@link #UNIT} 的单位表决定（只到「亿」），不是可随意调大的配置项；
     * 需要更大金额时应先扩充单位表，故不提供放开上限的重载。</p>
     */
    public static final double MAX_MONEY = 999999999.99;
    public static final double MIN_MONEY = -999999999.99;

    private StringUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== Object 深度判空 ====================

    /**
     * 判断对象是否为空（深度判空）
     * <p>
     * 依次判断以下情况，满足其一即返回 true：
     * <ul>
     *   <li>{@code null}</li>
     *   <li>{@code CharSequence}：空字符串或全空白字符</li>
     *   <li>{@code Collection}：{@code isEmpty()} 为 true</li>
     *   <li>{@code Map}：{@code isEmpty()} 为 true</li>
     *   <li>{@code Array}：{@code length == 0}</li>
     *   <li>{@code Optional}：{@code !isPresent()}</li>
     * </ul>
     *
     * <pre>
     * StringUtils.isObjectBlank(null)              → true
     * StringUtils.isObjectBlank("")                → true
     * StringUtils.isObjectBlank("  ")              → true
     * StringUtils.isObjectBlank(new ArrayList())   → true
     * StringUtils.isObjectBlank(new HashMap())     → true
     * StringUtils.isObjectBlank(new int[]{})       → true
     * StringUtils.isObjectBlank(Optional.empty())  → true
     * StringUtils.isObjectBlank("abc")             → false
     * StringUtils.isObjectBlank(Arrays.asList(1))  → false
     * </pre>
     *
     * @param obj 待判断对象
     * @return true 表示为空
     */
    public static boolean isObjectBlank(Object obj) {
        if (obj == null) {
            if (log.isDebugEnabled()) {
                log.debug("isObjectBlank: obj 为 null，返回 true");
            }
            return true;
        }
        if (obj instanceof CharSequence) {
            String str = obj.toString();
            boolean empty = str.isEmpty() || str.trim().isEmpty();
            if (log.isDebugEnabled()) {
                log.debug("isObjectBlank: CharSequence=[{}], result={}", str, empty);
            }
            return empty;
        }
        if (obj instanceof Collection) {
            boolean empty = ((Collection<?>) obj).isEmpty();
            if (log.isDebugEnabled()) {
                log.debug("isObjectBlank: Collection size={}, result={}", ((Collection<?>) obj).size(), empty);
            }
            return empty;
        }
        if (obj instanceof Map) {
            boolean empty = ((Map<?, ?>) obj).isEmpty();
            if (log.isDebugEnabled()) {
                log.debug("isObjectBlank: Map size={}, result={}", ((Map<?, ?>) obj).size(), empty);
            }
            return empty;
        }
        if (obj.getClass().isArray()) {
            int len = Array.getLength(obj);
            boolean empty = len == 0;
            if (log.isDebugEnabled()) {
                log.debug("isObjectBlank: Array length={}, result={}", len, empty);
            }
            return empty;
        }
        if (obj instanceof Optional) {
            boolean empty = !((Optional<?>) obj).isPresent();
            if (log.isDebugEnabled()) {
                log.debug("isObjectBlank: Optional isPresent={}, result={}", ((Optional<?>) obj).isPresent(), empty);
            }
            return empty;
        }
        if (log.isDebugEnabled()) {
            log.debug("isObjectBlank: obj={}, result=false", obj.getClass().getSimpleName());
        }
        return false;
    }

    /**
     * 判断对象是否不为空
     *
     * @param obj 待判断对象
     * @return true 表示不为空
     * @see #isEmpty(Object)
     */
    public static boolean isObjectNotBlank(Object obj) {
        boolean result = !isObjectBlank(obj);
        if (log.isDebugEnabled()) {
            log.debug("isObjectNotBlank: result={}", result);
        }
        return result;
    }

    /**
     * 判断所有对象是否都为空
     *
     * @param objs 待判断对象（可变参数）
     * @return true 表示全部为空；objs 本身为 null 或空数组时也返回 true
     */
    public static boolean isAllEmpty(Object... objs) {
        if (ArrayUtils.isEmpty(objs)) {
            if (log.isDebugEnabled()) {
                log.debug("isAllEmpty: objs 为空数组，返回 true");
            }
            return true;
        }
        for (Object obj : objs) {
            if (isObjectNotBlank(obj)) {
                if (log.isDebugEnabled()) {
                    log.debug("isAllEmpty: 发现非空对象，返回 false");
                }
                return false;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("isAllEmpty: 所有对象均为空，返回 true");
        }
        return true;
    }

    /**
     * 判断是否至少有一个对象不为空
     *
     * @param objs 待判断对象（可变参数）
     * @return true 表示至少有一个不为空；objs 本身为 null 或空数组时返回 false
     */
    public static boolean isAnyNotEmpty(Object... objs) {
        if (ArrayUtils.isEmpty(objs)) {
            if (log.isDebugEnabled()) {
                log.debug("isAnyNotEmpty: objs 为空数组，返回 false");
            }
            return false;
        }
        for (Object obj : objs) {
            if (isObjectNotBlank(obj)) {
                if (log.isDebugEnabled()) {
                    log.debug("isAnyNotEmpty: 发现非空对象，返回 true");
                }
                return true;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("isAnyNotEmpty: 所有对象均为空，返回 false");
        }
        return false;
    }

    /**
     * 判断所有对象是否都不为空（全部有值）
     *
     * @param objs 待判断对象（可变参数）
     * @return true 表示所有对象都不为空；objs 本身为 null 或空数组时返回 false
     */
    public static boolean isAllNotEmpty(Object... objs) {
        if (ArrayUtils.isEmpty(objs)) {
            if (log.isDebugEnabled()) {
                log.debug("isAllNotEmpty: objs 为空数组，返回 false");
            }
            return false;
        }
        for (Object obj : objs) {
            if (isObjectBlank(obj)) {
                if (log.isDebugEnabled()) {
                    log.debug("isAllNotEmpty: 发现空对象，返回 false");
                }
                return false;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("isAllNotEmpty: 所有对象均非空，返回 true");
        }
        return true;
    }

    /**
     * 判断是否至少有一个对象为空
     *
     * @param objs 待判断对象（可变参数）
     * @return true 表示至少有一个为空；objs 本身为 null 或空数组时返回 true
     */
    public static boolean isAnyEmpty(Object... objs) {
        if (ArrayUtils.isEmpty(objs)) {
            if (log.isDebugEnabled()) {
                log.debug("isAnyEmpty: objs 为空数组，返回 true");
            }
            return true;
        }
        for (Object obj : objs) {
            if (isObjectBlank(obj)) {
                if (log.isDebugEnabled()) {
                    log.debug("isAnyEmpty: 发现空对象，返回 true");
                }
                return true;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("isAnyEmpty: 所有对象均非空，返回 false");
        }
        return false;
    }

    /**
     * 对象为空时返回指定默认值
     *
     * <pre>
     * StringUtils.ofNullable(null, "def")   → "def"
     * StringUtils.ofNullable("", "def")     → "def"
     * StringUtils.ofNullable("abc", "def")  → "abc"
     * </pre>
     *
     * @param value        待判断对象
     * @param defaultValue 为空时返回的默认值
     * @param <T>          对象类型
     * @return 不为空返回 value，为空返回 defaultValue
     */
    public static <T> T ofNullable(T value, T defaultValue) {
        if (isObjectBlank(value)) {
            if (log.isDebugEnabled()) {
                log.debug("ofNullable: value 为空，返回默认值 {}", defaultValue);
            }
            return defaultValue;
        }
        if (log.isDebugEnabled()) {
            log.debug("ofNullable: value 非空，返回原值 {}", value);
        }
        return value;
    }

    /**
     * 对象为空时返回 null
     *
     * @param value 待判断对象
     * @param <T>   对象类型
     * @return 不为空返回 value，为空返回 null
     * @see #ofNullable(Object, Object)
     */
    public static <T> T ofNullable(T value) {
        return ofNullable(value, null);
    }

    /**
     * 对象为空时返回 字符串 defaultValue
     */
    public static String ofNullableString(Object obj, String defaultValue) {
        if(isObjectBlank( obj)){
            return defaultValue;
        }
        if (obj instanceof CharSequence) {
            String str = obj.toString();
            return str;
        }
        return JsonUtils.toJson(obj);
    }


    // ==================== 基于 StringPool 的快捷拼接 ====================

    /**
     * 以逗号拼接（跳过空值）
     * <p>示例：{@code joinComma("a", "b", null, "c") → "a,b,c"}
     *
     * @param parts 待拼接字符串数组
     * @return 拼接后的字符串
     */
    public static String joinComma(String... parts) {
        return joinWithSeparator(parts, StringPool.COMMA);
    }

    /**
     * 以逗号空格拼接（跳过空值）
     * <p>示例：{@code joinCommaSpace("a", "b") → "a, b"}
     *
     * @param parts 待拼接字符串数组
     * @return 拼接后的字符串
     */
    public static String joinCommaSpace(String... parts) {
        return joinWithSeparator(parts, StringPool.COMMA_SPACE);
    }

    /**
     * 以点号拼接（跳过空值）
     * <p>示例：{@code joinDot("com", "example", "api") → "com.example.api"}
     *
     * @param parts 待拼接字符串数组
     * @return 拼接后的字符串
     */
    public static String joinDot(String... parts) {
        return joinWithSeparator(parts, StringPool.DOT);
    }

    /**
     * 以冒号拼接（跳过空值），常用于构建缓存 Key
     * <p>示例：{@code joinColon("user", "info", "1") → "user:info:1"}
     *
     * @param parts 待拼接字符串数组
     * @return 拼接后的字符串
     */
    public static String joinColon(String... parts) {
        return joinWithSeparator(parts, StringPool.COLON);
    }

    /**
     * 以斜杠拼接（跳过空值），常用于构建路径
     * <p>示例：{@code joinSlash("home", "user", "file.txt") → "home/user/file.txt"}
     *
     * @param parts 待拼接字符串数组
     * @return 拼接后的字符串
     */
    public static String joinSlash(String... parts) {
        return joinWithSeparator(parts, StringPool.SLASH);
    }

    /**
     * 以竖线拼接（跳过空值）
     * <p>示例：{@code joinPipe("a", "b", "c") → "a|b|c"}
     *
     * @param parts 待拼接字符串数组
     * @return 拼接后的字符串
     */
    public static String joinPipe(String... parts) {
        return joinWithSeparator(parts, StringPool.PIPE);
    }

    /**
     * 以下划线拼接（跳过空值），常用于数据库字段名拼接
     * <p>示例：{@code joinUnderscore("user", "name") → "user_name"}
     *
     * @param parts 待拼接字符串数组
     * @return 拼接后的字符串
     */
    public static String joinUnderscore(String... parts) {
        return joinWithSeparator(parts, StringPool.UNDERSCORE);
    }

    /**
     * 以中划线拼接（跳过空值），常用于 UUID、订单号等
     * <p>示例：{@code joinDash("2026", "07", "11") → "2026-07-11"}
     *
     * @param parts 待拼接字符串数组
     * @return 拼接后的字符串
     */
    public static String joinDash(String... parts) {
        return joinWithSeparator(parts, StringPool.DASH);
    }

    /**
     * 直接拼接（无分隔符，跳过空值）
     * <p>示例：{@code joinEmpty("Hello", " ", "World") → "Hello World"}
     *
     * @param parts 待拼接字符串数组
     * @return 拼接后的字符串
     */
    public static String joinEmpty(String... parts) {
        return joinWithSeparator(parts, StringPool.EMPTY);
    }

    /**
     * 通用拼接方法（跳过 null 和空白字符串）
     *
     * @param parts     待拼接数组
     * @param separator 分隔符枚举
     * @return 拼接后的字符串；parts 为空时返回空字符串
     */
    private static String joinWithSeparator(String[] parts, StringPool separator) {
        if (ArrayUtils.isEmpty(parts)) {
            if (log.isDebugEnabled()) {
                log.debug("joinWithSeparator: parts 为空，返回空字符串");
            }
            return EMPTY;
        }
        String result = Stream.of(parts)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(separator.getValue()));
        if (log.isDebugEnabled()) {
            log.debug("joinWithSeparator: separator={}, result={}", separator.getValue(), result);
        }
        return result;
    }

    // ==================== 基于 StringPool 的快捷分割 ====================

    /**
     * 按逗号分割
     * <p>示例：{@code splitComma("a,b,c") → ["a","b","c"]}
     *
     * @param str 待分割字符串
     * @return 分割结果数组
     */
    public static String[] splitComma(String str) {
        return splitWithSeparator(str, StringPool.COMMA);
    }

    /**
     * 按竖线分割
     * <p>示例：{@code splitPipe("a|b|c") → ["a","b","c"]}
     *
     * @param str 待分割字符串
     * @return 分割结果数组
     */
    public static String[] splitPipe(String str) {
        return splitWithSeparator(str, StringPool.PIPE);
    }

    /**
     * 按点号分割
     * <p>示例：{@code splitDot("com.example.api") → ["com","example","api"]}
     *
     * @param str 待分割字符串
     * @return 分割结果数组
     */
    public static String[] splitDot(String str) {
        return splitWithSeparator(str, StringPool.DOT);
    }

    /**
     * 按冒号分割
     * <p>示例：{@code splitColon("user:info:1") → ["user","info","1"]}
     *
     * @param str 待分割字符串
     * @return 分割结果数组
     */
    public static String[] splitColon(String str) {
        return splitWithSeparator(str, StringPool.COLON);
    }

    /**
     * 按斜杠分割
     * <p>示例：{@code splitSlash("home/user/file") → ["home","user","file"]}
     *
     * @param str 待分割字符串
     * @return 分割结果数组
     */
    public static String[] splitSlash(String str) {
        return splitWithSeparator(str, StringPool.SLASH);
    }

    /**
     * 按下划线分割
     * <p>示例：{@code splitUnderscore("user_name") → ["user","name"]}
     *
     * @param str 待分割字符串
     * @return 分割结果数组
     */
    public static String[] splitUnderscore(String str) {
        return splitWithSeparator(str, StringPool.UNDERSCORE);
    }

    /**
     * 按中划线分割
     * <p>示例：{@code splitDash("2026-07-11") → ["2026","07","11"]}
     *
     * @param str 待分割字符串
     * @return 分割结果数组
     */
    public static String[] splitDash(String str) {
        return splitWithSeparator(str, StringPool.DASH);
    }

    /**
     * 通用分割方法
     *
     * @param str       待分割字符串
     * @param separator 分隔符枚举
     * @return 分割结果数组；str 为空白时返回空数组
     */
    private static String[] splitWithSeparator(String str, StringPool separator) {
        if (isBlank(str)) {
            if (log.isDebugEnabled()) {
                log.debug("splitWithSeparator: str 为空白，返回空数组");
            }
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        String[] result = split(str, separator.getValue());
        if (log.isDebugEnabled()) {
            log.debug("splitWithSeparator: separator={}, result={}", separator.getValue(), Arrays.toString(result));
        }
        return result;
    }

    // ==================== 字符串处理 ====================

    /**
     * 截断字符串（超长时追加省略号 …）
     * <p>
     * 当 maxLength ≥ 3 时，截断后保留 {@code maxLength - 3} 个字符再追加 "…"；
     * 当 maxLength < 3 时，直接截断不加省略号。
     * <pre>
     * truncate("Hello World", 8)   → "Hello…"
     * truncate("Hello", 10)        → "Hello"
     * truncate("Hello World", 2)   → "He"
     * </pre>
     *
     * @param str       待截断字符串
     * @param maxLength 最大长度（正整数）
     * @return 截断结果；str 为空白或 maxLength ≤ 0 时返回原值
     */
    public static String truncate(String str, int maxLength) {
        if (isBlank(str) || maxLength <= 0 || str.length() <= maxLength) {
            if (log.isDebugEnabled()) {
                log.debug("truncate: 无需截断, str={}, maxLength={}", str, maxLength);
            }
            return str;
        }
        String result;
        if (maxLength < 3) {
            result = str.substring(0, maxLength);
        } else {
            result = str.substring(0, maxLength - 3) + StringPool.HTML_HELLIP.getValue();
        }
        if (log.isDebugEnabled()) {
            log.debug("truncate: {} → {}", str, result);
        }
        return result;
    }

    /**
     * 截断字符串（纯截断，不追加省略号）
     *
     * @param str       待截断字符串
     * @param maxLength 最大长度
     * @return 截断结果；str 为空白或 maxLength ≤ 0 时返回原值
     */
    public static String truncatePlain(String str, int maxLength) {
        if (isBlank(str) || maxLength <= 0 || str.length() <= maxLength) {
            if (log.isDebugEnabled()) {
                log.debug("truncatePlain: 无需截断, str={}, maxLength={}", str, maxLength);
            }
            return str;
        }
        String result = str.substring(0, maxLength);
        if (log.isDebugEnabled()) {
            log.debug("truncatePlain: {} → {}", str, result);
        }
        return result;
    }

    /**
     * 去除字符串左边的所有空白字符
     * <p>示例：{@code trimLeft("  hello  ") → "hello  "}
     *
     * @param str 字符串
     * @return 去除左侧空白后的字符串
     */
    public static String trimLeft(String str) {
        if (isEmpty(str)) {
            return str;
        }
        int beginIdx = 0;
        while (beginIdx < str.length() && Character.isWhitespace(str.charAt(beginIdx))) {
            beginIdx++;
        }
        String result = str.substring(beginIdx);
        if (log.isDebugEnabled()) {
            log.debug("trimLeft: {} → {}", str, result);
        }
        return result;
    }

    /**
     * 去除字符串右边的所有空白字符
     * <p>示例：{@code trimRight("  hello  ") → "  hello"}
     *
     * @param str 字符串
     * @return 去除右侧空白后的字符串
     */
    public static String trimRight(String str) {
        if (isEmpty(str)) {
            return str;
        }
        int endIdx = str.length() - 1;
        while (endIdx >= 0 && Character.isWhitespace(str.charAt(endIdx))) {
            endIdx--;
        }
        String result = str.substring(0, endIdx + 1);
        if (log.isDebugEnabled()) {
            log.debug("trimRight: {} → {}", str, result);
        }
        return result;
    }

    /**
     * 去除字符串中所有空白字符（包括空格、制表符、换行等）
     * <p>示例：{@code trimAll(" h e l l o ") → "hello"}
     *
     * @param str 字符串
     * @return 去除所有空白后的字符串
     */
    public static String trimAll(String str) {
        return deleteWhitespace(str);
    }

    /**
     * 驼峰转下划线（处理连续大写，如 HTTPUtil → http_util）
     * <pre>
     * camelToUnderline("userName")    → "user_name"
     * camelToUnderline("HTTPUtil")    → "http_util"
     * camelToUnderline("getHTTPCode") → "get_http_code"
     * </pre>
     *
     * @param str 驼峰字符串
     * @return 下划线格式字符串
     */
    public static String camelToUnderline(String str) {
        if (isBlank(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length() + 4);
        char[] chars = str.toCharArray();
        boolean lastIsUpper = false;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (Character.isUpperCase(c)) {
                boolean prevIsLowerOrDigit = i > 0
                        && (Character.isLowerCase(chars[i - 1]) || Character.isDigit(chars[i - 1]));
                boolean nextIsLower = i < chars.length - 1 && Character.isLowerCase(chars[i + 1]);
                // 小写/数字 转大写，或连续大写块后接小写时插入下划线，如 HTTPUtil → http_util
                if (prevIsLowerOrDigit || (lastIsUpper && nextIsLower)) {
                    sb.append(StringPool.UNDERSCORE.getValue());
                }
                sb.append(Character.toLowerCase(c));
                lastIsUpper = true;
            } else {
                sb.append(c);
                lastIsUpper = false;
            }
        }
        String result = sb.toString();
        if (log.isDebugEnabled()) {
            log.debug("camelToUnderline: {} → {}", str, result);
        }
        return result;
    }

    /**
     * 下划线转驼峰（首字母小写）
     * <pre>
     * underlineToCamel("user_name")  → "userName"
     * underlineToCamel("_user_name") → "userName"（首下划线忽略）
     * </pre>
     *
     * @param str 下划线字符串
     * @return 驼峰格式字符串
     */
    public static String underlineToCamel(String str) {
        if (isBlank(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length());
        char[] chars = str.toCharArray();
        boolean upperNext = false;
        for (char c : chars) {
            if (c == '_') {
                upperNext = true;
            } else {
                sb.append(upperNext ? Character.toUpperCase(c) : c);
                upperNext = false;
            }
        }
        String result = sb.toString();
        if (log.isDebugEnabled()) {
            log.debug("underlineToCamel: {} → {}", str, result);
        }
        return result;
    }

    /**
     * 下划线转帕斯卡（首字母大写）
     * <p>示例：{@code user_name → UserName}
     *
     * @param str 下划线字符串
     * @return 帕斯卡格式字符串
     */
    public static String underlineToPascal(String str) {
        String camel = underlineToCamel(str);
        if (isBlank(camel)) {
            return camel;
        }
        char[] chars = camel.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        String result = new String(chars);
        if (log.isDebugEnabled()) {
            log.debug("underlineToPascal: {} → {}", str, result);
        }
        return result;
    }

    /**
     * 左填充（指定字符补齐到目标长度）
     * <pre>
     * padLeft("42", 5, '0')   → "00042"
     * padLeft("hello", 3, '0') → "hello"（长度已足，不填充）
     * </pre>
     *
     * @param str       待填充字符串
     * @param totalLen  目标总长度
     * @param padChar   填充字符
     * @return 填充后的字符串
     */
    public static String padLeft(String str, int totalLen, char padChar) {
        if (str == null) {
            str = EMPTY;
        }
        if (str.length() >= totalLen) {
            return str;
        }
        StringBuilder sb = new StringBuilder(totalLen);
        for (int i = str.length(); i < totalLen; i++) {
            sb.append(padChar);
        }
        sb.append(str);
        String result = sb.toString();
        if (log.isDebugEnabled()) {
            log.debug("padLeft: {} → {}", str, result);
        }
        return result;
    }

    /**
     * 右填充（指定字符补齐到目标长度）
     * <pre>
     * padRight("42", 5, '0')   → "42000"
     * padRight("hello", 3, '0') → "hello"
     * </pre>
     *
     * @param str       待填充字符串
     * @param totalLen  目标总长度
     * @param padChar   填充字符
     * @return 填充后的字符串
     */
    public static String padRight(String str, int totalLen, char padChar) {
        if (str == null) {
            str = EMPTY;
        }
        if (str.length() >= totalLen) {
            return str;
        }
        StringBuilder sb = new StringBuilder(totalLen);
        sb.append(str);
        for (int i = str.length(); i < totalLen; i++) {
            sb.append(padChar);
        }
        String result = sb.toString();
        if (log.isDebugEnabled()) {
            log.debug("padRight: {} → {}", str, result);
        }
        return result;
    }

    /**
     * 反转字符串
     * <p>示例：{@code reverse("hello") → "olleh"}
     *
     * @param str 待反转字符串
     * @return 反转结果；输入为 null 返回 null
     */
    public static String reverse(String str) {
        if (str == null) {
            return null;
        }
        String result = new StringBuilder(str).reverse().toString();
        if (log.isDebugEnabled()) {
            log.debug("reverse: {} → {}", str, result);
        }
        return result;
    }

    /**
     * 去除 HTML 标签
     * <pre>
     * stripHtml("<p>Hello <b>World</b></p>") → "Hello World"
     * stripHtml("<a href='#'>链接</a>")       → "链接"
     * </pre>
     *
     * @param html 含 HTML 标签的字符串
     * @return 去除标签后的纯文本；输入为空白返回原值
     */
    public static String stripHtml(String html) {
        if (isBlank(html)) {
            return html;
        }
        String result = html.replaceAll("<[^>]*>", EMPTY).trim();
        if (log.isDebugEnabled()) {
            log.debug("stripHtml: {} → {}", html, result);
        }
        return result;
    }

    /**
     * 是否包含中文
     * <p>示例：{@code containsChinese("Hello你好") → true}
     *
     * @param str 待判断字符串
     * @return true 表示包含至少一个中文字符
     */
    public static boolean containsChinese(String str) {
        if (isBlank(str)) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                if (log.isDebugEnabled()) {
                    log.debug("containsChinese: {} → true", str);
                }
                return true;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("containsChinese: {} → false", str);
        }
        return false;
    }

    /**
     * 字符串长度判断（安全处理 null）
     *
     * @param str 字符串
     * @return 字符串长度，null 返回 0
     */
    public static int length(String str) {
        return str == null ? 0 : str.length();
    }

    /**
     * 判断字符串长度是否在指定范围内（闭区间）
     *
     * @param str    字符串
     * @param minLen 最小长度（含）
     * @param maxLen 最大长度（含）
     * @return true 表示长度在 [minLen, maxLen] 范围内
     */
    public static boolean lengthBetween(String str, int minLen, int maxLen) {
        int len = length(str);
        boolean result = len >= minLen && len <= maxLen;
        if (log.isDebugEnabled()) {
            log.debug("lengthBetween: str={}, len={}, result={}", str, len, result);
        }
        return result;
    }

    // ==================== 类型安全转换 ====================

    /**
     * String 转 char[]
     *
     * @param str 字符串
     * @return char 数组；输入为空返回 null
     */
    public static char[] toCharArray(String str) {
        if (isEmpty(str)) {
            return null;
        }
        return str.toCharArray();
    }

    /**
     * String 转 Character（取第一个字符）
     *
     * @param str 字符串
     * @return 第一个字符；输入为空返回 null
     */
    public static Character toChar(String str) {
        char[] c = toCharArray(str);
        if (c == null) {
            return null;
        }
        return c[0];
    }

    /**
     * Object 安全转 String
     * <p>
     * null 返回空字符串，其他类型调用 {@code toString()}
     *
     * @param obj 对象
     * @return 字符串；null 返回 ""
     */
    public static String toStr(Object obj) {
        return toStr(obj, EMPTY);
    }

    /**
     * Object 安全转 String（支持默认值）
     * <pre>
     * toStr(null, "N/A")   → "N/A"
     * toStr(123, "N/A")    → "123"
     * toStr("", "N/A")     → "N/A"
     * </pre>
     *
     * @param obj        对象
     * @param defaultVal 默认值
     * @return 字符串；null 或空串返回 defaultVal
     */
    public static String toStr(Object obj, String defaultVal) {
        if (obj == null) {
            return defaultVal;
        }
        if (obj instanceof CharSequence) {
            String s = obj.toString().trim();
            return s.isEmpty() ? defaultVal : s;
        }
        return obj.toString();
    }

    /**
     * Object 安全转 Integer
     * <p>
     * 支持 Number 直接取值、字符串解析；解析失败返回默认值
     *
     * @param obj        对象
     * @param defaultVal 默认值
     * @return Integer；无法转换返回 defaultVal
     */
    public static Integer toInt(Object obj, Integer defaultVal) {
        if (obj == null) {
            return defaultVal;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        try {
            String s = obj.toString().trim();
            if (s.isEmpty()) {
                return defaultVal;
            }
            if (s.contains(StringPool.DOT.getValue())) {
                s = s.substring(0, s.indexOf(StringPool.DOT.getValue()));
            }
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            if (log.isDebugEnabled()) {
                log.debug("toInt: 转换失败, obj={}, default={}", obj, defaultVal);
            }
            return defaultVal;
        }
    }

    /**
     * Object 安全转 Integer（null 返回 null）
     *
     * @param obj 对象
     * @return Integer；无法转换返回 null
     * @see #toInt(Object, Integer)
     */
    public static Integer toInt(Object obj) {
        return toInt(obj, null);
    }

    /**
     * Object 安全转 Long
     *
     * @param obj        对象
     * @param defaultVal 默认值
     * @return Long；无法转换返回 defaultVal
     */
    public static Long toLong(Object obj, Long defaultVal) {
        if (obj == null) {
            return defaultVal;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            String s = obj.toString().trim();
            if (s.isEmpty()) {
                return defaultVal;
            }
            if (s.contains(StringPool.DOT.getValue())) {
                s = s.substring(0, s.indexOf(StringPool.DOT.getValue()));
            }
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            if (log.isDebugEnabled()) {
                log.debug("toLong: 转换失败, obj={}, default={}", obj, defaultVal);
            }
            return defaultVal;
        }
    }

    /**
     * Object 安全转 Long（null 返回 null）
     *
     * @param obj 对象
     * @return Long；无法转换返回 null
     * @see #toLong(Object, Long)
     */
    public static Long toLong(Object obj) {
        return toLong(obj, null);
    }

    /**
     * Object 安全转 Double
     *
     * @param obj        对象
     * @param defaultVal 默认值
     * @return Double；无法转换返回 defaultVal
     */
    public static Double toDouble(Object obj, Double defaultVal) {
        if (obj == null) {
            return defaultVal;
        }
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        try {
            String s = obj.toString().trim();
            if (s.isEmpty()) {
                return defaultVal;
            }
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            if (log.isDebugEnabled()) {
                log.debug("toDouble: 转换失败, obj={}, default={}", obj, defaultVal);
            }
            return defaultVal;
        }
    }

    /**
     * Object 安全转 Double（null 返回 null）
     *
     * @param obj 对象
     * @return Double；无法转换返回 null
     * @see #toDouble(Object, Double)
     */
    public static Double toDouble(Object obj) {
        return toDouble(obj, null);
    }

    /**
     * Object 安全转 Boolean
     * <p>
     * 支持："true"/"1"/"yes"/"on"/"Y"/"T" → true（忽略大小写）
     *
     * @param obj        对象
     * @param defaultVal 默认值
     * @return Boolean；无法判断返回 defaultVal
     */
    public static Boolean toBoolean(Object obj, Boolean defaultVal) {
        if (obj == null) {
            return defaultVal;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        String s = obj.toString().trim().toLowerCase();
        if (s.isEmpty()) {
            return defaultVal;
        }
        if ("true".equals(s) || "1".equals(s) || "yes".equals(s)
                || "on".equals(s) || "y".equals(s) || "t".equals(s)) {
            return true;
        }
        if ("false".equals(s) || "0".equals(s) || "no".equals(s)
                || "off".equals(s) || "n".equals(s) || "f".equals(s)) {
            return false;
        }
        return defaultVal;
    }

    /**
     * Object 安全转 Boolean（null 返回 null）
     *
     * @param obj 对象
     * @return Boolean；无法判断返回 null
     * @see #toBoolean(Object, Boolean)
     */
    public static Boolean toBoolean(Object obj) {
        return toBoolean(obj, null);
    }

    // ==================== 业务工具 ====================

    /**
     * 判断字符串是否等于 "0"（启用状态）
     *
     * @param str 待判断字符串
     * @return true 表示等于 {@link Business#STATUS_ENABLE}
     */
    public static boolean isEnable(String str) {
        return Business.STATUS_ENABLE.equals(str);
    }

    /**
     * 判断字符串是否等于 "1"（禁用状态）
     *
     * @param str 待判断字符串
     * @return true 表示等于 {@link Business#STATUS_DISABLE}
     */
    public static boolean isDisable(String str) {
        return Business.STATUS_DISABLE.equals(str);
    }

    /**
     * 判断字符串是否为 "1"（是）
     *
     * @param str 待判断字符串
     * @return true 表示等于 {@link Business#YES}
     */
    public static boolean isYes(String str) {
        return Business.YES.equals(str);
    }

    /**
     * 判断字符串是否为 "0"（否）
     *
     * @param str 待判断字符串
     * @return true 表示等于 {@link Business#NO}
     */
    public static boolean isNo(String str) {
        return Business.NO.equals(str);
    }

    /**
     * 构建缓存 Key（以冒号分隔各段，跳过空段）
     * <p>示例：{@code buildCacheKey("user", "info", "1") → "user:info:1"}
     *
     * @param parts Key 各段
     * @return 拼接后的缓存 Key；parts 为空返回空字符串
     */
    public static String buildCacheKey(String... parts) {
        if (ArrayUtils.isEmpty(parts)) {
            return EMPTY;
        }
        String result = Stream.of(parts)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(StringPool.COLON.getValue()));
        if (log.isDebugEnabled()) {
            log.debug("buildCacheKey: {}", result);
        }
        return result;
    }

    /**
     * 构建 Redis Key（前缀 + 业务名 + 参数，以冒号分隔）
     * <pre>
     * buildRedisKey("dream", "user", 1, "info")  → "dream:user:1:info"
     * buildRedisKey("", "user", "list")           → "user:list"
     * </pre>
     *
     * @param prefix Key 前缀（如项目名）
     * @param biz    业务标识
     * @param params 附加参数
     * @return Redis Key；prefix 和 biz 都为空时返回空字符串
     */
    public static String buildRedisKey(String prefix, String biz, Object... params) {
        if (isBlank(prefix) && isBlank(biz)) {
            return EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        if (isNotBlank(prefix)) {
            sb.append(prefix).append(StringPool.COLON.getValue());
        }
        if (isNotBlank(biz)) {
            sb.append(biz);
        }
        if (ArrayUtils.isNotEmpty(params)) {
            for (Object param : params) {
                sb.append(StringPool.COLON.getValue()).append(param);
            }
        }
        String result = sb.toString();
        if (log.isDebugEnabled()) {
            log.debug("buildRedisKey: {}", result);
        }
        return result;
    }

    /**
     * 简易格式化（使用 {} 作为占位符，类似 Slf4j 风格）
     * <pre>
     * format("Hello, {}!", "World")         → "Hello, World!"
     * format("User {} logged in at {}", "admin", "2026-07-11")
     *                                       → "User admin logged in at 2026-07-11"
     * format("Value: {}", (Object) null)     → "Value: null"
     * </pre>
     *
     * @param template 模板字符串，使用 {} 作为占位符
     * @param args     参数列表
     * @return 格式化后的字符串；template 为 null 返回 null
     */
    public static String format(String template, Object... args) {
        if (template == null) {
            return null;
        }
        if (args == null || args.length == 0) {
            return template;
        }
        StringBuilder sb = new StringBuilder(template.length() + 64);
        int argIndex = 0;
        int i = 0;
        while (i < template.length()) {
            if (i + 1 < template.length()
                    && template.charAt(i) == '{'
                    && template.charAt(i + 1) == '}') {
                sb.append(argIndex < args.length ? String.valueOf(args[argIndex++]) : "{}");
                i += 2;
            } else {
                sb.append(template.charAt(i));
                i++;
            }
        }
        String result = sb.toString();
        if (log.isDebugEnabled()) {
            log.debug("format: template={}, result={}", template, result);
        }
        return result;
    }

    /**
     * 拼接字符串并添加前缀和后缀
     * <pre>
     * wrap("[", "]", "content")  → "[content]"
     * wrap("(", ")", null)       → "()"
     * </pre>
     *
     * @param prefix  前缀
     * @param suffix  后缀
     * @param content 中间内容
     * @return 拼接结果
     */
    public static String wrap(String prefix, String suffix, String content) {
        return defaultString(prefix) + defaultString(content) + defaultString(suffix);
    }

    // ==================== 截取与填充（支持负索引） ====================

    /**
     * 截取字符串（支持负索引，-1 表示最后一个字符）
     * <pre>
     * substring("Hello", 0)   → "Hello"
     * substring("Hello", 2)   → "llo"
     * substring("Hello", -3)  → "llo"（从倒数第3位开始）
     * </pre>
     *
     * @param str   字符串
     * @param start 开始位置（负数表示从末尾倒数）
     * @return 截取结果；str 为 null 返回空字符串
     */
    public static String substring(String str, int start) {
        if (str == null) {
            return EMPTY;
        }
        if (start < 0) {
            start = str.length() + start;
        }
        if (start < 0) {
            start = 0;
        }
        if (start > str.length()) {
            return EMPTY;
        }
        return str.substring(start);
    }

    /**
     * 截取字符串（支持负索引）
     * <pre>
     * substring("Hello", 1, 4)   → "ell"
     * substring("Hello", -4, -1) → "ell"（从倒数第4位到倒数第1位）
     * substring("Hello", 0, 10)  → "Hello"（超出长度自动截断）
     * </pre>
     *
     * @param str   字符串
     * @param start 开始位置（负数表示从末尾倒数）
     * @param end   结束位置（负数表示从末尾倒数）
     * @return 截取结果；str 为 null 返回空字符串
     */
    public static String substring(String str, int start, int end) {
        if (str == null) {
            return EMPTY;
        }
        if (start < 0) {
            start = str.length() + start;
        }
        if (end < 0) {
            end = str.length() + end;
        }
        if (start < 0) {
            start = 0;
        }
        if (end < 0) {
            end = 0;
        }
        if (end > str.length()) {
            end = str.length();
        }
        if (start > end) {
            return EMPTY;
        }
        return str.substring(start, end);
    }

    /**
     * 左侧补齐（默认补 '0'），常用于订单号、编号生成
     * <pre>
     * pads(42, 6)       → "000042"
     * pads("7", 4)      → "0007"
     * pads("12345", 3)  → "345"（超长截取末尾3位）
     * </pre>
     *
     * @param obj  数字或字符串对象
     * @param size 目标长度
     * @return 补齐后的字符串
     */
    public static String pads(Object obj, int size) {
        return pads(obj, size, '0');
    }

    /**
     * 左侧补齐指定字符，超长则截取末尾 size 位
     * <pre>
     * pads(42, 6, '0')   → "000042"
     * pads("AB", 5, '*') → "***AB"
     * pads(null, 4, '0')  → "0000"
     * </pre>
     *
     * @param obj  数字或字符串对象
     * @param size 目标长度
     * @param c    补齐字符
     * @return 补齐后的字符串
     */
    public static String pads(Object obj, int size, char c) {
        StringBuilder sb = new StringBuilder(size);
        if (obj != null) {
            String objStr = String.valueOf(obj);
            int len = objStr.length();
            if (len <= size) {
                for (int i = size - len; i > 0; i--) {
                    sb.append(c);
                }
                sb.append(objStr);
            } else {
                return objStr.substring(len - size);
            }
        } else {
            for (int i = size; i > 0; i--) {
                sb.append(c);
            }
        }
        String result = sb.toString();
        if (log.isDebugEnabled()) {
            log.debug("pads: obj={}, size={}, result={}", obj, size, result);
        }
        return result;
    }

    /**
     * 生成标准 UUID（含 -）
     * <p>
     * 统一委托 {@link UuidGenerator#uuid()}（UUID v4 随机），与 id 包共用一个后端，
     * 不再单独走 {@code java.util.UUID}。
     * </p>
     * <p>示例：{@code uuid() → "550e8400-e29b-41d4-a716-446655440000"}</p>
     *
     * @return UUID 字符串
     */
    public static String uuid() {
        return UuidGenerator.getInstance().uuid();
    }

    /**
     * 生成无 - 的 UUID
     * <p>示例：{@code simpleUuid() → "550e8400e29b41d4a716446655440000"}
     *
     * @return 无连字符的 UUID 字符串
     */
    public static String simpleUuid() {
        return UuidGenerator.getInstance().simpleUuid();
    }

    /**
     * 生成6位纯数字验证码
     * <p>
     * 使用 {@link SecureRandom} 取值，保证不可预测；每位独立取 0-9，
     * 因此可能出现前导 0（如 {@code "034521"}），比较时请按字符串而非数值。
     * 出于安全考虑不打印验证码日志。
     * </p>
     * <p>示例：{@code generateVerifyCode() → "834521"}</p>
     *
     * @return 6位随机数字字符串
     */
    public static String generateVerifyCode() {
        return generateVerifyCode(VERIFY_CODE_LENGTH);
    }

    /**
     * 生成指定长度的数字验证码。
     *
     * @param length 位数，必须大于 0
     * @return 指定长度的随机数字字符串
     * @throws IllegalArgumentException length 小于 1 时抛出
     */
    public static String generateVerifyCode(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("验证码位数必须大于 0");
        }
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(SECURE_RANDOM.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 生成 [min, max] 范围内的随机整数（包含边界）
     * <p>示例：{@code randomInteger(1, 10) → 7}
     *
     * @param min 最小值（含）
     * @param max 最大值（含）
     * @return 随机整数
     */
    public static Integer randomInteger(Integer min, Integer max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * 从列表中随机获取一个元素
     *
     * @param list 元素列表
     * @param <T>  元素类型
     * @return 随机元素；list 为空返回 null
     */
    public static <T> T randomEle(List<T> list) {
        if (isObjectBlank(list)) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        return list.get(randomInteger(0, list.size() - 1));
    }

    /**
     * 生成 a-z 小写字母列表
     *
     * @return 26 个小写字母组成的列表
     */
    public static List<String> generateAtoZ() {
        return IntStream.rangeClosed('a', 'z')
                .mapToObj(c -> String.valueOf((char) c))
                .collect(Collectors.toList());
    }

    /**
     * 数字金额转中文大写（最高支持亿元）
     * <pre>
     * digitUppercase(0)       → "零元整"
     * digitUppercase(100.5)   → "壹佰元伍角"
     * digitUppercase(1001.01) → "壹仟零壹元零壹分"
     * digitUppercase(-50.5)   → "负伍拾元伍角"
     * </pre>
     *
     * @param dou 金额，范围 [-999999999.99, 999999999.99]
     * @return 中文大写金额
     * @throws IllegalArgumentException 金额超出范围时抛出
     */
    public static String digitUppercase(double dou) {
        if (dou > MAX_MONEY || dou < MIN_MONEY) {
            throw new IllegalArgumentException(
                    "请传入[" + BigDecimal.valueOf(MIN_MONEY) + "至" + BigDecimal.valueOf(MAX_MONEY) + "]之间的金额");
        }
        String head = dou < 0 ? "负" : EMPTY;
        dou = Math.abs(dou);

        StringBuilder s = new StringBuilder();
        for (int i = 0; i < FRACTION.length; i++) {
            s.append((DIGIT[(int) (Math.floor(dou * 10 * Math.pow(10, i)) % 10)] + FRACTION[i])
                    .replaceAll("(零.)+", EMPTY));
        }
        if (s.length() < 1) {
            s = new StringBuilder("整");
        }
        int integerPart = (int) Math.floor(dou);

        for (int i = 0; i < UNIT[0].length && integerPart > 0; i++) {
            StringBuilder p = new StringBuilder();
            for (int j = 0; j < UNIT[1].length && dou > 0; j++) {
                p.insert(0, DIGIT[integerPart % 10] + UNIT[1][j]);
                integerPart = integerPart / 10;
            }
            s.insert(0, p.toString().replaceAll("(零.)*零$", EMPTY).replaceAll("^$", "零") + UNIT[0][i]);
        }
        String result = head + s.toString()
                .replaceAll("(零.)*零元", "元")
                .replaceFirst("(零.)+", EMPTY)
                .replaceAll("(零.)+", "零")
                .replaceAll("^整$", "零元整");
        if (log.isDebugEnabled()) {
            log.debug("digitUppercase: {} → {}", dou, result);
        }
        return result;
    }

    /**
     * 集合分片（将大集合按指定大小拆分为多个子集合）
     * <pre>
     * listChunk(Arrays.asList(1,2,3,4,5), 2)  → [[1,2],[3,4],[5]]
     * </pre>
     *
     * @param list      原始集合
     * @param chunkSize 每片大小
     * @param <T>       元素类型
     * @return 分片后的集合列表；list 为空返回空列表
     */
    public static <T> List<List<T>> listChunk(List<T> list, int chunkSize) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive, actual=" + chunkSize);
        }
        return IntStream.range(0, (int) Math.ceil((double) list.size() / chunkSize))
                .mapToObj(i -> list.subList(i * chunkSize, Math.min((i + 1) * chunkSize, list.size())))
                .collect(Collectors.toList());
    }

    /**
     * URL 路径匹配（支持 ?、*、** 通配符）
     * <ul>
     *   <li>{@code ?}  表示单个字符</li>
     *   <li>{@code *}  表示一层路径内的任意字符串，不可跨层级</li>
     *   <li>{@code **} 表示任意层路径</li>
     * </ul>
     * <pre>
     * isMatch("/api/*", "/api/user")    → true
     * isMatch("/api/**", "/api/user/1") → true
     * isMatch("/user/?.png", "/user/a.png") → true
     * </pre>
     *
     * @param pattern 匹配规则
     * @param url     待匹配的 URL
     * @return true 表示匹配；pattern 或 url 为 null 返回 false
     */
    public static boolean isMatch(String pattern, String url) {
        if (pattern == null || url == null) {
            return false;
        }
        // 将通配符转换为正则表达式
        StringBuilder regex = new StringBuilder("^");
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if (c == '*') {
                if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '*') {
                    regex.append(".*");
                    i += 2;
                } else {
                    regex.append("[^/]*");
                    i++;
                }
            } else if (c == '?') {
                regex.append("[^/]");
                i++;
            } else if ("\\[]{}().+^$|".indexOf(c) >= 0) {
                regex.append('\\').append(c);
                i++;
            } else {
                regex.append(c);
                i++;
            }
        }
        regex.append("$");
        boolean result = url.matches(regex.toString());
        if (log.isDebugEnabled()) {
            log.debug("isMatch: pattern={}, url={}, result={}", pattern, url, result);
        }
        return result;
    }

}