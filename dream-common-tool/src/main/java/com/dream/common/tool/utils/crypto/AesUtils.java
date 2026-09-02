package com.dream.common.tool.utils.crypto;

import com.dream.common.core.enums.CharsetEnum;
import com.dream.common.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * AES 对称加密工具类
 * <p>
 * 提供 AES-128/192/256 加密解密能力，支持 ECB 和 GCM 两种模式。
 * <b>推荐使用 GCM 模式（认证加密，安全性更高）</b>；ECB 方法已标记过时，仅供兼容历史数据。
 * </p>
 * <p>
 * 核心能力：
 * <ul>
 *   <li>{@link #encrypt(String, String)} / {@link #decrypt(String, String)} —— AES/ECB 加解密（<b>已过时</b>，仅兼容历史数据）</li>
 *   <li>{@link #encryptGcm(String, String)} / {@link #decryptGcm(String, String)} —— AES/GCM 加解密（推荐）</li>
 *   <li>{@link #encryptWithSalt(String, String, String)} —— 带盐值加密（PBKDF2 密钥派生 + GCM）</li>
 *   <li>{@link #generateKey()} / {@link #generateKey(int)} —— 生成随机密钥</li>
 * </ul>
 * </p>
 *
 * @author dream
 */
@Slf4j
@SuppressWarnings({"unused", "WeakerAccess"})
public final class AesUtils {

    /** AES 算法名 */
    private static final String AES = "AES";

    /** AES/ECB/PKCS5Padding 转换 */
    private static final String AES_ECB_PKCS5 = "AES/ECB/PKCS5Padding";

    /** AES/GCM/NoPadding 转换 */
    private static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";

    /** GCM 标签长度（128 位） */
    private static final int GCM_TAG_LENGTH = 128;

    /** GCM IV 长度（96 位） */
    private static final int GCM_IV_LENGTH = 12;

    /** 默认密钥长度 */
    private static final int DEFAULT_KEY_SIZE = 256;

    /** PBKDF2 迭代次数（OWASP 建议不低于 600,000，此处取 100,000 兼顾性能与安全） */
    private static final int PBKDF2_ITERATIONS = 100_000;

    /** 安全随机数生成器 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AesUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== AES/ECB 加密解密 ====================

    /**
     * AES/ECB 加密（字符串 → Base64）
     * <p>
     * <b>已过时</b>：ECB 分组模式下相同明文块产生相同密文，明文结构（长度、重复块）可被统计分析，
     * 仅用于兼容历史数据；新业务请使用 {@link #encryptGcm(String, String)}。
     * </p>
     *
     * @param data 明文数据，不能为 null
     * @param key  密钥（16/24/32 字节，对应 AES-128/192/256）
     * @return Base64 编码的密文
     * @throws BusinessException 如果加密失败
     * @deprecated 使用存在安全隐患的 ECB 模式，请改用 {@link #encryptGcm(String, String)}
     */
    @Deprecated
    public static String encrypt(String data, String key) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(key, "key must not be null");

        try {
            SecretKeySpec keySpec = parseKey(key);
            Cipher cipher = Cipher.getInstance(AES_ECB_PKCS5);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data.getBytes(CharsetEnum.UTF_8.getCharset()));
            log.debug("AES/ECB encryption completed");
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("AES/ECB encrypt failed", e);
            throw new BusinessException("AES/ECB encrypt failed", e);
        }
    }

    /**
     * AES/ECB 解密（Base64 → 字符串）
     * <p>
     * <b>已过时</b>：ECB 模式存在安全隐患，仅用于解密历史密文；新业务请使用 {@link #decryptGcm(String, String)}。
     * </p>
     *
     * @param encryptedData Base64 编码的密文，不能为 null
     * @param key           密钥（16/24/32 字节）
     * @return 解密后的明文
     * @throws BusinessException 如果解密失败
     * @deprecated 使用存在安全隐患的 ECB 模式，请改用 {@link #decryptGcm(String, String)}
     */
    @Deprecated
    public static String decrypt(String encryptedData, String key) {
        Objects.requireNonNull(encryptedData, "encryptedData must not be null");
        Objects.requireNonNull(key, "key must not be null");

        try {
            SecretKeySpec keySpec = parseKey(key);
            Cipher cipher = Cipher.getInstance(AES_ECB_PKCS5);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            log.debug("AES/ECB decryption completed");
            return new String(decrypted, CharsetEnum.UTF_8.getCharset());
        } catch (Exception e) {
            log.error("AES/ECB decrypt failed", e);
            throw new BusinessException("AES/ECB decrypt failed", e);
        }
    }

    /**
     * AES/ECB 加密（字节数组）
     * <p>
     * <b>已过时</b>：ECB 模式存在安全隐患，仅用于兼容历史数据；新业务请使用 {@link #encryptGcm(byte[], byte[])}。
     * </p>
     *
     * @param data 明文数据
     * @param key  密钥字节数组（16/24/32 字节）
     * @return 密文字节数组
     * @throws BusinessException 如果加密失败
     * @deprecated 使用存在安全隐患的 ECB 模式，请改用 {@link #encryptGcm(byte[], byte[])}
     */
    @Deprecated
    public static byte[] encrypt(byte[] data, byte[] key) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(key, "key must not be null");

        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, AES);
            Cipher cipher = Cipher.getInstance(AES_ECB_PKCS5);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            log.error("AES/ECB encrypt (bytes) failed", e);
            throw new BusinessException("AES/ECB encrypt failed", e);
        }
    }

    /**
     * AES/ECB 解密（字节数组）
     * <p>
     * <b>已过时</b>：ECB 模式存在安全隐患，仅用于解密历史密文；新业务请使用 {@link #decryptGcm(byte[], byte[])}。
     * </p>
     *
     * @param encryptedData 密文字节数组
     * @param key           密钥字节数组（16/24/32 字节）
     * @return 明文字节数组
     * @throws BusinessException 如果解密失败
     * @deprecated 使用存在安全隐患的 ECB 模式，请改用 {@link #decryptGcm(byte[], byte[])}
     */
    @Deprecated
    public static byte[] decrypt(byte[] encryptedData, byte[] key) {
        Objects.requireNonNull(encryptedData, "encryptedData must not be null");
        Objects.requireNonNull(key, "key must not be null");

        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, AES);
            Cipher cipher = Cipher.getInstance(AES_ECB_PKCS5);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            log.error("AES/ECB decrypt (bytes) failed", e);
            throw new BusinessException("AES/ECB decrypt failed", e);
        }
    }

    // ==================== AES/GCM 加密解密（推荐） ====================

    /**
     * AES/GCM 加密（字符串 → Base64）
     * <p>
     * 每次加密使用随机 IV，输出格式：Base64(IV + 密文)
     * </p>
     *
     * @param data 明文数据，不能为 null
     * @param key  密钥（16/24/32 字节）
     * @return Base64 编码的密文（含 IV）
     * @throws BusinessException 如果加密失败
     */
    public static String encryptGcm(String data, String key) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(key, "key must not be null");

        try {
            SecretKeySpec keySpec = parseKey(key);
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes(CharsetEnum.UTF_8.getCharset()));

            // IV + 密文拼接
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);

            log.debug("AES/GCM encryption completed");
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("AES/GCM encrypt failed", e);
            throw new BusinessException("AES/GCM encrypt failed", e);
        }
    }

    /**
     * AES/GCM 解密（Base64 → 字符串）
     *
     * @param encryptedData Base64 编码的密文（含 IV），不能为 null
     * @param key           密钥（16/24/32 字节）
     * @return 解密后的明文
     * @throws BusinessException 如果解密失败
     */
    public static String decryptGcm(String encryptedData, String key) {
        Objects.requireNonNull(encryptedData, "encryptedData must not be null");
        Objects.requireNonNull(key, "key must not be null");

        try {
            SecretKeySpec keySpec = parseKey(key);
            byte[] decoded = Base64.getDecoder().decode(encryptedData);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] decrypted = cipher.doFinal(cipherText);
            log.debug("AES/GCM decryption completed");
            return new String(decrypted, CharsetEnum.UTF_8.getCharset());
        } catch (Exception e) {
            log.error("AES/GCM decrypt failed", e);
            throw new BusinessException("AES/GCM decrypt failed", e);
        }
    }

    /**
     * AES/GCM 加密（字节数组）
     *
     * @param data 明文数据
     * @param key  密钥字节数组（16/24/32 字节）
     * @return 密文字节数组（IV + 密文）
     */
    public static byte[] encryptGcm(byte[] data, byte[] key) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(key, "key must not be null");

        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, AES);
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] encrypted = cipher.doFinal(data);

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);
            return byteBuffer.array();
        } catch (Exception e) {
            log.error("AES/GCM encrypt (bytes) failed", e);
            throw new BusinessException("AES/GCM encrypt failed", e);
        }
    }

    /**
     * AES/GCM 解密（字节数组）
     *
     * @param encryptedData 密文字节数组（IV + 密文）
     * @param key           密钥字节数组（16/24/32 字节）
     * @return 明文字节数组
     */
    public static byte[] decryptGcm(byte[] encryptedData, byte[] key) {
        Objects.requireNonNull(encryptedData, "encryptedData must not be null");
        Objects.requireNonNull(key, "key must not be null");

        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, AES);

            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            log.error("AES/GCM decrypt (bytes) failed", e);
            throw new BusinessException("AES/GCM decrypt failed", e);
        }
    }

    // ==================== 带盐值加密（密码派生） ====================

    /**
     * 使用密码+盐值加密（AES-GCM）
     * <p>
     * 从密码和盐值派生 AES 密钥，然后使用 GCM 模式加密。
     * 输出格式：Base64(盐值 + IV + 密文)
     * </p>
     *
     * @param data     明文数据，不能为 null
     * @param password 密码，不能为 null
     * @param salt     盐值，可为 null（自动生成）
     * @return Base64 编码的密文（含盐值和 IV）
     */
    public static String encryptWithSalt(String data, String password, String salt) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(password, "password must not be null");

        try {
            // 生成或使用提供的盐值
            byte[] saltBytes;
            if (salt != null && !salt.isEmpty()) {
                saltBytes = salt.getBytes(CharsetEnum.UTF_8.getCharset());
            } else {
                saltBytes = new byte[16];
                SECURE_RANDOM.nextBytes(saltBytes);
            }

            // 从密码派生密钥
            byte[] keyBytes = deriveKey(password, saltBytes);

            // 使用派生密钥加密
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES);
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes(CharsetEnum.UTF_8.getCharset()));

            // 盐值 + IV + 密文
            ByteBuffer byteBuffer = ByteBuffer.allocate(saltBytes.length + iv.length + encrypted.length);
            byteBuffer.put(saltBytes);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);

            log.debug("AES-GCM with salt encryption completed");
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("AES-GCM with salt encrypt failed", e);
            throw new BusinessException("AES-GCM with salt encrypt failed", e);
        }
    }

    /**
     * 使用密码+盐值解密（AES-GCM）
     *
     * @param encryptedData Base64 编码的密文（含盐值和 IV），不能为 null
     * @param password      密码，不能为 null
     * @return 解密后的明文
     */
    public static String decryptWithSalt(String encryptedData, String password) {
        Objects.requireNonNull(encryptedData, "encryptedData must not be null");
        Objects.requireNonNull(password, "password must not be null");

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedData);

            // 提取盐值（前16字节）
            int saltLength = 16;
            if (decoded.length < saltLength + GCM_IV_LENGTH) {
                throw new BusinessException("Invalid encrypted data format");
            }

            byte[] saltBytes = new byte[saltLength];
            System.arraycopy(decoded, 0, saltBytes, 0, saltLength);

            // 提取 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decoded, saltLength, iv, 0, GCM_IV_LENGTH);

            // 提取密文
            byte[] cipherText = new byte[decoded.length - saltLength - GCM_IV_LENGTH];
            System.arraycopy(decoded, saltLength + GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            // 派生密钥
            byte[] keyBytes = deriveKey(password, saltBytes);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES);

            // 解密
            Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] decrypted = cipher.doFinal(cipherText);
            log.debug("AES-GCM with salt decryption completed");
            return new String(decrypted, CharsetEnum.UTF_8.getCharset());
        } catch (Exception e) {
            log.error("AES-GCM with salt decrypt failed", e);
            throw new BusinessException("AES-GCM with salt decrypt failed", e);
        }
    }

    // ==================== 密钥生成 ====================

    /**
     * 生成随机 AES 密钥（Base64 编码）
     *
     * @return Base64 编码的密钥字符串
     */
    public static String generateKey() {
        return generateKey(DEFAULT_KEY_SIZE);
    }

    /**
     * 生成指定长度的 AES 密钥（Base64 编码）
     *
     * @param keySize 密钥位数（128/192/256）
     * @return Base64 编码的密钥字符串
     */
    public static String generateKey(int keySize) {
        if (keySize != 128 && keySize != 192 && keySize != 256) {
            log.warn("Invalid AES key size: {}, using 256", keySize);
            keySize = 256;
        }
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(AES);
            keyGen.init(keySize, SECURE_RANDOM);
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            log.error("generateKey failed", e);
            throw new BusinessException("Failed to generate AES key", e);
        }
    }

    /**
     * 生成随机 AES 密钥（字节数组）
     *
     * @param keySize 密钥位数（128/192/256）
     * @return 密钥字节数组
     */
    public static byte[] generateKeyBytes(int keySize) {
        if (keySize != 128 && keySize != 192 && keySize != 256) {
            keySize = 256;
        }
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(AES);
            keyGen.init(keySize, SECURE_RANDOM);
            return keyGen.generateKey().getEncoded();
        } catch (Exception e) {
            log.error("generateKeyBytes failed", e);
            throw new BusinessException("Failed to generate AES key", e);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 从字符串密钥解析为 SecretKeySpec
     */
    private static SecretKeySpec parseKey(String key) {
        byte[] keyBytes;
        byte[] rawBytes = key.getBytes(CharsetEnum.UTF_8.getCharset());

        // 检查是否为有效的密钥长度
        if (rawBytes.length == 16 || rawBytes.length == 24 || rawBytes.length == 32) {
            keyBytes = rawBytes;
        } else {
            // 尝试 Base64 解码
            try {
                keyBytes = Base64.getDecoder().decode(key);
                if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                    // 使用 SHA-256 派生固定长度密钥
                    keyBytes = deriveKeyFromString(key, 32);
                }
            } catch (Exception e) {
                // 使用 SHA-256 派生固定长度密钥
                keyBytes = deriveKeyFromString(key, 32);
            }
        }
        return new SecretKeySpec(keyBytes, AES);
    }

    /**
     * 从字符串派生密钥
     */
    private static byte[] deriveKeyFromString(String key, int targetLength) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(key.getBytes(CharsetEnum.UTF_8.getCharset()));
            byte[] result = new byte[targetLength];
            System.arraycopy(hash, 0, result, 0, Math.min(hash.length, targetLength));
            return result;
        } catch (Exception e) {
            // 降级方案：截取或填充
            byte[] bytes = key.getBytes(CharsetEnum.UTF_8.getCharset());
            byte[] result = new byte[targetLength];
            for (int i = 0; i < targetLength; i++) {
                result[i] = bytes[i % bytes.length];
            }
            return result;
        }
    }

    /**
     * 从密码和盐值派生密钥（PBKDF2WithHmacSHA256）
     * <p>
     * 使用标准 PBKDF2（HMAC-SHA256）派生 256 位 AES 密钥，
     * 迭代次数为 {@link #PBKDF2_ITERATIONS}，抵御暴力猜测与彩虹表攻击。
     * </p>
     */
    private static byte[] deriveKey(String password, byte[] salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256);
            byte[] key = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return key;
        } catch (Exception e) {
            log.error("Key derivation failed", e);
            throw new BusinessException("Key derivation failed", e);
        }
    }
}