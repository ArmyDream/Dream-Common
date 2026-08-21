package com.dream.common.framework.cache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 统一缓存操作契约
 * <p>
 * 屏蔽底层缓存实现差异（Redis / Caffeine / 其他），为上层提供一致的读写删接口。
 * 实现方包括但不限于：
 * <ul>
 *   <li>{@link com.dream.common.framework.cache.caffeine.CaffeineCacheOperations} —— 基于 Caffeine 的本地缓存</li>
 *   <li>{@code RedisCacheOperations}（Redis 在场时由 {@code RedisUtils} 实现）—— 基于分布式缓存</li>
 * </ul>
 * </p>
 * <p>
 * 本接口刻意不引入任何 Redis 相关类型，以保证「未引入 Redis 依赖也不报错」，
 * 由 {@link CachesUtils} 作为对外统一入口，按可用性自动选择实现并在 Redis 不可用时降级到 Caffeine。
 * </p>
 *
 * @author dream
 */
public interface CacheOperations {

    // ==================== 写入 ====================

    /**
     * 设置缓存
     *
     * @param key      唯一键；为空则忽略
     * @param value    存储对象
     * @param expire   过期时间；{@code <=0} 或 {@link Long#MAX_VALUE} 视为永不过期（受实现限制）
     * @param timeUnit 时间单位；为 null 时默认秒
     * @param <T>      值类型
     */
    <T> void set(String key, T value, long expire, TimeUnit timeUnit);

    /**
     * 设置缓存（单位：秒）
     *
     * @param key    唯一键
     * @param value  存储对象
     * @param expire 过期时间（秒）
     * @param <T>    值类型
     */
    default <T> void set(String key, T value, long expire) {
        set(key, value, expire, TimeUnit.SECONDS);
    }

    /**
     * 设置缓存（默认过期时间，由实现决定）
     *
     * @param key   唯一键
     * @param value 存储对象
     * @param <T>   值类型
     */
    <T> void set(String key, T value);

    /**
     * 设置永不过期缓存
     *
     * @param key   唯一键
     * @param value 存储对象
     * @param <T>   值类型
     */
    <T> void setNever(String key, T value);

    /**
     * 仅当 key 不存在时设置缓存（防覆盖）
     *
     * @param key      唯一键
     * @param value    存储对象
     * @param expire   过期时间
     * @param timeUnit 时间单位
     * @param <T>      值类型
     * @return true 表示成功设置；false 表示已存在
     */
    <T> boolean setIfAbsent(String key, T value, long expire, TimeUnit timeUnit);

    /**
     * 批量设置缓存
     *
     * @param map      键值对
     * @param expire   过期时间
     * @param timeUnit 时间单位
     * @param <T>      值类型
     */
    <T> void batchSet(Map<String, T> map, long expire, TimeUnit timeUnit);

    // ==================== 读取 ====================

    /**
     * 获取缓存值
     *
     * @param key 唯一键
     * @param <T> 值类型
     * @return 缓存值；不存在返回 null
     */
    <T> T get(String key);

    /**
     * 获取缓存值，不存在返回默认值
     *
     * @param key          唯一键
     * @param defaultValue 默认值
     * @param <T>          值类型
     * @return 缓存值或默认值
     */
    <T> T getOrDefault(String key, T defaultValue);

    /**
     * 懒加载获取缓存（不存在时调用 supplier 生成并缓存）
     *
     * @param key      唯一键
     * @param supplier 值生成器
     * @param expire   过期时间
     * @param timeUnit 时间单位
     * @param <T>      值类型
     * @return 缓存值或新生成的值
     */
    <T> T getOrCreate(String key, Supplier<T> supplier, long expire, TimeUnit timeUnit);

    /**
     * 批量获取缓存值（仅返回存在的条目）
     *
     * @param keys key 集合
     * @param <T>  值类型
     * @return key → value 映射（仅含命中的 key）
     */
    <T> Map<String, T> batchGet(Collection<String> keys);

    // ==================== 删除 ====================

    /**
     * 删除单个缓存
     *
     * @param key 唯一键
     * @return true 表示删除成功（或原本不存在）
     */
    boolean delete(String key);

    /**
     * 批量删除缓存
     *
     * @param keys key 集合
     * @return 实际删除条数
     */
    long deleteKeys(Collection<String> keys);

    /**
     * 按前缀批量删除缓存
     *
     * @param keyPrefix key 前缀
     * @return 实际删除条数
     */
    long deletePrefix(String keyPrefix);

    /**
     * 清空全部缓存
     */
    void clear();

    // ==================== 信息 ====================

    /**
     * 判断缓存是否存在
     *
     * @param key 唯一键
     * @return true 表示存在
     */
    boolean contains(String key);

    /**
     * 获取缓存条目数（估计值）
     *
     * @return 缓存条目数
     */
    long size();

    /**
     * 按前缀获取缓存 key
     *
     * @param keyPrefix key 前缀；为空返回全部
     * @return 匹配的 key 列表
     */
    Collection<String> getKeys(String keyPrefix);

    /**
     * 返回底层实现类型标识（用于日志 / 诊断）
     *
     * @return 实现类型名称，如 {@code "REDIS"} / {@code "CAFFEINE"}
     */
    String getType();
}
