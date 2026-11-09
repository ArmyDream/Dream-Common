package com.dream.common.framework.aspect;

import com.dream.common.core.annotation.PreventDuplicate;
import com.dream.common.core.exception.BusinessException;
import com.dream.common.framework.cache.CacheOperations;
import com.dream.common.framework.support.SpelSupport;
import com.dream.common.framework.utils.IpUtils;
import com.dream.common.tool.utils.PreventDuplicateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import com.dream.common.core.constant.OrderValue;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交切面。
 * <p>
 * 拦截 {@link PreventDuplicate} 注解方法，以 {@link CacheOperations#setIfAbsent}
 * （Redis 在场时为集群语义，否则 Caffeine 单机兜底）原子占位实现"窗口期内仅放行一次"：
 * </p>
 * <ul>
 *   <li>占位成功 → 正常执行目标方法</li>
 *   <li>占位失败 → {@code strategy = ERROR} 抛 {@link BusinessException}（由全局异常处理器转统一响应）；
 *       {@code strategy = SILENT} 静默丢弃，返回 {@code null}</li>
 * </ul>
 *
 * <h2>Key 生成策略（优先级从高到低）</h2>
 * <ol>
 *   <li>{@link PreventDuplicate#spel()} —— SpEL 表达式求值（只读上下文，参数名可用
 *       {@code #dto.id} 形式引用；表达式异常时回退到 2）</li>
 *   <li>{@link PreventDuplicateUtils#buildPreventDuplicateKey} —— key 前缀 +
 *       用户 ID + IP + field 提取值，冒号拼接</li>
 * </ol>
 * <p>所有 Key 统一追加 {@code prevent-duplicate:} 命名空间前缀，避免与业务缓存冲突。</p>
 *
 * <h2>用户 ID 解析</h2>
 * <p>
 * 本切面位于 framework 层，不感知具体认证框架：业务侧（如 security 模块）注册
 * {@link DuplicateUserIdProvider} Bean 即可让 {@code includeUserId = true} 的 Key 按用户隔离；
 * 未注册时该段缺省（记 WARN 一次），此时防重为"接口级全局窗口"，请显式配置
 * {@code includeUserId = false} 或改用 {@code includeIp = true}。
 * </p>
 *
 * <h2>集群语义</h2>
 * <p>
 * 防重依赖 {@link CacheOperations#setIfAbsent} 的原子占位。只有 Redis 实现（
 * {@code getType()} 为 {@code REDIS}）跨实例生效；Caffeine 兜底实现的占位仅存在于
 * 当前 JVM，多实例部署时同一窗口的重复请求会各自放行，此时首次拦截会记 WARN 一次。
 * </p>
 *
 * <h2>开关</h2>
 * <p>{@code dream.framework.prevent-duplicate.enabled=false} 可整体关闭本切面。</p>
 *
 * @author dream
 * @see PreventDuplicate
 * @see PreventDuplicateUtils
 */
@Slf4j
@Aspect
@Component
@Order(OrderValue.ASPECT_PREVENT_DUPLICATE)
@ConditionalOnProperty(prefix = "dream.framework.prevent-duplicate", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class PreventDuplicateAspect {

    /** 统一 Key 命名空间前缀 */
    private static final String KEY_NAMESPACE = "prevent-duplicate:";

    /** 具备集群语义的缓存实现类型（见 {@link CacheOperations#getType()}） */
    private static final String DISTRIBUTED_CACHE_TYPE = "REDIS";

    /** 缓存操作入口（Redis 优先，Caffeine 兜底） */
    private final ObjectProvider<CacheOperations> cacheOperationsProvider;

    /** 当前用户 ID 提供者（可选，由认证侧注册） */
    private final ObjectProvider<DuplicateUserIdProvider> userIdProvider;

    /** includeUserId 开启但无 Provider 时仅告警一次 */
    private volatile boolean missingUserIdWarned;

    /** 缓存非 Redis 实现（单机语义）时仅告警一次 */
    private volatile boolean nonDistributedWarned;

    public PreventDuplicateAspect(ObjectProvider<CacheOperations> cacheOperationsProvider,
                                  ObjectProvider<DuplicateUserIdProvider> userIdProvider) {
        this.cacheOperationsProvider = cacheOperationsProvider;
        this.userIdProvider = userIdProvider;
    }

    // ==================== 核心拦截 ====================

    /**
     * 环绕拦截 {@link PreventDuplicate} 注解方法。
     *
     * @param joinPoint  连接点
     * @param annotation 注解实例（由切点绑定）
     * @return 目标方法返回值；SILENT 策略命中重复时返回 {@code null}
     * @throws Throwable 目标方法抛出的异常原样上送
     */
    @Around("@annotation(annotation)")
    public Object around(ProceedingJoinPoint joinPoint, PreventDuplicate annotation) throws Throwable {
        CacheOperations cacheOperations = cacheOperationsProvider.getIfAvailable();
        if (cacheOperations == null) {
            log.warn("未发现 CacheOperations Bean，跳过防重复提交检查: method={}", joinPoint.toShortString());
            return joinPoint.proceed();
        }
        warnIfNonDistributed(cacheOperations);

        Method targetMethod = resolveTargetMethod(joinPoint);
        String key = buildKey(annotation, targetMethod, joinPoint);
        long expireMillis = PreventDuplicateUtils.getExpireMillis(annotation);

        boolean acquired = cacheOperations.setIfAbsent(key, "1", expireMillis, TimeUnit.MILLISECONDS);
        if (acquired) {
            if (log.isDebugEnabled()) {
                log.debug("防重复占位成功: key={}, expireMs={}", key, expireMillis);
            }
            return joinPoint.proceed();
        }

        String message = PreventDuplicateUtils.getPreventDuplicateMessage(annotation);
        if (PreventDuplicateUtils.isSilentStrategy(annotation)) {
            log.warn("重复提交被静默丢弃: key={}, message={}", key, message);
            return null;
        }
        log.warn("重复提交被拦截: key={}, message={}", key, message);
        throw new BusinessException(message);
    }

    // ==================== Key 构建 ====================

    /**
     * 按注解声明的策略构建防重 Key。
     *
     * @param annotation 注解实例
     * @param method     目标方法
     * @param joinPoint  连接点（提供 target 与 args）
     * @return 完整 Key（含命名空间前缀）
     */
    private String buildKey(PreventDuplicate annotation, Method method, ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget() != null
                ? joinPoint.getTarget().getClass().getName()
                : method.getDeclaringClass().getName();
        String methodName = method.getName();
        String userId = annotation.includeUserId() ? resolveUserId() : null;
        String ip = annotation.includeIp() ? IpUtils.getIpAddress() : null;

        String key = annotation.spel().isEmpty() ? null : evaluateSpel(annotation.spel(), method, joinPoint);
        if (key == null) {
            key = PreventDuplicateUtils.buildPreventDuplicateKey(
                    annotation, className, methodName, userId, ip, joinPoint.getArgs());
        }
        return KEY_NAMESPACE + key;
    }

    /**
     * SpEL 求值（只读上下文）；表达式非法或求值失败时回退 null（走通用 Key 构建）。
     */
    private String evaluateSpel(String expressionString, Method method, ProceedingJoinPoint joinPoint) {
        try {
            Expression expression = SpelSupport.getParser().parseExpression(expressionString);
            SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
            String[] paramNames = SpelSupport.getNameDiscoverer().getParameterNames(method);
            Object[] args = joinPoint.getArgs();
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length && i < args.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            context.setVariable("args", args);
            Object value = expression.getValue(context);
            if (value == null || StringUtils.isBlank(value.toString())) {
                log.warn("SpEL 防重表达式求值为空，回退默认 Key 构建: spel={}", expressionString);
                return null;
            }
            return value.toString();
        } catch (Exception e) {
            log.warn("SpEL 防重表达式求值失败，回退默认 Key 构建: spel={}", expressionString, e);
            return null;
        }
    }

    // ==================== 缓存实现集群语义 ====================

    /**
     * 当前缓存实现不具备集群语义时告警一次。
     * <p>
     * Caffeine 兜底实现的 {@code setIfAbsent} 只在单个 JVM 内生效，多实例部署时
     * 同一窗口内的重复请求会各自放行，防重能力退化为"单机窗口"。
     * </p>
     *
     * @param cacheOperations 当前生效的缓存操作入口
     */
    private void warnIfNonDistributed(CacheOperations cacheOperations) {
        if (nonDistributedWarned) {
            return;
        }
        String type = cacheOperations.getType();
        if (type == null || DISTRIBUTED_CACHE_TYPE.equalsIgnoreCase(type.trim())) {
            return;
        }
        nonDistributedWarned = true;
        log.warn("当前 CacheOperations 实现类型为 {}（非 {}），防重复提交仅在单个 JVM 内生效；"
                + "多实例部署请接入 Redis 以获得集群语义", type, DISTRIBUTED_CACHE_TYPE);
    }

    // ==================== 用户 ID ====================

    /**
     * 通过可选的 {@link DuplicateUserIdProvider} 解析当前用户 ID。
     */
    private String resolveUserId() {
        DuplicateUserIdProvider provider = this.userIdProvider.getIfAvailable();
        if (provider == null) {
            if (!missingUserIdWarned) {
                missingUserIdWarned = true;
                log.warn("includeUserId=true 但未注册 {}，防重 Key 不含用户维度（接口级全局窗口）",
                        DuplicateUserIdProvider.class.getName());
            }
            return null;
        }
        try {
            return provider.getUserId();
        } catch (Exception e) {
            log.debug("DuplicateUserIdProvider 解析用户 ID 失败", e);
            return null;
        }
    }

    // ==================== 方法解析 ====================

    /**
     * 解析目标类上的实际执行方法（避免 JDK 代理下接口方法缺失注解/参数名）。
     */
    private Method resolveTargetMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (joinPoint.getTarget() == null || method.getDeclaringClass() == joinPoint.getTarget().getClass()) {
            return method;
        }
        try {
            Method specific = ReflectionUtils.findMethod(joinPoint.getTarget().getClass(), method.getName(), method.getParameterTypes());
            return specific != null ? specific : method;
        } catch (Exception e) {
            return method;
        }
    }

    // ==================== SPI ====================

    /**
     * 当前用户 ID 提供者 SPI。
     * <p>
     * 由认证侧（如 security 模块基于 sa-token）注册为 Bean 后，
     * {@code includeUserId = true} 的防重 Key 将按用户隔离。
     * </p>
     */
    @FunctionalInterface
    public interface DuplicateUserIdProvider {

        /**
         * 获取当前登录用户 ID。
         *
         * @return 用户 ID；未登录/无法获取时返回 {@code null}
         */
        String getUserId();
    }
}
