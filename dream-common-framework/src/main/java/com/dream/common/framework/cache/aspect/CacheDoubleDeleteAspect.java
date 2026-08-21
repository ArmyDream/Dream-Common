package com.dream.common.framework.cache.aspect;

import com.dream.common.framework.cache.CachesUtils;
import com.dream.common.framework.cache.annotation.CacheDoubleDelete;
import com.dream.common.framework.cache.config.CacheProperties;
import com.dream.common.framework.utils.ThreadPoolUtils;
import com.dream.common.tool.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.lang.reflect.Method;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 延时双删切面
 * <p>
 * 拦截标注了 {@link CacheDoubleDelete} 的方法，按「先删 → 执行 → 延时再删」织入，
 * 保证数据库与缓存最终一致性，避免主从同步延迟窗口内的脏读回写。
 * </p>
 *
 * <h2>执行流程</h2>
 * <ol>
 *   <li>解析注解 Key（固定 / SpEL / 多 Key）</li>
 *   <li>{@link CacheDoubleDelete#firstDelete()} 为 true 时，方法执行前删除缓存</li>
 *   <li>执行被拦截方法</li>
 *   <li>方法正常返回（或 {@link CacheDoubleDelete#deleteOnException()} 为 true 时异常也执行）
 *       调度延时任务，{@link CacheDoubleDelete#delay()} 后执行第二次删除</li>
 * </ol>
 * <p>
 * 第二次删除通过独立守护线程的 {@link ScheduledExecutorService} 异步执行，
 * 并经 {@link ThreadPoolUtils#wrap(Runnable)} 传播 MDC 链路上下文，不阻塞业务线程。
 * </p>
 *
 * <h2>开关</h2>
 * <p>
 * 默认启用，可通过 {@code dream.cache.double-delete.enabled=false} 全局关闭。
 * </p>
 *
 * @author dream
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(prefix = "dream.cache.double-delete", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheDoubleDeleteAspect {

    /** SpEL 表达式解析器（线程安全，可复用） */
    private final ExpressionParser parser = new SpelExpressionParser();

    /** 方法参数名发现器 */
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /** 延时删除调度器（守护线程，不阻塞 JVM 退出） */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "cache-double-delete-scheduler");
        t.setDaemon(true);
        return t;
    });

    /** 缓存配置（可选注入，未配置时使用默认值） */
    private final ObjectProvider<CacheProperties> propertiesProvider;

    /**
     * 构造切面
     *
     * @param propertiesProvider 缓存配置 Provider
     */
    public CacheDoubleDeleteAspect(ObjectProvider<CacheProperties> propertiesProvider) {
        this.propertiesProvider = propertiesProvider;
        log.info("CacheDoubleDeleteAspect 已启用（延时双删切面生效）");
    }

    /**
     * 匹配标注 {@link CacheDoubleDelete} 或其容器 {@link CacheDoubleDelete.List} 的方法
     */
    @Pointcut("@annotation(com.dream.common.framework.cache.annotation.CacheDoubleDelete) || "
            + "@annotation(com.dream.common.framework.cache.annotation.CacheDoubleDelete.List)")
    public void cacheDoubleDeletePointcut() {
        // pointcut holder
    }

    /**
     * 环绕增强：执行延时双删
     *
     * @param pjp 连接点
     * @return 方法返回值
     * @throws Throwable 方法抛出的异常（透传）
     */
    @Around("cacheDoubleDeletePointcut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = method(signature);
        Object[] args = pjp.getArgs();

        // 合并获取所有 @CacheDoubleDelete 注解（支持 @Repeatable）
        Set<CacheDoubleDelete> annotations = AnnotatedElementUtils_findAll(method);
        if (annotations.isEmpty()) {
            return pjp.proceed();
        }

        // 解析所有 Key + 模式
        List<ResolvedKey> resolvedKeys = new ArrayList<>();
        for (CacheDoubleDelete ann : annotations) {
            List<String> keys = resolveKeys(ann, method, args);
            for (String key : keys) {
                resolvedKeys.add(new ResolvedKey(key, ann.prefix(), ann));
            }
        }

        // 1. 第一次删除（前置）
        for (ResolvedKey rk : resolvedKeys) {
            if (rk.annotation.firstDelete()) {
                deleteOnce(rk);
            }
        }

        // 2. 执行业务方法
        Throwable thrown = null;
        Object result;
        try {
            result = pjp.proceed();
        } catch (Throwable t) {
            thrown = t;
            throw t;
        } finally {
            // 3. 调度延时第二次删除：成功删全部；异常仅删 deleteOnException=true 的
            List<ResolvedKey> toDelete;
            if (thrown == null) {
                toDelete = resolvedKeys;
            } else {
                toDelete = resolvedKeys.stream()
                        .filter(rk -> rk.annotation.deleteOnException())
                        .collect(Collectors.toList());
            }
            if (!toDelete.isEmpty()) {
                scheduleSecondDelete(toDelete);
            }
        }
        return result;
    }

    // ==================== Key 解析 ====================

    /**
     * 解析单个注解的 Key 集合
     *
     * @param ann   注解
     * @param method 方法
     * @param args  方法参数
     * @return Key 列表（已去空）
     */
    private List<String> resolveKeys(CacheDoubleDelete ann, Method method, Object[] args) {
        Set<String> keys = new LinkedHashSet<>();
        if (StringUtils.isNotBlank(ann.spel())) {
            Object value = evalSpel(ann.spel(), method, args);
            if (value instanceof Collection) {
                for (Object o : (Collection<?>) value) {
                    if (o != null) {
                        keys.add(String.valueOf(o));
                    }
                }
            } else if (value != null && value.getClass().isArray()) {
                for (Object o : org.springframework.util.ObjectUtils.toObjectArray(value)) {
                    if (o != null) {
                        keys.add(String.valueOf(o));
                    }
                }
            } else if (value != null) {
                keys.add(String.valueOf(value));
            }
        } else if (ann.keys() != null && ann.keys().length > 0) {
            keys.addAll(Arrays.asList(ann.keys()));
        } else if (StringUtils.isNotBlank(ann.key())) {
            keys.add(ann.key());
        }
        return keys.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
    }

    /**
     * 执行 SpEL 表达式
     *
     * @param spel   表达式
     * @param method 方法
     * @param args   参数
     * @return 计算结果
     */
    private Object evalSpel(String spel, Method method, Object[] args) {
        try {
            MethodBasedEvaluationContext ctx = new MethodBasedEvaluationContext(
                    null, method, args, parameterNameDiscoverer);
            ctx.setVariable("args", args);
            return parser.parseExpression(spel).getValue(ctx);
        } catch (Exception e) {
            log.warn("[CacheDoubleDelete] SpEL 解析失败，跳过该注解 | spel={} | err={}", spel, e.getMessage());
            return null;
        }
    }

    // ==================== 删除执行 ====================

    /**
     * 执行一次删除（前缀 / 精确）
     *
     * @param rk 已解析 Key
     */
    private void deleteOnce(ResolvedKey rk) {
        try {
            if (rk.prefix) {
                long count = CachesUtils.deletePrefix(rk.key);
                if (log.isDebugEnabled()) {
                    log.debug("[CacheDoubleDelete] 前缀删除 key={}, removed={}", rk.key, count);
                }
            } else {
                boolean ok = CachesUtils.delete(rk.key);
                if (log.isDebugEnabled()) {
                    log.debug("[CacheDoubleDelete] 精确删除 key={}, removed={}", rk.key, ok);
                }
            }
        } catch (Exception e) {
            log.warn("[CacheDoubleDelete] 删除缓存异常（降级机制兜底，不影响业务）| key={} | err={}", rk.key, e.getMessage());
        }
    }

    /**
     * 调度延时第二次删除
     *
     * @param resolvedKeys 已解析 Key 列表
     */
    private void scheduleSecondDelete(List<ResolvedKey> resolvedKeys) {
        long delay = -1L;
        TimeUnit unit = TimeUnit.MILLISECONDS;
        // 取首个有效延时配置
        for (ResolvedKey rk : resolvedKeys) {
            if (rk.annotation.delay() >= 0L) {
                delay = rk.annotation.delay();
                unit = rk.annotation.timeUnit();
                break;
            }
        }
        if (delay < 0L) {
            CacheProperties props = propertiesProvider.getIfAvailable();
            delay = (props != null) ? props.getDoubleDelete().getDefaultDelayMs() : 500L;
            unit = TimeUnit.MILLISECONDS;
        }

        final long finalDelay = delay;
        final TimeUnit finalUnit = unit;
        final List<ResolvedKey> snapshot = new ArrayList<>(resolvedKeys);
        Runnable task = ThreadPoolUtils.wrap(() -> {
            for (ResolvedKey rk : snapshot) {
                deleteOnce(rk);
            }
            if (log.isDebugEnabled()) {
                log.debug("[CacheDoubleDelete] 延时二次删除完成 | delay={}{} | keys={}",
                        finalDelay, finalUnit.toString().toLowerCase(),
                        snapshot.stream().map(rk -> rk.key).collect(Collectors.toList()));
            }
        });

        try {
            scheduler.schedule(task, finalDelay, finalUnit);
            if (log.isDebugEnabled()) {
                log.debug("[CacheDoubleDelete] 调度延时二次删除 | delay={}{} | keys={}",
                        finalDelay, finalUnit.toString().toLowerCase(),
                        snapshot.stream().map(rk -> rk.key).collect(Collectors.toList()));
            }
        } catch (Exception e) {
            log.error("[CacheDoubleDelete] 调度延时二次删除失败，立即执行一次兜底删除 | err={}", e.getMessage(), e);
            for (ResolvedKey rk : snapshot) {
                deleteOnce(rk);
            }
        }
    }

    // ==================== 辅助 ====================

    /**
     * 从 MethodSignature 获取 Method（兼容接口代理与 CGLIB 代理）
     *
     * @param signature 方法签名
     * @return Method
     */
    private Method method(MethodSignature signature) {
        Method method = signature.getMethod();
        if (method == null) {
            throw new IllegalStateException("无法解析方法签名: " + signature);
        }
        return method;
    }

    /**
     * 获取方法上的所有 {@link CacheDoubleDelete}（兼容 @Repeatable 容器）
     *
     * @param element 注解元素
     * @return 注解集合
     */
    private Set<CacheDoubleDelete> AnnotatedElementUtils_findAll(AnnotatedElement element) {
        return new LinkedHashSet<>(
                org.springframework.core.annotation.AnnotatedElementUtils
                        .getAllMergedAnnotations(element, CacheDoubleDelete.class));
    }

    /**
     * 容器销毁时关闭调度器
     */
    @PreDestroy
    public void destroy() {
        ThreadPoolUtils.shutdown(scheduler, 5L, TimeUnit.SECONDS);
        log.info("CacheDoubleDeleteAspect scheduler closed");
    }

    /**
     * 已解析 Key 内部模型
     *
     * @author dream
     */
    private static final class ResolvedKey {
        /** 缓存 Key */
        final String key;
        /** 是否前缀模式 */
        final boolean prefix;
        /** 来源注解 */
        final CacheDoubleDelete annotation;

        ResolvedKey(String key, boolean prefix, CacheDoubleDelete annotation) {
            this.key = key;
            this.prefix = prefix;
            this.annotation = annotation;
        }
    }
}
