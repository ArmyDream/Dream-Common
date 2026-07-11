package com.dream.common.core.utils;

import com.dream.common.core.enums.DesensitizeType;

/**
 * 数据脱敏工具类
 * <p>
 * 配合 {@link DesensitizeType} 使用，对敏感数据进行掩码处理
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 手机号脱敏：138****8000
 * DesensitizeUtils.desensitize("13800138000", DesensitizeTypeEnum.PHONE);
 *
 * // 身份证脱敏：110101********1234
 * DesensitizeUtils.desensitize("110101199001011234", DesensitizeTypeEnum.ID_CARD);
 *
 * // 邮箱脱敏：ab***@example.com
 * DesensitizeUtils.desensitize("abcdef@example.com", DesensitizeTypeEnum.EMAIL);
 *
 * // 自定义脱敏：保留前2后2
 * DesensitizeUtils.desensitize("HelloWorld", 2, 2, '*');
 * }</pre>
 *
 */
public final class DesensitizeUtils {

    private DesensitizeUtils() {
    }

    /**
     * 根据脱敏类型对数据进行脱敏
     *
     * @param text 原始文本
     * @param type 脱敏类型
     * @return 脱敏后的文本
     */
    public static String desensitize(String text, DesensitizeType type) {
        if (text == null || text.isEmpty() || type == null) {
            return text;
        }
        return desensitize(text, type.getPrefixLen(), type.getSuffixLen(), type.getMaskChar());
    }

    /**
     * 自定义脱敏：保留前 prefixLen 位和后 suffixLen 位，中间用掩码替换
     *
     * @param text      原始文本
     * @param prefixLen 保留前缀长度
     * @param suffixLen 保留后缀长度
     * @param maskChar  掩码字符
     * @return 脱敏后的文本
     */
    public static String desensitize(String text, int prefixLen, int suffixLen, char maskChar) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        int len = text.length();

        // 全部掩码（如密码）
        if (prefixLen <= 0 && suffixLen <= 0) {
            return repeat(maskChar, len);
        }

        // 文本长度不足以保留前后缀
        if (len <= prefixLen + suffixLen) {
            return text;
        }

        StringBuilder sb = new StringBuilder(len);

        // 前缀
        sb.append(text, 0, prefixLen);

        // 掩码
        int maskLen = len - prefixLen - suffixLen;
        sb.append(repeat(maskChar, maskLen));

        // 后缀
        if (suffixLen > 0) {
            sb.append(text, len - suffixLen, len);
        }

        return sb.toString();
    }

    /**
     * 手机号脱敏：138****8000
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String desensitizePhone(String phone) {
        return desensitize(phone, DesensitizeType.PHONE);
    }

    /**
     * 身份证号脱敏：110101********1234
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String desensitizeIdCard(String idCard) {
        return desensitize(idCard, DesensitizeType.ID_CARD);
    }

    /**
     * 邮箱脱敏：ab***@example.com
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String desensitizeEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return desensitize(email, DesensitizeType.EMAIL);
        }
        // 保留 @ 前2位 + @ 后完整域名
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        int keepLen = Math.min(2, localPart.length());
        int maskLen = Math.max(localPart.length() - keepLen, 1);
        return localPart.substring(0, keepLen) + repeat('*', maskLen) + domainPart;
    }

    /**
     * 银行卡号脱敏：622202******1234
     *
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String desensitizeBankCard(String bankCard) {
        return desensitize(bankCard, DesensitizeType.BANK_CARD);
    }

    /**
     * 姓名脱敏：张*
     *
     * @param name 姓名
     * @return 脱敏后的姓名
     */
    public static String desensitizeName(String name) {
        return desensitize(name, DesensitizeType.REAL_NAME);
    }

    /**
     * 重复字符
     */
    private static String repeat(char ch, int count) {
        if (count <= 0) {
            return "";
        }
        char[] chars = new char[count];
        java.util.Arrays.fill(chars, ch);
        return new String(chars);
    }
}
