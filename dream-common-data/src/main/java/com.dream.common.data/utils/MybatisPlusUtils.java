package com.dream.common.data.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MyBatis-Plus 工具类
 * <p>
 * 提供表名解析、SQL IN 条件字符串拼接、泛型参数类型解析等通用能力，
 * 用于配合 MyBatis-Plus 的 Wrapper 或实体泛型解析场景。
 * </p>
 *
 * @author dream
 */
@Slf4j
public class MybatisPlusUtils {

    /**
     * 私有构造方法，禁止实例化工具类
     */
    private MybatisPlusUtils() {
    }

    /**
     * 获取实体类对应的表名
     * <p>
     * 优先读取实体类上的 {@link com.baomidou.mybatisplus.annotation.TableName} 注解值，
     * 未标注注解时回退使用类的简单名称（{@link Class#getSimpleName()}）。
     * </p>
     *
     * @param entityClass 实体类
     * @return 表名；注解为空字符串时返回注解值，无注解时返回类简单名称
     */
    public static String getTableName(Class<?> entityClass) {
        com.baomidou.mybatisplus.annotation.TableName tableName =
                entityClass.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class);
        return tableName != null ? tableName.value() : entityClass.getSimpleName();
    }

    /**
     * 将字符串列表拼接为 SQL IN 条件字符串
     * <p>例如：{@code ["a","b"]} → {@code 'a','b'}；数值模式 {@code ["1","2"]} → {@code 1,2}。</p>
     * <p><b>安全说明：</b>本方法返回的字符串仅用于 MyBatis-Plus 的 {@code QueryWrapper.in(...)} 等
     * 条件构造器入参，由 MyBatis-Plus 内部进行参数化处理，<b>不应直接拼接到原始 SQL 语句中</b>，
     * 以避免 SQL 注入风险。</p>
     *
     * @param ids       字符串列表
     * @param isNumeric 是否按数值拼接（true 不加引号，false 加单引号），{@code null} 视为 false
     * @return SQL IN 字符串，空列表或 {@code null} 返回空字符串
     */
    public static String toIn(List<String> ids, Boolean isNumeric) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        if (isNumeric == null) {
            isNumeric = false;
        }

        Boolean finalIsNumeric = isNumeric;
        return ids.stream()
                .filter(StringUtils::hasText)
                .map(id -> finalIsNumeric ? id : "'" + id.replace("'", "''") + "'")
                .collect(Collectors.joining(","));
    }

    /**
     * 将字符串列表拼接为 SQL IN 条件字符串（按字符串处理）
     * <p>例如：{@code ["a","b"]} → {@code 'a','b'}。</p>
     *
     * @param ids 字符串列表
     * @return SQL IN 字符串，空列表或 {@code null} 返回空字符串
     */
    public static String toIn(List<String> ids) {
        return toIn(ids, null);
    }


    /**
     * 将逗号分隔的字符串拼接为 SQL IN 条件字符串（按字符串处理）
     * <p>例如：{@code "a,b"} → {@code 'a','b'}。</p>
     *
     * @param ids 逗号分隔的字符串
     * @return SQL IN 字符串，空白字符串返回空字符串
     */
    public static String toIn(String ids) {
        if (!StringUtils.hasText(ids)) {
            return "";
        }
        return toIn(Arrays.asList(ids.split(",")), null);
    }

    /**
     * 获得类声明的泛型参数的类型
     * <p>
     * 解析指定类的父类（{@link Class#getGenericSuperclass()}）的泛型参数，
     * 并返回第 {@code index} 个泛型参数的原始类型。
     * 当父类未参数化、索引越界或无法解析为具体类时返回 {@link Object}。
     * </p>
     *
     * @param clazz 指定类
     * @param index 第几个泛型参数，从 0 开始
     * @return 泛型参数的 {@link Class}；无法解析时返回 {@code Object.class}
     */
    public static Class<?> getClassByType(Class<?> clazz, int index) {
        Type genType = clazz.getGenericSuperclass();

        if (!(genType instanceof ParameterizedType)) {
            return Object.class;
        }

        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();

        if (index >= params.length || index < 0) {
            return Object.class;
        }

        Type paramType = params[index];

        // 处理泛型类型
        if (paramType instanceof Class) {
            return (Class<?>) paramType;
        } else if (paramType instanceof ParameterizedType) {
            // 如果是泛型类型，返回原始类型
            return (Class<?>) ((ParameterizedType) paramType).getRawType();
        }

        return Object.class;
    }

    /**
     * 获得类声明的第一个泛型参数的类型
     *
     * @param clazz 指定类
     * @return 第一个泛型参数的 {@link Class}；无法解析时返回 {@code Object.class}
     */
    public static Class<?> getFirstGenericType(Class<?> clazz) {
        return getClassByType(clazz, 0);
    }

    /**
     * 获得类声明的第二个泛型参数的类型
     *
     * @param clazz 指定类
     * @return 第二个泛型参数的 {@link Class}；无法解析时返回 {@code Object.class}
     */
    public static Class<?> getSecondGenericType(Class<?> clazz) {
        return getClassByType(clazz, 1);
    }

    /**
     * 获得类声明的第三个泛型参数的类型
     *
     * @param clazz 指定类
     * @return 第三个泛型参数的 {@link Class}；无法解析时返回 {@code Object.class}
     */
    public static Class<?> getThirdGenericType(Class<?> clazz) {
        return getClassByType(clazz, 2);
    }
}
