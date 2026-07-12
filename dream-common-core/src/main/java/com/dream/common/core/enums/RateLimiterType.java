package com.dream.common.core.enums;

/**
 * 限流类型枚举
 *
 * @author dream
 */
public enum RateLimiterType implements BaseEnum<String> {

    /**
     * 计数器限流
     */
    COUNT("count", "计数器"),

    /**
     * 滑动窗口限流
     */
    SLIDING_WINDOW("slidingWindow", "滑动窗口"),

    /**
     * 漏桶限流
     */
    LEAKY_BUCKET("leakyBucket", "漏桶"),

    /**
     * 令牌桶限流
     */
    TOKEN_BUCKET("tokenBucket", "令牌桶"),

    /**
     * 并发限流
     */
    CONCURRENT("concurrent", "并发限流"),

    /**
     * 默认限流
     */
    DEFAULT("default", "默认限流");

    private final String value;
    private final String label;

    RateLimiterType(String value, String label) {
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

    @Override
    public String getColor() {
        switch (this) {
            case COUNT:
                return "#1890FF";
            case SLIDING_WINDOW:
                return "#52C41A";
            case LEAKY_BUCKET:
                return "#FAAD14";
            case TOKEN_BUCKET:
                return "#722ED1";
            case CONCURRENT:
                return "#FF7A45";
            default:
                return "#8C8C8C";
        }
    }

    public static RateLimiterType fromValue(String value) {
        return BaseEnum.fromValue(RateLimiterType.class, value);
    }
}