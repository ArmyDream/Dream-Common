package com.dream.common.data.utils;

import com.dream.common.data.enums.EncryptType;
import com.dream.common.tool.utils.crypto.CodecUtils;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密解密工具类
 * <p>
 * 提供 AES、DES、SM4（占位实现）、Base64、MD5 等常用加密解密能力。
 * </p>
 * <p>
 * 安全说明：
 * <ul>
 *   <li>AES 采用 {@code AES/CBC/PKCS5Padding} 模式，加密结果格式为 {@code Base64(IV + 密文)}。</li>
 *   <li>DES 采用 {@code DES/CBC/PKCS5Padding} 模式，加密结果格式为 {@code Base64(IV + 密文)}。</li>
 *   <li><b>禁用 ECB 分组模式</b>：本类全部使用 CBC 模式，密文头部携带 IV 以便解密。</li>
 *   <li><b>IV 每次加密随机生成</b>（{@link SecureRandom}）：固定 IV 会让同一明文产生同一密文，
 *       等值关系直接泄露（可判断两条记录的手机号相同）。历史密文的 IV 一直存放在密文头部，
 *       因此本变更不影响旧数据的解密。</li>
 *   <li><b>DES 属于弱加密算法</b>（56 位有效密钥），仅用于兼容历史数据，新业务请使用 AES。</li>
 *   <li><b>SM4 为占位实现（实际复用 AES），并非国密算法</b>：首次使用会输出 WARN。
 *       需要国密合规时请引入 BouncyCastle 后替换实现；若库中已存有 {@code SM4} 标记的 AES 密文，
 *       替换前必须先做数据迁移，否则旧密文无法解密。</li>
 *   <li>MD5 计算时加入 salt 前缀，增强抗彩虹表攻击能力；但 MD5 仅适用于指纹/校验场景，
 *       <b>密码存储请使用 BCrypt/Argon2</b>。</li>
 * </ul>
 * </p>
 * <p>
 * <b>密钥配置要求（fail-closed）</b>：本类不提供任何内置默认密钥或默认盐。
 * AES / DES 密钥与 MD5 盐必须先通过 {@code dream.encrypt.*} 配置注入（由 EncryptAutoConfiguration
 * 调用 {@link #configure}），或在调用处显式传入；否则相应操作抛出 {@link IllegalStateException}。
 * 配置方式：
 * <pre>{@code
 * dream:
 *   encrypt:
 *     aes-key: xxxxxxxx  # AES 密钥（16 字节）
 *     des-key: xxxxxx    # DES 密钥（8 字节）
 *     md5-salt: xxxxxx   # MD5 加盐前缀（必填；兼容旧数据可显式配置为历史使用的盐）
 * }</pre>
 * </p>
 * <p>
 * <b>实现结构</b>：AES / DES / SM4(占位) 走同一套 CBC 流程
 * （{@link #cbcEncrypt} / {@link #cbcDecrypt} + {@link CbcSpec} 参数表），
 * 不再各算法一份代码；Base64 编解码复用 tool 模块 {@link CodecUtils}。
 * </p>
 * <p>
 * <b>与其他加密工具的分工</b>（互不兼容，勿混用）：
 * <ul>
 *   <li>本类——字段落库/取库解密（{@code @Encrypt}），密文为 {@code Base64(IV + 密文)} 的 CBC 产物，
 *       无完整性校验</li>
 *   <li>{@code AesUtils}（tool）——报文与配置值的对称加密，AES/<b>GCM</b> 认证加密，
 *       密文格式不同，无法解本类写入的数据；新代码优先用它</li>
 *   <li>{@code DigestUtils}（tool）——标准加盐摘要是 {@code data + salt}（后缀），
 *       本类 MD5 是 {@code salt + content}（前缀），摘要值不同，<b>不可互换</b>，
 *       替换会让库中已存的加盐 MD5 全部失配</li>
 * </ul>
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

    /** AES 密钥字节数（AES-128） */
    private static final int AES_KEY_SIZE = 16;
    /** AES IV 字节数 */
    private static final int AES_IV_SIZE = 16;
    /** DES 密钥字节数 */
    private static final int DES_KEY_SIZE = 8;
    /** DES IV 字节数 */
    private static final int DES_IV_SIZE = 8;

    /** 随机 IV 生成器（线程安全，全类共用） */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ==================== 运行时密钥（必须由 dream.encrypt.* 配置注入，无内置默认值） ====================

    /** 当前生效的 AES 密钥（未配置时为 null，使用即抛异常，fail-closed） */
    private static volatile String AES_KEY = null;
    /** 当前生效的 DES 密钥（未配置时为 null，使用即抛异常，fail-closed） */
    private static volatile String DES_KEY = null;
    /** 当前生效的 MD5 盐（未配置时为 null，使用即抛异常，fail-closed） */
    private static volatile String MD5_SALT = null;

    /** SM4 占位实现的告警是否已输出 */
    private static volatile boolean sm4Warned;

    /**
     * 通过 Spring 配置注入密钥与盐。
     * <p>由框架自动装配（{@code EncryptAutoConfiguration}）读取 {@code dream.encrypt.*} 后调用。</p>
     * <p>
     * 仅覆盖传入的非空项：传 {@code null} 或空串不会清空已有值，也不会抛异常，
     * 相应操作仍按 fail-closed 规则处理。日志只输出被注入的配置项名，绝不输出密钥内容。
     * </p>
     *
     * @param aesKey  AES 密钥（建议 16 字节）
     * @param desKey  DES 密钥（建议 8 字节）
     * @param md5Salt MD5 加盐前缀
     */
    public static synchronized void configure(String aesKey, String desKey, String md5Salt) {
        StringBuilder injected = new StringBuilder();
        if (aesKey != null && !aesKey.isEmpty()) {
            AES_KEY = aesKey;
            injected.append(", aes-key");
        }
        if (desKey != null && !desKey.isEmpty()) {
            DES_KEY = desKey;
            injected.append(", des-key");
        }
        if (md5Salt != null && !md5Salt.isEmpty()) {
            MD5_SALT = md5Salt;
            injected.append(", md5-salt");
        }
        if (injected.length() == 0) {
            // 未配置属于合法状态（不使用 @Encrypt / 加解密的项目同样会走到这里），
            // 真正的失败留到调用点 fail-closed 抛出，这里只作提示，避免启动期噪音升级成 WARN
            log.info("[EncryptUtil] dream.encrypt.* 未配置密钥/盐，调用加解密时将 fail-closed 抛异常");
            return;
        }
        log.info("[EncryptUtil] 已注入密钥/盐：{}", injected.substring(2));
    }

    /**
     * 校验密钥/盐已就绪（fail-closed）。
     * <p>既未显式传参也未通过 {@code dream.encrypt.*} 配置时直接抛出异常，
     * 不再回退到内置默认值——已知常量的"默认密钥/默认盐"等同于不加密、不加盐。</p>
     *
     * @param key        生效的密钥或盐
     * @param configName 对应的配置项名，用于错误提示
     * @throws IllegalStateException 未配置时抛出
     */
    private static void requireKey(String key, String configName) {
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("加密参数未配置：请通过 dream.encrypt." + configName
                    + " 注入，或在调用处显式传入。EncryptUtil 不提供内置默认密钥/默认盐。");
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
     *   <li>{@link EncryptType#SM4}：占位实现，复用 AES/CBC（并非国密，首次使用输出 WARN）</li>
     *   <li>{@link EncryptType#BASE64}：Base64 编码（复用 {@code CodecUtils}，仅编码不是加密）</li>
     *   <li>{@link EncryptType#MD5}：加盐 MD5 摘要（不可逆）</li>
     * </ul>
     * </p>
     *
     * @param content 待加密内容，为 null 时返回 null
     * @param type    加密类型，为 null 时抛出 {@link IllegalArgumentException}（fail-closed）
     * @param key     加密密钥，为 null 或空时回退到 {@code dream.encrypt.*} 配置密钥；
     *                两者均缺失时抛出异常，不使用任何内置默认值
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
                    return cbcEncrypt(content, key, CbcSpec.AES);
                case DES:
                    return cbcEncrypt(content, key, CbcSpec.DES);
                case SM4:
                    // 占位实现：并非国密，复用 AES 参数行，详见 warnSm4Placeholder
                    warnSm4Placeholder();
                    return cbcEncrypt(content, key, CbcSpec.AES);
                case BASE64:
                    return CodecUtils.base64Encode(content);
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
     * @param key     解密密钥，为 null 或空时回退到 {@code dream.encrypt.*} 配置密钥；
     *                两者均缺失时抛出异常，不使用任何内置默认值
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
                    return cbcDecrypt(content, key, CbcSpec.AES);
                case DES:
                    return cbcDecrypt(content, key, CbcSpec.DES);
                case SM4:
                    // 占位实现：与加密侧一致，按 AES 参数解密
                    return cbcDecrypt(content, key, CbcSpec.AES);
                case BASE64:
                    return CodecUtils.base64Decode(content);
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
     * CBC 分组模式加解密的<b>唯一</b>实现（AES / DES 及复用 AES 的 SM4 占位共用）
     * <p>
     * 流程：密钥规范化（不足补 {@code '0'}、超出截断）→ <b>每次随机生成 IV</b> →
     * 输出 {@code Base64(IV + 密文)}。算法、密钥长度、IV 长度全部由 {@link CbcSpec} 参数表给出，
     * 新增分组算法只需在表里加一行。
     * </p>
     * <p>
     * IV 随机化不影响历史密文解密：IV 一直存放在密文头部。
     * </p>
     *
     * @param content 待加密内容
     * @param key     调用处显式传入的密钥，为空时回退到 {@code dream.encrypt.*} 配置的密钥
     * @param spec    算法参数
     * @return Base64 编码的加密结果（含 IV 前缀）
     * @throws Exception 加密失败时抛出
     */
    private static String cbcEncrypt(String content, String key, CbcSpec spec) throws Exception {
        String secretKey = (key == null || key.isEmpty()) ? spec.configuredKey() : key;
        requireKey(secretKey, spec.configName);
        byte[] keyBytes = normalizeKey(secretKey, spec.keySize);
        // 每次加密随机生成 IV，避免固定 IV 导致同明文同密文（等值关系泄露）
        byte[] ivBytes = randomBytes(spec.ivSize);

        Cipher cipher = Cipher.getInstance(spec.cipherAlgorithm);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, spec.keyAlgorithm),
                new IvParameterSpec(ivBytes));
        byte[] encryptedBytes = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));

        // 拼接 IV + 密文后统一 Base64 编码
        byte[] output = new byte[spec.ivSize + encryptedBytes.length];
        System.arraycopy(ivBytes, 0, output, 0, spec.ivSize);
        System.arraycopy(encryptedBytes, 0, output, spec.ivSize, encryptedBytes.length);
        return Base64.getEncoder().encodeToString(output);
    }

    /**
     * CBC 分组模式解密的<b>唯一</b>实现，与 {@link #cbcEncrypt} 对称：
     * 从 Base64 解码后按 {@link CbcSpec#ivSize} 分离 IV 与密文。
     *
     * @param content Base64 编码的加密结果（含 IV 前缀）
     * @param key     解密密钥，为空时回退到配置密钥
     * @param spec    算法参数
     * @return 解密后的明文
     * @throws Exception 解密失败时抛出
     */
    private static String cbcDecrypt(String content, String key, CbcSpec spec) throws Exception {
        String secretKey = (key == null || key.isEmpty()) ? spec.configuredKey() : key;
        requireKey(secretKey, spec.configName);
        byte[] keyBytes = normalizeKey(secretKey, spec.keySize);

        byte[] decodedBytes = Base64.getDecoder().decode(content);
        // 分离 IV 与密文
        byte[] iv = new byte[spec.ivSize];
        byte[] encryptedBytes = new byte[decodedBytes.length - spec.ivSize];
        System.arraycopy(decodedBytes, 0, iv, 0, spec.ivSize);
        System.arraycopy(decodedBytes, spec.ivSize, encryptedBytes, 0, encryptedBytes.length);

        Cipher cipher = Cipher.getInstance(spec.cipherAlgorithm);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, spec.keyAlgorithm),
                new IvParameterSpec(iv));
        return new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8);
    }

    /**
     * CBC 分组算法参数表：密钥算法、转换名、密钥/IV 长度与对应的配置项
     * <p>
     * {@link EncryptType#SM4} 复用 {@link #AES} 行（占位实现，并非国密），
     * 因此 SM4 密文实为 AES 密文，替换真实 SM4 前必须做转密迁移。
     * </p>
     */
    private enum CbcSpec {

        /** AES/CBC/PKCS5Padding，16 字节密钥与 IV */
        AES(AES_ALGORITHM, AES_CIPHER_ALGORITHM, AES_KEY_SIZE, AES_IV_SIZE, "aes-key"),

        /** DES/CBC/PKCS5Padding，8 字节密钥与 IV（弱算法，仅兼容历史数据） */
        DES(DES_ALGORITHM, DES_CIPHER_ALGORITHM, DES_KEY_SIZE, DES_IV_SIZE, "des-key");

        /** {@link SecretKeySpec} 使用的密钥算法名 */
        private final String keyAlgorithm;
        /** {@link Cipher#getInstance(String)} 转换名 */
        private final String cipherAlgorithm;
        /** 密钥字节数 */
        private final int keySize;
        /** IV 字节数 */
        private final int ivSize;
        /** 配置项名（{@code dream.encrypt.xxx}），用于缺失密钥时的错误提示 */
        private final String configName;

        CbcSpec(String keyAlgorithm, String cipherAlgorithm, int keySize, int ivSize, String configName) {
            this.keyAlgorithm = keyAlgorithm;
            this.cipherAlgorithm = cipherAlgorithm;
            this.keySize = keySize;
            this.ivSize = ivSize;
            this.configName = configName;
        }

        /**
         * 当前通过 {@code dream.encrypt.*} 注入的密钥（未注入时为 {@code null}，使用即 fail-closed）
         *
         * @return 配置密钥
         */
        private String configuredKey() {
            return this == DES ? DES_KEY : AES_KEY;
        }
    }

    /**
     * 输出一次性告警，提示 SM4 当前为占位实现（并非国密），避免逐次刷屏。
     * <p>
     * 需要真正的国密合规时：引入 BouncyCastle（{@code org.bouncycastle:bcprov-jdk18on}）后替换
     * {@link CbcSpec#AES} 行对应的实现；<b>库中已存有以 {@code SM4} 标记、实为 AES 的历史密文</b>，
     * 直接切换算法会让旧密文无法解密，迁移前须先转密。
     * </p>
     */
    private static void warnSm4Placeholder() {
        if (sm4Warned) {
            return;
        }
        sm4Warned = true;
        log.warn("[EncryptUtil] EncryptType.SM4 当前复用 AES/CBC 实现，并非国密 SM4 算法。"
                + "需国密合规请引入 BouncyCastle 替换实现，并注意历史 SM4 标记数据需先做转密迁移。");
    }

    /**
     * MD5 加密（不可逆）
     * <p>
     * 计算时在原始内容前加入固定 salt 前缀，以增强抗彩虹表攻击能力。
     * 注意：MD5 本身已不推荐用于密码存储等高安全场景，建议改用 BCrypt/Argon2。
     * </p>
     * <p>
     * <b>不要替换成 tool 模块 {@code DigestUtils.md5Salt(data, salt)}</b>：
     * 本方法的盐是 {@code salt + data}（前缀），DigestUtils 的是 {@code data + salt}（后缀），
     * 两者摘要不同，直接替换会让库中已存的加盐 MD5 值全部失配。
     * </p>
     *
     * @param content 待摘要内容
     * @return 32 位小写十六进制 MD5 摘要字符串
     * @throws Exception 摘要计算失败时抛出
     */
    private static String md5Encrypt(String content) throws Exception {
        // MD5 盐必须由配置注入，未配置则 fail-closed——空盐等于不加盐
        requireKey(MD5_SALT, "md5-salt");
        MessageDigest md = MessageDigest.getInstance(MD5_ALGORITHM);
        // 加入 salt 前缀，增强安全性
        byte[] digest = md.digest((MD5_SALT + content).getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /**
     * 生成指定长度的 cryptographically-secure 随机字节，用于 CBC 的 IV。
     *
     * @param size 字节数
     * @return 随机字节数组
     */
    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
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
