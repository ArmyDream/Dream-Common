package com.dream.common.data.enums;

/**
 * 脱敏类型枚举，定义字段脱敏所支持的规则类型。
 * <p>
 * 配合 {@link com.dream.common.data.annotation.Sensitive} 注解使用，
 * 标注实体字段需采用的脱敏方式。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * public class User extends BaseEntity {
 *     @Sensitive(type = SensitiveType.MOBILE)
 *     private String phone;
 *     @Sensitive(type = SensitiveType.ID_CARD)
 *     private String idCard;
 * }
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
public enum SensitiveType {
    /**
     * 手机号 138****1234
     */
    MOBILE,
    /**
     * 身份证 110***********1234
     */
    ID_CARD,
    /**
     * 银行卡号 6222***********1234
     */
    BANK_CARD,
    /**
     * 邮箱 abc***@xx.com
     */
    EMAIL,
    /**
     * 姓名 *三
     */
    NAME,
    /**
     * 地址 北京市****
     */
    ADDRESS,
    /**
     * 自定义
     */
    CUSTOM
}
