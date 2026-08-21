package com.dream.common.framework.utils.easyExcel;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel 导入结果消息
 * <p>
 * 用于记录 Excel 导入过程中的成功/失败数量、错误详情与异常堆栈，
 * 支持行级错误信息记录、错误码、批量消息合并以及链式调用。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * ExcelMessage message = new ExcelMessage();
 * message.setTotal(100);
 *
 * // 链式调用
 * message.addRowError(3, "手机号不能为空")
 *        .addRowError(5, "邮箱格式不正确");
 *
 * if (message.hasErrors()) {
 *     throw new BusinessException(message.buildMessage());
 * }
 *
 * // 合并多个批次结果
 * ExcelMessage merged = new ExcelMessage();
 * ExcelMessage.joinExcelMessage(merged, message);
 * ExcelMessage.joinExcelMessage(merged, anotherMessage);
 * }</pre>
 *
 * @author dream
 */
@Data
public class ExcelMessage implements Serializable {

    private static final long serialVersionUID = 1L;

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
    @Getter
    @Setter
    private List<String> errorList = new ArrayList<>();

    /**
     * 异常记录列表（保存导入过程中的关键异常信息，便于排查）
     */
    @Getter
    @Setter
    private List<String> exceptionList = new ArrayList<>();

    /**
     * 扩展属性（key-value，用于业务侧传递额外信息，如批次号、文件名等）
     */
    @Getter
    @Setter
    private Map<String, Object> attributes = new LinkedHashMap<>();

    /**
     * 默认构造
     */
    public ExcelMessage() {
    }

    /**
     * 记录某行导入失败的错误信息
     * <p>
     * 自动累加失败数量，并将错误信息追加到 {@link #errorDetail} 和 {@link #errorList}。
     * </p>
     *
     * <pre>
     * message.setRowError(3, "手机号不能为空")
     *   → errorDetail = "第[3]行导入失败,错误原因:[手机号不能为空];\n"
     *   → failedCount++
     * </pre>
     *
     * @param rowIndex 行号（从 1 开始）
     * @param errorMsg 错误原因
     */
    public void setRowError(int rowIndex, String errorMsg) {
        this.errorDetail += String.format("第[%d]行导入失败,错误原因:[%s];\n", rowIndex, errorMsg);
        this.errorList.add(String.format("第%d行：%s", rowIndex, errorMsg));
        this.failedCount++;
    }

    /**
     * 链式记录某行导入失败的错误信息
     *
     * @param rowIndex 行号（从 1 开始）
     * @param errorMsg 错误原因
     * @return 当前对象（链式调用）
     */
    public ExcelMessage addRowError(int rowIndex, String errorMsg) {
        setRowError(rowIndex, errorMsg);
        return this;
    }

    /**
     * 链式记录某行导入失败的错误信息（含列号）
     *
     * @param rowIndex 行号（从 1 开始）
     * @param colIndex 列号（从 1 开始）
     * @param errorMsg 错误原因
     * @return 当前对象（链式调用）
     */
    public ExcelMessage addRowError(int rowIndex, int colIndex, String errorMsg) {
        String msg = String.format("第[%d]列%2$s", colIndex, errorMsg);
        return addRowError(rowIndex, msg);
    }

    /**
     * 链式累加成功数量
     *
     * @param count 成功数量
     * @return 当前对象（链式调用）
     */
    public ExcelMessage addSuccess(int count) {
        this.successCount += count;
        return this;
    }

    /**
     * 链式记录异常信息（不影响失败计数，仅记录用于排查的异常）
     *
     * @param exceptionMsg 异常信息
     * @return 当前对象（链式调用）
     */
    public ExcelMessage addException(String exceptionMsg) {
        if (exceptionMsg != null && !exceptionMsg.isEmpty()) {
            this.exceptionList.add(exceptionMsg);
        }
        return this;
    }

    /**
     * 链式设置扩展属性
     *
     * @param key   属性 key
     * @param value 属性 value
     * @return 当前对象（链式调用）
     */
    public ExcelMessage addAttribute(String key, Object value) {
        if (key != null) {
            this.attributes.put(key, value);
        }
        return this;
    }

    /**
     * 设置导入总数并自动重算成功数
     *
     * @param total 总数
     */
    public void setTotalAndRecalculate(int total) {
        this.total = total;
        this.successCount = total - this.failedCount;
    }

    /**
     * 合并两个 ExcelMessage（用于批量处理时合并每批的结果）
     * <p>
     * 将 source 的错误信息、错误列表、失败数量、异常列表、扩展属性合并到 target，
     * 并正确累加总数和成功数。
     * </p>
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
        target.exceptionList.addAll(source.exceptionList);
        target.attributes.putAll(source.attributes);
        target.failedCount = target.failedCount + source.failedCount;
        target.total = target.total + source.total;
        target.successCount = target.total - target.failedCount;
    }

    /**
     * 是否存在错误
     *
     * @return 失败数量大于 0 返回 true
     */
    public boolean hasErrors() {
        return failedCount > 0;
    }

    /**
     * 是否存在异常记录
     *
     * @return 异常列表非空返回 true
     */
    public boolean hasExceptions() {
        return !exceptionList.isEmpty();
    }

    /**
     * 构建汇总错误消息（包含错误详情与异常信息）
     * <p>
     * 业务侧可直接抛出：{@code throw new BusinessException(message.buildMessage());}
     * </p>
     *
     * @return 汇总消息
     */
    public String buildMessage() {
        StringBuilder sb = new StringBuilder();
        if (hasErrors()) {
            sb.append(String.format("导入失败 %d 行", failedCount));
            if (!errorList.isEmpty()) {
                sb.append("：").append(String.join("；", errorList));
            }
        }
        if (hasExceptions()) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append("异常 ").append(exceptionList.size()).append(" 条");
        }
        return sb.length() > 0 ? sb.toString() : "导入成功";
    }

    /**
     * 获取错误列表的不可变视图
     *
     * @return 不可变错误列表
     */
    public List<String> getErrorListView() {
        return Collections.unmodifiableList(errorList);
    }

    /**
     * 获取异常列表的不可变视图
     *
     * @return 不可变异常列表
     */
    public List<String> getExceptionListView() {
        return Collections.unmodifiableList(exceptionList);
    }

    /**
     * 获取扩展属性的不可变视图
     *
     * @return 不可变属性 Map
     */
    public Map<String, Object> getAttributesView() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * 清空所有错误与异常记录（保留 total）
     */
    public void clearErrors() {
        this.errorDetail = "";
        this.errorList.clear();
        this.exceptionList.clear();
        this.failedCount = 0;
        this.successCount = this.total;
    }

    /**
     * 获取导入成功率（百分比，保留 2 位小数）
     *
     * @return 成功率字符串，如 "85.50"；total=0 返回 "0"
     */
    public String getSuccessRate() {
        if (total == 0) {
            return "0";
        }
        double rate = (successCount * 100.0) / total;
        return String.format("%.2f", rate);
    }

    /**
     * 汇总摘要（用于日志或返回给前端）
     *
     * @return 摘要字符串
     */
    @Override
    public String toString() {
        return String.format("ExcelMessage{total=%d, success=%d, failed=%d, successRate=%s%%}",
                total, successCount, failedCount, getSuccessRate());
    }
}
