package com.dream.common.tool.utils.crypto;

import com.dream.common.core.enums.CharsetEnum;
import com.dream.common.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;

/**
 * 摘要/哈希工具类
 * <p>
 * 提供 MD5、SHA-1、SHA-256、SHA-512 等摘要算法，支持加盐、文件哈希、多次迭代等能力。
 * 内部使用 {@link ThreadLocal} 缓存 {@link MessageDigest} 实例，避免重复创建开销。
 * </p>
 * <p>
 * 核心能力：
 * <ul>
 *   <li>{@link #md5(String)} / {@link #sha256(String)} —— 标准摘要</li>
 *   <li>{@link #md5Salt(String, String)} / {@link #sha256Salt(String, String)} —— 加盐摘要</li>
 *   <li>{@link #md5Iterate(String, String, int)} —— 迭代摘要</li>
 *   <li>{@link #fileMd5(File)} / {@link #fileSha256(File)} —— 文件摘要</li>
 *   <li>{@link #generateSalt()} —— 生成随机盐值</li>
 * </ul>
 * </p>
 *
 * @author dream
 */
@Slf4j
@SuppressWarnings({"unused", "WeakerAccess"})
public final class DigestUtils {

    /** 算法名称常量 */
    private static final String MD5 = "MD5";
    private static final String SHA1 = "SHA-1";
    private static final String SHA256 = "SHA-256";
    private static final String SHA512 = "SHA-512";

    /** 默认盐值长度 */
    private static final int DEFAULT_SALT_LENGTH = 16;

    /** 文件读取缓冲区大小 */
    private static final int BUFFER_SIZE = 8192;

    /** 安全随机数生成器（用于盐值生成，禁止使用 java.util.Random） */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * ThreadLocal 缓存 MessageDigest 实例
     */
    private static final ThreadLocal<MessageDigest> MD5_DIGEST =
            ThreadLocal.withInitial(() -> getInstance(MD5));
    private static final ThreadLocal<MessageDigest> SHA1_DIGEST =
            ThreadLocal.withInitial(() -> getInstance(SHA1));
    private static final ThreadLocal<MessageDigest> SHA256_DIGEST =
            ThreadLocal.withInitial(() -> getInstance(SHA256));
    private static final ThreadLocal<MessageDigest> SHA512_DIGEST =
            ThreadLocal.withInitial(() -> getInstance(SHA512));

    private DigestUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== MD5 ====================

    public static String md5(String data) {
        Objects.requireNonNull(data, "data must not be null");
        return digest(MD5_DIGEST.get(), data.getBytes(CharsetEnum.UTF_8.getCharset()));
    }

    public static String md5(byte[] data) {
        Objects.requireNonNull(data, "data must not be null");
        return digest(MD5_DIGEST.get(), data);
    }

    public static String md5Salt(String data, String salt) {
        Objects.requireNonNull(data, "data must not be null");
        String combined = data + (salt != null ? salt : "");
        return digest(MD5_DIGEST.get(), combined.getBytes(CharsetEnum.UTF_8.getCharset()));
    }

    public static String md5Salt(byte[] data, byte[] salt) {
        Objects.requireNonNull(data, "data must not be null");
        byte[] combined = salt != null ? concat(data, salt) : data;
        return digest(MD5_DIGEST.get(), combined);
    }

    public static String md5Iterate(String data, String salt, int iterations) {
        Objects.requireNonNull(data, "data must not be null");
        if (iterations < 1) {
            iterations = 1;
        }

        byte[] hash = (data + (salt != null ? salt : "")).getBytes(CharsetEnum.UTF_8.getCharset());
        MessageDigest md = MD5_DIGEST.get();
        for (int i = 0; i < iterations; i++) {
            hash = md.digest(hash);
        }
        return toHex(hash);
    }

    // ==================== SHA-1 ====================

    public static String sha1(String data) {
        Objects.requireNonNull(data, "data must not be null");
        return digest(SHA1_DIGEST.get(), data.getBytes(CharsetEnum.UTF_8.getCharset()));
    }

    public static String sha1(byte[] data) {
        Objects.requireNonNull(data, "data must not be null");
        return digest(SHA1_DIGEST.get(), data);
    }

    public static String sha1Salt(String data, String salt) {
        Objects.requireNonNull(data, "data must not be null");
        String combined = data + (salt != null ? salt : "");
        return digest(SHA1_DIGEST.get(), combined.getBytes(CharsetEnum.UTF_8.getCharset()));
    }

    // ==================== SHA-256 ====================

    public static String sha256(String data) {
        Objects.requireNonNull(data, "data must not be null");
        return digest(SHA256_DIGEST.get(), data.getBytes(CharsetEnum.UTF_8.getCharset()));
    }

    public static String sha256(byte[] data) {
        Objects.requireNonNull(data, "data must not be null");
        return digest(SHA256_DIGEST.get(), data);
    }

    public static String sha256Salt(String data, String salt) {
        Objects.requireNonNull(data, "data must not be null");
        String combined = data + (salt != null ? salt : "");
        return digest(SHA256_DIGEST.get(), combined.getBytes(CharsetEnum.UTF_8.getCharset()));
    }

    public static String sha256Salt(byte[] data, byte[] salt) {
        Objects.requireNonNull(data, "data must not be null");
        byte[] combined = salt != null ? concat(data, salt) : data;
        return digest(SHA256_DIGEST.get(), combined);
    }

    public static String sha256Iterate(String data, String salt, int iterations) {
        Objects.requireNonNull(data, "data must not be null");
        if (iterations < 1) {
            iterations = 1;
        }

        byte[] hash = (data + (salt != null ? salt : "")).getBytes(CharsetEnum.UTF_8.getCharset());
        MessageDigest md = SHA256_DIGEST.get();
        for (int i = 0; i < iterations; i++) {
            hash = md.digest(hash);
        }
        return toHex(hash);
    }

    // ==================== SHA-512 ====================

    public static String sha512(String data) {
        Objects.requireNonNull(data, "data must not be null");
        return digest(SHA512_DIGEST.get(), data.getBytes(CharsetEnum.UTF_8.getCharset()));
    }

    public static String sha512(byte[] data) {
        Objects.requireNonNull(data, "data must not be null");
        return digest(SHA512_DIGEST.get(), data);
    }

    public static String sha512Salt(String data, String salt) {
        Objects.requireNonNull(data, "data must not be null");
        String combined = data + (salt != null ? salt : "");
        return digest(SHA512_DIGEST.get(), combined.getBytes(CharsetEnum.UTF_8.getCharset()));
    }

    // ==================== 文件摘要 ====================

    public static String fileMd5(File file) {
        return fileDigest(file, MD5_DIGEST.get());
    }

    public static String fileSha1(File file) {
        return fileDigest(file, SHA1_DIGEST.get());
    }

    public static String fileSha256(File file) {
        return fileDigest(file, SHA256_DIGEST.get());
    }

    public static String fileSha512(File file) {
        return fileDigest(file, SHA512_DIGEST.get());
    }

    public static String streamSha256(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        try {
            MessageDigest md = SHA256_DIGEST.get();
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            return toHex(md.digest());
        } catch (IOException e) {
            log.error("streamSha256 failed", e);
            throw new BusinessException("Stream digest failed", e);
        }
    }

    // ==================== 盐值生成 ====================

    public static String generateSalt() {
        return generateSalt(DEFAULT_SALT_LENGTH);
    }

    public static String generateSalt(int byteLength) {
        if (byteLength <= 0) {
            byteLength = DEFAULT_SALT_LENGTH;
        }
        byte[] saltBytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(saltBytes);
        return toHex(saltBytes);
    }

    public static String generateAlphanumericSalt(int length) {
        if (length <= 0) {
            length = 16;
        }
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ==================== 校验 ====================

    public static boolean verifyMd5(String data, String expected) {
        if (data == null || expected == null) {
            return false;
        }
        return secureEquals(expected, md5(data));
    }

    public static boolean verifyMd5Salt(String data, String salt, String expected) {
        if (data == null || expected == null) {
            return false;
        }
        return secureEquals(expected, md5Salt(data, salt));
    }

    public static boolean verifySha256Salt(String data, String salt, String expected) {
        if (data == null || expected == null) {
            return false;
        }
        return secureEquals(expected, sha256Salt(data, salt));
    }

    /**
     * 十六进制摘要的常量时间比较（忽略大小写），避免逐字节短路比较带来的时序侧信道。
     *
     * @param expected 期望摘要
     * @param actual   实际摘要
     * @return 一致返回 {@code true}
     */
    private static boolean secureEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                actual.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    // ==================== 通用摘要 ====================

    /**
     * 按指定算法计算字符串摘要（UTF-8 字节，小写十六进制输出）。
     *
     * @param algorithm 摘要算法，如 {@code MD5}、{@code SHA-256}
     * @param data      待摘要字符串
     * @return 小写十六进制摘要
     * @throws BusinessException 算法不被 JDK 支持
     */
    public static String digest(String algorithm, String data) {
        return digest(algorithm, data.getBytes(CharsetEnum.UTF_8.getCharset()));
    }

    /**
     * 按指定算法计算字节数组摘要（小写十六进制输出）。
     *
     * @param algorithm 摘要算法，如 {@code MD5}、{@code SHA-256}
     * @param data      待摘要字节数组
     * @return 小写十六进制摘要
     * @throws BusinessException 算法不被 JDK 支持
     */
    public static String digest(String algorithm, byte[] data) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        Objects.requireNonNull(data, "data must not be null");

        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            return toHex(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            log.error("Unknown algorithm: {}", algorithm, e);
            throw new BusinessException("Unknown algorithm: " + algorithm, e);
        }
    }

    public static String digestSalt(String algorithm, String data, String salt) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        Objects.requireNonNull(data, "data must not be null");

        try {
            String combined = data + (salt != null ? salt : "");
            MessageDigest md = MessageDigest.getInstance(algorithm);
            return toHex(md.digest(combined.getBytes(CharsetEnum.UTF_8.getCharset())));
        } catch (NoSuchAlgorithmException e) {
            log.error("Unknown algorithm: {}", algorithm, e);
            throw new BusinessException("Unknown algorithm: " + algorithm, e);
        }
    }

    // ==================== 内部方法 ====================

    private static MessageDigest getInstance(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to get MessageDigest for: {}", algorithm, e);
            throw new BusinessException("Unsupported algorithm: " + algorithm, e);
        }
    }

    private static String digest(MessageDigest md, byte[] data) {
        byte[] digest = md.digest(data);
        return toHex(digest);
    }

    /**
     * 字节数组转小写十六进制，统一委托 {@link CodecUtils#hexEncode(byte[])}。
     */
    private static String toHex(byte[] bytes) {
        return CodecUtils.hexEncode(bytes);
    }

    private static String fileDigest(File file, MessageDigest md) {
        Objects.requireNonNull(file, "file must not be null");

        if (!file.exists() || !file.isFile()) {
            throw new BusinessException("File does not exist or is not a file: " + file.getPath());
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            return toHex(md.digest());
        } catch (IOException e) {
            log.error("fileDigest failed: {}", file.getName(), e);
            throw new BusinessException("File digest failed: " + file.getName(), e);
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}