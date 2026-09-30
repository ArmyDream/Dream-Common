package com.dream.common.core.enums;

import com.dream.common.core.utils.ValidateUtils;

import java.util.regex.Pattern;

/**
 * 正则表达式枚举
 * <p>
 * 统一管理项目中常用的正则表达式，并在枚举加载时预编译 {@link Pattern}
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 获取正则表达式
 * String mobileRegex = Regex.MOBILE.getRegex();
 *
 * // 校验手机号（推荐：复用预编译 Pattern，避免重复编译）
 * boolean valid = ValidateUtils.isMobile("13800138000");
 * boolean valid2 = Regex.MOBILE.getPattern().matcher("13800138000").matches();
 *
 * // 获取所有正则枚举
 * List<BaseEnum.EnumOption<String>> options = BaseEnum.options(Regex.class);
 * }</pre>
 *
 * @author dream
 */
@SuppressWarnings({"unused", "WeakerAccess"})
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
    CREDIT("统一社会信用代码", "^[0-9A-HJ-NPQRTUWXY]{2}\\d{6}[0-9A-HJ-NPQRTUWXY]{10}$",
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
     * 用户名（4-16 位字母、数字、下划线）
     */
    USERNAME("用户名", "^[a-zA-Z0-9_]{4,16}$",
            "4-16位字母、数字、下划线"),

    /**
     * 中文
     */
    CHINESE("中文", "^[\\u4e00-\\u9fa5]+$", "中文");

    private final String label;
    private final String regex;
    private final String description;

    /**
     * 预编译的正则，避免每次校验都重新编译
     */
    private final Pattern pattern;

    Regex(String label, String regex, String description) {
        this.label = label;
        this.regex = regex;
        this.description = description;
        this.pattern = Pattern.compile(regex);
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
     * 获取预编译的正则对象。
     * <p>
     * 枚举加载时一次性编译，校验高频场景请直接用它，
     * 避免 {@code String.matches}/{@code Pattern.compile} 每次调用重复编译。
     * </p>
     *
     * @return 已编译的 {@link Pattern}
     */
    public Pattern getPattern() {
        return pattern;
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
            case CREDIT:
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
            case USERNAME:
                return "#00A3E0";
            case BANK_CARD:
            default:
                return "#8C8C8C";
        }
    }

}