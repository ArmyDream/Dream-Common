package com.dream.common.core.constant;

/**
 * 全库统一的执行优先级常量表。
 * <p>
 * 值越小越先执行（越靠外层）。所有过滤器（Filter）、切面（Aspect）、
 * ControllerAdvice 的 {@code @Order} 一律引用本类，禁止再使用
 * {@code Ordered.HIGHEST_PRECEDENCE} 或魔法数字，避免多模块次序冲突。
 * </p>
 * <p>
 * 过滤器链规划（请求方向由外到内）：
 * <pre>
 *   TraceIdFilter(-200) → CorsFilter(-150) → SaServletFilter(-140)
 *     → SecurityHeaderFilter(-130) → AccessLogFilter(-120) → 业务
 * </pre>
 * AccessLog 位于鉴权之后，避免 OPTIONS 预检与未鉴权请求刷爆访问日志；
 * TraceId 预留 -200 至 {@link #MIN_VALUE} 区间给下游 APM（SkyWalking 等）。
 * </p>
 *
 * @author dream
 * @since 1.0.0
 */
public final class OrderValue {

    private OrderValue() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    /**
     * 最小，最先加载
     */
    public static final int MIN_VALUE = Integer.MIN_VALUE;

    /**
     * 最大，最后加载
     */
    public static final int MAX_VALUE = Integer.MAX_VALUE;

    /**
     * RestControllerAdvice、ExceptionHandler
     */
    public static final int ADVICE = Integer.MAX_VALUE;

    /**
     * 链路追踪过滤器（为下游 APM 预留更靠前的区间 -200 ~ MIN_VALUE）
     */
    public static final int TRACE_FILTER = -200;

    /**
     * CORS 过滤器
     */
    public static final int CORS_FILTER = -150;

    /**
     * Sa-Token 鉴权过滤器
     */
    public static final int SA_TOKEN_FILTER = -140;

    /**
     * 安全响应头过滤器
     */
    public static final int SECURITY_HEADER_FILTER = -130;

    /**
     * 访问日志过滤器（位于鉴权之后，只记录已通过鉴权入口的请求）
     */
    public static final int ACCESS_LOG_FILTER = -120;

    /**
     * 切面统一规划：权限校验
     */
    public static final int ASPECT_PERMISSION = -100;

    /**
     * 切面统一规划：防重复提交
     */
    public static final int ASPECT_PREVENT_DUPLICATE = -90;

    /**
     * 切面统一规划：操作日志
     */
    public static final int ASPECT_OPERATION_LOG = -80;

    /**
     * 切面统一规划：查询/双删缓存
     */
    public static final int ASPECT_CACHE = 0;
}
