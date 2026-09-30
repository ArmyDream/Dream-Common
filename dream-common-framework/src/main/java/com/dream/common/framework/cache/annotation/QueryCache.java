package com.dream.common.framework.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 查询缓存注解
 * <p>
 * 用于 Controller 层方法查询，由 {@code QueryCacheAspect} 切面拦截：
 * <ol>
 *   <li>方法执行前，按 Key 规则查询缓存</li>
 *   <li>命中缓存：直接包装为 {@code R<?>} 返回，跳过原方法执行</li>
 *   <li>未命中：执行原方法，将返回的 {@code R<?>} 的 {@code data} 写入缓存</li>
 * </ol>
 * </p>
 *
 * <h2>返回类型约束</h2>
 * <p>
 * <b>使用本注解的方法返回类型必须为 {@code R<?>}</b>（{@link com.dream.common.core.utils.R}），
 * 否则切面会跳过缓存直接执行原方法（仅记录 WARN 日志）。
 * 缓存的对象为 {@code R} 中的 {@code data} 字段。
 * </p>
 *
 * <h2>Key 生成规则</h2>
 * <ul>
 *   <li>{@link #key()}：固定 Key（常用于无参 / 固定场景）</li>
 *   <li>{@link #spel()}：SpEL 表达式动态生成 Key（推荐，如 {@code "'user:' + #id"}、{@code "'list:' + #dto.page"}）</li>
 *   <li>两者均配置时 {@link #spel()} 优先</li>
 *   <li>均未配置时使用 类名:方法名:参数Hash 作为 Key</li>
 * </ul>
 *
 * <h2>Key 命名空间约定</h2>
 * <p>
 * 缓存实现不加全局前缀，注解生成的 Key 落在同一个扁平空间；框架自带功能各自占用保留前缀：
 * {@code prevent-duplicate:}（{@code @PreventDuplicate}）、{@code ws:msg:}（WebSocket 消息缓存）。
 * 所以业务 Key 必须自带模块前缀（{@code dream:user:1001} 而非 {@code user:1001}），
 * 既避免撞框架命名空间，也避免多业务模块共库时互相覆盖。
 * </p>
 * <p>
 * 与 {@link CacheDoubleDelete} 配对时注意：删除端没有 {@link #prefix()} 概念、原样使用注解里写出的 Key，
 * 两侧最终 Key 必须逐字一致（{@code @QueryCache(prefix="dream:user", spel="'1001'")}
 * 对应 {@code @CacheDoubleDelete(spel="'dream:user:1001'")}），否则双删删不掉查询缓存。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 固定 Key（适合无参查询）
 * @QueryCache(key = "dream:dict:all", expire = 3600)
 * @GetMapping("/dict/all")
 * public R<List<Dict>> allDict() { ... }
 *
 * // 2. SpEL 动态 Key（推荐）
 * @QueryCache(spel = "'dream:user:' + #id", expire = 600)
 * @GetMapping("/user/{id}")
 * public R<User> getUser(@PathVariable Long id) { ... }
 *
 * // 3. 按需条件缓存（ spel 为 true 时才走缓存）
 * @QueryCache(prefix = "dream:user", spel = "'list:' + #dto.page", expire = 300, condition = "#dto.page <= 10")
 * @PostMapping("/list")
 * public R<Page<User>> list(@RequestBody UserDTO dto) { ... }
 * }</pre>
 *
 * @author dream
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryCache {

    /**
     * 固定 Key（与 {@link #spel()} 互斥，{@link #spel()} 优先）
     *
     * @return 固定 Key
     */
    String key() default "";

    /**
     * SpEL 表达式动态生成 Key
     * <p>
     * 支持引用方法参数，如 {@code "#id"}、{@code "#dto.page"}、{@code "'user:' + #id"}
     * </p>
     *
     * @return SpEL 表达式
     */
    String spel() default "";

    /**
     * 缓存 Key 前缀（自动拼接到生成的 Key 前，以冒号分隔）
     * <p>
     * 默认为空。建议配置项目名 / 业务模块，避免冲突，如 {@code "dream:user"}
     * </p>
     *
     * @return Key 前缀
     */
    String prefix() default "";

    /**
     * 过期时间（配合 {@link #timeUnit()}）
     * <p>
     * 默认 -1 表示使用 {@code dream.cache.default-expire} 配置值
     * </p>
     *
     * @return 过期时间
     */
    long expire() default -1L;

    /**
     * 时间单位，默认秒
     *
     * @return 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * SpEL 条件表达式（仅当求值为 true 时才走缓存）
     * <p>
     * 例如 {@code "#dto.page <= 10"} 表示只对前 10 页数据启用缓存
     * </p>
     *
     * @return 条件表达式
     */
    String condition() default "";

    /**
     * 是否缓存 null 值
     * <p>
     * 默认 false：方法返回 data 为 null 时不写入缓存（避免缓存穿透时被 null 占位）
     * true：将 null 也写入缓存（配合短过期时间用于防穿透）
     * </p>
     *
     * @return 是否缓存 null
     */
    boolean cacheNull() default false;

    /**
     * 防穿透空值过期时间（秒），仅 {@link #cacheNull()} 为 true 时生效
     * <p>
     * 默认 60 秒，避免长期占用缓存
     * </p>
     *
     * @return 空值缓存过期秒数
     */
    long nullExpireSeconds() default 60L;
}
