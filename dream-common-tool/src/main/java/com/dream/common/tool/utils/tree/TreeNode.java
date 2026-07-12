package com.dream.common.tool.utils.tree;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 树节点
 * <p>
 * 继承自 {@link LinkedHashMap}，以 Map 形式存储节点数据，
 * 配合 {@link TreeUtils} 和 {@link com.dream.common.core.annotation.Tree} 注解使用。
 * <p>
 * 特点：
 * <ul>
 *   <li>保留字段插入顺序（LinkedHashMap）</li>
 *   <li>实现 {@link Serializable}，支持序列化传输</li>
 * </ul>
 *
 */
public class TreeNode extends LinkedHashMap<String, Object> implements Serializable {

    private static final long serialVersionUID = 1L;

    public TreeNode() {
        super();
    }

    /**
     * 以初始容量创建节点
     *
     * @param initialCapacity 初始容量
     */
    public TreeNode(int initialCapacity) {
        super(initialCapacity);
    }

    // ==================== 类型安全访问器 ====================

    /**
     * 获取节点 ID（自动转 String）
     *
     * @param idField ID 字段名
     * @return ID 值字符串；不存在返回 null
     */
    public String getId(String idField) {
        Object value = get(idField);
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * 获取父节点 ID（自动转 String）
     *
     * @param parentIdField 父 ID 字段名
     * @return 父 ID 值字符串；不存在返回 null
     */
    public String getParentId(String parentIdField) {
        Object value = get(parentIdField);
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * 获取节点名称
     *
     * @param nameField 名称字段名
     * @return 名称字符串；不存在返回 null
     */
    public String getName(String nameField) {
        Object value = get(nameField);
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * 获取子节点列表（未转型，需要调用方自行强转）
     *
     * @param childrenField 子节点字段名
     * @return 子节点列表；不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public List<TreeNode> getChildren(String childrenField) {
        Object value = get(childrenField);
        if (value instanceof List) {
            return (List<TreeNode>) value;
        }
        return null;
    }

    /**
     * 获取排序值（自动转 Integer）
     *
     * @param sortField 排序字段名
     * @return 排序值；不存在或无法转换返回 0
     */
    public Integer getSort(String sortField) {
        Object value = get(sortField);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 设置子节点列表
     *
     * @param childrenField 子节点字段名
     * @param children      子节点列表
     */
    public void setChildren(String childrenField, List<TreeNode> children) {
        put(childrenField, children);
    }

    /**
     * 获取属性值并自动转 String
     *
     * @param key 属性名
     * @return 字符串值；不存在返回 null
     */
    public String getString(String key) {
        Object value = get(key);
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * 获取属性值并自动转 Integer
     *
     * @param key 属性名
     * @return Integer 值；不存在或无法转换返回 null
     */
    public Integer getInteger(String key) {
        Object value = get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取属性值并自动转 Long
     *
     * @param key 属性名
     * @return Long 值；不存在或无法转换返回 null
     */
    public Long getLong(String key) {
        Object value = get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取属性值并自动转 Boolean
     *
     * @param key 属性名
     * @return Boolean 值；不存在返回 false
     */
    public Boolean getBoolean(String key) {
        Object value = get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return false;
    }
}
