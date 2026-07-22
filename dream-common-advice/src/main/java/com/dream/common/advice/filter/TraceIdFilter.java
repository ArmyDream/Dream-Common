package com.dream.common.advice.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * 链路追踪过滤器，在 MDC 中注入 traceId。
 * <p>
 * 支持从请求头 {@code X-Trace-Id} 获取，若无则自动生成。
 * 日志配置中可使用 {@code %X{traceId}} 输出。
 * </p>
 *
 * @author dream
 */
@Slf4j
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @Nullable HttpServletResponse response,
                                    @Nullable FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        try {
            MDC.put(TRACE_ID_MDC_KEY, traceId);
            if (response != null) {
                response.setHeader(TRACE_ID_HEADER, traceId);
            }
            if (filterChain != null) {
                filterChain.doFilter(request, response);
            }
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }
}