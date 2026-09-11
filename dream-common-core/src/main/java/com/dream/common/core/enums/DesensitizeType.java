package com.dream.common.core.enums;

/**
 * 数据脱敏类型枚举
 * <p>
 * 用于敏感数据展示时的脱敏处理
 *
 * @author dream
 */
public enum DesensitizeType implements BaseEnum<String> {

    /**
     * 手机号：保留前3后4，中间隐藏
     */
    PHONE("phone", "手机号", 3, 4),

    /**
     * 固定电话：保留前3后3
     */
    TELEPHONE("telephone", "固定电话", 3, 3),

    /**
     * 邮箱：保留前2位和后3位
     */
    EMAIL("email", "邮箱", 2, 3),

    /**
     * 身份证号：保留前6后4
     */
    ID_CARD("idCard", "身份证号", 6, 4),

    /**
     * 银行卡号：保留前6后4
     */
    BANK_CARD("bankCard", "银行卡号", 6, 4),

    /**
     * 姓名：保留姓氏，名字隐藏
     */
    REAL_NAME("realName", "姓名", 1, 0),

    /**
     * 地址：保留前6个字符
     */
    ADDRESS("address", "地址", 6, 0),

    /**
     * 密码：全部隐藏
     */
    PASSWORD("password", "密码", 0, 0),

    /**
     * 车牌号：保留前2后2
     */
    CAR_NUMBER("carNumber", "车牌号", 2, 2),

    /**
     * 营业执照号：保留前2后2
     */
    BUSINESS_LICENSE_NUMBER("businessLicenseNumber", "营业执照号", 2, 2),

    /**
     * 组织机构代码：保留前2后2
     */
    ORGANIZATION_CODE("organizationCode", "组织机构代码", 2, 2),

    /**
     * 统一社会信用代码：保留前2后2
     */
    SOCIAL_CREDIT_CODE("socialCreditCode", "统一社会信用代码", 2, 2),

    /**
     * 自定义脱敏
     */
    CUSTOM("custom", "自定义", 0, 0);

    private final String value;
    private final String label;
    private final int prefixLen;
    private final int suffixLen;

    DesensitizeType(String value, String label, int prefixLen, int suffixLen) {
        this.value = value;
        this.label = label;
        this.prefixLen = prefixLen;
        this.suffixLen = suffixLen;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public int getPrefixLen() {
        return prefixLen;
    }

    public int getSuffixLen() {
        return suffixLen;
    }

    /**
     * 获取脱敏掩码字符
     */
    public char getMaskChar() {
        return '*';
    }

    @Override
    public String getColor() {
        switch (this) {
            case PHONE:
            case TELEPHONE:
                return "#1890FF";
            case EMAIL:
                return "#52C41A";
            case ID_CARD:
            case BANK_CARD:
                return "#FAAD14";
            case REAL_NAME:
            case ADDRESS:
                return "#FF7A45";
            case PASSWORD:
                return "#FF4D4F";
            default:
                return "#8C8C8C";
        }
    }

}