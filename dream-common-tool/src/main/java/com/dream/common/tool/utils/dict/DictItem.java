package com.dream.common.tool.utils.dict;

import com.dream.common.core.annotation.Tree;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 字典项
 * <p>
 * 标注 {@link Tree} 注解，自身即为树节点，可直接构建树形结构。
 * </p>
 * <p>
 * 用于表示一条字典数据，包含字典值、显示标签、图标、颜色、禁用状态、
 * 终止状态、父级值（树形字典）、排序号、扩展数据等字段。
 * </p>
 *
 * @author dream
 * @since 2026-08-21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Tree(
        id = "value",
        parentId = "parentValue",
        name = "label",
        children = "children",
        sort = "sort"
)
public class DictItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字典值（代码值）- 对应 Tree.id
     */
    private String value;

    /**
     * 字典标签（显示值）- 对应 Tree.name
     */
    private String label;

    /**
     * 图标名称
     */
    private String icon;

    /**
     * 颜色值
     */
    private String color;

    /**
     * 是否禁用
     */
    private boolean disabled;

    /**
     * 是否终止
     */
    private boolean stop;

    /**
     * 父级值（用于树形字典）- 对应 Tree.parentId
     */
    private String parentValue;

    /**
     * 排序号 - 对应 Tree.sort
     */
    private Integer sort;

    /**
     * 额外扩展数据
     */
    private Object extra;

    /**
     * 子节点列表 - 对应 Tree.children
     */
    private List<DictItem> children;

    public DictItem(String value, String label) {
        this(value, label, null, null, false, false, null, null, null, null);
    }

    public DictItem(String value, String label, String icon, String color, boolean disabled, boolean stop,
                    String parentValue, Integer sort, Object extra) {
        this(value, label, icon, color, disabled, stop, parentValue, sort, extra, null);
    }

    public boolean hasIcon() {
        return icon != null && !icon.isEmpty();
    }

    public boolean hasColor() {
        return color != null && !color.isEmpty();
    }

    public boolean isValid() {
        return !disabled && !stop;
    }
}