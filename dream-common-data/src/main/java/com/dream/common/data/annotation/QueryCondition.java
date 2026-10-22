package com.dream.common.data.annotation;

import com.dream.common.data.enums.QueryType;

import java.lang.annotation.*;

/**
 * 查询条件注解，标注于实体字段上，定义该字段在动态构建查询条件时的数据库列、匹配方式等。
 * <p>
 * 通用查询构建器（如 {@link com.dream.common.data.service.CommonService#buildQueryWrapper(Object)}）
 * 会扫描实体字段上的本注解，根据注解配置拼接 SQL where 条件。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * public class UserQuery {
 *     @QueryCondition(column = "u.user_name", type = QueryType.LIKE)
 *     private String username;
 *
 *     @QueryCondition(type = QueryType.GE, tableAlias = "u")
 *     private Integer age;
 *
 *     @QueryCondition(type = QueryType.IN, splitToIn = true, splitSeparator = ",")
 *     private String deptIds;
 *
 *     @QueryCondition(ignore = true)
 *     private String remark;
 * }
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QueryCondition {

    /**
     * 数据库字段名，支持别名，如：u.user_name
     */
    String column() default "";

    /**
     * 查询方式
     */
    QueryType type() default QueryType.EQ;

    /**
     * 是否开启驼峰转换
     */
    boolean camelToUnderscore() default true;

    /**
     * 当字段类型为String时，是否按分隔符切割转为IN查询
     */
    boolean splitToIn() default false;

    /**
     * 切割分隔符，默认逗号
     */
    String splitSeparator() default ",";

    /**
     * 关联表别名
     */
    String tableAlias() default "";

    /**
     * 是否忽略该字段
     */
    boolean ignore() default false;
}