package com.dream.common.core.vo.sys;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 菜单路由 VO。
 *
 * <p>通过 {@code GET /user/menus} 独立接口返回，<b>不参与登录响应</b>。
 * 字段与 Vue Router 配置一一对应，前端可直接用于动态注册路由。</p>
 *
 * <h2>字段分组</h2>
 * <ul>
 *   <li><b>标识</b>：menuId / parentId</li>
 *   <li><b>展示</b>：menuName / fullName / icon / sortOrder</li>
 *   <li><b>路由</b>：routePath / routeName / component / query / redirect</li>
 *   <li><b>类型权限</b>：menuType / perms</li>
 *   <li><b>显示控制</b>：visible / external / keepAlive / enabled / breadcrumb</li>
 *   <li><b>其他</b>：remark / extra / children</li>
 * </ul>
 *
 * <h2>类型选择说明</h2>
 * <ul>
 *   <li>{@code sortOrder} —— {@link Integer}，需数值比较（{@code 10 > 9}）</li>
 *   <li>{@code visible} / {@code external} / {@code keepAlive} / {@code enabled}
 *       / {@code breadcrumb} —— {@link Boolean}，严格二值</li>
 *   <li>{@code menuType} —— {@link String}，枚举 value（{@code M}/{@code C}/{@code F}）</li>
 * </ul>
 *
 * <h2>命名对齐</h2>
 * <ul>
 *   <li>{@code sortOrder} —— 词性明确，对齐 Spring Data {@code Sort.Order}</li>
 *   <li>{@code external} —— 业务语义清晰（是否外链），替代含糊的 {@code frame}</li>
 *   <li>{@code keepAlive} —— 对齐 Vue 生态标准叫法，替代含糊的 {@code cache}</li>
 *   <li>{@code enabled} —— 明确"启用/停用"，替代含糊的 {@code status}</li>
 * </ul>
 *
 * <h2>菜单类型</h2>
 * <ul>
 *   <li>{@code M} —— 目录（Directory）</li>
 *   <li>{@code C} —— 菜单（Component）</li>
 *   <li>{@code F} —— 按钮（Function）</li>
 * </ul>
 *
 * @author dream
 * @since 2026-09-10
 */
public class MenuVo implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 标识 ====================

    /** 菜单 ID */
    private String menuId;

    /** 父菜单 ID */
    private String parentId;

    // ==================== 展示 ====================

    /** 菜单名称 */
    private String menuName;

    /** 完整名称（如"系统管理/用户管理"） */
    private String fullName;

    /** 图标 */
    private String icon;

    /** 排序顺序（数值越小越靠前） */
    private Integer sortOrder;

    // ==================== 路由 ====================

    /** 路由地址（如 {@code user}） */
    private String routePath;

    /** 路由名称（如 {@code User}） */
    private String routeName;

    /** 组件路径（如 {@code system/user/index}） */
    private String component;

    /** 路由参数（JSON 字符串） */
    private String query;

    /** 重定向地址 */
    private String redirect;

    // ==================== 类型与权限 ====================

    /**
     * 菜单类型。
     *
     * <p>取值来源：{@link com.dream.common.core.enums.MenuType#getValue()}。</p>
     *
     * @see com.dream.common.core.enums.MenuType
     */
    private String menuType;

    /** 权限标识（按钮级，如 {@code system:user:list}） */
    private String perms;

    // ==================== 显示控制 ====================

    /** 是否可见（{@code false} 时不在菜单栏展示） */
    private Boolean visible = true;

    /** 是否外链（{@code true} 时路由地址为 http 开头，点击跳转外网） */
    private Boolean external = false;

    /** 是否缓存（{@code true} 时启用 Vue Keep-Alive） */
    private Boolean keepAlive = false;

    /** 是否启用（{@code false} 时不可访问） */
    private Boolean enabled = true;

    /** 是否显示面包屑 */
    private Boolean breadcrumb = true;

    // ==================== 其他 ====================

    /** 备注 */
    private String remark;

    /** 扩展属性 */
    private Map<String, Object> extra;

    /** 子菜单 */
    private List<MenuVo> children;

    // ==================== Getter / Setter ====================

    public String getMenuId() {
        return menuId;
    }

    public void setMenuId(String menuId) {
        this.menuId = menuId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    public String getPerms() {
        return perms;
    }

    public void setPerms(String perms) {
        this.perms = perms;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Boolean getExternal() {
        return external;
    }

    public void setExternal(Boolean external) {
        this.external = external;
    }

    public Boolean getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(Boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getBreadcrumb() {
        return breadcrumb;
    }

    public void setBreadcrumb(Boolean breadcrumb) {
        this.breadcrumb = breadcrumb;
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

    /**
     * 获取子菜单（空安全）。
     *
     * @return 子菜单列表，永不为 {@code null}
     */
    public List<MenuVo> getChildren() {
        return children == null ? Collections.emptyList() : children;
    }

    public void setChildren(List<MenuVo> children) {
        this.children = children;
    }
}