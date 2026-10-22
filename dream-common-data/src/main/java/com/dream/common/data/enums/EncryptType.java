package com.dream.common.data.enums;

/**
 * 加密类型枚举，定义字段加密所支持的算法。
 * <p>
 * 配合 {@link com.dream.common.data.annotation.Encrypt} 注解使用，
 * 标注实体字段需采用的加密方式。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * public class User extends BaseEntity {
 *     @Encrypt(type = EncryptType.AES)
 *     private String phone;
 * }
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
public enum EncryptType {
    /**
     * AES加密，对称加密算法
     */
    AES,
    /**
     * DES加密，对称加密算法
     */
    DES,
    /**
     * SM4加密(国密)，国密对称加密算法
     */
    SM4,
    /**
     * Base64编码，可逆编码（非加密）
     */
    BASE64,
    /**
     * MD5加密(不可逆)，单向散列算法
     */
    MD5
}
