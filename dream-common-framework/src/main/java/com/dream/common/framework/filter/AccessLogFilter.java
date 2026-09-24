package com.dream.common.framework.filter;

import com.dream.common.core.constant.SysKey;
import com.dream.common.framework.api.AccessLogHandler;
import com.dream.common.framework.model.AccessLog;
import com.dream.common.framework.utils.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 请求访问日志过滤器。
 * <p>
 * 采集每个请求的 {@code method}、{@code uri}、客户端 {@code ip}、响应状态码、
 * 耗时（毫秒）以及链路追踪 {@code traceId}，封装为 {@link AccessLog}。
 * </p>
 * <p>
 * 若容器中存在 {@link AccessLogHandler} 实现，则交由业务方自定义处理（如落库、上报）；
 * 否则使用默认行为（打印一条访问日志）。需配合 {@link TraceIdFilter} 使用，本过滤器
 * 执行顺序应位于其之后，以保证 MDC 中的 traceId 可用。
 * </p>
 *
 * @author dream
 */
@Slf4j
public class AccessLogFilter extends OncePerRequestFilter {

    private final AccessLogHandler accessLogHandler;

    public AccessLogFilter(@Nullable AccessLogHandler accessLogHandler) {
        this.accessLogHandler = accessLogHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @Nullable HttpServletResponse response,
                                    @Nullable FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = sanitizeQuery(request.getQueryString());
        String fullUri = queryString != null ? uri + "?" + queryString : uri;
        String ip = IpUtils.getIpAddress(request);

        try {
            if (filterChain != null) {
                filterChain.doFilter(request, response);
            }
        } finally {
            long cost = System.currentTimeMillis() - start;
            int status = response != null ? response.getStatus() : 0;
            AccessLog accessLog = new AccessLog(MDC.get(SysKey.TRACE_ID), method, fullUri, ip, status, cost, start);
            dispatch(accessLog);
        }
    }

    /**
     * 需要脱敏的查询参数关键字（命中则值替换为 ***），全部小写比较。
     */
    private static final String[] SENSITIVE_PARAMS = {
            "password", "passwd", "pwd", "token", "secret", "authorization", "credential", "code", "captcha"
    };

    /**
     * 对 queryString 中的敏感参数值脱敏，避免密码/令牌等进入访问日志。
     *
     * @param queryString 原始查询串（可空）
     * @return 脱敏后的查询串，入参为空时返回 {@code null}
     */
    @Nullable
    static String sanitizeQuery(@Nullable String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return queryString;
        }
        String[] pairs = queryString.split("&");
        StringBuilder sb = new StringBuilder(queryString.length());
        for (int i = 0; i < pairs.length; i++) {
            if (i > 0) {
                sb.append('&');
            }
            String pair = pairs[i];
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String name = pair.substring(0, eq);
                if (isSensitive(name)) {
                    sb.append(name).append("=***");
                    continue;
                }
            }
            sb.append(pair);
        }
        return sb.toString();
    }

    private static boolean isSensitive(String paramName) {
        String lower = paramName.toLowerCase(java.util.Locale.ROOT);
        for (String sensitive : SENSITIVE_PARAMS) {
            if (lower.contains(sensitive)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 分发访问日志：优先使用业务方实现，否则打印默认日志。
     *
     * @param accessLog 访问日志数据
     */
    private void dispatch(AccessLog accessLog) {
        if (accessLogHandler != null) {
            try {
                accessLogHandler.handle(accessLog);
            } catch (Exception e) {
                log.error("自定义访问日志处理失败: {}", accessLog, e);
            }
            return;
        }
        log.info("请求访问日志: traceId={}, method={}, uri={}, ip={}, status={}, cost={}ms",
                accessLog.getTraceId(), accessLog.getMethod(), accessLog.getUri(),
                accessLog.getIp(), accessLog.getStatus(), accessLog.getCost());
    }
}
