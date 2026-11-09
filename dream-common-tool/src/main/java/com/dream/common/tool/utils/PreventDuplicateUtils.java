package com.dream.common.tool.utils;

import com.dream.common.core.annotation.PreventDuplicate;
import com.dream.common.core.enums.StringPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交工具类
 * <p>
 * 封装 {@link PreventDuplicate} 注解的解析与辅助操作：
 * <ul>
 *   <li>{@link #getPreventDuplicate(Method)} —— 获取方法上的注解</li>
 *   <li>{@link #getExpireMillis(PreventDuplicate)} —— 获取过期时间（毫秒）</li>
 *   <li>{@link #getExpireSeconds(PreventDuplicate)} —— 获取过期时间（秒）</li>
 *   <li>{@link #buildPreventDuplicateKey(PreventDuplicate, String, String, String, String, Object...)}
 *           —— 构建防重复 Key</li>
 *   <li>{@link #isSilentStrategy(PreventDuplicate)} —— 判断是否为静默模式</li>
 *   <li>{@link #getPreventDuplicateMessage(PreventDuplicate)} —— 获取提示信息</li>
 * </ul>
 *
 * @author dream
 */
@Slf4j
@SuppressWarnings("all")
public final class PreventDuplicateUtils {

    /** 默认防重复提示信息，统一取自注解定义，避免两处文案漂移 */
    private static final String DEFAULT_MESSAGE = PreventDuplicate.DEFAULT_MESSAGE;

    private PreventDuplicateUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== PreventDuplicate 操作 ====================

    /**
     * 获取方法上的 {@link PreventDuplicate} 注解
     *
     * @param method 方法对象
     * @return 注解实例；未标注返回 null
     */
    public static PreventDuplicate getPreventDuplicate(Method method) {
        if (method == null) {
            log.warn("getPreventDuplicate: method 为 null");
            return null;
        }
        PreventDuplicate annotation = ReflectionUtils.getMethodAnnotation(method, PreventDuplicate.class);
        if (log.isDebugEnabled()) {
            log.debug("获取防重复注解: method={}, hasAnnotation={}", method.getName(), annotation != null);
        }
        return annotation;
    }

    /**
     * 获取防重复过期时间（毫秒）
     *
     * @param annotation PreventDuplicate 注解
     * @return 过期毫秒数；注解为 null 返回 0
     */
    public static long getExpireMillis(PreventDuplicate annotation) {
        if (annotation == null) {
            log.warn("getExpireMillis: annotation 为 null");
            return 0L;
        }
        long millis = annotation.timeUnit().toMillis(annotation.expire());
        if (log.isDebugEnabled()) {
            log.debug("获取防重复过期时间: expire={}, timeUnit={}, millis={}ms",
                    annotation.expire(), annotation.timeUnit(), millis);
        }
        return millis;
    }

    /**
     * 获取防重复过期时间（秒）
     *
     * @param annotation PreventDuplicate 注解
     * @return 过期秒数；注解为 null 返回 0
     */
    public static long getExpireSeconds(PreventDuplicate annotation) {
        if (annotation == null) {
            log.warn("getExpireSeconds: annotation 为 null");
            return 0L;
        }
        long seconds = TimeUnit.MILLISECONDS.toSeconds(getExpireMillis(annotation));
        if (log.isDebugEnabled()) {
            log.debug("获取防重复过期时间: expire={}, timeUnit={}, seconds={}s",
                    annotation.expire(), annotation.timeUnit(), seconds);
        }
        return seconds;
    }

    /**
     * 构建防重复 Key（通用策略段；SpEL 优先级由框架层切面
     * {@code PreventDuplicateAspect} 处理，本方法不参与表达式求值）
     * <p>
     * 按以下策略拼接各段（以冒号分隔）：
     * <ol>
     *   <li>前缀：{@link PreventDuplicate#key()}，留空时取 "类名:方法名"</li>
     *   <li>用户 ID（{@link PreventDuplicate#includeUserId()} = true 时）</li>
     *   <li>IP 地址（{@link PreventDuplicate#includeIp()} = true 时）</li>
     *   <li>{@link PreventDuplicate#field()} 提取的字段值</li>
     * </ol>
     * <pre>
     * buildKey(annotation, "com.UserController", "save", "user123", "192.168.1.1", dto)
     *   → "order:submit:user123:fieldValue1:fieldValue2"
     * </pre>
     *
     * @param annotation 注解实例
     * @param className  类全限定名
     * @param methodName 方法名
     * @param userId     当前用户 ID（可为 null）
     * @param ip         请求 IP（可为 null）
     * @param args       方法参数（用于 field 提取）
     * @return 防重复 Key
     */
    public static String buildPreventDuplicateKey(PreventDuplicate annotation,
                                                  String className, String methodName,
                                                  String userId, String ip, Object... args) {
        if (annotation == null) {
            log.warn("buildPreventDuplicateKey: annotation 为 null，返回空字符串");
            return StringUtils.EMPTY;
        }

        List<String> parts = new ArrayList<>();

        // 1. 前缀
        String keyPrefix = annotation.key();
        if (StringUtils.isBlank(keyPrefix)) {
            keyPrefix = className + StringPool.COLON.getValue() + methodName;
        }
        parts.add(keyPrefix);

        // 2. 用户 ID
        if (annotation.includeUserId() && StringUtils.isNotBlank(userId)) {
            parts.add(userId);
        }

        // 3. IP
        if (annotation.includeIp() && StringUtils.isNotBlank(ip)) {
            parts.add(ip);
        }

        // 4. 字段提取
        String[] fields = annotation.field();
        if (ArrayUtils.isNotEmpty(fields) && ArrayUtils.isNotEmpty(args)) {
            for (String fieldName : fields) {
                for (Object arg : args) {
                    if (arg == null) {
                        continue;
                    }
                    Object fieldValue = AnnotationUtils.extractFieldValue(arg, fieldName);
                    if (fieldValue != null) {
                        parts.add(fieldValue.toString());
                        break;
                    }
                }
            }
        }

        String key = String.join(StringPool.COLON.getValue(), parts);
        if (log.isDebugEnabled()) {
            log.debug("构建防重复 Key: className={}, methodName={}, userId={}, ip={}, fields={}, key={}",
                    className, methodName, userId, ip, fields, key);
        }
        return key;
    }

    /**
     * 判断防重复策略是否为静默模式
     *
     * @param annotation 注解实例
     * @return true 表示静默丢弃（不抛异常）
     */
    public static boolean isSilentStrategy(PreventDuplicate annotation) {
        if (annotation == null) {
            log.warn("isSilentStrategy: annotation 为 null，返回 false");
            return false;
        }
        boolean silent = annotation.strategy() == PreventDuplicate.Strategy.SILENT;
        if (log.isDebugEnabled()) {
            log.debug("防重复策略判断: strategy={}, isSilent={}", annotation.strategy(), silent);
        }
        return silent;
    }

    /**
     * 获取防重复提示信息
     *
     * @param annotation 注解实例
     * @return 提示信息；注解为 null 返回默认提示
     */
    public static String getPreventDuplicateMessage(PreventDuplicate annotation) {
        if (annotation == null) {
            log.warn("getPreventDuplicateMessage: annotation 为 null，返回默认提示");
            return DEFAULT_MESSAGE;
        }
        String message = annotation.message();
        if (log.isDebugEnabled()) {
            log.debug("获取防重复提示信息: message={}", message);
        }
        return message;
    }

    /**
     * 检查是否重复提交（业务层调用）
     *
     * @param annotation 注解实例
     * @param className  类名
     * @param methodName 方法名
     * @param userId     用户 ID
     * @param ip         请求 IP
     * @param args       方法参数
     * @return 构建的 Key，用于缓存判断
     */
    public static String buildKeyAndLog(PreventDuplicate annotation,
                                        String className, String methodName,
                                        String userId, String ip, Object... args) {
        String key = buildPreventDuplicateKey(annotation, className, methodName, userId, ip, args);
        if (log.isDebugEnabled()) {
            log.debug("防重复提交 Key 构建完成: key={}, expireMs={}",
                    key, annotation != null ? getExpireMillis(annotation) : 0);
        }
        return key;
    }
}