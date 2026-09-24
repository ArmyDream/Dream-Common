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
 * 安全模块自动配置入口。
 * <p>
 * 当 classpath 中存在 Sa-Token 且 {@code dream.security.enabled=true}（默认）时，
 * 通过 {@link Import} 注册以下组件：
 * </p>
 * <ul>
 *   <li>{@link SaTokenWebConfig} —— CorsFilter + SaServletFilter 注册</li>
 *   <li>{@link SaTokenExceptionHandler} —— DispatcherServlet 阶段的 Sa-Token 异常处理</li>
 *   <li>{@link StpInterfaceImpl} —— Sa-Token 权限/角色数据提供者</li>
 *   <li>{@link PermissionAspect} —— {@code @Permission} 注解 AOP 切面（见下方 @Bean）</li>
 * </ul>
 * <p>
 * 本类是模块在 {@code AutoConfiguration.imports} 中的唯一入口，子组件统一由 {@link Import}
 * 引入，避免在 imports 文件中重复登记造成的二次注册。
 * </p>
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
