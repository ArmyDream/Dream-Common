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
 * @author dream
 */
@Data
@ConfigurationProperties(prefix = "dream.filter.security-header")
public class SecurityHeaderProperties {

    /**
     * 是否启用安全响应头过滤器，默认启用
     */
    private boolean enabled = true;

    /**
     * X-Content-Type-Options，默认 nosniff（禁止 MIME 嗅探）
     */
    private String contentTypeOptions = "nosniff";

    /**
     * X-Frame-Options，默认 DENY（禁止被 iframe 嵌套，防点击劫持）
     */
    private String frameOptions = "DENY";

    /**
     * X-XSS-Protection，默认 1; mode=block
     */
    private String xssProtection = "1; mode=block";

    /**
     * Strict-Transport-Security，默认空（不输出）；HTTPS 场景可配置如 max-age=31536000
     */
    private String strictTransportSecurity = "";
}
