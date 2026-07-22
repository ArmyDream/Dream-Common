package com.dream.common.advice.config;

import com.dream.common.advice.filter.TraceIdFilter;
import com.dream.common.advice.handler.GlobalExceptionHandler;
import com.dream.common.advice.handler.GlobalResponseHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 全局 Advice 自动装配配置。
 * <p>
 * 显式注册异常处理器和响应包装器，并确保在 Jackson 自动配置之后加载，
 * 以保证 {@link ObjectMapper} 可用。
 * </p>
 * <p>
 * 可通过配置项 {@code dream.advice.enabled=false} 关闭整个模块。
 * </p>
 *
 * @author dream
 */
@Slf4j
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "dream.advice", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(JacksonAutoConfiguration.class)
public class AdviceAutoConfig {

    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler globalExceptionHandler() {
        log.info("GlobalExceptionHandler 已注册");
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean(GlobalResponseHandler.class)
    public GlobalResponseHandler globalResponseHandler(ObjectProvider<ObjectMapper> objectMapperProvider) {
        // Web 环境下由 JacksonAutoConfiguration 提供 ObjectMapper；缺失时兜底创建，避免启动失败
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        log.info("GlobalResponseHandler 已注册");
        return new GlobalResponseHandler(objectMapper);
    }

    /**
     * 链路追踪过滤器（可选），在 MDC 中放入 traceId，便于日志追踪。
     * 默认启用，可通过 {@code dream.advice.trace.enabled=false} 关闭。
     */
    @Bean
    @ConditionalOnProperty(prefix = "dream.advice.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TraceIdFilter traceIdFilter() {
        log.info("TraceIdFilter 已注册");
        return new TraceIdFilter();
    }
}