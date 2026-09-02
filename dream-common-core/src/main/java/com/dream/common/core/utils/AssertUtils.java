package com.dream.common.core.utils;

import com.dream.common.core.enums.ResponseCode;
import com.dream.common.core.exception.BusinessException;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;

/**
 * 业务断言工具类
 * <p>
 * 基于 {@link BusinessException} 的全局断言工具，用于在业务逻辑中快速校验参数和状态。
 * 断言失败时自动抛出 {@link BusinessException}，支持自定义消息和 {@link ResponseCode} 错误码。
 * </p>
 * <p>
 * 设计理念：
 * <ul>
 *   <li><b>防御式编程</b>：提前校验参数和状态，快速失败</li>
 *   <li><b>统一错误码</b>：使用 {@link ResponseCode} 枚举统一管理错误码</li>
 *   <li><b>消息参数化</b>：支持 MessageFormat 占位符 {@code {0}}、{@code {1}}</li>
 *   <li><b>链式调用</b>：部分方法返回校验对象，便于后续操作</li>
 * </ul>
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 基础非空校验（返回原值，便于链式操作）
 * User user = AssertUtils.notNull(user, "用户不能为空");
 * String name = AssertUtils.notBlank(user.getName(), "用户名不能为空");
 *
 * // 2. 带 ResponseCode 的校验
 * AssertUtils.notNull(user, ResponseCode.NOT_FOUND, "用户 {0} 不存在", userId);
 *
 * // 3. 条件断言
 * AssertUtils.isTrue(age >= 0, "年龄必须大于等于0");
 * AssertUtils.isTrue(age >= 18, ResponseCode.BAD_REQUEST, "未满18岁");
 *
 * // 4. 集合校验
 * AssertUtils.notEmpty(list, "列表不能为空");
 * AssertUtils.notEmpty(map, ResponseCode.BAD_REQUEST, "Map不能为空");
 *
 * // 5. 字符串校验
 * AssertUtils.notBlank(name, "姓名不能为空");
 * AssertUtils.lengthInRange(phone, 11, 11, "手机号长度必须为11位");
 * AssertUtils.matches(email, "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", "邮箱格式不正确");
 *
 * // 6. 范围校验
 * AssertUtils.inRange(score, 0, 100, "分数必须在 0-100 之间");
 * AssertUtils.greaterThan(price, 0, "价格必须大于0");
 *
 * // 7. 状态校验
 * AssertUtils.state(user.isActive(), "用户状态异常，当前状态：{0}", user.getStatus());
 *
 * // 8. 检查重复数据
 * AssertUtils.isNull(existingUser, "用户已存在：{0}", userName);
 *
 * // 9. 快速失败
 * AssertUtils.fail("操作失败");
 * AssertUtils.fail(ResponseCode.INTERNAL_ERROR, "系统繁忙");
 * }</pre>
 *
 * @author dream
 * @see BusinessException
 * @see ResponseCode
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class AssertUtils {

    private AssertUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 布尔条件断言 ====================

    /**
     * 断言条件为 true
     *
     * @param expression 布尔表达式
     * @param message    失败时的错误消息
     * @throws BusinessException 条件为 false 时抛出
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new BusinessException(message);
        }
    }

    /**
     * 断言条件为 true（带 ResponseCode）
     *
     * @param expression   布尔表达式
     * @param responseCode 错误码
     * @throws BusinessException 条件为 false 时抛出
     */
    public static void isTrue(boolean expression, ResponseCode responseCode) {
        if (!expression) {
            throw new BusinessException(responseCode);
        }
    }

    /**
     * 断言条件为 true（带 ResponseCode + 自定义消息）
     *
     * @param expression   布尔表达式
     * @param responseCode 错误码
     * @param message      自定义消息
     * @throws BusinessException 条件为 false 时抛出
     */
    public static void isTrue(boolean expression, ResponseCode responseCode, String message) {
        if (!expression) {
            throw new BusinessException(responseCode, message);
        }
    }

    /**
     * 断言条件为 true（参数化消息）
     *
     * @param expression   布尔表达式
     * @param responseCode 错误码
     * @param message      消息模板（支持 MessageFormat 占位符 {@code {0}}、{@code {1}}）
     * @param args         消息参数
     * @throws BusinessException 条件为 false 时抛出
     */
    public static void isTrue(boolean expression, ResponseCode responseCode, String message, Object... args) {
        if (!expression) {
            throw new BusinessException(responseCode, formatMessage(message, args));
        }
    }

    /**
     * 断言条件为 false
     *
     * @param expression 布尔表达式
     * @param message    失败时的错误消息
     * @throws BusinessException 条件为 true 时抛出
     */
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw new BusinessException(message);
        }
    }

    /**
     * 断言条件为 false（带 ResponseCode）
     *
     * @param expression   布尔表达式
     * @param responseCode 错误码
     * @throws BusinessException 条件为 true 时抛出
     */
    public static void isFalse(boolean expression, ResponseCode responseCode) {
        if (expression) {
            throw new BusinessException(responseCode);
        }
    }

    /**
     * 断言条件为 false（带 ResponseCode + 自定义消息）
     *
     * @param expression   布尔表达式
     * @param responseCode 错误码
     * @param message      自定义消息
     * @throws BusinessException 条件为 true 时抛出
     */
    public static void isFalse(boolean expression, ResponseCode responseCode, String message) {
        if (expression) {
            throw new BusinessException(responseCode, message);
        }
    }

    // ==================== 空值断言 ====================

    /**
     * 断言对象为 null
     *
     * @param object  待检查对象
     * @param message 失败时的错误消息
     * @throws BusinessException 对象不为 null 时抛出
     */
    public static void isNull(Object object, String message) {
        if (object != null) {
            throw new BusinessException(message);
        }
    }

    /**
     * 断言对象为 null（带 ResponseCode）
     *
     * @param object       待检查对象
     * @param responseCode 错误码
     * @throws BusinessException 对象不为 null 时抛出
     */
    public static void isNull(Object object, ResponseCode responseCode) {
        if (object != null) {
            throw new BusinessException(responseCode);
        }
    }

    /**
     * 断言对象为 null（参数化消息）
     *
     * @param object       待检查对象
     * @param responseCode 错误码
     * @param message      消息模板
     * @param args         消息参数
     * @throws BusinessException 对象不为 null 时抛出
     */
    public static void isNull(Object object, ResponseCode responseCode, String message, Object... args) {
        if (object != null) {
            throw new BusinessException(responseCode, formatMessage(message, args));
        }
    }

    /**
     * 断言对象非 null（返回原对象，便于链式调用）
     * <pre>{@code
     * User user = AssertUtils.notNull(user, "用户不存在");
     * String name = AssertUtils.notNull(user.getName(), "用户名不能为空");
     * }</pre>
     *
     * @param object  待检查对象
     * @param message 失败时的错误消息
     * @param <T>     对象类型
     * @throws BusinessException 对象为 null 时抛出
     */
    public static <T> void notNull(T object, String message) {
        if (object == null) {
            throw new BusinessException(message);
        }
    }

    /**
     * 断言对象非 null（带 ResponseCode，返回原对象）
     *
     * @param object       待检查对象
     * @param responseCode 错误码
     * @param <T>          对象类型
     * @return 非 null 的对象
     * @throws BusinessException 对象为 null 时抛出
     */
    public static <T> T notNull(T object, ResponseCode responseCode) {
        if (object == null) {
            throw new BusinessException(responseCode);
        }
        return object;
    }

    /**
     * 断言对象非 null（带 ResponseCode + 自定义消息，返回原对象）
     *
     * @param object       待检查对象
     * @param responseCode 错误码
     * @param message      自定义消息
     * @param <T>          对象类型
     * @throws BusinessException 对象为 null 时抛出
     */
    public static <T> void notNull(T object, ResponseCode responseCode, String message) {
        if (object == null) {
            throw new BusinessException(responseCode, message);
        }
    }

    /**
     * 断言对象非 null（参数化消息，返回原对象）
     *
     * @param object       待检查对象
     * @param responseCode 错误码
     * @param message      消息模板
     * @param args         消息参数
     * @param <T>          对象类型
     * @return 非 null 的对象
     * @throws BusinessException 对象为 null 时抛出
     */
    public static <T> T notNull(T object, ResponseCode responseCode, String message, Object... args) {
        if (object == null) {
            throw new BusinessException(responseCode, formatMessage(message, args));
        }
        return object;
    }

    // ==================== 字符串断言 ====================

    /**
     * 断言字符串非空白（非 null 且 trim 后非空）
     * <pre>{@code
     * String name = AssertUtils.notBlank(username, "用户名不能为空");
     * }</pre>
     *
     * @param text    待检查字符串
     * @param message 失败时的错误消息
     * @return 非空白的字符串
     * @throws BusinessException 字符串为空白时抛出
     */
    public static String notBlank(String text, String message) {
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(message);
        }
        return text;
    }

    /**
     * 断言字符串非空白（带 ResponseCode）
     *
     * @param text         待检查字符串
     * @param responseCode 错误码
     * @return 非空白的字符串
     * @throws BusinessException 字符串为空白时抛出
     */
    public static String notBlank(String text, ResponseCode responseCode) {
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(responseCode);
        }
        return text;
    }

    /**
     * 断言字符串非空白（带 ResponseCode + 自定义消息）
     *
     * @param text         待检查字符串
     * @param responseCode 错误码
     * @param message      自定义消息
     * @return 非空白的字符串
     * @throws BusinessException 字符串为空白时抛出
     */
    public static String notBlank(String text, ResponseCode responseCode, String message) {
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(responseCode, message);
        }
        return text;
    }

    /**
     * 断言字符串非空（非 null 且长度 &gt; 0）
     *
     * @param text    待检查字符串
     * @param message 失败时的错误消息
     * @return 非空的字符串
     * @throws BusinessException 字符串为空时抛出
     */
    public static String notEmpty(String text, String message) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException(message);
        }
        return text;
    }

    /**
     * 断言字符串非空（带 ResponseCode）
     *
     * @param text         待检查字符串
     * @param responseCode 错误码
     * @return 非空的字符串
     * @throws BusinessException 字符串为空时抛出
     */
    public static String notEmpty(String text, ResponseCode responseCode) {
        if (text == null || text.isEmpty()) {
            throw new BusinessException(responseCode);
        }
        return text;
    }

    /**
     * 断言字符串长度为指定值
     *
     * @param text    待检查字符串
     * @param length  期望长度
     * @param message 失败时的错误消息
     * @return 符合条件的字符串
     * @throws BusinessException 长度不匹配时抛出
     */
    public static String lengthEquals(String text, int length, String message) {
        if (text == null || text.length() != length) {
            throw new BusinessException(message);
        }
        return text;
    }

    /**
     * 断言字符串长度在范围内
     *
     * @param text    待检查字符串
     * @param min     最小长度（含）
     * @param max     最大长度（含）
     * @param message 失败时的错误消息
     * @return 符合条件的字符串
     * @throws BusinessException 长度超出范围时抛出
     */
    public static String lengthInRange(String text, int min, int max, String message) {
        if (text == null || text.length() < min || text.length() > max) {
            throw new BusinessException(message);
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式
     *
     * @param text    待检查字符串
     * @param regex   正则表达式
     * @param message 失败时的错误消息
     * @return 匹配的字符串
     * @throws BusinessException 字符串不匹配正则表达式时抛出
     */
    public static String matches(String text, String regex, String message) {
        notNull(text, message);
        if (!text.matches(regex)) {
            throw new BusinessException(message);
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（带 ResponseCode）
     *
     * @param text         待检查字符串
     * @param regex        正则表达式
     * @param responseCode 错误码
     * @param message      失败时的错误消息
     * @return 匹配的字符串
     * @throws BusinessException 字符串不匹配正则表达式时抛出
     */
    public static String matches(String text, String regex, ResponseCode responseCode, String message) {
        notNull(text, responseCode, message);
        if (!text.matches(regex)) {
            throw new BusinessException(responseCode, message);
        }
        return text;
    }

    // ==================== 集合断言 ====================

    /**
     * 断言集合非空（非 null 且至少有一个元素）
     * <pre>{@code
     * List<User> users = AssertUtils.notEmpty(userList, "用户列表不能为空");
     * }</pre>
     *
     * @param collection 待检查集合
     * @param message    失败时的错误消息
     * @param <E>        集合元素类型
     * @return 非空的集合
     * @throws BusinessException 集合为空时抛出
     */
    public static <E> Collection<E> notEmpty(Collection<E> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(message);
        }
        return collection;
    }

    /**
     * 断言集合非空（带 ResponseCode）
     *
     * @param collection   待检查集合
     * @param responseCode 错误码
     * @param <E>          集合元素类型
     * @return 非空的集合
     * @throws BusinessException 集合为空时抛出
     */
    public static <E> Collection<E> notEmpty(Collection<E> collection, ResponseCode responseCode) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(responseCode);
        }
        return collection;
    }

    /**
     * 断言集合非空（带 ResponseCode + 自定义消息）
     *
     * @param collection   待检查集合
     * @param responseCode 错误码
     * @param message      自定义消息
     * @param <E>          集合元素类型
     * @return 非空的集合
     * @throws BusinessException 集合为空时抛出
     */
    public static <E> Collection<E> notEmpty(Collection<E> collection, ResponseCode responseCode, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(responseCode, message);
        }
        return collection;
    }

    /**
     * 断言 Map 非空
     *
     * @param map     待检查 Map
     * @param message 失败时的错误消息
     * @param <K>     键类型
     * @param <V>     值类型
     * @return 非空的 Map
     * @throws BusinessException Map 为空时抛出
     */
    public static <K, V> Map<K, V> notEmpty(Map<K, V> map, String message) {
        if (map == null || map.isEmpty()) {
            throw new BusinessException(message);
        }
        return map;
    }

    /**
     * 断言 Map 非空（带 ResponseCode）
     *
     * @param map          待检查 Map
     * @param responseCode 错误码
     * @param <K>          键类型
     * @param <V>          值类型
     * @return 非空的 Map
     * @throws BusinessException Map 为空时抛出
     */
    public static <K, V> Map<K, V> notEmpty(Map<K, V> map, ResponseCode responseCode) {
        if (map == null || map.isEmpty()) {
            throw new BusinessException(responseCode);
        }
        return map;
    }

    /**
     * 断言数组非空（非 null 且至少有一个元素）
     *
     * @param array   待检查数组
     * @param message 失败时的错误消息
     * @param <T>     数组元素类型
     * @return 非空的数组
     * @throws BusinessException 数组为空时抛出
     */
    public static <T> T[] notEmpty(T[] array, String message) {
        if (array == null || array.length == 0) {
            throw new BusinessException(message);
        }
        return array;
    }

    /**
     * 断言数组非空（带 ResponseCode）
     *
     * @param array        待检查数组
     * @param responseCode 错误码
     * @param <T>          数组元素类型
     * @return 非空的数组
     * @throws BusinessException 数组为空时抛出
     */
    public static <T> T[] notEmpty(T[] array, ResponseCode responseCode) {
        if (array == null || array.length == 0) {
            throw new BusinessException(responseCode);
        }
        return array;
    }

    // ==================== 状态断言 ====================

    /**
     * 状态断言（用于检查对象/系统的业务状态）
     * <pre>{@code
     * AssertUtils.state(user.isEnabled(), "用户已被禁用");
     * AssertUtils.state(order.isPaid(), ResponseCode.BAD_REQUEST, "订单未支付");
     * }</pre>
     *
     * @param expression 状态表达式
     * @param message    失败时的错误消息
     * @throws BusinessException 状态不满足时抛出
     */
    public static void state(boolean expression, String message) {
        if (!expression) {
            throw new BusinessException(message);
        }
    }

    /**
     * 状态断言（带 ResponseCode）
     *
     * @param expression   状态表达式
     * @param responseCode 错误码
     * @throws BusinessException 状态不满足时抛出
     */
    public static void state(boolean expression, ResponseCode responseCode) {
        if (!expression) {
            throw new BusinessException(responseCode);
        }
    }

    /**
     * 状态断言（带 ResponseCode + 自定义消息）
     *
     * @param expression   状态表达式
     * @param responseCode 错误码
     * @param message      自定义消息
     * @throws BusinessException 状态不满足时抛出
     */
    public static void state(boolean expression, ResponseCode responseCode, String message) {
        if (!expression) {
            throw new BusinessException(responseCode, message);
        }
    }

    /**
     * 状态断言（参数化消息）
     *
     * @param expression   状态表达式
     * @param responseCode 错误码
     * @param message      消息模板
     * @param args         消息参数
     * @throws BusinessException 状态不满足时抛出
     */
    public static void state(boolean expression, ResponseCode responseCode, String message, Object... args) {
        if (!expression) {
            throw new BusinessException(responseCode, formatMessage(message, args));
        }
    }

    // ==================== 相等断言 ====================

    /**
     * 断言两个对象相等（使用 {@link Object#equals}）
     *
     * @param actual   实际值
     * @param expected 期望值
     * @param message  失败时的错误消息
     * @throws BusinessException 不相等时抛出
     */
    public static void equals(Object actual, Object expected, String message) {
        if (expected == null) {
            if (actual != null) {
                throw new BusinessException(message);
            }
        } else if (!expected.equals(actual)) {
            throw new BusinessException(message);
        }
    }

    /**
     * 断言两个对象相等（带 ResponseCode）
     *
     * @param actual       实际值
     * @param expected     期望值
     * @param responseCode 错误码
     * @throws BusinessException 不相等时抛出
     */
    public static void equals(Object actual, Object expected, ResponseCode responseCode) {
        if (expected == null) {
            if (actual != null) {
                throw new BusinessException(responseCode);
            }
        } else if (!expected.equals(actual)) {
            throw new BusinessException(responseCode);
        }
    }

    /**
     * 断言两个对象不相等
     *
     * @param actual     实际值
     * @param unexpected 不应等于的值
     * @param message    失败时的错误消息
     * @throws BusinessException 相等时抛出
     */
    public static void notEquals(Object actual, Object unexpected, String message) {
        if (unexpected == null) {
            if (actual == null) {
                throw new BusinessException(message);
            }
        } else if (unexpected.equals(actual)) {
            throw new BusinessException(message);
        }
    }

    // ==================== 范围断言 ====================

    /**
     * 断言数值在指定范围内（含边界）
     * <pre>{@code
     * AssertUtils.inRange(score, 0, 100, "分数必须在 0-100 之间");
     * }</pre>
     *
     * @param value   待检查数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param message 失败时的错误消息
     * @param <T>     数值类型
     * @return 范围内的数值
     * @throws BusinessException 超出范围时抛出
     */
    public static <T extends Number> T inRange(T value, Number min, Number max, String message) {
        if (value == null || min == null || max == null
                || value.doubleValue() < min.doubleValue() || value.doubleValue() > max.doubleValue()) {
            throw new BusinessException(message);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（带 ResponseCode）
     *
     * @param value        待检查数值
     * @param min          最小值（含）
     * @param max          最大值（含）
     * @param responseCode 错误码
     * @param message      失败时的错误消息
     * @param <T>          数值类型
     * @return 范围内的数值
     * @throws BusinessException 超出范围时抛出
     */
    public static <T extends Number> T inRange(T value, Number min, Number max, ResponseCode responseCode, String message) {
        if (value == null || min == null || max == null
                || value.doubleValue() < min.doubleValue() || value.doubleValue() > max.doubleValue()) {
            throw new BusinessException(responseCode, message);
        }
        return value;
    }

    /**
     * 断言数值大于指定值
     *
     * @param value   待检查数值
     * @param target  目标值
     * @param message 失败时的错误消息
     * @param <T>     数值类型
     * @return 满足条件的数值
     * @throws BusinessException 不满足时抛出
     */
    public static <T extends Number> T greaterThan(T value, Number target, String message) {
        if (value == null || target == null || value.doubleValue() <= target.doubleValue()) {
            throw new BusinessException(message);
        }
        return value;
    }

    /**
     * 断言数值大于等于指定值
     *
     * @param value   待检查数值
     * @param target  目标值
     * @param message 失败时的错误消息
     * @param <T>     数值类型
     * @return 满足条件的数值
     * @throws BusinessException 不满足时抛出
     */
    public static <T extends Number> T greaterOrEqual(T value, Number target, String message) {
        if (value == null || target == null || value.doubleValue() < target.doubleValue()) {
            throw new BusinessException(message);
        }
        return value;
    }

    /**
     * 断言数值小于指定值
     *
     * @param value   待检查数值
     * @param target  目标值
     * @param message 失败时的错误消息
     * @param <T>     数值类型
     * @return 满足条件的数值
     * @throws BusinessException 不满足时抛出
     */
    public static <T extends Number> T lessThan(T value, Number target, String message) {
        if (value == null || target == null || value.doubleValue() >= target.doubleValue()) {
            throw new BusinessException(message);
        }
        return value;
    }

    /**
     * 断言数值小于等于指定值
     *
     * @param value   待检查数值
     * @param target  目标值
     * @param message 失败时的错误消息
     * @param <T>     数值类型
     * @return 满足条件的数值
     * @throws BusinessException 不满足时抛出
     */
    public static <T extends Number> T lessOrEqual(T value, Number target, String message) {
        if (value == null || target == null || value.doubleValue() > target.doubleValue()) {
            throw new BusinessException(message);
        }
        return value;
    }

    /**
     * 断言数值为正数（> 0）
     *
     * @param value   待检查数值
     * @param message 失败时的错误消息
     * @param <T>     数值类型
     * @return 正数
     * @throws BusinessException 数值 <= 0 时抛出
     */
    public static <T extends Number> T positive(T value, String message) {
        if (value == null || value.doubleValue() <= 0) {
            throw new BusinessException(message);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）
     *
     * @param value   待检查数值
     * @param message 失败时的错误消息
     * @param <T>     数值类型
     * @return 非负数
     * @throws BusinessException 数值 < 0 时抛出
     */
    public static <T extends Number> T nonNegative(T value, String message) {
        if (value == null || value.doubleValue() < 0) {
            throw new BusinessException(message);
        }
        return value;
    }

    // ==================== 快速失败 ====================

    /**
     * 直接抛出业务异常
     * <pre>{@code
     * AssertUtils.fail("操作失败");
     * }</pre>
     *
     * @param message 错误消息
     * @throws BusinessException 始终抛出
     */
    public static void fail(String message) {
        throw new BusinessException(message);
    }

    /**
     * 直接抛出业务异常（带 ResponseCode）
     *
     * @param responseCode 错误码
     * @throws BusinessException 始终抛出
     */
    public static void fail(ResponseCode responseCode) {
        throw new BusinessException(responseCode);
    }

    /**
     * 直接抛出业务异常（带 ResponseCode + 自定义消息）
     *
     * @param responseCode 错误码
     * @param message      自定义消息
     * @throws BusinessException 始终抛出
     */
    public static void fail(ResponseCode responseCode, String message) {
        throw new BusinessException(responseCode, message);
    }

    /**
     * 直接抛出业务异常（参数化消息）
     *
     * @param responseCode 错误码
     * @param message      消息模板
     * @param args         消息参数
     * @throws BusinessException 始终抛出
     */
    public static void fail(ResponseCode responseCode, String message, Object... args) {
        throw new BusinessException(responseCode, formatMessage(message, args));
    }

    /**
     * 直接抛出业务异常（带原因）
     *
     * @param message 错误消息
     * @param cause   原因
     * @throws BusinessException 始终抛出
     */
    public static void fail(String message, Throwable cause) {
        throw new BusinessException(message, cause);
    }

    /**
     * 直接抛出业务异常（带 ResponseCode + 原因）
     *
     * @param responseCode 错误码
     * @param message      错误消息
     * @param cause        原因
     * @throws BusinessException 始终抛出
     */
    public static void fail(ResponseCode responseCode, String message, Throwable cause) {
        throw new BusinessException(responseCode, message, cause);
    }

    // ==================== 私有方法 ====================

    /**
     * 格式化消息（支持 MessageFormat 占位符）
     *
     * @param message 消息模板
     * @param args    参数
     * @return 格式化后的消息
     */
    private static String formatMessage(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        try {
            return MessageFormat.format(message, args);
        } catch (IllegalArgumentException e) {
            // 格式化失败时返回原始消息
            return message;
        }
    }
}