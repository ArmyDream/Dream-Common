package com.dream.common.advice.handler;

import com.dream.common.advice.annotation.IgnoreResponse;
import com.dream.common.core.enums.ResponseCode;
import com.dream.common.core.utils.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletResponse;

/**
 * 全局统一响应包装处理器。
 * <p>
 * 将 Controller 返回值自动包装为 {@link R} 格式。
 * 以下情况<strong>不包装</strong>：
 * <ul>
 *   <li>类或方法标注了 {@link IgnoreResponse}</li>
 *   <li>返回值已是 {@code R} 类型</li>
 *   <li>HTTP 状态码非 2xx</li>
 *   <li>非 JSON 响应（如文件下载，由 {@code selectedContentType} 判定）</li>
 * </ul>
 * 特别处理：若返回值为 {@code String}，自动转换为 JSON 格式的 {@code R} 对象，
 * 并设置响应头 {@code Content-Type=application/json}。
 * </p>
 *
 * @author dream
 */
@Slf4j
@RestControllerAdvice
@ConditionalOnProperty(prefix = "dream.advice.response", name = "enabled", havingValue = "true", matchIfMissing = true)
@Order(20) // 值越大，执行越靠后。
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public GlobalResponseHandler(@Nullable ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, @Nullable Class<? extends HttpMessageConverter<?>> converterType) {
        // 类级别或方法级别标注了 @IgnoreResponse 则跳过
        if (returnType.getContainingClass().isAnnotationPresent(IgnoreResponse.class)) {
            return false;
        }
        return !returnType.hasMethodAnnotation(IgnoreResponse.class);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  @Nullable MethodParameter returnType,
                                  @Nullable MediaType selectedContentType,
                                  @Nullable Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @Nullable ServerHttpRequest request,
                                  @Nullable ServerHttpResponse response) {

        // 已是 R 类型 → 不再包装
        if (body instanceof R<?>) {
            return body;
        }

        // byte[] 类型 → 二进制响应（如文件下载），直接跳过，不受状态码影响
        if (body instanceof byte[]) {
            return body;
        }

        // 非 2xx 状态码不包装（如异常处理器已自行构造响应）
        if (response instanceof ServletServerHttpResponse) {
            HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
            int status = servletResponse.getStatus();
            if (status < HttpStatus.OK.value() || status >= HttpStatus.MULTIPLE_CHOICES.value()) {
                HttpStatus httpStatus = HttpStatus.resolve(status);
                if (httpStatus != null) {
                    return R.error(status, httpStatus.getReasonPhrase());
                }
                return R.fail(ResponseCode.UNKNOWN_ERROR);
            }
        }

        // void 返回 → 包装为成功空对象
        if (body == null) {
            return R.ok();
        }

        // String 特殊处理：转为 JSON 格式的 R
        if (body instanceof String) {
            try {
                // 强制修改 Content-Type 为 application/json
                if (response instanceof ServletServerHttpResponse) {
                    ((ServletServerHttpResponse) response).getServletResponse().setContentType(MediaType.APPLICATION_JSON_VALUE);
                }
                return objectMapper.writeValueAsString(R.ok(body));
            } catch (Exception e) {
                log.error("序列化 String 响应为 JSON 失败，返回原始内容", e);
                return body;
            }
        }

        // 非 JSON 媒体类型（如文件流）不包装
        if (selectedContentType != null && !selectedContentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            return body;
        }

        // 普通对象 → 包装为 R
        return R.ok(body);
    }
}