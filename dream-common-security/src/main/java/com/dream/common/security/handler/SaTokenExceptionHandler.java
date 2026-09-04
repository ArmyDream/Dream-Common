package com.dream.common.security.handler;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.dream.common.core.api.ExceptionHandlerDelegate;
import com.dream.common.core.constant.OrderValue;
import com.dream.common.core.enums.ResponseCode;
import com.dream.common.core.utils.R;
import com.dream.common.framework.utils.ServletUtils;
import com.dream.common.security.aspect.PermissionAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Sa-Token 异常处理器（适用于 DispatcherServlet 之后抛出的异常）。
 * <p>处理 {@link PermissionAspect} 或业务代码中抛出的 Sa-Token 异常，
 * 统一转换为标准 {@link R} 响应。</p>
 * <p>支持通过 {@link ExceptionHandlerDelegate} 扩展自定义处理逻辑。</p>
 *
 * @author dream
 * @since 2026-09-03
 */
@Slf4j
@RestControllerAdvice
@ConditionalOnClass(name = "cn.dev33.satoken.stp.StpUtil")
@ConditionalOnProperty(prefix = "dream.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@Order(OrderValue.MIN_VALUE) // 确保优先于其他全局异常处理器
public class SaTokenExceptionHandler {

    @Nullable
    private final ExceptionHandlerDelegate delegate;

    /**
     * 构造器，注入可选的委托处理器。
     *
     * @param delegate 自定义委托，可为 {@code null}
     */
    public SaTokenExceptionHandler(@Autowired(required = false) @Nullable ExceptionHandlerDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * 调用委托处理器的回调方法，尝试返回自定义响应。
     *
     * @param ex      异常
     * @param request 当前请求
     * @return 自定义响应，若无委托或返回 {@code null} 则继续使用默认逻辑
     */
    @Nullable
    private R<?> applyDelegate(Exception ex, WebRequest request) {
        if (delegate == null) {
            return null;
        }
        try {
            delegate.onException(ex, request);
        } catch (Exception e) {
            log.warn("[SaToken] onException 回调失败", e);
        }
        try {
            return delegate.handleUnknownException(ex, request);
        } catch (Exception e) {
            log.warn("[SaToken] handleUnknownException 回调失败", e);
            return null;
        }
    }

    /**
     * 处理未登录异常。
     *
     * @param e   异常
     * @param req HTTP 请求
     * @return 401 响应
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<?> handleNotLogin(NotLoginException e, WebRequest req) {
        log.warn("[Sa-Token] 未登录 | path={} | type={} | msg={}", ServletUtils.getRequestUri(req), e.getType(), e.getMessage());
        R<?> custom = applyDelegate(e, req);
        return custom != null ? custom : R.fail(ResponseCode.UNAUTHORIZED, "未登录或登录已过期，请重新登录");
    }

    /**
     * 处理无权限异常。
     *
     * @param e   异常
     * @param req HTTP 请求
     * @return 403 响应
     */
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<?> handleNotPermission(NotPermissionException e, WebRequest req) {
        log.warn("[Sa-Token] 无权限 | path={} | code={}", ServletUtils.getRequestUri(req), e.getCode());
        R<?> custom = applyDelegate(e, req);
        return custom != null ? custom : R.fail(ResponseCode.FORBIDDEN, "无权限：" + e.getCode());
    }

    /**
     * 处理角色不足异常。
     *
     * @param e   异常
     * @param req HTTP 请求
     * @return 403 响应
     */
    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<?> handleNotRole(NotRoleException e, WebRequest req) {
        log.warn("[Sa-Token] 角色不足 | path={} | role={}", ServletUtils.getRequestUri(req), e.getRole());
        R<?> custom = applyDelegate(e, req);
        return custom != null ? custom : R.fail(ResponseCode.FORBIDDEN, "角色不足：" + e.getRole());
    }

    /**
     * 处理账号被禁用异常。
     *
     * @param e   异常
     * @param req HTTP 请求
     * @return 403 响应
     */
    @ExceptionHandler(DisableServiceException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<?> handleDisableService(DisableServiceException e, WebRequest req) {
        log.warn("[Sa-Token] 账号被禁用 | path={} | service={} | msg={}", ServletUtils.getRequestUri(req), e.getService(), e.getMessage());
        R<?> custom = applyDelegate(e, req);
        return custom != null ? custom : R.fail(ResponseCode.FORBIDDEN, "账号已被禁用：" + e.getMessage());
    }
}