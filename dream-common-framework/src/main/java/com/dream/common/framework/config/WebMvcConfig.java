package com.dream.common.framework.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC 全局配置
 * <p>
 * 将自定义 {@link JacksonObjectMapper} 注册为默认消息转换器，
 * 确保所有 HTTP 请求 / 响应使用统一的 JSON 序列化规则。
 * </p>
 * <p>
 * 注意：实现 {@link WebMvcConfigurer} 接口而非继承 {@code WebMvcConfigurationSupport}，
 * 后者会完全禁用 Spring Boot 的自动配置（{@code @EnableWebMvc} 效果），
 * 导致内容协商、静态资源、类型转换等默认能力丢失。
 * </p>
 *
 * @author dream
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JacksonObjectMapper jacksonObjectMapper = new JacksonObjectMapper();

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 创建自定义消息转换器并插入到最前面，优先于默认转换器
        MappingJackson2HttpMessageConverter messageConverter =
                new MappingJackson2HttpMessageConverter(jacksonObjectMapper);
        converters.add(0, messageConverter);
    }

    /**
     * 暴露 ObjectMapper 供其他组件复用
     * <p>
     * 例如：RestTemplate、WebSocket、定时任务等需要 JSON 序列化的场景
     * </p>
     *
     * @return 全局统一的 JacksonObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return jacksonObjectMapper;
    }
}