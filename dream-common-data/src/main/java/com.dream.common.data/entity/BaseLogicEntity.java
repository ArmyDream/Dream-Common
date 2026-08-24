package com.dream.common.data.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 逻辑删除基础类，提供逻辑删除（软删除）能力。
 * <p>
 * 继承自 {@link BaseEntity}，新增 {@code deleted} 字段，通过 {@link TableLogic}
 * 注解由 MyBatis-Plus 拦截删除操作，将其转换为 update 语句（0-正常，1-已删除）。
 * 查询时自动过滤已删除数据，且该字段不序列化输出（{@link JsonIgnore}）。
 * </p>
 * <p>
 * 继承关系：{@code BaseLogicEntity} extends {@link BaseEntity} extends {@link BaseIdEntity}。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Data
 * @EqualsAndHashCode(callSuper = true)
 * @TableName("sys_user")
 * public class User extends BaseLogicEntity {
 *     private String username;
 * }
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseLogicEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 逻辑删除标志(0-正常，1-已删除)
     */
    @TableLogic
    @TableField(value = "deleted", select = false, fill = FieldFill.INSERT)
    @JsonIgnore
    private Integer deleted;

}
