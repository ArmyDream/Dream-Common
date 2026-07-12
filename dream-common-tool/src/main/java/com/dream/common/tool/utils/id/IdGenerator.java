package com.dream.common.tool.utils.id;

/**
 * ID 生成器接口
 * <p>
 * 统一 ID 生成规范，便于扩展和替换
 *
 * @param <T> ID 类型
 */
public interface IdGenerator<T> {

    /**
     * 生成下一个 ID
     *
     * @return ID 值
     */
    T nextId();

    /**
     * 生成下一个 ID（字符串形式）
     */
    default String nextIdString() {
        T id = nextId();
        return id != null ? String.valueOf(id) : null;
    }
}