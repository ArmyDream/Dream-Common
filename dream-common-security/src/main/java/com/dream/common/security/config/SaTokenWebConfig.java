package com.dream.common.security.config;

import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.dream.common.core.constant.OrderValue;
import com.dream.common.security.filter.CorsFilter;
import com.dream.common.security.handler.SaTokenFilterErrorHandler;
import com.dream.common.security.support.SecurityExcludePathProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 安全过滤器注册配置。
 * <p>
 * 注册两个独立过滤器，按 {@link OrderValue} 顺序执行（CorsFilter 在前，SaServletFilter 紧随其后）：
 * </p>
 * <ol>
 *   <li>{@link CorsFilter}（{@link OrderValue#CORS_FILTER}）—— 注入 CORS 响应头、处理 OPTIONS 预检</li>
 *   <li>{@link SaServletFilter}（{@link OrderValue#SA_TOKEN_FILTER}）—— 登录校验 + 异常 JSON 响应</li>
 * </ol>
 * <p>
 * 职责分离：白名单合并委托给 {@link SecurityExcludePathProvider}，
 * 异常响应委托给 {@link SaTokenFilterErrorHandler}，本类只负责过滤器装配。
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

    private final SecurityProperties properties;
    private final SecurityExcludePathProvider excludePathProvider;
    private final SaTokenFilterErrorHandler errorHandler;

    /**
     * 注册 CORS 过滤器（若 {@code dream.security.cors.enabled=true}，默认 true）。
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
     * 注册 Sa-Token 鉴权过滤器（若 {@code dream.security.filter.enabled=true}，默认 true）。
     * <p>
     * 登录校验开关：{@code dream.security.filter.check-login}；
     * 白名单：{@link SecurityExcludePathProvider} 逐请求合并（支持热更新）；
     * 异常响应：{@link SaTokenFilterErrorHandler} 统一转 JSON。
     * </p>
     *
     * @return FilterRegistrationBean
     */
    @Bean
    @ConditionalOnProperty(prefix = "dream.security.filter", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<SaServletFilter> saServletFilterRegistration() {
        boolean checkLogin = properties.getFilter().isCheckLogin();

        SaServletFilter filter = new SaServletFilter()
                .addInclude("/**")
                .addExclude("/favicon.ico")
                .setBeforeAuth(obj -> {
                    // OPTIONS 预检直接放行（CORS 头由 CorsFilter 已注入）
                    SaRouter.match(SaHttpMethod.OPTIONS).back();
                })
                .setAuth(obj -> {
                    if (!checkLogin) {
                        return;
                    }
                    SaRouter.match("/**")
                            .notMatch(excludePathProvider.getExcludePaths())
                            .check(StpUtil::checkLogin);
                })
                .setError(errorHandler::handle);

        FilterRegistrationBean<SaServletFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(OrderValue.SA_TOKEN_FILTER);
        registration.setName("saTokenFilter");
        log.info("[SaToken] SaServletFilter 已注册 | order={} | checkLogin={}", OrderValue.SA_TOKEN_FILTER, checkLogin);
        return registration;
    }
}
