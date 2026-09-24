package com.dream.common.framework.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 启动 Banner 自动装配。
 * <p>
 * 通过 {@code spring.banner.custom.enabled=false} 关闭（默认开启）。
 * 以 Bean 形式注册而非依赖组件扫描，保证下游不扫描 {@code com.dream} 包时同样生效。
 * </p>
 *
 * @author dream
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.banner.custom.enabled", havingValue = "true", matchIfMissing = true)
public class BannerAutoConfiguration {

    /**
     * 自定义 Dream Banner 打印器
     *
     * @return ApplicationRunner 实例
     */
    @Bean
    @ConditionalOnMissingBean(CustomBannerRunner.class)
    public CustomBannerRunner customBannerRunner(Environment environment) {
        return new CustomBannerRunner(environment);
    }
}
