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
import java.util.regex.Pattern;

/**
 * 链路追踪过滤器，在 MDC 中注入 traceId。
 * <p>
 * 优先从请求头 {@code X-Trace-Id} 获取，格式非法或不存在时自动生成 UUID。
 * 日志配置中可使用 {@code %X{traceId}} 输出。
 * </p>
 *
 * <h2>为什么必须校验入站 traceId</h2>
 * <p>
 * 该请求头完全由客户端控制。若原样写入 MDC 并输出到日志，
 * 攻击者可提交含换行/控制字符的值伪造日志行（<i>Log Injection</i>），
 * 污染审计与排障现场；超长值还会放大日志体积。
 * 因此只接受 {@code [A-Za-z0-9_-]} 且长度不超过 64 的值，
 * 其余（含缺失）一律服务端重新生成——不信任、不报错、不影响业务请求。
 * </p>
 *
 * @author dream
 */
@Slf4j
public class TraceIdFilter extends OncePerRequestFilter {

    /** 合法 traceId：字母、数字、下划线、中划线，长度 1~64 */
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @Nullable HttpServletResponse response,
                                    @Nullable FilterChain filterChain) throws ServletException, IOException {
        String traceId = sanitize(request.getHeader(Http.HEADER_X_TRACE_ID));
        if (traceId == null) {
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
            // 只清理本过滤器写入的 traceId，避免误伤业务方写入的其他 ThreadLocal 上下文
            ThreadLocalUtils.remove(SysKey.TRACE_ID);
        }
    }

    /**
     * 校验入站 traceId 是否可安全用于日志与响应头。
     *
     * @param raw 请求头原始值，可为 {@code null}
     * @return 合法时返回原值；非法或缺失返回 {@code null}（由调用方重新生成）
     */
    @Nullable
    private static String sanitize(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return TRACE_ID_PATTERN.matcher(raw).matches() ? raw : null;
    }
}
