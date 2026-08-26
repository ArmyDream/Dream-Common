package com.dream.common.security.config;

import com.dream.common.security.aspect.PermissionAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 安全模块自动配置
 * <p>
 * 当项目引入 {@code dream-common-security} 依赖且 classpath 中存在 Sa-Token 时，
 * 自动注册 {@link PermissionAspect} 切面 Bean。
 * </p>
 * <p>
 * 可通过配置 {@code dream.security.enabled=false} 关闭权限校验功能。
 * </p>
 *
 * <h2>配置示例</h2>
 * <pre>{@code
 * # application.yml
 * dream:
 *   security:
 *     enabled: true  # 默认 true，设为 false 可关闭权限校验
 * }</pre>
 *
 * @author dream
 * @since 2026-08-24
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "cn.dev33.satoken.stp.StpUtil")
@ConditionalOnProperty(prefix = "dream.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityAutoConfiguration {

    /**
     * 权限校验切面
     * <p>
     * 注册 {@link PermissionAspect} 为 Spring Bean，由 AOP 代理拦截标注
     * {@link com.dream.common.core.annotation.Permission} 的方法。
     * </p>
     *
     * @return 权限校验切面实例
     */
    @Bean
    @ConditionalOnMissingBean(PermissionAspect.class)
    public PermissionAspect permissionAspect() {
        log.info("初始化 PermissionAspect 权限校验切面");
        return new PermissionAspect();
    }
}
