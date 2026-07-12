package com.dream.common.tool.utils.id;

import com.dream.common.tool.utils.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ID 生成工具类（统一门面）
 * <p>
 * 提供多种 ID/序号生成策略，所有方法均为无状态，适合微服务架构。
 * <p>
 * <b>主键选型建议：</b>
 * <ul>
 *   <li><b>数据库主键推荐</b>：ULID 或 UUID v7（均支持时间排序，提升索引性能）</li>
 *   <li><b>已有 UUID 列</b>：使用 UUID v7，兼容现有数据类型</li>
 *   <li><b>追求极致性能和紧凑格式</b>：使用 ULID</li>
 *   <li><b>需要数字型 ID</b>：使用雪花算法</li>
 * </ul>
 */
public final class IdUtils {

    // ==================== 常量 ====================

    /** 默认序号位数 */
    private static final int DEFAULT_SEQ_LEN = 5;

    /** 默认时间格式：yyyyMMddHHmmss */
    private static final String DEFAULT_TIME_PATTERN = "yyyyMMddHHmmss";

    // ==================== 生成器单例 ====================

    private static final SnowFlakeIdGenerator SNOW_FLAKE = SnowFlakeIdGenerator.getInstance();
    private static final UuidGenerator UUID_GEN = UuidGenerator.getInstance();

    private IdUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 雪花算法 ====================

    public static Long snowflakeId() {
        return SNOW_FLAKE.nextId();
    }

    public static String snowflakeIdString() {
        return SNOW_FLAKE.nextIdString();
    }

    // ==================== UUID v4 ====================

    public static String uuid() {
        return UUID_GEN.uuid();
    }

    public static String simpleUuid() {
        return UUID_GEN.simpleUuid();
    }

    public static String fastUuid() {
        return UUID_GEN.fastUuid();
    }

    // ==================== UUID v7 ====================

    public static String guid7() {
        return UUID_GEN.guid7();
    }

    public static String simpleGuid7() {
        return UUID_GEN.simpleGuid7();
    }

    // ==================== ULID ====================

    public static String ulid() {
        return UUID_GEN.ulid();
    }

    public static String monotonicUlid() {
        return UUID_GEN.monotonicUlid();
    }

    /**
     * 将 ULID 字符串转换为 UUID
     *
     * @param ulidString ULID 字符串（26 字符）
     * @return UUID 对象
     */
    public static java.util.UUID ulidToUuid(String ulidString) {
        return UUID_GEN.ulidToUuid(ulidString);
    }

    /**
     * 将 UUID 转换为 ULID 字符串
     *
     * @param uuid UUID 对象
     * @return ULID 字符串（26 字符）
     */
    public static String uuidToUlid(java.util.UUID uuid) {
        return UUID_GEN.uuidToUlid(uuid);
    }

    // ==================== 时间序号 ====================

    public static String timeSeq(String prefix, int currentSeq) {
        return timeSeq(prefix, currentSeq, DEFAULT_TIME_PATTERN, DEFAULT_SEQ_LEN);
    }

    public static String timeSeq(String prefix, int currentSeq, String pattern, int seqLen) {
        if (seqLen <= 0) {
            throw new IllegalArgumentException("seqLen 必须大于0");
        }
        if (pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("pattern 不能为空");
        }

        String resultPrefix = StringUtils.isBlank(prefix) ? "" : prefix;
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));

        return resultPrefix + timeStr + StringUtils.pads(currentSeq + 1, seqLen);
    }

    // ==================== 日期序号 ====================

    public static String dateSeq(String prefix, int currentSeq) {
        return dateSeq(prefix, currentSeq, DEFAULT_SEQ_LEN);
    }

    public static String dateSeq(String prefix, int currentSeq, int seqLen) {
        if (seqLen <= 0) {
            throw new IllegalArgumentException("seqLen 必须大于0");
        }

        String resultPrefix = StringUtils.isBlank(prefix) ? "" : prefix;
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        return resultPrefix + dateStr + StringUtils.pads(currentSeq + 1, seqLen);
    }

    // ==================== 自定义序号 ====================

    public static String seq(String prefix, int currentSeq) {
        return seq(prefix, currentSeq, DEFAULT_SEQ_LEN);
    }

    public static String seq(String prefix, int currentSeq, int seqLen) {
        return SeqGenerator.next(prefix, currentSeq, seqLen);
    }

    // ==================== 序号解析 ====================

    public static int parseSeq(String serialNo, String prefix) {
        return SeqGenerator.parseSeq(serialNo, prefix);
    }

    public static String extractPrefix(String serialNo, int seqLen) {
        return SeqGenerator.extractPrefix(serialNo, seqLen);
    }
}