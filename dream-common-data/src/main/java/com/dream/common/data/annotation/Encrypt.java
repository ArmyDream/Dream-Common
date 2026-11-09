package com.dream.common.data.annotation;

import com.dream.common.data.enums.EncryptType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段加密注解，标注于实体字段上，声明该字段在持久化前需加密、读取时需解密。
 * <p>
 * 支持自定义加密算法与密钥，配合 {@link com.dream.common.data.enums.EncryptType} 枚举使用。
 * {@link EncryptType#MD5} 是单向摘要，读取时不会尝试解密，实体查询结果中的该字段统一置空。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * public class User extends BaseEntity {
 *     @Encrypt(type = EncryptType.AES, key = "mySecretKey")
 *     private String phone;
 *
 *     @Encrypt(type = EncryptType.MD5)
 *     private String password;
 * }
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Encrypt {

    /**
     * 加密类型
     */
    EncryptType type() default EncryptType.AES;

    /**
     * 自定义密钥key
     */
    String key() default "";
}
