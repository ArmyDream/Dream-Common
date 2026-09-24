package com.dream.common.security.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.dream.common.core.api.ExceptionHandlerDelegate;
import com.dream.common.core.constant.OrderValue;
import com.dream.common.core.constant.Security;
import com.dream.common.core.enums.ResponseCode;
import com.dream.common.core.utils.HandleUtils;
import com.dream.common.core.utils.R;
import com.dream.common.framework.cache.CachesUtils;
import com.dream.common.framework.utils.ServletUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token Web 过滤器配置。
 * <p>
 * 注册两个独立过滤器，按 {@link OrderValue} 顺序执行：
 * </p>
 * <ol>
 *   <li>{@link CorsFilter}（{@link OrderValue#CORS_FILTER}）—— 注入 CORS 响应头、处理 OPTIONS 预检</li>
 *   <li>{@link SaServletFilter}（{@link OrderValue#SA_TOKEN_FILTER}）—— 登录校验 + 异常 JSON 响应</li>
 * </ol>
 * <p>
 * CORS 与鉴权职责严格分离：SaServletFilter 不处理任何 CORS 头，避免职责重叠。
 * </p>
 *
 * <h2>白名单热更新</h2>
 * <p>
 * 白名单路径每次请求实时合并以下来源，支持运行期热更新（无需重启）：
 * </p>
 * <ul>
 *   <li>内置：{@code /error}（Spring Boot 错误页）</li>
 *   <li>配置：{@code dream.security.interceptor.exclude-paths}（Ant 风格）</li>
 *   <li>缓存：{@link CachesUtils} key = {@link Security#CACHE_PREFIX_EXCLUDE_PATHS}（逗号分隔）</li>
 * </ul>
 * <p>
 * 不再使用本地 Caffeine 缓存白名单，确保缓存写入后立即生效；
 * {@link CachesUtils} 自身已具备 Redis 优先 + Caffeine 兜底 + 熔断降级，无需额外层级。
 * </p>
 *
 * @author dream
 * @since 2026-09-02
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(name = "cn.dev33.satoken.stp.StpUtil")
@ConditionalOnProperty(prefix = "dream.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SaTokenWebConfig {

    /** Spring Boot 默认错误页路径，必须放行否则异常响应会被二次拦截 */
    private static final String BUILTIN_EXCLUDE_ERROR = "/error";

    private final SecurityProperties properties;

    @Nullable
    private ExceptionHandlerDelegate delegate;

    /**
     * 可选注入业务方自定义异常委托，未实现时使用默认处理。
     *
     * @param delegate 自定义委托，可为 {@code null}
     */
    @Autowired(required = false)
    public void setDelegate(@Nullable ExceptionHandlerDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * 注册 CORS 过滤器，优先级最高（{@link OrderValue#CORS_FILTER}）。
     *
     * @return FilterRegistrationBean
     */
    @Bean
    @ConditionalOnProperty(prefix = "dream.security.cors", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CorsFilter(properties.getCors()));
        registration.addUrlPatterns("/*");
        registration.setOrder(OrderValue.CORS_FILTER);
        registration.setName("corsFilter");
        log.info("[SaToken] CorsFilter 已注册 | order={}", OrderValue.CORS_FILTER);
        return registration;
    }

    /**
     * 注册 Sa-Token 鉴权过滤器，优先级紧随 CorsFilter（{@link OrderValue#SA_TOKEN_FILTER}）。
     * <p>
     * 职责：登录校验、白名单放行、Sa-Token 异常转 JSON 响应。
     * 不处理 CORS（由 {@link CorsFilter} 独立完成）。
     * </p>
     *
     * @return FilterRegistrationBean
     */
    @Bean
    @ConditionalOnProperty(prefix = "dream.security.filter", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<SaServletFilter> saServletFilterRegistration() {
        boolean loginCheckEnabled = properties.getInterceptor().isEnabled();

        SaServletFilter filter = new SaServletFilter()
                .addInclude("/**")
                .addExclude("/favicon.ico")
                .setBeforeAuth(obj -> {
                    // OPTIONS 预检直接放行（CORS 头由 CorsFilter 已注入）
                    SaRouter.match(SaHttpMethod.OPTIONS).back();
                })
                .setAuth(obj -> {
                    if (!loginCheckEnabled) {
                        return;
                    }
                    List<String> excludePaths = mergeExcludePaths();
                    SaRouter.match("/**")
                            .notMatch(excludePaths)
                            .check(StpUtil::checkLogin);
                })
                .setError(this::handleFilterError);

        FilterRegistrationBean<SaServletFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(OrderValue.SA_TOKEN_FILTER);
        registration.setName("saTokenFilter");
        log.info("[SaToken] SaServletFilter 已注册（鉴权 + 异常处理）| order={} | loginCheck={}",
                OrderValue.SA_TOKEN_FILTER, loginCheckEnabled);
        return registration;
    }

    /**
     * SaServletFilter 异常响应处理器：将 Sa-Token 抛出的异常转为标准 {@link R} JSON。
     * <p>
     * 此处处理的异常来自过滤器链（DispatcherServlet 之前），{@link com.dream.common.security.handler.SaTokenExceptionHandler}
     * 的 {@code @ExceptionHandler} 无法拦截，故必须在此自行构造响应。
     * </p>
     * <p>
     * 若业务方注册了 {@link ExceptionHandlerDelegate}，先调用其 {@code handleUnknownException}
     * 获取自定义响应；返回非 null 时直接使用，否则按异常类型走默认分支。
     * </p>
     *
     * @param e Sa-Token 抛出的异常
     * @return 统一响应对象
     */
    private Object handleFilterError(Throwable e) {
        Exception ex = (e instanceof Exception) ? (Exception) e : null;
        R<?> custom = null;
        if (ex != null) {
            custom = HandleUtils.handleWithDelegate(delegate, ex, null, true,
                    (x, req) -> {
                        if (delegate != null) {
                            return delegate.handleUnknownException(x, req);
                        }
                        return null;
                    });
        }

        if (custom != null) {
            applyHttpStatus(ex, HttpServletResponse.SC_OK);
            return custom;
        }

        ResponseCode code;
        int status;
        if (ex instanceof NotLoginException) {
            code = ResponseCode.UNAUTHORIZED;
            status = HttpServletResponse.SC_UNAUTHORIZED;
            log.warn("[SaToken] 未登录 | uri={} | type={} | msg={}",
                    ServletUtils.getRequestURI(),
                    ex instanceof NotLoginException ? ((NotLoginException) ex).getType() : null,
                    ex.getMessage());
        } else if (ex instanceof NotPermissionException) {
            code = ResponseCode.FORBIDDEN;
            status = HttpServletResponse.SC_FORBIDDEN;
            log.warn("[SaToken] 无权限 | uri={} | code={}",
                    ServletUtils.getRequestURI(), ((NotPermissionException) ex).getCode());
        } else if (ex instanceof NotRoleException) {
            code = ResponseCode.FORBIDDEN;
            status = HttpServletResponse.SC_FORBIDDEN;
            log.warn("[SaToken] 角色不足 | uri={} | role={}",
                    ServletUtils.getRequestURI(), ((NotRoleException) ex).getRole());
        } else {
            code = ResponseCode.INTERNAL_ERROR;
            status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            log.error("[SaToken] 鉴权链路未知异常 | uri={} | msg={}",
                    ServletUtils.getRequestURI(), ex != null ? ex.getMessage() : null, e);
        }
        applyHttpStatus(ex, status);
        return R.fail(code);
    }

    /**
     * 写入 HTTP 响应状态码（取不到 response 时静默跳过，不阻断响应体输出）。
     *
     * @param ex     异常（仅用于日志）
     * @param status HTTP 状态码
     */
    private void applyHttpStatus(Exception ex, int status) {
        HttpServletResponse resp = ServletUtils.getResponse();
        if (resp == null) {
            if (log.isDebugEnabled()) {
                log.debug("[SaToken] 无法获取 HttpServletResponse，跳过状态码设置 | status={}", status);
            }
            return;
        }
        resp.setStatus(status);
    }

    /**
     * 合并白名单路径：内置 + 配置 + 缓存（每次请求读取，支持热更新）。
     *
     * @return 不可修改的白名单路径列表
     */
    private List<String> mergeExcludePaths() {
        List<String> result = new ArrayList<>();
        result.add(BUILTIN_EXCLUDE_ERROR);

        // 配置文件静态白名单
        List<String> configPaths = properties.getInterceptor().getExcludePaths();
        if (configPaths != null) {
            for (String path : configPaths) {
                if (path != null) {
                    String trimmed = path.trim();
                    if (!trimmed.isEmpty() && !result.contains(trimmed)) {
                        result.add(trimmed);
                    }
                }
            }
        }

        // 缓存动态白名单（支持运行期热更新）
        mergeCachedExcludePaths(result);

        if (log.isDebugEnabled()) {
            log.debug("[SaToken] 白名单路径: {}", result);
        }
        return result;
    }

    /**
     * 将缓存中的动态白名单合并进结果列表。
     * <p>
     * 缓存 key = {@link Security#CACHE_PREFIX_EXCLUDE_PATHS}，值为逗号分隔的路径串。
     * 缓存读取异常时降级为仅使用已合并的路径，不阻断鉴权流程。
     * </p>
     *
     * @param result 待合并的结果列表
     */
    private void mergeCachedExcludePaths(List<String> result) {
        String cached;
        try {
            cached = CachesUtils.get(Security.CACHE_PREFIX_EXCLUDE_PATHS);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("[SaToken] 从缓存读取白名单失败，仅使用配置路径", e);
            }
            return;
        }
        if (cached == null || cached.trim().isEmpty()) {
            return;
        }
        for (String path : cached.split(",")) {
            String trimmed = path.trim();
            if (!trimmed.isEmpty() && !result.contains(trimmed)) {
                result.add(trimmed);
            }
        }
    }
}
