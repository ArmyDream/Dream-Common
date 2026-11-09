package com.dream.common.core.utils;

import com.dream.common.core.enums.DesensitizeType;
import com.dream.common.core.enums.Regex;

/**
 * 数据脱敏工具类 —— 框架内掩码逻辑的唯一实现
 * <p>
 * 配合 {@link DesensitizeType} 使用，对敏感数据进行掩码处理。
 * 各业务模块（如 data 模块的 {@code @Sensitive}）只保留"类型 → 保留位数"的映射，
 * 掩码算法一律下沉到 {@link #desensitize(String, int, int, String)}，不再各自实现一份。
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 手机号脱敏：138****8000
 * DesensitizeUtils.desensitize("13800138000", DesensitizeType.PHONE);
 *
 * // 身份证脱敏：110101********1234
 * DesensitizeUtils.desensitize("110101199001011234", DesensitizeType.ID_CARD);
 *
 * // 邮箱脱敏：a*****@example.com
 * DesensitizeUtils.desensitizeEmail("abcdef@example.com");
 *
 * // 自定义脱敏：保留前2后2
 * DesensitizeUtils.desensitize("HelloWorld", 2, 2, '*');
 * }</pre>
 *
 * @author dream
 */
@SuppressWarnings({"unused", "WeakerAccess"})
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
     * 自定义脱敏：保留前 prefixLen 位和后 suffixLen 位，中间用掩码字符替换
     *
     * @param text      原始文本
     * @param prefixLen 保留前缀长度
     * @param suffixLen 保留后缀长度
     * @param maskChar  掩码字符
     * @return 脱敏后的文本
     */
    public static String desensitize(String text, int prefixLen, int suffixLen, char maskChar) {
        return desensitize(text, prefixLen, suffixLen, String.valueOf(maskChar));
    }

    /**
     * 自定义脱敏的唯一实现：保留前后缀，中间用掩码串重复替换
     * <p>
     * 掩码为字符串而非字符，便于调用方（如 data 模块 {@code @Sensitive} 的 {@code replaceChar}）
     * 指定任意占位内容；框架内所有掩码逻辑都走本方法，不再各写一份。
     * </p>
     * <p>
     * 规则：{@code prefixLen} 与 {@code suffixLen} 均为 0 时整体掩码；文本长度不足以同时保留
     * 前后缀时原样返回（不泄漏部分掩码结果）；负数按 0 处理。
     * </p>
     *
     * @param text      原始文本
     * @param prefixLen 保留前缀长度
     * @param suffixLen 保留后缀长度
     * @param mask      掩码串，null 或空时按 {@code *} 处理
     * @return 脱敏后的文本
     */
    public static String desensitize(String text, int prefixLen, int suffixLen, String mask) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String maskStr = (mask == null || mask.isEmpty()) ? "*" : mask;
        int prefix = Math.max(prefixLen, 0);
        int suffix = Math.max(suffixLen, 0);
        int len = text.length();

        // 全部掩码（如密码）
        if (prefix == 0 && suffix == 0) {
            return repeat(maskStr, len);
        }

        // 文本长度不足以保留前后缀
        if (len <= prefix + suffix) {
            return text;
        }

        StringBuilder sb = new StringBuilder(len + maskStr.length());

        // 前缀
        sb.append(text, 0, prefix);

        // 掩码
        sb.append(repeat(maskStr, len - prefix - suffix));

        // 后缀
        if (suffix > 0) {
            sb.append(text, len - suffix, len);
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
     * 邮箱脱敏：a*****@example.com
     * <p>
     * 框架内唯一的邮箱规则（data 模块 {@code @Sensitive(type = EMAIL)} 与之相同）：
     * 保留本地部分首字符与完整的 {@code @} + 域名，本地部分其余字符逐位掩码；
     * 本地部分只有 1 位时连该字符一起掩掉（否则等于不脱敏）；
     * 不含 {@code @} 的入参按 {@link DesensitizeType#EMAIL} 的通用前后缀规则处理。
     * </p>
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String desensitizeEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex < 1) {
            // 不是邮箱形态：退回通用"保留前2后3"规则
            return desensitize(email, DesensitizeType.EMAIL);
        }
        if (atIndex == 1) {
            // 本地部分仅 1 位，保留它等于不脱敏：掩掉首字符，@ 与域名保留
            return desensitize(email, 0, email.length() - 1, '*');
        }
        // 框架内唯一的邮箱规则（data 模块 @Sensitive 的 EMAIL 与之相同）：
        // 保留本地部分首字符 + 完整 @ 与域名，本地部分其余字符逐位掩码
        return desensitize(email, 1, email.length() - atIndex, '*');
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
     * 重复掩码串（JDK 8 无 {@link String#repeat(int)}，此处循环拼接）
     */
    private static String repeat(String mask, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(mask.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(mask);
        }
        return sb.toString();
    }

    /**
     * 敏感参数名关键字（小写包含匹配）。
     * <p>
     * 访问日志、异常日志、操作日志共用本表判断"参数名是否承载凭据类信息"，
     * 避免各处各写一份、清单互相不一致。
     * </p>
     */
    private static final String[] SENSITIVE_PARAM_KEYWORDS = {
            "password", "passwd", "pwd", "token", "secret", "authorization", "credential", "code", "captcha"
    };

    /** 业务追加的敏感参数名关键字，与内置清单叠加生效 */
    private static final java.util.Set<String> EXTRA_SENSITIVE_PARAM_KEYWORDS =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * 追加业务自定义的敏感参数名关键字（小写包含匹配），与内置清单叠加生效。
     * <p>内置清单不含业务自命名的凭据参数时（如 {@code smsCode}、{@code apiKey}），
     * 在应用启动时调用本方法注册，访问日志/异常日志/操作日志会一并掩码。</p>
     *
     * @param keywords 关键字，自动转小写并去除首尾空白，空值忽略
     */
    public static void registerSensitiveParamKeywords(String... keywords) {
        if (keywords == null) {
            return;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                EXTRA_SENSITIVE_PARAM_KEYWORDS.add(keyword.trim().toLowerCase(java.util.Locale.ROOT));
            }
        }
    }

    /**
     * 判断参数名是否属于敏感参数（不区分大小写的包含匹配）
     *
     * @param paramName 参数名，可为 null
     * @return true 表示命中敏感关键字
     */
    public static boolean isSensitiveParamName(String paramName) {
        if (paramName == null || paramName.isEmpty()) {
            return false;
        }
        String lower = paramName.toLowerCase(java.util.Locale.ROOT);
        for (String keyword : SENSITIVE_PARAM_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        for (String keyword : EXTRA_SENSITIVE_PARAM_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对查询串中的敏感参数值脱敏（{@code password=123 → password=***}）
     * <p>
     * 参数名保持原样，仅替换值；无法解析为 {@code k=v} 的片段原样保留。
     * </p>
     *
     * @param queryString 原始查询串，可为 null
     * @return 脱敏后的查询串；入参为 null 时返回 null
     */
    public static String maskSensitiveQuery(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return queryString;
        }
        String[] pairs = queryString.split("&");
        StringBuilder sb = new StringBuilder(queryString.length());
        for (int i = 0; i < pairs.length; i++) {
            if (i > 0) {
                sb.append('&');
            }
            String pair = pairs[i];
            int eq = pair.indexOf('=');
            if (eq > 0 && isSensitiveParamName(pair.substring(0, eq))) {
                sb.append(pair, 0, eq).append("=***");
                continue;
            }
            sb.append(pair);
        }
        return sb.toString();
    }

    /**
     * 判断字符串是否匹配指定正则枚举
     *
     * @param str   待校验字符串
     * @param regex 正则枚举
     * @return true 表示匹配；str 或 regex 为 null 时返回 false
     */
    public static boolean matches(String str, Regex regex) {
        return str != null && regex != null && regex.getPattern().matcher(str).matches();
    }
}
