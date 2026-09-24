package com.dream.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
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
 *   <li>{@code dream.security.interceptor.enabled} —— 是否在 setAuth 阶段执行登录校验</li>
 *   <li>{@code dream.security.interceptor.exclude-paths} —— 静态白名单（与缓存动态合并）</li>
 *   <li>{@code dream.security.cors.*} —— CORS 跨域配置</li>
 * </ul>
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

    /** SaServletFilter 过滤器配置 */
    private Filter filter = new Filter();

    /** 登录校验（SaServletFilter.setAuth 阶段）配置 */
    private Interceptor interceptor = new Interceptor();

    /** CORS 跨域配置 */
    private Cors cors = new Cors();

    /**
     * SaServletFilter 启用配置。
     */
    @Data
    public static class Filter {
        /**
         * 是否注册 SaServletFilter。
         * 关闭后需自行实现登录校验和异常响应，PermissionAspect 仍可生效。
         */
        private boolean enabled = true;
    }

    /**
     * 登录校验相关配置。
     */
    @Data
    public static class Interceptor {
        /**
         * 是否在 SaServletFilter.setAuth 阶段执行登录校验。
         * <p>
         * 关闭后所有请求直接放行（白名单失效），仅适用于开放 API 或纯内网场景。
         * </p>
         */
        private boolean enabled = true;

        /**
         * 不拦截的静态白名单路径（Ant 风格），与缓存中的动态白名单逐请求合并。
         * <p>
         * 默认放行系统登录/登出、验证码、监控端点等公开路径。
         * </p>
         */
        private List<String> excludePaths = new ArrayList<>();

        {
            excludePaths.add("/sys/login");
            excludePaths.add("/sys/logout");
            excludePaths.add("/auth/**");
            excludePaths.add("/captcha/**");
            excludePaths.add("/actuator/**");
        }
    }

    /**
     * CORS 跨域配置。
     */
    @Data
    public static class Cors {
        /** 是否启用独立 CorsFilter */
        private boolean enabled = true;

        /**
         * 允许的源列表，{@code "*"} 表示全部。
         * <p>
         * 当 {@code allowCredentials=true} 时不能使用 {@code "*"}，
         * 需指定具体源或改用 {@link #allowedOriginPatterns}。
         * </p>
         */
        private List<String> allowedOrigins = new ArrayList<>();

        /** Ant 风格源模式（优先于 allowedOrigins），可与 allowCredentials=true 共存 */
        private List<String> allowedOriginPatterns = new ArrayList<>();

        /** 允许的 HTTP 方法 */
        private List<String> allowedMethods = new ArrayList<>();

        /** 允许的请求头，{@code "*"} 表示全部 */
        private List<String> allowedHeaders = new ArrayList<>();

        /** 暴露给前端 JavaScript 可读取的响应头 */
        private List<String> exposedHeaders = new ArrayList<>();

        /** 是否允许携带 Cookie 等凭证 */
        private boolean allowCredentials = false;

        /** 预检请求缓存时间（秒） */
        private long maxAge = 3600L;

        {
            allowedOrigins.add("*");
            allowedMethods.add("GET");
            allowedMethods.add("POST");
            allowedMethods.add("PUT");
            allowedMethods.add("DELETE");
            allowedMethods.add("OPTIONS");
            allowedMethods.add("PATCH");
            allowedHeaders.add("*");
        }
    }
}
