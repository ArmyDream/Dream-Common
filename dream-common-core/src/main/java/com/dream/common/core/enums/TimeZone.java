package com.dream.common.core.enums;

/**
 * 时区枚举
 *
 * @author dream
 */
public enum TimeZone implements BaseEnum<String> {

    /**
     * UTC+8（北京时间/新加坡/香港）
     */
    UTC_PLUS_8("GMT+8", "UTC+8", "Asia/Shanghai"),

    /**
     * UTC+9（日本/韩国）
     */
    UTC_PLUS_9("GMT+9", "UTC+9", "Asia/Tokyo"),

    /**
     * UTC+10（澳大利亚东部）
     */
    UTC_PLUS_10("GMT+10", "UTC+10", "Australia/Sydney"),

    /**
     * UTC+0（格林威治标准时间）
     */
    UTC_0("GMT+0", "UTC+0", "Europe/London"),

    /**
     * UTC-5（美国东部时间）
     */
    UTC_MINUS_5("GMT-5", "UTC-5", "America/New_York"),

    /**
     * UTC-8（美国太平洋时间）
     */
    UTC_MINUS_8("GMT-8", "UTC-8", "America/Los_Angeles"),

    /**
     * UTC+1（欧洲中部时间）
     */
    UTC_PLUS_1("GMT+1", "UTC+1", "Europe/Paris"),

    /**
     * UTC+5:30（印度）
     */
    UTC_PLUS_5_30("GMT+5:30", "UTC+5:30", "Asia/Kolkata"),

    /**
     * UTC+12（新西兰）
     */
    UTC_PLUS_12("GMT+12", "UTC+12", "Pacific/Auckland");

    private final String value;
    private final String label;
    private final String zoneId;

    TimeZone(String value, String label, String zoneId) {
        this.value = value;
        this.label = label;
        this.zoneId = zoneId;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public String getZoneId() {
        return zoneId;
    }

    /**
     * 获取时区偏移量（小时）
     */
    public int getOffsetHours() {
        switch (this) {
            case UTC_PLUS_8: return 8;
            case UTC_PLUS_9: return 9;
            case UTC_PLUS_10: return 10;
            case UTC_0: return 0;
            case UTC_MINUS_5: return -5;
            case UTC_MINUS_8: return -8;
            case UTC_PLUS_1: return 1;
            case UTC_PLUS_5_30: return 5;
            case UTC_PLUS_12: return 12;
            default: return 0;
        }
    }

    @Override
    public String getColor() {
        switch (this) {
            case UTC_PLUS_8:
                return "#FF4D4F";
            case UTC_PLUS_9:
            case UTC_PLUS_10:
                return "#FAAD14";
            case UTC_0:
                return "#1890FF";
            case UTC_MINUS_5:
            case UTC_MINUS_8:
                return "#52C41A";
            default:
                return "#8C8C8C";
        }
    }

}