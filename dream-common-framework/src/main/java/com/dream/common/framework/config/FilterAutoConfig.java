package com.dream.common.framework.config;

import com.dream.common.framework.filter.TraceIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 过滤器自动装配配置。
 * <p>
 * 注册框架级过滤器（如链路追踪），通过 {@link FilterRegistrationBean} 控制
 * URL 匹配和执行顺序。
 * </p>
 *
 * @author dream
 */
@Slf4j
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
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
}
