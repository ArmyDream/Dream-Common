package com.dream.common.tool.utils.tree;

import com.dream.common.core.annotation.Tree;
import com.dream.common.core.enums.StringPool;
import com.dream.common.tool.utils.ReflectionUtils;
import com.dream.common.tool.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 树工具类
 * <p>
 * 配合 {@link Tree} 注解使用，将扁平的实体列表转换为树形结构。
 *
 * <p><b>根节点判断规则（满足任一条件即为根节点）：</b>
 * <ul>
 *   <li>parentId 为 null</li>
 *   <li>parentId 为空字符串 ""</li>
 *   <li>parentId 为空白字符串（如 "  "、"\t"）</li>
 *   <li>parentId 等于 rootNodeValue（注解配置值，默认 ""）</li>
 * </ul>
 *
 * <p><b>使用示例：</b>
 * <pre>{@code
 * // 实体类标注 @Tree 注解
 * @Tree(id = "code", parentId = "parentCode", name = "name")
 * class Dict {
 *     private String code;
 *     private String parentCode;
 *     private String name;
 *     private List<Dict> children;
 * }
 *
 * // 构建树
 * List<TreeNode> tree = TreeUtils.buildTree(dictList);
 * }</pre>
 *
 */
@SuppressWarnings("all")
@Slf4j
public final class TreeUtils {

    private TreeUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 公开 API ====================

    /**
     * 构建树（使用注解中的 rootNodeValue）
     *
     * @param list 扁平实体列表
     * @return 树形节点列表
     */
    public static List<TreeNode> buildTree(List<?> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        Tree tree = getTreeAnnotation(list);
        if (tree == null) {
            return new ArrayList<>();
        }

        return buildTree(list, tree.rootNodeValue());
    }

    /**
     * 构建树（指定根节点值）
     *
     * @param list          扁平实体列表
     * @param rootNodeValue 根节点父 ID 值（null、空、空白 都表示根节点）
     * @return 树形节点列表
     */
    public static List<TreeNode> buildTree(List<?> list, String rootNodeValue) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        Tree tree = getTreeAnnotation(list);
        if (tree == null) {
            return new ArrayList<>();
        }

        // 1. 转换为扁平节点列表
        List<TreeNode> allNodes = toNodeList(list);
        if (allNodes.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 找出根节点
        String effectiveRootValue = StringUtils.isBlank(rootNodeValue) ? "" : rootNodeValue;
        List<TreeNode> roots = allNodes.stream()
                .filter(node -> isRootNode(node.get(tree.parentId()), effectiveRootValue))
                .collect(Collectors.toList());

        if (roots.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 构建父子映射（父ID → 子节点列表）
        Map<String, List<TreeNode>> parentIdMap = allNodes.stream()
                .filter(node -> !isRootNode(node.get(tree.parentId()), effectiveRootValue))
                .collect(Collectors.groupingBy(
                        node -> node.getString(tree.parentId()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 4. 递归构建树
        String idField = tree.id();
        String childrenField = tree.children();
        String sortField = tree.sort();
        boolean sortChildren = tree.sortChildren();
        Tree.SortDir sortDir = tree.sortDir();

        for (TreeNode root : roots) {
            buildChildren(root, allNodes, parentIdMap, tree, 1);
        }

        // 5. 排序根节点
        if (sortChildren) {
            sortNodes(roots, sortField, sortDir);
        }

        // 6. 是否包含根节点（虚拟根节点）
        if (tree.includeRoot()) {
            TreeNode virtualRoot = createVirtualRoot(tree, roots);
            return Collections.singletonList(virtualRoot);
        }

        return roots;
    }

    /**
     * 转换为扁平节点列表
     */
    public static List<TreeNode> toNodeList(List<?> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        Tree tree = getTreeAnnotation(list);
        if (tree == null) {
            return new ArrayList<>();
        }

        // 字段过滤
        Set<String> includeFields = parseIncludeFields(tree);
        Set<String> excludeFields = parseExcludeFields(tree);
        boolean useInclude = !includeFields.isEmpty();

        List<Field> allFields = ReflectionUtils.getAllFields(list.get(0).getClass());

        List<Field> filteredFields = new ArrayList<>();
        for (Field field : allFields) {
            int mod = field.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
                continue;
            }
            String name = field.getName();
            if (useInclude) {
                if (isFieldIncluded(tree, includeFields, name)) {
                    filteredFields.add(field);
                }
            } else {
                if (!excludeFields.contains(name)) {
                    filteredFields.add(field);
                }
            }
        }

        if (filteredFields.isEmpty()) {
            return new ArrayList<>();
        }

        String idField = tree.id();
        String parentIdField = tree.parentId();

        List<TreeNode> nodes = new ArrayList<>(list.size());

        for (Object obj : list) {
            TreeNode node = new TreeNode(filteredFields.size() + 4);
            for (Field field : filteredFields) {
                String key = field.getName();
                Object value = ReflectionUtils.getFieldValue(obj, key);
                node.put(key, value);
            }

            // 校验核心字段
            if (!node.containsKey(idField) || !node.containsKey(parentIdField)) {
                return new ArrayList<>();
            }

            nodes.add(node);
        }

        return nodes;
    }

    // ==================== 节点查找 ====================

    public static TreeNode findNodeById(List<?> list, Object id) {
        List<TreeNode> nodes = toNodeList(list);
        if (nodes.isEmpty() || id == null) {
            return null;
        }

        Tree tree = getTreeAnnotation(list);
        if (tree == null) {
            return null;
        }

        String idStr = String.valueOf(id);
        return nodes.stream()
                .filter(node -> nodeIdEquals(idStr, node.get(tree.id())))
                .findFirst()
                .orElse(null);
    }

    public static List<TreeNode> findNodesByParentId(List<?> list, Object parentId) {
        List<TreeNode> nodes = toNodeList(list);
        if (nodes.isEmpty() || parentId == null) {
            return new ArrayList<>();
        }

        Tree tree = getTreeAnnotation(list);
        if (tree == null) {
            return new ArrayList<>();
        }

        String parentIdStr = String.valueOf(parentId);
        return nodes.stream()
                .filter(node -> nodeIdEquals(parentIdStr, node.get(tree.parentId())))
                .collect(Collectors.toList());
    }

    public static List<TreeNode> getParentsById(List<?> list, Object id, boolean isInclude) {
        return traverseById(list, id, isInclude, true);
    }

    public static List<TreeNode> getChildrenById(List<?> list, Object id, boolean isInclude) {
        return traverseById(list, id, isInclude, false);
    }

    // ==================== 私有方法 ====================

    /**
     * 递归构建子节点
     */
    private static void buildChildren(TreeNode node, List<TreeNode> allNodes,
                                      Map<String, List<TreeNode>> parentIdMap,
                                      Tree tree, int depth) {
        String idField = tree.id();
        String childrenField = tree.children();
        String sortField = tree.sort();
        String levelField = tree.level();
        int maxDepth = tree.maxDepth();
        boolean sortChildren = tree.sortChildren();
        Tree.SortDir sortDir = tree.sortDir();

        String nodeId = node.getString(idField);

        // 设置层级
        node.put(levelField, depth);

        // 获取子节点
        List<TreeNode> children = parentIdMap.getOrDefault(nodeId, new ArrayList<>());

        // 限制深度
        if (maxDepth > 0 && depth >= maxDepth) {
            node.put(childrenField, new ArrayList<>());
            return;
        }

        // 递归构建子节点的子节点
        for (TreeNode child : children) {
            buildChildren(child, allNodes, parentIdMap, tree, depth + 1);
        }

        // 排序
        if (sortChildren) {
            sortNodes(children, sortField, sortDir);
        }

        node.put(childrenField, children);
    }

    /**
     * 排序节点列表
     */
    private static void sortNodes(List<TreeNode> nodes, String sortField, Tree.SortDir sortDir) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        Comparator<TreeNode> comparator = Comparator.comparingInt(
                node -> node.getSort(sortField)
        );

        if (sortDir == Tree.SortDir.DESC) {
            comparator = comparator.reversed();
        }

        nodes.sort(comparator);
    }

    /**
     * 创建虚拟根节点（用于 includeRoot = true）
     */
    private static TreeNode createVirtualRoot(Tree tree, List<TreeNode> roots) {
        TreeNode root = new TreeNode();

        String idField = tree.id();
        String nameField = tree.name();
        String labelField = tree.label();
        String childrenField = tree.children();
        String levelField = tree.level();
        String expandField = tree.expand();
        String checkedField = tree.checked();

        root.put(idField, "root");
        root.put(nameField, "全部");
        root.put(labelField, "全部");
        root.put(childrenField, roots);
        root.put(levelField, 0);
        root.put(expandField, false);
        root.put(checkedField, false);

        return root;
    }

    /**
     * 子查父 / 父查子 遍历
     */
    private static List<TreeNode> traverseById(List<?> list, Object id, boolean isInclude, boolean childToParent) {
        if (list == null || list.isEmpty() || id == null) {
            return new ArrayList<>();
        }

        List<TreeNode> nodes = toNodeList(list);
        if (nodes.isEmpty()) {
            return new ArrayList<>();
        }

        Tree tree = getTreeAnnotation(list);
        if (tree == null) {
            return new ArrayList<>();
        }

        String idField = tree.id();
        String parentIdField = tree.parentId();
        String rootNodeValue = tree.rootNodeValue();
        String idStr = String.valueOf(id);

        TreeNode curNode = nodes.stream()
                .filter(node -> nodeIdEquals(idStr, node.get(idField)))
                .findFirst()
                .orElse(null);

        if (curNode == null) {
            return new ArrayList<>();
        }

        List<TreeNode> result = new ArrayList<>();

        if (isInclude) {
            result.add(curNode);
        }

        if (childToParent) {
            // 子查父：向上遍历
            Object currentParentId = curNode.get(parentIdField);

            while (!isRootNode(currentParentId, rootNodeValue)) {
                TreeNode found = null;
                String currentParentIdStr = String.valueOf(currentParentId);

                for (TreeNode node : nodes) {
                    if (nodeIdEquals(currentParentIdStr, node.get(idField))) {
                        found = node;
                        break;
                    }
                }

                if (found == null) {
                    break;
                }

                result.add(found);
                currentParentId = found.get(parentIdField);
            }
        } else {
            // 父查子：向下遍历
            Set<String> collectedIds = new LinkedHashSet<>();
            String curId = curNode.getString(idField);

            if (curId != null) {
                collectedIds.add(curId);
            }

            boolean changed = true;
            while (changed) {
                changed = false;

                for (TreeNode node : nodes) {
                    String nodeParentId = node.getString(parentIdField);

                    if (nodeParentId != null && collectedIds.contains(nodeParentId)) {
                        String nodeId = node.getString(idField);

                        if (nodeId != null && collectedIds.add(nodeId)) {
                            result.add(node);
                            changed = true;
                        }
                    }
                }
            }
        }

        return result;
    }

    // ==================== 核心判断方法 ====================

    /**
     * 判断是否为根节点
     * <p>
     * 满足以下任一条件即为根节点：
     * <ul>
     *   <li>value 为 null</li>
     *   <li>value 为空字符串 ""</li>
     *   <li>value 为空白字符串（如 "  "、" "）</li>
     *   <li>value 等于 rootParentId</li>
     * </ul>
     */
    private static boolean isRootNode(Object value, String rootParentId) {
        if (value == null) {
            return true;
        }

        if (value instanceof CharSequence) {
            String str = value.toString().trim();
            if (str.isEmpty()) {
                return true;
            }
            return str.equals(rootParentId);
        }

        return rootParentId.equals(String.valueOf(value));
    }

    /**
     * 节点 ID 比较
     */
    private static boolean nodeIdEquals(String expected, Object actual) {
        if (expected == null && actual == null) {
            return true;
        }
        if (expected == null || actual == null) {
            return false;
        }
        return expected.equals(String.valueOf(actual));
    }

    // ==================== 辅助方法 ====================

    private static Tree getTreeAnnotation(List<?> list) {
        if (list == null || list.isEmpty() || list.get(0) == null) {
            return null;
        }
        return ReflectionUtils.getAnnotation(list.get(0).getClass(), Tree.class);
    }

    private static Set<String> parseIncludeFields(Tree tree) {
        if (tree == null || StringUtils.isBlank(tree.includeFields())) {
            return Collections.emptySet();
        }

        return Arrays.stream(tree.includeFields().split(StringPool.COMMA.getValue()))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    private static Set<String> parseExcludeFields(Tree tree) {
        if (tree == null || StringUtils.isBlank(tree.excludeFields())) {
            return Collections.emptySet();
        }

        return Arrays.stream(tree.excludeFields().split(StringPool.COMMA.getValue()))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    private static boolean isFieldIncluded(Tree tree, Set<String> includeFields, String fieldName) {
        // 核心树字段始终包含
        if (tree.id().equals(fieldName) || tree.parentId().equals(fieldName)
                || tree.children().equals(fieldName) || tree.sort().equals(fieldName)) {
            return true;
        }

        return includeFields.contains(fieldName);
    }

    // ==================== 树打印 ====================

    /**
     * 打印树形结构（控制台输出，用于调试）
     * <p>
     * 输出示例：
     * <pre>
     * ├─ 系统管理 (id=1, level=1)
     * │  ├─ 用户管理 (id=2, level=2)
     * │  │  ├─ 用户列表 (id=3, level=3)
     * │  │  └─ 用户角色 (id=4, level=3)
     * │  └─ 菜单管理 (id=5, level=2)
     * └─ 日志管理 (id=6, level=1)
     * </pre>
     *
     * @param treeNodes 树节点列表
     * @param tree      Tree 注解
     */
    public static void printTree(List<TreeNode> treeNodes, Tree tree) {
        if (treeNodes == null || treeNodes.isEmpty() || tree == null) {
            log.info("(空树)");
            return;
        }

        String nameField = tree.name();
        String idField = tree.id();
        String parentIdField = tree.parentId();
        String childrenField = tree.children();
        String levelField = tree.level();

        for (int i = 0; i < treeNodes.size(); i++) {
            TreeNode node = treeNodes.get(i);
            boolean isLast = (i == treeNodes.size() - 1);
            printNode(node, "", isLast, nameField, idField, parentIdField, childrenField, levelField);
        }
    }

    /**
     * 递归打印节点
     */
    private static void printNode(TreeNode node, String indent, boolean isLast,
                                  String nameField, String idField, String parentIdField,
                                  String childrenField, String levelField) {
        String connector = isLast ? "└─ " : "├─ ";
        String name = node.getString(nameField);
        String id = node.getString(idField);
        Object level = node.get(levelField);

        log.info(indent + connector + name +
                " (id=" + id +
                ", level=" + level + ")");

        List<TreeNode> children = node.getChildren(childrenField);
        if (children != null && !children.isEmpty()) {
            String childIndent = indent + (isLast ? "   " : "│  ");
            for (int i = 0; i < children.size(); i++) {
                TreeNode child = children.get(i);
                boolean childIsLast = (i == children.size() - 1);
                printNode(child, childIndent, childIsLast,
                        nameField, idField, parentIdField, childrenField, levelField);
            }
        }
    }

    /**
     * 打印树形结构（使用注解中的配置）
     *
     * @param list 扁平实体列表（需标注 @Tree 注解）
     */
    public static void printTree(List<?> list) {
        if (list == null || list.isEmpty()) {
            log.info("(空列表)");
            return;
        }

        Tree tree = getTreeAnnotation(list);
        if (tree == null) {
            log.info("(未标注 @Tree 注解)");
            return;
        }

        List<TreeNode> treeNodes = buildTree(list);
        printTree(treeNodes, tree);
    }
}