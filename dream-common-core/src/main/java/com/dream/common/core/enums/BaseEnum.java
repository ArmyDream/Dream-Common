package com.dream.common.core.enums;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 基础枚举接口。
 *
 * <p>所有业务枚举实现此接口，统一暴露 {@code value}（存储值）与
 * {@code label}（展示名）两个核心契约，并复用接口静态方法提供的
 * 查找、列表、字典、下拉等能力。</p>
 *
 * <h2>核心契约（业务枚举必须实现）</h2>
 * <ul>
 *   <li>{@link #getValue()} —— 存储值</li>
 *   <li>{@link #getLabel()} —— 展示名</li>
 * </ul>
 *
 * <h2>扩展契约（子类按需覆盖）</h2>
 * <ul>
 *   <li>{@link #getDescription()} —— 详细描述，默认同 label</li>
 *   <li>{@link #getColor()} —— UI 颜色，默认空串</li>
 *   <li>{@link #getIcon()} —— UI 图标，默认空串</li>
 *   <li>{@link #getOrder()} —— 排序权重，默认枚举声明顺序</li>
 *   <li>{@link #isValid()} —— 是否有效，默认 {@code true}</li>
 * </ul>
 *
 * <h2>命名规范</h2>
 * <ul>
 *   <li>实例方法使用 {@code getXxx} / {@code isXxx} 前缀（Java Bean 规范）</li>
 *   <li>匹配方法命名对齐 Spring {@code HttpMethod}（{@code matches} / {@code resolve}）</li>
 *   <li>查找方法使用 {@code fromXxx} 前缀，避免与 JDK {@code Enum.valueOf} 冲突</li>
 *   <li>列表方法统一以 {@code List} 结尾（{@code enumList} / {@code valueList} / {@code labelList}）</li>
 *   <li>字典方法以 {@code Map} 结尾，并在名称中体现源与目标（{@code valueLabelMap} / {@code valueEnumMap}）</li>
 * </ul>
 *
 * <h2>实现示例</h2>
 * <pre>{@code
 * public enum StatusEnum implements BaseEnum<Integer> {
 *     ENABLED(1, "启用"),
 *     DISABLED(0, "禁用");
 *
 *     private final Integer value;
 *     private final String label;
 *
 *     StatusEnum(Integer value, String label) {
 *         this.value = value;
 *         this.label = label;
 *     }
 *
 *     @Override public Integer getValue() { return value; }
 *     @Override public String getLabel() { return label; }
 * }
 * }</pre>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // ========== 实例方法 ==========
 * StatusEnum.ENABLED.matches("ENABLED");                    // true
 * StatusEnum.ENABLED.equalsValue(1);                        // true
 * StatusEnum.ENABLED.getOrder();                            // 0（声明顺序）
 * StatusEnum.ENABLED.isValid();                             // true
 *
 * // ========== 静态查找 ==========
 * StatusEnum a = BaseEnum.fromValue(StatusEnum.class, 1);                    // ENABLED
 * StatusEnum b = BaseEnum.fromValueOrDefault(StatusEnum.class, 99, StatusEnum.DISABLED);
 * StatusEnum c = BaseEnum.resolve(StatusEnum.class, "ENABLED");              // ENABLED
 * StatusEnum d = BaseEnum.fromLabel(StatusEnum.class, "启用");                // ENABLED
 *
 * // ========== 静态便捷 ==========
 * String label = BaseEnum.labelOf(StatusEnum.class, 1);                       // "启用"
 * boolean ok = BaseEnum.contains(StatusEnum.class, 1);                        // true
 *
 * // ========== 静态列表 ==========
 * List<StatusEnum> all = BaseEnum.enumList(StatusEnum.class);                 // [ENABLED, DISABLED]
 * List<Integer> values = BaseEnum.valueList(StatusEnum.class);                // [1, 0]
 * List<String> labels = BaseEnum.labelList(StatusEnum.class);                 // ["启用", "禁用"]
 *
 * // ========== 静态字典 ==========
 * Map<Integer, String> dict = BaseEnum.valueLabelMap(StatusEnum.class);
 * Map<Integer, StatusEnum> enumMap = BaseEnum.valueEnumMap(StatusEnum.class);
 *
 * // ========== 静态下拉 / 过滤 ==========
 * List<BaseEnum.Option<Integer>> opts = BaseEnum.options(StatusEnum.class);
 * List<StatusEnum> enableds = BaseEnum.filter(StatusEnum.class, StatusEnum::isValid);
 *
 * 根据任意字段获取枚举
 * StatusEnum s1 = BaseEnum.fromField(StatusEnum.class, StatusEnum::getLabel, "启用");
 * StatusEnum s2 = BaseEnum.fromField(StatusEnum.class, StatusEnum::getColor, "#52c41a");
 * StatusEnum s3 = BaseEnum.fromField(StatusEnum.class, StatusEnum::getCode, "SYS_01");
 *
 * }</pre>
 *
 * <h2>Java 8 静态方法说明</h2>
 * <p>受 Java 语言限制，接口静态方法无法引用接口自身的类型参数 {@code V}，
 * 因此每个静态方法均独立声明 {@code <E extends Enum<E> & BaseEnum<V>, V extends Serializable>}。
 * 这是 Java 8 下的固有约束，调用侧由编译器自动推断，无需关心。</p>
 *
 * @param <V> 枚举值类型（推荐 {@link String} 或 {@link Integer}）
 * @author dream
 * @since 2026-09-10
 */
public interface BaseEnum<V extends Serializable> {

    // ==================== 核心契约 ====================

    /**
     * 获取枚举的值（存储到数据库 / 传输给前端）。
     * <p>推荐使用 {@link String} 或 {@link Integer}：</p>
     * <ul>
     *   <li><b>业务枚举（状态、类型等）</b>：推荐 {@link String}，可读性好、便于扩展</li>
     *   <li><b>标准枚举（HTTP 状态码等）</b>：推荐 {@link Integer}，遵循规范</li>
     * </ul>
     *
     * @return 枚举值
     */
    V getValue();

    /**
     * 获取枚举标签（展示给用户看的中文描述）。
     *
     * @return 枚举标签
     */
    String getLabel();

    // ==================== 扩展属性（子类按需覆盖） ====================

    /**
     * 获取详细描述，默认同 {@link #getLabel()}。
     * <p>用于 Tooltip、日志等需要更详细信息的场景。</p>
     *
     * @return 描述
     */
    default String getDescription() {
        return getLabel();
    }

    /**
     * 获取颜色值（前端 UI 展示，如 Tag 颜色、按钮类型）。
     * <p>子类可覆盖返回具体值，默认返回空串。</p>
     *
     * @return 颜色值（十六进制或 CSS 颜色名）
     */
    default String getColor() {
        return "";
    }

    /**
     * 获取图标名称（前端 UI 展示）。
     * <p>子类可覆盖返回具体值，默认返回空串。</p>
     *
     * @return 图标名
     */
    default String getIcon() {
        return "";
    }

    /**
     * 获取排序权重（数值越小越靠前）。
     * <p>默认使用枚举声明顺序（{@link Enum#ordinal()}）。</p>
     *
     * @return 排序权重
     */
    default int getOrder() {
        return ((Enum<?>) this).ordinal();
    }

    /**
     * 判断枚举是否有效/可用。
     * <p>默认所有枚举都有效，子类可覆盖实现以隐藏部分枚举。</p>
     *
     * @return 有效返回 {@code true}
     */
    default boolean isValid() {
        return true;
    }

    // ==================== 实例匹配 ====================

    /**
     * 判断当前枚举的常量名是否与给定字符串严格相等。
     *
     * <p>参考 Spring {@code HttpMethod#matches(String)} 的语义，用于将枚举与
     * 字符串做<b>区分大小写</b>的名称匹配。</p>
     *
     * <pre>{@code
     * StatusEnum.ENABLED.matches("ENABLED");   // true
     * StatusEnum.ENABLED.matches("enabled");   // false
     * StatusEnum.ENABLED.matches(null);        // false
     * }</pre>
     *
     * @param name 待比较的名称，可为 {@code null}
     * @return 匹配返回 {@code true}；{@code name} 为 {@code null} 时返回 {@code false}
     */
    default boolean matches(String name) {
        return name != null && name.equals(((Enum<?>) this).name());
    }

    /**
     * 判断当前枚举的 value 是否与给定值严格相等。
     *
     * <pre>{@code
     * StatusEnum.ENABLED.equalsValue(1);   // true
     * StatusEnum.ENABLED.equalsValue(0);   // false
     * }</pre>
     *
     * @param value 待比较的值，可为 {@code null}
     * @return 相等返回 {@code true}
     */
    default boolean equalsValue(Object value) {
        return Objects.equals(getValue(), value);
    }

    // ==================== 静态查找 ====================

    /**
     * 根据 value 查找枚举。
     *
     * <p>value 参数支持任意类型，内部以字符串形式归一化比较，
     * 因此 {@code 1}、{@code 1L}、{@code "1"} 都能命中同一个枚举。</p>
     *
     * <pre>{@code
     * StatusEnum s = BaseEnum.fromValue(StatusEnum.class, 1);      // ENABLED
     * StatusEnum s2 = BaseEnum.fromValue(StatusEnum.class, "1");   // ENABLED（等价）
     * }</pre>
     *
     * @param enumClass 枚举类
     * @param value     枚举值（可为 {@code null}）
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 匹配的枚举；未找到或参数为 {@code null} 时返回 {@code null}
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> E fromValue(
            Class<E> enumClass, Object value) {
        if (enumClass == null || value == null) {
            return null;
        }
        String key = String.valueOf(value);
        for (E e : enumClass.getEnumConstants()) {
            V v = e.getValue();
            if (v != null && key.equals(String.valueOf(v))) {
                return e;
            }
        }
        return null;
    }

    /**
     * 根据 value 查找枚举，未找到时返回默认值。
     * <p>命名遵循 JDK {@link Map#getOrDefault(Object, Object)} 惯例。</p>
     *
     * @param enumClass    枚举类
     * @param value        枚举值
     * @param defaultValue 默认枚举（可为 {@code null}）
     * @param <E>          枚举类型
     * @param <V>          值类型
     * @return 匹配的枚举；未找到返回 {@code defaultValue}
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> E fromValueOrDefault(
            Class<E> enumClass, Object value, E defaultValue) {
        E result = fromValue(enumClass, value);
        return result != null ? result : defaultValue;
    }

    /**
     * 解析字符串为枚举常量。
     *
     * <p>命名对齐 Spring {@code HttpMethod#resolve(String)} 的语义，
     * 用于将字符串形式的枚举名解析为枚举对象。典型场景是校验前端传入的字符串是否合法。</p>
     *
     * <pre>{@code
     * StatusEnum s = BaseEnum.resolve(StatusEnum.class, "ENABLED");   // ENABLED
     * StatusEnum x = BaseEnum.resolve(StatusEnum.class, "UNKNOWN");   // null
     * }</pre>
     *
     * @param enumClass 枚举类
     * @param name      枚举常量名（区分大小写）
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 匹配的枚举；未找到或参数为 {@code null} 时返回 {@code null}
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> E resolve(
            Class<E> enumClass, String name) {
        if (enumClass == null || name == null || name.isEmpty()) {
            return null;
        }
        for (E e : enumClass.getEnumConstants()) {
            if (name.equals(e.name())) {
                return e;
            }
        }
        return null;
    }

    /**
     * 根据 label 查找枚举（区分大小写）。
     *
     * @param enumClass 枚举类
     * @param label     枚举标签
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 匹配的枚举；未找到或参数为 {@code null} 时返回 {@code null}
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> E fromLabel(
            Class<E> enumClass, String label) {
        if (enumClass == null || label == null || label.isEmpty()) {
            return null;
        }
        for (E e : enumClass.getEnumConstants()) {
            if (label.equals(e.getLabel())) {
                return e;
            }
        }
        return null;
    }

    /**
     * 根据枚举的任意字段值查找枚举（返回首个匹配项）。
     *
     * <p>通过 {@link Function} 提取器指定要比较的字段，支持 {@link BaseEnum} 的
     * 任意契约字段（label / color / icon / order / description）以及枚举自身
     * 的扩展字段。</p>
     *
     * <p>若字段值可能对应多个枚举，只返回<b>声明顺序最靠前</b>的一个；
     * 如需全部结果，请使用 {@link #fromFieldList(Class, Function, Object)}。</p>
     *
     * <pre>{@code
     * StatusEnum s1 = BaseEnum.fromField(StatusEnum.class, StatusEnum::getLabel, "启用");
     * StatusEnum s2 = BaseEnum.fromField(StatusEnum.class, StatusEnum::getColor, "#52c41a");
     * StatusEnum s3 = BaseEnum.fromField(StatusEnum.class, StatusEnum::getCode, "SYS_01");
     * }</pre>
     *
     * @param enumClass  枚举类
     * @param extractor  字段提取器（方法引用）
     * @param fieldValue 要匹配的字段值
     * @param <E>        枚举类型
     * @param <V>        值类型
     * @param <F>        字段类型
     * @return 首个匹配的枚举；未找到或参数为 {@code null} 时返回 {@code null}
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable, F> E fromField(
            Class<E> enumClass, Function<E, F> extractor, F fieldValue) {
        List<E> list = fromFieldList(enumClass, extractor, fieldValue);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 根据枚举的任意字段值查找枚举，未找到时返回默认值。
     *
     * @param enumClass    枚举类
     * @param extractor    字段提取器
     * @param fieldValue   要匹配的字段值
     * @param defaultValue 默认枚举（可为 {@code null}）
     * @param <E>          枚举类型
     * @param <V>          值类型
     * @param <F>          字段类型
     * @return 首个匹配的枚举；未找到返回 {@code defaultValue}
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable, F> E fromFieldOrDefault(
            Class<E> enumClass, Function<E, F> extractor, F fieldValue, E defaultValue) {
        E result = fromField(enumClass, extractor, fieldValue);
        return result != null ? result : defaultValue;
    }

    /**
     * 根据枚举的任意字段值查找所有匹配的枚举（返回列表）。
     *
     * <p>适用于字段值可能重复的场景，例如多个枚举共享同一个 color、同一个
     * group、同一个业务 code 等。返回结果<b>保持枚举声明顺序</b>，且永不为
     * {@code null}（无匹配时返回空列表）。</p>
     *
     * <pre>{@code
     * // 查找所有 icon 为 "warning" 的枚举
     * List<StatusEnum> list = BaseEnum.fromFieldList(
     *         StatusEnum.class, StatusEnum::getIcon, "warning");
     *
     * // 查找所有属于同一分组的枚举
     * List<StatusEnum> group = BaseEnum.fromFieldList(
     *         StatusEnum.class, StatusEnum::getGroup, "system");
     * }</pre>
     *
     * @param enumClass  枚举类
     * @param extractor  字段提取器
     * @param fieldValue 要匹配的字段值
     * @param <E>        枚举类型
     * @param <V>        值类型
     * @param <F>        字段类型
     * @return 匹配的枚举列表（保持声明顺序）；参数为 {@code null} 时返回空列表
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable, F> List<E> fromFieldList(
            Class<E> enumClass, Function<E, F> extractor, F fieldValue) {
        if (enumClass == null || extractor == null || fieldValue == null) {
            return Collections.emptyList();
        }
        List<E> result = new ArrayList<>();
        for (E e : enumClass.getEnumConstants()) {
            if (fieldValue.equals(extractor.apply(e))) {
                result.add(e);
            }
        }
        return result;
    }

    /**
     * 根据枚举的字符串字段值查找枚举（忽略大小写，返回首个匹配项）。
     *
     * <p>适用于按 label、color 等字符串字段做大小写不敏感匹配的场景。
     * 若字段值可能对应多个枚举，只返回<b>声明顺序最靠前</b>的一个；
     * 如需全部结果，请使用 {@link #fromFieldIgnoreCaseList(Class, Function, String)}。</p>
     *
     * <pre>{@code
     * StatusEnum s = BaseEnum.fromFieldIgnoreCase(
     *         StatusEnum.class, StatusEnum::getLabel, "启用");
     * }</pre>
     *
     * @param enumClass  枚举类
     * @param extractor  字符串字段提取器（返回 {@link String}）
     * @param fieldValue 要匹配的字段值（忽略大小写）
     * @param <E>        枚举类型
     * @param <V>        值类型
     * @return 首个匹配的枚举；未找到或参数为 {@code null} 时返回 {@code null}
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> E fromFieldIgnoreCase(
            Class<E> enumClass, Function<E, String> extractor, String fieldValue) {
        List<E> list = fromFieldIgnoreCaseList(enumClass, extractor, fieldValue);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 根据枚举的字符串字段值查找所有匹配的枚举（忽略大小写，返回列表）。
     *
     * <p>返回结果<b>保持枚举声明顺序</b>，且永不为 {@code null}（无匹配时返回空列表）。</p>
     *
     * @param enumClass  枚举类
     * @param extractor  字符串字段提取器（返回 {@link String}）
     * @param fieldValue 要匹配的字段值（忽略大小写）
     * @param <E>        枚举类型
     * @param <V>        值类型
     * @return 匹配的枚举列表（保持声明顺序）；参数为 {@code null} 时返回空列表
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> List<E> fromFieldIgnoreCaseList(
            Class<E> enumClass, Function<E, String> extractor, String fieldValue) {
        if (enumClass == null || extractor == null || fieldValue == null) {
            return Collections.emptyList();
        }
        List<E> result = new ArrayList<>();
        for (E e : enumClass.getEnumConstants()) {
            String extracted = extractor.apply(e);
            if (extracted != null && extracted.equalsIgnoreCase(fieldValue)) {
                result.add(e);
            }
        }
        return result;
    }

    // ==================== 静态便捷 ====================

    /**
     * 根据 value 获取 label（业务最高频操作）。
     * <p>命名参考 JDK {@code Optional.of} / {@code Map.of} 的 {@code of} 语义。</p>
     *
     * <pre>{@code
     * String label = BaseEnum.labelOf(StatusEnum.class, 1);   // "启用"
     * }</pre>
     *
     * @param enumClass 枚举类
     * @param value     枚举值
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return label；未找到返回 {@code null}
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> String labelOf(
            Class<E> enumClass, Object value) {
        E e = fromValue(enumClass, value);
        return e != null ? e.getLabel() : null;
    }

    /**
     * 判断给定 value 是否存在对应枚举。
     * <p>命名对齐 {@link java.util.Collection#contains(Object)}。</p>
     *
     * @param enumClass 枚举类
     * @param value     待校验的值
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 存在返回 {@code true}
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> boolean contains(
            Class<E> enumClass, Object value) {
        return fromValue(enumClass, value) != null;
    }

    // ==================== 静态列表 ====================

    /**
     * 获取所有枚举对象（保持声明顺序）。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 枚举对象列表；{@code enumClass} 为 {@code null} 时返回空列表
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> List<E> enumList(
            Class<E> enumClass) {
        if (enumClass == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(enumClass.getEnumConstants());
    }

    /**
     * 获取所有枚举的 value 列表（保持声明顺序）。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return value 列表；{@code enumClass} 为 {@code null} 时返回空列表
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> List<V> valueList(
            Class<E> enumClass) {
        if (enumClass == null) {
            return Collections.emptyList();
        }
        E[] constants = enumClass.getEnumConstants();
        List<V> result = new ArrayList<>(constants.length);
        for (E e : constants) {
            result.add(e.getValue());
        }
        return result;
    }

    /**
     * 获取所有枚举的 label 列表（保持声明顺序）。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return label 列表；{@code enumClass} 为 {@code null} 时返回空列表
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> List<String> labelList(
            Class<E> enumClass) {
        if (enumClass == null) {
            return Collections.emptyList();
        }
        E[] constants = enumClass.getEnumConstants();
        List<String> result = new ArrayList<>(constants.length);
        for (E e : constants) {
            result.add(e.getLabel());
        }
        return result;
    }

    // ==================== 静态字典 ====================

    /**
     * 获取 {@code value → label} 的字典（保持声明顺序）。
     *
     * <pre>{@code
     * Map<Integer, String> dict = BaseEnum.valueLabelMap(StatusEnum.class);
     * // {1="启用", 0="禁用"}
     * }</pre>
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return value → label；{@code enumClass} 为 {@code null} 时返回空 Map
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> Map<V, String> valueLabelMap(
            Class<E> enumClass) {
        Map<V, String> map = new LinkedHashMap<>();
        if (enumClass == null) {
            return map;
        }
        for (E e : enumClass.getEnumConstants()) {
            map.putIfAbsent(e.getValue(), e.getLabel());
        }
        return map;
    }

    /**
     * 获取 {@code value → 枚举对象} 的字典（保持声明顺序）。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return value → 枚举对象；{@code enumClass} 为 {@code null} 时返回空 Map
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> Map<V, E> valueEnumMap(
            Class<E> enumClass) {
        Map<V, E> map = new LinkedHashMap<>();
        if (enumClass == null) {
            return map;
        }
        for (E e : enumClass.getEnumConstants()) {
            map.putIfAbsent(e.getValue(), e);
        }
        return map;
    }

    // ==================== 静态下拉列表 ====================

    /**
     * 获取下拉列表（常用于前端 Select 组件）。
     *
     * <p>返回的每个 {@link Option} 包含枚举的全部契约字段
     * （value / label / description / color / icon / order / valid）。</p>
     *
     * <pre>{@code
     * List<BaseEnum.Option<Integer>> options = BaseEnum.options(StatusEnum.class);
     * // [{value: 1, label: "启用", description: "启用", color: "#52c41a", ...}, ...]
     * }</pre>
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 下拉选项列表；{@code enumClass} 为 {@code null} 时返回空列表
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> List<Option<V>> options(
            Class<E> enumClass) {
        if (enumClass == null) {
            return Collections.emptyList();
        }
        E[] constants = enumClass.getEnumConstants();
        List<Option<V>> list = new ArrayList<>(constants.length);
        for (E e : constants) {
            Option<V> from = Option.from(e);
            if (from != null) {
                list.add(from);
            }
        }
        return list;
    }

    // ==================== 静态过滤 ====================

    /**
     * 按条件过滤枚举。
     * <p>命名对齐 {@link java.util.stream.Stream#filter(Predicate)}。</p>
     *
     * @param enumClass 枚举类
     * @param predicate 过滤条件
     * @param <E>       枚举类型
     * @param <V>       值类型
     * @return 过滤后的枚举列表；{@code enumClass} 或 {@code predicate} 为 {@code null} 时返回空列表
     */
    static <E extends Enum<E> & BaseEnum<V>, V extends Serializable> List<E> filter(
            Class<E> enumClass, Predicate<E> predicate) {
        if (enumClass == null || predicate == null) {
            return Collections.emptyList();
        }
        List<E> result = new ArrayList<>();
        for (E e : enumClass.getEnumConstants()) {
            if (predicate.test(e)) {
                result.add(e);
            }
        }
        return result;
    }

    // ==================== 内部类 ====================

    /**
     * 下拉选项（前端 Select / Tree / Tag 等组件使用）。
     *
     * <p>字段与 {@link BaseEnum} 的契约一一对应，覆盖前端展示所需的全部信息：
     * 值、标签、描述、颜色、图标、排序、是否有效。</p>
     *
     * <h2>构造方式</h2>
     * <ul>
     *   <li>{@link #from(Enum)} —— 从枚举对象直接构建（推荐，无需手写字段）</li>
     *   <li>{@link #Option(Serializable, String)} —— 最简：值 + 标签</li>
     *   <li>{@link #Option(Serializable, String, String)} —— 值 + 标签 + 描述</li>
     *   <li>{@link #Option(Serializable, String, String, String)} —— 值 + 标签 + 颜色 + 图标</li>
     *   <li>{@link #Option(Serializable, String, String, String, String, int, boolean)} —— 完整</li>
     * </ul>
     *
     * <h2>使用示例</h2>
     * <pre>{@code
     * // 方式 1：批量从枚举类构建
     * List<BaseEnum.Option<Integer>> options = BaseEnum.options(StatusEnum.class);
     *
     * // 方式 2：单个枚举构建
     * BaseEnum.Option<Integer> opt = BaseEnum.Option.from(StatusEnum.ENABLED);
     *
     * // 方式 3：手工构建
     * BaseEnum.Option<Integer> custom = new BaseEnum.Option<>(1, "启用");
     * }</pre>
     *
     * @param <V> 枚举值类型
     */
    class Option<V extends Serializable> {

        /**
         * 枚举值（前端提交给后端的值）。
         */
        private final V value;

        /**
         * 显示标签（下拉项文本）。
         */
        private final String label;

        /**
         * 详细描述（用于 Tooltip、悬浮说明）。
         */
        private final String description;

        /**
         * UI 颜色（Tag、Badge 等组件的颜色值）。
         */
        private final String color;

        /**
         * UI 图标（前端图标名称）。
         */
        private final String icon;

        /**
         * 排序权重（数值越小越靠前）。
         */
        private final int order;

        /**
         * 是否有效（无效项默认不展示，或展示为禁用态）。
         */
        private final boolean valid;

        /**
         * 最简构造器：仅 value + label。
         * <p>其余字段使用默认值：description 同 label，color/icon 为空串，
         * order 为 0，valid 为 {@code true}。</p>
         *
         * @param value 枚举值
         * @param label 显示标签
         */
        public Option(V value, String label) {
            this(value, label, label, "", "", 0, true);
        }

        /**
         * 常用构造器：value + label + description。
         * <p>其余字段使用默认值：color/icon 为空串，order 为 0，valid 为 {@code true}。</p>
         *
         * @param value       枚举值
         * @param label       显示标签
         * @param description 详细描述
         */
        public Option(V value, String label, String description) {
            this(value, label, description, "", "", 0, true);
        }

        /**
         * 常用构造器：value + label + color + icon。
         * <p>description 同 label，order 为 0，valid 为 {@code true}。</p>
         *
         * @param value 枚举值
         * @param label 显示标签
         * @param color UI 颜色
         * @param icon  UI 图标
         */
        public Option(V value, String label, String color, String icon) {
            this(value, label, label, color, icon, 0, true);
        }

        /**
         * 完整构造器：所有字段。
         *
         * @param value       枚举值
         * @param label       显示标签
         * @param description 详细描述
         * @param color       UI 颜色
         * @param icon        UI 图标
         * @param order       排序权重
         * @param valid       是否有效
         */
        public Option(V value, String label, String description,
                      String color, String icon, int order, boolean valid) {
            this.value = value;
            this.label = label;
            this.description = description;
            this.color = color;
            this.icon = icon;
            this.order = order;
            this.valid = valid;
        }

        /**
         * 从枚举对象构建 Option（推荐方式）。
         * <p>自动读取枚举的全部契约字段，避免手写字段遗漏。</p>
         *
         * <pre>{@code
         * BaseEnum.Option<Integer> opt = BaseEnum.Option.from(StatusEnum.ENABLED);
         * }</pre>
         *
         * @param enumValue 枚举对象
         * @param <E>       枚举类型
         * @param <T>       枚举值类型
         * @return 构建的 Option；{@code enumValue} 为 {@code null} 时返回 {@code null}
         */
        static <E extends Enum<E> & BaseEnum<T>, T extends Serializable> Option<T> from(E enumValue) {
            if (enumValue == null) {
                return null;
            }
            return new Option<>(
                    enumValue.getValue(),
                    enumValue.getLabel(),
                    enumValue.getDescription(),
                    enumValue.getColor(),
                    enumValue.getIcon(),
                    enumValue.getOrder(),
                    enumValue.isValid()
            );
        }

        // ==================== Getter ====================

        /**
         * 获取枚举值。
         *
         * @return 枚举值
         */
        public V getValue() {
            return value;
        }

        /**
         * 获取显示标签。
         *
         * @return 显示标签
         */
        public String getLabel() {
            return label;
        }

        /**
         * 获取详细描述。
         *
         * @return 详细描述
         */
        public String getDescription() {
            return description;
        }

        /**
         * 获取 UI 颜色。
         *
         * @return UI 颜色
         */
        public String getColor() {
            return color;
        }

        /**
         * 获取 UI 图标。
         *
         * @return UI 图标
         */
        public String getIcon() {
            return icon;
        }

        /**
         * 获取排序权重。
         *
         * @return 排序权重
         */
        public int getOrder() {
            return order;
        }

        /**
         * 是否有效。
         *
         * @return 有效返回 {@code true}
         */
        public boolean isValid() {
            return valid;
        }
    }
}