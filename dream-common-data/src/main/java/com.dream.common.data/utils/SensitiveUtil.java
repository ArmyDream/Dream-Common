package com.dream.common.data.utils;

import com.dream.common.data.enums.SensitiveType;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据脱敏工具类
 * <p>
 * 根据不同的敏感数据类型（手机号、身份证、银行卡、邮箱、姓名、地址等）
 * 对原始数据进行脱敏处理，将敏感部分替换为占位字符（默认 {@code *}）。
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * String masked = SensitiveUtil.desensitize("13812341234", SensitiveType.MOBILE, null, "*");
 * // 结果：138****1234
 * }</pre>
 * </p>
 *
 * @author dream
 */
@Slf4j
public class SensitiveUtil {

    /**
     * 私有构造方法，禁止实例化工具类
     */
    private SensitiveUtil() {
    }

    /**
     * 数据脱敏处理入口
     * <p>
     * 根据指定的敏感数据类型选择对应的脱敏策略。当 {@code type} 为
     * {@link SensitiveType#CUSTOM} 时，需通过 {@code rule} 指定前后保留长度。
     * 其余类型仅按规则脱敏，{@code rule} 可为 {@code null}。
     * </p>
     *
     * @param value       原始数据，{@code null} 或空串原样返回
     * @param type        脱敏类型
     * @param rule        自定义脱敏规则（格式：{@code 前缀长度-后缀长度}，如 {@code 3-4}），
     *                    仅 {@code CUSTOM} 类型生效
     * @param replaceChar 替换占位字符，{@code null} 时默认使用 {@code *}
     * @return 脱敏后的字符串
     */
    public static String desensitize(String value, SensitiveType type, String rule, String replaceChar) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        String replace = replaceChar == null ? "*" : replaceChar;

        switch (type) {
            case MOBILE:
                return desensitizeMobile(value, replace);
            case ID_CARD:
                return desensitizeIdCard(value, replace);
            case BANK_CARD:
                return desensitizeBankCard(value, replace);
            case EMAIL:
                return desensitizeEmail(value, replace);
            case NAME:
                return desensitizeName(value, replace);
            case ADDRESS:
                return desensitizeAddress(value, replace);
            case CUSTOM:
                return desensitizeCustom(value, rule, replace);
            default:
                return value;
        }
    }

    /**
     * 手机号脱敏（138****1234）
     * <p>仅对长度为 11 的手机号脱敏，其余原样返回。</p>
     *
     * @param mobile       手机号
     * @param replaceChar  替换占位字符
     * @return 脱敏后的手机号
     */
    private static String desensitizeMobile(String mobile, String replaceChar) {
        if (mobile.length() != 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + repeat(replaceChar, 4) + mobile.substring(7);
    }

    /**
     * 身份证脱敏（110***********1234）
     * <p>保留前 3 位与后 4 位，中间部分替换为占位字符。长度不足 10 时原样返回。</p>
     *
     * @param idCard       身份证号
     * @param replaceChar  替换占位字符
     * @return 脱敏后的身份证号
     */
    private static String desensitizeIdCard(String idCard, String replaceChar) {
        if (idCard.length() < 10) {
            return idCard;
        }
        int prefixLen = 3;
        int suffixLen = 4;
        String prefix = idCard.substring(0, prefixLen);
        String suffix = idCard.substring(idCard.length() - suffixLen);
        return prefix + repeat(replaceChar, idCard.length() - prefixLen - suffixLen) + suffix;
    }

    /**
     * 银行卡号脱敏（6222***********1234）
     * <p>保留前 4 位与后 4 位，中间部分替换为占位字符。长度不足 10 时原样返回。</p>
     *
     * @param bankCard     银行卡号
     * @param replaceChar  替换占位字符
     * @return 脱敏后的银行卡号
     */
    private static String desensitizeBankCard(String bankCard, String replaceChar) {
        if (bankCard.length() < 10) {
            return bankCard;
        }
        int prefixLen = 4;
        int suffixLen = 4;
        String prefix = bankCard.substring(0, prefixLen);
        String suffix = bankCard.substring(bankCard.length() - suffixLen);
        return prefix + repeat(replaceChar, bankCard.length() - prefixLen - suffixLen) + suffix;
    }

    /**
     * 邮箱脱敏（abc***@xx.com）
     * <p>保留首字符与域名部分，中间替换为 3 个占位字符。
     * 当 {@code @} 前缀长度不足时原样返回。</p>
     *
     * @param email        邮箱地址
     * @param replaceChar  替换占位字符
     * @return 脱敏后的邮箱地址
     */
    private static String desensitizeEmail(String email, String replaceChar) {
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) {
            return email;
        }
        String prefix = email.substring(0, 1);
        String suffix = email.substring(atIndex);
        return prefix + repeat(replaceChar, 3) + suffix;
    }

    /**
     * 姓名脱敏（*三）
     * <p>保留姓之后的部分，首字符替换为占位字符。长度不足等于 1 时原样返回。</p>
     *
     * @param name         姓名
     * @param replaceChar  替换占位字符
     * @return 脱敏后的姓名
     */
    private static String desensitizeName(String name, String replaceChar) {
        if (name.length() <= 1) {
            return name;
        }
        return replaceChar + name.substring(1);
    }

    /**
     * 地址脱敏（北京市****）
     * <p>保留前 2 位，其余部分替换为占位字符。长度不足等于 2 时原样返回。</p>
     *
     * @param address      地址
     * @param replaceChar  替换占位字符
     * @return 脱敏后的地址
     */
    private static String desensitizeAddress(String address, String replaceChar) {
        int len = address.length();
        if (len <= 2) {
            return address;
        }
        int keepLen = 2;
        String prefix = address.substring(0, keepLen);
        return prefix + repeat(replaceChar, len - keepLen);
    }

    /**
     * 自定义脱敏
     * <p>
     * 根据规则字符串 {@code rule}（格式：{@code 前缀长度-后缀长度}，如 {@code 3-4}）
     * 保留指定长度的前后缀，中间部分替换为占位字符。
     * </p>
     * <p>当规则为空、解析失败或保留长度之和大于等于原始长度时，原样返回。</p>
     *
     * @param value        原始数据
     * @param rule         自定义脱敏规则（格式：{@code 前缀长度-后缀长度}）
     * @param replaceChar  替换占位字符
     * @return 脱敏后的字符串
     */
    private static String desensitizeCustom(String value, String rule, String replaceChar) {
        if (rule == null || rule.isEmpty()) {
            return value;
        }

        try {
            String[] parts = rule.split("-");
            int prefixLen = Integer.parseInt(parts[0]);
            int suffixLen = Integer.parseInt(parts[1]);

            if (value.length() <= prefixLen + suffixLen) {
                return value;
            }

            String prefix = value.substring(0, prefixLen);
            String suffix = value.substring(value.length() - suffixLen);
            return prefix + repeat(replaceChar, value.length() - prefixLen - suffixLen) + suffix;
        } catch (Exception e) {
            // 规则解析或处理失败时记录日志并原样返回，避免影响主流程
            log.warn("自定义脱敏规则处理失败，value=\"{}\", rule=\"{}\"", value, rule, e);
            return value;
        }
    }

    /**
     * 将指定字符重复拼接指定次数
     * <p>
     * 注：JDK 11+ 可直接使用 {@link String#repeat(int)} 替代本方法，
     * 此处为兼容更低版本 JDK 保留循环拼接实现。
     * </p>
     *
     * @param str   需要重复的字符
     * @param count 重复次数
     * @return 重复拼接后的字符串
     */
    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
