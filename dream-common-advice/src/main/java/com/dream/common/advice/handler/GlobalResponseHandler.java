package com.dream.common.advice.handler;

import com.dream.common.advice.annotation.IgnoreResponse;
import com.dream.common.core.constant.SysKey;
import com.dream.common.core.enums.ResponseCode;
import com.dream.common.core.utils.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

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
 * <p>
 * 可通过 {@code dream.advice.response.include-metadata} 控制是否为响应补充
 * {@code timestamp}、{@code path} 元数据。
 * </p>
 *
 * @author dream
 */
@Slf4j
@RestControllerAdvice
@ConditionalOnProperty(prefix = "dream.advice.response", name = "enabled", havingValue = "true", matchIfMissing = true)
@Order(Ordered.LOWEST_PRECEDENCE) // 显式声明最低优先级，确保包装逻辑最后执行
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    /**
     * 内置不包装路径（文档端点与健康检查），Ant 风格通配
     */
    private static final List<String> DEFAULT_IGNORE_PATHS = Arrays.asList(
            "/v3/api-docs**", "/swagger-ui**", "/swagger-resources**", "/doc.html**", "/webjars**", "/actuator**");

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ObjectMapper objectMapper;

    /**
     * 是否在响应体中补充元数据（path），默认开启
     */
    @Value("${dream.advice.response.include-metadata:true}")
    private boolean includeMetadata;

    /**
     * 忽略响应包装的路径，Ant 风格（如 /export/**），多个路径用英文逗号分隔，默认为空
     */
    @Value("${dream.advice.response.ignore-response-path:}")
    private String ignoreResponsePath;


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

        try {
            // 路径忽略：内置文档/健康检查端点 + 配置项，Ant 风格精确匹配，避免 contains 误放行
            String uri = resolveRequestUri(request);
            if (uri != null && isIgnoredPath(uri)) {
                return body;
            }
        } catch (Exception e) {
            log.error("ignoreResponsePath match error", e);
        }

        // 已是 R 类型 → 不再包装，仅补充元数据
        if (body instanceof R<?>) {
            return enrichMetadata((R<?>) body, request);
        }

        // 二进制/流式/资源类响应（文件下载等）不包装，且不受状态码分支影响
        if (body instanceof byte[] || body instanceof Resource || body instanceof StreamingResponseBody) {
            return body;
        }

        // 非 2xx 状态码不包装（如异常处理器已自行构造响应），保留原始响应体避免丢失错误详情
        if (response instanceof ServletServerHttpResponse) {
            HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
            int status = servletResponse.getStatus();
            if (status < HttpStatus.OK.value() || status >= HttpStatus.MULTIPLE_CHOICES.value()) {
                return body != null ? body : resolveFallbackError(status);
            }
        }

        // void 返回 → 包装为成功空对象
        if (body == null) {
            return enrichMetadata(R.ok(), request);
        }

        // String 特殊处理：转为 JSON 格式的 R
        if (body instanceof String) {
            try {
                // 强制修改 Content-Type 为 application/json
                if (response instanceof ServletServerHttpResponse) {
                    ((ServletServerHttpResponse) response).getServletResponse().setContentType(MediaType.APPLICATION_JSON_VALUE);
                }
                return objectMapper.writeValueAsString(enrichMetadata(R.ok(body), request));
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
        return enrichMetadata(R.ok(body), request);
    }

    /**
     * 解析当前请求路径。优先使用 {@link ServerHttpRequest}，为空时回退到 {@code RequestContextHolder}。
     *
     * @param request 当前请求（可能为空）
     * @return 请求 URI 路径，无法解析时返回 {@code null}
     */
    @Nullable
    private String resolveRequestUri(@Nullable ServerHttpRequest request) {
        if (request != null && request.getURI() != null) {
            return request.getURI().getPath();
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
            HttpServletRequest servletRequest = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            return servletRequest.getRequestURI();
        }
        return null;
    }

    /**
     * 判断路径是否命中忽略包装名单（内置端点 + 配置项）。
     * <p>配置项支持 Ant 风格通配；不含通配符的路径按前缀匹配其子路径。</p>
     *
     * @param uri 请求路径
     * @return 命中返回 {@code true}
     */
    private boolean isIgnoredPath(String uri) {
        for (String pattern : DEFAULT_IGNORE_PATHS) {
            if (PATH_MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        if (ignoreResponsePath != null && !ignoreResponsePath.trim().isEmpty()) {
            for (String pattern : ignoreResponsePath.split(",")) {
                String trimmed = pattern.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (PATH_MATCHER.match(trimmed, uri)) {
                    return true;
                }
                // 不含通配符时同时放行其子路径，如 /export 命中 /export/file
                if (trimmed.indexOf('*') < 0 && PATH_MATCHER.match(trimmed + "/**", uri)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 非 2xx 且响应体为空时的兜底错误结果。
     *
     * @param status HTTP 状态码
     * @return 兜底 {@link R} 对象
     */
    private R<?> resolveFallbackError(int status) {
        HttpStatus httpStatus = HttpStatus.resolve(status);
        return httpStatus != null ? R.error(status, httpStatus.getReasonPhrase()) : R.fail(ResponseCode.UNKNOWN_ERROR);
    }

    /**
     * 为响应结果补充元数据（timestamp、path）。
     *
     * @param result  统一响应结果
     * @param request 当前请求，用于提取路径
     * @return 补充元数据后的结果
     */
    private R<?> enrichMetadata(R<?> result, @Nullable ServerHttpRequest request) {
        if (includeMetadata && result != null) {
            if (request != null && request.getURI() != null) {
                result.put(SysKey.PATH, request.getURI().getPath());
            }
        }
        return result;
    }
}