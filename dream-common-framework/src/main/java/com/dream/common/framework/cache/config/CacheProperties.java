package com.dream.common.framework.cache.config;

import com.dream.common.core.constant.Business;
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
     * 默认过期时间（秒），与 {@code CachesCaffeineUtils.DEFAULT_EXPIRE_TIME} 同源
     * <p>
     * 未绑定 Spring 配置（如静态工具先于容器初始化）时也走这一常量，避免各处再硬编码 180。
     * </p>
     */
    public static final long DEFAULT_EXPIRE_SECONDS = Business.CACHE_EXPIRE_3_MIN;

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
     * <p>
     * 取 {@link #DEFAULT_EXPIRE_SECONDS}，与 {@code CachesCaffeineUtils.DEFAULT_EXPIRE_TIME}
     * 同一来源，避免两处各写 180。
     * </p>
     */
    private long defaultExpire = DEFAULT_EXPIRE_SECONDS;

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
         * 熔断后再探活的默认冷却时间（秒），未绑定配置时使用
         */
        public static final long DEFAULT_HEALTH_COOLDOWN_SECONDS = 30L;

        /**
         * Redis 熔断后再次探活的冷却时间（秒）
         * <p>
         * 探活为<b>被动</b>方式：冷却期结束后的首次真实调用即为探测，成功则回切主缓存，
         * 失败则重新进入冷却。因此不存在独立的主动 PING 通道，也就没有 PING 超时配置项。
         * </p>
         */
        private long healthCooldownSeconds = DEFAULT_HEALTH_COOLDOWN_SECONDS;
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
