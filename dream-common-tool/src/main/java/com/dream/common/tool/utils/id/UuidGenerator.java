package com.dream.common.tool.utils.id;

import com.github.f4b6a3.uuid.UuidCreator;
import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;

/**
 * UUID 生成器
 * <p>
 * 支持 UUID v4、UUID v7 和 ULID 三种标识符生成方式。
 * <p>
 * <b>标识符对比：</b>
 * <ul>
 *   <li><b>UUID v4</b>：随机 UUID，全局唯一，不可排序</li>
 *   <li><b>UUID v7</b>：时间有序 UUID，符合 RFC 9562 标准，适合数据库主键</li>
 *   <li><b>ULID</b>：时间有序标识符，26 字符 Base32 编码，比 UUID 更紧凑，性能更高</li>
 * </ul>
 * <p>
 * 高并发支持：底层库使用高性能算法，所有生成方法均为线程安全
 *
 * @see <a href="https://github.com/f4b6a3/uuid-creator">uuid-creator</a>
 * @see <a href="https://github.com/f4b6a3/ulid-creator">ulid-creator</a>
 */
public final class UuidGenerator implements IdGenerator<String> {

    private static final UuidGenerator INSTANCE = new UuidGenerator();

    public static UuidGenerator getInstance() {
        return INSTANCE;
    }

    private UuidGenerator() {
    }

    @Override
    public String nextId() {
        return guid7();
    }

    // ==================== UUID v4（随机） ====================

    public String uuid() {
        return UuidCreator.getRandomBased().toString();
    }

    public String simpleUuid() {
        return UuidCreator.getRandomBased().toString().replace("-", "");
    }

    public String fastUuid() {
        return UuidCreator.getRandomBasedFast().toString();
    }

    // ==================== UUID v7（时间有序，RFC 9562） ====================

    public String guid7() {
        return UuidCreator.getTimeOrderedEpoch().toString();
    }

    public String simpleGuid7() {
        return UuidCreator.getTimeOrderedEpoch().toString().replace("-", "");
    }

    // ==================== ULID（时间有序，紧凑格式） ====================

    /**
     * 生成标准 ULID
     * <p>
     * ULID（Universally Unique Lexicographically Sortable Identifier）具有以下特点：
     * <ul>
     *   <li>128 位标识符，与 UUID 同规模</li>
     *   <li>26 字符 Base32 编码，比 UUID 更紧凑</li>
     *   <li>前 48 位为 Unix 毫秒时间戳，天然支持时间排序</li>
     *   <li>后 80 位为随机数，保证唯一性</li>
     *   <li>URL 安全，不区分大小写，不含特殊字符</li>
     *   <li>生成性能比 UUID v7 更高</li>
     * </ul>
     * <p>
     * 示例：01H6X3VPKX0K0K0K0K0K0K0K0K
     *
     * @return 标准 ULID 字符串（26 字符）
     */
    public String ulid() {
        return UlidCreator.getUlid().toString();
    }

    /**
     * 生成单调递增 ULID
     * <p>
     * 与标准 ULID 相比，在同一毫秒内通过递增随机数保证严格的时间顺序。
     * 适合需要严格时序的场景。
     * <p>
     * 示例：01H6X3VPKX0K0K0K0K0K0K0K0L
     *
     * @return 单调递增 ULID 字符串（26 字符）
     */
    public String monotonicUlid() {
        return UlidCreator.getMonotonicUlid().toString();
    }

    /**
     * 将 ULID 字符串转换为 UUID
     * <p>
     * ULID 和 UUID 都是 128 位，可以互相转换。
     * 注意：转换后的 UUID 不是标准的时间有序 UUID，但可以用于兼容需要 UUID 类型的场景。
     *
     * @param ulidString ULID 字符串（26 字符）
     * @return UUID 对象
     */
    public java.util.UUID ulidToUuid(String ulidString) {
        if (ulidString == null || ulidString.isEmpty()) {
            return null;
        }
        return Ulid.from(ulidString).toUuid();
    }

    /**
     * 将 UUID 转换为 ULID 字符串
     *
     * @param uuid UUID 对象
     * @return ULID 字符串（26 字符）
     */
    public String uuidToUlid(java.util.UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return Ulid.from(uuid).toString();
    }
}