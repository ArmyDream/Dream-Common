package com.dream.common.core.enums;

/**
 * 内置数据权限范围枚举。
 *
 * <p>覆盖常见的组织架构、个人、多租户、业务区域、项目团队等场景。
 * 业务方可直接使用，也可自行定义枚举实现 {@link BaseEnum}。</p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 按 value 查找
 * DataScope scope = DataScope.fromValue("DEPT_AND_CHILD");
 *
 * // 按 label 查找
 * DataScope scope = DataScope.fromLabel("本部门及子部门数据");
 *
 * // 判断是否需要递归子级
 * boolean recursive = DataScope.DEPT_AND_CHILD.isRecursiveChild();
 *
 * // 判断是否需要自定义 ID 列表
 * boolean custom = DataScope.CUSTOM.isCustom();
 * }</pre>
 *
 * @author dream
 * @see BaseEnum
 * @since 2026-09-11
 */
public enum DataScope implements BaseEnum<String> {

    // ==================== 全量 / 自定义 ====================

    /**
     * 全部数据。
     * <p>无视任何限制，查询全量数据。通常仅授予超级管理员。</p>
     */
    ALL("ALL", "全部数据"),

    /**
     * 自定义数据。
     * <p>配合 {@code customDeptIds} 等字段使用，业务方手工指定可访问的 ID 列表。</p>
     */
    CUSTOM("CUSTOM", "自定义数据"),

    // ==================== 部门维度 ====================

    /**
     * 本部门数据。
     * <p>仅可见当前用户所在部门的数据。</p>
     */
    DEPT("DEPT", "本部门数据"),

    /**
     * 本部门及子部门数据。
     * <p>当前部门 + 递归所有子部门的数据，最常见的组织级数据范围。</p>
     */
    DEPT_AND_CHILD("DEPT_AND_CHILD", "本部门及子部门数据"),

    /**
     * 本部门及上级部门数据。
     * <p>当前部门 + 递归所有父部门的数据，常用于向上汇报的场景。</p>
     */
    DEPT_AND_PARENT("DEPT_AND_PARENT", "本部门及上级部门数据"),

    // ==================== 机构维度 ====================

    /**
     * 本机构数据。
     * <p>仅可见当前用户所在机构（如分公司）的数据。</p>
     */
    ORG("ORG", "本机构数据"),

    /**
     * 本机构及子机构数据。
     * <p>当前机构 + 递归所有子机构的数据。</p>
     */
    ORG_AND_CHILD("ORG_AND_CHILD", "本机构及子机构数据"),

    // ==================== 个人维度 ====================

    /**
     * 仅本人数据。
     * <p>仅可见当前登录用户自己创建的数据，适合普通用户。</p>
     */
    SELF("SELF", "仅本人数据"),

    /**
     * 本人及下属数据。
     * <p>当前用户 + 所有下属用户的数据，适合主管角色。</p>
     */
    SELF_AND_SUB("SELF_AND_SUB", "本人及下属数据"),

    // ==================== 多租户维度 ====================

    /**
     * 本租户数据。
     * <p>仅可见当前租户的数据，SaaS 场景常用。</p>
     */
    TENANT("TENANT", "本租户数据"),

    /**
     * 本租户及子租户数据。
     * <p>当前租户 + 递归所有子租户的数据。</p>
     */
    TENANT_AND_CHILD("TENANT_AND_CHILD", "本租户及子租户数据"),

    // ==================== 业务区域维度 ====================

    /**
     * 门店数据。
     * <p>仅可见当前门店的数据，零售、连锁餐饮等场景常用。</p>
     */
    STORE("STORE", "本门店数据"),

    /**
     * 门店及子门店数据。
     * <p>当前门店 + 递归所有子门店的数据。</p>
     */
    STORE_AND_CHILD("STORE_AND_CHILD", "本门店及子门店数据"),

    /**
     * 行政区划数据。
     * <p>仅可见当前行政区划的数据，政务、物流等场景常用。</p>
     */
    AREA("AREA", "本行政区划数据"),

    /**
     * 行政区划及子区划数据。
     * <p>当前区划 + 递归所有子区划的数据。</p>
     */
    AREA_AND_CHILD("AREA_AND_CHILD", "本行政区划及子区划数据"),

    // ==================== 项目 / 团队维度 ====================

    /**
     * 本项目数据。
     * <p>仅可见当前项目的数据，研发管理、工程管理等场景常用。</p>
     */
    PROJECT("PROJECT", "本项目数据"),

    /**
     * 本项目及子项目数据。
     * <p>当前项目 + 递归所有子项目的数据。</p>
     */
    PROJECT_AND_CHILD("PROJECT_AND_CHILD", "本项目及子项目数据"),

    /**
     * 本团队数据。
     * <p>仅可见当前团队的数据。</p>
     */
    TEAM("TEAM", "本团队数据"),

    /**
     * 本团队及子团队数据。
     * <p>当前团队 + 递归所有子团队的数据。</p>
     */
    TEAM_AND_CHILD("TEAM_AND_CHILD", "本团队及子团队数据");

    /**
     * 枚举值（存储到数据库 / 传输给前端）
     */
    private final String value;

    /**
     * 中文标签
     */
    private final String label;

    /**
     * 构造器。
     *
     * @param value 枚举值
     * @param label 中文标签
     */
    DataScope(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    // ==================== 便捷判断 ====================

    /**
     * 判断是否为"全部数据"。
     *
     * @return 是返回 {@code true}
     */
    public boolean isAll() {
        return this == ALL;
    }

    /**
     * 判断是否为"自定义数据"（需配合 ID 列表）。
     *
     * @return 是返回 {@code true}
     */
    public boolean isCustom() {
        return this == CUSTOM;
    }

    /**
     * 判断是否为"仅本人"。
     *
     * @return 是返回 {@code true}
     */
    public boolean isSelf() {
        return this == SELF;
    }

    /**
     * 判断是否需要递归查询子级。
     *
     * <p>所有以 {@code _AND_CHILD} 结尾的范围都需要递归。</p>
     *
     * @return 需要递归返回 {@code true}
     */
    public boolean isRecursiveChild() {
        return name().endsWith("_AND_CHILD");
    }

    /**
     * 判断是否为组织架构相关范围（部门 / 机构 / 门店 / 区划 / 团队 / 项目）。
     *
     * @return 是返回 {@code true}
     */
    public boolean isOrganizationScope() {
        return this == DEPT || this == DEPT_AND_CHILD || this == DEPT_AND_PARENT
                || this == ORG || this == ORG_AND_CHILD
                || this == STORE || this == STORE_AND_CHILD
                || this == AREA || this == AREA_AND_CHILD
                || this == PROJECT || this == PROJECT_AND_CHILD
                || this == TEAM || this == TEAM_AND_CHILD;
    }

    /**
     * 判断是否为多租户范围。
     *
     * @return 是返回 {@code true}
     */
    public boolean isTenantScope() {
        return this == TENANT || this == TENANT_AND_CHILD;
    }

    /**
     * 判断是否需要额外的 ID 列表（如自定义部门 ID、租户 ID）。
     *
     * @return 需要返回 {@code true}
     */
    public boolean needsIdList() {
        return this == CUSTOM;
    }

    /**
     * 判断是否为管理级别范围（ALL / CUSTOM）。
     *
     * @return 是返回 {@code true}
     */
    public boolean isAdminLevel() {
        return this == ALL || this == CUSTOM;
    }
}