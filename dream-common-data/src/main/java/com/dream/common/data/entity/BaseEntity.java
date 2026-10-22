package com.dream.common.data.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 公共字段实体类，提供统一的审计字段（创建人、创建时间、更新人、更新时间）。
 * <p>
 * 继承自 {@link BaseIdEntity}，在其基础上扩展四个公共审计字段：
 * createBy、createTime、updateBy、updateTime。这些字段通过 MyBatis-Plus 的
 * {@link FieldFill} 自动填充，无需业务代码手动赋值。
 * </p>
 * <p>
 * 继承关系：{@code BaseEntity} extends {@link BaseIdEntity}，可被
 * {@link BaseLogicEntity}、{@link BaseVersionEntity}、{@link DataChangeRecordEntity} 等进一步扩展。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Data
 * @EqualsAndHashCode(callSuper = true)
 * @TableName("sys_user")
 * public class User extends BaseEntity {
 *     private String username;
 * }
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseEntity extends BaseIdEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 创建人id
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新人ID
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
