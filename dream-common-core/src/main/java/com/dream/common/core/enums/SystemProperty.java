package com.dream.common.core.enums;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 系统属性枚举
 * <p>
 * 统一管理 Java 系统属性和路径分隔符
 * <p>
 * 性能优化：
 * <ul>
 *   <li>枚举值不存储属性值，减少内存占用</li>
 *   <li>使用 ConcurrentHashMap 缓存属性值，避免重复调用 System.getProperty()</li>
 *   <li>操作系统判断结果缓存，避免重复字符串比较</li>
 *   <li>提供 forceRefresh() 方法支持强制刷新缓存</li>
 * </ul>
 *
 * @author dream
 */
public enum SystemProperty implements BaseEnum<String> {

    // ==================== 路径分隔符 ====================

    /**
     * 系统行分隔符（\n / \r\n）
     */
    LINE_SEPARATOR("line.separator", "行分隔符", System::lineSeparator),

    /**
     * 文件路径分隔符（/ 或 \）
     */
    FILE_SEPARATOR("file.separator", "文件分隔符", () -> FileSystems.getDefault().getSeparator()),

    /**
     * 路径列表分隔符（; 或 :）
     */
    PATH_SEPARATOR("path.separator", "路径列表分隔符", () -> File.pathSeparator),

    // ==================== 目录路径 ====================

    /**
     * 用户当前工作目录
     */
    USER_DIR("user.dir", "用户工作目录", () -> System.getProperty("user.dir")),

    /**
     * 用户主目录
     */
    USER_HOME("user.home", "用户主目录", () -> System.getProperty("user.home")),

    // ==================== Java 环境 ====================

    /**
     * Java 安装目录
     */
    JAVA_HOME("java.home", "Java安装目录", () -> System.getProperty("java.home")),

    /**
     * Java 版本号
     */
    JAVA_VERSION("java.version", "Java版本", () -> System.getProperty("java.version")),

    /**
     * Java 运行时版本（详细）
     */
    JAVA_RUNTIME_VERSION("java.runtime.version", "Java运行时版本", () -> System.getProperty("java.runtime.version")),

    /**
     * Java 虚拟机名称
     */
    JAVA_VM_NAME("java.vm.name", "Java虚拟机名称", () -> System.getProperty("java.vm.name")),

    /**
     * Java 虚拟机版本
     */
    JAVA_VM_VERSION("java.vm.version", "Java虚拟机版本", () -> System.getProperty("java.vm.version")),

    /**
     * Java 规范版本
     */
    JAVA_SPECIFICATION_VERSION("java.specification.version", "Java规范版本", () -> System.getProperty("java.specification.version")),

    // ==================== 操作系统 ====================

    /**
     * 操作系统名称
     */
    OS_NAME("os.name", "操作系统名称", () -> System.getProperty("os.name")),

    /**
     * 操作系统版本
     */
    OS_VERSION("os.version", "操作系统版本", () -> System.getProperty("os.version")),

    /**
     * 操作系统架构
     */
    OS_ARCH("os.arch", "操作系统架构", () -> System.getProperty("os.arch")),

    // ==================== 其他 ====================

    /**
     * 临时目录
     */
    TMP_DIR("java.io.tmpdir", "临时目录", () -> System.getProperty("java.io.tmpdir")),

    /**
     * 文件编码
     */
    FILE_ENCODING("file.encoding", "文件编码", () -> Charset.defaultCharset().displayName()),

    /**
     * 当前用户名称
     */
    USER_NAME("user.name", "当前用户名", () -> System.getProperty("user.name"));

    // ==================== 字段 ====================

    /**
     * 系统属性 Key
     */
    private final String key;

    /**
     * 中文标签
     */
    private final String label;

    /**
     * 值提供者（懒加载）
     */
    private final Supplier<String> valueSupplier;

    /**
     * 缓存（用于存储实际值）
     */
    private static final ConcurrentHashMap<SystemProperty, String> CACHE = new ConcurrentHashMap<>();

    // ==================== 构造器 ====================

    SystemProperty(String key, String label, Supplier<String> valueSupplier) {
        this.key = key;
        this.label = label;
        this.valueSupplier = valueSupplier;
    }

    // ==================== BaseEnum 实现 ====================

    @Override
    public String getValue() {
        return getProperty();
    }

    @Override
    public String getLabel() {
        return label;
    }

    // ==================== 核心方法 ====================

    /**
     * 获取系统属性值（带缓存）
     * <p>
     * 首次调用时读取并缓存，后续直接从缓存返回
     *
     * @return 系统属性值
     */
    public String getProperty() {
        return CACHE.computeIfAbsent(this, k -> {
            String value = valueSupplier.get();
            return value != null ? value : "";
        });
    }

    /**
     * 获取系统属性 Key
     */
    public String getKey() {
        return key;
    }

    // ==================== 缓存管理 ====================

    /**
     * 强制刷新指定枚举的缓存
     * <p>
     * 当系统属性在运行时发生变化时调用
     *
     * @return 刷新后的值
     */
    public String refresh() {
        String value = valueSupplier.get();
        String result = value != null ? value : "";
        CACHE.put(this, result);
        return result;
    }

    /**
     * 强制刷新所有枚举的缓存
     */
    public static void refreshAll() {
        for (SystemProperty item : values()) {
            item.refresh();
        }
    }

    /**
     * 清空指定枚举的缓存（下次访问时重新加载）
     */
    public void clearCache() {
        CACHE.remove(this);
    }

    /**
     * 清空所有缓存
     */
    public static void clearAllCache() {
        CACHE.clear();
    }

    // ==================== 操作系统判断（带缓存） ====================

    private static Boolean osWindows = null;
    private static Boolean osLinux = null;
    private static Boolean osMac = null;

    /**
     * 判断当前操作系统是否为 Windows
     */
    public static boolean isWindows() {
        if (osWindows == null) {
            osWindows = OS_NAME.getProperty().toLowerCase().startsWith("windows");
        }
        return osWindows;
    }

    /**
     * 判断当前操作系统是否为 Linux
     */
    public static boolean isLinux() {
        if (osLinux == null) {
            osLinux = OS_NAME.getProperty().toLowerCase().startsWith("linux");
        }
        return osLinux;
    }

    /**
     * 判断当前操作系统是否为 Mac
     */
    public static boolean isMac() {
        if (osMac == null) {
            osMac = OS_NAME.getProperty().toLowerCase().startsWith("mac");
        }
        return osMac;
    }

    /**
     * 判断当前操作系统是否为 Unix 类（Linux/Mac）
     */
    public static boolean isUnixLike() {
        return isLinux() || isMac();
    }

    // ==================== 常用属性快捷方法 ====================

    /**
     * 获取当前系统换行符
     */
    public static String getLineSeparator() {
        return LINE_SEPARATOR.getProperty();
    }

    /**
     * 获取当前系统文件分隔符
     */
    public static String getFileSeparator() {
        return FILE_SEPARATOR.getProperty();
    }

    /**
     * 获取当前系统路径分隔符
     */
    public static String getPathSeparator() {
        return PATH_SEPARATOR.getProperty();
    }

    /**
     * 获取用户工作目录
     */
    public static String getUserDir() {
        return USER_DIR.getProperty();
    }

    /**
     * 获取用户主目录
     */
    public static String getUserHome() {
        return USER_HOME.getProperty();
    }

    /**
     * 获取临时目录
     */
    public static String getTempDir() {
        return TMP_DIR.getProperty();
    }

    // ==================== 静态方法 ====================

    public static SystemProperty fromKey(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        for (SystemProperty item : values()) {
            if (item.key.equals(key)) {
                return item;
            }
        }
        return null;
    }

    public static SystemProperty fromValue(String value) {
        return BaseEnum.fromValue(SystemProperty.class, value);
    }
}