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
 *     aes-iv:  XXXX  # AES IV（建议 16 字节）
 *     des-key: XXXX  # DES 密钥（8 字节）
 *     des-iv:  XXXX  # DES IV（8 字节）
 *     md5-salt: XXXX # MD5 加盐前缀
 * }</pre>
 *
 * @author dream
 */
@ConfigurationProperties(prefix = "dream.encrypt")
public class EncryptProperties {

    /** AES 密钥 */
    private String aesKey;

    /** AES IV */
    private String aesIv;

    /** DES 密钥 */
    private String desKey;

    /** DES IV */
    private String desIv;

    /** MD5 加盐前缀 */
    private String md5Salt;

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }

    public String getAesIv() {
        return aesIv;
    }

    public void setAesIv(String aesIv) {
        this.aesIv = aesIv;
    }

    public String getDesKey() {
        return desKey;
    }

    public void setDesKey(String desKey) {
        this.desKey = desKey;
    }

    public String getDesIv() {
        return desIv;
    }

    public void setDesIv(String desIv) {
        this.desIv = desIv;
    }

    public String getMd5Salt() {
        return md5Salt;
    }

    public void setMd5Salt(String md5Salt) {
        this.md5Salt = md5Salt;
    }
}