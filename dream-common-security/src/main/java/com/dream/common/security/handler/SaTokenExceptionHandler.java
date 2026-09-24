package com.dream.common.security.handler;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.dream.common.core.api.ExceptionHandlerDelegate;
import com.dream.common.core.constant.OrderValue;
import com.dream.common.core.enums.ResponseCode;
import com.dream.common.core.utils.HandleUtils;
import com.dream.common.core.utils.R;
import com.dream.common.framework.utils.ServletUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Sa-Token 异常处理器（适用于 DispatcherServlet 阶段抛出的异常）。
 * <p>
 * 处理 {@link com.dream.common.security.aspect.PermissionAspect} 或业务代码中抛出的
 * Sa-Token 异常，统一转换为标准 {@link R} 响应。返回与异常对应的 HTTP 状态码：
 * </p>
 * <ul>
 *   <li>{@link NotLoginException} → 401 UNAUTHORIZED</li>
 *   <li>{@link NotPermissionException} / {@link NotRoleException} / {@link DisableServiceException} → 403 FORBIDDEN</li>
 * </ul>
 * <p>
 * 过滤器链阶段（DispatcherServlet 之前）的 Sa-Token 异常由
 * {@link com.dream.common.security.config.SaTokenWebConfig} 的 {@code setError} 处理，
 * 本处理器无法拦截。
 * </p>
 * <p>
 * 业务方可通过实现 {@link ExceptionHandlerDelegate#handleUnknownException(Exception, Object)}
 * 注入自定义响应（返回非 {@code null} 即覆盖默认）。
 * </p>
 *
 * @author dream
 * @since 2026-09-03
 */
@Slf4j
@RestControllerAdvice
@ConditionalOnClass(name = "cn.dev33.satoken.stp.StpUtil")
@ConditionalOnProperty(prefix = "dream.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@Order(OrderValue.MIN_VALUE)
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
     * 处理未登录异常。
     *
     * @param e   异常
     * @param req 请求上下文
     * @return 401 响应
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<?> handleNotLogin(NotLoginException e, WebRequest req) {
        log.warn("[Sa-Token] 未登录 | path={} | type={} | msg={}",
                ServletUtils.getRequestUri(req), e.getType(), e.getMessage());
        R<?> custom = HandleUtils.handleWithDelegate(delegate, e, req, true,
                (ex, r) -> {
                    if (delegate != null) {
                        return delegate.handleUnknownException(ex, r);
                    }
                    return null;
                });
        return custom != null ? custom : R.fail(ResponseCode.UNAUTHORIZED, "未登录或登录已过期，请重新登录");
    }

    /**
     * 处理无权限异常。
     *
     * @param e   异常
     * @param req 请求上下文
     * @return 403 响应
     */
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<?> handleNotPermission(NotPermissionException e, WebRequest req) {
        log.warn("[Sa-Token] 无权限 | path={} | code={}", ServletUtils.getRequestUri(req), e.getCode());
        R<?> custom = HandleUtils.handleWithDelegate(delegate, e, req, true,
                (ex, r) -> {
                    if (delegate != null) {
                        return delegate.handleUnknownException(ex, r);
                    }
                    return null;
                });
        return custom != null ? custom : R.fail(ResponseCode.FORBIDDEN, "无权限：" + e.getCode());
    }

    /**
     * 处理角色不足异常。
     *
     * @param e   异常
     * @param req 请求上下文
     * @return 403 响应
     */
    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<?> handleNotRole(NotRoleException e, WebRequest req) {
        log.warn("[Sa-Token] 角色不足 | path={} | role={}", ServletUtils.getRequestUri(req), e.getRole());
        R<?> custom = HandleUtils.handleWithDelegate(delegate, e, req, true,
                (ex, r) -> {
                    if (delegate != null) {
                        return delegate.handleUnknownException(ex, r);
                    }
                    return null;
                });
        return custom != null ? custom : R.fail(ResponseCode.FORBIDDEN, "角色不足：" + e.getRole());
    }

    /**
     * 处理账号被禁用异常。
     *
     * @param e   异常
     * @param req 请求上下文
     * @return 403 响应
     */
    @ExceptionHandler(DisableServiceException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<?> handleDisableService(DisableServiceException e, WebRequest req) {
        log.warn("[Sa-Token] 账号被禁用 | path={} | service={} | msg={}",
                ServletUtils.getRequestUri(req), e.getService(), e.getMessage());
        R<?> custom = HandleUtils.handleWithDelegate(delegate, e, req, true,
                (ex, r) -> {
                    if (delegate != null) {
                        return delegate.handleUnknownException(ex, r);
                    }
                    return null;
                });
        return custom != null ? custom : R.fail(ResponseCode.FORBIDDEN, "账号已被禁用：" + e.getMessage());
    }
}
