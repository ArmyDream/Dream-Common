package com.dream.common.security.vo;

import com.dream.common.security.enums.DataScope;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色信息
 * <p>
 * RBAC 权限模型中的角色实体，关联权限与用户。
 * {@link DataScope} 控制角色可查询的数据范围。
 * </p>
 *
 * @author dream
 * @since 2026-08-24
 */
@Data
public class RoleInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色 ID
     */
    private String roleId;

    /**
     * 角色名称（如 "超级管理员"）
     */
    private String roleName;

    /**
     * 角色编码（如 "admin"），用于代码中判断
     */
    private String roleCode;

    /**
     * 数据权限范围
     */
    private DataScope dataScope;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 状态（0 正常 / 1 停用）
     */
    private String status;

    /**
     * 数据权限自定义关联的部门 ID（逗号分隔），仅 {@link DataScope#CUSTOM} 时有效
     */
    private String dataScopeDeptIds;
}
