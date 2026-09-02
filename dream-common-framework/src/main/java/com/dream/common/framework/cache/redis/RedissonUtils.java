package com.dream.common.framework.cache.redis;

import com.dream.common.core.enums.LockType;
import com.dream.common.tool.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 分布式锁工具类
 * <p>
 * 基于 {@link RedissonClient} 封装，提供常用分布式锁能力，屏蔽底层 API 细节：
 * <ul>
 *   <li>{@link #tryLock(String, long, TimeUnit, Supplier)} —— 加锁执行（自动释放）</li>
 *   <li>{@link #lock(String, Supplier)} —— 阻塞加锁执行</li>
 *   <li>{@link #tryLock(String, long, TimeUnit)} —— 获取锁句柄</li>
 *   <li>{@link #tryFairLock(String, long, TimeUnit, Supplier)} —— 公平锁执行</li>
 *   <li>{@link #tryReadLock(String, long, TimeUnit, Supplier)} / {@link #tryWriteLock(String, long, TimeUnit, Supplier)} —— 读写锁</li>
 *   <li>{@link #tryMultiLock(long, TimeUnit, Supplier, String...)} —— 联锁（多 Key 同时锁定）</li>
 *   <li>{@link #semaphore(String, int, long, TimeUnit, Supplier)} —— 信号量</li>
 *   <li>{@link #countDownLatch(String, int, Supplier)} —— 闭锁</li>
 *   <li>{@link #isLocked(String)} / {@link #forceUnlock(String)} —— 锁状态查询 / 强制释放</li>
 * </ul>
 * </p>
 *
 * <h2>「不引入不报错」</h2>
 * <p>
 * 本类不使用 {@code @Component}，而是由 {@code RedissonAutoConfiguration} 在 Redisson 依赖在场时
 * 通过 {@code @Bean} 注册，并以 {@code @ConditionalOnClass(RedissonClient.class)} 守护，
 * 保证「未引入 redisson-spring-boot-starter 也不会报错」。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. try-with-success 风格：获取锁失败抛出异常
 * String result = RedissonUtils.tryLock("order:lock:1001", 5, TimeUnit.SECONDS, () -> {
 *     // 业务逻辑
 *     return "ok";
 * });
 *
 * // 2. 获取锁失败返回 null
 * String v = RedissonUtils.tryLock("order:lock:1001", 5, TimeUnit.SECONDS, () -> "ok", null);
 *
 * // 3. 公平锁
 * RedissonUtils.tryFairLock("order:fair:1001", 5, TimeUnit.SECONDS, () -> doBiz());
 *
 * // 4. 联锁（同时锁多个资源）
 * RedissonUtils.tryMultiLock(5, TimeUnit.SECONDS, () -> transfer(a, b), "acct:A", "acct:B");
 * }</pre>
 *
 * @author dream
 */
@Slf4j
public class RedissonUtils {

    /** 默认等待时间（秒） */
    public static final long DEFAULT_WAIT_SECONDS = 10L;

    /** 默认租约时间（秒），-1 表示 Redisson 默认（30s 并启用 watchdog 自动续期） */
    public static final long DEFAULT_LEASE_SECONDS = -1L;

    /** 底层 Redisson 客户端 */
    private final RedissonClient redissonClient;

    /**
     * 构造 Redisson 工具
     *
     * @param redissonClient 由自动配置注入的 RedissonClient
     */
    public RedissonUtils(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        log.info("RedissonUtils initialized with RedissonClient: {}", redissonClient.getClass().getSimpleName());
    }

    /**
     * 获取底层 Redisson 客户端（供高级场景使用）
     *
     * @return RedissonClient 实例
     */
    public RedissonClient getRedissonClient() {
        return redissonClient;
    }

    // ==================== 可重入锁 ====================

    /**
     * 尝试获取可重入锁并执行业务（获取失败抛出 {@link IllegalStateException}）
     * <p>
     * 默认租约时间 -1，启用 Redisson watchdog 自动续期（默认 30s，每 10s 续期一次）。
     * </p>
     *
     * @param key       锁 Key
     * @param waitTime  等待获取锁的最长时间
     * @param unit      时间单位
     * @param supplier  持锁期间执行的业务
     * @param <T>       返回类型
     * @return 业务返回值
     * @throws IllegalStateException 获取锁失败
     */
    public <T> T tryLock(String key, long waitTime, TimeUnit unit, Supplier<T> supplier) {
        return tryLock(key, waitTime, DEFAULT_LEASE_SECONDS, unit, supplier,
                () -> { throw new IllegalStateException("获取分布式锁失败: " + key); });
    }

    /**
     * 尝试获取可重入锁并执行业务（获取失败返回默认值）
     *
     * @param key          锁 Key
     * @param waitTime     等待获取锁的最长时间
     * @param unit         时间单位
     * @param supplier     持锁期间执行的业务
     * @param defaultValue 获取失败时的返回值
     * @param <T>          返回类型
     * @return 业务返回值或 defaultValue
     */
    public <T> T tryLock(String key, long waitTime, TimeUnit unit, Supplier<T> supplier, T defaultValue) {
        return tryLock(key, waitTime, DEFAULT_LEASE_SECONDS, unit, supplier, () -> defaultValue);
    }

    /**
     * 尝试获取可重入锁并执行业务（完整参数）
     *
     * @param key           锁 Key
     * @param waitTime      等待获取锁的最长时间
     * @param leaseTime     持锁最长时间（&lt;=0 表示启用 watchdog 自动续期）
     * @param unit          时间单位
     * @param supplier      持锁期间执行的业务
     * @param fallback      获取失败时调用的兜底逻辑
     * @param <T>           返回类型
     * @return 业务返回值或兜底返回值
     */
    public <T> T tryLock(String key, long waitTime, long leaseTime, TimeUnit unit,
                         Supplier<T> supplier, Supplier<T> fallback) {
        if (StringUtils.isBlank(key)) {
            return fallback.get();
        }
        RLock lock = redissonClient.getLock(key);
        boolean locked = false;
        try {
            locked = (leaseTime > 0L)
                    ? lock.tryLock(waitTime, leaseTime, unit)
                    : lock.tryLock(waitTime, unit);
            if (!locked) {
                log.warn("[Redisson] 获取锁失败（已等待 {}{}） | key={}", waitTime, unit.toString().toLowerCase(), key);
                return fallback.get();
            }
            if (log.isDebugEnabled()) {
                log.debug("[Redisson] 获取锁成功 | key={} | lease={}", key, leaseTime);
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[Redisson] 获取锁被中断 | key={} | err={}", key, e.getMessage());
            return fallback.get();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                if (log.isDebugEnabled()) {
                    log.debug("[Redisson] 释放锁 | key={}", key);
                }
            }
        }
    }

    /**
     * 阻塞获取可重入锁并执行业务（无超时等待）
     * <p>
     * 默认租约时间 -1（启用 watchdog 自动续期）。
     * </p>
     *
     * @param key      锁 Key
     * @param supplier 持锁期间执行的业务
     * @param <T>      返回类型
     * @return 业务返回值
     */
    public <T> T lock(String key, Supplier<T> supplier) {
        return lock(key, DEFAULT_LEASE_SECONDS, TimeUnit.SECONDS, supplier);
    }

    /**
     * 阻塞获取可重入锁并执行业务（指定租约时间）
     *
     * @param key       锁 Key
     * @param leaseTime 持锁最长时间（&lt;=0 启用 watchdog）
     * @param unit      时间单位
     * @param supplier  持锁期间执行的业务
     * @param <T>       返回类型
     * @return 业务返回值
     */
    public <T> T lock(String key, long leaseTime, TimeUnit unit, Supplier<T> supplier) {
        if (StringUtils.isBlank(key)) {
            return supplier.get();
        }
        RLock lock = redissonClient.getLock(key);
        if (leaseTime > 0L) {
            lock.lock(leaseTime, unit);
        } else {
            lock.lock();
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("[Redisson] 阻塞获取锁成功 | key={} | lease={}", key, leaseTime);
            }
            return supplier.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                if (log.isDebugEnabled()) {
                    log.debug("[Redisson] 释放锁 | key={}", key);
                }
            }
        }
    }

    /**
     * 获取可重入锁句柄（不自动释放，需调用方手动 {@link RLock#unlock()}）
     * <p>
     * 推荐优先使用 {@link #tryLock(String, long, TimeUnit, Supplier)} 系列自动管理版本。
     * </p>
     *
     * @param key      锁 Key
     * @param waitTime 等待时间
     * @param unit     时间单位
     * @return 锁句柄；获取失败返回 null
     */
    public RLock tryLock(String key, long waitTime, TimeUnit unit) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        RLock lock = redissonClient.getLock(key);
        try {
            boolean ok = lock.tryLock(waitTime, unit);
            return ok ? lock : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    // ==================== 公平锁 ====================

    /**
     * 尝试获取公平锁并执行业务（获取失败抛出 {@link IllegalStateException}）
     *
     * @param key      锁 Key
     * @param waitTime 等待时间
     * @param unit     时间单位
     * @param supplier 业务逻辑
     * @param <T>      返回类型
     * @return 业务返回值
     */
    public <T> T tryFairLock(String key, long waitTime, TimeUnit unit, Supplier<T> supplier) {
        return tryFairLock(key, waitTime, DEFAULT_LEASE_SECONDS, unit, supplier,
                () -> { throw new IllegalStateException("获取公平锁失败: " + key); });
    }

    /**
     * 尝试获取公平锁并执行业务（完整参数）
     *
     * @param key       锁 Key
     * @param waitTime  等待时间
     * @param leaseTime 租约时间（&lt;=0 启用 watchdog）
     * @param unit      时间单位
     * @param supplier  业务逻辑
     * @param fallback  获取失败兜底
     * @param <T>       返回类型
     * @return 业务返回值或兜底返回值
     */
    public <T> T tryFairLock(String key, long waitTime, long leaseTime, TimeUnit unit,
                              Supplier<T> supplier, Supplier<T> fallback) {
        if (StringUtils.isBlank(key)) {
            return fallback.get();
        }
        RLock lock = redissonClient.getFairLock(key);
        boolean locked = false;
        try {
            locked = (leaseTime > 0L)
                    ? lock.tryLock(waitTime, leaseTime, unit)
                    : lock.tryLock(waitTime, unit);
            if (!locked) {
                log.warn("[Redisson] 获取公平锁失败 | key={} | wait={}{}", key, waitTime, unit);
                return fallback.get();
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback.get();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // ==================== 读写锁 ====================

    /**
     * 尝试获取读锁并执行业务
     *
     * @param key      锁 Key
     * @param waitTime 等待时间
     * @param unit     时间单位
     * @param supplier 业务逻辑
     * @param <T>      返回类型
     * @return 业务返回值
     */
    public <T> T tryReadLock(String key, long waitTime, TimeUnit unit, Supplier<T> supplier) {
        return tryReadLock(key, waitTime, unit, supplier, () -> { throw new IllegalStateException("获取读锁失败: " + key); });
    }

    /**
     * 尝试获取读锁并执行业务（完整参数）
     *
     * @param key          锁 Key
     * @param waitTime     等待时间
     * @param unit         时间单位
     * @param supplier     业务逻辑
     * @param fallback     获取失败兜底
     * @param <T>          返回类型
     * @return 业务返回值或兜底返回值
     */
    public <T> T tryReadLock(String key, long waitTime, TimeUnit unit, Supplier<T> supplier, Supplier<T> fallback) {
        if (StringUtils.isBlank(key)) {
            return fallback.get();
        }
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(key);
        RLock readLock = rwLock.readLock();
        boolean locked = false;
        try {
            locked = readLock.tryLock(waitTime, unit);
            if (!locked) {
                log.warn("[Redisson] 获取读锁失败 | key={}", key);
                return fallback.get();
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback.get();
        } finally {
            if (locked && readLock.isHeldByCurrentThread()) {
                readLock.unlock();
            }
        }
    }

    /**
     * 尝试获取写锁并执行业务
     *
     * @param key      锁 Key
     * @param waitTime 等待时间
     * @param unit     时间单位
     * @param supplier 业务逻辑
     * @param <T>      返回类型
     * @return 业务返回值
     */
    public <T> T tryWriteLock(String key, long waitTime, TimeUnit unit, Supplier<T> supplier) {
        return tryWriteLock(key, waitTime, unit, supplier, () -> { throw new IllegalStateException("获取写锁失败: " + key); });
    }

    /**
     * 尝试获取写锁并执行业务（完整参数）
     *
     * @param key          锁 Key
     * @param waitTime     等待时间
     * @param unit         时间单位
     * @param supplier     业务逻辑
     * @param fallback     获取失败兜底
     * @param <T>          返回类型
     * @return 业务返回值或兜底返回值
     */
    public <T> T tryWriteLock(String key, long waitTime, TimeUnit unit, Supplier<T> supplier, Supplier<T> fallback) {
        if (StringUtils.isBlank(key)) {
            return fallback.get();
        }
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(key);
        RLock writeLock = rwLock.writeLock();
        boolean locked = false;
        try {
            locked = writeLock.tryLock(waitTime, unit);
            if (!locked) {
                log.warn("[Redisson] 获取写锁失败 | key={}", key);
                return fallback.get();
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback.get();
        } finally {
            if (locked && writeLock.isHeldByCurrentThread()) {
                writeLock.unlock();
            }
        }
    }

    // ==================== 联锁 / 红锁 ====================

    /**
     * 尝试获取联锁（多 Key 同时锁定）并执行业务
     * <p>
     * 联锁要求所有 Key 都获取成功才算加锁成功；任一 Key 失败则全部释放并返回兜底。
     * </p>
     *
     * @param waitTime 等待时间
     * @param unit     时间单位
     * @param supplier 业务逻辑
     * @param keys     多个锁 Key
     * @param <T>      返回类型
     * @return 业务返回值
     */
    public <T> T tryMultiLock(long waitTime, TimeUnit unit, Supplier<T> supplier, String... keys) {
        if (keys == null || keys.length == 0) {
            return supplier.get();
        }
        RLock multiLock = redissonClient.getMultiLock(toLocks(keys));
        boolean locked = false;
        try {
            locked = multiLock.tryLock(waitTime, unit);
            if (!locked) {
                throw new IllegalStateException("获取联锁失败: " + Arrays.toString(keys));
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("获取联锁被中断: " + Arrays.toString(keys), e);
        } finally {
            if (locked && multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
            }
        }
    }

    /**
     * 将多 Key 转为 RLock 数组
     *
     * @param keys 多个 Key
     * @return RLock 数组
     */
    private RLock[] toLocks(String[] keys) {
        List<RLock> locks = new ArrayList<>(keys.length);
        for (String k : keys) {
            if (StringUtils.isNotBlank(k)) {
                locks.add(redissonClient.getLock(k));
            }
        }
        return locks.toArray(new RLock[0]);
    }

    // ==================== 信号量 / 闭锁 ====================

    /**
     * 尝试获取信号量并执行业务
     *
     * @param key       信号量 Key
     * @param permits   一次性获取的许可数
     * @param waitTime  等待时间
     * @param unit      时间单位
     * @param supplier  业务逻辑
     * @param <T>       返回类型
     * @return 业务返回值
     */
    public <T> T semaphore(String key, int permits, long waitTime, TimeUnit unit, Supplier<T> supplier) {
        if (StringUtils.isBlank(key) || permits <= 0) {
            return supplier.get();
        }
        RSemaphore semaphore = redissonClient.getSemaphore(key);
        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(permits, waitTime, unit);
            if (!acquired) {
                throw new IllegalStateException("获取信号量失败: " + key);
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("获取信号量被中断: " + key, e);
        } finally {
            if (acquired) {
                semaphore.release(permits);
            }
        }
    }

    /**
     * 初始化闭锁并执行业务
     * <p>
     * 闭锁在 count 归零前一直阻塞，常用于多线程协调。本方法仅获取闭锁句柄，
     * 调用方需配合 {@link RCountDownLatch#countDown()} 使用。
     * </p>
     *
     * @param key      闭锁 Key
     * @param count    初始计数
     * @param supplier 业务逻辑（在 {@link RCountDownLatch#await()} 后执行）
     * @param <T>      返回类型
     * @return 业务返回值
     */
    public <T> T countDownLatch(String key, int count, Supplier<T> supplier) {
        if (StringUtils.isBlank(key)) {
            return supplier.get();
        }
        RCountDownLatch latch = redissonClient.getCountDownLatch(key);
        try {
            latch.trySetCount(count);
            latch.await();
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("闭锁等待被中断: " + key, e);
        }
    }

    // ==================== 锁状态查询 / 强制释放 ====================

    /**
     * 判断指定 Key 当前是否被锁定
     *
     * @param key 锁 Key
     * @return true 表示被锁定
     */
    public boolean isLocked(String key) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        try {
            return redissonClient.getLock(key).isLocked();
        } catch (Exception e) {
            log.warn("[Redisson] 查询锁状态失败 | key={} | err={}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 强制释放锁（不校验持有线程）
     * <p>
     * <b>警告</b>：仅在已知锁异常残留（如进程崩溃）的运维场景使用，
     * 业务场景请通过 {@link #tryLock(String, long, TimeUnit, Supplier)} 自动管理。
     * </p>
     *
     * @param key 锁 Key
     * @return true 表示已强制释放
     */
    public boolean forceUnlock(String key) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        try {
            boolean unlocked = redissonClient.getLock(key).forceUnlock();
            log.warn("[Redisson] 强制释放锁 | key={} | unlocked={}", key, unlocked);
            return unlocked;
        } catch (Exception e) {
            log.warn("[Redisson] 强制释放锁失败 | key={} | err={}", key, e.getMessage());
            return false;
        }
    }

    // ==================== 通用锁分发（按 LockType） ====================

    /**
     * 按 {@link LockType} 类型分发加锁并执行业务
     * <p>
     * 提供统一入口，由调用方指定锁类型，便于注解切面或策略模式使用。
     * 仅支持 {@link LockType#REENTRANT}、{@link LockType#FAIR}、{@link LockType#READ}、{@link LockType#WRITE}。
     * 其他类型（信号量、闭锁、联锁、红锁）请使用对应的专用方法。
     * </p>
     *
     * @param type      锁类型
     * @param key       锁 Key
     * @param waitTime  等待时间
     * @param unit      时间单位
     * @param supplier  业务逻辑
     * @param fallback  获取失败兜底
     * @param <T>       返回类型
     * @return 业务返回值或兜底返回值
     */
    @SuppressWarnings("unchecked")
    public <T> T tryLockByType(LockType type, String key, long waitTime, TimeUnit unit,
                                Supplier<T> supplier, Supplier<T> fallback) {
        if (type == null) {
            return tryLock(key, waitTime, DEFAULT_LEASE_SECONDS, unit, supplier, fallback);
        }
        switch (type) {
            case FAIR:
                return tryFairLock(key, waitTime, DEFAULT_LEASE_SECONDS, unit, supplier, fallback);
            case READ:
                return tryReadLock(key, waitTime, unit, supplier, fallback);
            case WRITE:
                return tryWriteLock(key, waitTime, unit, supplier, fallback);
            case REENTRANT:
            default:
                return tryLock(key, waitTime, DEFAULT_LEASE_SECONDS, unit, supplier, fallback);
        }
    }
}
