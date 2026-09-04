package com.dream.common.security.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单树
 * <p>
 * 继承 {@link MenuInfo}，增加子节点列表，用于前端构建路由树和菜单树。
 * </p>
 *
 * @author dream
 * @since 2026-08-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MenuTree extends MenuInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 子菜单列表
     */
    private List<MenuTree> children = new ArrayList<>();

    /**
     * 将平铺菜单列表构建为树形结构。
     * <p>
     * 只构建 menuType 为 M（目录）或 C（菜单）的节点，按钮（F）不入树。
     * 时间复杂度 O(n)：先按 parentId 分组建索引，再一次性构建整棵树。
     * </p>
     *
     * @param menus        平铺菜单列表
     * @param rootParentId 根父 ID（通常为 "0"）
     * @return 树形菜单列表
     */
    public static List<MenuTree> build(List<MenuInfo> menus, String rootParentId) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 过滤可见菜单，按 parentId 分组（O(n)）
        Map<String, List<MenuInfo>> parentIdMap = new HashMap<>();
        List<MenuInfo> roots = new ArrayList<>();
        for (MenuInfo menu : menus) {
            if (!isMenuVisible(menu)) {
                continue;
            }
            String parentId = menu.getParentId();
            if (rootParentId != null && rootParentId.equals(parentId)) {
                roots.add(menu);
            } else if (parentId != null) {
                parentIdMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(menu);
            }
        }

        // 2. 递归构建子树（每个节点只访问一次，O(n)）
        List<MenuTree> tree = new ArrayList<>(roots.size());
        for (MenuInfo root : roots) {
            tree.add(buildNode(root, parentIdMap));
        }
        tree.sort(Comparator.comparingInt(m -> m.getSort() == null ? 0 : m.getSort()));
        return tree;
    }

    /**
     * 递归构建单个节点及其子树
     *
     * @param menu         当前菜单
     * @param parentIdMap  parentId → 子菜单列表索引
     * @return 树节点
     */
    private static MenuTree buildNode(MenuInfo menu, Map<String, List<MenuInfo>> parentIdMap) {
        MenuTree node = new MenuTree();
        copyProperties(menu, node);

        List<MenuInfo> children = parentIdMap.get(menu.getMenuId());
        if (children != null && !children.isEmpty()) {
            List<MenuTree> childNodes = new ArrayList<>(children.size());
            for (MenuInfo child : children) {
                childNodes.add(buildNode(child, parentIdMap));
            }
            childNodes.sort(Comparator.comparingInt(m -> m.getSort() == null ? 0 : m.getSort()));
            node.setChildren(childNodes);
        }
        return node;
    }

    /**
     * 判断菜单是否可入树（M 或 C 类型，且可见且正常状态）
     *
     * @param menu 菜单
     * @return true 可入树
     */
    private static boolean isMenuVisible(MenuInfo menu) {
        if (menu == null || menu.getMenuId() == null) {
            return false;
        }
        // F（按钮）不入树
        if ("F".equals(menu.getMenuType())) {
            return false;
        }
        // 停用不入树
        if ("1".equals(menu.getStatus())) {
            return false;
        }
        // 隐藏不入树
        if ("1".equals(menu.getVisible())) {
            return false;
        }
        return true;
    }

    /**
     * 复制 {@link MenuInfo} 属性到 {@link MenuTree}
     *
     * @param src  源
     * @param dest 目标
     */
    private static void copyProperties(MenuInfo src, MenuTree dest) {
        dest.setMenuId(src.getMenuId());
        dest.setMenuName(src.getMenuName());
        dest.setParentId(src.getParentId());
        dest.setSort(src.getSort());
        dest.setPath(src.getPath());
        dest.setComponent(src.getComponent());
        dest.setQuery(src.getQuery());
        dest.setIcon(src.getIcon());
        dest.setMenuType(src.getMenuType());
        dest.setPerms(src.getPerms());
        dest.setVisible(src.getVisible());
        dest.setIsFrame(src.getIsFrame());
        dest.setCache(src.getCache());
        dest.setStatus(src.getStatus());
    }

    /**
     * 从树中提取所有权限标识（perms）
     * <p>
     * 用于登录时收集用户拥有的全部权限码。
     * </p>
     *
     * @param menus 平铺菜单列表
     * @return 权限标识集合（去重）
     */
    public static List<String> extractPermissions(List<MenuInfo> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        return menus.stream()
                .filter(m -> m.getPerms() != null && !m.getPerms().trim().isEmpty())
                .map(MenuInfo::getPerms)
                .distinct()
                .collect(Collectors.toList());
    }
}
