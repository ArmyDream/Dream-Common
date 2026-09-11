package com.dream.common.core.enums;

/**
 * 任务状态 枚举
 *
 * @author dream
 */
public enum JobStatus implements BaseEnum<String> {

    RUNNING("1", "Running", "运行中"),
    PAUSED("2", "Paused", "已暂停"),
    STOPPED("3", "Stopped", "已停止"),
    COMPLETED("4", "Completed", "已完成"),
    FAILED("5", "Failed", "执行失败"),
    ABORTED("6", "Aborted", "已终止"),
    WAITING("7", "Waiting", "等待中"),
    SCHEDULED("8", "Scheduled", "已安排"),
    EXECUTING("9", "Executing", "正在执行"),
    CANCELLED("10", "Cancelled", "已取消"),
    SUSPENDED("11", "Suspended", "已挂起"),
    RETRYING("12", "Retrying", "正在重试"),
    CANCELLING("13", "Cancelling", "正在取消"),
    RETRY_FAILED("14", "Retry Failed", "重试失败"),
    RETRY_SUCCESS("15", "Retry Success", "重试成功"),
    RETRY_CANCELLED("16", "Retry Cancelled", "重试已取消"),
    RETRY_ABORTED("17", "Retry Aborted", "重试已终止"),
    UNKNOWN("99", "Unknown", "未知"),
    ;

    private final String value;

    private final String labelEn;

    private final String labelZh;

    JobStatus(String value, String labelEn, String labelZh) {
        this.value = value;
        this.labelEn = labelEn;
        this.labelZh = labelZh;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return labelZh;
    }

    public String getLabelEn() {
        return labelEn;
    }

    public String getLabelZh() {
        return labelZh;
    }

    /**
     * 是否为运行中状态
     */
    public boolean isRunning() {
        return this == RUNNING || this == EXECUTING || this == RETRYING;
    }

    /**
     * 是否为已终止状态
     */
    public boolean isTerminated() {
        return this == STOPPED || this == COMPLETED || this == FAILED
                || this == ABORTED || this == CANCELLED
                || this == RETRY_FAILED || this == RETRY_CANCELLED || this == RETRY_ABORTED;
    }

    /**
     * 是否为等待状态
     */
    public boolean isPending() {
        return this == WAITING || this == SCHEDULED || this == PAUSED || this == SUSPENDED;
    }

    @Override
    public String getColor() {
        switch (this) {
            case RUNNING:
            case EXECUTING:
            case RETRYING:
                return "#1890FF";
            case PAUSED:
            case SUSPENDED:
                return "#FAAD14";
            case STOPPED:
            case CANCELLED:
            case CANCELLING:
                return "#8C8C8C";
            case COMPLETED:
            case RETRY_SUCCESS:
                return "#52C41A";
            case FAILED:
            case ABORTED:
            case RETRY_FAILED:
            case RETRY_ABORTED:
                return "#FF4D4F";
            case WAITING:
            case SCHEDULED:
                return "#13C2C2";
            case UNKNOWN:
                return "#8C8C8C";
            default:
                return "#8C8C8C";
        }
    }
}
