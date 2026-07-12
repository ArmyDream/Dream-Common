package com.dream.common.core.enums;

/**
 * 正则表达式枚举
 * <p>
 * 统一管理项目中常用的正则表达式
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 获取正则表达式
 * String mobileRegex = RegexEnum.MOBILE.getRegex();
 *
 * // 校验手机号
 * boolean valid = "13800138000".matches(RegexEnum.MOBILE.getRegex());
 *
 * // 获取所有正则枚举
 * List<BaseEnum.EnumOption<String>> options = BaseEnum.options(RegexEnum.class);
 * }</pre>
 *
 * @author dream
 */
public enum Regex implements BaseEnum<String> {

    // ==================== 联系方式 ====================

    /**
     * 手机号（中国大陆）
     * <p>
     * 规则：1开头的11位数字，第二位为3-9
     */
    MOBILE("手机号", "^1[3-9]\\d{9}$",
            "1开头的11位数字，第二位为3-9"),

    /**
     * 固定电话
     * <p>
     * 规则：区号(3-4位)-号码(7-8位)
     */
    PHONE("固定电话", "^(\\d{3,4}-)?\\d{7,8}$",
            "区号(3-4位)-号码(7-8位)"),

    /**
     * 邮箱
     * <p>
     * 规则：用户名@域名.后缀
     */
    EMAIL("邮箱", "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            "用户名@域名.后缀"),

    // ==================== 身份证/证件 ====================

    /**
     * 身份证号（18位）
     * <p>
     * 规则：6位地区码 + 8位生日 + 3位顺序码 + 1位校验码（数字或X）
     */
    ID_CARD("身份证号", "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$",
            "18位身份证号，含X"),

    /**
     * 统一社会信用代码（18位）
     * <p>
     * 规则：2位登记管理机关 + 6位组织机构代码 + 10位其他
     */
    USCC("统一社会信用代码", "^[0-9A-HJ-NPQRTUWXY]{2}\\d{6}[0-9A-HJ-NPQRTUWXY]{10}$",
            "18位统一社会信用代码"),

    // ==================== 网络 ====================

    /**
     * URL
     * <p>
     * 规则：协议://域名/路径
     */
    URL("URL", "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$",
            "http/https/ftp 协议"),

    /**
     * IPv4 地址
     * <p>
     * 规则：4段 0-255 的数字，用点分隔
     */
    IPV4("IPv4", "^(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$",
            "192.168.1.1"),

    // ==================== 数字/金额 ====================

    /**
     * 纯数字（正整数）
     * <p>
     * 规则：0-9组成
     */
    NUMBER("纯数字", "^[0-9]+$",
            "正整数"),

    /**
     * 金额（最多两位小数）
     * <p>
     * 规则：整数部分 + 可选的小数部分（最多2位）
     */
    AMOUNT("金额", "^[0-9]+(\\.[0-9]{1,2})?$",
            "数字，最多两位小数"),

    // ==================== 日期/时间 ====================

    /**
     * 日期（yyyy-MM-dd）
     * <p>
     * 规则：4位年-2位月-2位日
     */
    DATE("日期", "^\\d{4}-\\d{2}-\\d{2}$",
            "yyyy-MM-dd"),

    /**
     * 日期时间（yyyy-MM-dd HH:mm:ss）
     * <p>
     * 规则：日期 + 时间
     */
    DATETIME("日期时间", "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$",
            "yyyy-MM-dd HH:mm:ss"),

    // ==================== 密码 ====================

    /**
     * 密码（强密码）
     * <p>
     * 规则：8-20位，至少包含大小写字母和数字
     */
    PASSWORD("密码", "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,20}$",
            "8-20位，含大小写字母和数字"),

    /**
     * 密码（包含特殊字符）
     * <p>
     * 规则：8-20位，至少包含大小写字母、数字、特殊字符
     */
    PASSWORD_STRONG("密码(强)", "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,20}$",
            "8-20位，含大小写字母、数字、特殊字符"),

    // ==================== 其他 ====================

    /**
     * 邮政编码（6位）
     */
    POSTAL_CODE("邮政编码", "^[1-9]\\d{5}$",
            "6位数字"),

    /**
     * QQ号（5-11位）
     */
    QQ("QQ号", "^[1-9]\\d{4,10}$",
            "5-11位数字，首位不为0"),

    /**
     * 微信号（6-20位字母、数字、下划线、减号）
     */
    WECHAT("微信号", "^[a-zA-Z][-_a-zA-Z0-9]{5,19}$",
            "6-20位，字母开头"),

    /**
     * 车牌号（新能源/普通）
     * <p>
     * 规则：省份简称 + 城市代码 + 5-6位车牌号
     */
    CAR_NUMBER("车牌号", "^[\\u4e00-\\u9fa5]{1}[A-Z]{1}[A-Z0-9]{5,6}$",
            "中文省份简称 + 大写字母 + 5-6位"),

    /**
     * 银行卡号（12-19位数字）
     */
    BANK_CARD("银行卡号", "^[1-9]\\d{11,18}$",
            "12-19位数字"),

    /**
     * 用户名（4-16位字母、数字、下划线）
     */
    USERNAME("用户名", "^[a-zA-Z0-9_]{4,16}$",
            "4-16位字母、数字、下划线");

    private final String label;
    private final String regex;
    private final String description;

    Regex(String label, String regex, String description) {
        this.label = label;
        this.regex = regex;
        this.description = description;
    }

    @Override
    public String getValue() {
        return regex;
    }

    @Override
    public String getLabel() {
        return label;
    }

    /**
     * 获取正则表达式
     */
    public String getRegex() {
        return regex;
    }

    /**
     * 获取描述说明
     */
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getColor() {
        switch (this) {
            case MOBILE:
            case PHONE:
                return "#1890FF";
            case EMAIL:
                return "#52C41A";
            case ID_CARD:
            case USCC:
                return "#FAAD14";
            case URL:
            case IPV4:
                return "#722ED1";
            case NUMBER:
            case AMOUNT:
                return "#13C2C2";
            case DATE:
            case DATETIME:
                return "#FF7A45";
            case PASSWORD:
            case PASSWORD_STRONG:
                return "#FF4D4F";
            case POSTAL_CODE:
            case QQ:
            case WECHAT:
                return "#2F54EB";
            case CAR_NUMBER:
                return "#FF6B00";
            case BANK_CARD:
                return "#8C8C8C";
            case USERNAME:
                return "#00A3E0";
            default:
                return "#8C8C8C";
        }
    }

    // ==================== 静态方法 ====================

    /**
     * 根据正则表达式查找枚举
     *
     * @param regex 正则表达式
     * @return 匹配的枚举，未找到返回 null
     */
    public static Regex fromRegex(String regex) {
        if (regex == null || regex.isEmpty()) {
            return null;
        }
        for (Regex item : values()) {
            if (item.regex.equals(regex)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 校验字符串是否匹配指定枚举的正则
     *
     * @param input     待校验字符串
     * @param regexEnum 正则枚举
     * @return 匹配返回 true
     */
    public static boolean matches(String input, Regex regexEnum) {
        if (input == null || regexEnum == null) {
            return false;
        }
        return input.matches(regexEnum.getRegex());
    }

    /**
     * 校验字符串是否匹配指定枚举的正则
     *
     * @param input 待校验字符串
     * @param regex 正则表达式
     * @return 匹配返回 true
     */
    public static boolean matches(String input, String regex) {
        if (input == null || regex == null) {
            return false;
        }
        return input.matches(regex);
    }

    public static Regex fromValue(String value) {
        return BaseEnum.fromValue(Regex.class, value);
    }
}