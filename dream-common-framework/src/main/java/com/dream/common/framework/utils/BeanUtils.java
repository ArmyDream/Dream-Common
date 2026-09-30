package com.dream.common.framework.utils;

import com.dream.common.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.cglib.core.Converter;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Bean 操作工具类
 * <p>
 * 基于 Spring 和 CGLIB 提供高性能的 Bean 拷贝、转换、合并等操作。
 * </p>
 * <p>
 * 核心能力：
 * <ul>
 *   <li>{@link #copyProperties(Object, Object)} —— 属性拷贝（支持忽略、覆盖）</li>
 *   <li>{@link #copyProperties(Object, Class)} —— 属性拷贝并转换类型</li>
 *   <li>{@link #copyPropertiesIgnoreNull(Object, Object)} —— 忽略空值/空字符串拷贝</li>
 *   <li>{@link #merge(Object, Object)} —— 合并两个对象</li>
 *   <li>{@link #convert(Object, Class)} —— 对象类型转换</li>
 *   <li>{@link #getFieldValue(Object, String)} —— 获取字段值</li>
 *   <li>{@link #setFieldValue(Object, String, Object)} —— 设置字段值</li>
 *   <li>{@link #getEmptyPropertyNames(Object)} —— 获取空值属性名列表</li>
 *   <li>{@link #getPropertyNames(Object)} —— 获取所有属性名</li>
 * </ul>
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 基础拷贝
 * UserDTO dto = BeanUtils.copyProperties(user, UserDTO.class);
 *
 * // 2. 拷贝到已有对象
 * BeanUtils.copyProperties(user, existingUserDTO);
 *
 * // 3. 忽略空值/空字符串拷贝（源对象中 null 或空字符串的属性不覆盖目标对象）
 * BeanUtils.copyPropertiesIgnoreNull(source, target);
 *
 * // 4. 忽略指定属性拷贝
 * BeanUtils.copyProperties(source, target, "id", "createTime");
 *
 * // 5. 合并对象（target 中 null/空字符串的属性用 source 填充）
 * BeanUtils.merge(source, target);
 *
 * // 6. 列表转换
 * List<UserDTO> dtoList = BeanUtils.convertList(userList, UserDTO.class);
 *
 * // 7. 获取/设置字段值
 * String name = BeanUtils.getFieldValue(user, "name");
 * BeanUtils.setFieldValue(user, "age", 20);
 *
 * // 8. 获取空值属性名（包含 null 和空字符串）
 * String[] emptyProps = BeanUtils.getEmptyPropertyNames(user);
 * }</pre>
 *
 * @author dream
 * @see org.springframework.beans.BeanUtils
 * @see org.springframework.cglib.beans.BeanCopier
 */
@Slf4j
@SuppressWarnings({"unused", "WeakerAccess"})
public final class BeanUtils {

    /**
     * BeanCopier 缓存（提升性能）
     * Key: "sourceClass:targetClass"
     * Value: BeanCopier 实例
     */
    private static final Map<String, BeanCopier> BEAN_COPIER_CACHE = new ConcurrentHashMap<>(256);

    /**
     * 字段缓存
     */
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>(128);

    /**
     * 属性描述符缓存
     */
    private static final Map<Class<?>, PropertyDescriptor[]> PROPERTY_DESCRIPTOR_CACHE = new ConcurrentHashMap<>(128);

    private BeanUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 基础拷贝 ====================

    /**
     * 拷贝属性（源对象 -> 目标对象）
     * <p>
     * 使用 CGLIB BeanCopier 实现高性能拷贝。
     * </p>
     *
     * @param source 源对象，不能为 null
     * @param target 目标对象，不能为 null
     * @throws NullPointerException 如果 source 或 target 为 null
     */
    public static void copyProperties(Object source, Object target) {
        copyProperties(source, target, (String[]) null);
    }

    /**
     * 拷贝属性（忽略指定属性）
     *
     * @param source           源对象，不能为 null
     * @param target           目标对象，不能为 null
     * @param ignoreProperties 忽略的属性名
     * @throws NullPointerException 如果 source 或 target 为 null
     */
    public static void copyProperties(Object source, Object target, String... ignoreProperties) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");

        if (ignoreProperties != null && ignoreProperties.length > 0) {
            Set<String> ignoreSet = new HashSet<>(Arrays.asList(ignoreProperties));
            // 使用 Spring BeanUtils 处理忽略
            String[] ignoreArray = ignoreSet.toArray(new String[0]);
            BeanUtils.copyProperties(source, target, ignoreArray);
        } else {
            // 使用 CGLIB BeanCopier（性能更好）
            String key = generateCacheKey(source.getClass(), target.getClass());
            BeanCopier copier = BEAN_COPIER_CACHE.computeIfAbsent(key, k -> {
                log.debug("Creating BeanCopier for: {}", key);
                return BeanCopier.create(source.getClass(), target.getClass(), false);
            });
            copier.copy(source, target, null);
        }
    }

    /**
     * 拷贝属性（带 Converter）
     *
     * @param source    源对象，不能为 null
     * @param target    目标对象，不能为 null
     * @param converter 类型转换器
     * @throws NullPointerException 如果 source 或 target 为 null
     */
    public static void copyProperties(Object source, Object target, Converter converter) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");

        String key = generateCacheKey(source.getClass(), target.getClass());
        BeanCopier copier = BEAN_COPIER_CACHE.computeIfAbsent(key, k -> {
            log.debug("Creating BeanCopier with converter for: {}", key);
            return BeanCopier.create(source.getClass(), target.getClass(), true);
        });
        copier.copy(source, target, converter);
    }

    /**
     * 拷贝属性（源对象 -> 目标对象实例，自动创建目标对象）
     *
     * @param source      源对象，不能为 null
     * @param targetClass 目标类，不能为 null
     * @param <T>         目标类型
     * @return 拷贝后的目标对象
     * @throws BusinessException 如果创建目标对象失败
     */
    public static <T> T copyProperties(Object source, Class<T> targetClass) {
        return copyProperties(source, targetClass, (String[]) null);
    }

    /**
     * 拷贝属性（忽略指定属性，自动创建目标对象）
     *
     * @param source           源对象，不能为 null
     * @param targetClass      目标类，不能为 null
     * @param ignoreProperties 忽略的属性名
     * @param <T>              目标类型
     * @return 拷贝后的目标对象
     * @throws BusinessException 如果创建目标对象失败
     */
    public static <T> T copyProperties(Object source, Class<T> targetClass, String... ignoreProperties) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(targetClass, "targetClass must not be null");

        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            copyProperties(source, target, ignoreProperties);
            return target;
        } catch (Exception e) {
            log.error("Failed to create instance of: {}", targetClass.getName(), e);
            throw new BusinessException("Failed to copy properties to " + targetClass.getName(), e);
        }
    }

    // ==================== 忽略空值/空字符串拷贝 ====================

    /**
     * 忽略空值/空字符串拷贝（源对象中 null 或空字符串/空白字符串的属性不会覆盖目标对象）
     * <p>
     * 注意：空字符串包括 null、""、以及只包含空白字符的字符串（如 "  "）。
     * </p>
     *
     * @param source 源对象，不能为 null
     * @param target 目标对象，不能为 null
     * @throws NullPointerException 如果 source 或 target 为 null
     */
    public static void copyPropertiesIgnoreNull(Object source, Object target) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");

        String[] emptyPropertyNames = getEmptyPropertyNames(source);
        if (emptyPropertyNames.length > 0) {
            log.debug("Ignoring empty properties: {}", Arrays.toString(emptyPropertyNames));
        }
        BeanUtils.copyProperties(source, target, emptyPropertyNames);
    }

    /**
     * 忽略空值/空字符串拷贝（自动创建目标对象）
     *
     * @param source      源对象，不能为 null
     * @param targetClass 目标类，不能为 null
     * @param <T>         目标类型
     * @return 拷贝后的目标对象
     */
    public static <T> T copyPropertiesIgnoreNull(Object source, Class<T> targetClass) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(targetClass, "targetClass must not be null");

        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            copyPropertiesIgnoreNull(source, target);
            return target;
        } catch (Exception e) {
            log.error("Failed to create instance of: {}", targetClass.getName(), e);
            throw new BusinessException("Failed to copy properties to " + targetClass.getName(), e);
        }
    }

    // ==================== 合并 ====================

    /**
     * 合并对象（target 中为 null 或空字符串的属性使用 source 中的值填充）
     * <p>
     * 注意：此操作会修改 target 对象。
     * </p>
     *
     * @param source 源对象（提供默认值），不能为 null
     * @param target 目标对象（会被修改），不能为 null
     * @throws NullPointerException 如果 source 或 target 为 null
     */
    public static void merge(Object source, Object target) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");

        String[] emptyPropertyNames = getEmptyPropertyNames(target);
        if (emptyPropertyNames.length == 0) {
            log.debug("Target has no empty properties, skip merge");
            return;
        }

        log.debug("Merging {} empty properties from source to target", emptyPropertyNames.length);
        BeanUtils.copyProperties(source, target, emptyPropertyNames);
    }

    /**
     * 合并对象（返回新对象）
     *
     * @param source      源对象（提供默认值），不能为 null
     * @param targetClass 目标类，不能为 null
     * @param <T>         目标类型
     * @return 合并后的新对象
     */
    public static <T> T merge(Object source, Class<T> targetClass) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(targetClass, "targetClass must not be null");

        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            // 先拷贝 source 到 target
            copyProperties(source, target);
            // 再拷贝 target 到 source（覆盖非空属性）
            copyPropertiesIgnoreNull(target, source);
            return target;
        } catch (Exception e) {
            log.error("Failed to merge to: {}", targetClass.getName(), e);
            throw new BusinessException("Failed to merge to " + targetClass.getName(), e);
        }
    }

    // ==================== 类型转换 ====================

    /**
     * 对象类型转换
     *
     * @param source      源对象，可为 null
     * @param targetClass 目标类，不能为 null
     * @param <T>         目标类型
     * @return 转换后的对象，如果 source 为 null 则返回 null
     */
    public static <T> T convert(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        return copyProperties(source, targetClass);
    }

    /**
     * 列表类型转换
     *
     * @param sourceList  源列表，可为 null
     * @param targetClass 目标类，不能为 null
     * @param <S>         源类型
     * @param <T>         目标类型
     * @return 转换后的列表，如果 sourceList 为 null 则返回空列表
     */
    public static <S, T> List<T> convertList(Collection<S> sourceList, Class<T> targetClass) {
        return convertList(sourceList, targetClass, null);
    }

    /**
     * 列表类型转换（带后置处理）
     *
     * @param sourceList    源列表，可为 null
     * @param targetClass   目标类，不能为 null
     * @param postProcessor 后置处理器，可为 null
     * @param <S>           源类型
     * @param <T>           目标类型
     * @return 转换后的列表，如果 sourceList 为 null 则返回空列表
     */
    public static <S, T> List<T> convertList(Collection<S> sourceList, Class<T> targetClass, Consumer<T> postProcessor) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }

        return sourceList.stream()
                .map(source -> {
                    T target = convert(source, targetClass);
                    if (postProcessor != null && target != null) {
                        postProcessor.accept(target);
                    }
                    return target;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ==================== 字段操作 ====================

    /**
     * 获取字段值（支持嵌套属性，如 "user.name"）
     *
     * @param object    对象，不能为 null
     * @param fieldName 字段名，支持嵌套，不能为 null
     * @param <T>       返回值类型
     * @return 字段值
     * @throws BusinessException 如果获取失败
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object object, String fieldName) {
        Objects.requireNonNull(object, "object must not be null");
        Objects.requireNonNull(fieldName, "fieldName must not be null");

        // 处理嵌套属性
        if (fieldName.contains(".")) {
            String[] parts = fieldName.split("\\.");
            Object current = object;
            for (String part : parts) {
                current = getFieldValueDirect(current, part);
                if (current == null) {
                    return null;
                }
            }
            return (T) current;
        }

        return getFieldValueDirect(object, fieldName);
    }

    /**
     * 直接获取字段值（不支持嵌套）
     */
    @SuppressWarnings("unchecked")
    private static <T> T getFieldValueDirect(Object object, String fieldName) {
        try {
            Field field = getDeclaredField(object.getClass(), fieldName);
            field.setAccessible(true);
            return (T) field.get(object);
        } catch (Exception e) {
            log.warn("Failed to get field value: {}.{}", object.getClass().getSimpleName(), fieldName, e);
            throw new BusinessException("Failed to get field value: " + fieldName, e);
        }
    }

    /**
     * 设置字段值（支持嵌套属性，如 "user.name"）
     *
     * @param object    对象，不能为 null
     * @param fieldName 字段名，支持嵌套，不能为 null
     * @param value     值
     * @throws BusinessException 如果设置失败
     */
    public static void setFieldValue(Object object, String fieldName, Object value) {
        Objects.requireNonNull(object, "object must not be null");
        Objects.requireNonNull(fieldName, "fieldName must not be null");

        // 处理嵌套属性
        if (fieldName.contains(".")) {
            String[] parts = fieldName.split("\\.");
            Object current = object;
            for (int i = 0; i < parts.length - 1; i++) {
                current = getFieldValueDirect(current, parts[i]);
                if (current == null) {
                    log.warn("Nested object is null, cannot set field: {}", fieldName);
                    return;
                }
            }
            setFieldValueDirect(current, parts[parts.length - 1], value);
            return;
        }

        setFieldValueDirect(object, fieldName, value);
    }

    /**
     * 直接设置字段值（不支持嵌套）
     */
    private static void setFieldValueDirect(Object object, String fieldName, Object value) {
        try {
            Field field = getDeclaredField(object.getClass(), fieldName);
            // 检查是否 final
            if (Modifier.isFinal(field.getModifiers())) {
                log.warn("Cannot set final field: {}.{}", object.getClass().getSimpleName(), fieldName);
                return;
            }
            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception e) {
            log.warn("Failed to set field value: {}.{}", object.getClass().getSimpleName(), fieldName, e);
            throw new BusinessException("Failed to set field value: " + fieldName, e);
        }
    }

    // ==================== 属性查询 ====================

    /**
     * 获取对象中所有空属性名（包含 null 和空字符串/空白字符串）
     * <p>
     * 空值判断规则：
     * <ul>
     *   <li>null → 视为空</li>
     *   <li>空字符串 "" → 视为空</li>
     *   <li>空白字符串 "   " → 视为空</li>
     *   <li>其他类型（如数字、集合、Map）只判断是否为 null</li>
     * </ul>
     * </p>
     *
     * @param object 对象，不能为 null
     * @return 空属性名数组
     */
    public static String[] getEmptyPropertyNames(Object object) {
        Objects.requireNonNull(object, "object must not be null");

        BeanWrapper beanWrapper = new BeanWrapperImpl(object);
        PropertyDescriptor[] pds = beanWrapper.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<>();

        for (PropertyDescriptor pd : pds) {
            String propertyName = pd.getName();
            if ("class".equals(propertyName)) {
                continue;
            }
            Object value = beanWrapper.getPropertyValue(propertyName);
            if (isEmptyValue(value)) {
                emptyNames.add(propertyName);
            }
        }

        return emptyNames.toArray(new String[0]);
    }

    /**
     * 判断值是否为空
     * <p>
     * 空值判断规则：
     * <ul>
     *   <li>null → true</li>
     *   <li>空字符串 "" → true</li>
     *   <li>空白字符串 "   " → true</li>
     *   <li>其他类型 → false（只判断 null）</li>
     * </ul>
     * </p>
     *
     * @param value 待判断的值
     * @return true 如果值为空
     */
    private static boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        // 字符串特殊处理：空字符串和空白字符串视为空
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        // 其他类型只判断 null
        return false;
    }

    /**
     * 获取对象中所有非空属性名（不包含 null 和空字符串/空白字符串）
     *
     * @param object 对象，不能为 null
     * @return 非空属性名数组
     */
    public static String[] getNonEmptyPropertyNames(Object object) {
        Objects.requireNonNull(object, "object must not be null");

        BeanWrapper beanWrapper = new BeanWrapperImpl(object);
        PropertyDescriptor[] pds = beanWrapper.getPropertyDescriptors();
        Set<String> nonEmptyNames = new HashSet<>();

        for (PropertyDescriptor pd : pds) {
            String propertyName = pd.getName();
            if ("class".equals(propertyName)) {
                continue;
            }
            Object value = beanWrapper.getPropertyValue(propertyName);
            if (!isEmptyValue(value)) {
                nonEmptyNames.add(propertyName);
            }
        }

        return nonEmptyNames.toArray(new String[0]);
    }

    /**
     * 获取对象的所有属性名
     *
     * @param object 对象，不能为 null
     * @return 属性名数组
     */
    public static String[] getPropertyNames(Object object) {
        Objects.requireNonNull(object, "object must not be null");

        PropertyDescriptor[] pds = getPropertyDescriptors(object.getClass());
        return Arrays.stream(pds)
                .map(PropertyDescriptor::getName)
                .filter(name -> !"class".equals(name))
                .toArray(String[]::new);
    }

    // ==================== 复制属性（条件复制） ====================

    /**
     * 条件复制（满足条件的属性才复制）
     *
     * @param source    源对象，不能为 null
     * @param target    目标对象，不能为 null
     * @param predicate 条件判断，不能为 null
     */
    public static void copyPropertiesIf(Object source, Object target, Predicate<String> predicate) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(predicate, "predicate must not be null");

        String[] allProps = getPropertyNames(source);
        List<String> matchedProps = Arrays.stream(allProps)
                .filter(predicate)
                .collect(Collectors.toList());

        if (!matchedProps.isEmpty()) {
            copyProperties(source, target, matchedProps.toArray(new String[0]));
        }
    }

    /**
     * 复制指定属性（仅复制指定的属性）
     *
     * @param source     源对象，不能为 null
     * @param target     目标对象，不能为 null
     * @param properties 要复制的属性名
     */
    public static void copyPropertiesInclude(Object source, Object target, String... properties) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");

        if (properties == null || properties.length == 0) {
            return;
        }

        Set<String> includeSet = new HashSet<>(Arrays.asList(properties));
        String[] allProps = getPropertyNames(source);
        List<String> ignoreProps = Arrays.stream(allProps)
                .filter(prop -> !includeSet.contains(prop) && !"class".equals(prop))
                .collect(Collectors.toList());

        if (!ignoreProps.isEmpty()) {
            copyProperties(source, target, ignoreProps.toArray(new String[0]));
        } else {
            copyProperties(source, target);
        }
    }

    // ==================== Map 转换 ====================

    /**
     * 将对象转换为 Map
     *
     * @param object 对象，不能为 null
     * @return Map（key 为属性名，value 为属性值）
     */
    public static Map<String, Object> toMap(Object object) {
        Objects.requireNonNull(object, "object must not be null");

        BeanWrapper beanWrapper = new BeanWrapperImpl(object);
        PropertyDescriptor[] pds = beanWrapper.getPropertyDescriptors();
        Map<String, Object> map = new LinkedHashMap<>();

        for (PropertyDescriptor pd : pds) {
            String propertyName = pd.getName();
            if ("class".equals(propertyName)) {
                continue;
            }
            try {
                Object value = beanWrapper.getPropertyValue(propertyName);
                map.put(propertyName, value);
            } catch (Exception e) {
                log.warn("Failed to get property value: {}", propertyName, e);
            }
        }

        return map;
    }

    /**
     * 将 Map 转换为 Bean
     *
     * @param map         Map，不能为 null
     * @param targetClass 目标类，不能为 null
     * @param <T>         目标类型
     * @return Bean 实例
     */
    public static <T> T fromMap(Map<String, Object> map, Class<T> targetClass) {
        Objects.requireNonNull(map, "map must not be null");
        Objects.requireNonNull(targetClass, "targetClass must not be null");

        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanWrapper beanWrapper = new BeanWrapperImpl(target);

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value != null && beanWrapper.isWritableProperty(key)) {
                    try {
                        beanWrapper.setPropertyValue(key, value);
                    } catch (Exception e) {
                        log.warn("Failed to set property: {} = {}", key, value, e);
                    }
                }
            }

            return target;
        } catch (Exception e) {
            log.error("Failed to create instance of: {}", targetClass.getName(), e);
            throw new BusinessException("Failed to convert map to " + targetClass.getName(), e);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 获取声明字段（含父类）
     *
     * @param clazz     类，不能为 null
     * @param fieldName 字段名，不能为 null
     * @return 字段对象
     * @throws NoSuchFieldException 如果字段不存在
     */
    public static Field getDeclaredField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Objects.requireNonNull(clazz, "clazz must not be null");
        Objects.requireNonNull(fieldName, "fieldName must not be null");

        Map<String, Field> fieldMap = FIELD_CACHE.computeIfAbsent(clazz, k -> {
            Map<String, Field> map = new LinkedHashMap<>();
            Class<?> current = k;
            while (current != null && current != Object.class) {
                Field[] fields = current.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    map.putIfAbsent(field.getName(), field);
                }
                current = current.getSuperclass();
            }
            return map;
        });

        Field field = fieldMap.get(fieldName);
        if (field == null) {
            throw new NoSuchFieldException("Field '" + fieldName + "' not found in " + clazz.getName());
        }
        return field;
    }

    /**
     * 获取属性描述符
     *
     * @param clazz 类，不能为 null
     * @return 属性描述符数组
     */
    public static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) {
        Objects.requireNonNull(clazz, "clazz must not be null");
        return PROPERTY_DESCRIPTOR_CACHE.computeIfAbsent(clazz, k -> {
            BeanWrapper beanWrapper = new BeanWrapperImpl(k);
            return beanWrapper.getPropertyDescriptors();
        });
    }

    /**
     * 生成 BeanCopier 缓存 Key
     */
    private static String generateCacheKey(Class<?> sourceClass, Class<?> targetClass) {
        return sourceClass.getName() + ":" + targetClass.getName();
    }

    // ==================== 函数式操作 ====================

    /**
     * 对象处理（非空处理）
     *
     * @param object 对象
     * @param action 处理函数
     * @param <T>    对象类型
     */
    public static <T> void doIfNotNull(T object, Consumer<T> action) {
        if (object != null && action != null) {
            action.accept(object);
        }
    }

    /**
     * 对象转换（非空转换）
     *
     * @param object 对象
     * @param mapper 转换函数
     * @param <T>    原类型
     * @param <R>    目标类型
     * @return 转换后的对象，如果 object 为 null 则返回 null
     */
    public static <T, R> R mapIfNotNull(T object, Function<T, R> mapper) {
        if (object == null || mapper == null) {
            return null;
        }
        return mapper.apply(object);
    }

    // ==================== 静态缓存清理 ====================

    /**
     * 清空缓存（用于测试或内存紧张时）
     */
    public static void clearCache() {
        BEAN_COPIER_CACHE.clear();
        FIELD_CACHE.clear();
        PROPERTY_DESCRIPTOR_CACHE.clear();
        log.info("BeanUtils cache cleared");
    }

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计信息
     */
    public static Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("beanCopierCache", BEAN_COPIER_CACHE.size());
        stats.put("fieldCache", FIELD_CACHE.size());
        stats.put("propertyDescriptorCache", PROPERTY_DESCRIPTOR_CACHE.size());
        return stats;
    }
}