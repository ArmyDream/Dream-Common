package com.dream.common.core.enums;

/**
 * 优先级枚举
 * <p>
 * 对应字典类型：{@link DictType#SYS_PRIORITY}
 *
 * @author dream
 */
public enum Priority implements BaseEnum<String> {

    /**
     * 最低优先级
     */
    LOWEST("0", "最低", "#8C8C8C"),

    /**
     * 低优先级
     */
    LOW("1", "低", "#52C41A"),

    /**
     * 普通优先级
     */
    NORMAL("2", "普通", "#1890FF"),

    /**
     * 高优先级
     */
    HIGH("3", "高", "#FAAD14"),

    /**
     * 紧急
     */
    URGENT("4", "紧急", "#FF7A45"),

    /**
     * 最高优先级
     */
    HIGHEST("5", "最高", "#FF4D4F");

    private final String value;
    private final String label;
    private final String color;

    Priority(String value, String label, String color) {
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
     * 获取优先级数值（用于比较）
     *
     * @return 优先级数值
     */
    public int getLevel() {
        return Integer.parseInt(value);
    }

    /**
     * 判断是否高于指定优先级
     *
     * @param other 另一个优先级
     * @return 当前优先级更高返回 true
     */
    public boolean isHigherThan(Priority other) {
        return this.getLevel() > other.getLevel();
    }

    /**
     * 是否为紧急级别（紧急或最高）
     */
    public boolean isUrgent() {
        return this == URGENT || this == HIGHEST;
    }

}
