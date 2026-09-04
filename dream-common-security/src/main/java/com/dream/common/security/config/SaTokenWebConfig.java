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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sa-Token 相关过滤器配置。
 * <p>注册 {@link CorsFilter} 和 {@link SaServletFilter}，分别处理 CORS 和登录鉴权。</p>
 * <p>登录鉴权支持通过配置文件和缓存（Redis）动态白名单，缓存使用 Caffeine 本地缓存（30秒过期）。</p>
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

    private static final int CORS_FILTER_ORDER = OrderValue.CORS_FILTER;
    private static final int SA_TOKEN_FILTER_ORDER = OrderValue.SA_TOKEN_FILTER;
    private static final String BUILTIN_EXCLUDE_ERROR = "/error";
    private static final String BUILTIN_EXCLUDE_LOGIN = "/sys/login";
    private static final String BUILTIN_EXCLUDE_LOGOUT = "/sys/logout";
    public static final String EXCLUDE_PATHS_CACHE_KEY = Security.CACHE_PREFIX_EXCLUDE_PATHS;

    private final SecurityProperties properties;

    private ExceptionHandlerDelegate delegate;

    @Autowired(required = false)
    public void setDelegate(ExceptionHandlerDelegate delegate) {
        this.delegate = delegate;
    }


    /**
     * 本地缓存：白名单路径列表，30秒过期，最多缓存10个键。
     */
    private final Cache<String, List<String>> whitelistCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(10)
            .recordStats()
            .build();

    /**
     * 注册 CORS 过滤器，优先级最高。
     *
     * @return FilterRegistrationBean
     */
    @Bean
    @ConditionalOnProperty(prefix = "dream.security.cors", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CorsFilter(properties.getCors()));
        registration.addUrlPatterns("/*");
        registration.setOrder(CORS_FILTER_ORDER);
        registration.setName("corsFilter");
        log.info("[SaToken] CorsFilter 已注册");
        return registration;
    }

    /**
     * 注册 Sa-Token 鉴权过滤器，负责登录校验、异常处理。
     *
     * @return FilterRegistrationBean
     */
    @Bean
    @ConditionalOnProperty(prefix = "dream.security.filter", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<SaServletFilter> saServletFilterRegistration() {
        boolean interceptorEnabled = properties.getInterceptor().isEnabled();

        SaServletFilter filter = new SaServletFilter()
                .addInclude("/**")
                .addExclude("/favicon.ico")
                .setBeforeAuth(obj -> {
                    // OPTIONS 请求直接放行（由 CorsFilter 处理）
                    SaRouter.match(SaHttpMethod.OPTIONS).back();
                })
                .setAuth(obj -> {
                    if (!interceptorEnabled) {
                        return;
                    }
                    List<String> excludePaths = getExcludePaths();
                    SaRouter.match("/**")
                            .notMatch(excludePaths)
                            .check(StpUtil::checkLogin);
                })
                .setError(e -> {
                    R<?> result;
                    int status;
                    if (e instanceof Exception) {
                        HandleUtils.handleWithDelegate(delegate, (Exception) e, null, false,null);
                    }
                    if (e instanceof NotLoginException) {
                        status = HttpServletResponse.SC_UNAUTHORIZED;
                        result = R.fail(ResponseCode.UNAUTHORIZED);
                        log.error("[SaToken] 未登录: {}", e.getMessage());
                    } else if (e instanceof NotPermissionException) {
                        status = HttpServletResponse.SC_FORBIDDEN;
                        result = R.fail(ResponseCode.FORBIDDEN);
                        log.error("[SaToken] 无权限: {}", e.getMessage());
                    } else if (e instanceof NotRoleException) {
                        status = HttpServletResponse.SC_FORBIDDEN;
                        result = R.fail(ResponseCode.FORBIDDEN);
                        log.error("[SaToken] 角色不足: {}", e.getMessage());
                    } else {
                        status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                        result = R.fail(ResponseCode.INTERNAL_ERROR);
                        log.error("[SaToken] 未知异常: {}", e.getMessage());
                    }
                    HttpServletResponse httpResp = ServletUtils.getResponse();
                    if (httpResp != null) {
                        httpResp.setStatus(status);
                    }
                    return result;
                });

        FilterRegistrationBean<SaServletFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(SA_TOKEN_FILTER_ORDER);
        registration.setName("saTokenFilter");
        log.info("[SaToken] SaServletFilter 已注册（鉴权 + 异常处理）");
        return registration;
    }

    /**
     * 获取当前白名单路径列表（合并内置、配置文件、缓存动态路径）。
     * 使用 Caffeine 缓存减少频繁 Redis 访问。
     *
     * @return 不可修改的白名单路径列表
     */
    private List<String> getExcludePaths() {
        return whitelistCache.get(EXCLUDE_PATHS_CACHE_KEY, key -> mergeExcludePaths());
    }

    /**
     * 合并白名单路径。
     *
     * @return 合并后的路径列表
     */
    private List<String> mergeExcludePaths() {
        List<String> result = new ArrayList<>();
        result.add(BUILTIN_EXCLUDE_ERROR);
        result.add(BUILTIN_EXCLUDE_LOGIN);
        result.add(BUILTIN_EXCLUDE_LOGOUT);

        // 配置文件静态白名单
        List<String> configPaths = properties.getInterceptor().getExcludePaths();
        if (configPaths != null) {
            for (String path : configPaths) {
                if (path != null && !path.trim().isEmpty() && !result.contains(path.trim())) {
                    result.add(path.trim());
                }
            }
        }

        // 缓存动态白名单（支持热更新）
        try {
            String cached = CachesUtils.get(EXCLUDE_PATHS_CACHE_KEY);
            if (cached != null && !cached.trim().isEmpty()) {
                for (String path : cached.split(",")) {
                    String trimmed = path.trim();
                    if (!trimmed.isEmpty() && !result.contains(trimmed)) {
                        result.add(trimmed);
                    }
                }
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("[SaToken] 从缓存获取白名单失败，仅使用配置路径", e);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("[SaToken] 白名单路径: {}", result);
        }
        return result;
    }
}