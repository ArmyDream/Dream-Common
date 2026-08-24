package com.dream.common.data.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 乐观锁基础类，基于版本号实现乐观并发控制。
 * <p>
 * 继承自 {@link BaseLogicEntity}，新增 {@code version} 字段，通过 {@link Version}
 * 注解由 MyBatis-Plus 在更新时校验并自增版本号，防止并发修改覆盖。该字段不序列化输出。
 * </p>
 * <p>
 * 继承关系：{@code BaseVersionEntity} extends {@link BaseLogicEntity} extends {@link BaseEntity}
 * extends {@link BaseIdEntity}，是实体体系中功能最完整的基础类。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Data
 * @EqualsAndHashCode(callSuper = true)
 * @TableName("sys_order")
 * public class Order extends BaseVersionEntity {
 *     private String orderNo;
 * }
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseVersionEntity extends BaseLogicEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField(value = "version", fill = FieldFill.INSERT)
    @JsonIgnore
    private Long version;

}
