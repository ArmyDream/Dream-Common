package com.dream.common.core.enums;

/**
 * 逻辑运算符枚举
 * <p>
 * 用于查询条件拼接、规则引擎等场景
 *
 * @author dream
 */
public enum LogicalOperator implements BaseEnum<String> {

    /**
     * 与（所有条件都满足）
     */
    AND("and", "AND", "与", "&&"),

    /**
     * 或（任一条件满足）
     */
    OR("or", "OR", "或", "||"),

    /**
     * 非（条件不满足）
     */
    NOT("not", "NOT", "非", "!"),

    /**
     * 异或
     */
    XOR("xor", "XOR", "异或", "^");

    private final String value;
    private final String labelEn;
    private final String labelZh;
    private final String symbol;

    LogicalOperator(String value, String labelEn, String labelZh, String symbol) {
        this.value = value;
        this.labelEn = labelEn;
        this.labelZh = labelZh;
        this.symbol = symbol;
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
     * 获取运算符符号
     *
     * @return 运算符符号（如 &&、||、!、^）
     */
    public String getSymbol() {
        return symbol;
    }

    public String getLabel(String lang) {
        if ("en".equalsIgnoreCase(lang)) {
            return labelEn;
        }
        return labelZh;
    }

    @Override
    public String getColor() {
        switch (this) {
            case AND:
                return "#52C41A";
            case OR:
                return "#1890FF";
            case NOT:
                return "#FF4D4F";
            case XOR:
                return "#722ED1";
            default:
                return "#8C8C8C";
        }
    }

}
