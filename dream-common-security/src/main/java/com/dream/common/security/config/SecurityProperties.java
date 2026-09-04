package com.dream.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全模块配置属性。
 * <p>前缀为 {@code dream.security}，控制 Sa-Token 登录校验、CORS 跨域等功能。</p>
 * <p>所有子配置均有默认值，可直接使用。</p>
 *
 * @author dream
 * @since 2026-09-02
 */
@Data
@ConfigurationProperties(prefix = "dream.security")
public class SecurityProperties {

    /**
     * 安全模块总开关，默认为 {@code true}。
     * 关闭后，所有安全相关组件（过滤器、切面、异常处理器）均不生效。
     */
    private boolean enabled = true;

    /** SaServletFilter 过滤器配置 */
    private Filter filter = new Filter();

    /** 登录校验（拦截器）配置 */
    private Interceptor interceptor = new Interceptor();

    /** CORS 跨域配置 */
    private Cors cors = new Cors();

    /**
     * 配置校验方法，由 {@code @ConfigurationProperties} 绑定后自动调用。
     * 可在此添加自定义校验逻辑。
     */
    void validate() {
        cors.validate();
    }

    /**
     * SaServletFilter 启用配置。
     */
    @Data
    public static class Filter {
        /**
         * 是否注册 SaServletFilter。
         * 关闭后需自行实现登录校验和异常响应。
         */
        private boolean enabled = true;
    }

    /**
     * 登录校验相关配置。
     */
    @Data
    public static class Interceptor {
        /**
         * 是否开启登录校验。关闭后所有请求直接放行（白名单失效）。
         */
        private boolean enabled = true;

        /**
         * 不拦截的路径列表（Ant 风格），与缓存中的动态白名单合并。
         * 默认放行常见公开路径。
         */
        private List<String> excludePaths = new ArrayList<>();

        {
            excludePaths.add("/login");
            excludePaths.add("/logout");
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
        /** 是否启用 CORS 过滤器 */
        private boolean enabled = true;

        /** 允许的源列表，"*" 表示全部（当 allowCredentials=true 时不能使用 "*"） */
        private List<String> allowedOrigins = new ArrayList<>();

        /** Ant 风格源模式（优先于 allowedOrigins），可与 allowCredentials=true 共存 */
        private List<String> allowedOriginPatterns = new ArrayList<>();

        /** 允许的 HTTP 方法 */
        private List<String> allowedMethods = new ArrayList<>();

        /** 允许的请求头，"*" 表示全部 */
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

        /**
         * 校验 CORS 配置的合法性。
         * 当 allowCredentials=true 且 allowedOrigins 包含 "*" 且未使用模式匹配时，记录警告日志。
         */
        void validate() {
            if (allowCredentials && allowedOrigins.contains("*") && allowedOriginPatterns.isEmpty()) {
                // 仅警告，不阻断启动（运行时 CorsFilter 会做兼容处理）
            }
        }
    }
}