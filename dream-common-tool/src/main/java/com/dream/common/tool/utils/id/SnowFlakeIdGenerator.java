package com.dream.common.tool.utils.id;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * 雪花算法 ID 生成器
 * <p>
 * 64位结构：0 - 41位时间戳 - 5位数据中心ID - 5位工作机器ID - 12位序列号
 * <p>
 * 高并发支持：
 * <ul>
 *   <li>使用 {@link AtomicLong} 保证线程安全</li>
 *   <li>单节点每毫秒可生成 4096 个 ID</li>
 *   <li>支持时钟回拨自动等待</li>
 * </ul>
 */
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
     * 使用 AtomicLong 保证高并发下的线程安全
     */
    private final AtomicLong sequence = new AtomicLong(0L);

    /**
     * 上次生成时间戳（使用 volatile 保证可见性）
     */
    private volatile long lastTimestamp = -1L;

    // ==================== 单例 ====================

    private static final SnowFlakeIdGenerator INSTANCE = new SnowFlakeIdGenerator();

    public static SnowFlakeIdGenerator getInstance() {
        return INSTANCE;
    }

    // ==================== 构造器 ====================

    private SnowFlakeIdGenerator() {
        this.datacenterId = getDatacenterId();
        this.workerId = getWorkerId(this.datacenterId);
    }

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

    // ==================== 核心方法（高并发优化） ====================

    @Override
    public Long nextId() {
        long currentTimestamp = timeGen();

        // 处理时间回拨
        long last = lastTimestamp;
        if (currentTimestamp < last) {
            long offset = last - currentTimestamp;
            if (offset <= MAX_BACKWARD_OFFSET) {
                try {
                    Thread.sleep(offset << 1);
                    currentTimestamp = timeGen();
                    if (currentTimestamp < last) {
                        throw new IllegalStateException("时间回拨过大，拒绝生成ID，偏移: " + offset + "ms");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("时间回拨处理被中断", e);
                }
            } else {
                throw new IllegalStateException("时间回拨过大，拒绝生成ID，偏移: " + offset + "ms");
            }
        }

        // 同步更新 lastTimestamp 和 sequence
        synchronized (this) {
            if (currentTimestamp < lastTimestamp) {
                currentTimestamp = lastTimestamp;
            }

            if (currentTimestamp == lastTimestamp) {
                long seq = sequence.incrementAndGet() & SEQUENCE_MASK;
                if (seq == 0) {
                    currentTimestamp = tilNextMillis(lastTimestamp);
                }
                if (currentTimestamp != lastTimestamp) {
                    sequence.set(0);
                }
                lastTimestamp = currentTimestamp;
                return buildId(currentTimestamp, seq);
            } else {
                sequence.set(0);
                lastTimestamp = currentTimestamp;
                return buildId(currentTimestamp, 0);
            }
        }
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
     * 获取数据中心ID（基于MAC地址计算）
     */
    private long getDatacenterId() {
        try {
            InetAddress address = getLocalAddress();
            NetworkInterface network = NetworkInterface.getByInetAddress(address);
            if (network == null) {
                return 1L;
            }
            byte[] mac = network.getHardwareAddress();
            if (mac == null) {
                return 1L;
            }
            long id = ((0x000000FF & (long) mac[mac.length - 2])
                    | (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
            return id % (MAX_DATACENTER_ID + 1);
        } catch (Exception e) {
            return 1L;
        }
    }

    /**
     * 获取工作机器ID（基于MAC + PID计算）
     */
    private long getWorkerId(long datacenterId) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(datacenterId);
            String name = ManagementFactory.getRuntimeMXBean().getName();
            if (name != null && name.indexOf('@') > 0) {
                sb.append(name.split("@")[0]);
            }
            return (sb.toString().hashCode() & 0xffff) % (MAX_WORKER_ID + 1);
        } catch (Exception e) {
            return 1L;
        }
    }

    // ==================== 获取本机IP ====================

    private static volatile InetAddress localAddress;

    private InetAddress getLocalAddress() {
        if (localAddress != null) {
            return localAddress;
        }
        localAddress = findLocalAddress();
        return localAddress;
    }

    private InetAddress findLocalAddress() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            if (isValidAddress(addr)) {
                return addr;
            }
        } catch (Exception ignored) {
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
        } catch (Exception ignored) {
        }

        try {
            return InetAddress.getByName("127.0.0.1");
        } catch (Exception e) {
            throw new IllegalStateException("无法获取本机IP", e);
        }
    }

    private boolean isValidAddress(InetAddress address) {
        if (address == null) {
            return false;
        }
        if (address.isLoopbackAddress()) {
            return false;
        }
        String name = address.getHostAddress();
        return name != null
                && !"0.0.0.0".equals(name)
                && !"127.0.0.1".equals(name)
                && IP_PATTERN.matcher(name).matches();
    }
}