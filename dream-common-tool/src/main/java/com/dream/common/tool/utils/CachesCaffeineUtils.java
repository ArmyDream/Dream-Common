package com.dream.common.tool.utils;

import com.dream.common.core.constant.Business;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Caffeine 本地缓存工具类
 * <p>
 * 基于 {@link Caffeine} 封装，提供本地高性能缓存能力：
 * <ul>
 *   <li>{@link #set(String, Object, long, TimeUnit)} —— 设置缓存（支持自定义过期时间）</li>
 *   <li>{@link #get(String)} —— 获取缓存值</li>
 *   <li>{@link #getOrDefault(String, Object)} —— 获取缓存（含默认值兜底）</li>
 *   <li>{@link #getOrCreate(String, Supplier, long, TimeUnit)} —— 懒加载获取缓存</li>
 *   <li>{@link #contains(String)} —— 判断缓存是否存在</li>
 *   <li>{@link #delete(String)} / {@link #deleteKeys(Collection)} —— 删除缓存</li>
 *   <li>{@link #deletePrefix(String)} —— 按前缀批量删除</li>
 *   <li>{@link #clear()} —— 清空全部缓存</li>
 * </ul>
 *
 * @author dream
 */
@Slf4j
@SuppressWarnings("all")
public final class CachesCaffeineUtils {

    // ==================== 常量 ====================

    /**
     * 不设置过期时长 -1
     */
    public static final long NOT_EXPIRE = Business.CACHE_NEVER_EXPIRE;

    public static final long CACHE_EXPIRE_MAX = Business.CACHE_EXPIRE_MAX;

    /**
     * 默认过期时间：3 分钟（单位：秒）
     */
    public static final long DEFAULT_EXPIRE_TIME = Business.CACHE_EXPIRE_3_MIN;

    /**
     * 默认时间单位：秒
     */
    public static final TimeUnit DEFAULT_TIME_UNIT = Business.DEFAULT_TIME_UNIT;

    /**
     * 初始容量
     */
    private static final int INITIAL_CAPACITY = Business.CACHE_INITIAL_CAPACITY;

    /**
     * 最大缓存条目数 10 万条
     */
    private static final long MAXIMUM_SIZE = Business.CACHE_MAXIMUM_SIZE;

    private CachesCaffeineUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 缓存实例 ====================

    /**
     * 全局 Caffeine 缓存实例
     * <p>
     * 按 {@link CacheObject} 中设置的过期时间自动失效，初始容量 250，最大 100000 条。
     */
    public static final Cache<String, CacheObject<?>> CAFFEINE = Caffeine.newBuilder()
            .expireAfter(new Expiry<String, CacheObject<?>>() {
                @Override
                public long expireAfterCreate(@NonNull String key, @NonNull CacheObject<?> value, long currentTime) {
                    return value.expire;
                }

                @Override
                public long expireAfterUpdate(@NonNull String key, @NonNull CacheObject<?> value,
                                              long currentTime, @NonNegative long currentDuration) {
                    return value.expire;
                }

                @Override
                public long expireAfterRead(@NonNull String key, @NonNull CacheObject<?> value,
                                            long currentTime, @NonNegative long currentDuration) {
                    // 读取不重置 TTL，保持剩余过期时间，避免热 key 永不过期
                    return currentDuration;
                }
            })
            .initialCapacity(INITIAL_CAPACITY)
            .maximumSize(MAXIMUM_SIZE)
            .build();

    // ==================== 缓存对象封装 ====================

    /**
     * 缓存对象内部封装类
     *
     * @param <T> 缓存值类型
     */
    private static class CacheObject<T> {

        /**
         * 缓存数据
         */
        T data;

        /**
         * 过期时间（纳秒）
         */
        long expire;

        /**
         * 时间单位
         */
        TimeUnit timeUnit;

        CacheObject(T data, long expire, TimeUnit timeUnit) {
            this.data = data;
            this.timeUnit = (timeUnit == null) ? DEFAULT_TIME_UNIT : timeUnit;
            if (expire == -1L) {
                // 永不过期哨兵值
                this.expire = CACHE_EXPIRE_MAX;
            } else if (expire <= 0L) {
                // 非法过期时间：视为立即失效（Caffeine Expiry 要求返回非负值）
                this.expire = 0L;
            } else {
                long nanos = this.timeUnit.toNanos(expire);
                // toNanos 溢出会得到负数，钳制为最大值等同"永不过期"
                this.expire = nanos < 0 ? CACHE_EXPIRE_MAX : nanos;
            }
        }
    }

    // ==================== 设置缓存 ====================

    /**
     * 设置缓存
     *
     * @param key      唯一键
     * @param value    存储对象
     * @param expire   过期时间
     * @param timeUnit 时间单位（null 时默认使用秒）
     * @param <T>      值类型
     */
    public static <T> void set(String key, T value, long expire, TimeUnit timeUnit) {
        if (StringUtils.isBlank(key)) {
            log.warn("设置缓存失败：key 为空");
            return;
        }
        CacheObject<T> cacheObject = new CacheObject<>(value, expire, timeUnit);
        CAFFEINE.put(key, cacheObject);
        if (log.isDebugEnabled()) {
            log.debug("设置缓存成功: key={}, expire={}, timeUnit={}", key, expire, timeUnit != null ? timeUnit : DEFAULT_TIME_UNIT);
        }
    }

    /**
     * 设置缓存（默认单位：秒）
     *
     * @param key    唯一键
     * @param value  存储对象
     * @param expire 过期时间（秒）
     * @param <T>    值类型
     */
    public static <T> void set(String key, T value, long expire) {
        set(key, value, expire, DEFAULT_TIME_UNIT);
    }

    /**
     * 设置缓存（默认 3 分钟过期）
     *
     * @param key   唯一键
     * @param value 存储对象
     * @param <T>   值类型
     */
    public static <T> void set(String key, T value) {
        set(key, value, DEFAULT_EXPIRE_TIME, DEFAULT_TIME_UNIT);
    }

    /**
     * 设置永不过期缓存
     *
     * @param key   唯一键
     * @param value 存储对象
     * @param <T>   值类型
     */
    public static <T> void setNever(String key, T value) {
        set(key, value, NOT_EXPIRE, TimeUnit.NANOSECONDS);
    }

    /**
     * 仅当 key 不存在时设置缓存（防覆盖）
     *
     * @param key      唯一键
     * @param value    存储对象
     * @param expire   过期时间
     * @param timeUnit 时间单位
     * @param <T>      值类型
     * @return true 表示成功设置（之前不存在）；false 表示已存在
     */
    public static <T> boolean setIfAbsent(String key, T value, long expire, TimeUnit timeUnit) {
        if (StringUtils.isBlank(key)) {
            log.warn("设置缓存失败：key 为空");
            return false;
        }
        // asMap().putIfAbsent 为原子操作，避免 contains+set 两步竞态
        CacheObject<?> previous = CAFFEINE.asMap().putIfAbsent(key, new CacheObject<>(value, expire, timeUnit));
        boolean success = previous == null;
        if (log.isDebugEnabled()) {
            log.debug(success ? "缓存设置成功: key={}" : "缓存已存在，跳过设置: key={}", key);
        }
        return success;
    }

    /**
     * 仅当 key 不存在时设置缓存（默认 3 分钟）
     *
     * @param key   唯一键
     * @param value 存储对象
     * @param <T>   值类型
     * @return true 表示成功设置
     */
    public static <T> boolean setIfAbsent(String key, T value) {
        return setIfAbsent(key, value, DEFAULT_EXPIRE_TIME, DEFAULT_TIME_UNIT);
    }

    /**
     * 批量设置缓存（默认 3 分钟）
     *
     * @param map 键值对 Map
     * @param <T> 值类型
     */
    public static <T> void batchSet(Map<String, T> map) {
        batchSet(map, DEFAULT_EXPIRE_TIME, DEFAULT_TIME_UNIT);
    }

    /**
     * 批量设置缓存（指定过期时间）
     *
     * @param map      键值对 Map
     * @param expire   过期时间
     * @param timeUnit 时间单位
     * @param <T>      值类型
     */
    public static <T> void batchSet(Map<String, T> map, long expire, TimeUnit timeUnit) {
        if (map == null || map.isEmpty()) {
            log.warn("批量设置缓存失败：map 为空");
            return;
        }
        for (Map.Entry<String, T> entry : map.entrySet()) {
            set(entry.getKey(), entry.getValue(), expire, timeUnit);
        }
        if (log.isDebugEnabled()) {
            log.debug("批量设置缓存成功: count={}, expire={}", map.size(), expire);
        }
    }

    // ==================== 获取缓存 ====================

    /**
     * 获取缓存值
     *
     * @param key 唯一键
     * @param <T> 值类型
     * @return 缓存值；不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        CacheObject<?> cacheObject = CAFFEINE.getIfPresent(key);
        if (cacheObject == null) {
            if (log.isDebugEnabled()) {
                log.debug("缓存未命中: key={}", key);
            }
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("缓存命中: key={}", key);
        }
        return (T) cacheObject.data;
    }

    /**
     * 获取缓存值（不存在时返回默认值）
     *
     * @param key          唯一键
     * @param defaultValue 默认值
     * @param <T>          值类型
     * @return 缓存值或默认值
     */
    @SuppressWarnings("unchecked")
    public static <T> T getOrDefault(String key, T defaultValue) {
        if (StringUtils.isBlank(key)) {
            return defaultValue;
        }
        CacheObject<?> cacheObject = CAFFEINE.getIfPresent(key);
        if (cacheObject == null) {
            if (log.isDebugEnabled()) {
                log.debug("缓存未命中，返回默认值: key={}", key);
            }
            return defaultValue;
        }
        if (log.isDebugEnabled()) {
            log.debug("缓存命中: key={}", key);
        }
        return (T) cacheObject.data;
    }

    /**
     * 懒加载获取缓存（不存在时调用 supplier 生成并缓存）
     * <pre>
     * User user = CachesUtils.getOrCreate("user:1001", () -> userService.getById(1001), 60, TimeUnit.SECONDS);
     * </pre>
     *
     * @param key      唯一键
     * @param supplier 值生成器（仅在缓存不存在时调用）
     * @param expire   过期时间
     * @param timeUnit 时间单位
     * @param <T>      值类型
     * @return 缓存值或新生成的值
     */
    @SuppressWarnings("unchecked")
    public static <T> T getOrCreate(String key, Supplier<T> supplier, long expire, TimeUnit timeUnit) {
        if (StringUtils.isBlank(key) || supplier == null) {
            log.warn("获取或创建缓存失败：key 或 supplier 为空");
            return null;
        }
        CacheObject<?> existing = CAFFEINE.getIfPresent(key);
        if (existing != null) {
            if (log.isDebugEnabled()) {
                log.debug("缓存命中，直接返回: key={}", key);
            }
            return (T) existing.data;
        }
        // asMap().computeIfAbsent 保证同一 key 的加载互斥，避免缓存击穿导致 supplier 重复执行
        CacheObject<T> created = (CacheObject<T>) CAFFEINE.asMap().computeIfAbsent(key, k -> {
            log.debug("缓存未命中，执行 supplier 加载: key={}", k);
            T value = supplier.get();
            return value == null ? null : new CacheObject<>(value, expire, timeUnit);
        });
        if (created == null) {
            log.warn("缓存加载失败，supplier 返回 null: key={}", key);
            return null;
        }
        return created.data;
    }

    /**
     * 懒加载获取缓存（默认 3 分钟）
     *
     * @param key      唯一键
     * @param supplier 值生成器
     * @param <T>      值类型
     * @return 缓存值或新生成的值
     */
    public static <T> T getOrCreate(String key, Supplier<T> supplier) {
        return getOrCreate(key, supplier, DEFAULT_EXPIRE_TIME, DEFAULT_TIME_UNIT);
    }

    /**
     * 批量获取缓存值
     *
     * @param keys key 集合
     * @param <T>  值类型
     * @return key → value 的 LinkedHashMap（保持顺序，不存在的 key 值为 null）
     */
    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> batchGet(Collection<String> keys) {
        Map<String, T> result = new LinkedHashMap<>();
        if (keys == null || keys.isEmpty()) {
            return result;
        }
        for (String key : keys) {
            T value = get(key);
            result.put(key, value);
        }
        if (log.isDebugEnabled()) {
            long hitCount = result.values().stream().filter(Objects::nonNull).count();
            log.debug("批量获取缓存完成: total={}, hit={}, miss={}", keys.size(), hitCount, keys.size() - hitCount);
        }
        return result;
    }

    /**
     * 批量获取缓存值（仅返回存在的条目）
     *
     * @param keys key 集合
     * @param <T>  值类型
     * @return key → value 的 LinkedHashMap（仅含存在的 key）
     */
    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> batchGetExisting(Collection<String> keys) {
        Map<String, T> result = new LinkedHashMap<>();
        if (keys == null || keys.isEmpty()) {
            return result;
        }
        for (String key : keys) {
            CacheObject<?> cacheObject = CAFFEINE.getIfPresent(key);
            if (cacheObject != null) {
                result.put(key, (T) cacheObject.data);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("批量获取缓存（仅存在）完成: total={}, found={}", keys.size(), result.size());
        }
        return result;
    }

    // ==================== 缓存信息 ====================

    /**
     * 判断缓存是否存在
     *
     * @param key 唯一键
     * @return true 表示存在
     */
    public static boolean contains(String key) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        boolean exists = CAFFEINE.getIfPresent(key) != null;
        if (log.isDebugEnabled()) {
            log.debug("缓存存在检查: key={}, exists={}", key, exists);
        }
        return exists;
    }

    /**
     * 获取缓存条目数（估计值，基于 caffeine 的 statisticsSize）
     *
     * @return 缓存条目数
     */
    public static long size() {
        long size = CAFFEINE.estimatedSize();
        if (log.isDebugEnabled()) {
            log.debug("获取缓存条目数: size={}", size);
        }
        return size;
    }

    /**
     * 判断缓存是否为空
     *
     * @return true 表示缓存中没有任何条目
     */
    public static boolean isEmpty() {
        return CAFFEINE.estimatedSize() <= 0;
    }

    /**
     * 获取所有缓存 key
     *
     * @return key 列表
     */
    public static List<String> getKeys() {
        List<String> keys = new ArrayList<>(CAFFEINE.asMap().keySet());
        if (log.isDebugEnabled()) {
            log.debug("获取所有缓存 key: count={}", keys.size());
        }
        return keys;
    }

    /**
     * 按前缀获取缓存 key
     * <pre>
     * getKeys("user:") → ["user:1001", "user:1002", ...]
     * </pre>
     *
     * @param keyPrefix key 前缀；为空则返回全部 key
     * @return 匹配的 key 列表
     */
    public static List<String> getKeys(String keyPrefix) {
        if (StringUtils.isBlank(keyPrefix)) {
            return getKeys();
        }
        List<String> keys = new ArrayList<>();
        CAFFEINE.asMap().forEach((key, value) -> {
            if (key.startsWith(keyPrefix)) {
                keys.add(key);
            }
        });
        if (log.isDebugEnabled()) {
            log.debug("按前缀获取缓存 key: prefix={}, count={}", keyPrefix, keys.size());
        }
        return keys;
    }

    // ==================== 删除缓存 ====================

    /**
     * 删除单个缓存
     *
     * @param key 唯一键
     */
    public static void delete(String key) {
        if (StringUtils.isBlank(key)) {
            log.warn("删除缓存失败：key 为空");
            return;
        }
        CAFFEINE.invalidate(key);
        if (log.isDebugEnabled()) {
            log.debug("删除缓存成功: key={}", key);
        }
    }

    /**
     * 批量删除缓存（可变参数）
     *
     * @param keys 要删除的 key
     */
    public static void deleteKeys(String... keys) {
        if (keys == null || keys.length == 0) {
            log.warn("批量删除缓存失败：keys 为空");
            return;
        }
        CAFFEINE.invalidateAll(Arrays.asList(keys));
        if (log.isDebugEnabled()) {
            log.debug("批量删除缓存成功: count={}", keys.length);
        }
    }

    /**
     * 批量删除缓存（Collection）
     *
     * @param keys 要删除的 key 集合
     */
    public static void deleteKeys(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            log.warn("批量删除缓存失败：keys 为空");
            return;
        }
        CAFFEINE.invalidateAll(keys);
        if (log.isDebugEnabled()) {
            log.debug("批量删除缓存成功: count={}", keys.size());
        }
    }

    /**
     * 按前缀批量删除缓存
     * <pre>
     * deletePrefix("user:") → 删除所有以 "user:" 开头的缓存
     * </pre>
     *
     * @param keyPrefix key 前缀
     */
    public static void deletePrefix(String keyPrefix) {
        if (StringUtils.isBlank(keyPrefix)) {
            log.warn("按前缀删除缓存失败：keyPrefix 为空");
            return;
        }
        final List<String> keys = getKeys(keyPrefix);
        if (!keys.isEmpty()) {
            CAFFEINE.invalidateAll(keys);
            if (log.isDebugEnabled()) {
                log.debug("按前缀删除缓存成功: prefix={}, count={}", keyPrefix, keys.size());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("按前缀删除缓存: 无匹配缓存, prefix={}", keyPrefix);
            }
        }
    }

    /**
     * 清空全部缓存
     */
    public static void clear() {
        long beforeSize = CAFFEINE.estimatedSize();
        CAFFEINE.invalidateAll();
        if (log.isDebugEnabled()) {
            log.debug("清空全部缓存成功: beforeSize={}", beforeSize);
        }
    }

    // ==================== 统计与维护 ====================

    /**
     * 执行清理维护（触发过期条目的清除）
     * <p>
     * Caffeine 默认在每次写操作后触发清理，
     * 此方法可在需要时主动触发（如定时任务调用）。
     */
    public static void cleanUp() {
        long beforeSize = CAFFEINE.estimatedSize();
        CAFFEINE.cleanUp();
        long afterSize = CAFFEINE.estimatedSize();
        if (log.isDebugEnabled() && beforeSize != afterSize) {
            log.debug("执行缓存清理: beforeSize={}, afterSize={}, removed={}", beforeSize, afterSize, beforeSize - afterSize);
        }
    }

    /**
     * 获取底层 ConcurrentMap（用于高级操作）
     *
     * @return 缓存的 ConcurrentMap 视图
     */
    public static ConcurrentMap<String, CacheObject<?>> asMap() {
        if (log.isDebugEnabled()) {
            log.debug("获取底层 ConcurrentMap");
        }
        return CAFFEINE.asMap();
    }
}