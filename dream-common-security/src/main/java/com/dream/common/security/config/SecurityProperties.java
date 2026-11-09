package com.dream.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 安全模块配置属性。
 * <p>
 * 前缀 {@code dream.security}，控制 Sa-Token 登录校验、CORS 跨域、过滤器开关。
 * 所有子配置均提供合理默认值，开箱即用。
 * </p>
 *
 * <h2>配置项一览</h2>
 * <ul>
 *   <li>{@code dream.security.enabled} —— 模块总开关（默认 true）</li>
 *   <li>{@code dream.security.filter.enabled} —— 是否注册 SaServletFilter</li>
 *   <li>{@code dream.security.filter.check-login} —— 是否执行登录校验</li>
 *   <li>{@code dream.security.filter.exclude-paths} —— 静态白名单（与缓存动态合并）</li>
 *   <li>{@code dream.security.cors.*} —— CORS 跨域配置</li>
 * </ul>
 *
 * <h2>配置示例</h2>
 * <pre>{@code
 * dream:
 *   security:
 *     enabled: true                      # 模块总开关
 *     filter:
 *       enabled: true                    # 是否注册 SaServletFilter
 *       check-login: true                # 是否执行登录校验
 *       exclude-paths:                   # 静态白名单（Ant 风格）
 *         - /sys/login
 *         - /sys/logout
 *         - /captcha/**
 *     cors:
 *       enabled: true                    # 是否注册独立 CorsFilter
 *       allowed-origin-patterns:         # Ant 风格源匹配（优先于 allowed-origins）
 *         - https://*.example.com
 *       allow-credentials: true          # 允许携带 Cookie（与通配源共存需用 patterns）
 *       max-age: 1800                    # 预检缓存秒数
 * }</pre>
 *
 * @author dream
 * @since 2026-09-02
 */
@Data
@ConfigurationProperties(prefix = "dream.security")
public class SecurityProperties {

    /**
     * 安全模块总开关，默认 {@code true}。
     * <p>
     * 关闭后，所有安全相关组件（CorsFilter、SaServletFilter、PermissionAspect、
     * SaTokenExceptionHandler、StpInterfaceImpl）均不生效。
     * </p>
     */
    private boolean enabled = true;

    /** SaServletFilter 鉴权过滤器配置 */
    private Filter filter = new Filter();

    /** CORS 跨域配置 */
    private Cors cors = new Cors();

    /**
     * SaServletFilter 鉴权过滤器配置。
     * <p>
     * 本框架只采用 SaServletFilter 方案（过滤器链统一鉴权），
     * 不引入 SaInterceptor，避免重复认证与性能开销。
     * </p>
     *
     * <h2>配置示例</h2>
     * <pre>{@code
     * dream:
     *   security:
     *     filter:
     *       enabled: true          # 是否注册 SaServletFilter
     *       check-login: true      # 是否执行登录校验
     *       exclude-paths:         # 静态白名单，另可与缓存动态白名单合并（热更新）
     *         - /sys/login
     *         - /captcha/**
     * }</pre>
     */
    @Data
    public static class Filter {
        /**
         * 是否注册 SaServletFilter。
         * 关闭后需自行实现登录校验和异常响应，PermissionAspect 仍可生效。
         */
        private boolean enabled = true;

        /**
         * 是否在鉴权阶段执行登录校验。
         * <p>
         * 关闭后所有请求直接放行（白名单失效），仅适用于开放 API 或纯内网场景。
         * </p>
         */
        private boolean checkLogin = true;

        /**
         * 不拦截的静态白名单路径（Ant 风格），与缓存中的动态白名单逐请求合并。
         * <p>
         * 默认仅放行系统登录/登出、认证、验证码路径。
         * </p>
         * <p>
         * <b>注意：不再默认放行 {@code /actuator/**}</b>。Actuator 的 {@code /env}、
         * {@code /heapdump} 等端点会暴露配置与内存中的凭据，需要监控时由业务侧显式加入白名单
         * 并配合网络层限制，例如：
         * </p>
         * <pre>{@code
         * dream:
         *   security:
         *     filter:
         *       exclude-paths:
         *         - /actuator/health    # 只放行健康检查
         * }</pre>
         */
        private List<String> excludePaths = new ArrayList<>(Arrays.asList(
                "/sys/login", "/sys/logout", "/auth/**", "/captcha/**"));
    }

    /**
     * CORS 跨域配置。
     *
     * <h2>配置示例</h2>
     * <pre>{@code
     * dream:
     *   security:
     *     cors:
     *       enabled: true                  # 是否注册独立 CorsFilter
     *       allowed-origins:               # 精确源列表（与 allow-credentials=true 不兼容 "*"）
     *         - https://app.example.com
     *       allowed-origin-patterns:       # Ant 风格源模式，优先于 allowed-origins
     *         - https://*.example.com
     *       allowed-methods: [GET, POST]   # 允许的 HTTP 方法
     *       allowed-headers: "*"           # 允许的请求头
     *       exposed-headers: [X-Request-Id]  # 暴露给前端 JS 的响应头
     *       allow-credentials: true        # 允许携带 Cookie
     *       max-age: 3600                  # 预检请求缓存秒数
     * }</pre>
     */
    @Data
    public static class Cors {
        /** 是否启用独立 CorsFilter */
        private boolean enabled = true;

        /**
         * 允许的源列表，{@code "*"} 表示全部。
         * <p>
         * 当 {@code allowCredentials=true} 时不能使用 {@code "*"}（浏览器会拒绝该组合），
         * 需指定具体源或改用 {@link #allowedOriginPatterns}；此时 {@code "*"} 会被忽略并告警。
         * </p>
         * <p>
         * <b>默认为空</b>：不放开任何跨域源（同源请求不受影响）。需要跨域时由业务侧显式配置，
         * 避免"框架默认全开"导致带凭证请求被任意站点调用。
         * </p>
         */
        private List<String> allowedOrigins = new ArrayList<>();

        /** Ant 风格源模式（优先于 allowedOrigins），可与 allowCredentials=true 共存 */
        private List<String> allowedOriginPatterns = new ArrayList<>();

        /** 允许的 HTTP 方法 */
        private List<String> allowedMethods = new ArrayList<>(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        /** 允许的请求头，{@code "*"} 表示全部 */
        private List<String> allowedHeaders = new ArrayList<>(Arrays.asList("*"));

        /** 暴露给前端 JavaScript 可读取的响应头 */
        private List<String> exposedHeaders = new ArrayList<>();

        /** 是否允许携带 Cookie 等凭证 */
        private boolean allowCredentials = false;

        /** 预检请求缓存时间（秒） */
        private long maxAge = 3600L;
    }
}
