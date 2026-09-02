# AesUtils 密钥派生算法变更迁移说明

## 1. 变更概览

| 项目     | 说明                                                                          |
| ------ | --------------------------------------------------------------------------- |
| 变更位置   | `com.dream.common.tool.utils.crypto.AesUtils#deriveKey`                     |
| 影响 API | `encryptWithSalt(String, String, String)`、`decryptWithSalt(String, String)` |
| 变更类型   | **Breaking Change（不兼容）**：旧算法加密的密文，新版本无法解密                                   |
| 生效版本   | 1.0.0（2026-09-02）                                                           |

## 2. 为什么变更

原实现对 `deriveKey` 的 javadoc 声称是 PBKDF2，实际是「SHA-256 裸哈希链迭代 1000 次」的简化实现，存在以下问题：

- 并非标准 KDF：没有基于 HMAC 的 PBKDF2 结构，只是一条 `hash = SHA256(hash)` 的迭代链；

- 抗暴力破解能力弱：裸 SHA-256 可被 GPU / ASIC 高速并行计算，1000 次迭代也远低于安全基线（OWASP 建议 PBKDF2-HMAC-SHA256 不低于 600,000 次）；

- 实现与文档不符，易造成使用方对安全强度的误判。

新实现改用 JDK 标准 **`PBKDF2WithHmacSHA256`**：

- 标准构造：`HMAC-SHA256(pwd, salt)` × 迭代次数；

- 迭代次数：100,000（常量 `PBKDF2_ITERATIONS`，兼顾安全与加解密性能）；

- 输出固定 256 位（32 字节）AES 密钥；

- 每次调用通过 `PBEKeySpec.clearPassword()` 及时清除内存中的口令副本。

## 3. 影响范围

| API                                        | 是否受影响                          |
| ------------------------------------------ | ------------------------------ |
| `encryptWithSalt` / `decryptWithSalt`      | ✅ 受影响（派生密钥发生变化）                |
| `encryptGcm` / `decryptGcm`                | ❌ 不受影响（密钥由调用方直接提供 16/24/32 字节） |
| `encrypt` / `decrypt`（ECB，已 `@Deprecated`） | ❌ 不受影响                         |
| `generateKey` / `generateKeyBytes`         | ❌ 不受影响                         |
| `parseKey` 内的 SHA-256 派生兜底                 | ❌ 不受影响                         |

受影响的原因：`encryptWithSalt` 的密文格式未变（`Base64(盐值 16 字节 + IV 12 字节 + 密文)`），
但同一 `password + salt` 在新旧版本中派生的密钥不同，因此新版本无法解密旧版本产出的密文。

## 4. 迁移步骤

### 方案一：升级前批量解密（推荐）

1. 在升级到新版本**之前**，用旧版本 `AesUtils.decryptWithSalt` 解密全部存量密文，得到明文并落盘/入库；
2. 升级到新版本后，使用新版本 `encryptWithSalt` 重新加密存量明文并覆盖存储；
3. 全量完成后再放开业务入口，存量数据整体切换到新算法。

### 方案二：过渡期双写（零停机）

1. 升级后，读路径先用新 `decryptWithSalt` 解密；解密失败说明是旧密文，改用第 5 节的「旧算法兼容解密」代码；
2. 写路径一律使用新版 `encryptWithSalt` 加密；
3. 通过定时/分批任务扫描存量数据，将旧密文解密后按新算法重加密入库；全部迁移完成后移除兼容分支。

## 5. 旧算法兼容解密参考代码

仅用于迁移期解密存量密文，**迁移完成后请删除**。密文格式（盐值 + IV + 密文）保持不变，只需还原旧派生逻辑：

```java
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 迁移期专用：解密旧版本（裸 SHA-256 链）算法产出的密文。
 * 仅用于存量数据迁移，迁移完成后删除。
 */
public static String decryptWithLegacySalt(String encryptedData, String password) throws Exception {
    final int gcmIvLength = 12;
    final int gcmTagLength = 128;

    byte[] decoded = Base64.getDecoder().decode(encryptedData);

    // 提取盐值（前 16 字节）
    int saltLength = 16;
    byte[] saltBytes = new byte[saltLength];
    System.arraycopy(decoded, 0, saltBytes, 0, saltLength);

    // 提取 IV
    byte[] iv = new byte[gcmIvLength];
    System.arraycopy(decoded, saltLength, iv, 0, gcmIvLength);

    // 提取密文
    byte[] cipherText = new byte[decoded.length - saltLength - gcmIvLength];
    System.arraycopy(decoded, saltLength + gcmIvLength, cipherText, 0, cipherText.length);

    // 还原旧版密钥派生：SHA-256 迭代 1000 次
    byte[] keyBytes = legacyDeriveKey(password, saltBytes);
    SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(gcmTagLength, iv));
    return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
}

private static byte[] legacyDeriveKey(String password, byte[] salt) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] input = password.getBytes(StandardCharsets.UTF_8);
    byte[] combined = new byte[input.length + salt.length];
    System.arraycopy(input, 0, combined, 0, input.length);
    System.arraycopy(salt, 0, combined, input.length, salt.length);

    byte[] hash = combined;
    for (int i = 0; i < 1000; i++) {
        hash = md.digest(hash);
    }

    byte[] key = new byte[32];
    System.arraycopy(hash, 0, key, 0, Math.min(hash.length, 32));
    return key;
}
```

## 6. 迁移验证建议

- 用新版本对同一明文执行 `encryptWithSalt`，断言输出密文可被新版 `decryptWithSalt` 正确还原；

- 用第 5 节兼容代码对一条**升级前**产出的存量密文做解密冒烟，确认能还原明文；

- 若同一密钥/口令被多个系统共享使用，需各系统同步升级并统一切换时间窗口。

## 7. 附：本次一并进行的安全调整

- 4 个 AES/ECB 加解密方法已标注 `@Deprecated` 并在 javadoc 中说明 ECB 模式的安全隐患（相同明文块产生相同密文，明文结构可被统计分析），仅用于兼容历史数据，新业务请改用 GCM 系列方法。

