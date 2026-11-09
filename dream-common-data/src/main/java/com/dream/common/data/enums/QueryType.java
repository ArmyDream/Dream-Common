package com.dream.common.data.enums;

/**
 * 查询类型枚举，定义动态查询条件的匹配方式。
 * <p>
 * 配合 {@link com.dream.common.data.annotation.QueryCondition} 注解使用，
 * 标注实体字段在动态构建查询条件时采用的 SQL 匹配操作符。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * public class UserQuery {
 *     @QueryCondition(type = QueryType.LIKE)
 *     private String username;
 *     @QueryCondition(type = QueryType.GE)
 *     private Integer age;
 * }
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
public enum QueryType {
    /**
     * 等于
     */
    EQ,
    /**
     * 不等于
     */
    NE,
    /**
     * 大于
     */
    GT,
    /**
     * 大于等于
     */
    GE,
    /**
     * 小于
     */
    LT,
    /**
     * 小于等于
     */
    LE,
    /**
     * 模糊查询
     */
    LIKE,
    /**
     * 左模糊
     */
    LIKE_LEFT,
    /**
     * 右模糊
     */
    LIKE_RIGHT,
    /**
     * 模糊排除（NOT LIKE %v%）
     */
    NOT_LIKE,
    /**
     * 左模糊排除（NOT LIKE %v）
     */
    NOT_LIKE_LEFT,
    /**
     * 右模糊排除（NOT LIKE v%）
     */
    NOT_LIKE_RIGHT,
    /**
     * IN查询
     */
    IN,
    /**
     * NOT IN查询
     */
    NOT_IN,
    /**
     * BETWEEN
     */
    BETWEEN,
    /**
     * NOT BETWEEN
     */
    NOT_BETWEEN,
    /**
     * IS NULL
     */
    IS_NULL,
    /**
     * IS NOT NULL
     */
    IS_NOT_NULL,
    /**
     * 等于；值为 {@code null} 时降级为 IS NULL
     */
    EQ_OR_IS_NULL

}
