package com.dream.common.security.config;

import com.dream.common.security.aspect.PermissionAspect;
import com.dream.common.security.handler.SaTokenExceptionHandler;
import com.dream.common.security.provider.StpInterfaceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 安全模块自动配置类。
 * <p>当 classpath 中存在 Sa-Token 且 {@code dream.security.enabled=true} 时，自动注册以下组件：</p>
 * <ul>
 *   <li>{@link PermissionAspect} 权限校验切面</li>
 *   <li>{@link SaTokenWebConfig} 过滤器配置（CorsFilter + SaServletFilter）</li>
 *   <li>{@link SaTokenExceptionHandler} 异常处理器</li>
 *   <li>{@link StpInterfaceImpl} 权限数据提供者</li>
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
     * 注册权限校验切面（仅当用户未自定义时）。
     *
     * @return PermissionAspect 实例
     */
    @Bean
    @ConditionalOnMissingBean(PermissionAspect.class)
    public PermissionAspect permissionAspect() {
        log.info("[Security] 初始化 PermissionAspect 权限校验切面");
        return new PermissionAspect();
    }
}