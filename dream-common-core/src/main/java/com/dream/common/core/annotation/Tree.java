package com.dream.common.core.annotation;

import com.dream.common.core.constant.SysKey;

import java.lang.annotation.*;

/**
 * 树结构注解
 * <p>
 * 标注在需要转换为树形结构的实体类或 DTO 上，
 * 用于指定树节点各属性的字段名映射关系。
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 基础用法：使用默认字段名（id / parentId / name / children / sort）
 * @Tree
 * public class Menu {
 *     private Long id;
 *     private Long parentId;
 *     private String name;
 *     private Integer sort;
 *     private List<Menu> children;
 * }
 *
 * // 自定义字段名映射
 * @Tree(
 *     id = "menuId",
 *     parentId = "pId",
 *     name = "menuName",
 *     children = "subMenus"
 * )
 * public class MenuDTO {
 *     private Long menuId;
 *     private Long pId;
 *     private String menuName;
 *     private List<MenuDTO> subMenus;
 * }
 *
 * // 限制最大深度（只展开前 3 层）
 * @Tree(maxDepth = 3)
 * public class Category {
 *     private Long id;
 *     private Long parentId;
 *     private String name;
 *     private List<Category> children;
 * }
 *
 * // 只包含指定字段（白名单模式）
 * @Tree(includeFields = "id,parentId,name,sort")
 * public class DeptTree {
 *     private Long id;
 *     private Long parentId;
 *     private String name;
 *     private Integer sort;
 *     private String phone;      // 不会出现在树节点中
 *     private String leader;     // 不会出现在树节点中
 *     private List<DeptTree> children;
 * }
 *
 * // 前端组件适配（Ant Design Tree 使用 key/title/children）
 * @Tree(
 *     id = "key",
 *     name = "title",
 *     children = "children"
 * )
 * public class AntdTreeNode {
 *     private String key;
 *     private String title;
 *     private String parentKey;
 *     private List<AntdTreeNode> children;
 * }
 * }</pre>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface Tree {

    // ==================== 核心字段映射 ====================

    /**
     * 节点 ID 字段名
     * <p>
     * 默认值：id
     */
    String id() default SysKey.TREE_ID;

    /**
     * 父节点 ID 字段名
     * <p>
     * 默认值：parentId
     */
    String parentId() default SysKey.TREE_PARENT_ID;

    /**
     * 节点名称字段名
     * <p>
     * 默认值：name
     */
    String name() default SysKey.TREE_NAME;

    /**
     * 节点标签字段名（前端组件显示用，如 label）
     * <p>
     * 默认值：label
     */
    String label() default SysKey.TREE_LABEL;

    /**
     * 子节点列表字段名
     * <p>
     * 默认值：children
     */
    String children() default SysKey.TREE_CHILDREN;

    // ==================== 排序相关 ====================

    /**
     * 排序号字段名
     * <p>
     * 默认值：sort
     */
    String sort() default SysKey.SORT;

    /**
     * 排序方向
     * <p>
     * {@link SortDir#ASC}：升序（sort 值从小到大，默认）
     * <p>
     * {@link SortDir#DESC}：降序（sort 值从大到小）
     */
    SortDir sortDir() default SortDir.ASC;

    /**
     * 是否按排序号排序
     * <p>
     * 默认值：true
     */
    boolean sortChildren() default true;

    // ==================== 树结构控制 ====================

    /**
     * 根节点 值
     * <p>
     *    ""  " " null 3者 都表示根节点
     * 默认值：""
     */
    String rootNodeValue() default SysKey.TREE_ROOT_NODE_VALUE;

    /**
     * 是否包含根节点
     * <p>
     * true：根节点作为树的顶层节点返回
     * <p>
     * false：从根节点的子级开始返回（默认）
     */
    boolean includeRoot() default false;

    /**
     * 虚拟根节点的 ID 值，仅 {@link #includeRoot()} 为 true 时使用
     * <p>默认值：root。多棵树同页展示或前端对节点 ID 有约定时可按需修改。</p>
     */
    String virtualRootId() default "root";

    /**
     * 虚拟根节点的显示名称，同时写入 {@link #name()} 与 {@link #label()} 字段，
     * 仅 {@link #includeRoot()} 为 true 时使用
     * <p>默认值：全部。</p>
     */
    String virtualRootName() default "全部";

    /**
     * 树的最大深度（0 表示不限制）
     * <p>
     * 超过深度的子节点将被截断，不再展开
     * <p>
     * 示例：maxDepth = 3 时，只展开到第 3 层
     */
    int maxDepth() default 0;

    // ==================== 状态字段 ====================

    /**
     * 层级字段名
     * <p>
     * 默认值：level
     */
    String level() default SysKey.TREE_LEVEL;

    /**
     * 是否禁用字段名
     * <p>
     * 默认值：disabled
     */
    String disabled() default SysKey.DISABLED;

    /**
     * 是否展开字段名
     * <p>
     * 默认值：expand
     */
    String expand() default SysKey.TREE_EXPAND;

    /**
     * 是否选中字段名
     * <p>
     * 默认值：checked
     */
    String checked() default SysKey.TREE_CHECKED;

    // ==================== 字段过滤 ====================

    /**
     * 排除的字段名（多个用逗号分隔，黑名单模式）
     * <p>
     * 默认排除：serialVersionUID
     * <p>
     * 与 {@link #includeFields()} 互斥，同时设置时以 includeFields 为准
     */
    String excludeFields() default SysKey.TREE_EXCLUDE_FIELDS;

    /**
     * 只包含的字段名（多个用逗号分隔，白名单模式）
     * <p>
     * 留空表示包含所有字段（默认）
     * <p>
     * 注意：id、parentId、children、sort 等树结构字段会自动包含，无需手动指定
     * <p>
     * 示例："id,parentId,name,sort,status"
     */
    String includeFields() default "";

    // ==================== 排序方向枚举 ====================

    /**
     * 排序方向枚举
     *
     * @author dream
     */
    enum SortDir {
        /**
         * 升序（默认）
         */
        ASC,
        /**
         * 降序
         */
        DESC
    }
}
