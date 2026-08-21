package com.dream.common.framework.cache.caffeine;

import com.dream.common.framework.cache.CacheOperations;
import com.dream.common.tool.utils.CachesCaffeineUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Caffeine 的本地缓存实现
 * <p>
 * 适配已封装的 {@link CachesCaffeineUtils}（静态工具类）至 {@link CacheOperations} 契约。
 * 始终可用（Caffeine 依赖位于 dream-common-tool），作为 Redis 不可用时的兜底实现。
 * </p>
 * <p>
 * 本 Bean 始终注册（不使用 {@code @ConditionalOnMissingBean}），
 * 以保证 Redis 在场时仍可被 {@link com.dream.common.framework.cache.CachesUtils} 作为降级后端使用。
 * 当 Redis 在场时，Redis 实现（{@code RedisUtils}）以 {@code @Primary} 成为默认 {@link CacheOperations}。
 * </p>
 *
 * @author dream
 */
@Slf4j
@Component("caffeineCacheOperations")
public class CaffeineCacheOperations implements CacheOperations {

    @Override
    public <T> void set(String key, T value, long expire, TimeUnit timeUnit) {
        CachesCaffeineUtils.set(key, value, expire, timeUnit);
    }

    @Override
    public <T> void set(String key, T value) {
        CachesCaffeineUtils.set(key, value);
    }

    @Override
    public <T> void setNever(String key, T value) {
        CachesCaffeineUtils.setNever(key, value);
    }

    @Override
    public <T> boolean setIfAbsent(String key, T value, long expire, TimeUnit timeUnit) {
        return CachesCaffeineUtils.setIfAbsent(key, value, expire, timeUnit);
    }

    @Override
    public <T> void batchSet(Map<String, T> map, long expire, TimeUnit timeUnit) {
        CachesCaffeineUtils.batchSet(map, expire, timeUnit);
    }

    @Override
    public <T> T get(String key) {
        return CachesCaffeineUtils.get(key);
    }

    @Override
    public <T> T getOrDefault(String key, T defaultValue) {
        return CachesCaffeineUtils.getOrDefault(key, defaultValue);
    }

    @Override
    public <T> T getOrCreate(String key, Supplier<T> supplier, long expire, TimeUnit timeUnit) {
        return CachesCaffeineUtils.getOrCreate(key, supplier, expire, timeUnit);
    }

    @Override
    public <T> Map<String, T> batchGet(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return CachesCaffeineUtils.batchGetExisting(keys);
    }

    @Override
    public boolean delete(String key) {
        boolean exists = CachesCaffeineUtils.contains(key);
        CachesCaffeineUtils.delete(key);
        return true;
    }

    @Override
    public long deleteKeys(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        long count = 0L;
        for (String key : keys) {
            if (CachesCaffeineUtils.contains(key)) {
                count++;
            }
        }
        CachesCaffeineUtils.deleteKeys(keys);
        return count;
    }

    @Override
    public long deletePrefix(String keyPrefix) {
        java.util.List<String> matched = CachesCaffeineUtils.getKeys(keyPrefix);
        long count = matched.size();
        CachesCaffeineUtils.deletePrefix(keyPrefix);
        return count;
    }

    @Override
    public void clear() {
        CachesCaffeineUtils.clear();
    }

    @Override
    public boolean contains(String key) {
        return CachesCaffeineUtils.contains(key);
    }

    @Override
    public long size() {
        return CachesCaffeineUtils.size();
    }

    @Override
    public Collection<String> getKeys(String keyPrefix) {
        return CachesCaffeineUtils.getKeys(keyPrefix);
    }

    @Override
    public String getType() {
        return "CAFFEINE";
    }
}
