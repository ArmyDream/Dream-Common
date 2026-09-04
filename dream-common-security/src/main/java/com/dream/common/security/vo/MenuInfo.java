package com.dream.common.security.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 菜单信息
 * <p>
 * 描述前端导航菜单与按钮权限。menuType 取值参考 RuoYi 约定：
 * <ul>
 *   <li>{@code M}：目录（含子菜单，无页面）</li>
 *   <li>{@code C}：菜单（有路由页面）</li>
 *   <li>{@code F}：按钮（仅权限标识，不渲染）</li>
 * </ul>
 * </p>
 *
 * @author dream
 * @since 2026-08-24
 */
@Data
public class MenuInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 菜单 ID
     */
    private String menuId;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 父菜单 ID（根菜单为 "0"）
     */
    private String parentId;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 路由地址（如 "user"），对应 Vue Router 的 path
     */
    private String path;

    /**
     * 组件路径（如 "system/user/index"），对应 Vue 组件
     */
    private String component;

    /**
     * 路由参数（如 "{'key':'value'}"）
     */
    private String query;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 菜单类型：M 目录 / C 菜单 / F 按钮
     */
    private String menuType;

    /**
     * 权限标识（如 "system:user:list"），仅 F 类型或 C 类型按钮用
     */
    private String perms;

    /**
     * 是否可见（0 显示 / 1 隐藏）
     */
    private String visible;

    /**
     * 是否外链（0 是 / 1 否）
     */
    private String isFrame;

    /**
     * 是否缓存（0 缓存 / 1 不缓存）
     */
    private String cache;

    /**
     * 状态（0 正常 / 1 停用）
     */
    private String status;
}
