package com.dream.common.framework.filter;

import com.dream.common.core.constant.Http;
import com.dream.common.framework.config.SecurityHeaderProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 安全响应头过滤器。
 * <p>
 * 为每个响应注入安全相关头，缓解 MIME 嗅探、点击劫持、XSS、CSRF 等风险：
 * </p>
 *
 * <h2>注入的安全头</h2>
 * <table border="1">
 * <tr><th>响应头</th><th>默认值</th><th>说明</th></tr>
 * <tr><td>X-Content-Type-Options</td><td>nosniff</td><td>禁止 MIME 嗅探</td></tr>
 * <tr><td>X-Frame-Options</td><td>SAMEORIGIN</td><td>防点击劫持</td></tr>
 * <tr><td>X-XSS-Protection</td><td>1; mode=block</td><td>XSS 过滤器</td></tr>
 * <tr><td>Strict-Transport-Security</td><td>（空）</td><td>HSTS，HTTPS 场景配置</td></tr>
 * <tr><td>Referrer-Policy</td><td>（空）</td><td>Referer 发送策略</td></tr>
 * <tr><td>Content-Security-Policy</td><td>（空）</td><td>资源加载白名单</td></tr>
 * <tr><td>X-Permitted-Cross-Domain-Policies</td><td>master-only</td><td>跨域策略文件</td></tr>
 * <tr><td>X-Download-Options</td><td>noopen</td><td>禁止 IE 自动打开下载</td></tr>
 * <tr><td>Cross-Origin-Embedder-Policy</td><td>（空）</td><td>COEP 跨域隔离</td></tr>
 * <tr><td>Cross-Origin-Opener-Policy</td><td>（空）</td><td>COOP 顶层窗口隔离</td></tr>
 * <tr><td>Cross-Origin-Resource-Policy</td><td>cross-origin</td><td>CORP 资源加载</td></tr>
 * <tr><td>Permissions-Policy</td><td>（空）</td><td>浏览器功能权限</td></tr>
 * <tr><td>Clear-Site-Data</td><td>（空）</td><td>清除浏览器数据（仅登出场景）</td></tr>
 * </table>
 *
 * <h2>缓存控制</h2>
 * <p>
 * 当 {@code cacheControl} 配置为 {@code no-cache} 时，同时输出：
 * {@code Cache-Control: no-cache} + {@code Expires: -1} + {@code Pragma: no-cache}。
 * </p>
 *
 * <p>具体取值由 {@link SecurityHeaderProperties} 控制，配置为空则跳过对应头。</p>
 *
 * @author dream
 */
@Slf4j
public class SecurityHeaderFilter extends OncePerRequestFilter {

    private final SecurityHeaderProperties properties;

    public SecurityHeaderFilter(SecurityHeaderProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        injectSecurityHeaders(response);
        filterChain.doFilter(request, response);
    }

    /**
     * 注入全部安全响应头
     *
     * @param response HTTP 响应
     */
    private void injectSecurityHeaders(HttpServletResponse response) {
        // ---------- 基础安全头 ----------
        setHeaderIfPresent(response, Http.HEADER_X_CONTENT_TYPE_OPTIONS, properties.getContentTypeOptions());
        setHeaderIfPresent(response, Http.HEADER_X_FRAME_OPTIONS, properties.getFrameOptions());
        setHeaderIfPresent(response, Http.HEADER_X_XSS_PROTECTION, properties.getXssProtection());
        setHeaderIfPresent(response, Http.HEADER_STRICT_TRANSPORT_SECURITY, properties.getStrictTransportSecurity());

        // ---------- 扩展安全头 ----------
        setHeaderIfPresent(response, Http.HEADER_REFERRER_POLICY, properties.getReferrerPolicy());
        setHeaderIfPresent(response, Http.HEADER_CONTENT_SECURITY_POLICY, properties.getContentSecurityPolicy());
        setHeaderIfPresent(response, Http.HEADER_X_PERMITTED_CROSS_DOMAIN_POLICIES, properties.getXPermittedCrossDomainPolicies());
        setHeaderIfPresent(response, Http.HEADER_X_DOWNLOAD_OPTIONS, properties.getXDownloadOptions());

        // ---------- Cross-Origin 隔离策略 ----------
        setHeaderIfPresent(response, Http.HEADER_CROSS_ORIGIN_EMBEDDER_POLICY, properties.getCrossOriginEmbedderPolicy());
        setHeaderIfPresent(response, Http.HEADER_CROSS_ORIGIN_OPENER_POLICY, properties.getCrossOriginOpenerPolicy());
        setHeaderIfPresent(response, Http.HEADER_CROSS_ORIGIN_RESOURCE_POLICY, properties.getCrossOriginResourcePolicy());

        // ---------- 隐私与权限 ----------
        setHeaderIfPresent(response, Http.HEADER_PERMISSIONS_POLICY, properties.getPermissionsPolicy());
        setHeaderIfPresent(response, Http.HEADER_CLEAR_SITE_DATA, properties.getClearSiteData());

        // ---------- 缓存控制 ----------
        if (StringUtils.hasText(properties.getCacheControl())) {
            response.setHeader(Http.HEADER_CACHE_CONTROL, properties.getCacheControl());
            response.setHeader(Http.HEADER_EXPIRES, "-1");
            response.setHeader(Http.HEADER_PRAGMA, "no-cache");
        }
    }

    /**
     * 当配置值非空时设置响应头
     *
     * @param response HTTP 响应
     * @param name     头名称
     * @param value    头值；为空时跳过
     */
    private void setHeaderIfPresent(HttpServletResponse response, String name, String value) {
        if (StringUtils.hasText(value)) {
            response.setHeader(name, value);
        }
    }
}
