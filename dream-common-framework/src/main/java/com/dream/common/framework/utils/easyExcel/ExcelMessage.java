package com.dream.common.framework.utils.easyExcel;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导入结果消息
 * <p>
 * 用于记录 Excel 导入过程中的成功/失败数量及错误详情，支持行级错误信息记录和批量消息合并。
 * <p>
 * 使用示例：
 * <pre>{@code
 * ExcelMessage message = new ExcelMessage();
 * message.setTotal(100);
 * message.setRowError(3, "手机号不能为空");
 * if (message.hasErrors()) {
 *     throw new BusinessException(message.getMessage());
 * }
 * }</pre>
 *
 * @author dream
 */
@Data
public class ExcelMessage {

    /**
     * 错误提示信息（包含所有行的错误详情，格式：第[X]行导入失败,错误原因:[Y];）
     */
    private String errorDetail = "";

    /**
     * 成功数量
     */
    private int successCount;

    /**
     * 失败数量
     */
    private int failedCount;

    /**
     * 导入总数（有效数据行数，不含标题）
     */
    private int total;

    /**
     * 错误详情列表（每条对应一行的错误）
     */
    private List<String> errorList = new ArrayList<>();

    /**
     * 记录某行导入失败的错误信息
     * <p>
     * 自动累加失败数量，并将错误信息追加到 {@link #errorDetail} 和 {@link #errorList}。
     * <pre>
     * message.setRowError(3, "手机号不能为空")
     *   → errorDetail = "第[3]行导入失败,错误原因:[手机号不能为空];\n"
     *   → failedCount++
     * </pre>
     *
     * @param rowIndex 行号（从1开始）
     * @param errorMsg 错误原因
     */
    public void setRowError(int rowIndex, String errorMsg) {
        this.errorDetail += String.format("第[%d]行导入失败,错误原因:[%s];\n", rowIndex, errorMsg);
        this.errorList.add(String.format("第%d行：%s", rowIndex, errorMsg));
        this.failedCount++;
    }

    /**
     * 合并两个 ExcelMessage（用于批量处理时合并每批的结果）
     * <p>
     * 将 source 的错误信息、错误列表、失败数量合并到 target，并正确累加总数和成功数。
     *
     * @param target 最终消息对象（会被修改）
     * @param source 待合并的消息对象
     */
    public static void joinExcelMessage(ExcelMessage target, ExcelMessage source) {
        if (target == null || source == null) {
            return;
        }
        target.errorDetail = target.errorDetail + source.errorDetail;
        target.errorList.addAll(source.errorList);
        target.failedCount = target.failedCount + source.failedCount;
        target.total = target.total + source.total;
        target.successCount = target.total - target.failedCount;
    }

    /**
     * 是否存在错误
     *
     * @return 失败数量大于0返回 true
     */
    public boolean hasErrors() {
        return failedCount > 0;
    }
}