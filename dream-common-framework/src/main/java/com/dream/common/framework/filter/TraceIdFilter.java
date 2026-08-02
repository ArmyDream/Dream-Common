package com.dream.common.framework.filter;

import com.dream.common.core.constant.Http;
import com.dream.common.core.constant.SysKey;
import com.dream.common.core.utils.ThreadLocalUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @Nullable HttpServletResponse response,
                                    @Nullable FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(Http.HEADER_X_TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        try {
            MDC.put(SysKey.TRACE_ID, traceId);
            // 存入 ThreadLocal 供全局使用
            ThreadLocalUtils.put(SysKey.TRACE_ID, traceId);
            if (response != null) {
                response.setHeader(Http.HEADER_X_TRACE_ID, traceId);
            }
            if (filterChain != null) {
                filterChain.doFilter(request, response);
            }
        } finally {
            MDC.remove(SysKey.TRACE_ID);
            // 优化点：必须清理，防止内存泄漏和数据串用
            ThreadLocalUtils.clear();
        }
    }
}
