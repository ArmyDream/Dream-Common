package com.dream.common.core.enums;

/**
 * 通知状态枚举
 * <p>
 * 对应字典类型：{@link DictType#SYS_NOTIFY_STATUS}
 *
 * @author dream
 */
public enum NotifyStatus implements BaseEnum<String> {

    /**
     * 待发送
     */
    PENDING("pending", "待发送", "warning"),

    /**
     * 发送中
     */
    SENDING("sending", "发送中", "primary"),

    /**
     * 发送成功
     */
    SUCCESS("success", "发送成功", "success"),

    /**
     * 发送失败
     */
    FAILED("failed", "发送失败", "danger"),

    /**
     * 已读
     */
    READ("read", "已读", "success"),

    /**
     * 未读
     */
    UNREAD("unread", "未读", "warning"),

    /**
     * 已取消
     */
    CANCELLED("cancelled", "已取消", "default");

    private final String value;
    private final String label;
    private final String color;

    NotifyStatus(String value, String label, String color) {
        this.value = value;
        this.label = label;
        this.color = color;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getColor() {
        return color;
    }

    /**
     * 是否已发送成功
     */
    public boolean isSent() {
        return this == SUCCESS || this == READ;
    }

    /**
     * 是否未读
     */
    public boolean isUnread() {
        return this == UNREAD || this == PENDING || this == SENDING;
    }

    /**
     * 是否为终态
     */
    public boolean isFinal() {
        return this == SUCCESS || this == FAILED || this == READ || this == CANCELLED;
    }

}
