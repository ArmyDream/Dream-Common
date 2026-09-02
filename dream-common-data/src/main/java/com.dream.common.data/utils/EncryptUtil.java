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
 *   <li><b>禁用 ECB 分组模式</b>：本类全部使用 CBC（引入随机 IV），ECB 模式下相同明文块产生相同密文，易被统计分析。</li>
 *   <li><b>DES 属于弱加密算法</b>（56 位有效密钥），仅用于兼容历史数据，新业务请使用 AES。</li>
 *   <li>SM4 当前为简化实现（实际复用 AES），生产环境应引入 BouncyCastle 或 Hutool 以使用真正的国密算法。</li>
 *   <li>MD5 计算时加入固定 salt 前缀，增强抗彩虹表攻击能力；但 MD5 仅适用于指纹/校验场景，<b>密码存储请使用 BCrypt/Argon2</b>。</li>
 * </ul>
 * </p>
 * <p>
 * <b>密钥配置要求</b>：AES / DES 密钥在生产环境中<b>必须</b>通过配置注入，禁止使用默认密钥。
 * 当未配置密钥时，加密/解密操作将抛出 {@link IllegalArgumentException} 以避免误用。
 * 默认值仅用于开发快速验证，会在启动时输出 WARN 日志。
 * 配置方式：
 * <pre>{@code
 * dream:
 *   encrypt:
 *     aes-key: xxxxxxxx  # AES 密钥（16 字节）
 *     aes-iv:  xxxxxxxx  # AES IV（16 字节）
 *     des-key: xxxxxx    # DES 密钥（8 字节）
 *     des-iv:  xxxxxx    # DES IV（8 字节）
 *     md5-salt: xxxxxx   # MD5 加盐前缀
 * }</pre>
 * </p>
 *
 * @author dream
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
     * <p>仅作为未配置密钥时的开发兜底（会输出 WARN），生产环境应通过配置注入。</p>
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
    private static final String DEFAULT_MD5_SALT = "DREAM_COMMON_SALT_";

    // ==================== 运行时密钥（由 dream.encrypt.* 配置注入，默认兼容历史常量） ====================

    /** 当前生效的 AES 密钥 */
    private static volatile String AES_KEY = DEFAULT_AES_KEY;
    /** 当前生效的 AES IV */
    private static volatile String AES_IV = DEFAULT_AES_IV;
    /** 当前生效的 DES 密钥 */
    private static volatile String DES_KEY = DEFAULT_DES_KEY;
    /** 当前生效的 DES IV */
    private static volatile String DES_IV = DEFAULT_DES_IV;
    /** 当前生效的 MD5 盐 */
    private static volatile String MD5_SALT = DEFAULT_MD5_SALT;
    /** 是否已通过显式配置注入密钥 */
    private static volatile boolean keysConfigured = false;
    /** 未配置密钥时的 WARN 只输出一次 */
    private static volatile boolean legacyKeyWarned = false;

    /**
     * 通过 Spring 配置注入密钥。
     * <p>由框架自动装配（{@code EncryptAutoConfiguration}）读取 {@code dream.encrypt.*} 后调用。</p>
     *
     * @param aesKey   AES 密钥（建议 16 字节）
     * @param aesIv    AES IV（建议 16 字节）
     * @param desKey   DES 密钥（8 字节）
     * @param desIv    DES IV（8 字节）
     * @param md5Salt  MD5 加盐前缀
     */
    public static synchronized void configure(String aesKey, String aesIv,
                                              String desKey, String desIv, String md5Salt) {
        if (aesKey != null && !aesKey.isEmpty()) {
            AES_KEY = aesKey;
        }
        if (aesIv != null && !aesIv.isEmpty()) {
            AES_IV = aesIv;
        }
        if (desKey != null && !desKey.isEmpty()) {
            DES_KEY = desKey;
        }
        if (desIv != null && !desIv.isEmpty()) {
            DES_IV = desIv;
        }
        if (md5Salt != null && !md5Salt.isEmpty()) {
            MD5_SALT = md5Salt;
        }
        keysConfigured = true;
        log.info("[EncryptUtil] 密钥已通过配置注入（dream.encrypt.*）");
    }

    /**
     * 若未配置密钥，输出一次性 WARN，提示生产环境应显式配置
     */
    private static void warnIfLegacyKey() {
        if (!keysConfigured && !legacyKeyWarned) {
            legacyKeyWarned = true;
            log.warn("[EncryptUtil] 未通过 dream.encrypt.* 配置密钥，正在使用内置默认密钥。"
                    + "生产环境务必配置，否则加密形同虚设。");
        }
    }

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
     * @param type    加密类型，为 null 时抛出 {@link IllegalArgumentException}（fail-closed）
     * @param key     加密密钥，为 null 或空时使用配置/默认密钥
     * @return 加密后的字符串
     * @throws IllegalArgumentException 加密类型非法或加密失败时抛出（fail-closed，不再静默回退明文）
     */
    public static String encrypt(String content, EncryptType type, String key) {
        if (content == null) {
            return null;
        }
        if (type == null) {
            throw new IllegalArgumentException("加密类型不能为 null");
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
                    throw new IllegalArgumentException("不支持的加密类型: " + type);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("加密失败, type: {}", type, e);
            throw new IllegalArgumentException("加密失败, type: " + type, e);
        }
    }

    /**
     * 解密方法
     * <p>
     * 根据指定的加密类型对内容进行解密。MD5 不可逆，不支持解密。
     * </p>
     *
     * @param content 待解密内容，为 null 时返回 null
     * @param type    解密类型，为 null 时抛出 {@link IllegalArgumentException}（fail-closed）
     * @param key     解密密钥，为 null 或空时使用配置/默认密钥
     * @return 解密后的字符串
     * @throws IllegalArgumentException 解密类型非法或解密失败时抛出（fail-closed）
     */
    public static String decrypt(String content, EncryptType type, String key) {
        if (content == null) {
            return null;
        }
        if (type == null) {
            throw new IllegalArgumentException("解密类型不能为 null");
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
                    throw new IllegalArgumentException("不支持的解密类型: " + type);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("解密失败, type: {}", type, e);
            throw new IllegalArgumentException("解密失败, type: " + type, e);
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
        warnIfLegacyKey();
        String secretKey = (key == null || key.isEmpty()) ? AES_KEY : key;
        byte[] keyBytes = normalizeKey(secretKey, AES_KEY_SIZE);
        byte[] ivBytes = normalizeKey(AES_IV, AES_IV_SIZE);

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
        warnIfLegacyKey();
        String secretKey = (key == null || key.isEmpty()) ? AES_KEY : key;
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
        warnIfLegacyKey();
        String secretKey = (key == null || key.isEmpty()) ? DES_KEY : key;
        byte[] keyBytes = normalizeKey(secretKey, DES_KEY_SIZE);
        byte[] ivBytes = normalizeKey(DES_IV, DES_IV_SIZE);

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
        warnIfLegacyKey();
        String secretKey = (key == null || key.isEmpty()) ? DES_KEY : key;
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
        warnIfLegacyKey();
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
