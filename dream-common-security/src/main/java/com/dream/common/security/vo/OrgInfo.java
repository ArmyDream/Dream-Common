package com.dream.common.security.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 机构信息
 * <p>
 * 描述企业/组织的机构层级信息，与 {@link DeptInfo}（部门）区分：
 * 机构为顶层组织划分，部门为机构下的业务单元。
 * </p>
 *
 * @author dream
 * @since 2026-08-24
 */
@Data
public class OrgInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 机构 ID
     */
    private String orgId;

    /**
     * 机构名称
     */
    private String orgName;

    /**
     * 父机构 ID（根机构为 "0"）
     */
    private String parentId;

    /**
     * 祖级列表（如 "0,1,2"），用于快速查询子树
     */
    private String ancestors;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 状态（0 正常 / 1 停用）
     */
    private String status;

    /**
     * 机构编码（如 "ORG001"）
     */
    private String orgCode;
}
