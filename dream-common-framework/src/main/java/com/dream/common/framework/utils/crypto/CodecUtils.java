package com.dream.common.framework.utils.crypto;

import com.dream.common.core.enums.CharsetEnum;
import com.dream.common.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * 编码/解码工具类
 * <p>
 * 提供 Base64、URL 安全 Base64、MIME Base64、Hex、URL 编码等能力。
 * </p>
 * <p>
 * 核心能力：
 * <ul>
 *   <li>{@link #base64Encode(String)} / {@link #base64Decode(String)} —— 标准 Base64</li>
 *   <li>{@link #base64UrlEncode(String)} / {@link #base64UrlDecode(String)} —— URL 安全 Base64</li>
 *   <li>{@link #hexEncode(byte[])} / {@link #hexDecode(String)} —— 十六进制编解码</li>
 *   <li>{@link #urlEncode(String)} / {@link #urlDecode(String)} —— URL 编码</li>
 *   <li>{@link #isBase64(String)} / {@link #isHex(String)} —— 格式校验</li>
 * </ul>
 * </p>
 *
 * @author dream
 */
@Slf4j
@SuppressWarnings({"unused", "WeakerAccess"})
public final class CodecUtils {

    /**
     * Hex 字符表
     */
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private CodecUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== 标准 Base64 ====================

    /**
     * 标准 Base64 编码
     *
     * @param data 原始数据，不能为 null
     * @return Base64 编码字符串
     * @throws BusinessException 如果编码失败
     */
    public static String base64Encode(String data) {
        Objects.requireNonNull(data, "data must not be null");
        return Base64.getEncoder().encodeToString(data.getBytes(CharsetEnum.UTF_8.getCharset()));
    }

    /**
     * 标准 Base64 编码（字节数组）
     *
     * @param data 原始字节数组，不能为 null
     * @return Base64 编码字符串
     */
    public static String base64Encode(byte[] data) {
        Objects.requireNonNull(data, "data must not be null");
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * 标准 Base64 解码
     *
     * @param base64 Base64 编码字符串，不能为 null
     * @return 解码后的原始字符串
     * @throws BusinessException 如果解码失败
     */
    public static String base64Decode(String base64) {
        Objects.requireNonNull(base64, "base64 must not be null");
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            return new String(decoded, CharsetEnum.UTF_8.getCharset());
        } catch (IllegalArgumentException e) {
            log.error("Base64 decode failed", e);
            throw new BusinessException("Base64 decode failed", e);
        }
    }

    /**
     * 标准 Base64 解码为字节数组
     *
     * @param base64 Base64 编码字符串，不能为 null
     * @return 解码后的字节数组
     * @throws BusinessException 如果解码失败
     */
    public static byte[] base64DecodeToBytes(String base64) {
        Objects.requireNonNull(base64, "base64 must not be null");
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            log.error("Base64 decode failed", e);
            throw new BusinessException("Base64 decode failed", e);
        }
    }

    // ==================== URL 安全 Base64 ====================

    /**
     * URL 安全 Base64 编码（无填充）
     *
     * @param data 原始数据，不能为 null
     * @return URL 安全的 Base64 编码
     */
    public static String base64UrlEncode(String data) {
        Objects.requireNonNull(data, "data must not be null");
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(data.getBytes(CharsetEnum.UTF_8.getCharset()));
    }

    /**
     * URL 安全 Base64 编码（字节数组）
     *
     * @param data 原始字节数组，不能为 null
     * @return URL 安全的 Base64 编码
     */
    public static String base64UrlEncode(byte[] data) {
        Objects.requireNonNull(data, "data must not be null");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    /**
     * URL 安全 Base64 解码
     *
     * @param base64 URL 安全的 Base64 编码，不能为 null
     * @return 解码后的原始字符串
     * @throws BusinessException 如果解码失败
     */
    public static String base64UrlDecode(String base64) {
        Objects.requireNonNull(base64, "base64 must not be null");
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(base64);
            return new String(decoded, CharsetEnum.UTF_8.getCharset());
        } catch (IllegalArgumentException e) {
            log.error("Base64 URL decode failed", e);
            throw new BusinessException("Base64 URL decode failed", e);
        }
    }

    /**
     * URL 安全 Base64 解码为字节数组
     *
     * @param base64 URL 安全的 Base64 编码，不能为 null
     * @return 解码后的字节数组
     * @throws BusinessException 如果解码失败
     */
    public static byte[] base64UrlDecodeToBytes(String base64) {
        Objects.requireNonNull(base64, "base64 must not be null");
        try {
            return Base64.getUrlDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            log.error("Base64 URL decode failed", e);
            throw new BusinessException("Base64 URL decode failed", e);
        }
    }

    // ==================== MIME Base64 ====================

    /**
     * MIME Base64 编码（每 76 字符自动换行）
     *
     * @param data 原始数据，不能为 null
     * @return MIME Base64 编码
     */
    public static String base64MimeEncode(String data) {
        Objects.requireNonNull(data, "data must not be null");
        return Base64.getMimeEncoder().encodeToString(data.getBytes(CharsetEnum.UTF_8.getCharset()));
    }

    /**
     * MIME Base64 解码
     *
     * @param base64 MIME Base64 编码，不能为 null
     * @return 解码后的原始字符串
     * @throws BusinessException 如果解码失败
     */
    public static String base64MimeDecode(String base64) {
        Objects.requireNonNull(base64, "base64 must not be null");
        try {
            byte[] decoded = Base64.getMimeDecoder().decode(base64);
            return new String(decoded, CharsetEnum.UTF_8.getCharset());
        } catch (IllegalArgumentException e) {
            log.error("Base64 MIME decode failed", e);
            throw new BusinessException("Base64 MIME decode failed", e);
        }
    }

    // ==================== 十六进制 ====================

    /**
     * 字节数组转十六进制字符串
     *
     * @param bytes 字节数组，不能为 null
     * @return 小写十六进制字符串
     */
    public static String hexEncode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes must not be null");
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            chars[i * 2] = HEX_DIGITS[b >>> 4];
            chars[i * 2 + 1] = HEX_DIGITS[b & 0x0F];
        }
        return new String(chars);
    }

    /**
     * 字符串转十六进制
     *
     * @param data 原始字符串，不能为 null
     * @return 十六进制字符串
     */
    public static String hexEncode(String data) {
        Objects.requireNonNull(data, "data must not be null");
        return hexEncode(data.getBytes(CharsetEnum.UTF_8.getCharset()));
    }

    /**
     * 十六进制字符串转字节数组
     *
     * @param hex 十六进制字符串，不能为 null
     * @return 字节数组
     * @throws BusinessException 如果解码失败
     */
    public static byte[] hexDecode(String hex) {
        Objects.requireNonNull(hex, "hex must not be null");

        String cleanHex = hex;
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            cleanHex = hex.substring(2);
        }

        int len = cleanHex.length();
        if (len % 2 != 0) {
            throw new BusinessException("Hex string must have even length");
        }

        byte[] result = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int high = Character.digit(cleanHex.charAt(i), 16);
            int low = Character.digit(cleanHex.charAt(i + 1), 16);
            if (high == -1 || low == -1) {
                throw new BusinessException("Invalid hex character at position " + i);
            }
            result[i / 2] = (byte) ((high << 4) | low);
        }
        return result;
    }

    /**
     * 十六进制字符串转字符串
     *
     * @param hex 十六进制字符串，不能为 null
     * @return 原始字符串
     * @throws BusinessException 如果解码失败
     */
    public static String hexDecodeToString(String hex) {
        byte[] bytes = hexDecode(hex);
        return new String(bytes, CharsetEnum.UTF_8.getCharset());
    }

    // ==================== URL 编码 ====================

    /**
     * URL 编码（UTF-8）
     *
     * @param data 原始数据，不能为 null
     * @return URL 编码字符串
     * @throws BusinessException 如果编码失败
     */
    public static String urlEncode(String data) {
        Objects.requireNonNull(data, "data must not be null");
        try {
            return URLEncoder.encode(data, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new BusinessException("URL encode failed", e);
        }
    }

    /**
     * URL 解码（UTF-8）
     *
     * @param encoded URL 编码字符串，不能为 null
     * @return 解码后的原始字符串
     * @throws BusinessException 如果解码失败
     */
    public static String urlDecode(String encoded) {
        Objects.requireNonNull(encoded, "encoded must not be null");
        try {
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new BusinessException("URL decode failed", e);
        }
    }

    // ==================== 格式校验 ====================

    /**
     * 判断字符串是否为合法的 Base64 编码
     *
     * @param data 待检查的字符串
     * @return 是否为合法 Base64
     */
    public static boolean isBase64(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        try {
            Base64.getDecoder().decode(data);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为合法的十六进制编码
     *
     * @param data 待检查的字符串
     * @return 是否为合法十六进制
     */
    public static boolean isHex(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        String cleanHex = data;
        if (data.startsWith("0x") || data.startsWith("0X")) {
            cleanHex = data.substring(2);
        }
        if (cleanHex.length() % 2 != 0) {
            return false;
        }
        for (char c : cleanHex.toCharArray()) {
            if (Character.digit(c, 16) == -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否为合法的 URL 安全 Base64 编码
     *
     * @param data 待检查的字符串
     * @return 是否为合法 URL Base64
     */
    public static boolean isUrlBase64(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        try {
            Base64.getUrlDecoder().decode(data);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}