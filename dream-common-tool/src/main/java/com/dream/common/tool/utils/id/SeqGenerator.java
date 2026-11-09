package com.dream.common.tool.utils.id;

import com.dream.common.tool.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 序号生成器（无状态工具类）
 * <p>
 * 不包含任何缓存，由业务层自行管理序号状态（如从数据库/Redis获取当前序号）。
 * 适合微服务环境下各服务独立管理序号的场景。
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 1. 从数据库获取当前最大序号
 * int currentSeq = dictMapper.selectMaxSeq("ORD");
 *
 * // 2. 生成下一个序号
 * String orderNo = SeqGenerator.next("ORD-", currentSeq);
 *
 * // 3. 更新数据库序号
 * dictMapper.updateSeq("ORD", currentSeq + 1);
 * }</pre>
 * <p>
 * <b>注意：本类不保存任何状态，每次调用需要传入当前序号值。</b>
 *
 * @author dream
 */
@Slf4j
public final class SeqGenerator {

    private SeqGenerator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 默认序号位数，{@link IdUtils} 共用本常量，避免两处各写一份
     */
    public static final int DEFAULT_SEQ_LEN = 5;

    /**
     * 生成下一个序号（使用默认5位）
     *
     * @param prefix     前缀（可为 null 或空字符串）
     * @param currentSeq 当前序号
     * @return 带前缀的下一个序号，如 "ORD-00002"
     */
    public static String next(String prefix, int currentSeq) {
        return next(prefix, currentSeq, DEFAULT_SEQ_LEN);
    }

    /**
     * 生成下一个序号（指定序号位数）
     *
     * @param prefix     前缀（可为 null 或空字符串）
     * @param currentSeq 当前序号
     * @param seqLen     序号位数
     * @return 带前缀的下一个序号
     * @throws IllegalArgumentException seqLen 小于等于0时抛出
     */
    public static String next(String prefix, int currentSeq, int seqLen) {
        if (seqLen <= 0) {
            throw new IllegalArgumentException("seqLen 必须大于0");
        }

        String resultPrefix = StringUtils.isBlank(prefix) ? "" : prefix;
        return resultPrefix + StringUtils.pads(currentSeq + 1, seqLen);
    }

    /**
     * 生成下一个序号值（不含前缀，默认5位）
     *
     * @param currentSeq 当前序号
     * @return 序号值，如 "00002"
     */
    public static String nextValue(int currentSeq) {
        return nextValue(currentSeq, DEFAULT_SEQ_LEN);
    }

    /**
     * 生成下一个序号值（不含前缀，指定位数）
     *
     * @param currentSeq 当前序号
     * @param seqLen     序号位数
     * @return 序号值，如 "00002"
     */
    public static String nextValue(int currentSeq, int seqLen) {
        if (seqLen <= 0) {
            throw new IllegalArgumentException("seqLen 必须大于0");
        }
        return StringUtils.pads(currentSeq + 1, seqLen);
    }

    /**
     * 解析序号中的数字部分
     *
     * @param serialNo 完整序号（如 "ORD-00005"）
     * @param prefix   前缀（如 "ORD-"）
     * @return 数字部分（如 5），解析失败返回 0
     */
    public static int parseSeq(String serialNo, String prefix) {
        if (StringUtils.isBlank(serialNo)) {
            return 0;
        }
        String prefixStr = StringUtils.isBlank(prefix) ? "" : prefix;
        if (!serialNo.startsWith(prefixStr)) {
            return 0;
        }
        String numStr = serialNo.substring(prefixStr.length());
        try {
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            // 脏单号被当成「无历史」会把序号重置为 0，进而产生重复单号，必须留痕
            log.warn("序号解析失败，已按 0 处理，请检查历史单号数据 | serialNo={} | prefix={}", serialNo, prefix);
            return 0;
        }
    }

    /**
     * 从序号中提取前缀
     *
     * @param serialNo 完整序号
     * @param seqLen   序号位数
     * @return 前缀；无法提取返回空字符串
     */
    public static String extractPrefix(String serialNo, int seqLen) {
        if (StringUtils.isBlank(serialNo) || seqLen <= 0) {
            return "";
        }
        if (serialNo.length() <= seqLen) {
            return "";
        }
        return serialNo.substring(0, serialNo.length() - seqLen);
    }
}