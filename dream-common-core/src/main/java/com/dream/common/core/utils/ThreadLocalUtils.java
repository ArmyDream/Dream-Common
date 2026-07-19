package com.dream.common.core.utils;

import java.util.*;
import java.util.function.Supplier;

/**
 * 线程上下文工具类
 * <p>
 * 基于 {@link ThreadLocal} 实现线程级别的上下文变量存储，适用于：
 * <ul>
 *   <li>请求链路追踪（traceId、spanId）</li>
 *   <li>用户会话信息（userId、userName）</li>
 *   <li>请求上下文传递（requestId、clientIp）</li>
 *   <li>数据库连接/事务上下文</li>
 *   <li>线程池任务上下文传递</li>
 * </ul>
 * </p>
 * <p>
 * <b>设计特点：</b>
 * <ul>
 *   <li><b>单 Map 集中管理</b>：所有键值对存储在当前线程的同一个 Map 中，便于统一清理</li>
 *   <li><b>子线程继承</b>：使用 {@link InheritableThreadLocal}，子线程自动继承父线程上下文</li>
 *   <li><b>类型安全</b>：泛型 get 方法 + 类型安全转换 {@link #getAs(String, Class)}</li>
 *   <li><b>函数式支持</b>：{@link #computeIfAbsent(String, Supplier)} 延迟计算</li>
 *   <li><b>零依赖</b>：纯 JDK 实现，无任何第三方依赖</li>
 * </ul>
 * </p>
 * <p>
 * <b>重要提醒：</b>
 * <br>
 * 在 Web 应用或线程池场景中，任务执行完毕后务必调用 {@link #clear()} 清理上下文，
 * 防止内存泄漏或上下文污染。推荐在以下位置调用：
 * <ul>
 *   <li>Spring MVC {@code HandlerInterceptor.afterCompletion()}</li>
 *   <li>Servlet Filter 的 {@code finally} 块</li>
 *   <li>线程池任务的 {@code finally} 块</li>
 * </ul>
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 存储上下文
 * ThreadLocalUtils.set("userId", "12345");
 * ThreadLocalUtils.set("traceId", UUID.randomUUID().toString());
 *
 * // 2. 获取上下文（类型安全）
 * String userId = ThreadLocalUtils.get("userId");
 * String traceId = ThreadLocalUtils.get("traceId");
 *
 * // 3. 带默认值获取
 * String name = ThreadLocalUtils.get("name", "anonymous");
 *
 * // 4. 安全类型转换
 * Long userId = ThreadLocalUtils.getAs("userId", Long.class);
 *
 * // 5. 函数式获取（不存在时计算并存储）
 * String token = ThreadLocalUtils.computeIfAbsent("token", () -> generateToken());
 *
 * // 6. 判断是否存在
 * if (ThreadLocalUtils.containsKey("userId")) { ... }
 *
 * // 7. 获取所有上下文
 * Map<String, Object> context = ThreadLocalUtils.getContext();
 *
 * // 8. 批量设置
 * Map<String, Object> data = new HashMap<>();
 * data.put("userId", "12345");
 * data.put("role", "admin");
 * ThreadLocalUtils.setAll(data);
 *
 * // 9. 上下文执行（自动清理）
 * ThreadLocalUtils.withContext(data, () -> {
 *     // 业务逻辑
 * });
 *
 * // 10. 清理上下文（重要！）
 * ThreadLocalUtils.clear();
 * }</pre>
 *
 * @author dream
 * @see ThreadLocal
 * @see InheritableThreadLocal
 * @since 1.0.0
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class ThreadLocalUtils {

    /**
     * 线程上下文存储容器
     * <p>
     * 使用 {@link InheritableThreadLocal} 支持子线程自动继承父线程的上下文。
     * </p>
     */
    private static final ThreadLocal<Map<String, Object>> CONTEXT =
            new InheritableThreadLocal<Map<String, Object>>() {
                @Override
                protected Map<String, Object> initialValue() {
                    return new HashMap<>(8);
                }
            };

    // ==================== 私有构造器 ====================

    private ThreadLocalUtils() {
        throw new UnsupportedOperationException("ThreadLocalUtils is a utility class");
    }

    // ==================== 字符串工具（零依赖） ====================

    /**
     * 判断字符串是否为空（null 或 trim 后为空）
     *
     * @param str 待判断字符串
     * @return true=为空
     */
    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 判断字符串是否非空（非 null 且 trim 后非空）
     *
     * @param str 待判断字符串
     * @return true=非空
     */
    private static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    // ==================== 获取上下文 Map ====================

    /**
     * 获取当前线程的上下文 Map
     *
     * @return 当前线程的上下文 Map（不为 null）
     */
    private static Map<String, Object> getContextMap() {
        return CONTEXT.get();
    }

    // ==================== 设置 ====================

    /**
     * 设置线程上下文值
     * <p>
     * 如果 key 为 null 或空字符串（trim 后），不执行任何操作。
     * 如果 value 为 null，等效于移除该键。
     * </p>
     *
     * @param key   键
     * @param value 值（可为 null）
     */
    public static void set(String key, Object value) {
        if (isBlank(key)) {
            return;
        }
        Map<String, Object> map = getContextMap();
        if (value == null) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }

    /**
     * 批量设置线程上下文值
     * <p>
     * 自动跳过 key 为 null 或空字符串（trim 后）的条目。
     * </p>
     *
     * @param map 键值对集合（为 null 或空时不执行任何操作）
     */
    public static void setAll(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return;
        }
        Map<String, Object> contextMap = getContextMap();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (isNotBlank(key)) {
                Object value = entry.getValue();
                if (value == null) {
                    contextMap.remove(key);
                } else {
                    contextMap.put(key, value);
                }
            }
        }
    }

    // ==================== 获取 ====================

    /**
     * 获取线程上下文值
     *
     * @param key 键
     * @param <T> 值类型（自动推断）
     * @return 值；不存在或 key 无效时返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        if (isBlank(key)) {
            return null;
        }
        return (T) getContextMap().get(key);
    }

    /**
     * 获取线程上下文值（带默认值）
     *
     * @param key          键
     * @param defaultValue 默认值（可为 null）
     * @param <T>          值类型
     * @return 值；不存在或 key 无效时返回 defaultValue
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key, T defaultValue) {
        if (isBlank(key)) {
            return defaultValue;
        }
        Object value = getContextMap().get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 获取线程上下文值（安全类型转换）
     * <p>
     * 使用 {@link Class#cast(Object)} 进行类型检查，类型不匹配时返回 null。
     * </p>
     *
     * @param key   键
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 值；不存在、key 无效或类型不匹配时返回 null
     */
    public static <T> T getAs(String key, Class<T> clazz) {
        if (isBlank(key) || clazz == null) {
            return null;
        }
        Object value = getContextMap().get(key);
        if (value == null) {
            return null;
        }
        try {
            return clazz.cast(value);
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * 获取线程上下文值（Optional 包装）
     *
     * @param key 键
     * @param <T> 值类型
     * @return Optional 包装的值；key 无效时返回 Optional.empty()
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getOptional(String key) {
        if (isBlank(key)) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) getContextMap().get(key));
    }

    /**
     * 函数式获取：键不存在时通过 Supplier 计算并存储
     * <p>
     * 适用于默认值计算开销较大的场景。
     * </p>
     *
     * @param key      键
     * @param supplier 值计算器（不能为 null）
     * @param <T>      值类型
     * @return 已有值或计算结果；key 无效或 supplier 为 null 时返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T computeIfAbsent(String key, Supplier<T> supplier) {
        if (isBlank(key) || supplier == null) {
            return null;
        }
        Map<String, Object> map = getContextMap();
        Object value = map.get(key);
        if (value == null) {
            value = supplier.get();
            if (value != null) {
                map.put(key, value);
            }
        }
        return (T) value;
    }

    // ==================== 判断 ====================

    /**
     * 判断指定键是否存在
     *
     * @param key 键
     * @return true=存在且值不为 null；key 无效时返回 false
     */
    public static boolean containsKey(String key) {
        if (isBlank(key)) {
            return false;
        }
        return getContextMap().containsKey(key);
    }

    /**
     * 判断当前线程上下文是否为空
     *
     * @return true=无任何键值对
     */
    public static boolean isEmpty() {
        return getContextMap().isEmpty();
    }

    /**
     * 判断当前线程是否持有上下文
     *
     * @return true=已持有上下文（有键值对）
     */
    public static boolean hasContext() {
        return !isEmpty();
    }

    // ==================== 移除 ====================

    /**
     * 移除指定键的值
     *
     * @param key 键
     * @return 被移除的值；不存在或 key 无效时返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T remove(String key) {
        if (isBlank(key)) {
            return null;
        }
        return (T) getContextMap().remove(key);
    }

    /**
     * 批量移除指定键的值
     *
     * @param keys 键集合（为 null 时不执行任何操作）
     */
    public static void removeKeys(Iterable<String> keys) {
        if (keys == null) {
            return;
        }
        Map<String, Object> map = getContextMap();
        for (String key : keys) {
            if (isNotBlank(key)) {
                map.remove(key);
            }
        }
    }

    /**
     * 清除当前线程的所有上下文
     * <p>
     * <strong>重要</strong>：调用 {@link ThreadLocal#remove()} 彻底清理，
     * 防止线程池场景下的内存泄漏。
     * </p>
     * <p>
     * 推荐在以下位置调用：
     * <ul>
     *   <li>Spring MVC {@code HandlerInterceptor.afterCompletion()}</li>
     *   <li>Servlet Filter 的 {@code finally} 块</li>
     *   <li>线程池任务的 {@code finally} 块</li>
     * </ul>
     * </p>
     */
    public static void clear() {
        CONTEXT.remove();
    }

    // ==================== 上下文视图 ====================

    /**
     * 获取当前线程上下文的不可变视图（副本）
     * <p>
     * 返回的 Map 为只读副本，修改不会影响原始上下文。
     * </p>
     *
     * @return 不可变的上下文 Map（副本）
     */
    public static Map<String, Object> getContext() {
        Map<String, Object> map = getContextMap();
        if (map.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<>(map));
    }

    /**
     * 获取当前线程上下文的键集合（不可变）
     *
     * @return 键集合（不可变）
     */
    public static Set<String> keySet() {
        return Collections.unmodifiableSet(getContextMap().keySet());
    }

    /**
     * 获取当前线程上下文的键值对数量
     *
     * @return 数量
     */
    public static int size() {
        return getContextMap().size();
    }

    // ==================== 上下文执行 ====================

    /**
     * 在当前线程上下文中执行操作，操作后自动清理
     * <p>
     * 适用于需要临时设置上下文，执行完毕后自动清理的场景。
     * </p>
     *
     * @param context  临时上下文数据
     * @param runnable 要执行的操作
     * @throws IllegalArgumentException 如果 context 或 runnable 为 null
     */
    public static void withContext(Map<String, Object> context, Runnable runnable) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (runnable == null) {
            throw new IllegalArgumentException("runnable must not be null");
        }
        Map<String, Object> savedContext = new HashMap<>(getContextMap());
        try {
            clear();
            setAll(context);
            runnable.run();
        } finally {
            clear();
            setAll(savedContext);
        }
    }

    /**
     * 在当前线程上下文中执行操作，操作后自动清理（带返回值）
     *
     * @param context  临时上下文数据
     * @param supplier 要执行的操作
     * @param <T>      返回值类型
     * @return 操作结果
     * @throws IllegalArgumentException 如果 context 或 supplier 为 null
     */
    public static <T> T withContext(Map<String, Object> context, Supplier<T> supplier) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (supplier == null) {
            throw new IllegalArgumentException("supplier must not be null");
        }
        Map<String, Object> savedContext = new HashMap<>(getContextMap());
        try {
            clear();
            setAll(context);
            return supplier.get();
        } finally {
            clear();
            setAll(savedContext);
        }
    }

    // ==================== 跨线程传递 ====================

    /**
     * 获取当前上下文的快照（用于跨线程传递）
     * <p>
     * 由于使用了 {@link InheritableThreadLocal}，子线程默认自动继承父线程上下文。
     * 此方法返回当前上下文的副本，便于在创建子线程时显式传递。
     * </p>
     *
     * @return 当前上下文副本
     */
    public static Map<String, Object> snapshot() {
        return new HashMap<>(getContextMap());
    }

    /**
     * 为当前线程恢复上下文快照（先清理再恢复）
     *
     * @param snapshot 从其他线程获取的上下文快照
     * @throws IllegalArgumentException 如果 snapshot 为 null
     */
    public static void restoreSnapshot(Map<String, Object> snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
        clear();
        if (!snapshot.isEmpty()) {
            setAll(snapshot);
        }
    }
}