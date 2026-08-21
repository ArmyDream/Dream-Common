package com.dream.common.framework.cache.config;

import com.dream.common.framework.cache.redis.RedissonUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Redisson 自动装配
 * <p>
 * 仅当类路径存在 {@link RedissonClient}（即引入了 {@code redisson}）时才装配。
 * <b>复用 Spring Boot 官方 {@code spring.redis.*} 配置</b>（{@link RedisProperties}），
 * 保证 Redisson 与 Lettuce 共用同一套连接参数，避免配置漂移。
 * </p>
 *
 * <h2>不冲突说明</h2>
 * <p>
 * 本类不注册 {@code RedissonConnectionFactory}，仅注册 {@link RedissonClient} 和 {@link RedissonUtils}，
 * 避免与 {@link org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory} 产生 Bean 冲突。
 * Redisson 仅用于分布式锁，{@code RedisTemplate} 仍使用 Lettuce 连接工厂。
 * </p>
 *
 * <h2>启用方式</h2>
 * <pre>{@code
 * dream:
 *   cache:
 *     redisson:
 *       enabled: true        # 默认启用
 * spring:
 *   redis:
 *     host: 127.0.0.1        # 单机模式
 *     # sentinel:           # 哨兵模式
 *     #   master: mymaster
 *     #   nodes: 192.168.1.10:26379
 *     # cluster:            # 集群模式
 *     #   nodes: 192.168.1.20:6379
 * }</pre>
 *
 * <h2>「不引入不报错」</h2>
 * <p>
 * 本类带 {@link ConditionalOnClass(RedissonClient.class)}，
 * Redisson 依赖缺失时整个配置类不会被加载，{@link RedissonUtils} 也不会被实例化。
 * </p>
 *
 * @author dream
 */
@Slf4j
@Configuration
@ConditionalOnClass({RedissonClient.class, RedisProperties.class})
@ConditionalOnProperty(prefix = "dream.cache.redisson", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RedisProperties.class)
public class RedissonAutoConfiguration {

    /** 默认连接池大小 */
    private static final int DEFAULT_POOL_SIZE = 64;

    /** 默认最小空闲连接数 */
    private static final int DEFAULT_MIN_IDLE = 10;

    /** 默认连接超时时间（毫秒） */
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10000;

    /** 默认命令超时时间（毫秒） */
    private static final int DEFAULT_TIMEOUT_MS = 3000;

    /**
     * 构建 Redisson 客户端
     * <p>
     * 复用 {@link RedisProperties}（{@code spring.redis.*}）配置，根据哨兵 / 集群 / 单机模式自动适配：
     * <ul>
     *   <li>配置了 {@code spring.redis.cluster.nodes} → {@code useClusterServers()}</li>
     *   <li>配置了 {@code spring.redis.sentinel.nodes} → {@code useSentinelServers()}</li>
     *   <li>其他 → {@code useSingleServer()}</li>
     * </ul>
     * 保证 Redisson 与 Lettuce 连接参数完全一致。
     * </p>
     *
     * @param properties Spring Boot 官方 Redis 配置
     * @return RedissonClient 实例
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient(RedisProperties properties) {
        Config config = new Config();
        int timeoutMs = properties.getTimeout() != null
                ? (int) properties.getTimeout().toMillis()
                : DEFAULT_TIMEOUT_MS;
        int connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;

        if (properties.getCluster() != null && hasNodes(properties.getCluster().getNodes())) {
            applyClusterConfig(config, properties, timeoutMs, connectTimeoutMs);
        } else if (properties.getSentinel() != null && hasNodes(properties.getSentinel().getNodes())) {
            applySentinelConfig(config, properties, timeoutMs, connectTimeoutMs);
        } else {
            applyStandaloneConfig(config, properties, timeoutMs, connectTimeoutMs);
        }

        return Redisson.create(config);
    }

    /**
     * 应用单机模式配置
     *
     * @param config          Redisson 配置
     * @param properties      Spring Boot Redis 配置
     * @param timeoutMs       命令超时（毫秒）
     * @param connectTimeoutMs 连接超时（毫秒）
     */
    private void applyStandaloneConfig(Config config, RedisProperties properties,
                                        int timeoutMs, int connectTimeoutMs) {
        SingleServerConfig serverConfig = config.useSingleServer();
        serverConfig.setAddress("redis://" + properties.getHost() + ":" + properties.getPort());
        serverConfig.setDatabase(properties.getDatabase());
        if (StringUtils.hasText(properties.getPassword())) {
            serverConfig.setPassword(properties.getPassword());
        }
        serverConfig.setConnectionPoolSize(DEFAULT_POOL_SIZE);
        serverConfig.setConnectionMinimumIdleSize(DEFAULT_MIN_IDLE);
        serverConfig.setTimeout(timeoutMs);
        serverConfig.setConnectTimeout(connectTimeoutMs);

        log.info("RedissonClient (standalone) | addr={}:{} | db={} | poolSize={}",
                properties.getHost(), properties.getPort(), properties.getDatabase(), DEFAULT_POOL_SIZE);
    }

    /**
     * 应用哨兵模式配置
     *
     * @param config          Redisson 配置
     * @param properties      Spring Boot Redis 配置
     * @param timeoutMs       命令超时（毫秒）
     * @param connectTimeoutMs 连接超时（毫秒）
     */
    private void applySentinelConfig(Config config, RedisProperties properties,
                                      int timeoutMs, int connectTimeoutMs) {
        SentinelServersConfig serverConfig = config.useSentinelServers();
        serverConfig.setMasterName(properties.getSentinel().getMaster());
        serverConfig.setDatabase(properties.getDatabase());
        if (StringUtils.hasText(properties.getPassword())) {
            serverConfig.setPassword(properties.getPassword());
        }
        if (StringUtils.hasText(properties.getSentinel().getPassword())) {
            serverConfig.setSentinelPassword(properties.getSentinel().getPassword());
        }
        for (String node : properties.getSentinel().getNodes()) {
            serverConfig.addSentinelAddress("redis://" + node);
        }
        serverConfig.setMasterConnectionPoolSize(DEFAULT_POOL_SIZE);
        serverConfig.setSlaveConnectionPoolSize(DEFAULT_POOL_SIZE);
        serverConfig.setMasterConnectionMinimumIdleSize(DEFAULT_MIN_IDLE);
        serverConfig.setSlaveConnectionMinimumIdleSize(DEFAULT_MIN_IDLE);
        serverConfig.setTimeout(timeoutMs);
        serverConfig.setConnectTimeout(connectTimeoutMs);

        log.info("RedissonClient (sentinel) | master={} | sentinels={}",
                properties.getSentinel().getMaster(),
                properties.getSentinel().getNodes().size());
    }

    /**
     * 应用集群模式配置
     *
     * @param config          Redisson 配置
     * @param properties      Spring Boot Redis 配置
     * @param timeoutMs       命令超时（毫秒）
     * @param connectTimeoutMs 连接超时（毫秒）
     */
    private void applyClusterConfig(Config config, RedisProperties properties,
                                     int timeoutMs, int connectTimeoutMs) {
        ClusterServersConfig serverConfig = config.useClusterServers();
        for (String node : properties.getCluster().getNodes()) {
            serverConfig.addNodeAddress("redis://" + node);
        }
        if (StringUtils.hasText(properties.getPassword())) {
            serverConfig.setPassword(properties.getPassword());
        }
        if (properties.getCluster().getMaxRedirects() != null) {
            serverConfig.setRetryAttempts(properties.getCluster().getMaxRedirects());
        }
        serverConfig.setSlaveConnectionPoolSize(DEFAULT_POOL_SIZE);
        serverConfig.setMasterConnectionPoolSize(DEFAULT_POOL_SIZE);
        serverConfig.setSlaveConnectionMinimumIdleSize(DEFAULT_MIN_IDLE);
        serverConfig.setMasterConnectionMinimumIdleSize(DEFAULT_MIN_IDLE);
        serverConfig.setTimeout(timeoutMs);
        serverConfig.setConnectTimeout(connectTimeoutMs);

        log.info("RedissonClient (cluster) | nodes={} | maxRedirects={}",
                properties.getCluster().getNodes().size(),
                properties.getCluster().getMaxRedirects());
    }

    /**
     * 判断节点列表是否非空
     *
     * @param nodes 节点列表
     * @return true 表示有节点
     */
    private boolean hasNodes(List<String> nodes) {
        return nodes != null && !nodes.isEmpty();
    }

    /**
     * 注册 {@link RedissonUtils} 工具 Bean
     *
     * @param redissonClient Redisson 客户端
     * @return RedissonUtils 实例
     */
    @Bean
    @ConditionalOnMissingBean(RedissonUtils.class)
    public RedissonUtils redissonUtils(RedissonClient redissonClient) {
        log.info("RedissonUtils registered");
        return new RedissonUtils(redissonClient);
    }
}
