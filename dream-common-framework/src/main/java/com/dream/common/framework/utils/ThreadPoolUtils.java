package com.dream.common.framework.utils;

import com.dream.common.tool.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 线程池工具类
 * <p>
 * 提供线程池的创建、任务提交、生命周期管理、状态监控、MDC 上下文传播等能力。
 * 支持 Spring Boot 标准配置（spring.task.execution），无需额外配置项。
 * </p>
 *
 * <h2>核心能力</h2>
 * <ul>
 *   <li><b>Spring Boot 自动配置</b>：使用 spring.task.execution 配置，自动集成 MDC 上下文传播</li>
 *   <li><b>MDC 上下文传播</b>：{@link #wrap(Runnable)} / {@link #wrap(Callable)} —— 自动传递链路追踪上下文</li>
 *   <li><b>线程池创建</b>：{@link #newFixedExecutor(int)} / {@link #newCachedExecutor()} / {@link #newScheduledExecutor(int)}</li>
 *   <li><b>任务提交</b>：{@link #execute(Runnable)} / {@link #submit(Callable)} —— 全局默认线程池</li>
 *   <li><b>异步编排</b>：{@link #runAsync(Runnable)} / {@link #runAsync(Runnable)} —— CompletableFuture</li>
 *   <li><b>生命周期</b>：{@link #shutdown(ExecutorService)} —— 优雅关闭</li>
 *   <li><b>状态监控</b>：{@link #getPoolStatus(ExecutorService)} / {@link #getPoolMetrics(ExecutorService)}</li>
 *   <li><b>线程工厂</b>：{@link #namedThreadFactory(String)}</li>
 * </ul>
 *
 * <h2>Spring Boot 配置示例（application.yml）</h2>
 * <pre>{@code
 * spring:
 *   task:
 *     execution:
 *       pool:
 *         core-size: 8                    # 核心线程数（默认 CPU 核心数）
 *         max-size: 16                    # 最大线程数（默认 core-size * 2）
 *         queue-capacity: 1000            # 任务队列容量
 *         keep-alive: 60s                 # 空闲线程存活时间
 *       thread-name-prefix: async-        # 线程名称前缀
 * }</pre>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 使用全局默认线程池（自动传播 MDC）
 * ThreadPoolUtils.execute(() -> log.info("异步任务"));
 *
 * // 2. 注入 Spring Bean
 * @Autowired @Qualifier("taskThreadPool")
 * private AsyncTaskExecutor taskExecutor;
 *
 * // 3. 手动创建线程池
 * ExecutorService pool = ThreadPoolUtils.newFixedExecutor(4, "order-");
 *
 * // 4. 异步编排
 * CompletableFuture<String> cf = ThreadPoolUtils.supplyAsync(() -> fetchData());
 *
 * // 5. 批量执行
 * List<Future<String>> futures = ThreadPoolUtils.invokeAll(pool, tasks);
 *
 * // 6. 状态监控
 * String status = ThreadPoolUtils.getPoolStatus(pool);
 * }</pre>
 *
 * @author dream
 */
@Slf4j
@SuppressWarnings({"unused", "WeakerAccess"})
public final class ThreadPoolUtils {

    // ==================== 默认常量 ====================

    /** 默认核心线程数（CPU 核心数） */
    public static final int DEFAULT_CORE_SIZE = Runtime.getRuntime().availableProcessors();

    /** 默认最大线程数（核心数 * 2） */
    public static final int DEFAULT_MAX_SIZE = DEFAULT_CORE_SIZE * 2;

    /** 默认空闲线程存活时间（秒） */
    public static final long DEFAULT_KEEP_ALIVE_SECONDS = 60L;

    /** 默认队列容量 */
    public static final int DEFAULT_QUEUE_CAPACITY = Integer.MAX_VALUE;

    /** 默认关闭等待时间（秒） */
    public static final long DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 30L;

    /** 默认线程池名称前缀 */
    public static final String DEFAULT_THREAD_NAME_PREFIX = "dream-";

    /** 全局默认线程池（懒加载） */
    private static volatile ExecutorService DEFAULT_EXECUTOR;

    /** 默认线程池锁 */
    private static final Object DEFAULT_LOCK = new Object();

    private ThreadPoolUtils() {
        throw new UnsupportedOperationException("ThreadPoolUtils is a utility class and cannot be instantiated");
    }

    // ==================== 全局默认线程池 ====================

    /**
     * 获取全局默认线程池（懒加载，JVM 关闭时自动关闭）
     *
     * @return 全局默认线程池
     */
    public static ExecutorService getDefaultExecutor() {
        if (DEFAULT_EXECUTOR == null) {
            synchronized (DEFAULT_LOCK) {
                if (DEFAULT_EXECUTOR == null) {
                    DEFAULT_EXECUTOR = newCustomExecutor(
                            DEFAULT_CORE_SIZE,
                            DEFAULT_MAX_SIZE,
                            DEFAULT_KEEP_ALIVE_SECONDS,
                            TimeUnit.SECONDS,
                            DEFAULT_QUEUE_CAPACITY,
                            DEFAULT_THREAD_NAME_PREFIX,
                            new ThreadPoolExecutor.CallerRunsPolicy()
                    );
                    registerShutdownHook(DEFAULT_EXECUTOR);
                    log.info("Default thread pool initialized: core={}, max={}, queue={}",
                            DEFAULT_CORE_SIZE, DEFAULT_MAX_SIZE, DEFAULT_QUEUE_CAPACITY);
                }
            }
        }
        return DEFAULT_EXECUTOR;
    }

    private static void registerShutdownHook(ExecutorService executor) {
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    log.info("JVM shutdown, closing default thread pool...");
                    shutdown(executor);
                }, "dream-pool-shutdown-hook")
        );
    }

    // ==================== MDC 上下文传播 ====================

    /**
     * 包装 Runnable，自动传播 MDC 上下文
     *
     * @param task 原始任务
     * @return 包装后的任务
     */
    public static Runnable wrap(Runnable task) {
        Objects.requireNonNull(task, "task must not be null");
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (contextMap != null && !contextMap.isEmpty()) {
                    MDC.setContextMap(contextMap);
                }
                task.run();
            } finally {
                MDC.clear();
            }
        };
    }

    /**
     * 包装 Callable，自动传播 MDC 上下文
     *
     * @param task 原始任务
     * @param <T>  返回值类型
     * @return 包装后的任务
     */
    public static <T> Callable<T> wrap(Callable<T> task) {
        Objects.requireNonNull(task, "task must not be null");
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (contextMap != null && !contextMap.isEmpty()) {
                    MDC.setContextMap(contextMap);
                }
                return task.call();
            } finally {
                MDC.clear();
            }
        };
    }

    /**
     * 包装 Supplier，自动传播 MDC 上下文（用于 CompletableFuture）
     *
     * @param supplier 值提供者
     * @param <T>      返回值类型
     * @return 包装后的 Supplier
     */
    public static <T> Supplier<T> wrapSupplier(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (contextMap != null && !contextMap.isEmpty()) {
                    MDC.setContextMap(contextMap);
                }
                return supplier.get();
            } finally {
                MDC.clear();
            }
        };
    }

    // ==================== MDC 上下文管理 ====================

    public static void putContext(String key, String value) {
        if (StringUtils.isNotBlank(key)) {
            MDC.put(key, value);
        }
    }

    public static String getContext(String key) {
        return MDC.get(key);
    }

    public static void removeContext(String key) {
        MDC.remove(key);
    }

    public static Map<String, String> getContextMap() {
        return MDC.getCopyOfContextMap();
    }

    public static void setContextMap(Map<String, String> contextMap) {
        if (contextMap != null && !contextMap.isEmpty()) {
            MDC.setContextMap(contextMap);
        } else {
            MDC.clear();
        }
    }

    public static void clearContext() {
        MDC.clear();
    }

    // ==================== 创建线程池 ====================

    public static ExecutorService newFixedExecutor(int nThreads) {
        return newFixedExecutor(nThreads, DEFAULT_THREAD_NAME_PREFIX);
    }

    public static ExecutorService newFixedExecutor(int nThreads, String namePrefix) {
        if (nThreads <= 0) {
            throw new IllegalArgumentException("nThreads must be > 0");
        }
        String prefix = resolvePrefix(namePrefix);
        return Executors.newFixedThreadPool(nThreads, namedThreadFactory(prefix));
    }

    public static ExecutorService newCachedExecutor() {
        return newCachedExecutor(DEFAULT_THREAD_NAME_PREFIX);
    }

    public static ExecutorService newCachedExecutor(String namePrefix) {
        String prefix = resolvePrefix(namePrefix);
        return Executors.newCachedThreadPool(namedThreadFactory(prefix));
    }

    public static ExecutorService newSingleExecutor() {
        return newSingleExecutor(DEFAULT_THREAD_NAME_PREFIX);
    }

    public static ExecutorService newSingleExecutor(String namePrefix) {
        String prefix = resolvePrefix(namePrefix);
        return Executors.newSingleThreadExecutor(namedThreadFactory(prefix));
    }

    public static ScheduledExecutorService newScheduledExecutor(int corePoolSize) {
        return newScheduledExecutor(corePoolSize, DEFAULT_THREAD_NAME_PREFIX);
    }

    public static ScheduledExecutorService newScheduledExecutor(int corePoolSize, String namePrefix) {
        if (corePoolSize <= 0) {
            throw new IllegalArgumentException("corePoolSize must be > 0");
        }
        String prefix = resolvePrefix(namePrefix);
        return Executors.newScheduledThreadPool(corePoolSize, namedThreadFactory(prefix));
    }

    /**
     * 创建自定义线程池（自动参数保护）
     *
     * @param corePoolSize  核心线程数
     * @param maxPoolSize   最大线程数
     * @param keepAliveTime 空闲存活时间
     * @param unit          时间单位
     * @param queueCapacity 队列容量
     * @param namePrefix    线程名称前缀
     * @param handler       拒绝策略
     * @return 自定义线程池
     */
    public static ThreadPoolExecutor newCustomExecutor(
            int corePoolSize,
            int maxPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            int queueCapacity,
            String namePrefix,
            RejectedExecutionHandler handler) {

        // 参数修正
        corePoolSize = Math.max(corePoolSize, 1);
        maxPoolSize = Math.max(maxPoolSize, corePoolSize);
        keepAliveTime = Math.max(keepAliveTime, 0);
        unit = unit != null ? unit : TimeUnit.SECONDS;
        queueCapacity = Math.max(queueCapacity, 1);
        handler = handler != null ? handler : new ThreadPoolExecutor.CallerRunsPolicy();
        String prefix = resolvePrefix(namePrefix);

        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(queueCapacity);

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                namedThreadFactory(prefix),
                handler
        );
        executor.allowCoreThreadTimeOut(true);

        log.info("Created custom thread pool: prefix={}, core={}, max={}, keepAlive={}{}, queue={}",
                prefix, corePoolSize, maxPoolSize, keepAliveTime, unit.toString().toLowerCase(), queueCapacity);

        return executor;
    }

    // ==================== 线程工厂 ====================

    public static ThreadFactory namedThreadFactory(String namePrefix) {
        return new NamedThreadFactory(resolvePrefix(namePrefix), false);
    }

    public static ThreadFactory daemonThreadFactory(String namePrefix) {
        return new NamedThreadFactory(resolvePrefix(namePrefix), true);
    }

    // ==================== 任务提交（全局默认线程池） ====================

    public static void execute(Runnable task) {
        getDefaultExecutor().execute(wrap(task));
    }

    public static <T> Future<T> submit(Callable<T> task) {
        return getDefaultExecutor().submit(wrap(task));
    }

    public static Future<?> submit(Runnable task) {
        return getDefaultExecutor().submit(wrap(task));
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(wrapSupplier(supplier), getDefaultExecutor());
    }

    public static CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(wrap(task), getDefaultExecutor());
    }

    // ==================== 任务提交（指定线程池） ====================

    public static void execute(Executor executor, Runnable task) {
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(task, "task must not be null");
        executor.execute(wrap(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("Task execution failed: {}", e.getMessage(), e);
            }
        }));
    }

    public static <T> Future<T> submit(ExecutorService executor, Callable<T> task) {
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(task, "task must not be null");
        return executor.submit(wrap(task));
    }

    public static Future<?> submit(ExecutorService executor, Runnable task) {
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(task, "task must not be null");
        return executor.submit(wrap(task));
    }

    // ==================== 批量执行 ====================

    public static <T> List<Future<T>> invokeAll(ExecutorService executor,
                                                Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        Objects.requireNonNull(executor, "executor must not be null");
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        return executor.invokeAll(tasks);
    }

    public static <T> List<Future<T>> invokeAll(ExecutorService executor,
                                                Collection<? extends Callable<T>> tasks,
                                                long timeout,
                                                TimeUnit unit)
            throws InterruptedException {
        Objects.requireNonNull(executor, "executor must not be null");
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        return executor.invokeAll(tasks, timeout, unit);
    }

    public static List<Future<?>> submitAll(ExecutorService executor, Collection<? extends Runnable> tasks) {
        Objects.requireNonNull(executor, "executor must not be null");
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<Future<?>> futures = new ArrayList<>(tasks.size());
        for (Runnable task : tasks) {
            futures.add(executor.submit(wrap(task)));
        }
        return futures;
    }

    // ==================== 生命周期管理 ====================

    public static void shutdown(ExecutorService executor) {
        shutdown(executor, DEFAULT_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public static void shutdown(ExecutorService executor, long timeout, TimeUnit unit) {
        if (executor == null || executor.isShutdown()) {
            return;
        }
        Objects.requireNonNull(unit, "unit must not be null");
        try {
            executor.shutdown();
            if (!executor.awaitTermination(timeout, unit)) {
                log.warn("Thread pool did not terminate within {} {}, forcing shutdown...", timeout, unit);
                List<Runnable> pending = executor.shutdownNow();
                if (!pending.isEmpty()) {
                    log.warn("Forced shutdown: {} pending tasks discarded", pending.size());
                }
                executor.awaitTermination(timeout, unit);
            }
            log.debug("Thread pool shutdown complete");
        } catch (InterruptedException e) {
            log.warn("Thread pool shutdown interrupted", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static List<Runnable> shutdownNow(ExecutorService executor) {
        if (executor == null || executor.isShutdown()) {
            return Collections.emptyList();
        }
        log.info("Shutting down thread pool immediately");
        return executor.shutdownNow();
    }

    // ==================== 状态监控 ====================

    public static String getPoolStatus(ExecutorService executor) {
        if (executor == null) {
            return "ThreadPool[null]";
        }
        if (!(executor instanceof ThreadPoolExecutor)) {
            return String.format("ThreadPool[%s, shutdown=%s]",
                    executor.getClass().getSimpleName(), executor.isShutdown());
        }
        ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
        return String.format(
                "ThreadPool[shutdown=%s, core=%d, max=%d, active=%d, pool=%d, " +
                        "completed=%d, total=%d, queue=%d]",
                tpe.isShutdown(),
                tpe.getCorePoolSize(),
                tpe.getMaximumPoolSize(),
                tpe.getActiveCount(),
                tpe.getPoolSize(),
                tpe.getCompletedTaskCount(),
                tpe.getTaskCount(),
                tpe.getQueue().size()
        );
    }

    public static Map<String, Object> getPoolMetrics(ExecutorService executor) {
        if (!(executor instanceof ThreadPoolExecutor)) {
            return Collections.emptyMap();
        }
        ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
        BlockingQueue<Runnable> queue = tpe.getQueue();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("shutdown", tpe.isShutdown());
        metrics.put("terminated", tpe.isTerminated());
        metrics.put("corePoolSize", tpe.getCorePoolSize());
        metrics.put("maxPoolSize", tpe.getMaximumPoolSize());
        metrics.put("poolSize", tpe.getPoolSize());
        metrics.put("activeCount", tpe.getActiveCount());
        metrics.put("largestPoolSize", tpe.getLargestPoolSize());
        metrics.put("completedTaskCount", tpe.getCompletedTaskCount());
        metrics.put("taskCount", tpe.getTaskCount());
        metrics.put("queueSize", queue.size());
        metrics.put("queueRemainingCapacity", queue.remainingCapacity());
        return metrics;
    }

    public static int getAvailableProcessors() {
        return DEFAULT_CORE_SIZE;
    }

    // ==================== 内部方法 ====================

    private static String resolvePrefix(String namePrefix) {
        return StringUtils.isNotBlank(namePrefix) ? namePrefix : DEFAULT_THREAD_NAME_PREFIX;
    }

    // ==================== Spring Boot 自动配置 ====================

    /**
     * Spring Boot 线程池自动配置
     * <p>
     * 使用 Spring Boot 标准配置前缀 {@code spring.task.execution}，
     * 无需额外配置项，与 Spring Boot 2.7.x 完全兼容。
     * </p>
     * <p>
     * 配置参数对应关系：
     * <ul>
     *   <li>{@code spring.task.execution.pool.core-size} → {@link ThreadPoolTaskExecutor#setCorePoolSize}</li>
     *   <li>{@code spring.task.execution.pool.max-size} → {@link ThreadPoolTaskExecutor#setMaxPoolSize}</li>
     *   <li>{@code spring.task.execution.pool.queue-capacity} → {@link ThreadPoolTaskExecutor#setQueueCapacity}</li>
     *   <li>{@code spring.task.execution.pool.keep-alive} → {@link ThreadPoolTaskExecutor#setKeepAliveSeconds}</li>
     *   <li>{@code spring.task.execution.thread-name-prefix} → {@link ThreadPoolTaskExecutor#setThreadNamePrefix}</li>
     * </ul>
     * </p>
     */
    @Setter
    @Getter
    @Slf4j
    @EnableAsync(proxyTargetClass = true)
    @Configuration
    @ConfigurationProperties(prefix = "spring.task.execution")
    public static class ThreadPoolAutoConfiguration {

        /** 线程池配置 */
        private Pool pool = new Pool();

        /** 线程名称前缀 */
        private String threadNamePrefix = DEFAULT_THREAD_NAME_PREFIX;

        @Bean("taskThreadPool")
        @ConditionalOnMissingBean(name = "taskThreadPool")
        public AsyncTaskExecutor taskThreadPool() {
            // 应用默认值
            int coreSize = pool.getCoreSize() != null ? pool.getCoreSize() : DEFAULT_CORE_SIZE;
            int maxSize = pool.getMaxSize() != null ? pool.getMaxSize() : coreSize * 2;
            int queueCapacity = pool.getQueueCapacity() != null ? pool.getQueueCapacity() : DEFAULT_QUEUE_CAPACITY;
            int keepAlive = pool.getKeepAlive() != null ? pool.getKeepAlive() : (int) DEFAULT_KEEP_ALIVE_SECONDS;
            String prefix = StringUtils.isNotBlank(threadNamePrefix) ? threadNamePrefix : DEFAULT_THREAD_NAME_PREFIX;

            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(coreSize);
            executor.setMaxPoolSize(maxSize);
            executor.setQueueCapacity(queueCapacity);
            executor.setKeepAliveSeconds(keepAlive);
            executor.setThreadNamePrefix(prefix);
            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
            executor.setWaitForTasksToCompleteOnShutdown(true);
            executor.setAwaitTerminationSeconds((int) DEFAULT_SHUTDOWN_TIMEOUT_SECONDS);
            // 使用自定义装饰器传播 MDC 上下文
            executor.setTaskDecorator(new MdcTaskDecorator());
            executor.initialize();

            log.info("Spring Boot thread pool initialized: prefix={}, core={}, max={}, queue={}, keepAlive={}s",
                    prefix, coreSize, maxSize, queueCapacity, keepAlive);

            return executor;
        }

        // ==================== Getter/Setter ====================

        /**
         * 线程池配置（对应 spring.task.execution.pool）
         */
        @Setter
        @Getter
        public static class Pool {
            private Integer coreSize;
            private Integer maxSize;
            private Integer queueCapacity;
            private Integer keepAlive;

        }
    }

    // ==================== 内部类 ====================

    /**
     * MDC 上下文任务装饰器
     */
    private static class MdcTaskDecorator implements TaskDecorator {

        @Override
        public Runnable decorate(@NonNull Runnable runnable) {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (contextMap != null && !contextMap.isEmpty()) {
                        MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        }
    }

    /**
     * 命名线程工厂
     */
    private static final class NamedThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final boolean daemon;

        NamedThreadFactory(String namePrefix, boolean daemon) {
            this.namePrefix = namePrefix;
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            thread.setDaemon(daemon);
            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            return thread;
        }
    }
}