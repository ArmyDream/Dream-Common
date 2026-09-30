package com.dream.common.framework.cache;

import com.dream.common.framework.cache.caffeine.CaffeineCacheOperations;
import com.dream.common.framework.cache.config.CacheProperties;
import com.dream.common.tool.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 统一缓存对外入口（Redis 优先，不可用自动降级 Caffeine）
 * <p>
 * 屏蔽底层缓存实现（Redis / Caffeine / 其他），对外提供一致的静态 API；
 * 内置「熔断 + 降级」机制，保证缓存故障不会击穿到业务层：
 * <ol>
 *   <li>启动期：按 {@link CacheProperties.CacheType} 与 Redis 可用性选择主缓存实现</li>
 *   <li>运行期：主缓存为 Redis 时，若调用异常则标记熔断，后续请求降级到 Caffeine</li>
 *   <li>恢复期：熔断冷却时间到达后，下一次请求自动重试 Redis，成功则恢复</li>
 * </ol>
 * </p>
 *
 * <h2>降级策略</h2>
 * <ul>
 *   <li>{@code AUTO}（默认）：Redis 在场优先；Redis 调用异常触发熔断并降级 Caffeine，冷却后自动回切</li>
 *   <li>{@code REDIS}：只用 Redis，<b>不降级、不熔断</b>。调用异常时仅记 WARN，读返回默认值、写静默丢弃，
 *       <b>不会向业务层抛异常</b>；需要"缓存故障即失败"的强一致语义时，请直接使用
 *       {@link com.dream.common.framework.cache.redis.RedisUtils}，其异常会原样上送</li>
 *   <li>{@code CAFFEINE}：强制本地 Caffeine</li>
 * </ul>
 * <p>
 * 本类对所有缓存异常做了兜底：静态 API <b>永不抛出缓存层异常</b>，故障表现为"读到默认值 / 写未生效"。
 * 因此不要把缓存当作强一致存储；一致性要求高的场景请用 {@code RedisUtils} 或数据库。
 * </p>
 * <p>
 * <b>「不引入不报错」</b>：本类仅依赖 {@link CacheOperations} / {@link CaffeineCacheOperations}
 * 等不引入 Redis 类型的抽象，Redis 依赖缺失时也能正常编译与运行（自动使用 Caffeine）。
 * 这与 Redis 侧自动配置的行为不同：显式 {@code spring.data.redis.*} 配置了不可达的地址时启动即失败，
 * 以便把连接问题暴露在部署期而非运行期。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 基础读写
 * CachesUtils.set("user:1", user, 60);
 * User u = CachesUtils.get("user:1");
 *
 * // 懒加载
 * User u = CachesUtils.getOrCreate("user:1", () -> userService.getById(1), 60);
 *
 * // 删除 / 前缀删除
 * CachesUtils.delete("user:1");
 * CachesUtils.deletePrefix("user:");
 *
 * // 延时双删（注解，见 CacheDoubleDelete）
 * }</pre>
 *
 * <h2>三个入口怎么选</h2>
 * <ul>
 *   <li>{@code CachesUtils}（本类，静态）：业务默认入口。带熔断/兜底、永不抛缓存异常，
 *       适合"缓存只加速、失效可回源"的场景</li>
 *   <li>{@link CacheOperations}（注入接口）：框架扩展点。自定义缓存实现（如多级缓存、测试桩）
 *       实现本接口替换底层；<b>直接调它绕过本类的熔断兜底</b>，异常原样上送，
 *       业务代码一般不应注入本接口</li>
 *   <li>{@link com.dream.common.framework.cache.redis.RedisUtils}（注入）：Redis 专属能力
 *       （原子自增/计数、过期时间控制、批量读写、{@code opsForXxx} 等本类未覆盖的命令）。
 *       同样<b>不降级、不吞异常</b>，一致性/正确性要求高的读写选它而不是选本类；
 *       分布式锁、信号量等原语在 {@code RedissonUtils}，不在本类</li>
 * </ul>
 * <p>
 * 同一个 Key 只应通过一套入口访问：{@code AUTO} 降级期间本类可能把数据写在 Caffeine 本地，
 * 而 {@code RedisUtils} 只读写 Redis，混用会出现"写了读不到"。
 * </p>
 *
 * @author dream
 */
@Slf4j
@Component
@Lazy(false)
@SuppressWarnings({"unused", "WeakerAccess"})
public class CachesUtils {

    /** 主缓存实现（Redis 在场时为 Redis，否则 Caffeine；可被 {@link CacheProperties.CacheType} 覆盖） */
    private static volatile CacheOperations primary;

    /** Caffeine 兜底实现（始终可用） */
    private static volatile CaffeineCacheOperations caffeineFallback;

    /** 缓存配置 */
    private static volatile CacheProperties properties;

    /** Redis 是否健康（仅 primary 为 Redis 时有意义） */
    private static volatile boolean primaryHealthy = true;

    /** 最近一次 Redis 失败时间戳（毫秒） */
    private static volatile long lastFailureTime = 0L;

    /** Spring 注入的主缓存（@Primary 解析：Redis 在场为 RedisUtils，否则 Caffeine） */
    private final CacheOperations injectedPrimary;

    /** Spring 注入的 Caffeine 兜底 */
    private final CaffeineCacheOperations injectedCaffeine;

    /** Spring 注入的配置 */
    private final CacheProperties injectedProperties;

    /**
     * Spring 构造注入
     *
     * @param injectedPrimary     主缓存实现（@Primary）
     * @param injectedCaffeine    Caffeine 兜底实现
     * @param injectedProperties  缓存配置
     */
    @Autowired
    public CachesUtils(CacheOperations injectedPrimary,
                       CaffeineCacheOperations injectedCaffeine,
                       CacheProperties injectedProperties) {
        this.injectedPrimary = injectedPrimary;
        this.injectedCaffeine = injectedCaffeine;
        this.injectedProperties = injectedProperties;
    }

    /**
     * 初始化静态引用，按配置解析主缓存实现
     */
    @PostConstruct
    public void init() {
        CacheProperties.CacheType type = injectedProperties != null ? injectedProperties.getType() : CacheProperties.CacheType.AUTO;
        CacheOperations resolved;
        if (type == CacheProperties.CacheType.CAFFEINE) {
            resolved = injectedCaffeine;
            log.info("[CachesUtils] forced CAFFEINE mode, primary={}", resolved.getType());
        } else {
            resolved = injectedPrimary;
            log.info("[CachesUtils] {} mode, primary={}, fallback={}",
                    type, resolved.getType(), injectedCaffeine.getType());
        }
        CachesUtils.primary = resolved;
        CachesUtils.caffeineFallback = injectedCaffeine;
        CachesUtils.properties = injectedProperties;
        CachesUtils.primaryHealthy = true;
    }

    // ==================== 熔断 / 降级 ====================

    /**
     * 主缓存是否为 Redis
     */
    private static boolean isPrimaryRedis() {
        CacheOperations p = primary;
        return p != null && "REDIS".equals(p.getType());
    }

    /**
     * 是否允许 Redis 降级到 Caffeine
     */
    private static boolean isFallbackAllowed() {
        CacheProperties props = properties;
        if (props == null) {
            return true;
        }
        if (props.getType() == CacheProperties.CacheType.REDIS) {
            return false;
        }
        return props.isFallbackEnabled();
    }

    /**
     * 解析当前应使用的缓存实现
     * <p>
     * 主缓存健康则用主缓存；若主缓存为 Redis 且处于熔断冷却期则用 Caffeine；
     * 冷却期过后返回主缓存以触发重试恢复。
     * </p>
     *
     * @return 生效的缓存实现
     */
    private static CacheOperations getEffectiveOps() {
        CacheOperations p = primary;
        if (p == null) {
            return caffeineFallback;
        }
        if (!isPrimaryRedis()) {
            return p;
        }
        if (primaryHealthy) {
            return p;
        }
        // 熔断中：检查冷却
        CacheProperties props = properties;
        long cooldownMs = (props != null
                ? props.getRedis().getHealthCooldownSeconds()
                : CacheProperties.Redis.DEFAULT_HEALTH_COOLDOWN_SECONDS) * 1000L;
        if (System.currentTimeMillis() - lastFailureTime >= cooldownMs) {
            log.info("[CachesUtils] Redis 熔断冷却结束，尝试恢复...");
            return p;
        }
        return caffeineFallback;
    }

    /**
     * 标记主缓存（Redis）熔断
     * <p>
     * 首次失败记录完整 WARN；恢复期探活失败仅刷新失败时间戳（记录 DEBUG），
     * 使下次探活重新进入冷却等待，避免冷却到期后每个请求都直连 Redis 反复探活。
     * </p>
     *
     * @param reason 原因
     * @param e      异常
     */
    private static void markUnhealthy(String reason, Exception e) {
        boolean firstFailure = primaryHealthy;
        primaryHealthy = false;
        lastFailureTime = System.currentTimeMillis();
        if (firstFailure) {
            log.warn("[CachesUtils] Redis 触发熔断，降级 Caffeine | reason={} | err={}", reason, e.getMessage());
        } else if (log.isDebugEnabled()) {
            log.debug("[CachesUtils] Redis 探活失败，继续保持熔断 | reason={}", reason);
        }
    }

    /**
     * 标记主缓存恢复健康
     */
    private static void markHealthy() {
        if (!primaryHealthy) {
            primaryHealthy = true;
            log.info("[CachesUtils] Redis 已恢复，回切主缓存");
        }
    }

    /**
     * 执行读操作，带降级
     *
     * @param action       缓存动作
     * @param defaultValue 默认返回值
     * @param <R>          返回类型
     * @return 结果
     */
    private static <R> R readWithFallback(Function<CacheOperations, R> action, R defaultValue) {
        CacheOperations ops = getEffectiveOps();
        try {
            R result = action.apply(ops);
            if (ops == primary && isPrimaryRedis() && !primaryHealthy) {
                markHealthy();
            }
            return result;
        } catch (RuntimeException e) {
            if (ops == primary && isPrimaryRedis() && isFallbackAllowed()) {
                markUnhealthy("read op failed", e);
                try {
                    return action.apply(caffeineFallback);
                } catch (RuntimeException e2) {
                    log.error("[CachesUtils] Caffeine 降级读取亦失败: {}", e2.getMessage(), e2);
                    return defaultValue;
                }
            }
            log.warn("[CachesUtils] 缓存读取失败 ({}): {}", ops.getType(), e.getMessage());
            return defaultValue;
        }
    }

    /**
     * 执行写操作，带降级（best-effort）
     *
     * @param action 缓存动作
     */
    private static void writeWithFallback(java.util.function.Consumer<CacheOperations> action) {
        CacheOperations ops = getEffectiveOps();
        try {
            action.accept(ops);
            if (ops == primary && isPrimaryRedis() && !primaryHealthy) {
                markHealthy();
            }
        } catch (RuntimeException e) {
            if (ops == primary && isPrimaryRedis() && isFallbackAllowed()) {
                markUnhealthy("write op failed", e);
                try {
                    action.accept(caffeineFallback);
                    return;
                } catch (RuntimeException e2) {
                    log.error("[CachesUtils] Caffeine 降级写入亦失败: {}", e2.getMessage(), e2);
                    return;
                }
            }
            log.warn("[CachesUtils] 缓存写入失败 ({}): {}", ops.getType(), e.getMessage());
        }
    }

    // ==================== 对外 API：写入 ====================

    /**
     * 设置缓存
     *
     * @param key      唯一键
     * @param value    存储对象
     * @param expire   过期时间
     * @param timeUnit 时间单位
     * @param <T>      值类型
     */
    public static <T> void set(String key, T value, long expire, TimeUnit timeUnit) {
        if (StringUtils.isBlank(key) || value == null) {
            return;
        }
        writeWithFallback(ops -> ops.set(key, value, expire, timeUnit));
    }

    /**
     * 设置缓存（单位：秒）
     *
     * @param key    唯一键
     * @param value  存储对象
     * @param expire 过期时间（秒）
     * @param <T>    值类型
     */
    public static <T> void set(String key, T value, long expire) {
        set(key, value, expire, TimeUnit.SECONDS);
    }

    /**
     * 设置缓存（默认过期时间）
     *
     * @param key   唯一键
     * @param value 存储对象
     * @param <T>   值类型
     */
    public static <T> void set(String key, T value) {
        if (StringUtils.isBlank(key) || value == null) {
            return;
        }
        CacheProperties props = properties;
        long expire = props != null ? props.getDefaultExpire() : CacheProperties.DEFAULT_EXPIRE_SECONDS;
        writeWithFallback(ops -> ops.set(key, value, expire, TimeUnit.SECONDS));
    }

    /**
     * 设置永不过期缓存
     *
     * @param key   唯一键
     * @param value 存储对象
     * @param <T>   值类型
     */
    public static <T> void setNever(String key, T value) {
        if (StringUtils.isBlank(key) || value == null) {
            return;
        }
        writeWithFallback(ops -> ops.setNever(key, value));
    }

    /**
     * 仅当 key 不存在时设置缓存
     *
     * @param key      唯一键
     * @param value    存储对象
     * @param expire   过期时间
     * @param timeUnit 时间单位
     * @param <T>      值类型
     * @return true 表示成功设置
     */
    public static <T> boolean setIfAbsent(String key, T value, long expire, TimeUnit timeUnit) {
        if (StringUtils.isBlank(key) || value == null) {
            return false;
        }
        return readWithFallback(ops -> ops.setIfAbsent(key, value, expire, timeUnit), false);
    }

    /**
     * 批量设置缓存
     *
     * @param map      键值对
     * @param expire   过期时间
     * @param timeUnit 时间单位
     * @param <T>      值类型
     */
    public static <T> void batchSet(Map<String, T> map, long expire, TimeUnit timeUnit) {
        if (map == null || map.isEmpty()) {
            return;
        }
        writeWithFallback(ops -> ops.batchSet(map, expire, timeUnit));
    }

    // ==================== 对外 API：读取 ====================

    /**
     * 获取缓存值
     *
     * @param key 唯一键
     * @param <T> 值类型
     * @return 缓存值；不存在或异常返回 null
     */
    public static <T> T get(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        return readWithFallback(ops -> ops.get(key), null);
    }

    /**
     * 获取缓存值，不存在返回默认值
     *
     * @param key          唯一键
     * @param defaultValue 默认值
     * @param <T>          值类型
     * @return 缓存值或默认值
     */
    public static <T> T getOrDefault(String key, T defaultValue) {
        if (StringUtils.isBlank(key)) {
            return defaultValue;
        }
        return readWithFallback(ops -> ops.getOrDefault(key, defaultValue), defaultValue);
    }

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
    public static <T> T getOrCreate(String key, java.util.function.Supplier<T> supplier, long expire, TimeUnit timeUnit) {
        if (StringUtils.isBlank(key) || supplier == null) {
            return null;
        }
        return readWithFallback(ops -> ops.getOrCreate(key, supplier::get, expire, timeUnit), null);
    }

    /**
     * 懒加载获取缓存（默认过期时间）
     *
     * @param key      唯一键
     * @param supplier 值生成器
     * @param <T>      值类型
     * @return 缓存值或新生成的值
     */
    public static <T> T getOrCreate(String key, java.util.function.Supplier<T> supplier) {
        CacheProperties props = properties;
        long expire = props != null ? props.getDefaultExpire() : CacheProperties.DEFAULT_EXPIRE_SECONDS;
        return getOrCreate(key, supplier, expire, TimeUnit.SECONDS);
    }

    /**
     * 批量获取缓存值（仅返回存在的条目）
     *
     * @param keys key 集合
     * @param <T>  值类型
     * @return key → value 映射
     */
    public static <T> Map<String, T> batchGet(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return new java.util.LinkedHashMap<>();
        }
        return readWithFallback(ops -> ops.batchGet(keys), new java.util.LinkedHashMap<>());
    }

    // ==================== 对外 API：删除 ====================

    /**
     * 删除单个缓存
     *
     * @param key 唯一键
     * @return true 表示删除成功
     */
    public static boolean delete(String key) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        return readWithFallback(ops -> ops.delete(key), false);
    }

    /**
     * 批量删除缓存
     *
     * @param keys key 集合
     * @return 实际删除条数
     */
    public static long deleteKeys(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        return readWithFallback(ops -> ops.deleteKeys(keys), 0L);
    }

    /**
     * 按前缀批量删除缓存
     *
     * @param keyPrefix key 前缀
     * @return 实际删除条数
     */
    public static long deletePrefix(String keyPrefix) {
        if (StringUtils.isBlank(keyPrefix)) {
            return 0L;
        }
        return readWithFallback(ops -> ops.deletePrefix(keyPrefix), 0L);
    }

    /**
     * 清空全部缓存
     */
    public static void clear() {
        writeWithFallback(CacheOperations::clear);
    }

    // ==================== 对外 API：信息 ====================

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
        return readWithFallback(ops -> ops.contains(key), false);
    }

    /**
     * 获取缓存条目数（估计值）
     *
     * @return 缓存条目数
     */
    public static long size() {
        return readWithFallback(CacheOperations::size, 0L);
    }

    /**
     * 按前缀获取缓存 key
     *
     * @param keyPrefix key 前缀
     * @return 匹配的 key 列表
     */
    public static Collection<String> getKeys(String keyPrefix) {
        return readWithFallback(ops -> ops.getKeys(keyPrefix), java.util.Collections.emptyList());
    }

    // ==================== 诊断 ====================

    /**
     * 获取当前生效的缓存类型标识
     *
     * @return {@code "REDIS"} / {@code "CAFFEINE"}；未初始化返回 {@code "UNINITIALIZED"}
     */
    public static String getActiveType() {
        CacheOperations ops = getEffectiveOps();
        return ops == null ? "UNINITIALIZED" : ops.getType();
    }

    /**
     * 主缓存（Redis）是否处于熔断状态
     *
     * @return true 表示当前 Redis 熔断，已降级 Caffeine
     */
    public static boolean isPrimaryHealthy() {
        return !isPrimaryRedis() || primaryHealthy;
    }

    /**
     * 手动恢复 Redis（清除熔断标记，立即重试）
     */
    public static void forceRecover() {
        if (isPrimaryRedis() && !primaryHealthy) {
            primaryHealthy = true;
            log.info("[CachesUtils] 手动恢复 Redis，清除熔断标记");
        }
    }
}
