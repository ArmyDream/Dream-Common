package com.dream.common.core.enums;

/**
 * 加密方式枚举
 * <p>
 * 涵盖密码加密、数据加密、签名算法等场景
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 获取当前使用的加密方式
 * CryptoEnum crypto = CryptoEnum.BCRYPT;
 *
 * // 判断算法类型
 * crypto.isPasswordEncoder();  // true
 * crypto.isSymmetric();        // false
 *
 * // 前端下拉框
 * List<BaseEnum.EnumOption<String>> options = BaseEnum.options(CryptoEnum.class);
 * }</pre>
 *
 * @author dream
 */
public enum Crypto implements BaseEnum<String> {

    // ==================== 密码加密算法 ====================
    BCRYPT("bcrypt", "BCrypt", "BCrypt 加密"),
    PBKDF2("pbkdf2", "PBKDF2", "PBKDF2 加密"),
    SCRYPT("scrypt", "SCrypt", "SCrypt 加密"),
    ARGON2("argon2", "Argon2", "Argon2 加密"),
    MD5("md5", "MD5", "MD5 加密"),
    SHA256("sha256", "SHA-256", "SHA-256 加密"),
    SHA512("sha512", "SHA-512", "SHA-512 加密"),

    // ==================== 对称加密 ====================
    AES("aes", "AES", "AES 对称加密"),
    DES("des", "DES", "DES 对称加密"),
    DES3("des3", "3DES", "3DES 对称加密"),
    SM4("sm4", "SM4", "国密 SM4 对称加密"),
    RC4("rc4", "RC4", "RC4 对称加密"),

    // ==================== 非对称加密 ====================
    RSA("rsa", "RSA", "RSA 非对称加密"),
    DSA("dsa", "DSA", "DSA 数字签名"),
    EC("ec", "Elliptic Curve", "椭圆曲线加密"),
    SM2("sm2", "SM2", "国密 SM2 非对称加密"),

    // ==================== 摘要算法 ====================
    SM3("sm3", "SM3", "国密 SM3 摘要算法"),
    SHA1("sha1", "SHA-1", "SHA-1 摘要算法"),
    MD5_DIGEST("md5_digest", "MD5 Digest", "MD5 摘要算法"),
    HASH("hash", "Hash", "哈希摘要"),

    // ==================== JWT 签名算法 ====================
    HS256("HS256", "HS256", "HMAC SHA-256"),
    HS384("HS384", "HS384", "HMAC SHA-384"),
    HS512("HS512", "HS512", "HMAC SHA-512"),
    RS256("RS256", "RS256", "RSA SHA-256"),
    RS384("RS384", "RS384", "RSA SHA-384"),
    RS512("RS512", "RS512", "RSA SHA-512"),
    ES256("ES256", "ES256", "ECDSA SHA-256"),
    ES384("ES384", "ES384", "ECDSA SHA-384"),
    ES512("ES512", "ES512", "ECDSA SHA-512"),
    PS256("PS256", "PS256", "RSA PSS SHA-256"),
    PS384("PS384", "PS384", "RSA PSS SHA-384"),
    PS512("PS512", "PS512", "RSA PSS SHA-512"),

    // ==================== Base 编码 ====================
    BASE64("base64", "Base64", "Base64 编码"),
    BASE64_URL("base64_url", "Base64 URL", "Base64 URL 安全编码"),
    BASE64_MIME("base64_mime", "Base64 MIME", "Base64 MIME 编码"),
    HEX("hex", "Hex", "十六进制编码"),
    URL_ENCODE("url_encode", "URL Encode", "URL 编码"),
    ;

    private final String value;
    private final String labelEn;
    private final String labelZh;

    Crypto(String value, String labelEn, String labelZh) {
        this.value = value;
        this.labelEn = labelEn;
        this.labelZh = labelZh;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return labelZh;
    }

    public String getLabelEn() {
        return labelEn;
    }

    public String getLabelZh() {
        return labelZh;
    }

    public String getLabel(String lang) {
        if ("en".equalsIgnoreCase(lang)) {
            return labelEn;
        }
        return labelZh;
    }

    @Override
    public String getColor() {
        switch (this) {
            // 密码加密（绿色系）
            case BCRYPT:
            case PBKDF2:
            case SCRYPT:
            case ARGON2:
                return "#52C41A";

            // MD5/SHA1（橙色系——不安全，建议废弃）
            case MD5:
            case SHA1:
            case MD5_DIGEST:
                return "#FAAD14";

            // SHA256/SHA512（蓝色系——安全）
            case SHA256:
            case SHA512:
                return "#1890FF";

            // 对称加密（紫色系）
            case AES:
            case DES:
            case DES3:
                return "#722ED1";

            // 非对称加密（红色系）
            case RSA:
            case DSA:
            case EC:
                return "#FF4D4F";

            // 国密算法（金色系）
            case SM3:
            case SM4:
            case SM2:
                return "#FA8C16";

            // JWT 签名（青色系）
            case HS256:
            case HS384:
            case HS512:
            case RS256:
            case RS384:
            case RS512:
            case ES256:
            case ES384:
            case ES512:
            case PS256:
            case PS384:
            case PS512:
                return "#13C2C2";

            // 编码类（灰色）
            case BASE64:
            case BASE64_URL:
            case BASE64_MIME:
            case HEX:
            case URL_ENCODE:
                return "#8C8C8C";

            default:
                return "#8C8C8C";
        }
    }

    // ==================== 业务判断方法 ====================

    /**
     * 是否为密码编码器（适合存储密码的算法）
     */
    public boolean isPasswordEncoder() {
        return this == BCRYPT || this == PBKDF2 || this == SCRYPT || this == ARGON2
                || this == MD5 || this == SHA256 || this == SHA512;
    }

    /**
     * 是否为对称加密
     */
    public boolean isSymmetric() {
        return this == AES || this == DES || this == DES3 || this == SM4 || this == RC4;
    }

    /**
     * 是否为非对称加密
     */
    public boolean isAsymmetric() {
        return this == RSA || this == DSA || this == EC || this == SM2;
    }

    /**
     * 是否为国密算法
     */
    public boolean isSM() {
        return this == SM2 || this == SM3 || this == SM4;
    }

    /**
     * 是否为 JWT 签名算法
     */
    public boolean isJwtAlgorithm() {
        return this == HS256 || this == HS384 || this == HS512
                || this == RS256 || this == RS384 || this == RS512
                || this == ES256 || this == ES384 || this == ES512
                || this == PS256 || this == PS384 || this == PS512;
    }

    /**
     * 是否为 HMAC 算法
     */
    public boolean isHmac() {
        return this == HS256 || this == HS384 || this == HS512;
    }

    /**
     * 是否为 RSA 算法
     */
    public boolean isRsa() {
        return this == RSA || this == RS256 || this == RS384 || this == RS512
                || this == PS256 || this == PS384 || this == PS512;
    }

    /**
     * 是否为摘要算法
     */
    public boolean isDigest() {
        return this == SM3 || this == SHA1 || this == MD5_DIGEST || this == HASH;
    }

    /**
     * 是否为编码（非加密）
     */
    public boolean isEncoding() {
        return this == BASE64 || this == BASE64_URL || this == BASE64_MIME
                || this == HEX || this == URL_ENCODE;
    }

    /**
     * 是否安全（推荐使用）
     */
    public boolean isSecure() {
        return this == BCRYPT || this == PBKDF2 || this == SCRYPT || this == ARGON2
                || this == SHA256 || this == SHA512
                || this == AES || this == RSA
                || this == SM2 || this == SM3 || this == SM4
                || isJwtAlgorithm();
    }

}