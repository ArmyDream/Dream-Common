package com.dream.common.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 防止重复提交注解
 * <p>
 * 基于分布式锁/Redis 实现，支持多种 Key 生成策略：
 * <ul>
 *   <li>默认：用户 ID + 请求 URL</li>
 *   <li>{@link #field()}：指定请求参数中的字段值</li>
 *   <li>{@link #key()}：自定义固定前缀</li>
 *   <li>{@link #spel()}：SpEL 表达式动态生成</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 基础用法：3 秒内防重复
 * @PreventDuplicate
 * @PostMapping("/save")
 * public R<?> save(@RequestBody UserDTO dto) { ... }
 *
 * // 指定字段：同一手机号 5 秒内防重复
 * @PreventDuplicate(field = {"phone"}, expire = 5)
 * @PostMapping("/register")
 * public R<?> register(@RequestBody RegisterDTO dto) { ... }
 *
 * // 自定义 key 前缀 + 包含用户 ID
 * @PreventDuplicate(key = "order:submit", includeUserId = true, expire = 10)
 * @PostMapping("/submit")
 * public R<?> submit(@RequestBody OrderDTO dto) { ... }
 *
 * // SpEL 表达式动态生成 key
 * @PreventDuplicate(spel = "#dto.id + ':' + #dto.type", expire = 5)
 * @PostMapping("/update")
 * public R<?> update(@RequestBody @Param("dto") UpdateDTO dto) { ... }
 *
 * // 静默丢弃（不抛异常，返回 null）
 * @PreventDuplicate(strategy = Strategy.SILENT)
 * @PostMapping("/click")
 * public R<?> click() { ... }
 * }</pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreventDuplicate {

    // ==================== 时间控制 ====================

    /**
     * 防重复提交时间（配合 {@link #timeUnit()} 使用，默认 3 秒）
     */
    int expire() default 3;

    /**
     * 时间单位，默认秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    // ==================== Key 生成策略 ====================

    /**
     * 自定义 Key 前缀
     * <p>
     * 留空时使用方法全限定名（类名.方法名）
     * <p>
     * 建议格式："业务模块:操作"，如 "order:submit"、"user:register"
     */
    String key() default "";

    /**
     * 从请求参数中提取的字段名数组（用于拼接 Key）
     * <p>
     * 支持嵌套属性，如 "user.phone"、"order.id"
     * <p>
     * 留空时不提取字段值
     */
    String[] field() default {};

    /**
     * SpEL 表达式（动态生成 Key）
     * <p>
     * 支持引用方法参数，如 "#dto.id"、"#args[0].name"
     * <p>
     * 优先级高于 {@link #key()} 和 {@link #field()}
     * <p>
     * 留空时不使用 SpEL
     */
    String spel() default "";

    /**
     * 是否将当前用户 ID 纳入 Key
     * <p>
     * true：同一用户在限定时间内防重复（推荐）
     * <p>
     * false：所有用户共享防重复（全局限制）
     * <p>
     * 默认值：true
     */
    boolean includeUserId() default true;

    /**
     * 是否将请求 IP 纳入 Key
     * <p>
     * 适用于未登录场景（如注册、短信发送）
     * <p>
     * 默认值：false
     */
    boolean includeIp() default false;

    // ==================== 拦截行为 ====================

    /**
     * 拦截策略
     * <p>
     * {@link Strategy#ERROR}：抛出业务异常，返回 {@link #message()} 提示（默认）
     * <p>
     * {@link Strategy#SILENT}：静默丢弃，返回 null（不抛异常）
     */
    Strategy strategy() default Strategy.ERROR;

    /**
     * 防重复提交提示信息（strategy = ERROR 时生效）
     */
    String message() default "请勿重复提交！";

    /**
     * 拦截策略枚举
     *
     * @author dream
     */
    enum Strategy {
        /**
         * 抛出异常（默认）
         */
        ERROR,
        /**
         * 静默丢弃
         */
        SILENT
    }
}
