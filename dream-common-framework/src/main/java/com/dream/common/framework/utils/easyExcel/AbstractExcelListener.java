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
 * 提供批量处理、数据校验、错误记录的能力，适合大文件导入。
 * 子类需实现 {@link #batchInsert} 完成批量入库。
 *
 * @param <T> 数据类型
 * @author dream
 */
@Slf4j
public abstract class AbstractExcelListener<T> implements ReadListener<T> {

    /**
     * 批量入库大小
     */
    protected static final int BATCH_COUNT = 200;

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

    public AbstractExcelListener() {
        this.cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        this.excelMessage = new ExcelMessage();
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) {
        if (exception instanceof ExcelDataConvertException) {
            ExcelDataConvertException e = (ExcelDataConvertException) exception;
            int rowIndex = e.getRowIndex() + 1;
            int colIndex = e.getColumnIndex() + 1;
            log.error("第{}行，第{}列解析异常，数据：{}", rowIndex, colIndex, e.getCellData());
            excelMessage.setRowError(rowIndex, "第[" + colIndex + "]列数据类型错误");
        } else {
            log.error("Excel 读取异常", exception);
        }
    }

    @Override
    public void invoke(T entity, AnalysisContext context) {
        rowCount.incrementAndGet();
        int rowIndex = context.readRowHolder().getRowIndex() + 1;

        // 1. 自定义校验
        String customError = customValidate(entity, context, rowIndex);
        if (customError != null && !customError.isEmpty()) {
            excelMessage.setRowError(rowIndex, customError);
            return;
        }

        // 2. 注解校验
        List<String> errors = ExcelValidUtils.validateAll(entity);
        if (!errors.isEmpty()) {
            excelMessage.setRowError(rowIndex, String.join("；", errors));
            return;
        }

        // 3. 加入缓存
        cachedDataList.add(entity);

        // 4. 达到批量阈值
        if (cachedDataList.size() >= BATCH_COUNT) {
            doBatchInsert();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        try {
            if (!cachedDataList.isEmpty()) {
                doBatchInsert();
            }
            // 使用实际读取的行数作为总数（比估算更准确）
            excelMessage.setTotal(rowCount.get());
            excelMessage.setSuccessCount(excelMessage.getTotal() - excelMessage.getFailedCount());
        } catch (Exception e) {
            log.error("最终入库失败", e);
            throw new RuntimeException("Excel 最终入库失败：" + e.getMessage(), e);
        } finally {
            cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        }
        log.info("Excel 导入完成，总计 {} 行，成功 {} 行，失败 {} 行",
                excelMessage.getTotal(), excelMessage.getSuccessCount(), excelMessage.getFailedCount());
    }

    private void doBatchInsert() {
        try {
            batchInsert(cachedDataList, excelMessage);
        } catch (Exception e) {
            log.error("批量入库失败", e);
            throw new RuntimeException("批量入库失败：" + e.getMessage(), e);
        } finally {
            cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        }
    }

    /**
     * 批量入库（子类实现）
     *
     * @param list    数据列表
     * @param message 消息对象（每入库一条调用 {@link ExcelMessage}）
     */
    protected abstract void batchInsert(List<T> list, ExcelMessage message);

    /**
     * 自定义校验钩子（子类重写）
     *
     * @param entity   当前行数据
     * @param context  上下文
     * @param rowIndex 行号（1-based）
     * @return 错误信息，null 或空串表示通过
     */
    protected String customValidate(T entity, AnalysisContext context, int rowIndex) {
        return null;
    }

    public ExcelMessage getExcelMessage() {
        return excelMessage;
    }
}