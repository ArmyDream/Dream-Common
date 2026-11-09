package com.dream.common.data.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dream.common.data.excel.ImportDataProcessor;
import com.dream.common.data.service.CommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 导入数据处理器适配器
 * <p>
 * 将 {@link CommonService} 适配到 {@link ImportDataProcessor} 接口，
 * 为数据导入场景提供存在性判断、批量增删改、数据校验与前后处理的默认实现。
 * </p>
 * <p>
 * 存在性判断优先级：唯一键 {@code uniqueKey} → 复合键 {@code compositeKeys}（逗号分隔）→ 默认 {@code id} 字段。
 * </p>
 *
 * @param <T> 实体类型
 * @author dream
 */
@Slf4j
public class ImportDataProcessorAdapter<T> implements ImportDataProcessor<T> {

    private final CommonService<T> service;
    private final String uniqueKey;
    private final String compositeKeys;

    /**
     * 构造适配器
     *
     * @param service       底层业务服务
     * @param uniqueKey     唯一键字段名，可为 {@code null}
     * @param compositeKeys 复合键字段名（逗号分隔），可为 {@code null}
     */
    public ImportDataProcessorAdapter(CommonService<T> service, String uniqueKey, String compositeKeys) {
        this.service = service;
        this.uniqueKey = uniqueKey;
        this.compositeKeys = compositeKeys;
    }

    /**
     * 构造适配器（无唯一键与复合键，存在性判断降级为 ID 字段）
     *
     * @param service 底层业务服务
     */
    public ImportDataProcessorAdapter(CommonService<T> service) {
        this(service, null, null);
    }

    /**
     * 判断数据是否已存在
     * <p>
     * 判断策略按优先级依次尝试：
     * <ol>
     *   <li>唯一键：当 {@code uniqueKey} 非空时，按该字段值查询是否存在；</li>
     *   <li>复合键：当 {@code compositeKeys} 非空时，按各字段值组合查询，任一字段为空则判定不存在；</li>
     *   <li>默认：按 {@code id} 字段查询，ID 为空或查询不到则视为新增。</li>
     * </ol>
     * </p>
     *
     * @param data 待判断的数据
     * @return true 表示已存在
     */
    @Override
    public boolean exists(T data) {
        // 优先使用唯一键判断
        if (uniqueKey != null && !uniqueKey.isEmpty()) {
            try {
                Field field = ReflectionUtils.findField(data.getClass(), uniqueKey);
                if (field != null) {
                    field.setAccessible(true);
                    Object value = field.get(data);
                    if (value != null) {
                        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq(uniqueKey, value);
                        return service.count(queryWrapper) > 0;
                    }
                }
            } catch (Exception e) {
                log.error("获取唯一键值失败", e);
            }
        }

        // 使用复合键判断
        if (compositeKeys != null && !compositeKeys.isEmpty()) {
            String[] keys = compositeKeys.split(",");
            QueryWrapper<T> queryWrapper = new QueryWrapper<>();
            boolean allNotNull = true;

            for (String key : keys) {
                try {
                    String trimmedKey = key.trim();
                    Field field = ReflectionUtils.findField(data.getClass(), trimmedKey);
                    if (field != null) {
                        field.setAccessible(true);
                        Object value = field.get(data);
                        if (value != null) {
                            queryWrapper.eq(trimmedKey, value);
                        } else {
                            allNotNull = false;
                            break;
                        }
                    } else {
                        allNotNull = false;
                        break;
                    }
                } catch (Exception e) {
                    log.error("获取复合键值失败", e);
                    allNotNull = false;
                    break;
                }
            }

            if (allNotNull) {
                return service.count(queryWrapper) > 0;
            }
        }

        // 默认使用ID判断
        try {
            Field idField = ReflectionUtils.findField(data.getClass(), "id");
            if (idField != null) {
                idField.setAccessible(true);
                // 主键不一定是 String（Long/Integer 等），强转 String 会抛 CCE 并被吞成「不存在」导致重复插入
                Object id = idField.get(data);
                if (id == null || (id instanceof String && ((String) id).isEmpty())) {
                    return false;
                }
                return service.getById((java.io.Serializable) id) != null;
            }
        } catch (Exception e) {
            log.warn("按ID判断数据是否已存在失败，按新增处理 | type={} | err={}", data.getClass().getName(), e.getMessage());
        }

        return false;
    }

    /**
     * 批量新增
     *
     * @param dataList 数据列表
     * @return 新增后的数据列表
     */
    @Override
    public List<T> batchInsert(List<T> dataList) {
        return service.addBatch(dataList);
    }

    /**
     * 批量修改
     *
     * @param dataList 数据列表
     * @return 修改后的数据列表
     */
    @Override
    public List<T> batchUpdate(List<T> dataList) {
        List<T> resultList = new java.util.ArrayList<>();
        for (T data : dataList) {
            resultList.add(service.updateEntity(data));
        }
        return resultList;
    }

    /**
     * 批量删除
     * <p>当 {@code conditions} 类型不匹配实体类时记录错误日志并忽略；
     * 当条件实体不包含任何查询条件字段时拒绝删除，避免空条件导致全表删除。</p>
     *
     * @param conditions 删除条件，类型应与实体类一致
     */
    @Override
    public void batchDelete(Object conditions) {
        if (conditions == null) {
            log.error("批量删除条件不能为 null，已忽略");
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            T condition = (T) conditions;
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<T> wrapper =
                    service.buildQueryWrapper(condition);
            // 空条件防护：无任何查询条件时拒绝删除，避免全表删除
            if (wrapper.isEmptyOfWhere()) {
                log.error("批量删除条件不包含任何查询字段，已拒绝执行以避免全表删除: {}",
                        condition.getClass().getName());
                throw new IllegalArgumentException("删除条件不包含任何查询字段，拒绝执行批量删除");
            }
            service.remove(wrapper);
        } catch (ClassCastException e) {
            log.error("删除条件类型转换失败", e);
        }
    }

    /**
     * 数据校验
     * <p>默认实现返回 {@code null}（即校验通过），子类可重写以实现自定义校验逻辑。</p>
     *
     * @param data   待校验数据
     * @param rowNum 行号（从 1 开始）
     * @return 校验错误消息，{@code null} 表示通过
     */
    @Override
    public String validate(T data, int rowNum) {
        // 默认返回null，子类可重写
        return null;
    }

    /**
     * 导入前预处理
     * <p>默认实现原样返回数据，子类可重写以实现自定义预处理逻辑。</p>
     *
     * @param data 原始数据
     * @return 处理后的数据
     */
    @Override
    public T preProcess(T data) {
        return data;
    }

    /**
     * 导入后后处理
     * <p>默认实现原样返回数据，子类可重写以实现自定义后处理逻辑。</p>
     *
     * @param data 导入后的数据
     * @return 处理后的数据
     */
    @Override
    public T postProcess(T data) {
        return data;
    }
}
