package com.dream.common.tool.utils.id;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * 雪花算法 ID 生成器
 * <p>
 * 64位结构：0 - 41位时间戳 - 5位数据中心ID - 5位工作机器ID - 12位序列号
 * <p>
 * 并发与时钟语义：
 * <ul>
 *   <li>{@link #nextId()} 整体同步，时间回拨检测/等待/拒绝均在临界区内完成，
 *       回拨超过 {@value #MAX_BACKWARD_OFFSET}ms 直接抛出 {@link IllegalStateException}（fail-fast）</li>
 *   <li>单节点每毫秒可生成 4096 个 ID</li>
 *   <li>容器/多实例部署<b>必须</b>显式指定机器号（配置项
 *       {@code dream.snowflake.worker-id}/{@code dream.snowflake.datacenter-id}
 *       或环境变量 {@code DREAM_SNOWFLAKE_WORKER_ID}/{@code DREAM_SNOWFLAKE_DATACENTER_ID}），
 *       否则将退化为 MAC+PID 推导，存在撞号风险</li>
 * </ul>
 *
 * @author dream
 */
@Slf4j
public final class SnowFlakeIdGenerator implements IdGenerator<Long> {

    // ==================== 位定义 ====================

    private static final long DATACENTER_ID_BITS = 5L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    // ==================== 常量 ====================

    /**
     * 起始时间戳（2022-02-23 15:43:16.508）
     */
    private static final long START_TIMESTAMP = 1645587796508L;

    /**
     * 时间回拨最大容忍偏移（5ms）
     */
    private static final long MAX_BACKWARD_OFFSET = 5L;

    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

    // ==================== 实例字段 ====================

    private final long datacenterId;
    private final long workerId;

    /**
     * 序列号（与 lastTimestamp 同受 nextId() 的同步保护，无需原子类）
     */
    private long sequence = 0L;

    /**
     * 上次生成时间戳
     */
    private long lastTimestamp = -1L;

    // ==================== 单例 ====================

    private static final SnowFlakeIdGenerator INSTANCE = createDefaultInstance();

    public static SnowFlakeIdGenerator getInstance() {
        return INSTANCE;
    }

    /**
     * 创建默认实例：优先读取显式配置的机器号（系统属性 {@code dream.snowflake.*}
     * 或环境变量 {@code DREAM_SNOWFLAKE_*}），未配置时回退 MAC+PID 推导并输出 WARN。
     */
    private static SnowFlakeIdGenerator createDefaultInstance() {
        Long workerId = readLongSetting("dream.snowflake.worker-id", "DREAM_SNOWFLAKE_WORKER_ID");
        Long datacenterId = readLongSetting("dream.snowflake.datacenter-id", "DREAM_SNOWFLAKE_DATACENTER_ID");
        if (workerId != null) {
            return new SnowFlakeIdGenerator(workerId, datacenterId == null ? 0L : datacenterId);
        }
        if (datacenterId != null) {
            return new SnowFlakeIdGenerator(deriveWorkerId(deriveDatacenterId()), datacenterId);
        }
        return new SnowFlakeIdGenerator();
    }

    /**
     * 依次从系统属性、环境变量读取 long 型配置
     */
    private static Long readLongSetting(String property, String env) {
        String raw = System.getProperty(property);
        if (raw == null || raw.trim().isEmpty()) {
            raw = System.getenv(env);
        }
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("雪花算法配置项 {}={} 非法，已忽略", property, raw);
            return null;
        }
    }

    // ==================== 构造器 ====================

    /**
     * 默认构造器：基于本机 MAC/进程信息推导机器号。
     * <p>仅供单实例开发环境使用；多实例部署请改用双参构造或配置
     * {@code dream.snowflake.worker-id}。</p>
     */
    private SnowFlakeIdGenerator() {
        this.datacenterId = deriveDatacenterId();
        this.workerId = deriveWorkerId(this.datacenterId);
        log.warn("[SnowFlake] 未显式配置机器号，已回退 MAC/PID 推导（datacenterId={}, workerId={}）。"
                + "多实例部署必须配置 dream.snowflake.worker-id，否则可能生成重复 ID。",
                datacenterId, workerId);
    }

    /**
     * 显式指定机器号的构造器
     *
     * @param workerId     工作机器ID（0 ~ 31）
     * @param datacenterId 数据中心ID（0 ~ 31）
     */
    public SnowFlakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("WorkerId 必须在 0 ~ " + MAX_WORKER_ID + " 之间");
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("DatacenterId 必须在 0 ~ " + MAX_DATACENTER_ID + " 之间");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    // ==================== 核心方法 ====================

    /**
     * 生成下一个 ID。
     * <p>方法整体同步：时间戳读取、回拨处理、序列推进、lastTimestamp 更新均在临界区内完成，
     * 任何路径下都不会发出重复 ID。</p>
     *
     * @return 全局唯一趋势递增 ID
     * @throws IllegalStateException 时钟回拨超过容忍阈值或等待被中断
     */
    @Override
    public synchronized Long nextId() {
        long currentTimestamp = timeGen();

        // 时钟回拨处理（小偏移等待追平，大偏移 fail-fast）
        if (currentTimestamp < lastTimestamp) {
            long offset = lastTimestamp - currentTimestamp;
            if (offset > MAX_BACKWARD_OFFSET) {
                throw new IllegalStateException("时间回拨过大，拒绝生成ID，偏移: " + offset + "ms");
            }
            try {
                Thread.sleep(offset << 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("时间回拨处理被中断", e);
            }
            currentTimestamp = timeGen();
            if (currentTimestamp < lastTimestamp) {
                throw new IllegalStateException("时间回拨过大，拒绝生成ID，偏移: " + offset + "ms");
            }
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 当前毫秒序列号用尽，等待下一毫秒
                currentTimestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = currentTimestamp;
        return buildId(currentTimestamp, sequence);
    }

    /**
     * 构建 ID
     */
    private long buildId(long timestamp, long seq) {
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | seq;
    }

    /**
     * 等待直到下一毫秒
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳（毫秒）
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }

    // ==================== 机器 ID 获取 ====================

    /**
     * 获取数据中心ID（基于MAC地址计算）。
     * <p>推导失败时回退 1 并输出 WARN——多实例环境请显式配置，勿依赖本推导。</p>
     */
    private static long deriveDatacenterId() {
        try {
            InetAddress address = getLocalAddress();
            NetworkInterface network = NetworkInterface.getByInetAddress(address);
            if (network == null) {
                log.warn("[SnowFlake] 无法定位本机网卡，datacenterId 回退为 1");
                return 1L;
            }
            byte[] mac = network.getHardwareAddress();
            if (mac == null) {
                log.warn("[SnowFlake] 网卡无 MAC 地址，datacenterId 回退为 1");
                return 1L;
            }
            long id = ((0x000000FF & (long) mac[mac.length - 2])
                    | (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
            return id % (MAX_DATACENTER_ID + 1);
        } catch (Exception e) {
            log.warn("[SnowFlake] datacenterId 推导失败，回退为 1", e);
            return 1L;
        }
    }

    /**
     * 获取工作机器ID（基于MAC + PID计算）。
     * <p>推导失败时回退 1 并输出 WARN——多实例环境请显式配置，勿依赖本推导。</p>
     */
    private static long deriveWorkerId(long datacenterId) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(datacenterId);
            String name = ManagementFactory.getRuntimeMXBean().getName();
            if (name != null && name.indexOf('@') > 0) {
                sb.append(name.split("@")[0]);
            }
            return (sb.toString().hashCode() & 0xffff) % (MAX_WORKER_ID + 1);
        } catch (Exception e) {
            log.warn("[SnowFlake] workerId 推导失败，回退为 1", e);
            return 1L;
        }
    }

    // ==================== 获取本机IP ====================

    private static volatile InetAddress localAddress;

    private static InetAddress getLocalAddress() {
        InetAddress cached = localAddress;
        if (cached != null) {
            return cached;
        }
        localAddress = findLocalAddress();
        return localAddress;
    }

    private static InetAddress findLocalAddress() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            if (isValidAddress(addr)) {
                return addr;
            }
        } catch (Exception e) {
            log.warn("[SnowFlake] 通过 getLocalHost 获取本机地址失败，改为遍历网卡", e);
        }

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface network = interfaces.nextElement();
                    Enumeration<InetAddress> addresses = network.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (isValidAddress(addr)) {
                            return addr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[SnowFlake] 遍历网卡获取本机地址失败", e);
        }

        log.warn("[SnowFlake] 未能获取有效本机地址，回退 127.0.0.1；多实例部署可能撞号，请显式配置 dream.snowflake.worker-id/datacenter-id");
        try {
            return InetAddress.getByName("127.0.0.1");
        } catch (Exception e) {
            throw new IllegalStateException("无法获取本机IP", e);
        }
    }

    private static boolean isValidAddress(InetAddress address) {
        if (address == null) {
            return false;
        }
        if (address.isLoopbackAddress()) {
            return false;
        }
        String name = address.getHostAddress();
        if (name == null || address.isAnyLocalAddress()) {
            return false;
        }
        // 兼容 IPv6 主机：地址仅用于定位网卡，ID 实际由 MAC + PID 推导，不要求 IPv4 形态
        return IP_PATTERN.matcher(name).matches() || name.indexOf(':') > 0;
    }
}