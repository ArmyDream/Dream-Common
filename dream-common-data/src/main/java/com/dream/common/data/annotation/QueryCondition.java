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
 * <p>
 * 本注解只生成「列名 + 占位参数」形式的条件，不提供 {@code inSql / exists / apply} 一类原始 SQL 入口：
 * 注解里的值来自外部请求，允许拼 SQL 等于留下注入面。子查询、数据范围这类条件应由后端自行拼好参数，
 * 再通过 {@code CommonServiceImpl#beforeQuery} 钩子追加到 Wrapper 上。
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
     * 查询方式。多值方式（IN / NOT_IN / BETWEEN / NOT_BETWEEN）的字段类型可用集合、数组或切割后的字符串
     */
    QueryType type() default QueryType.EQ;

    /**
     * 是否开启驼峰转换
     */
    boolean camelToUnderscore() default true;

    /**
     * 当字段类型为 String 时，是否按 {@link #splitSeparator()} 切割为多值。
     * <p>切割后按 {@link #type()} 生成 IN 或 NOT IN；type 仍为默认的 EQ 时按 IN 处理（兼容旧用法）。
     * BETWEEN / NOT_BETWEEN 无需开启也会切割。空白片段会被丢弃，全部为空时不生成条件。</p>
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