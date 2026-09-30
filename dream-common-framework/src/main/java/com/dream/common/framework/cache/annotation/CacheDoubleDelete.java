package com.dream.common.framework.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 延时双删缓存注解
 * <p>
 * 用于保证「数据库 + 缓存」最终一致性。被标注的方法执行时，
 * 由 {@code CacheDoubleDeleteAspect} 织入以下流程：
 * <ol>
 *   <li><b>第一次删除</b>：方法执行前删除缓存，避免旧请求回源读到旧数据并回写</li>
 *   <li><b>执行业务</b>：执行被拦截方法（更新数据库）</li>
 *   <li><b>延时第二次删除</b>：方法正常返回后，延迟 {@link #delay()} 时间再次删除缓存，
 *       清理在主从同步延迟窗口内被其他请求写回的旧数据</li>
 * </ol>
 * </p>
 *
 * <h2>延时双删原理</h2>
 * <pre>
 *   时刻 T0：删缓存 ──► DB 写 ──► (主从同步延迟窗口) ──► 延时 T1 ──► 再删缓存
 * </pre>
 * <p>
 * 第二次删除通过 {@link com.dream.common.framework.utils.ThreadPoolUtils} 异步延时执行，
 * 不阻塞业务线程，不影响方法返回值。
 * </p>
 *
 * <h2>Key 生成规则</h2>
 * <ul>
 *   <li>{@link #key()}：固定 key 或前缀（与 {@link #keys()} 互斥，二者均配置时 {@link #keys()} 优先）</li>
 *   <li>{@link #keys()}：多个固定 key（批量删除）</li>
 *   <li>{@link #spel()}：SpEL 表达式，动态解析方法参数（如 {@code "#dto.id"}、{@code "#args[0]"}）</li>
 *   <li>{@link #prefix()}：是否作为前缀匹配批量删除（配合 {@link #key()} 使用，如 {@code key="user:", prefix=true}）</li>
 * </ul>
 * <p>
 * 多 Key 场景可在同一方法上重复标注（{@link Repeatable}）。
 * </p>
 *
 * <h2>与 {@code @QueryCache} 配对</h2>
 * <p>
 * 本注解<b>不做 Key 前缀拼接</b>：{@link #key()} / {@link #keys()} / {@link #spel()} 求出的字符串
 * 就是最终删除的 Key（{@link #prefix()} 只决定"精确删"还是"按前缀批量删"）。
 * 因此要删掉 {@code QueryCache} 写入的缓存，两侧 Key 必须逐字一致，
 * 含 {@code QueryCache} 侧 {@code prefix} 拼上的模块前缀。
 * 命名空间约定见 {@link QueryCache}：业务 Key 自带模块前缀（如 {@code dream:user:}），
 * 不要使用框架保留前缀 {@code prevent-duplicate:}、{@code ws:msg:}。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 固定 key
 * @CacheDoubleDelete(key = "dream:user:1001")
 * public void updateUser(UserDTO dto) { ... }
 *
 * // 2. SpEL 动态 key（推荐）
 * @CacheDoubleDelete(spel = "'dream:user:' + #dto.id", delay = 500)
 * public void updateUser(@Param("dto") UserDTO dto) { ... }
 *
 * // 3. 前缀批量删除（清空某用户相关的所有查询缓存）
 * @CacheDoubleDelete(key = "dream:user:list:", prefix = true)
 * public void batchUpdateUsers() { ... }
 *
 * // 4. 多 key 重复标注
 * @CacheDoubleDelete(spel = "'dream:user:' + #dto.id")
 * @CacheDoubleDelete(spel = "'dream:user_extra:' + #dto.id")
 * public void updateUser(@Param("dto") UserDTO dto) { ... }
 *
 * // 5. 异常时也执行第二次删除（默认 false：仅方法正常返回后删除）
 * @CacheDoubleDelete(key = "dream:order:1001", deleteOnException = true)
 * public void updateOrder(OrderDTO dto) { ... }
 * }</pre>
 *
 * @author dream
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CacheDoubleDelete.List.class)
public @interface CacheDoubleDelete {

    // ==================== Key 控制 ====================

    /**
     * 固定 Key 或 Key 前缀（配合 {@link #prefix()}）
     *
     * @return 固定 Key
     */
    String key() default "";

    /**
     * 多个固定 Key（批量删除）
     *
     * @return Key 数组
     */
    String[] keys() default {};

    /**
     * SpEL 表达式动态生成 Key
     * <p>
     * 支持引用方法参数，如 {@code "#dto.id"}、{@code "#args[0]"}、{@code "#user.name + ':' + #user.age"}
     * 优先级高于 {@link #key()} 和 {@link #keys()}
     * </p>
     *
     * @return SpEL 表达式
     */
    String spel() default "";

    /**
     * 是否作为前缀批量删除
     * <p>
     * true：使用 {@link com.dream.common.framework.cache.CachesUtils#deletePrefix(String)} 删除匹配前缀的所有 Key
     * false：精确删除单个 Key
     * </p>
     * <p>
     * 注意与 {@code QueryCache#prefix()} 无关：后者是拼进 Key 的字符串模块前缀，
     * 本属性只是"精确删 / 按前缀批量删"的模式开关。
     * </p>
     *
     * @return 是否前缀模式
     */
    boolean prefix() default false;

    // ==================== 时序控制 ====================

    /**
     * 第二次删除的延时时间
     *
     * @return 延时时长
     */
    long delay() default -1L;

    /**
     * 延时时间单位
     *
     * @return 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * 是否在方法抛出异常时仍执行第二次删除
     * <p>
     * 默认 false：方法正常返回后才延时二次删除
     * true：无论方法是否抛异常，第二次删除均执行（适用于异常时 DB 已部分写入的场景）
     * </p>
     *
     * @return 是否异常也删除
     */
    boolean deleteOnException() default false;

    /**
     * 是否执行第一次删除（方法执行前）
     * <p>
     * 默认 true。某些场景（如仅写不读）可关闭前置删除
     * </p>
     *
     * @return 是否前置删除
     */
    boolean firstDelete() default true;

    /**
     * 容器，支持 {@link Repeatable} 多注解
     *
     * @author dream
     */
    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        /**
         * @return 多个 {@link CacheDoubleDelete}
         */
        CacheDoubleDelete[] value();
    }
}
