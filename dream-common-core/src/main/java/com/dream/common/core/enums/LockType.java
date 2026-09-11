package com.dream.common.core.enums;

/**
 * 分布式锁类型枚举
 * <p>
 * 配合 Redisson 使用
 *
 * @author dream
 */
public enum LockType implements BaseEnum<String> {

    /**
     * 可重入锁
     */
    REENTRANT("reentrant", "可重入锁"),

    /**
     * 公平锁
     */
    FAIR("fair", "公平锁"),

    /**
     * 读锁
     */
    READ("read", "读锁"),

    /**
     * 写锁
     */
    WRITE("write", "写锁"),

    /**
     * 信号量
     */
    SEMAPHORE("semaphore", "信号量"),

    /**
     * 闭锁（CountDownLatch）
     */
    COUNT_DOWN_LATCH("countDownLatch", "闭锁"),

    /**
     * 红锁（Redis RedLock）
     */
    RED_LOCK("redLock", "红锁"),

    /**
     * 联锁（多个锁同时锁定）
     */
    MULTI_LOCK("multiLock", "联锁");

    private final String value;
    private final String label;

    LockType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    /**
     * 是否为写锁（排他锁）
     */
    public boolean isWriteLock() {
        return this == WRITE || this == REENTRANT || this == FAIR;
    }

    /**
     * 是否为读锁（共享锁）
     */
    public boolean isReadLock() {
        return this == READ;
    }

    @Override
    public String getColor() {
        switch (this) {
            case REENTRANT:
                return "#1890FF";
            case FAIR:
                return "#52C41A";
            case READ:
                return "#13C2C2";
            case WRITE:
                return "#FF4D4F";
            case SEMAPHORE:
                return "#FAAD14";
            case COUNT_DOWN_LATCH:
                return "#722ED1";
            case RED_LOCK:
                return "#FF7A45";
            case MULTI_LOCK:
                return "#2F54EB";
            default:
                return "#8C8C8C";
        }
    }

}