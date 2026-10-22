package com.dream.common.core.vo.sys;

import java.io.Serializable;

/**
 * 角色信息 VO。
 *
 * @author dream
 * @since 2026-09-10
 */
public class RoleVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色 ID */
    private String roleId;

    /**
     * 角色编码（如 {@code admin}）。
     *
     * <p>取值来源：{@link com.dream.common.core.enums.RoleType#getValue()}。</p>
     *
     * @see com.dream.common.core.enums.RoleType
     */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /**
     * 数据范围。
     *
     * <p>取值为 {@link com.dream.common.core.enums.DataScope} 枚举的 value（字符串），
     * 例如 {@code "ALL"} / {@code "DEPT_AND_CHILD"} / {@code "SELF"}。</p>
     *
     * @see com.dream.common.core.enums.DataScope
     */
    private String dataScope;

    /** 备注 */
    private String remark;

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}