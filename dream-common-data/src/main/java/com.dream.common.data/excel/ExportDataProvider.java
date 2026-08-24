package com.dream.common.data.excel;

import java.util.List;

/**
 * 导出数据提供者接口
 * <p>
 * 定义数据导出时的数据获取契约，由具体业务实现适配。
 * 支持分页查询、按 ID 查询、统计总数三种数据获取方式。
 * </p>
 *
 * @param <T> 实体类型
 * @author dream
 * @since 2026-08-21
 */
public interface ExportDataProvider<T> {

    /**
     * 分页查询导出数据
     *
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页条数
     * @param params   查询参数（可为 null）
     * @return 当前页数据列表
     */
    List<T> queryPage(int pageNum, int pageSize, Object params);

    /**
     * 根据 ID 列表查询数据
     *
     * @param ids ID 列表
     * @return 实体列表
     */
    List<T> queryByIds(List<String> ids);

    /**
     * 统计满足条件的数据总数
     *
     * @param params 查询参数（可为 null，null 时统计全部）
     * @return 数据总数
     */
    long count(Object params);

    /**
     * 获取实体类类型
     *
     * @return 实体类 Class 对象
     */
    Class<T> getEntityClass();
}
