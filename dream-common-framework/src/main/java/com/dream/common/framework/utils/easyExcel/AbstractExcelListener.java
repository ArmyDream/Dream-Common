package com.dream.common.framework.utils.easyExcel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.dream.common.tool.utils.ExcelValidUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Excel 批量导入监听器抽象基类
 * <p>
 * 提供批量处理、数据校验、错误记录、停止阈值、前后置钩子等能力，适合大文件导入。
 * 子类需实现 {@link #batchInsert} 完成批量入库，可选重写以下钩子：
 * <ul>
 *   <li>{@link #beforeInvoke} —— 每行数据校验前调用（可用于数据预处理）</li>
 *   <li>{@link #afterBatchInsert} —— 每批入库完成后调用（可用于清理临时状态）</li>
 *   <li>{@link #customValidate} —— 自定义校验逻辑</li>
 *   <li>{@link #onAllAnalysed} —— 全部解析完成后的收尾（在最终入库之后）</li>
 * </ul>
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * AbstractExcelListener<UserDTO> listener = new AbstractExcelListener<UserDTO>(500, 1000) {
 *     protected void batchInsert(List<UserDTO> list, ExcelMessage message) {
 *         userService.saveBatch(list);
 *     }
 *     protected String customValidate(UserDTO entity, AnalysisContext ctx, int row) {
 *         return entity.getPhone() == null ? "手机号不能为空" : null;
 *     }
 * };
 * EasyExcelUtils.readWithAbstractListener(inputStream, UserDTO.class, listener, 0, 1);
 * ExcelMessage msg = listener.getExcelMessage();
 * }</pre>
 *
 * @param <T> 数据类型
 * @author dream
 */
@Slf4j
public abstract class AbstractExcelListener<T> implements ReadListener<T> {

    /**
     * 默认批量入库阈值
     */
    private static final int DEFAULT_BATCH_COUNT = 200;

    /**
     * 默认错误停止阈值（0 表示不限制）
     */
    private static final int DEFAULT_MAX_ERROR_COUNT = 0;

    /**
     * 批量入库大小（可通过构造器自定义）
     */
    protected final int batchCount;

    /**
     * 最大错误数阈值：达到此阈值后停止处理（0 表示不限制）
     */
    protected final int maxErrorCount;

    /**
     * 缓存数据列表
     */
    protected List<T> cachedDataList;

    /**
     * 导入消息记录
     */
    protected ExcelMessage excelMessage;

    /**
     * 有效数据行计数器（含校验失败行，即非标题的数据行总数）
     */
    protected final AtomicInteger rowCount = new AtomicInteger(0);

    /**
     * 是否已触发停止（错误超阈值）
     */
    protected volatile boolean stopped = false;

    /**
     * 默认构造：批量阈值 200，不限制错误数
     */
    public AbstractExcelListener() {
        this(DEFAULT_BATCH_COUNT, DEFAULT_MAX_ERROR_COUNT);
    }

    /**
     * 自定义批量阈值构造
     *
     * @param batchCount 批量入库阈值
     */
    public AbstractExcelListener(int batchCount) {
        this(batchCount, DEFAULT_MAX_ERROR_COUNT);
    }

    /**
     * 自定义批量阈值 + 错误停止阈值构造
     *
     * @param batchCount    批量入库阈值（&gt;0）
     * @param maxErrorCount 最大错误数阈值（0 表示不限制）
     */
    public AbstractExcelListener(int batchCount, int maxErrorCount) {
        this.batchCount = batchCount > 0 ? batchCount : DEFAULT_BATCH_COUNT;
        this.maxErrorCount = maxErrorCount;
        this.cachedDataList = ListUtils.newArrayListWithExpectedSize(this.batchCount);
        this.excelMessage = new ExcelMessage();
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) {
        if (exception instanceof ExcelDataConvertException) {
            ExcelDataConvertException e = (ExcelDataConvertException) exception;
            int rowIndex = e.getRowIndex() + 1;
            int colIndex = e.getColumnIndex() + 1;
            log.error("第{}行，第{}列解析异常，数据：{}", rowIndex, colIndex, e.getCellData());
            excelMessage.addRowError(rowIndex, colIndex, "数据类型错误");
        } else {
            log.error("Excel 读取异常", exception);
            excelMessage.addException(exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
        checkStopThreshold();
    }

    @Override
    public void invoke(T entity, AnalysisContext context) {
        if (stopped) {
            return;
        }
        rowCount.incrementAndGet();
        int rowIndex = context.readRowHolder().getRowIndex() + 1;

        // 0. 前置钩子
        try {
            beforeInvoke(entity, context, rowIndex);
        } catch (Exception ex) {
            log.warn("beforeInvoke 异常 | row={} | err={}", rowIndex, ex.getMessage());
            excelMessage.addException("beforeInvoke: " + ex.getMessage());
        }

        // 1. 自定义校验
        String customError = customValidate(entity, context, rowIndex);
        if (customError != null && !customError.isEmpty()) {
            excelMessage.addRowError(rowIndex, customError);
            checkStopThreshold();
            return;
        }

        // 2. 注解校验
        List<String> errors = ExcelValidUtils.validateAll(entity);
        if (!errors.isEmpty()) {
            excelMessage.addRowError(rowIndex, String.join("；", errors));
            checkStopThreshold();
            return;
        }

        // 3. 加入缓存
        cachedDataList.add(entity);

        // 4. 达到批量阈值
        if (cachedDataList.size() >= batchCount) {
            doBatchInsert();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        if (stopped) {
            log.warn("已触发错误停止阈值，剩余数据不再入库 | total={} | failed={}",
                    excelMessage.getTotal(), excelMessage.getFailedCount());
            return;
        }
        try {
            if (!cachedDataList.isEmpty()) {
                doBatchInsert();
            }
            excelMessage.setTotalAndRecalculate(rowCount.get());
            onAllAnalysed(context, excelMessage);
        } catch (Exception e) {
            log.error("最终入库失败", e);
            excelMessage.addException("最终入库失败: " + e.getMessage());
            throw new RuntimeException("Excel 最终入库失败：" + e.getMessage(), e);
        } finally {
            cachedDataList = ListUtils.newArrayListWithExpectedSize(batchCount);
        }
        log.info("Excel 导入完成，总计 {} 行，成功 {} 行，失败 {} 行，成功率 {}%",
                excelMessage.getTotal(), excelMessage.getSuccessCount(),
                excelMessage.getFailedCount(), excelMessage.getSuccessRate());
    }

    /**
     * 执行批量入库并触发后置钩子
     */
    private void doBatchInsert() {
        try {
            batchInsert(cachedDataList, excelMessage);
            afterBatchInsert(cachedDataList, excelMessage);
        } catch (Exception e) {
            log.error("批量入库失败 | batchSize={} | err={}", cachedDataList.size(), e.getMessage());
            excelMessage.addException("批量入库失败: " + e.getMessage());
            throw new RuntimeException("批量入库失败：" + e.getMessage(), e);
        } finally {
            cachedDataList = ListUtils.newArrayListWithExpectedSize(batchCount);
        }
    }

    /**
     * 检查是否达到错误停止阈值
     */
    private void checkStopThreshold() {
        if (maxErrorCount > 0 && excelMessage.getFailedCount() >= maxErrorCount) {
            stopped = true;
            log.warn("达到错误停止阈值 {}，停止处理后续行", maxErrorCount);
        }
    }

    // ==================== 抽象方法与钩子 ====================

    /**
     * 批量入库（子类必须实现）
     *
     * @param list    数据列表
     * @param message 消息对象（每入库一条可调用 {@link ExcelMessage#addSuccess(int)}）
     */
    protected abstract void batchInsert(List<T> list, ExcelMessage message);

    /**
     * 每行数据校验前的钩子（子类可选重写）
     * <p>
     * 常用于数据预处理：去空格、默认值填充、格式转换等。
     * </p>
     *
     * @param entity   当前行数据
     * @param context  上下文
     * @param rowIndex 行号（1-based）
     */
    protected void beforeInvoke(T entity, AnalysisContext context, int rowIndex) {
        // 默认空实现
    }

    /**
     * 每批入库完成后的钩子（子类可选重写）
     * <p>
     * 常用于清理临时状态、记录批次日志等。
     * </p>
     *
     * @param list    本批数据
     * @param message 消息对象
     */
    protected void afterBatchInsert(List<T> list, ExcelMessage message) {
        // 默认空实现
    }

    /**
     * 自定义校验钩子（子类可选重写）
     *
     * @param entity   当前行数据
     * @param context  上下文
     * @param rowIndex 行号（1-based）
     * @return 错误信息，null 或空串表示通过
     */
    protected String customValidate(T entity, AnalysisContext context, int rowIndex) {
        return null;
    }

    /**
     * 全部解析完成后的收尾钩子（在最终入库之后调用）
     * <p>
     * 常用于发送通知、更新汇总状态等。
     * </p>
     *
     * @param context  上下文
     * @param message  最终消息对象
     */
    protected void onAllAnalysed(AnalysisContext context, ExcelMessage message) {
        // 默认空实现
    }

    /**
     * 获取导入结果消息
     *
     * @return ExcelMessage
     */
    public ExcelMessage getExcelMessage() {
        return excelMessage;
    }

    /**
     * 是否已触发停止
     *
     * @return true 表示因错误超阈值已停止
     */
    public boolean isStopped() {
        return stopped;
    }
}
