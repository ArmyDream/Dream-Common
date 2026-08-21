package com.dream.common.framework.cache.config;

import com.dream.common.framework.cache.CacheOperations;
import com.dream.common.framework.cache.redis.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

/**
 * Redis 缓存自动装配
 * <p>
 * 仅当类路径存在 {@link RedisTemplate}（即引入了 {@code spring-boot-starter-data-redis}）时才装配。
 * </p>
 *
 * <h2>配置项</h2>
 * <p>
 * <b>完全复用 Spring Boot 官方 {@code spring.redis.*} 配置</b>（{@link RedisProperties}），不引入自定义前缀，
 * 方便用户按官方文档配置即可使用。支持单机 / 哨兵 / 集群三种模式。
 * </p>
 *
 * <h3>application.yml 示例</h3>
 * <pre>{@code
 * spring:
 *   redis:
 *     host: 127.0.0.1
 *     port: 6379
 *     database: 0
 *     password:
 *     timeout: 5s
 *     lettuce:
 *       pool:
 *         max-active: 16
 *         max-idle: 8
 *         min-idle: 2
 *         max-wait: 3s
 *         time-between-eviction-runs: 30s
 *     # 哨兵模式
 *     sentinel:
 *       master: mymaster
 *       nodes: 192.168.1.10:26379,192.168.1.11:26379
 *       password:
 *     # 集群模式
 *     cluster:
 *       nodes: 192.168.1.20:6379,192.168.1.21:6379,192.168.1.22:6379
 *       max-redirects: 3
 * }</pre>
 *
 * <h2>合理默认值</h2>
 * <p>
 * 当用户未配置 {@code spring.redis.lettuce.pool.*} 时，自动使用以下生产级默认值：
 * <ul>
 *   <li>max-active: 16</li>
 *   <li>max-idle: 8</li>
 *   <li>min-idle: 2</li>
 *   <li>max-wait: 3s（避免无限等待）</li>
 *   <li>time-between-eviction-runs: 30s</li>
 * </ul>
 * </p>
 *
 * <h2>「不引入不报错」</h2>
 * <p>
 * 本类带 {@link ConditionalOnClass}，Redis 依赖缺失时整个配置类不会被加载。
 * </p>
 *
 * @author dream
 */
@Slf4j
@Configuration
@ConditionalOnClass({RedisTemplate.class, RedisProperties.class})
@EnableConfigurationProperties(RedisProperties.class)
public class RedisCacheAutoConfiguration {

    /** 默认连接池最大活跃连接数 */
    private static final int DEFAULT_MAX_ACTIVE = 16;

    /** 默认连接池最大空闲连接数 */
    private static final int DEFAULT_MAX_IDLE = 8;

    /** 默认连接池最小空闲连接数 */
    private static final int DEFAULT_MIN_IDLE = 2;

    /** 默认获取连接最大等待时间 */
    private static final Duration DEFAULT_MAX_WAIT = Duration.ofSeconds(3L);

    /** 默认空闲连接驱逐检测间隔 */
    private static final Duration DEFAULT_TIME_BETWEEN_EVICTION_RUNS = Duration.ofSeconds(30L);

    /**
     * 构建 Lettuce 连接工厂
     * <p>
     * 基于 {@link RedisProperties}（{@code spring.redis.*}）构建 {@link LettuceConnectionFactory}，
     * 自动适配单机 / 哨兵 / 集群模式。
     * </p>
     * <p>
     * <b>连接池降级</b>：当 commons-pool2 在类路径上时启用连接池（使用 Duration API）；
     * commons-pool2 缺失时自动回退到非池化 Lettuce 连接，避免 {@code NoClassDefFoundError}。
     * </p>
     *
     * @param properties Spring Boot 官方 Redis 配置
     * @return Lettuce 连接工厂
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedisConnectionFactory redisConnectionFactory(RedisProperties properties) {
        Duration commandTimeout = properties.getTimeout() != null
                ? properties.getTimeout()
                : Duration.ofSeconds(5L);
        RedisConfiguration redisConfig = buildRedisConfiguration(properties);

        if (isPoolAvailable()) {
            return buildPooledConnectionFactory(properties, redisConfig, commandTimeout);
        }
        return buildNonPooledConnectionFactory(redisConfig, commandTimeout);
    }

    /**
     * 构建（带连接池的）LettuceConnectionFactory
     *
     * @param properties     Redis 配置
     * @param redisConfig    Redis 部署配置
     * @param commandTimeout 命令超时
     * @return LettuceConnectionFactory
     */
    private LettuceConnectionFactory buildPooledConnectionFactory(RedisProperties properties,
                                                                 RedisConfiguration redisConfig,
                                                                 Duration commandTimeout) {
        GenericObjectPoolConfig<?> poolConfig = buildPoolConfig(properties);
        LettucePoolingClientConfiguration config = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .commandTimeout(commandTimeout)
                .build();
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, config);
        factory.afterPropertiesSet();
        log.info("LettuceConnectionFactory initialized (pooled) | mode={} | pool[maxActive={},maxIdle={},minIdle={}]",
                detectMode(properties), poolConfig.getMaxTotal(),
                poolConfig.getMaxIdle(), poolConfig.getMinIdle());
        return factory;
    }

    /**
     * 构建（非池化的）LettuceConnectionFactory
     * <p>
     * 当 commons-pool2 不在类路径上时使用，与 Spring Boot 默认行为一致。
     * </p>
     *
     * @param redisConfig    Redis 部署配置
     * @param commandTimeout 命令超时
     * @return LettuceConnectionFactory
     */
    private LettuceConnectionFactory buildNonPooledConnectionFactory(RedisConfiguration redisConfig,
                                                                     Duration commandTimeout) {
        org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration config =
                org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.builder()
                        .commandTimeout(commandTimeout)
                        .build();
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, config);
        factory.afterPropertiesSet();
        log.warn("LettuceConnectionFactory initialized (non-pooled, commons-pool2 not found) | "
                + "建议引入 commons-pool2 以启用连接池");
        return factory;
    }

    /**
     * 检查 commons-pool2 是否在类路径上
     *
     * @return true 表示 commons-pool2 可用
     */
    private boolean isPoolAvailable() {
        try {
            Class.forName("org.apache.commons.pool2.impl.GenericObjectPoolConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 根据 {@link RedisProperties} 配置选择对应的 Redis 部署模式构建配置
     *
     * @param properties Spring Boot 官方 Redis 配置
     * @return Spring Data Redis 配置对象
     */
    private RedisConfiguration buildRedisConfiguration(RedisProperties properties) {
        if (properties.getCluster() != null && !isEmpty(properties.getCluster().getNodes())) {
            return buildClusterConfig(properties);
        }
        if (properties.getSentinel() != null && !isEmpty(properties.getSentinel().getNodes())) {
            return buildSentinelConfig(properties);
        }
        return buildStandaloneConfig(properties);
    }

    /**
     * 构建单机模式配置
     *
     * @param properties Spring Boot 官方 Redis 配置
     * @return 单机 Redis 配置
     */
    private RedisStandaloneConfiguration buildStandaloneConfig(RedisProperties properties) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(properties.getHost());
        config.setPort(properties.getPort());
        config.setDatabase(properties.getDatabase());
        if (StringUtils.hasText(properties.getPassword())) {
            config.setPassword(properties.getPassword());
        }
        log.info("Redis standalone config | host={}:{} | db={}",
                properties.getHost(), properties.getPort(), properties.getDatabase());
        return config;
    }

    /**
     * 构建哨兵模式配置
     *
     * @param properties Spring Boot 官方 Redis 配置
     * @return 哨兵 Redis 配置
     */
    private RedisSentinelConfiguration buildSentinelConfig(RedisProperties properties) {
        RedisSentinelConfiguration config = new RedisSentinelConfiguration();
        config.master(properties.getSentinel().getMaster());
        config.setDatabase(properties.getDatabase());
        if (StringUtils.hasText(properties.getPassword())) {
            config.setPassword(properties.getPassword());
        }
        if (StringUtils.hasText(properties.getSentinel().getPassword())) {
            config.setSentinelPassword(properties.getSentinel().getPassword());
        }
        for (String node : properties.getSentinel().getNodes()) {
            String[] parts = splitNode(node);
            config.sentinel(parts[0], Integer.parseInt(parts[1]));
        }
        log.info("Redis sentinel config | master={} | sentinels={}",
                properties.getSentinel().getMaster(),
                properties.getSentinel().getNodes().size());
        return config;
    }

    /**
     * 构建集群模式配置
     *
     * @param properties Spring Boot 官方 Redis 配置
     * @return 集群 Redis 配置
     */
    private RedisClusterConfiguration buildClusterConfig(RedisProperties properties) {
        RedisClusterConfiguration config = new RedisClusterConfiguration();
        for (String node : properties.getCluster().getNodes()) {
            String[] parts = splitNode(node);
            config.clusterNode(parts[0], Integer.parseInt(parts[1]));
        }
        if (properties.getCluster().getMaxRedirects() != null) {
            config.setMaxRedirects(properties.getCluster().getMaxRedirects());
        }
        if (StringUtils.hasText(properties.getPassword())) {
            config.setPassword(properties.getPassword());
        }
        log.info("Redis cluster config | nodes={} | maxRedirects={}",
                properties.getCluster().getNodes().size(),
                properties.getCluster().getMaxRedirects());
        return config;
    }

    /**
     * 构建通用连接池配置
     * <p>
     * 兼容 commons-pool2 2.11.x（Spring Boot 2.7.18 默认）和 2.12+：
     * <ul>
     *   <li>2.12+ 在场时优先使用 {@link Duration} API（{@code setMaxWait} / {@code setTimeBetweenEvictionRuns}）</li>
     *   <li>2.11.x 在场时回退到 millis API（{@code setMaxWaitMillis} / {@code setTimeBetweenEvictionRunsMillis}）</li>
     * </ul>
     * 当用户未配置 {@code spring.redis.lettuce.pool.*} 时使用生产级默认值。
     * </p>
     *
     * @param properties Spring Boot 官方 Redis 配置
     * @return GenericObjectPoolConfig
     */
    @SuppressWarnings("deprecation")
    private GenericObjectPoolConfig<?> buildPoolConfig(RedisProperties properties) {
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        RedisProperties.Pool pool = properties.getLettuce().getPool();

        int maxActive = DEFAULT_MAX_ACTIVE;
        int maxIdle = DEFAULT_MAX_IDLE;
        int minIdle = DEFAULT_MIN_IDLE;
        Duration maxWait = DEFAULT_MAX_WAIT;
        Duration evictionRuns = DEFAULT_TIME_BETWEEN_EVICTION_RUNS;

        if (pool != null) {
            maxActive = pool.getMaxActive() > 0 ? pool.getMaxActive() : DEFAULT_MAX_ACTIVE;
            maxIdle = pool.getMaxIdle() > 0 ? pool.getMaxIdle() : DEFAULT_MAX_IDLE;
            minIdle = pool.getMinIdle() > 0 ? pool.getMinIdle() : DEFAULT_MIN_IDLE;
            if (pool.getMaxWait() != null) {
                maxWait = pool.getMaxWait();
            }
            if (pool.getTimeBetweenEvictionRuns() != null) {
                evictionRuns = pool.getTimeBetweenEvictionRuns();
            }
        }

        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);

        // 兼容 commons-pool2 2.11.x（无 Duration API）和 2.12+（Duration API + deprecated millis API）
        setPoolDuration(poolConfig, "setMaxWait", maxWait, "setMaxWaitMillis", maxWait.toMillis());
        setPoolDuration(poolConfig, "setTimeBetweenEvictionRuns", evictionRuns,
                "setTimeBetweenEvictionRunsMillis", evictionRuns.toMillis());

        poolConfig.setTestWhileIdle(true);
        return poolConfig;
    }

    /**
     * 反射设置连接池 Duration 参数，兼容 commons-pool2 2.11.x 和 2.12+
     * <p>
     * 优先尝试 Duration API（2.12+），不可用时回退到 millis API（2.11.x）。
     * </p>
     *
     * @param config           连接池配置
     * @param durationMethod   Duration API 方法名（如 {@code setMaxWait}）
     * @param durationValue    Duration 值
     * @param millisMethod     millis API 方法名（如 {@code setMaxWaitMillis}）
     * @param millisValue      millis 值
     */
    private void setPoolDuration(GenericObjectPoolConfig<?> config,
                                 String durationMethod, Duration durationValue,
                                 String millisMethod, long millisValue) {
        try {
            java.lang.reflect.Method method = config.getClass().getMethod(durationMethod, Duration.class);
            method.invoke(config, durationValue);
        } catch (NoSuchMethodException e) {
            // commons-pool2 2.11.x 没有 Duration API，回退到 millis
            try {
                java.lang.reflect.Method method = config.getClass().getMethod(millisMethod, long.class);
                method.invoke(config, millisValue);
            } catch (Exception ex) {
                log.warn("Failed to set pool duration: {} / {}", durationMethod, millisMethod, ex);
            }
        } catch (Exception e) {
            log.warn("Failed to set pool duration: {}", durationMethod, e);
        }
    }

    /**
     * 解析节点字符串（{@code host:port} 格式）
     *
     * @param node 节点字符串
     * @return [host, port] 数组
     */
    private String[] splitNode(String node) {
        String[] parts = node.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("无效的 Redis 节点地址: " + node + "，应为 host:port 格式");
        }
        return parts;
    }

    /**
     * 判断节点列表是否为空
     *
     * @param nodes 节点列表
     * @return true 表示为空
     */
    private boolean isEmpty(List<String> nodes) {
        return nodes == null || nodes.isEmpty();
    }

    /**
     * 检测当前部署模式
     *
     * @param properties Redis 配置
     * @return 模式名称
     */
    private String detectMode(RedisProperties properties) {
        if (properties.getCluster() != null && !isEmpty(properties.getCluster().getNodes())) {
            return "CLUSTER";
        }
        if (properties.getSentinel() != null && !isEmpty(properties.getSentinel().getNodes())) {
            return "SENTINEL";
        }
        return "STANDALONE";
    }

    /**
     * 配置统一的 RedisTemplate
     * <p>
     * Key 采用 {@link StringRedisSerializer}；Value 采用 {@link GenericJackson2JsonRedisSerializer}
     * （携带类型信息，便于反序列化为原始对象）。String 内容直出，避免 16 进制乱码。
     * </p>
     *
     * @param connectionFactory Redis 连接工厂
     * @return RedisTemplate 实例
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.setStringSerializer(stringSerializer);
        template.setEnableTransactionSupport(false);
        template.afterPropertiesSet();

        log.info("RedisTemplate<String,Object> initialized (String key + JSON value serializer, factory={})",
                connectionFactory.getClass().getSimpleName());
        return template;
    }

    /**
     * 注册 {@link RedisUtils} 作为 Redis 缓存实现，并以 @Primary 覆盖默认的 Caffeine 兜底
     *
     * @param redisTemplate RedisTemplate
     * @return RedisUtils 实例
     */
    @Bean("redisCacheOperations")
    @Primary
    @ConditionalOnMissingBean(RedisUtils.class)
    public RedisUtils redisUtils(RedisTemplate<String, Object> redisTemplate) {
        log.info("RedisUtils (primary CacheOperations) registered");
        return new RedisUtils(redisTemplate);
    }

    /**
     * 提供默认的 RedisSerializer&lt;Object&gt; 便于其他场景注入
     *
     * @return JSON 序列化器
     */
    @Bean
    @ConditionalOnMissingBean(RedisSerializer.class)
    public RedisSerializer<Object> redisValueSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }
}
