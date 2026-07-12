package com.dream.common.framework.utils.crypto;

import com.dream.common.core.enums.CharsetEnum;
import com.dream.common.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Objects;

/**
 * HMAC 签名工具类
 * <p>
 * 提供 HMAC-SHA256/384/512 和 HMAC-MD5 签名能力，用于接口签名、消息认证等场景。
 * 内部使用 {@link ThreadLocal} 缓存 {@link Mac} 实例，避免重复创建开销。
 * </p>
 * <p>
 * 核心能力：
 * <ul>
 *   <li>{@link #hmacSha256(String, String)} / {@link #hmacSha256Base64(String, String)} —— HMAC-SHA256 签名</li>
 *   <li>{@link #hmacSha512(String, String)} / {@link #hmacSha512Base64(String, String)} —— HMAC-SHA512 签名</li>
 *   <li>{@link #hmacMd5(String, String)} —— HMAC-MD5 签名</li>
 *   <li>{@link #verify(String, String, String)} —— 签名校验</li>
 *   <li>{@link #hmac(String, String, String)} —— 通用 HMAC 签名</li>
 * </ul>
 * </p>
 *
 * @author dream
 */
@Slf4j
@SuppressWarnings({"unused", "WeakerAccess"})
public final class HmacUtils {

    /**
     * HMAC 算法名称常量
     */
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String HMAC_SHA384 = "HmacSHA384";
    private static final String HMAC_SHA512 = "HmacSHA512";
    private static final String HMAC_MD5 = "HmacMD5";

    /**
     * Hex 字符表
     */
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    /**
     * ThreadLocal 缓存 Mac 实例
     */
    private static final ThreadLocal<Mac> HMAC_SHA256_MAC =
            ThreadLocal.withInitial(() -> getInstance(HMAC_SHA256));
    private static final ThreadLocal<Mac> HMAC_SHA384_MAC =
            ThreadLocal.withInitial(() -> getInstance(HMAC_SHA384));
    private static final ThreadLocal<Mac> HMAC_SHA512_MAC =
            ThreadLocal.withInitial(() -> getInstance(HMAC_SHA512));
    private static final ThreadLocal<Mac> HMAC_MD5_MAC =
            ThreadLocal.withInitial(() -> getInstance(HMAC_MD5));

    private HmacUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== HMAC-SHA256 ====================

    /**
     * HMAC-SHA256 签名（十六进制输出）
     *
     * @param data 数据，不能为 null
     * @param key  密钥，不能为 null
     * @return 十六进制签名
     * @throws BusinessException 如果签名失败
     */
    public static String hmacSha256(String data, String key) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(key, "key must not be null");
        return doHmac(HMAC_SHA256_MAC.get(), data, key);
    }

    /**
     * HMAC-SHA256 签名（Base64 输出）
     *
     * @param data 数据，不能为 null
     * @param key  密钥，不能为 null
     * @return Base64 签名
     * @throws BusinessException 如果签名失败
     */
    public static String hmacSha256Base64(String data, String key) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(key, "key must not be null");
        return doHmacBase64(HMAC_SHA256_MAC.get(), data, key);
    }

    /**
     * HMAC-SHA256 签名（字节数组）
     *
     * @param data 数据，不能为 null
     * @param key  密钥字节数组，不能为 null
     * @return 签名字节数组
     * @throws BusinessException 如果签名失败
     */
    public static byte[] hmacSha256(byte[] data, byte[] key) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(key, "key must not be null");
        return doHmacBytes(HMAC_SHA256_MAC.get(), data, key);
    }

    // ==================== HMAC-SHA384 ====================

    public static String hmacSha384(String data, String key) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(key, "key must not be null");
        return doHmac(HMAC_SHA384_MAC.get(), data, key);
    }

    public static String hmacSha384Base64(String data, String key) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(key, "key must not be null");
        return doHmacBase64(HMAC_SHA384_MAC.get(), data, key);
    }

    // ==================== HMAC-SHA512 ====================

    public static String hmacSha512(String data, String key) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(key, "key must not be null");
        return doHmac(HMAC_SHA512_MAC.get(), data, key);
    }

    public static String hmacSha512Base64(String data, String key) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(key, "key must not be null");
        return doHmacBase64(HMAC_SHA512_MAC.get(), data, key);
    }

    public static byte[] hmacSha512(byte[] data, byte[] key) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(key, "key must not be null");
        return doHmacBytes(HMAC_SHA512_MAC.get(), data, key);
    }

    // ==================== HMAC-MD5 ====================

    public static String hmacMd5(String data, String key) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(key, "key must not be null");
        return doHmac(HMAC_MD5_MAC.get(), data, key);
    }

    public static String hmacMd5Base64(String data, String key) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(key, "key must not be null");
        return doHmacBase64(HMAC_MD5_MAC.get(), data, key);
    }

    // ==================== 签名校验 ====================

    public static boolean verify(String data, String key, String expectedSign) {
        if (data == null || key == null || expectedSign == null) {
            return false;
        }
        String actual = hmacSha256(data, key);
        return expectedSign.equalsIgnoreCase(actual);
    }

    public static boolean verifySha512(String data, String key, String expectedSign) {
        if (data == null || key == null || expectedSign == null) {
            return false;
        }
        String actual = hmacSha512(data, key);
        return expectedSign.equalsIgnoreCase(actual);
    }

    // ==================== 通用 HMAC ====================

    /**
     * 通用 HMAC 签名（指定算法）
     *
     * @param algorithm HMAC 算法名（如 HmacSHA256、HmacMD5），不能为 null
     * @param data      数据，不能为 null
     * @param key       密钥，不能为 null
     * @return 十六进制签名
     * @throws BusinessException 如果签名失败
     */
    public static String hmac(String algorithm, String data, String key) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(key, "key must not be null");

        try {
            Mac mac = getInstance(algorithm);
            SecretKey secretKey = new SecretKeySpec(
                    key.getBytes(CharsetEnum.UTF_8.getCharset()), algorithm);
            mac.init(secretKey);
            byte[] result = mac.doFinal(data.getBytes(CharsetEnum.UTF_8.getCharset()));
            return toHex(result);
        } catch (Exception e) {
            log.error("HMAC failed with algorithm: {}", algorithm, e);
            throw new BusinessException("HMAC failed with algorithm: " + algorithm, e);
        }
    }

    /**
     * 通用 HMAC 签名（Base64 输出）
     */
    public static String hmacBase64(String algorithm, String data, String key) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(key, "key must not be null");

        try {
            Mac mac = getInstance(algorithm);
            SecretKey secretKey = new SecretKeySpec(
                    key.getBytes(CharsetEnum.UTF_8.getCharset()), algorithm);
            mac.init(secretKey);
            byte[] result = mac.doFinal(data.getBytes(CharsetEnum.UTF_8.getCharset()));
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            log.error("HMAC Base64 failed with algorithm: {}", algorithm, e);
            throw new BusinessException("HMAC Base64 failed with algorithm: " + algorithm, e);
        }
    }

    // ==================== 内部方法 ====================

    private static Mac getInstance(String algorithm) {
        try {
            return Mac.getInstance(algorithm);
        } catch (Exception e) {
            log.error("Failed to get Mac for algorithm: {}", algorithm, e);
            throw new BusinessException("Unsupported HMAC algorithm: " + algorithm, e);
        }
    }

    private static String doHmac(Mac mac, String data, String key) {
        try {
            SecretKey secretKey = new SecretKeySpec(
                    key.getBytes(CharsetEnum.UTF_8.getCharset()), mac.getAlgorithm());
            mac.init(secretKey);
            byte[] result = mac.doFinal(data.getBytes(CharsetEnum.UTF_8.getCharset()));
            return toHex(result);
        } catch (Exception e) {
            log.error("HMAC failed: {}", mac.getAlgorithm(), e);
            throw new BusinessException("HMAC failed: " + mac.getAlgorithm(), e);
        }
    }

    private static String doHmacBase64(Mac mac, String data, String key) {
        try {
            SecretKey secretKey = new SecretKeySpec(
                    key.getBytes(CharsetEnum.UTF_8.getCharset()), mac.getAlgorithm());
            mac.init(secretKey);
            byte[] result = mac.doFinal(data.getBytes(CharsetEnum.UTF_8.getCharset()));
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            log.error("HMAC Base64 failed: {}", mac.getAlgorithm(), e);
            throw new BusinessException("HMAC Base64 failed: " + mac.getAlgorithm(), e);
        }
    }

    private static byte[] doHmacBytes(Mac mac, byte[] data, byte[] key) {
        try {
            SecretKey secretKey = new SecretKeySpec(key, mac.getAlgorithm());
            mac.init(secretKey);
            return mac.doFinal(data);
        } catch (Exception e) {
            log.error("HMAC bytes failed: {}", mac.getAlgorithm(), e);
            throw new BusinessException("HMAC bytes failed: " + mac.getAlgorithm(), e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            chars[i * 2] = HEX_DIGITS[b >>> 4];
            chars[i * 2 + 1] = HEX_DIGITS[b & 0x0F];
        }
        return new String(chars);
    }
}