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
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * AES 对称加密工具类
 * <p>
 * 提供 AES-128/192/256 加密解密能力，统一采用 GCM 认证加密模式。
 * </p>
 * <p>
 * 核心能力：
 * <ul>
 *   <li>{@link #encryptGcm(String, String)} / {@link #decryptGcm(String, String)} —— AES/GCM 加解密（随机 IV，密文含 IV）</li>
 *   <li>{@link #encryptWithSalt(String, String, String)} —— 带盐值加密（PBKDF2 密钥派生 + GCM）</li>
 *   <li>{@link #generateKey()} / {@link #generateKey(int)} —— 生成随机密钥</li>
 * </ul>
 * </p>
 * <p>
 * <b>与 data 模块 {@code EncryptUtil} 的分工</b>：本类是<b>报文/配置值</b>加密入口（GCM，自带完整性校验，
 * 输出 {@code Base64(IV + 密文 + tag)}）；{@code EncryptUtil} 是<b>字段落库</b>加密入口
 * （CBC/PKCS5Padding，配合 {@code @Encrypt} 注解与 {@code dream.encrypt.*} 配置，密文格式
 * {@code Base64(IV + 密文)}，无认证 tag）。
 * </p>
 * <p>
 * 两套密文<b>格式互不兼容、不可混用</b>：把 {@code EncryptUtil} 已入库的历史密文改用本类解密会得到
 * 乱码或校验失败，反之亦然。切换存储算法必须先做数据迁移，因此两侧格式保持冻结，不要"顺手统一"。
 * 新代码若不涉及历史数据，优先使用本类（GCM 认证加密）。
 * </p>
 *
 * @author dream
 */
@Slf4j
@SuppressWarnings({"unused", "WeakerAccess"})
public final class AesUtils {

    /** AES 算法名 */
    private static final String AES = "AES";

    /** AES/GCM/NoPadding 转换 */
    private static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";

    /** GCM 标签长度（128 位） */
    private static final int GCM_TAG_LENGTH = 128;

    /** GCM IV 长度（96 位） */
    private static final int GCM_IV_LENGTH = 12;

    /** 带盐加密格式的盐值长度（字节），密文头部固定按此长度解析 */
    private static final int SALT_LENGTH = 16;

    /** 默认密钥长度 */
    private static final int DEFAULT_KEY_SIZE = 256;

    /** PBKDF2 迭代次数（OWASP 建议不低于 600,000，此处取 100,000 兼顾性能与安全） */
    private static final int PBKDF2_ITERATIONS = 100_000;

    /** 安全随机数生成器 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AesUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

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
     * 输出格式：Base64(盐值[16字节] + IV + 密文)
     * </p>
     * <p>
     * 传入的盐值不要求固定长度：非空盐统一经 SHA-256 摘要后截取前 16 字节使用，
     * 与 {@link #decryptWithSalt(String, String)} 的定长解析格式保持一致。
     * </p>
     *
     * @param data     明文数据，不能为 null
     * @param password 密码，不能为 null
     * @param salt     盐值，可为 null（自动生成随机盐）
     * @return Base64 编码的密文（含盐值和 IV）
     */
    public static String encryptWithSalt(String data, String password, String salt) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(password, "password must not be null");

        try {
            // 生成或使用提供的盐值（规范化为固定 16 字节，保证解密端可按长度定位）
            byte[] saltBytes;
            if (salt != null && !salt.isEmpty()) {
                saltBytes = Arrays.copyOf(
                        MessageDigest.getInstance("SHA-256")
                                .digest(salt.getBytes(CharsetEnum.UTF_8.getCharset())),
                        SALT_LENGTH);
            } else {
                saltBytes = new byte[SALT_LENGTH];
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

            // 提取盐值（前 16 字节，与 encryptWithSalt 的规范化格式对应）
            int saltLength = SALT_LENGTH;
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
     * <p>
     * 依次尝试：UTF-8 原始字节（长度恰为 16/24/32）→ Base64 解码（长度为 16/24/32）
     * → SHA-256 摘要派生 256 位密钥。口令类密钥请优先使用
     * {@link #encryptWithSalt(String, String, String)}（PBKDF2 派生），而非本方法。
     * </p>
     */
    private static SecretKeySpec parseKey(String key) {
        if (key.trim().isEmpty()) {
            throw new BusinessException("AES key must not be blank");
        }
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
     * 从字符串派生密钥（SHA-256 摘要截取）。
     * <p>派生失败时直接抛出 {@link BusinessException}，不做逐字节填充兜底——
     * 兜底路径会在空/异常输入时产生弱且不可复现的密钥。</p>
     */
    private static byte[] deriveKeyFromString(String key, int targetLength) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(key.getBytes(CharsetEnum.UTF_8.getCharset()));
            return Arrays.copyOf(hash, Math.min(hash.length, targetLength));
        } catch (Exception e) {
            log.error("Key derivation from string failed", e);
            throw new BusinessException("Failed to derive AES key", e);
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