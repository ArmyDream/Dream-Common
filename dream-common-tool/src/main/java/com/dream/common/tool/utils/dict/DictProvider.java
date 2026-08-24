package com.dream.common.tool.utils.dict;

import java.util.List;

/**
 * 字典数据提供者接口
 * <p>
 * 由业务模块实现，用于从数据库/缓存/外部系统加载字典数据，并提供字典的增删改查能力。
 * 微服务环境下，各服务可独立实现，自行决定是否使用缓存。
 * </p>
 *
 * <h2>能力划分</h2>
 * <ul>
 *   <li><b>读取</b>：{@link #getDictItems(String)}、{@link #getDictItem(String, String)}、{@link #supports(String)}</li>
 *   <li><b>写入</b>：{@link #addDictItem(String, DictItem)}、{@link #addDictItems(String, List)}、
 *       {@link #updateDictItem(String, DictItem)}</li>
 *   <li><b>删除</b>：{@link #removeDictItem(String, String)}、{@link #removeDictType(String)}</li>
 *   <li><b>缓存</b>：{@link #refreshDict(String)}、{@link #refreshAll()}</li>
 * </ul>
 *
 * <h2>只读提供者</h2>
 * <p>
 * 基于硬编码 mapping 的只读提供者无需实现写入/删除方法，默认实现会抛出
 * {@link UnsupportedOperationException}，调用方可通过 {@link #isReadOnly()} 提前判断。
 * </p>
 *
 * <h2>实现示例</h2>
 * <pre>{@code
 * @Slf4j
 * public class DatabaseDictProvider implements DictProvider {
 *     private final DictService dictService;
 *
 *     public DatabaseDictProvider(DictService dictService) {
 *         this.dictService = dictService;
 *     }
 *
 *     @Override
 *     public List<DictItem> getDictItems(String dictType) {
 *         return dictService.listByType(dictType);
 *     }
 *
 *     @Override
 *     public void addDictItem(String dictType, DictItem item) {
 *         dictService.save(dictType, item);
 *     }
 *
 *     @Override
 *     public void removeDictItem(String dictType, String value) {
 *         dictService.delete(dictType, value);
 *     }
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
public interface DictProvider {

    /**
     * 根据字典类型获取字典项列表
     *
     * @param dictType 字典类型标识
     * @return 字典项列表；未找到返回空列表，不为 {@code null}
     */
    List<DictItem> getDictItems(String dictType);

    /**
     * 根据字典类型和字典值获取单个字典项
     * <p>默认实现遍历 {@link #getDictItems(String)} 结果匹配，子类可重写以提升性能。</p>
     *
     * @param dictType 字典类型标识
     * @param value   字典值
     * @return 匹配的字典项；未找到返回 {@code null}
     */
    default DictItem getDictItem(String dictType, String value) {
        if (dictType == null || value == null) {
            return null;
        }
        List<DictItem> items = getDictItems(dictType);
        for (DictItem item : items) {
            if (value.equals(item.getValue())) {
                return item;
            }
        }
        return null;
    }

    /**
     * 判断是否支持该字典类型
     * <p>默认返回 {@code true}，表示支持所有类型。子类可重写以做过滤。</p>
     *
     * @param dictType 字典类型标识
     * @return {@code true} 表示支持
     */
    default boolean supports(String dictType) {
        return true;
    }

    /**
     * 判断该提供者是否为只读（不支持写入/删除操作）
     * <p>默认返回 {@code false}。只读提供者（如基于 mapping 的静态字典）应重写返回 {@code true}。</p>
     *
     * @return {@code true} 表示只读，写入/删除方法将抛出 {@link UnsupportedOperationException}
     */
    default boolean isReadOnly() {
        return false;
    }

    // ==================== 写入操作 ====================

    /**
     * 新增单个字典项
     *
     * @param dictType 字典类型标识
     * @param item     字典项，不能为 {@code null}
     * @throws UnsupportedOperationException 当提供者为只读时抛出
     */
    default void addDictItem(String dictType, DictItem item) {
        throw new UnsupportedOperationException("当前 DictProvider 不支持新增字典项，dictType=" + dictType);
    }

    /**
     * 批量新增字典项
     * <p>默认实现循环调用 {@link #addDictItem(String, DictItem)}，子类可重写以优化批量写入性能。</p>
     *
     * @param dictType 字典类型标识
     * @param items    字典项列表，不能为 {@code null}
     * @throws UnsupportedOperationException 当提供者为只读时抛出
     */
    default void addDictItems(String dictType, List<DictItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (DictItem item : items) {
            addDictItem(dictType, item);
        }
    }

    /**
     * 更新字典项
     * <p>根据字典项的 {@code value} 定位并更新，若不存在则新增。</p>
     *
     * @param dictType 字典类型标识
     * @param item     字典项，不能为 {@code null}
     * @throws UnsupportedOperationException 当提供者为只读时抛出
     */
    default void updateDictItem(String dictType, DictItem item) {
        throw new UnsupportedOperationException("当前 DictProvider 不支持更新字典项，dictType=" + dictType);
    }

    // ==================== 删除操作 ====================

    /**
     * 根据字典值删除单个字典项
     *
     * @param dictType 字典类型标识
     * @param value    字典值，不能为 {@code null}
     * @return {@code true} 表示删除成功；{@code false} 表示不存在或未删除
     * @throws UnsupportedOperationException 当提供者为只读时抛出
     */
    default boolean removeDictItem(String dictType, String value) {
        throw new UnsupportedOperationException("当前 DictProvider 不支持删除字典项，dictType=" + dictType);
    }

    /**
     * 删除指定字典类型的全部字典项
     *
     * @param dictType 字典类型标识
     * @return 实际删除的字典项数量
     * @throws UnsupportedOperationException 当提供者为只读时抛出
     */
    default int removeDictType(String dictType) {
        throw new UnsupportedOperationException("当前 DictProvider 不支持删除字典类型，dictType=" + dictType);
    }

    // ==================== 缓存刷新 ====================

    /**
     * 刷新指定字典类型的缓存
     * <p>默认实现为空操作（no-op），使用缓存的实现应重写此方法以清除/重新加载缓存。</p>
     *
     * @param dictType 字典类型标识
     */
    default void refreshDict(String dictType) {
        // 默认无缓存，无需刷新
    }

    /**
     * 刷新全部字典缓存
     * <p>默认实现为空操作（no-op），使用缓存的实现应重写此方法。</p>
     */
    default void refreshAll() {
        // 默认无缓存，无需刷新
    }
}
