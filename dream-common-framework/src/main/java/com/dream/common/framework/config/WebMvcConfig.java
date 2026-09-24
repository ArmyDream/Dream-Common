package com.dream.common.framework.config;

import com.dream.common.core.constant.OrderValue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC 全局 Jackson 配置。
 * <p>
 * 将自定义 {@link JacksonObjectMapper} 注册为默认消息转换器，
 * 确保所有 HTTP 请求 / 响应使用统一的 JSON 序列化规则（Long→String 精度保护等）。
 * </p>
 * <p>
 * 通过 {@code dream.framework.jackson.enabled=false} 可整体关闭，
 * 关闭后完全交还给 Spring Boot 原生 Jackson 自动配置。
 * </p>
 * <p>
 * 注意：实现 {@link WebMvcConfigurer} 接口而非继承 {@code WebMvcConfigurationSupport}，
 * 后者会完全禁用 Spring Boot 的自动配置（{@code @EnableWebMvc} 效果），
 * 导致内容协商、静态资源、类型转换等默认能力丢失。
 * </p>
 *
 * @author dream
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "dream.framework.jackson", name = "enabled", havingValue = "true", matchIfMissing = true)
@Order(OrderValue.ADVICE)
public class WebMvcConfig implements WebMvcConfigurer {

    private final JacksonObjectMapper jacksonObjectMapper = new JacksonObjectMapper();

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 创建自定义转换器并插入到最前面，优先于默认转换器
        MappingJackson2HttpMessageConverter messageConverter =
                new MappingJackson2HttpMessageConverter(jacksonObjectMapper);
        converters.add(0, messageConverter);
    }
}
