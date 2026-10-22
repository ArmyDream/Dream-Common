package com.dream.common.data.api;

import com.dream.common.data.entity.DataChangeRecordEntity;

import java.util.List;

/**
 * 数据变更记录处理接口（SPI）。
 * <p>
 * {@code CommonServiceImpl} 在实体新增 / 修改 / 删除时，对比标注了
 * {@link com.dream.common.data.annotation.DataChangeRecord} 注解的实体类字段前后值，
 * 汇总为本接口的批量记录后统一分发：
 * </p>
 * <ul>
 *   <li>容器中存在本接口 Bean → 由业务实现接管（典型用法：补全 tenantId / 操作人后异步落库）</li>
 *   <li>不存在 → 框架仅打印一条 INFO 日志，行为与 {@code OperationLogHandler} 的默认策略一致</li>
 * </ul>
 * <p>
 * 实现需自行保证不抛出异常（异常会被框架捕获并记录，不影响业务调用），
 * 需要异步落库时在实现内部提交线程池，注意在调用线程内提前捕获租户 / 操作人等 ThreadLocal 上下文。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Component
 * public class DbDataChangeRecordHandler implements DataChangeRecordHandler {
 *     @Override
 *     public void handle(List<DataChangeRecordEntity> records) {
 *         // 调用线程内同步捕获上下文（异步线程取不到 ThreadLocal / Sa-Token）
 *         records.forEach(r -> r.setTenantId(TenantContextHolder.get()));
 *         ThreadPoolUtils.execute(() -> dataChangeRecordService.saveBatch(records));
 *     }
 * }
 * }</pre>
 *
 * @author dream
 * @see DataChangeRecordEntity
 * @see com.dream.common.data.annotation.DataChangeRecord
 */
public interface DataChangeRecordHandler {

    /**
     * 处理一批数据变更记录。
     * <p>
     * 同一次业务操作产生的全部字段级记录一次性传入（INSERT/UPDATE/DELETE），
     * 实现方可按需批量落库或上报。
     * </p>
     *
     * @param records 字段级变更记录列表，不会为 {@code null}，可能为空
     */
    void handle(List<DataChangeRecordEntity> records);
}
