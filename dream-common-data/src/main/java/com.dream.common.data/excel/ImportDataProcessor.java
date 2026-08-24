package com.dream.common.data.excel;

import java.util.List;

/**
 * 导入数据处理器接口
 * <p>
 * 定义数据导入时的处理契约，由具体业务实现适配。
 * 支持存在性判断、批量增删改、数据校验与前后处理。
 * </p>
 *
 * @param <T> 实体类型
 * @author dream
 * @since 2026-08-21
 */
public interface ImportDataProcessor<T> {

    /**
     * 判断数据是否已存在
     *
     * @param data 待判断的数据
     * @return true 表示已存在
     */
    boolean exists(T data);

    /**
     * 批量新增
     *
     * @param dataList 数据列表
     * @return 新增后的数据列表
     */
    List<T> batchInsert(List<T> dataList);

    /**
     * 批量修改
     *
     * @param dataList 数据列表
     * @return 修改后的数据列表
     */
    List<T> batchUpdate(List<T> dataList);

    /**
     * 批量删除
     *
     * @param conditions 删除条件
     */
    void batchDelete(Object conditions);

    /**
     * 数据校验
     *
     * @param data   待校验数据
     * @param rowNum 行号（从 1 开始）
     * @return 校验错误消息，null 表示通过
     */
    String validate(T data, int rowNum);

    /**
     * 导入前预处理
     *
     * @param data 原始数据
     * @return 处理后的数据
     */
    T preProcess(T data);

    /**
     * 导入后后处理
     *
     * @param data 导入后的数据
     * @return 处理后的数据
     */
    T postProcess(T data);
}
