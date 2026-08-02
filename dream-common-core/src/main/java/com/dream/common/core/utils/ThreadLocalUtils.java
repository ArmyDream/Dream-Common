package com.dream.common.core.utils;

import com.dream.common.core.constant.SysKey;

import java.util.HashMap;
import java.util.Map;

/**
 * 线程本地上下文工具类，用于在当前线程内安全地存取和共享数据。
 *
 * <p>核心功能：</p>
 * <ul>
 *     <li>通用数据存取：提供基于键值对的 put/get/remove 操作，支持在同一线程内共享变量。</li>
 *     <li>上下文快照：提供 getSnapshot 和 setSnapshot 方法，用于解决线程池场景下子线程无法获取父线程上下文的问题。</li>
 *     <li>业务强类型快捷方法：内置如 setTraceId/getTraceId 等方法，消除魔法值，提升开发体验。</li>
 * </ul>
 *
 * <p>使用限制与副作用：</p>
 * <ul>
 *     <li>内存泄漏风险：由于内部使用了 static final 的 ThreadLocal，必须在线程归还给线程池前调用 clear() 方法清理上下文，
 *         否则会导致内存泄漏和数据污染。建议在 Filter 或 Interceptor 的 finally 块中调用 clear()。</li>
 *     <li>空值过滤：put 方法会自动过滤 null 值和空白键，存入的值不可为 null。</li>
 *     <li>类型转换警告：get 方法使用了泛型强制转换，调用方需确保取出的类型与存入类型一致，否则会抛出 ClassCastException。</li>
 * </ul>
 *
 * <p>代码示例：</p>
 * <pre>
 *     // 存取数据
 *     ThreadLocalUtils.put("userId", "12345");
 *     String userId = ThreadLocalUtils.get("userId");
 *
 *     // 线程池上下文传递
 *     Map&lt;String, Object&gt; snapshot = ThreadLocalUtils.getSnapshot();
 *     executor.submit(() -&gt; {
 *         try {
 *             ThreadLocalUtils.setSnapshot(snapshot);
 *             // 执行业务逻辑...
 *         } finally {
 *             ThreadLocalUtils.clear();
 *         }
 *     });
 * </pre>
 *
 * <p>构造函数参数：</p>
 * 无（该类为静态工具类，无需实例化，且无显式构造函数）。
 * //t
 * //r
 * //n
 */
public class ThreadLocalUtils {

    // 优化1：使用 static final，保证全局唯一的 ThreadLocal 实例，避免频繁创建导致内存泄漏
    private static final ThreadLocal<Map<String, Object>> CONTEXT_HOLDER =
            ThreadLocal.withInitial(HashMap::new);

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

    // ==================== 通用存取删 ====================

    /**
     * 向上下文容器中存储键值对
     *
     * @param key   键名，用于后续获取值的标识
     * @param value 需要存储的值，不能为null
     */
    public static void put(String key, Object value) {
        // 检查value是否为null，不为null才进行存储
        if (value != null && isNotBlank(key)) {
            // 获取当前线程的上下文容器，并将键值对存入其中
            CONTEXT_HOLDER.get().put(key, value);
        }
    }

    /**
     * 根据指定的键获取对应的值
     *
     * @param key 要获取值的键，不能为空或null
     * @param <T> 返回值的泛型类型
     * @return 返回键对应的值，如果键不存在或为空则返回null
     */
    public static <T> T get(String key) {
        // 检查键是否为空或null，如果是则直接返回null
        if (isBlank(key)) {
            return null;
        }
        // 从上下文持有者中获取键对应的值并强制转换为泛型类型 T后返回
        return (T) CONTEXT_HOLDER.get().get(key);
    }

    /**
     * 根据指定的键从当前线程的上下文中移除对应的值
     *
     * @param key 要移除的键，如果为空则直接返回
     */
    public static void remove(String key) {
        // 检查键是否为空，如果为空则直接返回不做任何操作
        if (isBlank(key)) {
            return;
        }
        // 从当前线程的上下文中移除指定键对应的值
        CONTEXT_HOLDER.get().remove(key);
    }

    /**
     * 必须在 Filter/Interceptor 的 finally 中调用，防止内存泄漏
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    // ==================== 批量操作 (核心：解决线程池上下文传递问题) ====================

    /**
     * 获取当前线程的上下文快照
     */
    public static Map<String, Object> getSnapshot() {
        return new HashMap<>(CONTEXT_HOLDER.get());
    }

    /**
     * 恢复上下文快照
     */
    public static void setSnapshot(Map<String, Object> snapshot) {
        if (snapshot != null) {
            CONTEXT_HOLDER.get().putAll(snapshot);
        }
    }

    // ==================== 业务强类型快捷方法 (消除魔法值，提升开发体验) ====================

    public static void setTraceId(String traceId) {
        if (isNotBlank(traceId)) {
            put(SysKey.TRACE_ID, traceId);
        }
    }

    public static String getTraceId() {
        return get(SysKey.TRACE_ID);
    }
}
