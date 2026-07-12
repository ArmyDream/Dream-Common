package com.dream.common.core.utils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * 函数式建造者工具类
 * <p>
 * 提供通用的链式建造能力，支持条件建造、验证、转换、集合建造、异步建造、分组建造等。
 * 所有方法均做 null 安全保护，建造失败时通过监听器通知。
 * </p>
 *
 * <h2>核心能力</h2>
 * <ul>
 *   <li>{@link #of(Supplier)} / {@link #of(Object)} —— 创建建造者</li>
 *   <li>{@link #build(Object, Consumer)} —— 快速建造（最简洁用法）</li>
 *   <li>{@link #buildList(Consumer)} / {@link #buildSet(Consumer)} —— 集合建造</li>
 *   <li>{@link #buildMap(Consumer)} / {@link #buildConcurrentMap(Consumer)} —— Map 建造</li>
 *   <li>{@link #groupBy(Collection, Function)} —— 分组工具</li>
 *   <li>{@link #buildAll(Supplier, Consumer[])} —— 批量建造</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 基础建造
 * User user = BuilderUtils.of(User::new)
 *     .with(u -> u.setName("张三"))
 *     .with(u -> u.setAge(20))
 *     .when(age > 18, u -> u.setAdult(true))
 *     .validateNotNull(User::getName, "name")
 *     .build();
 *
 * // 快速建造
 * User user = BuilderUtils.build(new User(), u -> {
 *     u.setName("张三");
 *     u.setAge(20);
 * });
 *
 * // 集合建造
 * List<String> list = BuilderUtils.buildList(b -> b
 *     .add("a").add("b").addIf(flag, "c"));
 *
 * // Map 建造
 * Map<String, Object> map = BuilderUtils.buildMap(b -> b
 *     .put("name", "张三").putIf(flag, "age", 20));
 *
 * // 分组建造
 * Map<String, List<User>> group = BuilderUtils.groupBy(users, User::getDepartment);
 * }</pre>
 *
 * @author dream
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class BuilderUtils {

    private BuilderUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 基础建造 ====================

    /**
     * 创建建造者（通过 Supplier 提供实例）
     *
     * @param supplier 实例提供者，不能为 null
     * @param <T>      实例类型
     * @return 建造者实例
     * @throws NullPointerException 如果 supplier 为 null
     */
    public static <T> Builder<T> of(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        return new Builder<>(supplier);
    }

    /**
     * 创建建造者（使用已有实例）
     *
     * @param instance 已有实例，不能为 null
     * @param <T>      实例类型
     * @return 建造者实例
     * @throws NullPointerException 如果 instance 为 null
     */
    public static <T> Builder<T> of(T instance) {
        Objects.requireNonNull(instance, "instance must not be null");
        return new Builder<>(instance);
    }

    /**
     * 快速建造（最简洁用法）
     *
     * @param instance 实例，不能为 null
     * @param config   配置函数，可为 null
     * @param <T>      实例类型
     * @return 配置后的实例
     * @throws NullPointerException 如果 instance 为 null
     */
    public static <T> T build(T instance, Consumer<T> config) {
        Objects.requireNonNull(instance, "instance must not be null");
        if (config != null) {
            config.accept(instance);
        }
        return instance;
    }

    /**
     * 快速建造（通过 Supplier 创建并配置）
     *
     * @param supplier 实例提供者，不能为 null
     * @param config   配置函数，可为 null
     * @param <T>      实例类型
     * @return 配置后的实例
     * @throws NullPointerException 如果 supplier 为 null
     */
    public static <T> T build(Supplier<T> supplier, Consumer<T> config) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        T instance = supplier.get();
        if (config != null) {
            config.accept(instance);
        }
        return instance;
    }

    // ==================== 集合建造 ====================

    /**
     * 建造 List
     *
     * @param config 配置函数，可为 null
     * @param <E>    元素类型
     * @return 建造完成的 List（ArrayList 实现）
     */
    public static <E> List<E> buildList(Consumer<CollectionBuilder<E>> config) {
        CollectionBuilder<E> builder = new CollectionBuilder<>(ArrayList::new);
        if (config != null) {
            config.accept(builder);
        }
        return builder.buildList();
    }

    /**
     * 建造不可变 List
     *
     * @param config 配置函数，可为 null
     * @param <E>    元素类型
     * @return 不可变 List
     */
    public static <E> List<E> buildUnmodifiableList(Consumer<CollectionBuilder<E>> config) {
        return Collections.unmodifiableList(buildList(config));
    }

    /**
     * 建造 Set
     *
     * @param config 配置函数，可为 null
     * @param <E>    元素类型
     * @return 建造完成的 Set（LinkedHashSet 实现）
     */
    public static <E> Set<E> buildSet(Consumer<CollectionBuilder<E>> config) {
        CollectionBuilder<E> builder = new CollectionBuilder<>(LinkedHashSet::new);
        if (config != null) {
            config.accept(builder);
        }
        return builder.buildSet();
    }

    /**
     * 建造指定类型的集合
     *
     * @param collectionSupplier 集合提供者，不能为 null
     * @param config             配置函数，可为 null
     * @param <E>                元素类型
     * @param <C>                集合类型
     * @return 建造完成的集合
     * @throws NullPointerException 如果 collectionSupplier 为 null
     */
    @SuppressWarnings("unchecked")
    public static <E, C extends Collection<E>> C buildCollection(
            Supplier<C> collectionSupplier,
            Consumer<CollectionBuilder<E>> config) {
        Objects.requireNonNull(collectionSupplier, "collectionSupplier must not be null");
        CollectionBuilder<E> builder = new CollectionBuilder<>((Supplier<Collection<E>>) collectionSupplier);
        if (config != null) {
            config.accept(builder);
        }
        return builder.buildTyped();
    }

    /**
     * 建造 Map
     *
     * @param config 配置函数，可为 null
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 建造完成的 Map（LinkedHashMap 实现）
     */
    public static <K, V> Map<K, V> buildMap(Consumer<MapBuilder<K, V>> config) {
        MapBuilder<K, V> builder = new MapBuilder<>(LinkedHashMap::new);
        if (config != null) {
            config.accept(builder);
        }
        return builder.build();
    }

    /**
     * 建造 ConcurrentHashMap
     *
     * @param config 配置函数，可为 null
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 建造完成的 ConcurrentHashMap
     */
    public static <K, V> Map<K, V> buildConcurrentMap(Consumer<MapBuilder<K, V>> config) {
        MapBuilder<K, V> builder = new MapBuilder<>(ConcurrentHashMap::new);
        if (config != null) {
            config.accept(builder);
        }
        return builder.build();
    }

    /**
     * 从数组建造不可变 List
     *
     * @param elements 元素数组，可为 null
     * @param <E>      元素类型
     * @return 不可变 List，如果 elements 为 null 或空则返回空 List
     */
    @SafeVarargs
    public static <E> List<E> listOf(E... elements) {
        if (elements == null || elements.length == 0) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(elements.clone()));
    }

    /**
     * 从数组建造不可变 Set
     *
     * @param elements 元素数组，可为 null
     * @param <E>      元素类型
     * @return 不可变 Set，如果 elements 为 null 或空则返回空 Set
     */
    @SafeVarargs
    public static <E> Set<E> setOf(E... elements) {
        if (elements == null || elements.length == 0) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(elements.clone())));
    }

    /**
     * 从键值对建造不可变 Map
     * <pre>{@code
     * Map<String, Object> map = BuilderUtils.mapOf("name", "张三", "age", 20);
     * }</pre>
     *
     * @param pairs 键值对，必须为偶数个，可为 null
     * @param <K>   键类型
     * @param <V>   值类型
     * @return 不可变 Map
     * @throws IllegalArgumentException 如果 pairs 长度为奇数
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> mapOf(Object... pairs) {
        if (pairs == null || pairs.length == 0) {
            return Collections.emptyMap();
        }
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("pairs must have even number of elements");
        }
        Map<K, V> map = new LinkedHashMap<>(pairs.length / 2);
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((K) pairs[i], (V) pairs[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }

    // ==================== 分组工具 ====================

    /**
     * 按分组函数分组
     *
     * @param collection 集合
     * @param classifier 分组分类器
     * @param <T>        元素类型
     * @param <K>        键类型
     * @return 分组结果 Map
     */
    public static <T, K> Map<K, List<T>> groupBy(Collection<T> collection, Function<T, K> classifier) {
        return groupBy(collection, classifier, Collectors.toList());
    }

    /**
     * 按分组函数分组（指定收集器）
     *
     * @param collection 集合
     * @param classifier 分组分类器
     * @param collector  收集器
     * @param <T>        元素类型
     * @param <K>        键类型
     * @param <D>        收集结果类型
     * @return 分组结果 Map
     */
    public static <T, K, D> Map<K, D> groupBy(
            Collection<T> collection,
            Function<T, K> classifier,
            Collector<T, ?, D> collector) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptyMap();
        }
        Objects.requireNonNull(classifier, "classifier must not be null");
        Objects.requireNonNull(collector, "collector must not be null");
        return collection.stream().collect(Collectors.groupingBy(classifier, LinkedHashMap::new, collector));
    }

    // ==================== 条件工具 ====================

    /**
     * 条件执行
     *
     * @param condition 条件
     * @param action    满足时执行的动作，可为 null
     */
    public static void when(boolean condition, Runnable action) {
        if (condition && action != null) {
            action.run();
        }
    }

    /**
     * 条件执行（带 else）
     *
     * @param condition   条件
     * @param trueAction  满足时执行，可为 null
     * @param falseAction 不满足时执行，可为 null
     */
    public static void whenElse(boolean condition, Runnable trueAction, Runnable falseAction) {
        if (condition) {
            if (trueAction != null) {
                trueAction.run();
            }
        } else {
            if (falseAction != null) {
                falseAction.run();
            }
        }
    }

    /**
     * 非空执行
     *
     * @param value  待检查值
     * @param action 非空时执行的动作，可为 null
     * @param <T>    值类型
     */
    public static <T> void ifPresent(T value, Consumer<T> action) {
        if (value != null && action != null) {
            action.accept(value);
        }
    }

    /**
     * 非空返回，否则返回默认值
     *
     * @param value        待检查值
     * @param defaultValue 默认值，可为 null
     * @param <T>          类型
     * @return 值或默认值
     */
    public static <T> T orElse(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * 非空返回，否则通过 Supplier 获取
     *
     * @param value    待检查值
     * @param supplier 默认值提供者，可为 null
     * @param <T>      类型
     * @return 值或 Supplier 提供的值
     */
    public static <T> T orElseGet(T value, Supplier<T> supplier) {
        return value != null ? value : (supplier != null ? supplier.get() : null);
    }

    // ==================== 批量建造 ====================

    /**
     * 批量建造
     *
     * @param supplier 实例提供者，不能为 null
     * @param configs  配置函数数组，可为 null
     * @param <T>      实例类型
     * @return 建造完成的实例列表，如果 supplier 或 configs 为 null 则返回空列表
     */
    @SafeVarargs
    public static <T> List<T> buildAll(Supplier<T> supplier, Consumer<T>... configs) {
        if (supplier == null || configs == null || configs.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(configs)
                .filter(Objects::nonNull)
                .map(config -> BuilderUtils.of(supplier).with(config).build())
                .collect(Collectors.toList());
    }

    // ==================== Builder 类 ====================

    /**
     * 通用建造者
     * <p>
     * 支持链式调用、条件建造、验证、后置处理、缓存、监听器等。
     * </p>
     *
     * @param <T> 建造目标类型
     */
    public static final class Builder<T> {
        private final T instance;
        private final Supplier<T> supplier;
        private final List<Consumer<T>> configurators;
        private final List<Validator<T>> validators;
        private final List<UnaryOperator<T>> processors;
        private final List<BuildListener<T>> listeners;
        private boolean cacheEnabled;
        private volatile T cachedInstance;

        /**
         * 通过 Supplier 创建 Builder
         */
        private Builder(Supplier<T> supplier) {
            this.supplier = supplier;
            this.instance = null;
            this.configurators = new ArrayList<>();
            this.validators = new ArrayList<>();
            this.processors = new ArrayList<>();
            this.listeners = new ArrayList<>();
        }

        /**
         * 通过已有实例创建 Builder
         */
        private Builder(T instance) {
            this.supplier = null;
            this.instance = instance;
            this.configurators = new ArrayList<>();
            this.validators = new ArrayList<>();
            this.processors = new ArrayList<>();
            this.listeners = new ArrayList<>();
        }

        /**
         * 设置属性
         *
         * @param configurator 配置函数，可为 null
         * @return this
         */
        public Builder<T> with(Consumer<T> configurator) {
            if (configurator != null) {
                this.configurators.add(configurator);
            }
            return this;
        }

        /**
         * 批量设置属性
         *
         * @param configurators 配置函数数组，可为 null
         * @return this
         */
        @SafeVarargs
        public final Builder<T> withAll(Consumer<T>... configurators) {
            if (configurators != null) {
                for (Consumer<T> c : configurators) {
                    if (c != null) {
                        this.configurators.add(c);
                    }
                }
            }
            return this;
        }

        /**
         * 条件设置
         *
         * @param condition    条件
         * @param configurator 满足时执行的配置，可为 null
         * @return this
         */
        public Builder<T> when(boolean condition, Consumer<T> configurator) {
            if (condition && configurator != null) {
                this.configurators.add(configurator);
            }
            return this;
        }

        /**
         * 条件设置（if-else）
         *
         * @param condition   条件
         * @param trueConfig  满足时执行，可为 null
         * @param falseConfig 不满足时执行，可为 null
         * @return this
         */
        public Builder<T> whenElse(boolean condition, Consumer<T> trueConfig, Consumer<T> falseConfig) {
            Consumer<T> config = condition ? trueConfig : falseConfig;
            if (config != null) {
                this.configurators.add(config);
            }
            return this;
        }

        /**
         * 可选值设置（非 null 才设置）
         *
         * @param value  待检查值
         * @param setter 设置函数，不能为 null
         * @param <V>    值类型
         * @return this
         */
        public <V> Builder<T> withOptional(V value, BiConsumer<T, V> setter) {
            Objects.requireNonNull(setter, "setter must not be null");
            if (value != null) {
                this.configurators.add(instance -> setter.accept(instance, value));
            }
            return this;
        }

        /**
         * 可选值设置（带默认值）
         *
         * @param value        待检查值
         * @param setter       设置函数，不能为 null
         * @param defaultValue 默认值，可为 null
         * @param <V>          值类型
         * @return this
         */
        public <V> Builder<T> withOptional(V value, BiConsumer<T, V> setter, V defaultValue) {
            Objects.requireNonNull(setter, "setter must not be null");
            final V finalValue = value != null ? value : defaultValue;
            this.configurators.add(instance -> setter.accept(instance, finalValue));
            return this;
        }

        /**
         * 转换后设置
         *
         * @param source 源值
         * @param mapper 映射函数，不能为 null
         * @param setter 设置函数，不能为 null
         * @param <S>    源类型
         * @param <K>    目标类型
         * @return this
         */
        public <S, K> Builder<T> withMapped(S source, Function<S, K> mapper, BiConsumer<T, K> setter) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            Objects.requireNonNull(setter, "setter must not be null");
            if (source != null) {
                K mapped = mapper.apply(source);
                if (mapped != null) {
                    this.configurators.add(instance -> setter.accept(instance, mapped));
                }
            }
            return this;
        }

        /**
         * 设置默认值（字段为 null 时填充）
         *
         * @param defaultValue 默认值提供者，不能为 null
         * @param getter       获取当前值，不能为 null
         * @param setter       设置默认值，不能为 null
         * @param <V>          值类型
         * @return this
         */
        public <V> Builder<T> withDefault(Supplier<V> defaultValue,
                                          Function<T, V> getter,
                                          BiConsumer<T, V> setter) {
            Objects.requireNonNull(defaultValue, "defaultValue must not be null");
            Objects.requireNonNull(getter, "getter must not be null");
            Objects.requireNonNull(setter, "setter must not be null");
            this.configurators.add(instance -> {
                if (getter.apply(instance) == null) {
                    setter.accept(instance, defaultValue.get());
                }
            });
            return this;
        }

        /**
         * 条件默认值
         *
         * @param condition    判断条件，不能为 null
         * @param defaultValue 默认值提供者，不能为 null
         * @param getter       获取当前值，不能为 null
         * @param setter       设置默认值，不能为 null
         * @param <V>          值类型
         * @return this
         */
        public <V> Builder<T> withDefaultIf(Predicate<V> condition,
                                            Supplier<V> defaultValue,
                                            Function<T, V> getter,
                                            BiConsumer<T, V> setter) {
            Objects.requireNonNull(condition, "condition must not be null");
            Objects.requireNonNull(defaultValue, "defaultValue must not be null");
            Objects.requireNonNull(getter, "getter must not be null");
            Objects.requireNonNull(setter, "setter must not be null");
            this.configurators.add(instance -> {
                V value = getter.apply(instance);
                if (value == null || condition.test(value)) {
                    setter.accept(instance, defaultValue.get());
                }
            });
            return this;
        }

        /**
         * 添加验证器
         *
         * @param predicate 验证条件，不能为 null
         * @param message   失败消息，可为 null
         * @return this
         */
        public Builder<T> validate(Predicate<T> predicate, String message) {
            Objects.requireNonNull(predicate, "predicate must not be null");
            this.validators.add(new Validator<>(predicate, message));
            return this;
        }

        /**
         * 验证字段非空
         *
         * @param getter    字段获取器，不能为 null
         * @param fieldName 字段名，不能为 null
         * @return this
         */
        public Builder<T> validateNotNull(Function<T, Object> getter, String fieldName) {
            Objects.requireNonNull(getter, "getter must not be null");
            Objects.requireNonNull(fieldName, "fieldName must not be null");
            return validate(instance -> getter.apply(instance) != null, fieldName + " must not be null");
        }

        /**
         * 验证字符串非空白
         *
         * @param getter    字符串字段获取器，不能为 null
         * @param fieldName 字段名，不能为 null
         * @return this
         */
        public Builder<T> validateNotBlank(Function<T, String> getter, String fieldName) {
            Objects.requireNonNull(getter, "getter must not be null");
            Objects.requireNonNull(fieldName, "fieldName must not be null");
            return validate(instance -> {
                String value = getter.apply(instance);
                return value != null && !value.trim().isEmpty();
            }, fieldName + " must not be blank");
        }

        /**
         * 验证正则表达式
         *
         * @param getter    字符串字段获取器，不能为 null
         * @param regex     正则表达式，不能为 null
         * @param fieldName 字段名，不能为 null
         * @return this
         */
        public Builder<T> validateRegex(Function<T, String> getter, String regex, String fieldName) {
            Objects.requireNonNull(getter, "getter must not be null");
            Objects.requireNonNull(regex, "regex must not be null");
            Objects.requireNonNull(fieldName, "fieldName must not be null");
            return validate(instance -> {
                String value = getter.apply(instance);
                return value != null && value.matches(regex);
            }, fieldName + " format is invalid");
        }

        /**
         * 验证数值范围
         *
         * @param getter    数值字段获取器，不能为 null
         * @param min       最小值，不能为 null
         * @param max       最大值，不能为 null
         * @param fieldName 字段名，不能为 null
         * @return this
         */
        public Builder<T> validateRange(Function<T, Number> getter, Number min, Number max, String fieldName) {
            Objects.requireNonNull(getter, "getter must not be null");
            Objects.requireNonNull(min, "min must not be null");
            Objects.requireNonNull(max, "max must not be null");
            Objects.requireNonNull(fieldName, "fieldName must not be null");
            return validate(instance -> {
                Number value = getter.apply(instance);
                return value != null &&
                        value.doubleValue() >= min.doubleValue() &&
                        value.doubleValue() <= max.doubleValue();
            }, fieldName + " must be between " + min + " and " + max);
        }

        /**
         * 添加后置处理器
         *
         * @param processor 处理函数，不能为 null
         * @return this
         */
        public Builder<T> postProcess(UnaryOperator<T> processor) {
            Objects.requireNonNull(processor, "processor must not be null");
            this.processors.add(processor);
            return this;
        }

        /**
         * 条件后置处理
         *
         * @param condition 条件，不能为 null
         * @param processor 满足时执行的处理，不能为 null
         * @return this
         */
        public Builder<T> postProcessIf(Predicate<T> condition, UnaryOperator<T> processor) {
            Objects.requireNonNull(condition, "condition must not be null");
            Objects.requireNonNull(processor, "processor must not be null");
            this.processors.add(instance -> condition.test(instance) ? processor.apply(instance) : instance);
            return this;
        }

        /**
         * 转换字段值
         *
         * @param getter      获取原值，不能为 null
         * @param setter      设置新值，不能为 null
         * @param transformer 转换函数，不能为 null
         * @param <V>         字段类型
         * @return this
         */
        public <V> Builder<T> transformField(Function<T, V> getter,
                                             BiConsumer<T, V> setter,
                                             UnaryOperator<V> transformer) {
            Objects.requireNonNull(getter, "getter must not be null");
            Objects.requireNonNull(setter, "setter must not be null");
            Objects.requireNonNull(transformer, "transformer must not be null");
            this.processors.add(instance -> {
                V value = getter.apply(instance);
                if (value != null) {
                    setter.accept(instance, transformer.apply(value));
                }
                return instance;
            });
            return this;
        }

        /**
         * 启用缓存（多次 build 返回同一实例）
         *
         * @return this
         */
        public Builder<T> enableCache() {
            this.cacheEnabled = true;
            return this;
        }

        /**
         * 清除缓存
         *
         * @return this
         */
        public Builder<T> clearCache() {
            this.cachedInstance = null;
            return this;
        }

        /**
         * 添加监听器
         *
         * @param listener 监听器，不能为 null
         * @return this
         */
        public Builder<T> addListener(BuildListener<T> listener) {
            Objects.requireNonNull(listener, "listener must not be null");
            this.listeners.add(listener);
            return this;
        }

        /**
         * 异步建造
         *
         * @return CompletableFuture
         */
        public CompletableFuture<T> buildAsync() {
            return CompletableFuture.supplyAsync(this::build);
        }

        /**
         * 异步建造（指定线程池）
         *
         * @param executor 线程池，不能为 null
         * @return CompletableFuture
         * @throws NullPointerException 如果 executor 为 null
         */
        public CompletableFuture<T> buildAsync(Executor executor) {
            Objects.requireNonNull(executor, "executor must not be null");
            return CompletableFuture.supplyAsync(this::build, executor);
        }

        /**
         * 执行建造
         * <p>
         * 流程：获取实例 → 前置通知 → 应用配置 → 应用处理器 → 执行验证 → 后置通知 → 返回
         * </p>
         *
         * @return 建造完成的实例
         * @throws IllegalStateException 验证失败或建造结果为 null 时抛出
         * @throws RuntimeException      配置/处理器执行失败时抛出
         */
        public T build() {
            // 检查缓存
            if (cacheEnabled && cachedInstance != null) {
                return cachedInstance;
            }

            // 获取实例
            T result = supplier != null ? supplier.get() : instance;
            if (result == null) {
                throw new IllegalStateException("Build result is null: check supplier or instance");
            }

            // 前置通知
            notifyPreBuild(result);

            try {
                // 应用配置
                for (Consumer<T> config : configurators) {
                    config.accept(result);
                }

                // 应用处理器
                for (UnaryOperator<T> processor : processors) {
                    result = processor.apply(result);
                }

                // 执行验证
                for (Validator<T> validator : validators) {
                    validator.validate(result);
                }

                // 缓存结果
                if (cacheEnabled) {
                    cachedInstance = result;
                }

                // 后置通知
                notifyPostBuild(result);

                return result;
            } catch (Exception e) {
                notifyError(result, e);
                throw e;
            }
        }

        private void notifyPreBuild(T instance) {
            for (BuildListener<T> listener : listeners) {
                try {
                    listener.onPreBuild(instance);
                } catch (Exception e) {
                    // 监听器异常不影响主流程，但需要上报
                    handleListenerError(e);
                }
            }
        }

        private void notifyPostBuild(T instance) {
            for (BuildListener<T> listener : listeners) {
                try {
                    listener.onPostBuild(instance);
                } catch (Exception e) {
                    handleListenerError(e);
                }
            }
        }

        private void notifyError(T instance, Exception e) {
            for (BuildListener<T> listener : listeners) {
                try {
                    listener.onError(instance, e);
                } catch (Exception ex) {
                    handleListenerError(ex);
                }
            }
        }

        /**
         * 处理监听器异常
         * <p>
         * 监听器异常不应影响主流程，但应通知调用方。
         * 这里通过抛出 RuntimeException 来让调用方感知。
         * </p>
         *
         * @param e 监听器抛出的异常
         * @throws RuntimeException 包装后的异常
         */
        private void handleListenerError(Exception e) {
            throw new RuntimeException("Listener execution failed", e);
        }
    }

    // ==================== 内部类 ====================

    /**
     * 验证器
     */
    private static final class Validator<T> {
        private final Predicate<T> predicate;
        private final String message;

        Validator(Predicate<T> predicate, String message) {
            this.predicate = predicate;
            this.message = message != null ? message : "Validation failed";
        }

        void validate(T instance) {
            if (!predicate.test(instance)) {
                throw new IllegalStateException(message);
            }
        }
    }

    /**
     * 建造监听器接口
     * <p>
     * 监听器方法中的异常将被捕获并包装为 {@link RuntimeException} 重新抛出。
     * </p>
     *
     * @param <T> 建造目标类型
     */
    public interface BuildListener<T> {
        /**
         * 建造前回调
         *
         * @param instance 待建造实例
         */
        void onPreBuild(T instance);

        /**
         * 建造后回调
         *
         * @param instance 建造完成的实例
         */
        void onPostBuild(T instance);

        /**
         * 建造失败回调
         *
         * @param instance 实例
         * @param e        异常
         */
        void onError(T instance, Exception e);
    }

    /**
     * 集合建造器
     *
     * @param <E> 元素类型
     */
    public static final class CollectionBuilder<E> {
        private final Supplier<Collection<E>> supplier;
        private final List<E> elements;

        /**
         * 创建集合建造器
         *
         * @param supplier 集合提供者，不能为 null
         */
        public CollectionBuilder(Supplier<Collection<E>> supplier) {
            Objects.requireNonNull(supplier, "supplier must not be null");
            this.supplier = supplier;
            this.elements = new ArrayList<>();
        }

        /**
         * 添加元素
         *
         * @param element 元素
         * @return this
         */
        public CollectionBuilder<E> add(E element) {
            this.elements.add(element);
            return this;
        }

        /**
         * 添加非 null 元素
         *
         * @param element 元素
         * @return this
         */
        public CollectionBuilder<E> addNonNull(E element) {
            if (element != null) {
                this.elements.add(element);
            }
            return this;
        }

        /**
         * 批量添加
         *
         * @param elements 元素数组，可为 null
         * @return this
         */
        @SafeVarargs
        public final CollectionBuilder<E> addAll(E... elements) {
            if (elements != null) {
                Collections.addAll(this.elements, elements);
            }
            return this;
        }

        /**
         * 从集合添加
         *
         * @param collection 元素集合，可为 null
         * @return this
         */
        public CollectionBuilder<E> addAll(Collection<E> collection) {
            if (collection != null) {
                this.elements.addAll(collection);
            }
            return this;
        }

        /**
         * 条件添加
         *
         * @param condition 条件
         * @param element   元素
         * @return this
         */
        public CollectionBuilder<E> addIf(boolean condition, E element) {
            if (condition) {
                this.elements.add(element);
            }
            return this;
        }

        /**
         * 条件批量添加
         *
         * @param condition 条件
         * @param elements  元素数组，可为 null
         * @return this
         */
        @SafeVarargs
        public final CollectionBuilder<E> addAllIf(boolean condition, E... elements) {
            if (condition && elements != null) {
                Collections.addAll(this.elements, elements);
            }
            return this;
        }

        /**
         * 映射添加
         *
         * @param source 源值
         * @param mapper 映射函数，不能为 null
         * @param <S>    源类型
         * @return this
         */
        public <S> CollectionBuilder<E> addMapped(S source, Function<S, E> mapper) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            if (source != null) {
                E mapped = mapper.apply(source);
                if (mapped != null) {
                    this.elements.add(mapped);
                }
            }
            return this;
        }

        /**
         * 去重
         *
         * @return this
         */
        public CollectionBuilder<E> distinct() {
            Set<E> set = new LinkedHashSet<>(elements);
            elements.clear();
            elements.addAll(set);
            return this;
        }

        /**
         * 过滤
         *
         * @param filter 过滤条件，不能为 null
         * @return this
         */
        public CollectionBuilder<E> filter(Predicate<E> filter) {
            Objects.requireNonNull(filter, "filter must not be null");
            elements.removeIf(filter.negate());
            return this;
        }

        /**
         * 建造为 List
         *
         * @return List 实例
         */
        public List<E> buildList() {
            return new ArrayList<>(elements);
        }

        /**
         * 建造为 Set
         *
         * @return Set 实例（LinkedHashSet）
         */
        public Set<E> buildSet() {
            return new LinkedHashSet<>(elements);
        }

        /**
         * 建造为指定类型集合
         *
         * @param <C> 集合类型
         * @return 集合实例
         */
        @SuppressWarnings("unchecked")
        public <C extends Collection<E>> C buildTyped() {
            C result = (C) supplier.get();
            result.addAll(elements);
            return result;
        }
    }

    /**
     * Map 建造器
     *
     * @param <K> 键类型
     * @param <V> 值类型
     */
    public static final class MapBuilder<K, V> {
        private final Supplier<Map<K, V>> supplier;
        private final Map<K, V> map;

        /**
         * 创建 HashMap 建造器
         */
        public MapBuilder() {
            this(LinkedHashMap::new);
        }

        /**
         * 创建指定类型 Map 建造器
         *
         * @param supplier Map 提供者，不能为 null
         */
        public MapBuilder(Supplier<Map<K, V>> supplier) {
            Objects.requireNonNull(supplier, "supplier must not be null");
            this.supplier = supplier;
            this.map = supplier.get();
        }

        /**
         * 放入键值对
         *
         * @param key   键
         * @param value 值
         * @return this
         */
        public MapBuilder<K, V> put(K key, V value) {
            this.map.put(key, value);
            return this;
        }

        /**
         * 放入非 null 值的键值对
         *
         * @param key   键
         * @param value 值（null 则不放入）
         * @return this
         */
        public MapBuilder<K, V> putIfNotNull(K key, V value) {
            if (value != null) {
                this.map.put(key, value);
            }
            return this;
        }

        /**
         * 条件放入
         *
         * @param condition 条件
         * @param key       键
         * @param value     值
         * @return this
         */
        public MapBuilder<K, V> putIf(boolean condition, K key, V value) {
            if (condition) {
                this.map.put(key, value);
            }
            return this;
        }

        /**
         * 批量放入
         *
         * @param other 另一个 Map，可为 null
         * @return this
         */
        public MapBuilder<K, V> putAll(Map<K, V> other) {
            if (other != null) {
                this.map.putAll(other);
            }
            return this;
        }

        /**
         * 映射放入
         *
         * @param key    键
         * @param source 源值
         * @param mapper 映射函数，不能为 null
         * @param <S>    源类型
         * @return this
         */
        public <S> MapBuilder<K, V> putMapped(K key, S source, Function<S, V> mapper) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            if (source != null) {
                V mapped = mapper.apply(source);
                if (mapped != null) {
                    this.map.put(key, mapped);
                }
            }
            return this;
        }

        /**
         * 条件映射放入
         *
         * @param condition 条件
         * @param key       键
         * @param source    源值
         * @param mapper    映射函数，不能为 null
         * @param <S>       源类型
         * @return this
         */
        public <S> MapBuilder<K, V> putMappedIf(boolean condition, K key, S source, Function<S, V> mapper) {
            if (condition) {
                return putMapped(key, source, mapper);
            }
            return this;
        }

        /**
         * 如果键不存在则放入
         *
         * @param key   键
         * @param value 值
         * @return this
         */
        public MapBuilder<K, V> putIfAbsent(K key, V value) {
            this.map.putIfAbsent(key, value);
            return this;
        }

        /**
         * 建造完成
         *
         * @return 建造好的 Map
         */
        public Map<K, V> build() {
            Map<K, V> result = supplier.get();
            result.putAll(this.map);
            return result;
        }
    }
}