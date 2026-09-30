package com.dream.common.data.enums;

/**
 * 脱敏类型枚举，定义字段脱敏所支持的规则类型。
 * <p>
 * 配合 {@link com.dream.common.data.annotation.Sensitive} 注解使用，
 * 标注实体字段需采用的脱敏方式。各类型保留的位数见
 * {@link com.dream.common.data.utils.SensitiveUtil}（映射表唯一入口），
 * 掩码算法复用 core 模块 {@code DesensitizeUtils}。
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
     * 手机号 138****1234（仅 11 位时脱敏）
     */
    MOBILE,
    /**
     * 身份证 110***********1234（保留前3后4，长度不足 10 位不处理）
     */
    ID_CARD,
    /**
     * 银行卡号 6222***********1234（保留前4后4，长度不足 10 位不处理）
     */
    BANK_CARD,
    /**
     * 邮箱 a*****@example.com（保留本地部分首字符与完整域名）
     */
    EMAIL,
    /**
     * 姓名 张*（保留姓氏，名字隐藏）
     */
    NAME,
    /**
     * 地址 北京*****（保留前 2 位，如省市；示例输入"北京市朝阳区"）
     */
    ADDRESS,
    /**
     * 自定义，由 {@code rule} 指定保留的前后缀长度（如 {@code 3-4}）
     */
    CUSTOM
}
