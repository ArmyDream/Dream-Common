package com.dream.common.security.config;

import com.dream.common.core.api.ExceptionHandlerDelegate;
import com.dream.common.framework.aspect.PreventDuplicateAspect;
import com.dream.common.security.aspect.PermissionAspect;
import com.dream.common.security.handler.SaTokenExceptionHandler;
import com.dream.common.security.handler.SaTokenFilterErrorHandler;
import com.dream.common.security.provider.StpInterfaceImpl;
import com.dream.common.security.support.SecurityExcludePathProvider;
import com.dream.common.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;

/**
 * 安全模块自动配置入口。
 * <p>
 * 当 classpath 中存在 Sa-Token 且 {@code dream.security.enabled=true}（默认）时生效。
 * 本类是模块在 {@code AutoConfiguration.imports} 中的唯一入口，
 * 组件注册分两类：
 * </p>
 * <ul>
 *   <li>简单组件（无复杂条件的）由本类 {@code @Bean} 方法注册，统一带
 *       {@link ConditionalOnMissingBean} 供业务方覆盖</li>
 *   <li>带独立条件的配置/组件类通过 {@link Import} 引入：
 *       {@link SaTokenWebConfig}（过滤器装配）、{@link SaTokenExceptionHandler}
 *       （MVC 层异常）、{@link StpInterfaceImpl}（权限/角色数据）</li>
 * </ul>
 *
 * @author dream
 * @since 2026-08-24
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnClass(name = "cn.dev33.satoken.stp.StpUtil")
@ConditionalOnProperty(prefix = "dream.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({SaTokenWebConfig.class, SaTokenExceptionHandler.class, StpInterfaceImpl.class})
public class SecurityAutoConfiguration {

    /**
     * 注册白名单路径提供者（内置 + 配置 + 缓存逐请求合并）。
     *
     * @param properties 安全配置属性
     * @return SecurityExcludePathProvider 实例
     */
    @Bean
    @ConditionalOnMissingBean(SecurityExcludePathProvider.class)
    public SecurityExcludePathProvider securityExcludePathProvider(SecurityProperties properties) {
        return new SecurityExcludePathProvider(properties);
    }

    /**
     * 注册过滤器阶段异常处理器（SaServletFilter.setError 回调目标）。
     *
     * @param delegate 可选的业务方自定义异常委托，可为 {@code null}
     * @return SaTokenFilterErrorHandler 实例
     */
    @Bean
    @ConditionalOnMissingBean(SaTokenFilterErrorHandler.class)
    public SaTokenFilterErrorHandler saTokenFilterErrorHandler(@Nullable ExceptionHandlerDelegate delegate) {
        return new SaTokenFilterErrorHandler(delegate);
    }

    /**
     * 注册权限校验切面（拦截 {@code @Permission} 注解方法）。
     *
     * @return PermissionAspect 实例
     */
    @Bean
    @ConditionalOnMissingBean(PermissionAspect.class)
    public PermissionAspect permissionAspect() {
        log.info("[Security] 初始化 PermissionAspect 权限校验切面");
        return new PermissionAspect();
    }

    /**
     * 注册防重复提交切面的用户 ID 提供者（security → framework SPI 接线）。
     * <p>
     * 使 {@code @PreventDuplicate(includeUserId = true)} 的 Key 按登录用户隔离；
     * 未登录场景由切面捕获异常后自动降级为无用户段。
     * </p>
     *
     * @return DuplicateUserIdProvider 实例
     */
    @Bean
    @ConditionalOnMissingBean(PreventDuplicateAspect.DuplicateUserIdProvider.class)
    public PreventDuplicateAspect.DuplicateUserIdProvider preventDuplicateUserIdProvider() {
        return SecurityUtils::getUserId;
    }
}
