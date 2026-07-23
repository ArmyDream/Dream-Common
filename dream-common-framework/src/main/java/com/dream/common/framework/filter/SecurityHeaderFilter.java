package com.dream.common.framework.filter;

import com.dream.common.core.constant.Http;
import com.dream.common.framework.config.SecurityHeaderProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
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
 * 为每个响应注入常见的安全相关头，缓解 MIME 嗅探、点击劫持、XSS 等风险：
 * <ul>
 *   <li>{@code X-Content-Type-Options}</li>
 *   <li>{@code X-Frame-Options}</li>
 *   <li>{@code X-XSS-Protection}</li>
 *   <li>{@code Strict-Transport-Security}（可选，HTTPS 场景）</li>
 * </ul>
 * 具体取值由 {@link SecurityHeaderProperties} 控制，配置为空则跳过对应头。
 * </p>
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
                                    @Nullable HttpServletResponse response,
                                    @Nullable FilterChain filterChain) throws ServletException, IOException {
        if (response != null) {
            setHeaderIfPresent(response, Http.HEADER_X_CONTENT_TYPE_OPTIONS, properties.getContentTypeOptions());
            setHeaderIfPresent(response, Http.HEADER_X_FRAME_OPTIONS, properties.getFrameOptions());
            setHeaderIfPresent(response, Http.HEADER_X_XSS_PROTECTION, properties.getXssProtection());
            setHeaderIfPresent(response, Http.HEADER_STRICT_TRANSPORT_SECURITY, properties.getStrictTransportSecurity());
        }
        if (filterChain != null) {
            filterChain.doFilter(request, response);
        }
    }

    private void setHeaderIfPresent(HttpServletResponse response, String name, String value) {
        if (StringUtils.hasText(value)) {
            response.setHeader(name, value);
        }
    }
}
