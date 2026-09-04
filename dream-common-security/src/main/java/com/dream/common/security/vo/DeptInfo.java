package com.dream.common.security.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 部门信息
 * <p>
 * 描述组织内部的部门层级信息，支持数据权限按部门隔离。
 * </p>
 *
 * @author dream
 * @since 2026-08-24
 */
@Data
public class DeptInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部门 ID
     */
    private String deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 父部门 ID（根部门为 "0"）
     */
    private String parentId;

    /**
     * 祖级列表（如 "0,100,101"），用于快速查询子树
     */
    private String ancestors;

    /**
     * 负责人
     */
    private String leader;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 状态（0 正常 / 1 停用）
     */
    private String status;
}
