package com.dream.common.core.enums;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 基础枚举接口
 * <p>
 * 所有业务枚举统一实现此接口，提供统一的 value、label 访问方式，
 * 以及丰富的通用工具方法。
 * <p>
 * 使用示例：
 * <pre>{@code
 * public enum StatusEnum implements BaseEnum<String> {
 *     ENABLED("enabled", "启用"),
 *     DISABLED("disabled", "禁用");
 *
 *     private final String value;
 *     private final String label;
 *
 *     StatusEnum(String value, String label) {
 *         this.value = value;
 *         this.label = label;
 *     }
 *
 *     public String getValue() { return value; }
 *     public String getLabel() { return label; }
 * }
 * }</pre>
 *
 * @param <V> 枚举值的类型（通常为 String 或 Integer）
 *
 * @author dream
 */
@SuppressWarnings("all")
public interface BaseEnum<V extends Serializable> {

    // ==================== 核心方法 ====================

    /**
     * 获取枚举的值（存储到数据库 / 传输给前端）
     * <p>
     * 推荐使用 {@link String} 或 {@link Integer} 类型：
     * <ul>
     *   <li><b>业务枚举（如状态、类型）</b>：推荐用 {@link String}，可读性好，便于扩展</li>
     *   <li><b>HTTP 状态码等标准枚举</b>：推荐用 {@link Integer}，遵循标准规范</li>
     * </ul>
     *
     * @return 枚举值
     */
    V getValue();

    /**
     * 获取枚举标签（展示给用户看的中文描述）
     *
     * @return 枚举标签
     */
    String getLabel();

    // ==================== 扩展方法（带默认实现） ====================

    /**
     * 获取枚举的描述（更详细的说明，默认同 label）
     * <p>
     * 可用于 Tooltip、日志等需要更详细信息的场景
     *
     * @return 枚举描述
     */
    default String getDescription() {
        return getLabel();
    }

    /**
     * 获取枚举的颜色（用于前端 UI 展示，如 Tag 的颜色，按钮 type 的颜色）
     * <p>
     * 子类可覆盖返回具体颜色值
     *
     * @return 颜色值（十六进制或 CSS 颜色名）
     */
    default String getColor() {
        return "";
    }

    /**
     * 获取枚举的图标名称（用于前端 UI 展示）
     *
     * @return 图标名称
     */
    default String getIcon() {
        return "";
    }

    /**
     * 获取枚举的排序权重（数值越小越靠前）
     * <p>
     * 默认使用枚举声明的顺序（ordinal）
     *
     * @return 排序权重
     */
    default int getOrder() {
        return ordinal();
    }

    /**
     * 判断枚举是否有效/可用
     * <p>
     * 默认所有枚举都有效，子类可覆盖实现
     *
     * @return 有效返回 true
     */
    default boolean isValid() {
        return true;
    }

    /**
     * 判断枚举是否已废弃/不推荐使用
     * <p>
     * 可用于标记枚举值不推荐使用，但仍保留以兼容历史数据
     *
     * @return 已废弃返回 true
     */
    default boolean isDeprecated() {
        return false;
    }

    // ==================== 枚举辅助方法（基于自身） ====================

    /**
     * 判断当前枚举是否等于指定的 value
     *
     * @param value 要比较的值
     * @return 相等返回 true
     */
    default boolean equalsValue(V value) {
        return Objects.equals(getValue(), value);
    }

    /**
     * 判断当前枚举是否等于指定的 value（忽略大小写，仅当 value 为 String 时有效）
     *
     * @param value 要比较的值
     * @return 相等返回 true
     */
    default boolean equalsValueIgnoreCase(String value) {
        if (value == null || getValue() == null) {
            return false;
        }
        return getValue().toString().equalsIgnoreCase(value);
    }

    /**
     * 获取当前枚举在枚举类中的声明顺序（0-based）
     *
     * @return 声明顺序
     */
    default int ordinal() {
        try {
            Enum<?> self = (Enum<?>) this;
            return self.ordinal();
        } catch (ClassCastException e) {
            return 0;
        }
    }

    /**
     * 获取当前枚举的名称（Enum.name()）
     *
     * @return 枚举名称
     */
    default String name() {
        try {
            Enum<?> self = (Enum<?>) this;
            return self.name();
        } catch (ClassCastException e) {
            return "UNKNOWN";
        }
    }

    // ==================== 静态工具方法 ====================

    /**
     * 根据 value 查找枚举
     *
     * <p>使用示例：
     * <pre>{@code
     * StatusEnum status = BaseEnum.fromValue(StatusEnum.class, "enabled");
     * }</pre>
     *
     * @param enumClass 枚举类
     * @param value     枚举值
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 匹配的枚举，未找到返回 null
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> E fromValue(Class<E> enumClass, V value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.equalsValue(value))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据 value 查找枚举，找不到时返回默认值
     *
     * @param enumClass    枚举类
     * @param value        枚举值
     * @param defaultValue 默认枚举
     * @param <E>          枚举类型
     * @param <V>          值类型
     * @return 匹配的枚举，未找到返回默认值
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> E fromValueOrDefault(
            Class<E> enumClass, V value, E defaultValue) {
        E result = fromValue(enumClass, value);
        return result != null ? result : defaultValue;
    }

    /**
     * 根据 label 查找枚举
     *
     * @param enumClass 枚举类
     * @param label     枚举标签
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 匹配的枚举，未找到返回 null
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> E fromLabel(Class<E> enumClass, String label) {
        if (label == null || label.isEmpty()) {
            return null;
        }
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.getLabel().equals(label))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据 label 查找枚举（忽略大小写）
     *
     * @param enumClass 枚举类
     * @param label     枚举标签
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 匹配的枚举，未找到返回 null
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> E fromLabelIgnoreCase(
            Class<E> enumClass, String label) {
        if (label == null || label.isEmpty()) {
            return null;
        }
        String lowerLabel = label.toLowerCase();
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.getLabel().toLowerCase().equals(lowerLabel))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据 name（枚举常量名）查找枚举
     *
     * @param enumClass 枚举类
     * @param name      枚举常量名
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 匹配的枚举，未找到返回 null
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> E fromName(Class<E> enumClass, String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 校验 value 是否存在
     *
     * @param enumClass 枚举类
     * @param value     要校验的值
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 存在返回 true
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> boolean exists(Class<E> enumClass, V value) {
        return fromValue(enumClass, value) != null;
    }

    /**
     * 校验 label 是否存在
     *
     * @param enumClass 枚举类
     * @param label     要校验的标签
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 存在返回 true
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> boolean existsLabel(Class<E> enumClass, String label) {
        return fromLabel(enumClass, label) != null;
    }

    /**
     * 获取所有枚举的 value 列表
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return value 列表
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> List<V> values(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(BaseEnum::getValue)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有枚举的 label 列表
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return label 列表
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> List<String> labels(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(BaseEnum::getLabel)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有有效枚举的 value 列表（过滤掉 isValid() = false 的枚举）
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 有效枚举的 value 列表
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> List<V> validValues(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(BaseEnum::isValid)
                .map(BaseEnum::getValue)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有枚举的 Map（value → label）
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return value → label 的 Map（保持枚举声明顺序）
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> Map<V, String> toMap(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .collect(Collectors.toMap(
                        BaseEnum::getValue,
                        BaseEnum::getLabel,
                        (old, newVal) -> old,
                        LinkedHashMap::new
                ));
    }

    /**
     * 获取所有枚举的 Map（value → 枚举对象）
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return value → 枚举对象的 Map（保持枚举声明顺序）
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> Map<V, E> toEnumMap(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .collect(Collectors.toMap(
                        BaseEnum::getValue,
                        Function.identity(),
                        (old, newVal) -> old,
                        LinkedHashMap::new
                ));
    }

    /**
     * 获取所有枚举的 Map（value → 枚举对象），带缓存
     * <p>
     * 适合高频调用场景，避免每次重新构建 Map
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return value → 枚举对象的 Map
     */
    @SuppressWarnings("unchecked")
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> Map<V, E> toEnumMapCached(Class<E> enumClass) {
        return (Map<V, E>) EnumCacheHolder.getCache(enumClass);
    }

    /**
     * 获取下拉列表所需的数据结构（常用于前端 Select 组件）
     * <p>
     * 使用示例：
     * <pre>{@code
     * List<BaseEnum.EnumOption<String>> options = BaseEnum.options(StatusEnum.class);
     * // 返回 [{value: "enabled", label: "启用"}, {value: "disabled", label: "禁用"}]
     * }</pre>
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 下拉列表数据（仅包含有效枚举）
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> List<EnumOption<V>> options(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(BaseEnum::isValid)
                .map(e -> new EnumOption<>(e.getValue(), e.getLabel()))
                .collect(Collectors.toList());
    }

    /**
     * 获取下拉列表（包含所有枚举，包括无效的）
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 下拉列表数据
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> List<EnumOption<V>> optionsAll(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(e -> new EnumOption<>(e.getValue(), e.getLabel()))
                .collect(Collectors.toList());
    }

    /**
     * 获取下拉列表（包含 color 和 deprecated 信息）
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 下拉列表数据（含颜色和废弃标记）
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> List<EnumOptionFull<V>> optionsFull(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(BaseEnum::isValid)
                .map(e -> new EnumOptionFull<>(e.getValue(), e.getLabel(), e.getColor(), e.isDeprecated()))
                .collect(Collectors.toList());
    }

    /**
     * 按条件过滤枚举
     *
     * @param enumClass 枚举类
     * @param predicate 过滤条件
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 过滤后的枚举列表
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> List<E> filter(
            Class<E> enumClass, Predicate<E> predicate) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(predicate)
                .collect(Collectors.toList());
    }

    /**
     * 判断指定的 value 是否在给定的枚举值集合中
     *
     * @param enumClass     枚举类
     * @param value         要校验的值
     * @param allowedValues 允许的枚举值列表
     * @param <E>           枚举类型
     * @param <V>           值类型
     * @return 在允许列表中返回 true
     */
    @SafeVarargs
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> boolean isAllowed(
            Class<E> enumClass, V value, E... allowedValues) {
        if (value == null || allowedValues == null || allowedValues.length == 0) {
            return false;
        }
        Set<V> allowedSet = Arrays.stream(allowedValues)
                .map(BaseEnum::getValue)
                .collect(Collectors.toSet());
        return allowedSet.contains(value);
    }

    // ==================== 内部类 ====================

    /**
     * 枚举缓存（线程安全）
     */
    final class EnumCacheHolder {
        private static final Map<Class<?>, Map<?, ?>> CACHE = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> Map<V, E> getCache(Class<E> enumClass) {
            return (Map<V, E>) CACHE.computeIfAbsent(enumClass, cls -> {
                E[] constants = (E[]) cls.getEnumConstants();
                Map<V, E> map = new LinkedHashMap<>();
                for (E e : constants) {
                    map.put(e.getValue(), e);
                }
                return map;
            });
        }

        /**
         * 清空所有缓存
         */
        public static void clear() {
            CACHE.clear();
        }

        /**
         * 清空指定枚举类的缓存
         */
        public static void clear(Class<?> enumClass) {
            CACHE.remove(enumClass);
        }
    }

    /**
     * 下拉选项类（基础版）
     */
    final class EnumOption<V> {
        private final V value;
        private final String label;

        public EnumOption(V value, String label) {
            this.value = value;
            this.label = label;
        }

        public V getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }

    /**
     * 下拉选项类（完整版，含颜色和废弃标记）
     */
    final class EnumOptionFull<V> {
        private final V value;
        private final String label;
        private final String color;
        private final boolean deprecated;

        public EnumOptionFull(V value, String label, String color, boolean deprecated) {
            this.value = value;
            this.label = label;
            this.color = color;
            this.deprecated = deprecated;
        }

        public V getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }

        public String getColor() {
            return color;
        }

        public boolean isDeprecated() {
            return deprecated;
        }
    }
}