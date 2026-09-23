package com.dream.common.core.utils;

import com.dream.common.core.enums.BaseEnum;
import com.dream.common.core.enums.SystemProperty;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * 系统工具类
 * <p>
 * 基于 {@link SystemProperty} 枚举提供统一的系统属性访问和工具方法。
 * 所有系统属性值均带缓存，性能优异。
 * </p>
 * <p>
 * 核心能力：
 * <ul>
 *   <li>{@link #getSystemProperty(SystemProperty)} —— 获取系统属性值</li>
 *   <li>{@link #getCurrentPid()} —— 获取当前进程 PID（Java 8 兼容）</li>
 *   <li>{@link #getHostName()} / {@link #getHostIp()} —— 主机信息</li>
 *   <li>{@link #isWindows()} / {@link #isLinux()} / {@link #isMac()} —— 操作系统判断</li>
 *   <li>{@link #getSystemInfo()} —— 获取系统信息摘要</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 获取系统属性
 * String javaVersion = SystemUtils.getSystemProperty(SystemProperty.JAVA_VERSION);
 * String userDir = SystemUtils.getUserDir();
 *
 * // 2. 操作系统判断
 * if (SystemUtils.isWindows()) {
 *     // Windows 特殊处理
 * }
 *
 * // 3. 获取系统信息
 * String info = SystemUtils.getSystemInfo();
 * long pid = SystemUtils.getCurrentPid();
 *
 * // 4. 获取主机信息
 * String hostname = SystemUtils.getHostname();
 * String ip = SystemUtils.getHostIp();
 * }</pre>
 *
 * @author dream
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class SystemUtils {

    /** 缓存主机名 */
    private static String hostname;

    /** 缓存主机 IP */
    private static String hostIp;

    /** 缓存当前进程 PID */
    private static Long currentPid;

    private SystemUtils() {
        throw new UnsupportedOperationException("SystemUtils is a utility class");
    }

    // ==================== 系统属性获取 ====================

    /**
     * 获取系统属性值
     *
     * @param property 系统属性枚举
     * @return 属性值，如果 property 为 null 返回 null
     */
    public static String getSystemProperty(SystemProperty property) {
        if (property == null) {
            return null;
        }
        return property.getProperty();
    }

    /**
     * 获取系统属性值（带默认值）
     *
     * @param property     系统属性枚举
     * @param defaultValue 默认值
     * @return 属性值，如果为空则返回默认值
     */
    public static String getSystemPropertyOrDefault(SystemProperty property, String defaultValue) {
        if (property == null) {
            return defaultValue;
        }
        String value = property.getProperty();
        return !isBlank(value) ? value : defaultValue;
    }

    /**
     * 获取系统属性值（按 Key 查找）
     *
     * @param key 系统属性 Key
     * @return 属性值，未找到返回 null
     */
    public static String getSystemProperty(String key) {
        if (isBlank(key)) {
            return null;
        }
        SystemProperty property = BaseEnum.fromValue(SystemProperty.class, key);
        return property != null ? property.getProperty() : null;
    }

    /**
     * 获取 JVM 启动时间戳（毫秒）
     *
     * @return JVM 启动时间戳
     */
    public static long getJvmStartTimeMillis() {
        try {
            return ManagementFactory.getRuntimeMXBean().getStartTime();
        } catch (Exception e) {
            return -1L;
        }
    }

    /**
     * 获取 JVM 启动时间（Date）
     *
     * @return JVM 启动时间
     */
    public static Date getJvmStartTime() {
        long millis = getJvmStartTimeMillis();
        return millis > 0 ? new Date(millis) : null;
    }

    /**
     * 获取 JVM 已运行时间（毫秒）
     *
     * @return 已运行时间（毫秒）
     */
    public static long getJvmUptime() {
        try {
            return ManagementFactory.getRuntimeMXBean().getUptime();
        } catch (Exception e) {
            return -1L;
        }
    }


    // ==================== 常用属性快捷方法 ====================

    public static String getLineSeparator() {
        return SystemProperty.getLineSeparator();
    }

    public static String getFileSeparator() {
        return SystemProperty.getFileSeparator();
    }

    public static String getPathSeparator() {
        return SystemProperty.getPathSeparator();
    }

    public static String getUserDir() {
        return SystemProperty.getUserDir();
    }

    public static String getUserHome() {
        return SystemProperty.getUserHome();
    }

    public static String getTempDir() {
        return SystemProperty.getTempDir();
    }

    public static String getJavaVersion() {
        return SystemProperty.getJavaVersion();
    }

    public static String getOsName() {
        return SystemProperty.getOsName();
    }

    public static String getUserName() {
        return SystemProperty.getUserName();
    }

    public static String getFileEncoding() {
        return SystemProperty.getFileEncoding();
    }

    // ==================== 操作系统判断 ====================

    public static boolean isWindows() {
        return SystemProperty.isWindows();
    }

    public static boolean isLinux() {
        return SystemProperty.isLinux();
    }

    public static boolean isMac() {
        return SystemProperty.isMac();
    }

    public static boolean isUnixLike() {
        return SystemProperty.isUnixLike();
    }

    // ==================== 进程信息 ====================

    /**
     * 获取当前进程 PID（Java 8 兼容）
     *
     * @return 当前进程 PID，获取失败返回 -1
     */
    public static long getCurrentPid() {
        if (currentPid != null) {
            return currentPid;
        }
        try {
            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            String name = runtime.getName();
            // 格式: pid@hostname 或 pid
            int index = name.indexOf('@');
            String pidStr = index > 0 ? name.substring(0, index) : name;
            currentPid = Long.parseLong(pidStr);
            return currentPid;
        } catch (Exception e) {
            return -1L;
        }
    }

    /**
     * 获取当前进程 PID（字符串形式）
     *
     * @return PID 字符串，获取失败返回 "unknown"
     */
    public static String getCurrentPidStr() {
        long pid = getCurrentPid();
        return pid > 0 ? String.valueOf(pid) : "unknown";
    }

    // ==================== 主机信息 ====================

    /**
     * 获取主机名
     *
     * @return 主机名，获取失败返回 "unknown"
     */
    public static String getHostName() {
        if (hostname != null) {
            return hostname;
        }
        try {
            hostname = InetAddress.getLocalHost().getHostName();
            return hostname;
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    /**
     * 获取主机 IP
     *
     * @return 主机 IP，获取失败返回 "unknown"
     */
    public static String getHostIp() {
        if (hostIp != null) {
            return hostIp;
        }
        try {
            hostIp = InetAddress.getLocalHost().getHostAddress();
            return hostIp;
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    // ==================== 系统信息汇总 ====================

    /**
     * 获取系统信息摘要
     *
     * @return 系统信息摘要字符串
     */
    public static String getSystemSummary() {
        return SystemProperty.getSystemSummary();
    }

    /**
     * 获取完整系统信息（包含 PID、主机、IP）
     *
     * @return 完整系统信息字符串
     */
    public static String getSystemInfo() {
        return String.format(
                "OS: %s %s (%s), Java: %s, User: %s, PID: %s, Host: %s, IP: %s, WorkDir: %s",
                getOsName(),
                SystemProperty.OS_VERSION.getProperty(),
                SystemProperty.OS_ARCH.getProperty(),
                getJavaVersion(),
                getUserName(),
                getCurrentPidStr(),
                getHostName(),
                getHostIp(),
                getUserDir()
        );
    }

    /**
     * 获取系统信息（Map 形式）
     *
     * @return 系统信息 Map
     */
    public static java.util.Map<String, String> getSystemInfoMap() {
        java.util.Map<String, String> info = new java.util.LinkedHashMap<>();
        info.put("os.name", getOsName());
        info.put("os.version", SystemProperty.OS_VERSION.getProperty());
        info.put("os.arch", SystemProperty.OS_ARCH.getProperty());
        info.put("java.version", getJavaVersion());
        info.put("user.name", getUserName());
        info.put("pid", getCurrentPidStr());
        info.put("hostname", getHostName());
        info.put("host.ip", getHostIp());
        info.put("user.dir", getUserDir());
        info.put("file.encoding", getFileEncoding());
        info.put("dream.application.name", SystemProperty.DREAM_NAME.getProperty());
        info.put("dream.application.version", SystemProperty.DREAM_VERSION.getProperty());
        return info;
    }

    // ==================== 缓存管理 ====================

    /**
     * 刷新所有系统属性缓存
     */
    public static void refreshAllCache() {
        SystemProperty.refreshAll();
        // 重置主机信息缓存
        hostname = null;
        hostIp = null;
        currentPid = null;
    }

    /**
     * 清除所有缓存
     */
    public static void clearAllCache() {
        SystemProperty.clearAllCache();
        hostname = null;
        hostIp = null;
        currentPid = null;
    }

     private static boolean isBlank(final String cs){
         return cs == null || cs.trim().isEmpty();
     }
}