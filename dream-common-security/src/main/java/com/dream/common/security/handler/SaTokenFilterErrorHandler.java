package com.dream.common.security.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.dream.common.core.api.ExceptionHandlerDelegate;
import com.dream.common.core.enums.ResponseCode;
import com.dream.common.core.utils.HandleUtils;
import com.dream.common.core.utils.R;
import com.dream.common.framework.utils.ServletUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletResponse;

/**
 * Sa-Token 过滤器阶段异常处理器。
 * <p>
 * 处理 {@code SaServletFilter.setError} 回调收到的异常。此阶段异常发生在
 * DispatcherServlet 之前，{@code @RestControllerAdvice} 无法拦截，
 * 故在此独立完成「异常 → 统一响应 + HTTP 状态码」的映射：
 * </p>
 * <ul>
 *   <li>{@link NotLoginException} → 401 + {@link ResponseCode#UNAUTHORIZED}</li>
 *   <li>{@link NotPermissionException} / {@link NotRoleException} → 403 + {@link ResponseCode#FORBIDDEN}</li>
 *   <li>其他异常 → 500 + {@link ResponseCode#INTERNAL_ERROR}</li>
 * </ul>
 * <p>
 * 业务方实现了 {@link ExceptionHandlerDelegate#handleUnknownException} 时优先调用之，
 * 返回非 {@code null} 即采用自定义响应。
 * </p>
 *
 * @author dream
 * @since 2026-09-24
 */
@Slf4j
public class SaTokenFilterErrorHandler {

    @Nullable
    private final ExceptionHandlerDelegate delegate;

    /**
     * 构造器注入可选的委托处理器。
     *
     * @param delegate 自定义委托，可为 {@code null}
     */
    public SaTokenFilterErrorHandler(@Nullable ExceptionHandlerDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * 处理过滤器链中抛出的异常，返回统一响应对象。
     *
     * @param e 异常
     * @return 统一响应（{@link R}）
     */
    public Object handle(Throwable e) {
        Exception ex = (e instanceof Exception) ? (Exception) e : null;

        // 业务方自定义响应优先
        R<?> custom = resolveCustomResponse(ex);
        if (custom != null) {
            applyHttpStatus(HttpServletResponse.SC_OK);
            return custom;
        }

        if (ex instanceof NotLoginException) {
            log.warn("[SaToken] 未登录 | uri={} | type={} | msg={}",
                    ServletUtils.getRequestURI(), ((NotLoginException) ex).getType(), ex.getMessage());
            return fail(ResponseCode.UNAUTHORIZED, HttpServletResponse.SC_UNAUTHORIZED);
        }
        if (ex instanceof NotPermissionException) {
            log.warn("[SaToken] 无权限 | uri={} | code={}",
                    ServletUtils.getRequestURI(), ((NotPermissionException) ex).getCode());
            return fail(ResponseCode.FORBIDDEN, HttpServletResponse.SC_FORBIDDEN);
        }
        if (ex instanceof NotRoleException) {
            log.warn("[SaToken] 角色不足 | uri={} | role={}",
                    ServletUtils.getRequestURI(), ((NotRoleException) ex).getRole());
            return fail(ResponseCode.FORBIDDEN, HttpServletResponse.SC_FORBIDDEN);
        }

        log.error("[SaToken] 鉴权链路未知异常 | uri={} | msg={}",
                ServletUtils.getRequestURI(), ex != null ? ex.getMessage() : null, e);
        return fail(ResponseCode.INTERNAL_ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * 调用业务方委托获取自定义响应。
     *
     * @param ex 异常
     * @return 自定义响应，未注册委托或委托返回 {@code null} 时返回 {@code null}
     */
    @Nullable
    private R<?> resolveCustomResponse(@Nullable Exception ex) {
        if (delegate == null || ex == null) {
            return null;
        }
        return HandleUtils.handleWithDelegate(delegate, ex, null, true,
                delegate::handleUnknownException);
    }

    /**
     * 构造失败响应并写入 HTTP 状态码。
     *
     * @param code   业务响应码
     * @param status HTTP 状态码
     * @return 统一失败响应
     */
    private R<?> fail(ResponseCode code, int status) {
        applyHttpStatus(status);
        return R.fail(code);
    }

    /**
     * 写入 HTTP 响应状态码（取不到 response 时静默跳过，不阻断响应体输出）。
     *
     * @param status HTTP 状态码
     */
    private void applyHttpStatus(int status) {
        HttpServletResponse resp = ServletUtils.getResponse();
        if (resp == null) {
            if (log.isDebugEnabled()) {
                log.debug("[SaToken] 无法获取 HttpServletResponse，跳过状态码设置 | status={}", status);
            }
            return;
        }
        resp.setStatus(status);
    }
}
