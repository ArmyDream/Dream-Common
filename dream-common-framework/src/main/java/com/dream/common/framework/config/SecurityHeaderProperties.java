package com.dream.common.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全响应头配置属性。
 * <p>
 * 前缀 {@code dream.filter.security-header}，用于配置注入到响应中的安全相关头。
 * 值为空字符串时表示不输出对应的头。
 * </p>
 *
 * <h2>application.yml 示例</h2>
 * <pre>{@code
 * dream:
 *   filter:
 *     security-header:
 *       enabled: true
 *       content-type-options: nosniff
 *       frame-options: DENY
 *       xss-protection: "1; mode=block"
 *       strict-transport-security: "max-age=31536000; includeSubDomains; preload"
 *       referrer-policy: strict-origin-when-cross-origin
 *       content-security-policy: "default-src 'self'"
 *       x-permitted-cross-domain-policies: master-only
 *       x-download-options: noopen
 *       cross-origin-embedder-policy: require-corp
 *       cross-origin-opener-policy: same-origin
 *       cross-origin-resource-policy: cross-origin
 *       permissions-policy: "geolocation=(self),camera=(),microphone=()"
 *       clear-site-data: ""           # 默认空（不输出）；登出场景可配 "*"
 *       cache-control: no-cache       # 默认空（不输出）
 * }</pre>
 *
 * <p>
 * Cookie 的 Secure / HttpOnly / SameSite 属性由 Servlet 容器与框架自身管理，
 * 请使用 Spring Boot 标准配置项 {@code server.servlet.session.cookie.*}
 * 以及所用鉴权框架（如 sa-token）的 token 配置，本类不提供重复开关。
 * </p>
 *
 * @author dream
 */
@Data
@ConfigurationProperties(prefix = "dream.filter.security-header")
public class SecurityHeaderProperties {

    /**
     * 是否启用安全响应头过滤器，默认启用
     */
    private boolean enabled = true;

    // ---------- 基础安全头 ----------

    /**
     * X-Content-Type-Options，默认 nosniff（禁止 MIME 嗅探）
     */
    private String contentTypeOptions = "nosniff";

    /**
     * X-Frame-Options，默认 SAMEORIGIN（仅同源可嵌套 iframe，防点击劫持）。
     * <p>
     * 可选值：DENY（完全禁止）/ SAMEORIGIN（仅同源）/ ALLOW-FROM uri
     * </p>
     */
    private String frameOptions = "SAMEORIGIN";

    /**
     * X-XSS-Protection，默认 1; mode=block
     */
    private String xssProtection = "1; mode=block";

    /**
     * Strict-Transport-Security，默认空（不输出）；HTTPS 场景可配置如 max-age=31536000; includeSubDomains; preload
     */
    private String strictTransportSecurity = "";

    // ---------- 扩展安全头 ----------

    /**
     * Referrer-Policy，控制 Referer 头的发送策略。
     * <p>
     * 可选值：no-referrer / no-referrer-when-downgrade / same-origin /
     * origin / strict-origin / origin-when-cross-origin / strict-origin-when-cross-origin / unsafe-url
     * </p>
     * 默认空（不输出）。
     */
    private String referrerPolicy = "";

    /**
     * Content-Security-Policy (CSP)，限制资源加载来源。
     * <p>
     * 示例：default-src 'self'; script-src 'self' 'unsafe-inline'
     * </p>
     * 默认空（不输出）。
     */
    private String contentSecurityPolicy = "";

    /**
     * X-Permitted-Cross-Domain-Policies，控制跨域策略文件（Flash/PDF）。
     * <p>
     * 可选值：none / master-only / by-content-type / by-ftp-filename / all
     * </p>
     * 默认 master-only。
     */
    private String xPermittedCrossDomainPolicies = "master-only";

    /**
     * X-Download-Options，禁止 IE 自动打开下载文件。默认 noopen。
     */
    private String xDownloadOptions = "noopen";

    // ---------- Cross-Origin 隔离策略 ----------

    /**
     * Cross-Origin-Embedder-Policy (COEP)。
     * <p>
     * 可选值：unsafe-none / require-corp。
     * 设为 require-corp 可启用跨域隔离，配合 SharedArrayBuffer 等高级 API。
     * </p>
     * 默认空（不输出）。
     */
    private String crossOriginEmbedderPolicy = "";

    /**
     * Cross-Origin-Opener-Policy (COOP)。
     * <p>
     * 可选值：unsafe-none / same-origin-allow-popups / same-origin。
     * </p>
     * 默认空（不输出）。
     */
    private String crossOriginOpenerPolicy = "";

    /**
     * Cross-Origin-Resource-Policy (CORP)。
     * <p>
     * 可选值：same-site / same-origin / cross-origin。
     * </p>
     * 默认 cross-origin（允许跨域资源加载）。
     */
    private String crossOriginResourcePolicy = "cross-origin";

    // ---------- 隐私与权限 ----------

    /**
     * Permissions-Policy，控制浏览器功能（摄像头、麦克风、地理位置等）的使用权限。
     * <p>
     * 示例：geolocation=(self),camera=(),microphone=()
     * </p>
     * 默认空（不输出）。
     */
    private String permissionsPolicy = "";

    /**
     * Clear-Site-Data，清除浏览器缓存/Cookie/存储。
     * <p>
     * 可选值：cache / cookies / storage / executionContexts / * 或组合。
     * <b>警告：</b>此头会在每次响应时清除浏览器数据，通常仅在登出接口使用，而非全局注入。
     * 默认空（不输出）。
     * </p>
     */
    private String clearSiteData = "";

    // ---------- 缓存控制 ----------

    /**
     * Cache-Control，禁用缓存。
     * <p>
     * 设为 no-cache 时输出 Cache-Control: no-cache + Expires: -1 + Pragma: no-cache 三件套。
     * </p>
     * 默认空（不输出，由 Spring MVC 默认行为决定）。
     */
    private String cacheControl = "";
}
