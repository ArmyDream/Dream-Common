package com.dream.common.data.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dream.common.data.entity.CommonPageQuery;
import com.dream.common.data.excel.ExportDataProvider;
import com.dream.common.data.service.CommonService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 导出数据提供者适配器
 * <p>
 * 将 {@link CommonService} 适配到 {@link ExportDataProvider} 接口，
 * 为数据导出场景提供分页查询、按 ID 查询、统计总数等数据获取能力的默认实现。
 * </p>
 *
 * @param <T> 实体类型
 * @author dream
 */
@Slf4j
public class ExportDataProviderAdapter<T> implements ExportDataProvider<T> {

    private final CommonService<T> service;
    private final Class<T> entityClass;

    /**
     * 构造适配器
     *
     * @param service     底层业务服务
     * @param entityClass 实体类类型
     */
    public ExportDataProviderAdapter(CommonService<T> service, Class<T> entityClass) {
        this.service = service;
        this.entityClass = entityClass;
    }

    /**
     * 分页查询导出数据
     * <p>
     * 根据 {@code params} 的类型智能组装查询条件：
     * <ul>
     *   <li>{@code params} 为 {@link CommonPageQuery} 时，复制其查询参数、排序字段等；</li>
     *   <li>否则尝试将 {@code params} 作为查询实体直接传入。</li>
     * </ul>
     * 类型不兼容时降级为空查询条件并记录告警日志。
     * </p>
     *
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页条数
     * @param params   查询参数，可为 {@code null}
     * @return 当前页数据列表
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<T> queryPage(int pageNum, int pageSize, Object params) {
        CommonPageQuery<T> pageQuery = new CommonPageQuery<>();
        pageQuery.setPageNum(pageNum);
        pageQuery.setPageSize(pageSize);
        pageQuery.setSearchCount(false);

        if (params != null) {
            if (params instanceof CommonPageQuery) {
                // 如果params是CommonPageQuery类型，复制其查询条件
                CommonPageQuery<T> sourceQuery = (CommonPageQuery<T>) params;
                pageQuery.setParams(sourceQuery.getParams());
                pageQuery.setOrderFields(sourceQuery.getOrderFields());
                pageQuery.setCamelToUnderscore(sourceQuery.getCamelToUnderscore());
            } else {
                // 尝试将params作为查询实体
                try {
                    pageQuery.setParams((T) params);
                } catch (ClassCastException e) {
                    log.warn("参数类型转换失败，将使用空查询条件", e);
                }
            }
        }

        return service.findPage(pageQuery).getRecords();
    }

    /**
     * 根据 ID 列表查询数据
     *
     * @param ids ID 列表
     * @return 实体列表
     */
    @Override
    public List<T> queryByIds(List<String> ids) {
        return service.listByIds(ids);
    }

    /**
     * 统计满足条件的数据总数
     * <p>
     * {@code params} 为 {@code null} 时统计全表；当 {@code params} 类型可赋值给实体类时，
     * 构造查询条件统计匹配记录数，异常时返回 {@code 0} 并记录告警日志。
     * </p>
     *
     * @param params 查询参数，可为 {@code null}
     * @return 数据总数
     */
    @Override
    public long count(Object params) {
        if (params == null) {
            return service.count();
        }

        try {
            // 尝试将params转换为查询实体
            if (entityClass.isAssignableFrom(params.getClass())) {
                @SuppressWarnings("unchecked")
                T queryEntity = (T) params;
                QueryWrapper<T> queryWrapper = service.buildQueryWrapper(queryEntity);
                return service.count(queryWrapper);
            }
        } catch (Exception e) {
            log.warn("统计记录数失败，将返回0", e);
        }

        return 0;
    }

    /**
     * 获取实体类类型
     *
     * @return 实体类 {@link Class} 对象
     */
    @Override
    public Class<T> getEntityClass() {
        return entityClass;
    }
}
