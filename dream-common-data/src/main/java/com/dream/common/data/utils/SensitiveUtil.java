package com.dream.common.data.utils;

import com.dream.common.core.utils.DesensitizeUtils;
import com.dream.common.data.enums.SensitiveType;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据脱敏工具类 —— {@link SensitiveType} 到掩码规则的映射表
 * <p>
 * 掩码算法不在本类实现，统一委托 core 模块的
 * {@link DesensitizeUtils#desensitize(String, int, int, String)}（框架内唯一实现）。
 * 本类只负责两件事：按 {@link SensitiveType} 选择"保留前几位 / 后几位"，
 * 以及在数据明显不属于该类型（长度不符、无 {@code @}）时不做处理，避免输出误导性的部分掩码。
 * </p>
 * <p>
 * <b>保留位数与 core 的 {@code DesensitizeType} 故意不同</b>：本注解面向前端展示，取值更严格
 * （身份证前3后4 而非前6后4，不暴露属地码；地址只留前2位即省市，而 core 的 ADDRESS 留前6位）。
 * 两侧数字不要相互"对齐"，改动任一侧都会影响已上线的展示结果。
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
        if (value == null || value.isEmpty() || type == null) {
            return value;
        }

        String replace = replaceChar == null ? "*" : replaceChar;

        switch (type) {
            case MOBILE:
                // 仅 11 位手机号才脱敏，其余长度原样返回（避免把座机/测试号掩成误导性的结果）
                return value.length() == 11
                        ? DesensitizeUtils.desensitize(value, 3, 4, replace)
                        : value;
            case ID_CARD:
                // 长度不足 10 位不处理；保留前3后4（core 的 ID_CARD 保留前6，会露出属地码，
                // 展示场景按更严格的这条走）
                return value.length() < 10
                        ? value
                        : DesensitizeUtils.desensitize(value, 3, 4, replace);
            case BANK_CARD:
                // 保留前4后4（BIN 前缀 + 末4位可定位发卡行与卡号尾号，再多了就等于不脱敏）
                return value.length() < 10
                        ? value
                        : DesensitizeUtils.desensitize(value, 4, 4, replace);
            case EMAIL:
                return desensitizeEmail(value, replace);
            case NAME:
                // 保留姓氏、隐藏名字：张三 -> 张*
                return DesensitizeUtils.desensitize(value, 1, 0, replace);
            case ADDRESS:
                // 只保留前 2 位（省市），core 的 ADDRESS 保留前 6 位对展示太宽
                return DesensitizeUtils.desensitize(value, 2, 0, replace);
            case CUSTOM:
                return desensitizeCustom(value, rule, replace);
            default:
                return value;
        }
    }

    /**
     * 邮箱脱敏：保留本地部分首字符，{@code @} 与域名完整保留，本地部分其余字符掩码替换
     * <p>
     * 与 core 的 {@code DesensitizeUtils.desensitizeEmail(String)} 为同一条规则，
     * 差别只在占位字符可自定义，以及无 {@code @} 的畸形值本类原样返回（core 会按通用前后缀规则掩码）。
     * </p>
     *
     * @param email       邮箱地址
     * @param replaceChar 替换占位字符
     * @return 脱敏后的邮箱地址
     */
    private static String desensitizeEmail(String email, String replaceChar) {
        int atIndex = email.indexOf('@');
        if (atIndex < 1) {
            // 无 @ 或 "@domain" 形态：没有本地部分可掩，原样返回
            return email;
        }
        // 本地部分仅 1 位时连该字符一起掩掉，否则等于不脱敏
        int prefix = atIndex == 1 ? 0 : 1;
        return DesensitizeUtils.desensitize(email, prefix, email.length() - atIndex, replaceChar);
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
            int prefixLen = Integer.parseInt(parts[0].trim());
            int suffixLen = Integer.parseInt(parts[1].trim());
            return DesensitizeUtils.desensitize(value, prefixLen, suffixLen, replaceChar);
        } catch (Exception e) {
            // 规则解析失败时记录日志并原样返回，避免影响主流程
            log.warn("自定义脱敏规则处理失败，value=\"{}\", rule=\"{}\"", value, rule, e);
            return value;
        }
    }
}
