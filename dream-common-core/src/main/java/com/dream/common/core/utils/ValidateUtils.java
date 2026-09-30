package com.dream.common.core.utils;

import com.dream.common.core.enums.Regex;

import java.util.ArrayList;
import java.util.List;

/**
 * 校验工具类
 * <p>
 * 配合 {@link Regex} 对常用数据进行格式校验
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 校验手机号
 * ValidateUtils.isMobile("13800138000"); // true
 *
 * // 校验邮箱
 * ValidateUtils.isEmail("test@example.com"); // true
 *
 * // 批量校验
 * List<String> errors = ValidateUtils.validateAll("13800138000",
 *     ValidateUtils.check(RegexEnum.MOBILE, "手机号"),
 *     ValidateUtils.check(RegexEnum.EMAIL, "邮箱")
 * );
 * }</pre>
 *
 * @author dream
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class ValidateUtils {

    private ValidateUtils() {
    }

    // ==================== 联系方式 ====================

    /**
     * 校验手机号
     *
     * @param mobile 手机号
     * @return 合法返回 true
     */
    public static boolean isMobile(String mobile) {
        return matches(mobile, Regex.MOBILE);
    }

    /**
     * 校验固定电话
     *
     * @param phone 固定电话
     * @return 合法返回 true
     */
    public static boolean isPhone(String phone) {
        return matches(phone, Regex.PHONE);
    }

    /**
     * 校验邮箱
     *
     * @param email 邮箱
     * @return 合法返回 true
     */
    public static boolean isEmail(String email) {
        return matches(email, Regex.EMAIL);
    }

    // ==================== 证件 ====================

    /**
     * 校验身份证号
     *
     * @param idCard 身份证号
     * @return 合法返回 true
     */
    public static boolean isIdCard(String idCard) {
        return matches(idCard, Regex.ID_CARD);
    }

    /**
     * 校验统一社会信用代码
     *
     * @param credit 统一社会信用代码
     * @return 合法返回 true
     */
    public static boolean isCredit(String credit) {
        return matches(credit, Regex.CREDIT);
    }

    // ==================== 网络 ====================

    /**
     * 校验 URL
     *
     * @param url URL
     * @return 合法返回 true
     */
    public static boolean isUrl(String url) {
        return matches(url, Regex.URL);
    }

    /**
     * 校验 IPv4 地址
     *
     * @param ip IPv4 地址
     * @return 合法返回 true
     */
    public static boolean isIpv4(String ip) {
        return matches(ip, Regex.IPV4);
    }

    // ==================== 数字/金额 ====================

    /**
     * 校验是否为纯数字（正整数）
     *
     * @param number 数字字符串
     * @return 合法返回 true
     */
    public static boolean isNumber(String number) {
        return matches(number, Regex.NUMBER);
    }

    /**
     * 校验是否为金额（最多两位小数）
     *
     * @param amount 金额字符串
     * @return 合法返回 true
     */
    public static boolean isAmount(String amount) {
        return matches(amount, Regex.AMOUNT);
    }

    // ==================== 日期/时间 ====================

    /**
     * 校验日期格式（yyyy-MM-dd）
     *
     * @param date 日期字符串
     * @return 合法返回 true
     */
    public static boolean isDate(String date) {
        return matches(date, Regex.DATE);
    }

    /**
     * 校验日期时间格式（yyyy-MM-dd HH:mm:ss）
     *
     * @param datetime 日期时间字符串
     * @return 合法返回 true
     */
    public static boolean isDateTime(String datetime) {
        return matches(datetime, Regex.DATETIME);
    }

    // ==================== 密码/账号 ====================

    /**
     * 校验密码（8-20位，含大小写字母和数字）
     *
     * @param password 密码
     * @return 合法返回 true
     */
    public static boolean isPassword(String password) {
        return matches(password, Regex.PASSWORD);
    }

    /**
     * 校验强密码（8-20位，含大小写字母、数字、特殊字符）
     *
     * @param password 密码
     * @return 合法返回 true
     */
    public static boolean isStrongPassword(String password) {
        return matches(password, Regex.PASSWORD_STRONG);
    }

    /**
     * 校验用户名（4-16 位字母、数字、下划线）
     *
     * @param username 用户名
     * @return 合法返回 true
     */
    public static boolean isUsername(String username) {
        return matches(username, Regex.USERNAME);
    }

    /**
     * 是否中文
     */
    public static boolean isChinese(String str) {
        return matches(str, Regex.CHINESE);
    }

    // ==================== 其他 ====================

    /**
     * 校验邮政编码（6位数字）
     *
     * @param postalCode 邮政编码
     * @return 合法返回 true
     */
    public static boolean isPostalCode(String postalCode) {
        return matches(postalCode, Regex.POSTAL_CODE);
    }

    /**
     * 校验银行卡号（12-19位数字）
     *
     * @param bankCard 银行卡号
     * @return 合法返回 true
     */
    public static boolean isBankCard(String bankCard) {
        return matches(bankCard, Regex.BANK_CARD);
    }

    /**
     * 校验车牌号
     *
     * @param carNumber 车牌号
     * @return 合法返回 true
     */
    public static boolean isCarNumber(String carNumber) {
        return matches(carNumber, Regex.CAR_NUMBER);
    }

    // ==================== 通用方法 ====================

    /**
     * 使用指定正则枚举校验字符串
     *
     * @param input     待校验字符串
     * @param regexEnum 正则枚举
     * @return 匹配返回 true
     */
    public static boolean matches(String input, Regex regexEnum) {
        if (input == null || input.isEmpty() || regexEnum == null) {
            return false;
        }
        return regexEnum.getPattern().matcher(input).matches();
    }

    /**
     * 使用自定义正则校验字符串
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

    /**
     * 判断字符串是否不为空
     *
     * @param str 字符串
     * @return 非空返回 true
     */
    public static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * 判断字符串是否为空
     *
     * @param str 字符串
     * @return 空返回 true
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 判断字符串长度是否在指定范围内
     *
     * @param str    字符串
     * @param minLen 最小长度（含）
     * @param maxLen 最大长度（含）
     * @return 在范围内返回 true
     */
    public static boolean isLengthBetween(String str, int minLen, int maxLen) {
        if (str == null) {
            return false;
        }
        int len = str.length();
        return len >= minLen && len <= maxLen;
    }

    // ==================== 批量校验 ====================

    /**
     * 批量校验，收集所有不通过的错误信息
     * <p>
     * 使用示例：
     * <pre>{@code
     * List<String> errors = new ArrayList<>();
     * if (!ValidateUtils.isMobile(phone)) errors.add("手机号格式不正确");
     * if (!ValidateUtils.isEmail(email)) errors.add("邮箱格式不正确");
     * if (!errors.isEmpty()) throw new BusinessException(String.join("; ", errors));
     * }</pre>
     *
     * @param validations 校验项列表
     * @return 错误信息列表（空表示全部通过）
     */
    public static List<String> validateAll(Validation... validations) {
        List<String> errors = new ArrayList<>();
        if (validations == null) {
            return errors;
        }
        for (Validation v : validations) {
            if (!v.pass) {
                errors.add(v.message);
            }
        }
        return errors;
    }

    /**
     * 构建一个校验项
     *
     * @param pass    校验结果
     * @param message 不通过时的错误信息
     * @return 校验项
     */
    public static Validation validation(boolean pass, String message) {
        return new Validation(pass, message);
    }

    /**
     * 校验项
     */
    public static class Validation {
        private final boolean pass;
        private final String message;

        public Validation(boolean pass, String message) {
            this.pass = pass;
            this.message = message;
        }

        public boolean isPass() {
            return pass;
        }

        public String getMessage() {
            return message;
        }
    }
}
