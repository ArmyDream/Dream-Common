package com.dream.common.core.enums;

/**
 * 时间单位枚举
 *
 * @author dream
 */
public enum TimeUnit implements BaseEnum<String> {

    SECOND("second", "Second", "秒"),
    MINUTE("minute", "Minute", "分钟"),
    HOUR("hour", "Hour", "小时"),
    DAY("day", "Day", "天"),
    WEEK("week", "Week", "周"),
    MONTH("month", "Month", "月"),
    YEAR("year", "Year", "年"),
    DECADE("decade", "Decade", "十年"),
    CENTURY("century", "Century", "世纪"),
    MILLENNIUM("millennium", "Millennium", "千年"),
    MILLISECOND("millisecond", "Millisecond", "毫秒"),
    MICROSECOND("microsecond", "Microsecond", "微秒"),
    NANOSECOND("nanosecond", "Nanosecond", "纳秒"),
    PICOSECOND("picosecond", "Picosecond", "皮秒");

    private final String value;
    private final String labelEn;
    private final String labelZh;

    TimeUnit(String value, String labelEn, String labelZh) {
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

    public String getLabel(String lang) {
        if ("en".equalsIgnoreCase(lang)) {
            return labelEn;
        }
        return labelZh;
    }

    public static TimeUnit fromValue(String value) {
        return BaseEnum.fromValue(TimeUnit.class, value);
    }

    /**
     * 转换为毫秒数
     *
     * @param amount 数量
     * @return 毫秒数
     */
    public long toMillis(long amount) {
        switch (this) {
            case NANOSECOND: return amount / 1_000_000;
            case MICROSECOND: return amount / 1_000;
            case MILLISECOND: return amount;
            case SECOND: return amount * 1_000;
            case MINUTE: return amount * 60_000;
            case HOUR: return amount * 3_600_000;
            case DAY: return amount * 86_400_000;
            case WEEK: return amount * 604_800_000;
            case MONTH: return amount * 2_592_000_000L; // 30天
            case YEAR: return amount * 31_536_000_000L; // 365天
            case DECADE: return amount * 315_360_000_000L; // 10年
            case CENTURY: return amount * 3_153_600_000_000L; // 100年
            case MILLENNIUM: return amount * 31_536_000_000_000L; // 1000年
            case PICOSECOND: return 0; // 皮秒太小，忽略
            default: return amount * 1_000;
        }
    }

    /**
     * 转换为秒数
     *
     * @param amount 数量
     * @return 秒数
     */
    public long toSeconds(long amount) {
        return toMillis(amount) / 1_000;
    }

    @Override
    public String getColor() {
        switch (this) {
            case NANOSECOND:
            case MICROSECOND:
            case MILLISECOND:
            case PICOSECOND:
                return "#8C8C8C";
            case SECOND:
            case MINUTE:
                return "#1890FF";
            case HOUR:
            case DAY:
                return "#52C41A";
            case WEEK:
            case MONTH:
                return "#FAAD14";
            case YEAR:
            case DECADE:
            case CENTURY:
            case MILLENNIUM:
                return "#722ED1";
            default:
                return "#8C8C8C";
        }
    }
}