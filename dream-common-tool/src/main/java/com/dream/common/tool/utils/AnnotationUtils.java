package com.dream.common.tool.utils;

import com.dream.common.core.annotation.ExcelValid;
import com.dream.common.core.annotation.PreventDuplicate;
import com.dream.common.core.annotation.Tree;
import com.dream.common.core.enums.StringPool;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 注解工具类
 * <p>
 * 封装 {@link com.dream.common.core.annotation} 中各注解的解析与辅助操作：
 * <ul>
 *   <li>{@link ExcelValid} —— Excel 校验：字段校验、字典转换、批量校验</li>
 *   <li>{@link PreventDuplicate} —— 防重复提交：Key 构建、过期时间计算、字段提取</li>
 *   <li>{@link Tree} —— 树结构：字段映射解析、字段过滤、配置提取</li>
 * </ul>
 * <p>
 * 注意：本类依赖 {@link ReflectionUtils} 进行反射操作，
 * 使用前请确保目标类有无参构造器（用于实例化场景）。
 *
 */
@SuppressWarnings("all")
public final class AnnotationUtils {

    private AnnotationUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 通用注解操作 ====================

    /**
     * 获取类上指定类型的注解（含父类链）
     *
     * @param clazz           目标类
     * @param annotationClass 注解类型
     * @return 注解实例；未找到返回 null
     */
    public static <A extends Annotation> A getAnnotation(Class<?> clazz, Class<A> annotationClass) {
        return ReflectionUtils.getAnnotation(clazz, annotationClass);
    }

    /**
     * 获取方法上指定类型的注解
     *
     * @param method          方法对象
     * @param annotationClass 注解类型
     * @return 注解实例；未找到返回 null
     */
    public static <A extends Annotation> A getAnnotation(Method method, Class<A> annotationClass) {
        return ReflectionUtils.getMethodAnnotation(method, annotationClass);
    }

    /**
     * 获取字段上指定类型的注解
     *
     * @param field           字段对象
     * @param annotationClass 注解类型
     * @return 注解实例；未找到返回 null
     */
    public static <A extends Annotation> A getAnnotation(Field field, Class<A> annotationClass) {
        return ReflectionUtils.getFieldAnnotation(field, annotationClass);
    }

    /**
     * 判断方法是否带有指定注解
     *
     * @param method          方法对象
     * @param annotationClass 注解类型
     * @return true 表示存在该注解
     */
    public static boolean hasAnnotation(Method method, Class<? extends Annotation> annotationClass) {
        return method != null && method.isAnnotationPresent(annotationClass);
    }

    /**
     * 判断类（含父类链）是否带有指定注解
     *
     * @param clazz           目标类
     * @param annotationClass 注解类型
     * @return true 表示存在该注解
     */
    public static boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        return ReflectionUtils.isAnnotationPresent(clazz, annotationClass);
    }

    // ==================== 工具方法 ====================

    /**
     * 从对象中按字段名（支持嵌套点分隔）提取值
     */
    public static Object extractFieldValue(Object obj, String fieldName) {
        if (obj == null || StringUtils.isBlank(fieldName)) {
            return null;
        }
        // 支持嵌套字段（如 "user.phone"）
        if (fieldName.contains(StringPool.DOT.getValue())) {
            String[] parts = fieldName.split("\\.", 2);
            Object nested = ReflectionUtils.getFieldValue(obj, parts[0]);
            return (nested == null) ? null : extractFieldValue(nested, parts[1]);
        }
        return ReflectionUtils.getFieldValue(obj, fieldName);
    }

    /**
     * 格式化校验消息（替换 {fieldName} 占位符，或使用默认消息）
     */
    public static String formatMsg(String customMsg, String defaultMsg) {
        if (StringUtils.isBlank(customMsg)) {
            return defaultMsg;
        }
        return customMsg;
    }
}
