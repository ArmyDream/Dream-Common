package com.dream.common.framework.cache.redis;

import com.dream.common.framework.cache.CacheOperations;
import com.dream.common.tool.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 缓存工具类
 * <p>
 * 既实现 {@link CacheOperations} 作为统一缓存入口的 Redis 实现，
 * 又扩展提供 Redis 专属能力（过期、自增自减、分布式锁原语、各类 Ops 操作等）。
 * </p>
 * <p>
 * <b>条件装配</b>：本类不使用 {@code @Component}，而是由
 * {@code RedisCacheAutoConfiguration} 在 Redis 依赖在场时通过 {@code @Bean} 注册，
 * 并以 {@code @ConditionalOnClass(RedisTemplate.class)} 守护，
 * 保证「未引入 spring-boot-starter-data-redis 也不会报错」。
 * </p>
 * <p>
 * 采用 {@link RedisTemplate} 配合 String/JSON 序列化器（由自动配置统一注入）。
 * 所有写操作失败时抛出 {@link DataAccessException}，由上层 {@code CachesUtils} 决定是否降级。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 作为 CacheOperations 注入使用
 * @Autowired
 * private CacheOperations cacheOperations;
 *
 * // 2. 需要 Redis 专属能力时直接注入
 * @Autowired
 * private RedisUtils redisUtils;
 * redisUtils.incr("counter:order", 1);
 * redisUtils.expire("token:abc", 30, TimeUnit.MINUTES);
 * }</pre>
 *
 * @author dream
 */
@Slf4j
public class RedisUtils implements CacheOperations {

    /**
     * Lua 脚本：CAS 自增（仅当 key 存在时自增）
     */
    private static final DefaultRedisScript<Long> INCR_IF_PRESENT_SCRIPT;

    /**
     * SCAN 单次游标批量拉取数量
     */
    private static final long SCAN_BATCH_SIZE = 1000L;

    static {
        INCR_IF_PRESENT_SCRIPT = new DefaultRedisScript<>();
        INCR_IF_PRESENT_SCRIPT.setScriptText(
                "if redis.call('exists', KEYS[1]) == 1 then " +
                        "return redis.call('incrby', KEYS[1], tonumber(ARGV[1])) " +
                        "else return 0 end");
        INCR_IF_PRESENT_SCRIPT.setResultType(Long.class);
    }

    /**
     * 底层 RedisTemplate（String key + JSON value 序列化）
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 构造 Redis 工具
     *
     * @param redisTemplate 由自动配置注入的 RedisTemplate
     */
    public RedisUtils(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        log.info("RedisUtils initialized with RedisTemplate: {}", redisTemplate.getClass().getSimpleName());
    }

    /**
     * 获取底层 RedisTemplate（供高级场景使用）
     *
     * @return RedisTemplate 实例
     */
    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    // ==================== CacheOperations 实现 ====================

    @Override
    public <T> void set(String key, T value, long expire, TimeUnit timeUnit) {
        if (StringUtils.isBlank(key) || value == null) {
            return;
        }
        if (expire > 0L && expire != Long.MAX_VALUE) {
            redisTemplate.opsForValue().set(key, value, expire, timeUnit == null ? TimeUnit.SECONDS : timeUnit);
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
        if (log.isDebugEnabled()) {
            log.debug("[Redis] set key={}, expire={}", key, expire);
        }
    }

    @Override
    public <T> void set(String key, T value) {
        if (StringUtils.isBlank(key) || value == null) {
            return;
        }
        redisTemplate.opsForValue().set(key, value);
        if (log.isDebugEnabled()) {
            log.debug("[Redis] set key={} (never expire)", key);
        }
    }

    @Override
    public <T> void setNever(String key, T value) {
        set(key, value);
    }

    @Override
    public <T> boolean setIfAbsent(String key, T value, long expire, TimeUnit timeUnit) {
        if (StringUtils.isBlank(key) || value == null) {
            return false;
        }
        TimeUnit unit = timeUnit == null ? TimeUnit.SECONDS : timeUnit;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, expire, unit);
        boolean result = Boolean.TRUE.equals(success);
        if (log.isDebugEnabled()) {
            log.debug("[Redis] setIfAbsent key={}, success={}", key, result);
        }
        return result;
    }

    @Override
    public <T> void batchSet(Map<String, T> map, long expire, TimeUnit timeUnit) {
        if (map == null || map.isEmpty()) {
            return;
        }
        TimeUnit unit = timeUnit == null ? TimeUnit.SECONDS : timeUnit;
        map.forEach((k, v) -> {
            if (StringUtils.isNotBlank(k) && v != null) {
                if (expire > 0L && expire != Long.MAX_VALUE) {
                    redisTemplate.opsForValue().set(k, v, expire, unit);
                } else {
                    redisTemplate.opsForValue().set(k, v);
                }
            }
        });
        if (log.isDebugEnabled()) {
            log.debug("[Redis] batchSet count={}", map.size());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        return (T) redisTemplate.opsForValue().get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        if (StringUtils.isBlank(key)) {
            return defaultValue;
        }
        T value = (T) redisTemplate.opsForValue().get(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public <T> T getOrCreate(String key, Supplier<T> supplier, long expire, TimeUnit timeUnit) {
        if (StringUtils.isBlank(key) || supplier == null) {
            return null;
        }
        T value = get(key);
        if (value != null) {
            return value;
        }
        value = supplier.get();
        if (value != null) {
            set(key, value, expire, timeUnit);
        }
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> batchGet(Collection<String> keys) {
        Map<String, T> result = new LinkedHashMap<>();
        if (keys == null || keys.isEmpty()) {
            return result;
        }
        List<String> keyList = keys instanceof List ? (List<String>) keys : new java.util.ArrayList<>(keys);
        List<Object> values = redisTemplate.opsForValue().multiGet(keyList);
        if (values != null) {
            for (int i = 0; i < keyList.size() && i < values.size(); i++) {
                Object v = values.get(i);
                if (v != null) {
                    result.put(keyList.get(i), (T) v);
                }
            }
        }
        return result;
    }

    @Override
    public boolean delete(String key) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        Boolean deleted = redisTemplate.delete(key);
        boolean result = Boolean.TRUE.equals(deleted);
        if (log.isDebugEnabled()) {
            log.debug("[Redis] delete key={}, removed={}", key, result);
        }
        return result;
    }

    @Override
    public long deleteKeys(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        Long count = redisTemplate.delete(keys);
        long result = count == null ? 0L : count;
        if (log.isDebugEnabled()) {
            log.debug("[Redis] deleteKeys count={}, removed={}", keys.size(), result);
        }
        return result;
    }

    @Override
    public long deletePrefix(String keyPrefix) {
        if (StringUtils.isBlank(keyPrefix)) {
            return 0L;
        }
        Set<String> keys = keys(keyPrefix + "*");
        if (keys.isEmpty()) {
            return 0L;
        }
        return deleteKeys(keys);
    }

    @Override
    public void clear() {
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.warn("[Redis] clear all keys, count={}", keys.size());
        }
    }

    @Override
    public boolean contains(String key) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        Boolean has = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(has);
    }

    @Override
    public long size() {
        Set<String> keys = redisTemplate.keys("*");
        return keys == null ? 0L : keys.size();
    }

    @Override
    public Collection<String> getKeys(String keyPrefix) {
        if (StringUtils.isBlank(keyPrefix)) {
            Set<String> all = redisTemplate.keys("*");
            return all == null ? Collections.emptyList() : all;
        }
        return keys(keyPrefix + "*");
    }

    @Override
    public String getType() {
        return "REDIS";
    }

    // ==================== Redis 专属能力 ====================

    /**
     * 设置过期时间
     *
     * @param key      键
     * @param expire   过期时间
     * @param timeUnit 时间单位
     * @return true 成功
     */
    public boolean expire(String key, long expire, TimeUnit timeUnit) {
        if (StringUtils.isBlank(key) || expire <= 0L) {
            return false;
        }
        Boolean ok = redisTemplate.expire(key, expire, timeUnit == null ? TimeUnit.SECONDS : timeUnit);
        return Boolean.TRUE.equals(ok);
    }

    /**
     * 获取剩余过期时间（秒）
     *
     * @param key 键
     * @return 剩余秒数；-1 永久；-2 不存在；null 异常
     */
    public Long getExpireSeconds(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 移除过期时间（持久化）
     *
     * @param key 键
     * @return true 成功
     */
    public boolean persist(String key) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        Boolean ok = redisTemplate.persist(key);
        return Boolean.TRUE.equals(ok);
    }

    /**
     * 自增
     *
     * @param key 键
     * @return 自增后的值
     */
    public long incr(String key) {
        return incr(key, 1L);
    }

    /**
     * 自增指定步长
     *
     * @param key   键
     * @param delta 步长（可为负数实现自减）
     * @return 自增后的值
     */
    public long incr(String key, long delta) {
        if (StringUtils.isBlank(key)) {
            return 0L;
        }
        Long val = redisTemplate.opsForValue().increment(key, delta);
        return val == null ? 0L : val;
    }

    /**
     * 仅当 key 存在时自增（CAS 语义）
     *
     * @param key   键
     * @param delta 步长
     * @return 自增后的值；key 不存在返回 0
     */
    public long incrIfPresent(String key, long delta) {
        if (StringUtils.isBlank(key)) {
            return 0L;
        }
        Long val = redisTemplate.execute(INCR_IF_PRESENT_SCRIPT,
                Collections.singletonList(key), String.valueOf(delta));
        return val == null ? 0L : val;
    }

    /**
     * 自减
     *
     * @param key 键
     * @return 自减后的值
     */
    public long decr(String key) {
        return incr(key, -1L);
    }

    /**
     * 自减指定步长
     *
     * @param key   键
     * @param delta 步长
     * @return 自减后的值
     */
    public long decr(String key, long delta) {
        return incr(key, -delta);
    }

    /**
     * 模糊匹配 key（基于 SCAN 游标，非阻塞，避免阻塞 Redis 主线程）
     * <p>生产环境应使用本方法替代 KEYS 命令。</p>
     *
     * @param pattern 模式，如 {@code user:*}
     * @return 匹配的 key 集合；异常时返回空集合
     */
    public Set<String> keys(String pattern) {
        if (StringUtils.isBlank(pattern)) {
            return Collections.emptySet();
        }
        Set<String> matched = new HashSet<>();
        try {
            @SuppressWarnings("unchecked")
            RedisSerializer<String> keySerializer = (RedisSerializer<String>) redisTemplate.getKeySerializer();
            Set<String> result = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
                Set<String> scanResult = new HashSet<>();
                ScanOptions options = ScanOptions.scanOptions().match(pattern).count(SCAN_BATCH_SIZE).build();
                try (Cursor<byte[]> cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        byte[] keyBytes = cursor.next();
                        String key = keySerializer == null
                                ? new String(keyBytes, StandardCharsets.UTF_8)
                                : keySerializer.deserialize(keyBytes);
                        if (key != null && !key.isEmpty()) {
                            scanResult.add(key);
                        }
                    }
                }
                return scanResult;
            });
            if (result != null) {
                matched.addAll(result);
            }
        } catch (Exception e) {
            log.warn("[Redis] SCAN failed, pattern={}, err={}", pattern, e.getMessage());
        }
        return matched;
    }

    /**
     * 判断 key 是否存在
     *
     * @param key 键
     * @return true 存在
     */
    public boolean hasKey(String key) {
        return contains(key);
    }

    /**
     * 获取当前 DB key 数量
     *
     * @return key 数量；异常返回 -1
     */
    public long dbSize() {
        try {
            Long size = redisTemplate.execute(org.springframework.data.redis.connection.RedisConnection::dbSize);
            return size == null ? -1L : size;
        } catch (Exception e) {
            log.warn("[Redis] dbSize failed: {}", e.getMessage());
            return -1L;
        }
    }

    /**
     * 探活 Redis（执行 PING）
     *
     * @return true 表示 Redis 可连通
     */
    public boolean ping() {
        try {
            String pong = redisTemplate.execute(org.springframework.data.redis.connection.RedisConnection::ping);
            return "PONG".equalsIgnoreCase(pong);
        } catch (Exception e) {
            log.warn("[Redis] ping failed: {}", e.getMessage());
            return false;
        }
    }

    // ==================== Ops 快捷访问 ====================

    /**
     * @return Hash 操作
     */
    public org.springframework.data.redis.core.HashOperations<String, Object, Object> opsForHash() {
        return redisTemplate.opsForHash();
    }

    /**
     * @return List 操作
     */
    public org.springframework.data.redis.core.ListOperations<String, Object> opsForList() {
        return redisTemplate.opsForList();
    }

    /**
     * @return Set 操作
     */
    public org.springframework.data.redis.core.SetOperations<String, Object> opsForSet() {
        return redisTemplate.opsForSet();
    }

    /**
     * @return ZSet 操作
     */
    public ZSetOperations<String, Object> opsForZSet() {
        return redisTemplate.opsForZSet();
    }

    /**
     * @return Value 操作
     */
    public org.springframework.data.redis.core.ValueOperations<String, Object> opsForValue() {
        return redisTemplate.opsForValue();
    }
}
