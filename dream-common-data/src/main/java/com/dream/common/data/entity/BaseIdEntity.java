package com.dream.common.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

/**
 * ID基础实体类，作为所有持久化实体的根基类。
 * <p>
 * 提供统一的主键ID字段，采用 MyBatis-Plus 的雪花算法（{@link IdType#ASSIGN_ID}）
 * 自动生成分布式唯一ID。实现 {@link Serializable} 以支持序列化与分布式传输。
 * </p>
 * <p>
 * 继承关系：本类为实体继承体系的最顶层，通常由 {@link BaseEntity} 继承扩展，
 * 进而衍生出 {@link BaseLogicEntity}（逻辑删除）、{@link BaseVersionEntity}（乐观锁）等基础类。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Data
 * @TableName("sys_user")
 * public class User extends BaseIdEntity {
 *     private String username;
 * }
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
@Data
public class BaseIdEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，基于雪花算法自动生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

}
