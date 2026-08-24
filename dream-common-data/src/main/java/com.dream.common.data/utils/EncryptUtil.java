package com.dream.common.data.utils;

import com.dream.common.data.enums.EncryptType;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 加密解密工具类
 * <p>
 * 提供 AES、DES、SM4（简化实现）、Base64、MD5 等常用加密解密能力。
 * </p>
 * <p>
 * 安全说明：
 * <ul>
 *   <li>AES 采用 {@code AES/CBC/PKCS5Padding} 模式，加密结果格式为 {@code Base64(IV + 密文)}。</li>
 *   <li>DES 采用 {@code DES/CBC/PKCS5Padding} 模式，加密结果格式为 {@code Base64(IV + 密文)}。</li>
 *   <li>SM4 当前为简化实现（实际复用 AES），生产环境应引入 BouncyCastle 或 Hutool 以使用真正的国密算法。</li>
 *   <li>MD5 计算时加入固定 salt 前缀，增强抗彩虹表攻击能力。</li>
 * </ul>
 * </p>
 * <p>
 * 注意：本类中默认密钥、IV、salt 仅为开发演示使用，
 * <b>生产环境应通过外部配置（如 application.yml）注入</b>，避免硬编码带来的安全风险。
 * </p>
 */
@Slf4j
public class EncryptUtil {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String DES_ALGORITHM = "DES";
    private static final String DES_CIPHER_ALGORITHM = "DES/CBC/PKCS5Padding";
    private static final String MD5_ALGORITHM = "MD5";
    private static final String SM4_ALGORITHM = "SM4";

    /** AES 密钥字节数（AES-128） */
    private static final int AES_KEY_SIZE = 16;
    /** AES IV 字节数 */
    private static final int AES_IV_SIZE = 16;
    /** DES 密钥字节数 */
    private static final int DES_KEY_SIZE = 8;
    /** DES IV 字节数 */
    private static final int DES_IV_SIZE = 8;

    /**
     * 默认 AES 密钥
     * <p>生产环境应通过配置注入，请勿直接使用默认值。</p>
     */
    private static final String DEFAULT_AES_KEY = "DEFAULT_AES_KEY_16";

    /**
     * 默认 AES IV（16 字节）
     * <p>生产环境应通过配置注入，请勿直接使用默认值。</p>
     */
    private static final String DEFAULT_AES_IV = "DEFAULT_AES_IV_16";

    /**
     * 默认 DES 密钥（8 位）
     * <p>生产环境应通过配置注入，请勿直接使用默认值。</p>
     */
    private static final String DEFAULT_DES_KEY = "DES_KEY8";

    /**
     * 默认 DES IV（8 字节）
     * <p>生产环境应通过配置注入，请勿直接使用默认值。</p>
     */
    private static final String DEFAULT_DES_IV = "DES_IV_8";

    /**
     * MD5 加盐前缀，用于增强 MD5 摘要的安全性
     * <p>生产环境应通过配置注入。</p>
     */
    private static final String MD5_SALT = "DREAM_COMMON_SALT_";

    /**
     * 私有构造方法，防止实例化
     */
    private EncryptUtil() {
    }

    /**
     * 加密方法
     * <p>
     * 根据指定的加密类型对内容进行加密：
     * <ul>
     *   <li>{@link EncryptType#AES}：AES/CBC/PKCS5Padding，输出 Base64(IV + 密文)</li>
     *   <li>{@link EncryptType#DES}：DES/CBC/PKCS5Padding，输出 Base64(IV + 密文)</li>
     *   <li>{@link EncryptType#SM4}：简化实现，复用 AES</li>
     *   <li>{@link EncryptType#BASE64}：Base64 编码</li>
     *   <li>{@link EncryptType#MD5}：加盐 MD5 摘要（不可逆）</li>
     * </ul>
     * </p>
     *
     * @param content 待加密内容，为 null 时返回 null
     * @param type    加密类型，为 null 时原样返回
     * @param key     加密密钥，为 null 或空时使用对应算法的默认密钥
     * @return 加密后的字符串；加密失败时原样返回 content
     */
    public static String encrypt(String content, EncryptType type, String key) {
        if (content == null) {
            return null;
        }
        if (type == null) {
            return content;
        }

        try {
            switch (type) {
                case AES:
                    return aesEncrypt(content, key);
                case DES:
                    return desEncrypt(content, key);
                case SM4:
                    return sm4Encrypt(content, key);
                case BASE64:
                    return base64Encode(content);
                case MD5:
                    return md5Encrypt(content);
                default:
                    return content;
            }
        } catch (Exception e) {
            log.error("加密失败, type: {}, content: {}", type, content, e);
            return content;
        }
    }

    /**
     * 解密方法
     * <p>
     * 根据指定的加密类型对内容进行解密。MD5 不可逆，不支持解密。
     * </p>
     *
     * @param content 待解密内容，为 null 时返回 null
     * @param type    加密类型，为 null 时原样返回
     * @param key     解密密钥，为 null 或空时使用对应算法的默认密钥
     * @return 解密后的字符串；解密失败时原样返回 content
     */
    public static String decrypt(String content, EncryptType type, String key) {
        if (content == null) {
            return null;
        }
        if (type == null) {
            return content;
        }

        try {
            switch (type) {
                case AES:
                    return aesDecrypt(content, key);
                case DES:
                    return desDecrypt(content, key);
                case SM4:
                    return sm4Decrypt(content, key);
                case BASE64:
                    return base64Decode(content);
                default:
                    return content;
            }
        } catch (Exception e) {
            log.error("解密失败, type: {}, content: {}", type, content, e);
            return content;
        }
    }

    /**
     * AES 加密
     * <p>
     * 采用 {@code AES/CBC/PKCS5Padding} 模式，使用 16 字节 IV 向量。
     * 加密结果格式：{@code Base64(IV + 密文)}，解密时从中分离 IV 与密文。
     * </p>
     *
     * @param content 待加密内容
     * @param key     加密密钥，为空时使用默认密钥；不足 16 位自动填充，超过 16 位截断
     * @return Base64 编码的加密结果（含 IV 前缀）
     * @throws Exception 加密失败时抛出
     */
    private static String aesEncrypt(String content, String key) throws Exception {
        String secretKey = (key == null || key.isEmpty()) ? DEFAULT_AES_KEY : key;
        byte[] keyBytes = normalizeKey(secretKey, AES_KEY_SIZE);
        byte[] ivBytes = normalizeKey(DEFAULT_AES_IV, AES_IV_SIZE);

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES_ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encryptedBytes = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));

        // 拼接 IV + 密文后统一 Base64 编码
        byte[] output = new byte[AES_IV_SIZE + encryptedBytes.length];
        System.arraycopy(ivBytes, 0, output, 0, AES_IV_SIZE);
        System.arraycopy(encryptedBytes, 0, output, AES_IV_SIZE, encryptedBytes.length);
        return Base64.getEncoder().encodeToString(output);
    }

    /**
     * AES 解密
     * <p>
     * 采用 {@code AES/CBC/PKCS5Padding} 模式，从 Base64 解码后分离 IV 与密文。
     * </p>
     *
     * @param content Base64 编码的加密结果（含 IV 前缀）
     * @param key     解密密钥，为空时使用默认密钥；不足 16 位自动填充，超过 16 位截断
     * @return 解密后的明文
     * @throws Exception 解密失败时抛出
     */
    private static String aesDecrypt(String content, String key) throws Exception {
        String secretKey = (key == null || key.isEmpty()) ? DEFAULT_AES_KEY : key;
        byte[] keyBytes = normalizeKey(secretKey, AES_KEY_SIZE);

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES_ALGORITHM);

        byte[] decodedBytes = Base64.getDecoder().decode(content);
        // 分离 IV 与密文
        byte[] iv = new byte[AES_IV_SIZE];
        byte[] encryptedBytes = new byte[decodedBytes.length - AES_IV_SIZE];
        System.arraycopy(decodedBytes, 0, iv, 0, AES_IV_SIZE);
        System.arraycopy(decodedBytes, AES_IV_SIZE, encryptedBytes, 0, encryptedBytes.length);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * DES 加密
     * <p>
     * 采用 {@code DES/CBC/PKCS5Padding} 模式，使用 8 字节 IV 向量。
     * 加密结果格式：{@code Base64(IV + 密文)}，解密时从中分离 IV 与密文。
     * </p>
     *
     * @param content 待加密内容
     * @param key     加密密钥，为空时使用默认密钥；不足 8 位自动填充，超过 8 位截断
     * @return Base64 编码的加密结果（含 IV 前缀）
     * @throws Exception 加密失败时抛出
     */
    private static String desEncrypt(String content, String key) throws Exception {
        String secretKey = (key == null || key.isEmpty()) ? DEFAULT_DES_KEY : key;
        byte[] keyBytes = normalizeKey(secretKey, DES_KEY_SIZE);
        byte[] ivBytes = normalizeKey(DEFAULT_DES_IV, DES_IV_SIZE);

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, DES_ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance(DES_CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encryptedBytes = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));

        // 拼接 IV + 密文后统一 Base64 编码
        byte[] output = new byte[DES_IV_SIZE + encryptedBytes.length];
        System.arraycopy(ivBytes, 0, output, 0, DES_IV_SIZE);
        System.arraycopy(encryptedBytes, 0, output, DES_IV_SIZE, encryptedBytes.length);
        return Base64.getEncoder().encodeToString(output);
    }

    /**
     * DES 解密
     * <p>
     * 采用 {@code DES/CBC/PKCS5Padding} 模式，从 Base64 解码后分离 IV 与密文。
     * </p>
     *
     * @param content Base64 编码的加密结果（含 IV 前缀）
     * @param key     解密密钥，为空时使用默认密钥；不足 8 位自动填充，超过 8 位截断
     * @return 解密后的明文
     * @throws Exception 解密失败时抛出
     */
    private static String desDecrypt(String content, String key) throws Exception {
        String secretKey = (key == null || key.isEmpty()) ? DEFAULT_DES_KEY : key;
        byte[] keyBytes = normalizeKey(secretKey, DES_KEY_SIZE);

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, DES_ALGORITHM);

        byte[] decodedBytes = Base64.getDecoder().decode(content);
        // 分离 IV 与密文
        byte[] iv = new byte[DES_IV_SIZE];
        byte[] encryptedBytes = new byte[decodedBytes.length - DES_IV_SIZE];
        System.arraycopy(decodedBytes, 0, iv, 0, DES_IV_SIZE);
        System.arraycopy(decodedBytes, DES_IV_SIZE, encryptedBytes, 0, encryptedBytes.length);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance(DES_CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * SM4 加密（简化实现）
     * <p>
     * 当前简化复用 {@code AES/CBC/PKCS5Padding} 实现，<b>并非真正的国密 SM4 算法</b>。
     * </p>
     * <p>
     * 生产环境应引入 BouncyCastle 或 Hutool 以使用真正的国密 SM4 算法，例如：
     * <pre>
     *   // 引入依赖：cn.hutool:hutool-crypto 或 org.bouncycastle:bcprov-jdk18on
     *   SymmetricCrypto sm4 = new SymmetricCrypto("SM4");
     *   String encrypted = sm4.encryptHex(content);
     *   String decrypted = sm4.decryptStr(encrypted, CharsetUtil.CHARSET_UTF_8);
     * </pre>
     * 或使用 BouncyCastle：
     * <pre>
     *   BouncyCastleProvider provider = new BouncyCastleProvider();
     *   Cipher cipher = Cipher.getInstance("SM4/CBC/PKCS5Padding", provider);
     * </pre>
     * </p>
     *
     * @param content 待加密内容
     * @param key     加密密钥
     * @return Base64 编码的加密结果
     * @throws Exception 加密失败时抛出
     */
    private static String sm4Encrypt(String content, String key) throws Exception {
        // 简化处理：复用 AES 实现。实际应使用 BouncyCastle 或 Hutool 提供的国密 SM4 实现。
        return aesEncrypt(content, key);
    }

    /**
     * SM4 解密（简化实现）
     * <p>
     * 当前简化复用 {@code AES/CBC/PKCS5Padding} 实现，<b>并非真正的国密 SM4 算法</b>。
     * 生产环境应引入 BouncyCastle 或 Hutool，详见 {@link #sm4Encrypt(String, String)} 的说明。
     * </p>
     *
     * @param content Base64 编码的加密结果
     * @param key     解密密钥
     * @return 解密后的明文
     * @throws Exception 解密失败时抛出
     */
    private static String sm4Decrypt(String content, String key) throws Exception {
        // 简化处理：复用 AES 实现。实际应使用 BouncyCastle 或 Hutool 提供的国密 SM4 实现。
        return aesDecrypt(content, key);
    }

    /**
     * Base64 编码
     *
     * @param content 待编码内容
     * @return Base64 编码后的字符串
     */
    private static String base64Encode(String content) {
        return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64 解码
     *
     * @param content Base64 编码的字符串
     * @return 解码后的原始字符串
     */
    private static String base64Decode(String content) {
        byte[] decodedBytes = Base64.getDecoder().decode(content);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    /**
     * MD5 加密（不可逆）
     * <p>
     * 计算时在原始内容前加入固定 salt 前缀，以增强抗彩虹表攻击能力。
     * 注意：MD5 本身已不推荐用于密码存储等高安全场景，建议改用 BCrypt/Argon2。
     * </p>
     *
     * @param content 待摘要内容
     * @return 32 位小写十六进制 MD5 摘要字符串
     * @throws Exception 摘要计算失败时抛出
     */
    private static String md5Encrypt(String content) throws Exception {
        MessageDigest md = MessageDigest.getInstance(MD5_ALGORITHM);
        // 加入固定 salt 前缀，增强安全性
        byte[] digest = md.digest((MD5_SALT + content).getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /**
     * 规范化密钥/IV 字节数组
     * <p>
     * 当输入字节数不足目标长度时，使用 {@code '0'} 字符在尾部填充；
     * 当超过目标长度时，截取前 {@code size} 个字节。
     * </p>
     *
     * @param value 原始字符串
     * @param size  目标字节数
     * @return 长度恰为 size 的字节数组
     */
    private static byte[] normalizeKey(String value, int size) {
        byte[] src = value.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[size];
        int len = Math.min(src.length, size);
        System.arraycopy(src, 0, result, 0, len);
        for (int i = len; i < size; i++) {
            result[i] = (byte) '0';
        }
        return result;
    }
}
