package com.dream.common.core.vo.sys;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 用户档案 VO。
 *
 * <p>通过 {@code GET /user/profile} 独立接口返回，同时序列化到 Redis
 * （Key: {@code dream:security:user:{userId}}），后续请求从缓存读取。</p>
 *
 * <h2>字段分组</h2>
 * <ul>
 *   <li><b>身份标识</b>：userId / username / nickname / shortName / avatar</li>
 *   <li><b>联系方式</b>：email / mobile / telephone</li>
 *   <li><b>个人属性</b>：gender / birthday</li>
 *   <li><b>账号状态</b>：userType / passwordChanged</li>
 *   <li><b>组织归属</b>：org / dept / posts</li>
 *   <li><b>授权信息</b>：roles / permissions / superAdmin</li>
 *   <li><b>租户信息</b>：tenantId / tenantName</li>
 *   <li><b>最近登录</b>：lastLoginTime / lastLoginIp / lastLoginLocation</li>
 *   <li><b>备注与扩展</b>：remark / extra</li>
 * </ul>
 *
 * <h2>类型选择说明</h2>
 * <ul>
 *   <li>{@code gender} —— {@link String}，枚举 value</li>
 *   <li>{@code userType} —— {@link String}，业务可扩展</li>
 *   <li>{@code passwordChanged} —— {@link Boolean}，严格二值（{@code false} 需强制改密）</li>
 *   <li>{@code superAdmin} —— {@link Boolean}，严格二值</li>
 * </ul>
 *
 * <h2>空安全约定</h2>
 * <p>集合类字段（posts / roles / permissions）的 getter 返回空集合而非 {@code null}，
 * 调用方无需判空即可遍历。Map 类型的 {@code extra} 同理。</p>
 *
 * @author dream
 * @since 2026-09-10
 */
public class UserProfileVo implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== ① 身份标识 ====================

    /** 用户 ID */
    private String userId;

    /** 登录账号 */
    private String username;

    /** 昵称 / 姓名 */
    private String nickname;

    /** 姓名简称 */
    private String shortName;

    /** 头像 URL */
    private String avatar;

    // ==================== ② 联系方式 ====================

    /** 邮箱（建议脱敏） */
    private String email;

    /** 手机号（建议脱敏） */
    private String mobile;

    /** 座机 */
    private String telephone;

    // ==================== ③ 个人属性 ====================

    /**
     * 性别。
     *
     * <p>取值来源：{@link com.dream.common.core.enums.Gender#getValue()}。</p>
     *
     * @see com.dream.common.core.enums.Gender
     */
    private String gender;

    /** 生日（ISO 8601 格式，如 {@code 1990-01-01}） */
    private String birthday;

    // ==================== ④ 账号状态 ====================

    /**
     * 用户类型。
     *
     * <p>业务自定义，如 {@code 0=系统用户}、{@code 1=普通用户}。</p>
     */
    private String userType;

    /** 密码是否已修改（{@code false} 表示首次登录需强制改密） */
    private Boolean passwordChanged = true;

    // ==================== ⑤ 组织归属 ====================

    /** 所属机构 */
    private OrgVo org;

    /** 所属部门 */
    private DeptVo dept;

    /**
     * 本部门子部门ID列表（登录时预计算）。
     * <p>用于数据范围 DEPT_AND_CHILD 的 SQL 过滤，避免拦截器内递归查询。</p>
     */
    private List<String> deptChildIds;

    /**
     * 所属机构子部门ID列表（登录时预计算）。
     * <p>用于数据范围 ORG / ORG_AND_CHILD 的 SQL 过滤，避免拦截器内递归查询。</p>
     */
    private List<String> orgChildDeptIds;

    /** 兼任岗位 */
    private List<PostVo> posts;

    // ==================== ⑥ 授权信息 ====================

    /** 角色列表 */
    private List<RoleVo> roles;

    /** 权限标识列表（如 {@code system:user:list}） */
    private List<String> permissions;

    /** 是否超级管理员 */
    private Boolean superAdmin = false;

    /**
     * 数据范围（登录时多角色合并后的最终结果）。
     * <p>取值来源：{@link com.dream.common.core.enums.DataScope#getValue()}
     * 的英文字面量（ALL/CUSTOM/ORG/ORG_AND_CHILD/DEPT_AND_CHILD/DEPT/SELF）。</p>
     */
    private String dataScope;

    // ==================== ⑦ 租户信息 ====================

    /** 租户 ID */
    private String tenantId;

    /** 租户名称 */
    private String tenantName;

    // ==================== ⑧ 最近登录审计 ====================

    /** 最近一次登录时刻（时间戳，毫秒） */
    private Long lastLoginTime;

    /** 最近一次登录 IP */
    private String lastLoginIp;

    /** 最近一次登录地（IP 解析结果） */
    private String lastLoginLocation;

    // ==================== ⑨ 备注与扩展 ====================

    /** 备注 */
    private String remark;

    /** 业务扩展属性 */
    private Map<String, Object> extra;

    // ==================== Getter / Setter ====================

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public Boolean getPasswordChanged() {
        return passwordChanged;
    }

    public void setPasswordChanged(Boolean passwordChanged) {
        this.passwordChanged = passwordChanged;
    }

    public OrgVo getOrg() {
        return org;
    }

    public void setOrg(OrgVo org) {
        this.org = org;
    }

    public DeptVo getDept() {
        return dept;
    }

    public void setDept(DeptVo dept) {
        this.dept = dept;
    }

    /**
     * 获取本部门子部门ID列表（空安全）。
     *
     * @return 子部门ID列表，永不为 {@code null}
     */
    public List<String> getDeptChildIds() {
        return deptChildIds == null ? Collections.emptyList() : deptChildIds;
    }

    public void setDeptChildIds(List<String> deptChildIds) {
        this.deptChildIds = deptChildIds;
    }

    /**
     * 获取所属机构子部门ID列表（空安全）。
     *
     * @return 子部门ID列表，永不为 {@code null}
     */
    public List<String> getOrgChildDeptIds() {
        return orgChildDeptIds == null ? Collections.emptyList() : orgChildDeptIds;
    }

    public void setOrgChildDeptIds(List<String> orgChildDeptIds) {
        this.orgChildDeptIds = orgChildDeptIds;
    }

    /**
     * 获取岗位列表（空安全）。
     *
     * @return 岗位列表，永不为 {@code null}
     */
    public List<PostVo> getPosts() {
        return posts == null ? Collections.emptyList() : posts;
    }

    public void setPosts(List<PostVo> posts) {
        this.posts = posts;
    }

    /**
     * 获取角色列表（空安全）。
     *
     * @return 角色列表，永不为 {@code null}
     */
    public List<RoleVo> getRoles() {
        return roles == null ? Collections.emptyList() : roles;
    }

    public void setRoles(List<RoleVo> roles) {
        this.roles = roles;
    }

    /**
     * 获取权限标识列表（空安全）。
     *
     * @return 权限标识列表，永不为 {@code null}
     */
    public List<String> getPermissions() {
        return permissions == null ? Collections.emptyList() : permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public Boolean getSuperAdmin() {
        return superAdmin;
    }

    public void setSuperAdmin(Boolean superAdmin) {
        this.superAdmin = superAdmin;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public Long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(Long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public String getLastLoginLocation() {
        return lastLoginLocation;
    }

    public void setLastLoginLocation(String lastLoginLocation) {
        this.lastLoginLocation = lastLoginLocation;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 获取扩展属性（空安全）。
     *
     * @return 扩展属性 Map，永不为 {@code null}
     */
    public Map<String, Object> getExtra() {
        return extra == null ? Collections.emptyMap() : extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }
}