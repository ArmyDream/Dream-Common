package com.dream.common.tool.utils;

import com.dream.common.core.annotation.PreventDuplicate;
import com.dream.common.core.enums.StringPool;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 注解工具类
 * <p>
 * 封装 {@link com.dream.common.core.annotation} 中各注解的解析与辅助操作：
 * <ul>
 *   <li>{@link PreventDuplicate} —— 防重复提交：Key 构建、过期时间计算、字段提取</li>
 * </ul>
 * <p>
 * 注意：本类依赖 {@link ReflectionUtils} 进行反射操作，
 * 使用前请确保目标类有无参构造器（用于实例化场景）。
 *
 * @author dream
 */
@SuppressWarnings("all")
public final class PreventDuplicateUtils {

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
        return ReflectionUtils.getMethodAnnotation(method, PreventDuplicate.class);
    }

    /**
     * 获取防重复过期时间（毫秒）
     *
     * @param annotation PreventDuplicate 注解
     * @return 过期毫秒数；注解为 null 返回 0
     */
    public static long getExpireMillis(PreventDuplicate annotation) {
        if (annotation == null) {
            return 0L;
        }
        return annotation.timeUnit().toMillis(annotation.expire());
    }

    /**
     * 获取防重复过期时间（秒）
     *
     * @param annotation PreventDuplicate 注解
     * @return 过期秒数；注解为 null 返回 0
     */
    public static long getExpireSeconds(PreventDuplicate annotation) {
        if (annotation == null) {
            return 0L;
        }
        return TimeUnit.MILLISECONDS.toSeconds(getExpireMillis(annotation));
    }

    /**
     * 构建防重复 Key（不依赖 SpEL）
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

        return String.join(StringPool.COLON.getValue(), parts);
    }

    /**
     * 判断防重复策略是否为静默模式
     *
     * @param annotation 注解实例
     * @return true 表示静默丢弃（不抛异常）
     */
    public static boolean isSilentStrategy(PreventDuplicate annotation) {
        return annotation != null && annotation.strategy() == PreventDuplicate.Strategy.SILENT;
    }

    /**
     * 获取防重复提示信息
     *
     * @param annotation 注解实例
     * @return 提示信息；注解为 null 返回默认提示
     */
    public static String getPreventDuplicateMessage(PreventDuplicate annotation) {
        if (annotation == null) {
            return "请勿重复提交！";
        }
        return annotation.message();
    }

}
