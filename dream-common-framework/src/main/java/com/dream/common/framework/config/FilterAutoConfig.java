package com.dream.common.framework.config;

import com.dream.common.framework.api.AccessLogHandler;
import com.dream.common.framework.filter.AccessLogFilter;
import com.dream.common.framework.filter.SecurityHeaderFilter;
import com.dream.common.framework.filter.TraceIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 过滤器自动装配配置。
 * <p>
 * 注册框架级过滤器，通过 {@link FilterRegistrationBean} 控制 URL 匹配和执行顺序：
 * <ul>
 *   <li>{@link SecurityHeaderFilter} 安全响应头（较高优先级，尽早注入）</li>
 *   <li>{@link TraceIdFilter} 链路追踪（最高优先级，确保 traceId 全程可用）</li>
 *   <li>{@link AccessLogFilter} 访问日志（位于 TraceIdFilter 之后，以获取 traceId）</li>
 * </ul>
 * </p>
 *
 * @author dream
 */
@Slf4j
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(SecurityHeaderProperties.class)
public class FilterAutoConfig {

    /**
     * 链路追踪过滤器，在 MDC 中放入 traceId，便于日志追踪。
     * <p>
     * 默认启用，可通过 {@code dream.filter.trace.enabled=false} 关闭。
     * 执行顺序为最高优先级，确保后续过滤器和业务流程均可获取 traceId。
     * </p>
     */
    @Bean
    @ConditionalOnProperty(prefix = "dream.filter.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdFilter());
        registration.addUrlPatterns("/*");
        registration.setName("traceIdFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        log.info("TraceIdFilter 已注册");
        return registration;
    }

    /**
     * 安全响应头过滤器，为响应注入常见安全相关头。
     * <p>
     * 默认启用，可通过 {@code dream.filter.security-header.enabled=false} 关闭。
     * </p>
     */
    @Bean
    @ConditionalOnProperty(prefix = "dream.filter.security-header", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<SecurityHeaderFilter> securityHeaderFilterRegistration(SecurityHeaderProperties properties) {
        FilterRegistrationBean<SecurityHeaderFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityHeaderFilter(properties));
        registration.addUrlPatterns("/*");
        registration.setName("securityHeaderFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        log.info("SecurityHeaderFilter 已注册");
        return registration;
    }

    /**
     * 请求访问日志过滤器，采集 method/uri/ip/状态码/耗时/traceId。
     * <p>
     * 默认启用，可通过 {@code dream.filter.access-log.enabled=false} 关闭。
     * 执行顺序位于 {@link TraceIdFilter} 之后，以保证 MDC 中的 traceId 可用。
     * 若容器中存在 {@link AccessLogHandler} 实现，则交由业务方自定义处理。
     * </p>
     */
    @Bean
    @ConditionalOnProperty(prefix = "dream.filter.access-log", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<AccessLogFilter> accessLogFilterRegistration(ObjectProvider<AccessLogHandler> accessLogHandlerProvider) {
        FilterRegistrationBean<AccessLogFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AccessLogFilter(accessLogHandlerProvider.getIfAvailable()));
        registration.addUrlPatterns("/*");
        registration.setName("accessLogFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        log.info("AccessLogFilter 已注册");
        return registration;
    }
}
