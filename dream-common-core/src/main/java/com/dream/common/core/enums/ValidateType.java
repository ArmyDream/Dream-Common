package com.dream.common.core.enums;

/**
 * 校验类型枚举
 * <p>
 * 与 {@link Regex} 关联，每种校验类型对应一个正则表达式枚举
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 获取对应的正则枚举
 * RegexEnum regex = ValidateTypeEnum.MOBILE.getRegexEnum();
 *
 * // 校验
 * boolean valid = ValidateTypeEnum.MOBILE.validate("13800138000");
 *
 * // 前端下拉框
 * List<BaseEnum.EnumOption<String>> options = BaseEnum.options(ValidateTypeEnum.class);
 * }</pre>
 *
 */
public enum ValidateType implements BaseEnum<String> {

    // ==================== 联系方式 ====================

    MOBILE("mobile", "手机号", "primary", Regex.MOBILE),
    PHONE("phone", "固定电话", "primary", Regex.PHONE),
    EMAIL("email", "邮箱", "success", Regex.EMAIL),

    // ==================== 证件 ====================

    ID_CARD("idCard", "身份证", "warning", Regex.ID_CARD),
    USCC("uscc", "统一社会信用代码", "warning", Regex.USCC),

    // ==================== 网络 ====================

    URL("url", "URL", "purple", Regex.URL),
    IP("ip", "IP地址", "purple", Regex.IPV4),

    // ==================== 数字/金额 ====================

    NUMBER("number", "数字", "cyan", Regex.NUMBER),
    AMOUNT("amount", "金额", "cyan", Regex.AMOUNT),

    // ==================== 日期/时间 ====================

    DATE("date", "日期", "orange", Regex.DATE),
    DATETIME("datetime", "日期时间", "orange", Regex.DATETIME),

    // ==================== 密码/账号 ====================

    PASSWORD("password", "密码", "danger", Regex.PASSWORD),
    PASSWORD_STRONG("passwordStrong", "强密码", "danger", Regex.PASSWORD_STRONG),
    USERNAME("username", "用户名", "info", Regex.USERNAME),

    // ==================== 其他 ====================

    POSTAL_CODE("postalCode", "邮政编码", "info", Regex.POSTAL_CODE),
    QQ("qq", "QQ号", "info", Regex.QQ),
    WECHAT("wechat", "微信号", "info", Regex.WECHAT),
    CAR_NUMBER("carNumber", "车牌号", "orange", Regex.CAR_NUMBER),
    BANK_CARD("bankCard", "银行卡号", "default", Regex.BANK_CARD),
    ;

    private final String value;
    private final String label;
    private final String color;
    private final Regex regexEnum;

    ValidateType(String value, String label, String color, Regex regexEnum) {
        this.value = value;
        this.label = label;
        this.color = color;
        this.regexEnum = regexEnum;
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
     * 获取关联的正则枚举
     *
     * @return 正则枚举
     */
    public Regex getRegexEnum() {
        return regexEnum;
    }

    /**
     * 获取正则表达式
     *
     * @return 正则表达式字符串
     */
    public String getRegex() {
        return regexEnum != null ? regexEnum.getRegex() : null;
    }

    /**
     * 校验字符串是否符合当前校验类型的规则
     *
     * @param input 待校验字符串
     * @return 匹配返回 true
     */
    public boolean validate(String input) {
        if (input == null || input.trim().isEmpty() || regexEnum == null) {
            return false;
        }
        return input.matches(regexEnum.getRegex());
    }

    /**
     * 根据 RegexEnum 查找对应的 ValidateTypeEnum
     *
     * @param regexEnum 正则枚举
     * @return 对应的校验类型枚举，未找到返回 null
     */
    public static ValidateType fromRegexEnum(Regex regexEnum) {
        if (regexEnum == null) {
            return null;
        }
        for (ValidateType type : values()) {
            if (type.regexEnum == regexEnum) {
                return type;
            }
        }
        return null;
    }

    public static ValidateType fromValue(String value) {
        return BaseEnum.fromValue(ValidateType.class, value);
    }

}
