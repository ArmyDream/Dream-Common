package com.dream.common.framework.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

/**
 * 缓存统一配置属性
 * <p>
 * 前缀 {@code dream.cache}，控制统一缓存入口（{@link com.dream.common.framework.cache.CachesUtils}）
 * 的降级行为、默认过期时间以及延时双删相关参数。
 * </p>
 *
 * <h2>application.yml 示例</h2>
 * <pre>{@code
 * dream:
 *   cache:
 *     type: AUTO                  # AUTO / REDIS / CAFFEINE
 *     fallback-enabled: true      # Redis 不可用是否降级到 Caffeine
 *     default-expire: 180         # 默认过期时间（秒）
 *     redis:
 *       health-cooldown-seconds: 30   # Redis 熔断后再次探活的冷却时间
 *     double-delete:
 *       enabled: true             # 是否启用注解延时双删
 *       default-delay-ms: 500     # 默认二次删除延时（毫秒）
 * }</pre>
 *
 * @author dream
 */
@Data
@ConfigurationProperties(prefix = "dream.cache")
public class CacheProperties {

    /**
     * 缓存类型选择
     * <ul>
     *   <li>{@code AUTO}：自动（Redis 在场优先，不可用降级 Caffeine）—— 默认</li>
     *   <li>{@code REDIS}：强制使用 Redis（Redis 不可用直接报错 / 不降级）</li>
     *   <li>{@code CAFFEINE}：强制使用本地 Caffeine</li>
     * </ul>
     */
    private CacheType type = CacheType.AUTO;

    /**
     * Redis 不可用时是否降级到 Caffeine（仅 type=AUTO 生效）
     */
    private boolean fallbackEnabled = true;

    /**
     * 默认过期时间（秒）
     */
    private long defaultExpire = 180L;

    /**
     * Redis 相关配置
     */
    private Redis redis = new Redis();

    /**
     * 延时双删相关配置
     */
    private DoubleDelete doubleDelete = new DoubleDelete();

    /**
     * 默认时间单位（秒）
     *
     * @return 秒
     */
    public TimeUnit getDefaultTimeUnit() {
        return TimeUnit.SECONDS;
    }

    /**
     * 缓存类型枚举
     *
     * @author dream
     */
    public enum CacheType {
        /**
         * 自动：Redis 在场优先，不可用降级 Caffeine
         */
        AUTO,
        /**
         * 强制 Redis
         */
        REDIS,
        /**
         * 强制 Caffeine
         */
        CAFFEINE
    }

    /**
     * Redis 子配置
     *
     * @author dream
     */
    @Data
    public static class Redis {

        /**
         * Redis 熔断后再次探活的冷却时间（秒）
         */
        private long healthCooldownSeconds = 30L;

        /**
         * 探活时执行的 PING 超时时间（毫秒）
         */
        private long pingTimeoutMs = 1000L;
    }

    /**
     * 延时双删子配置
     *
     * @author dream
     */
    @Data
    public static class DoubleDelete {

        /**
         * 是否启用注解延时双删
         */
        private boolean enabled = true;

        /**
         * 默认二次删除延时（毫秒）
         */
        private long defaultDelayMs = 500L;
    }
}
