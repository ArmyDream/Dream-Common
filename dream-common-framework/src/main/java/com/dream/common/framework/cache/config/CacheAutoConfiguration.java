package com.dream.common.framework.cache.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 统一缓存自动装配入口
 * <p>
 * 启用 {@link CacheProperties} 配置绑定，并声明缓存模块基础装配顺序。
 * </p>
 * <p>
 * <b>装配关系</b>
 * <ul>
 *   <li>{@link CacheProperties} —— 由 {@link EnableConfigurationProperties} 注册为 Bean</li>
 *   <li>{@link com.dream.common.framework.cache.caffeine.CaffeineCacheOperations} —— 始终注册（兜底）</li>
 *   <li>{@link com.dream.common.framework.cache.redis.RedisUtils} —— 由 {@link RedisCacheAutoConfiguration}
 *       在 Redis 依赖在场时按 {@code @ConditionalOnClass} 注册，并以 {@code @Primary} 优先</li>
 *   <li>{@link com.dream.common.framework.cache.CachesUtils} —— 对外统一入口，运行期熔断 + 降级</li>
 *   <li>{@link com.dream.common.framework.cache.aspect.CacheDoubleDeleteAspect} —— 延时双删切面</li>
 * </ul>
 * </p>
 * <p>
 * <b>「不引入不报错」</b>：本类不引入任何 Redis 类型；Redis 相关 Bean 在
 * {@link RedisCacheAutoConfiguration}（独立类，带 {@code @ConditionalOnClass}）中装配，
 * Redis 依赖缺失时该类不会被加载。
 * </p>
 *
 * @author dream
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {

    /**
     * 启动日志
     */
    public CacheAutoConfiguration() {
        log.info("CacheAutoConfiguration initialized (dream.cache.*)");
    }
}
