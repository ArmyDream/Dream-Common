package com.dream.common.framework.cache.aspect;

import com.dream.common.core.utils.R;
import com.dream.common.framework.cache.CachesUtils;
import com.dream.common.framework.cache.annotation.QueryCache;
import com.dream.common.framework.cache.config.CacheProperties;
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 查询缓存切面
 * <p>
 * 拦截标注 {@link QueryCache} 的方法，按以下流程处理：
 * <ol>
 *   <li>校验返回类型是否为 {@link R}（不匹配则跳过缓存并记录 WARN）</li>
 *   <li>解析 Key（固定 / SpEL / 默认）+ 前缀</li>
 *   <li>计算 condition SpEL，不满足则跳过缓存</li>
 *   <li>命中缓存：直接用 {@code R.ok(data)} 返回</li>
 *   <li>未命中：执行原方法，取 {@code R.data} 写入缓存并返回原结果</li>
 * </ol>
 * </p>
 *
 * <h2>启用开关</h2>
 * <p>
 * 默认启用，可通过 {@code dream.cache.query-cache.enabled=false} 全局关闭。
 * </p>
 *
 * <h2>缓存穿透防护</h2>
 * <p>
 * 当 {@link QueryCache#cacheNull()} 为 true 时，方法返回 data 为 null 也会写入缓存
 * （使用 {@link QueryCache#nullExpireSeconds()} 短过期时间），防止恶意请求穿透到 DB。
 * </p>
 *
 * @author dream
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(prefix = "dream.cache.query-cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class QueryCacheAspect {

    /** SpEL 表达式解析器（线程安全） */
    private final ExpressionParser parser = new SpelExpressionParser();

    /** 方法参数名发现器 */
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /** 缓存配置（可选注入） */
    private final ObjectProvider<CacheProperties> propertiesProvider;

    /**
     * 构造切面
     *
     * @param propertiesProvider 缓存配置 Provider
     */
    public QueryCacheAspect(ObjectProvider<CacheProperties> propertiesProvider) {
        this.propertiesProvider = propertiesProvider;
        log.info("QueryCacheAspect 已启用（Controller 查询缓存切面生效）");
    }

    /**
     * 匹配标注 {@link QueryCache} 的方法
     */
    @Pointcut("@annotation(com.dream.common.framework.cache.annotation.QueryCache)")
    public void queryCachePointcut() {
        // pointcut holder
    }

    /**
     * 环绕增强：执行查询缓存逻辑
     *
     * @param pjp 连接点
     * @return 方法返回值（命中缓存则直接包装 R；未命中则原方法结果）
     * @throws Throwable 方法抛出的异常（透传）
     */
    @Around("queryCachePointcut()")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Object[] args = pjp.getArgs();

        // 1. 校验返回类型必须为 R
        Class<?> returnType = method.getReturnType();
        if (!R.class.isAssignableFrom(returnType)) {
            log.warn("[QueryCache] 方法返回类型非 R<?>，跳过缓存 | method={}", method.getName());
            return pjp.proceed();
        }

        QueryCache annotation = method.getAnnotation(QueryCache.class);
        if (annotation == null) {
            return pjp.proceed();
        }

        // 2. 计算 condition
        MethodBasedEvaluationContext ctx = new MethodBasedEvaluationContext(
                null, method, args, parameterNameDiscoverer);
        ctx.setVariable("args", args);
        if (StringUtils.isNotBlank(annotation.condition())) {
            try {
                Boolean satisfied = parser.parseExpression(annotation.condition()).getValue(ctx, Boolean.class);
                if (satisfied == null || !satisfied) {
                    if (log.isDebugEnabled()) {
                        log.debug("[QueryCache] condition 不满足，跳过缓存 | method={} | cond={}",
                                method.getName(), annotation.condition());
                    }
                    return pjp.proceed();
                }
            } catch (Exception e) {
                log.warn("[QueryCache] condition 解析失败，跳过缓存 | cond={} | err={}",
                        annotation.condition(), e.getMessage());
                return pjp.proceed();
            }
        }

        // 3. 解析 Key
        String cacheKey = resolveKey(annotation, method, args, ctx);
        if (StringUtils.isBlank(cacheKey)) {
            log.warn("[QueryCache] 解析 Key 为空，跳过缓存 | method={}", method.getName());
            return pjp.proceed();
        }

        // 4. 命中缓存：直接包装 R 返回
        Object cached = CachesUtils.get(cacheKey);
        if (cached != null) {
            if (log.isDebugEnabled()) {
                log.debug("[QueryCache] 命中缓存 | key={} | method={}", cacheKey, method.getName());
            }
            // 空值占位符 → 返回 R.ok(null)
            if (NULL_PLACEHOLDER.equals(cached)) {
                return R.ok(null);
            }
            return R.ok(cached);
        }

        // 5. 未命中：执行原方法
        Object result = pjp.proceed();
        if (!(result instanceof R)) {
            log.warn("[QueryCache] 方法返回非 R 类型，不写入缓存 | method={}", method.getName());
            return result;
        }

        R r = (R) result;
        Object data = r.getData();

        // 6. 写入缓存
        try {
            if (data == null) {
                if (annotation.cacheNull()) {
                    CachesUtils.set(cacheKey, NULL_PLACEHOLDER, annotation.nullExpireSeconds(), TimeUnit.SECONDS);
                    if (log.isDebugEnabled()) {
                        log.debug("[QueryCache] 写入空值占位缓存 | key={} | ttl={}s", cacheKey, annotation.nullExpireSeconds());
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("[QueryCache] data 为 null 且未开启 cacheNull，不写入缓存 | key={}", cacheKey);
                    }
                }
                return result;
            }

            long expire = annotation.expire();
            TimeUnit unit = annotation.timeUnit();
            if (expire <= 0L) {
                // 回退到默认过期时间
                CacheProperties props = propertiesProvider.getIfAvailable();
                expire = (props != null) ? props.getDefaultExpire() : 180L;
                unit = TimeUnit.SECONDS;
            }
            CachesUtils.set(cacheKey, data, expire, unit);
            if (log.isDebugEnabled()) {
                log.debug("[QueryCache] 写入缓存 | key={} | ttl={}{} | method={}",
                        cacheKey, expire, unit.toString().toLowerCase(), method.getName());
            }
        } catch (Exception e) {
            log.warn("[QueryCache] 写入缓存失败（不影响业务）| key={} | err={}", cacheKey, e.getMessage());
        }

        return result;
    }

    // ==================== Key 解析 ====================

    /**
     * 解析缓存 Key
     * <p>
     * 优先级：{@link QueryCache#spel()} > {@link QueryCache#key()} > 默认（类名:方法名:参数Hash）
     * </p>
     *
     * @param annotation 注解
     * @param method     方法
     * @param args       方法参数
     * @param ctx        SpEL 上下文
     * @return 完整缓存 Key（含前缀）
     */
    private String resolveKey(QueryCache annotation, Method method, Object[] args,
                               MethodBasedEvaluationContext ctx) {
        String key;
        if (StringUtils.isNotBlank(annotation.spel())) {
            try {
                Object value = parser.parseExpression(annotation.spel()).getValue(ctx);
                key = (value == null) ? "" : String.valueOf(value);
            } catch (Exception e) {
                log.warn("[QueryCache] SpEL 解析失败，回退到默认 Key | spel={} | err={}",
                        annotation.spel(), e.getMessage());
                key = buildDefaultKey(method, args);
            }
        } else if (StringUtils.isNotBlank(annotation.key())) {
            key = annotation.key();
        } else {
            key = buildDefaultKey(method, args);
        }

        // 拼接前缀
        if (StringUtils.isNotBlank(annotation.prefix())) {
            key = annotation.prefix() + ":" + key;
        }
        return key;
    }

    /**
     * 构建默认 Key（类名:方法名:参数Hash）
     *
     * @param method 方法
     * @param args   方法参数
     * @return 默认 Key
     */
    private String buildDefaultKey(Method method, Object[] args) {
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String argsHash = (args == null || args.length == 0)
                ? "noargs"
                : Integer.toHexString(Arrays.deepHashCode(args));
        return className + ":" + methodName + ":" + argsHash;
    }

    /**
     * 空值占位符（避免直接缓存 null 导致反序列化歧义）
     */
    private static final String NULL_PLACEHOLDER = "##NULL##";
}
