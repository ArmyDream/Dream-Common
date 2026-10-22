package com.dream.common.data.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 加密密钥配置属性。
 * <p>
 * 绑定 {@code dream.encrypt.*} 前缀的密钥配置，用于 {@code EncryptUtil} 的密钥注入。
 * </p>
 *
 * <h2>配置示例</h2>
 * <pre>{@code
 * dream:
 *   encrypt:
 *     aes-key: XXXX  # AES 密钥（建议 16 字节）
 *     des-key: XXXX  # DES 密钥（8 字节）
 *     md5-salt: XXXX # MD5 加盐前缀
 * }</pre>
 *
 * <h2>关于 IV 配置项</h2>
 * <p>
 * 早期版本提供 {@code dream.encrypt.aes-iv} / {@code dream.encrypt.des-iv}，
 * 固定 IV 使同一明文恒产生同一密文，泄露等值关系，已被移除。
 * CBC 的 IV 现在每次加密随机生成并写入密文头部（{@code Base64(IV + 密文)}），
 * 历史密文仍可正常解密；残留的 {@code *-iv} 配置项会被 Spring 绑定忽略。
 * </p>
 *
 * @author dream
 */
@ConfigurationProperties(prefix = "dream.encrypt")
public class EncryptProperties {

    /** AES 密钥 */
    private String aesKey;

    /** DES 密钥 */
    private String desKey;

    /** MD5 加盐前缀 */
    private String md5Salt;

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }

    public String getDesKey() {
        return desKey;
    }

    public void setDesKey(String desKey) {
        this.desKey = desKey;
    }

    public String getMd5Salt() {
        return md5Salt;
    }

    public void setMd5Salt(String md5Salt) {
        this.md5Salt = md5Salt;
    }
}
