package com.dream.common.tool.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 反射工具类
 * <p>
 * 封装 Java 反射 API，提供以下能力：
 * <ul>
 *   <li>{@link #getField(Class, String)} —— 字段查找（含父类链遍历）</li>
 *   <li>{@link #getFieldValue(Object, String)} / {@link #setFieldValue(Object, String, Object)} —— 字段读写</li>
 *   <li>{@link #invokeMethod(Object, String, Object...)} —— 方法调用（自动匹配参数类型）</li>
 *   <li>{@link #newInstance(Class)} / {@link #newInstance(Class, Object...)} —— 实例创建</li>
 *   <li>{@link #getSuperClassGenericType(Class)} —— 泛型参数解析</li>
 *   <li>{@link #getAnnotation(Class, Class)} / {@link #isAnnotationPresent} —— 注解查找</li>
 *   <li>{@link #objectToMap(Object)} / {@link #mapToObject(Map, Class)} —— 对象与 Map 互转</li>
 *   <li>{@link #copyProperties(Object, Object)} —— 同名字段属性拷贝</li>
 * </ul>
 * <p>
 * 注意：本类所有反射操作均会绕过访问控制（{@code setAccessible(true)}），
 * 适用于工具类场景，生产环境请谨慎评估安全策略。
 *
 * @author dream
 */
@SuppressWarnings("all")
public final class ReflectionUtils {

    /**
     * 字段缓存（Class → fieldName → Field），加速重复访问
     */
    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    /**
     * 方法缓存（Class#methodName#paramCount → Method）
     */
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    private ReflectionUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 类层级分析 ====================

    /**
     * 获取类的直接父类（排除 Object）
     *
     * @param clazz 目标类
     * @return 父类 Class；无父类或父类为 Object 时返回 null
     */
    public static Class<?> getSuperClass(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return null;
        }
        Class<?> superClass = clazz.getSuperclass();
        return (superClass == Object.class) ? null : superClass;
    }

    /**
     * 获取类的完整继承链（从直接父类到 Object 之前，不包含自身）
     * <pre>
     * getAllSuperClasses(ArrayList.class) → [AbstractList, AbstractCollection]
     * </pre>
     *
     * @param clazz 目标类
     * @return 父类列表（由近到远）；clazz 为 null 返回空列表
     */
    public static List<Class<?>> getAllSuperClasses(Class<?> clazz) {
        List<Class<?>> list = new ArrayList<>();
        if (clazz == null) {
            return list;
        }
        Class<?> current = clazz.getSuperclass();
        while (current != null && current != Object.class) {
            list.add(current);
            current = current.getSuperclass();
        }
        return list;
    }

    /**
     * 获取类实现的所有接口（含父类实现的接口，去重）
     *
     * @param clazz 目标类
     * @return 接口列表；clazz 为 null 返回空列表
     */
    public static List<Class<?>> getAllInterfaces(Class<?> clazz) {
        if (clazz == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(ClassUtils.getAllInterfaces(clazz));
    }

    /**
     * 判断 source 是否可以赋值给 target（含继承、接口实现、基本类型装箱）
     *
     * @param source 源类型
     * @param target 目标类型
     * @return true 表示可以赋值
     */
    public static boolean isAssignable(Class<?> source, Class<?> target) {
        if (source == null || target == null) {
            return false;
        }
        return ClassUtils.isAssignable(source, target, true);
    }

    // ==================== 字段操作 ====================

    /**
     * 获取类中指定名称的字段（含父类链，优先子类）
     *
     * @param clazz     目标类
     * @param fieldName 字段名称
     * @return Field 对象；未找到返回 null
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        if (clazz == null || fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        String cacheKey = clazz.getName() + "#" + fieldName;
        Field cached = FIELD_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                FIELD_CACHE.put(cacheKey, field);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 获取类的所有字段（含父类链，去重，子类优先）
     *
     * @param clazz 目标类
     * @return 字段列表；clazz 为 null 返回空列表
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        if (clazz == null) {
            return fields;
        }
        Set<String> seen = new HashSet<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (seen.add(field.getName())) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * 获取类中带有指定注解的字段列表
     *
     * @param clazz           目标类
     * @param annotationClass 注解类型
     * @return 带该注解的字段列表
     */
    public static List<Field> getFieldsWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        if (clazz == null || annotationClass == null) {
            return new ArrayList<>();
        }
        return getAllFields(clazz).stream()
                .filter(f -> f.isAnnotationPresent(annotationClass))
                .collect(Collectors.toList());
    }

    /**
     * 读取对象字段值（含父类字段，支持基本类型自动拆箱）
     * <pre>
     * getFieldValue(user, "name")        → "张三"
     * getFieldValue(user, "address.city") → 支持嵌套字段（点分隔）
     * </pre>
     *
     * @param obj       目标对象
     * @param fieldName 字段名称（支持嵌套，以点分隔）
     * @return 字段值；obj 为 null 或字段不存在返回 null
     */
    public static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null || fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        // 支持嵌套字段（点分隔）
        if (fieldName.contains(".")) {
            String[] parts = fieldName.split("\\.", 2);
            Object nested = getFieldValue(obj, parts[0]);
            return (nested == null) ? null : getFieldValue(nested, parts[1]);
        }
        try {
            Field field = getField(obj.getClass(), fieldName);
            if (field == null) {
                return null;
            }
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("无法读取字段 [" + fieldName + "]", e);
        }
    }

    /**
     * 设置对象字段值（含父类字段）
     * <p>
     * 支持类型自动转换：目标字段为基本类型时，字符串/Number 会自动转换。
     *
     * @param obj       目标对象
     * @param fieldName 字段名称
     * @param value     字段值
     * @return true 表示设置成功；obj 为 null 或字段不存在返回 false
     */
    public static boolean setFieldValue(Object obj, String fieldName, Object value) {
        if (obj == null || fieldName == null || fieldName.isEmpty()) {
            return false;
        }
        try {
            Field field = getField(obj.getClass(), fieldName);
            if (field == null) {
                return false;
            }
            field.set(obj, convertValue(value, field.getType()));
            return true;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("无法设置字段 [" + fieldName + "]", e);
        }
    }

    /**
     * 批量读取对象字段值（转为 Map，key 为字段名）
     * <p>
     * 默认包含所有非 static、非 transient 字段（含父类）。
     *
     * @param obj 目标对象
     * @return 字段名 → 字段值 Map；obj 为 null 返回空 Map
     */
    public static Map<String, Object> getFieldValues(Object obj) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (obj == null) {
            return map;
        }
        for (Field field : getAllFields(obj.getClass())) {
            int mod = field.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
                continue;
            }
            try {
                map.put(field.getName(), field.get(obj));
            } catch (IllegalAccessException ignored) {
            }
        }
        return map;
    }

    // ==================== 方法操作 ====================

    /**
     * 获取类中指定名称的方法（精确匹配参数类型，含父类链）
     *
     * @param clazz      目标类
     * @param methodName 方法名称
     * @param paramTypes 参数类型数组（可为空）
     * @return Method 对象；未找到返回 null
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        if (clazz == null || methodName == null || methodName.isEmpty()) {
            return null;
        }
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                Method method = current.getDeclaredMethod(methodName, paramTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        // 尝试从公共方法中查找（含接口默认方法）
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * 按名称和参数数量模糊匹配方法（含父类链）
     *
     * @param clazz      目标类
     * @param methodName 方法名称
     * @param argCount   参数个数
     * @return 匹配的方法列表（可能多个重载）
     */
    public static List<Method> getMethodsByName(Class<?> clazz, String methodName, int argCount) {
        if (clazz == null || methodName == null) {
            return new ArrayList<>();
        }
        List<Method> result = new ArrayList<>();
        for (Method method : getAllMethods(clazz)) {
            if (method.getName().equals(methodName) && method.getParameterCount() == argCount) {
                result.add(method);
            }
        }
        return result;
    }

    /**
     * 获取类的所有方法（含父类链，去重）
     * <p>
     * 不包含 Object 类的方法（toString/hashCode/equals 等除外）。
     *
     * @param clazz 目标类
     * @return 方法列表；clazz 为 null 返回空列表
     */
    public static List<Method> getAllMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<>();
        if (clazz == null) {
            return methods;
        }
        Set<String> seen = new HashSet<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                String sig = method.getName() + "#" + method.getParameterCount();
                if (seen.add(sig)) {
                    method.setAccessible(true);
                    methods.add(method);
                }
            }
            current = current.getSuperclass();
        }
        return methods;
    }

    /**
     * 获取类中带有指定注解的方法列表
     *
     * @param clazz           目标类
     * @param annotationClass 注解类型
     * @return 带该注解的方法列表
     */
    public static List<Method> getMethodsWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        if (clazz == null || annotationClass == null) {
            return new ArrayList<>();
        }
        return getAllMethods(clazz).stream()
                .filter(m -> m.isAnnotationPresent(annotationClass))
                .collect(Collectors.toList());
    }

    /**
     * 调用对象方法（精确匹配参数类型）
     * <pre>
     * invokeMethod(user, "getName")                → "张三"
     * invokeMethod(user, "setName", "李四")          → null（void 方法）
     * invokeMethod(user, "calculate", 10, 20)       → 30
     * </pre>
     *
     * @param obj        目标对象（静态方法传 null）
     * @param methodName 方法名称
     * @param args       方法参数
     * @return 方法返回值；void 方法返回 null
     */
    public static Object invokeMethod(Object obj, String methodName, Object... args) {
        if (methodName == null || methodName.isEmpty()) {
            return null;
        }
        Class<?> clazz = (obj != null) ? obj.getClass() : null;
        if (clazz == null) {
            return null;
        }
        Class<?>[] paramTypes = getParameterTypes(args);
        Method method = findMatchingMethod(clazz, methodName, paramTypes);
        if (method == null) {
            throw new RuntimeException("未找到方法 [" + methodName + "]，类：" + clazz.getName());
        }
        try {
            return method.invoke(obj, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("无权访问方法 [" + methodName + "]", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw new RuntimeException("调用方法 [" + methodName + "] 异常：" + cause.getMessage(), cause);
        }
    }

    /**
     * 调用对象的 getter 方法
     *
     * @param obj       目标对象
     * @param fieldName 字段名称（自动转换为 getXxx / isXxx）
     * @return getter 返回值
     */
    public static Object invokeGetter(Object obj, String fieldName) {
        if (obj == null || fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        String capitalized = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        String getterName = "get" + capitalized;
        Method getter = getMethod(obj.getClass(), getterName);
        if (getter == null) {
            getterName = "is" + capitalized;
            getter = getMethod(obj.getClass(), getterName);
        }
        if (getter == null) {
            throw new RuntimeException("未找到 getter 方法 [get/is" + capitalized + "]");
        }
        return invokeMethod(obj, getterName);
    }

    /**
     * 调用对象的 setter 方法
     *
     * @param obj       目标对象
     * @param fieldName 字段名称（自动转换为 setXxx）
     * @param value     设置值
     * @return true 表示调用成功
     */
    public static boolean invokeSetter(Object obj, String fieldName, Object value) {
        if (obj == null || fieldName == null || fieldName.isEmpty()) {
            return false;
        }
        String capitalized = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        String setterName = "set" + capitalized;
        List<Method> setters = getMethodsByName(obj.getClass(), setterName, 1);
        if (setters.isEmpty()) {
            return false;
        }
        invokeMethod(obj, setterName, value);
        return true;
    }

    /**
     * 调用静态方法
     *
     * @param clazz      目标类
     * @param methodName 方法名称
     * @param args       方法参数
     * @return 方法返回值
     */
    public static Object invokeStaticMethod(Class<?> clazz, String methodName, Object... args) {
        if (clazz == null || methodName == null) {
            return null;
        }
        Class<?>[] paramTypes = getParameterTypes(args);
        Method method = findMatchingMethod(clazz, methodName, paramTypes);
        if (method == null || !Modifier.isStatic(method.getModifiers())) {
            throw new RuntimeException("未找到静态方法 [" + methodName + "]，类：" + clazz.getName());
        }
        try {
            return method.invoke(null, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("无权访问静态方法 [" + methodName + "]", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("调用静态方法 [" + methodName + "] 异常", e.getCause());
        }
    }

    // ==================== 构造器与实例化 ====================

    /**
     * 获取类的指定参数构造器
     *
     * @param clazz      目标类
     * @param paramTypes 参数类型
     * @return Constructor 对象；未找到返回 null
     */
    public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... paramTypes) {
        if (clazz == null) {
            return null;
        }
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * 获取类的所有构造器
     *
     * @param clazz 目标类
     * @return 构造器列表
     */
    public static <T> List<Constructor<T>> getAllConstructors(Class<T> clazz) {
        if (clazz == null) {
            return new ArrayList<>();
        }
        List<Constructor<T>> list = new ArrayList<>();
        for (Constructor<T> c : (Constructor<T>[]) clazz.getDeclaredConstructors()) {
            c.setAccessible(true);
            list.add(c);
        }
        return list;
    }

    /**
     * 通过无参构造器创建实例
     * <pre>
     * newInstance(User.class)  → new User()
     * </pre>
     *
     * @param clazz 目标类
     * @return 实例对象
     */
    public static <T> T newInstance(Class<T> clazz) {
        if (clazz == null) {
            return null;
        }
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("类 [" + clazz.getName() + "] 没有无参构造器", e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("创建实例 [" + clazz.getName() + "] 失败", e);
        }
    }

    /**
     * 通过有参构造器创建实例
     *
     * @param clazz 目标类
     * @param args  构造器参数（按顺序对应构造器参数列表）
     * @return 实例对象
     */
    public static <T> T newInstance(Class<T> clazz, Object... args) {
        if (clazz == null) {
            return null;
        }
        if (ArrayUtils.isEmpty(args)) {
            return newInstance(clazz);
        }
        Class<?>[] paramTypes = getParameterTypes(args);
        Constructor<T> constructor = findMatchingConstructor(clazz, paramTypes);
        if (constructor == null) {
            throw new RuntimeException("未找到匹配的构造器，类：" + clazz.getName());
        }
        try {
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("创建实例 [" + clazz.getName() + "] 失败", e);
        }
    }

    // ==================== 注解操作 ====================

    /**
     * 获取类上的指定注解（含父类链）
     *
     * @param clazz           目标类
     * @param annotationClass 注解类型
     * @return 注解实例；未找到返回 null
     */
    public static <A extends Annotation> A getAnnotation(Class<?> clazz, Class<A> annotationClass) {
        if (clazz == null || annotationClass == null) {
            return null;
        }
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            A annotation = current.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /**
     * 获取字段上的指定注解
     *
     * @param field           字段对象
     * @param annotationClass 注解类型
     * @return 注解实例；未找到返回 null
     */
    public static <A extends Annotation> A getFieldAnnotation(Field field, Class<A> annotationClass) {
        if (field == null || annotationClass == null) {
            return null;
        }
        return field.getAnnotation(annotationClass);
    }

    /**
     * 获取方法上的指定注解
     *
     * @param method          方法对象
     * @param annotationClass 注解类型
     * @return 注解实例；未找到返回 null
     */
    public static <A extends Annotation> A getMethodAnnotation(Method method, Class<A> annotationClass) {
        if (method == null || annotationClass == null) {
            return null;
        }
        return method.getAnnotation(annotationClass);
    }

    /**
     * 获取类上的所有注解（含父类链，去重）
     *
     * @param clazz 目标类
     * @return 注解列表
     */
    public static List<Annotation> getAllAnnotations(Class<?> clazz) {
        if (clazz == null) {
            return new ArrayList<>();
        }
        Map<Class<? extends Annotation>, Annotation> map = new LinkedHashMap<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Annotation a : current.getAnnotations()) {
                map.putIfAbsent(a.annotationType(), a);
            }
            current = current.getSuperclass();
        }
        return new ArrayList<>(map.values());
    }

    /**
     * 判断类（含父类链）是否带有指定注解
     *
     * @param clazz           目标类
     * @param annotationClass 注解类型
     * @return true 表示存在该注解
     */
    public static boolean isAnnotationPresent(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        return getAnnotation(clazz, annotationClass) != null;
    }

    /**
     * 获取方法参数上的所有注解（二维数组：第一维对应参数，第二维对应注解）
     *
     * @param method 方法对象
     * @return 参数注解数组；method 为 null 返回空数组
     */
    public static Annotation[][] getParameterAnnotations(Method method) {
        if (method == null) {
            return new Annotation[0][];
        }
        return method.getParameterAnnotations();
    }

    // ==================== 泛型操作 ====================

    /**
     * 获取类的父类泛型参数类型
     * <pre>
     * // class UserDao extends BaseDao&lt;User&gt; {}
     * getSuperClassGenericType(UserDao.class)  → User.class
     * </pre>
     *
     * @param clazz 目标类
     * @return 父类泛型参数 Class 数组；无泛型参数返回空数组
     */
    public static Class<?>[] getSuperClassGenericType(Class<?> clazz) {
        if (clazz == null) {
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        Type type = clazz.getGenericSuperclass();
        if (!(type instanceof ParameterizedType)) {
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        return extractRawTypes(((ParameterizedType) type).getActualTypeArguments());
    }

    /**
     * 获取类的父类指定索引位置的泛型参数
     *
     * @param clazz 目标类
     * @param index 泛型参数索引（从 0 开始）
     * @return 泛型参数 Class；索引越界或无泛型返回 null
     */
    public static Class<?> getSuperClassGenericType(Class<?> clazz, int index) {
        Class<?>[] types = getSuperClassGenericType(clazz);
        if (index < 0 || index >= types.length) {
            return null;
        }
        return types[index];
    }

    /**
     * 获取接口的泛型参数类型
     * <pre>
     * // class UserService implements Service&lt;User, Long&gt; {}
     * getInterfaceGenericType(UserService.class, Service.class)  → [User.class, Long.class]
     * </pre>
     *
     * @param clazz          实现类
     * @param interfaceClass 接口类
     * @return 泛型参数 Class 数组
     */
    public static Class<?>[] getInterfaceGenericType(Class<?> clazz, Class<?> interfaceClass) {
        if (clazz == null || interfaceClass == null) {
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        for (Type type : clazz.getGenericInterfaces()) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                if (pt.getRawType().equals(interfaceClass)) {
                    return extractRawTypes(pt.getActualTypeArguments());
                }
            }
        }
        return ArrayUtils.EMPTY_CLASS_ARRAY;
    }

    /**
     * 获取字段的泛型参数类型（适用于 {@code List<String>} 类型的字段）
     *
     * @param field 字段对象
     * @return 泛型参数 Class 数组；非泛型字段返回空数组
     */
    public static Class<?>[] getFieldGenericType(Field field) {
        if (field == null) {
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        Type type = field.getGenericType();
        if (!(type instanceof ParameterizedType)) {
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        return extractRawTypes(((ParameterizedType) type).getActualTypeArguments());
    }

    /**
     * 获取方法返回值的泛型参数类型
     *
     * @param method 方法对象
     * @return 泛型参数 Class 数组
     */
    public static Class<?>[] getMethodGenericReturnType(Method method) {
        if (method == null) {
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        Type type = method.getGenericReturnType();
        if (!(type instanceof ParameterizedType)) {
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        return extractRawTypes(((ParameterizedType) type).getActualTypeArguments());
    }

    /**
     * 获取方法指定参数的泛型类型
     *
     * @param method     方法对象
     * @param paramIndex 参数索引（从 0 开始）
     * @return 泛型参数 Class 数组
     */
    public static Class<?>[] getMethodGenericParameterType(Method method, int paramIndex) {
        if (method == null) {
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        Type[] paramTypes = method.getGenericParameterTypes();
        if (paramIndex < 0 || paramIndex >= paramTypes.length) {
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        Type type = paramTypes[paramIndex];
        if (!(type instanceof ParameterizedType)) {
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        return extractRawTypes(((ParameterizedType) type).getActualTypeArguments());
    }

    // ==================== 对象与 Map 互转 ====================

    /**
     * 对象转 Map（key 为字段名，value 为字段值）
     * <p>
     * 包含所有非 static、非 transient 字段（含父类），忽略 null 值字段。
     *
     * @param obj 目标对象
     * @return 字段 Map；obj 为 null 返回空 Map
     */
    public static Map<String, Object> objectToMap(Object obj) {
        return objectToMap(obj, false);
    }

    /**
     * 对象转 Map
     *
     * @param obj         目标对象
     * @param includeNull 是否包含 null 值字段
     * @return 字段 Map
     */
    public static Map<String, Object> objectToMap(Object obj, boolean includeNull) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (obj == null) {
            return map;
        }
        for (Field field : getAllFields(obj.getClass())) {
            int mod = field.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
                continue;
            }
            try {
                Object value = field.get(obj);
                if (includeNull || value != null) {
                    map.put(field.getName(), value);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return map;
    }

    /**
     * Map 转对象（字段名匹配，支持类型自动转换）
     * <pre>
     * Map&lt;String, Object&gt; map = Map.of("name", "张三", "age", 18);
     * User user = mapToObject(map, User.class);
     * </pre>
     *
     * @param map   字段 Map
     * @param clazz 目标类（需有无参构造器）
     * @return 实例对象；map 为 null 或空返回 null
     */
    public static <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        if (map == null || map.isEmpty() || clazz == null) {
            return null;
        }
        T obj = newInstance(clazz);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            setFieldValue(obj, entry.getKey(), entry.getValue());
        }
        return obj;
    }

    // ==================== 属性拷贝 ====================

    /**
     * 同名字段属性拷贝（source → target）
     * <p>
     * 将 source 中与 target 同名的非 static 字段值复制到 target。
     * 类型不兼容时跳过，不会抛异常。
     * <pre>
     * User source = new User("张三", 18);
     * UserVO target = new UserVO();
     * copyProperties(source, target);  // target.name = "张三", target.age = 18
     * </pre>
     *
     * @param source 源对象
     * @param target 目标对象
     * @return 成功拷贝的字段数量
     */
    public static int copyProperties(Object source, Object target) {
        return copyProperties(source, target, (String[]) null);
    }

    /**
     * 同名字段属性拷贝（支持忽略指定字段）
     *
     * @param source       源对象
     * @param target       目标对象
     * @param ignoreFields 忽略的字段名
     * @return 成功拷贝的字段数量
     */
    public static int copyProperties(Object source, Object target, String... ignoreFields) {
        if (source == null || target == null) {
            return 0;
        }
        Set<String> ignoreSet = (ignoreFields != null)
                ? new HashSet<>(Arrays.asList(ignoreFields))
                : Collections.emptySet();
        int count = 0;
        for (Field srcField : getAllFields(source.getClass())) {
            int mod = srcField.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
                continue;
            }
            String name = srcField.getName();
            if (ignoreSet.contains(name)) {
                continue;
            }
            Field targetField = getField(target.getClass(), name);
            if (targetField == null) {
                continue;
            }
            try {
                Object value = srcField.get(source);
                if (isAssignable(srcField.getType(), targetField.getType())) {
                    targetField.set(target, value);
                    count++;
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return count;
    }

    // ==================== 修饰符分析 ====================

    /**
     * 判断字段是否为 static
     *
     * @param field 字段对象
     * @return true 表示 static
     */
    public static boolean isStatic(Field field) {
        return field != null && Modifier.isStatic(field.getModifiers());
    }

    /**
     * 判断字段是否为 final
     *
     * @param field 字段对象
     * @return true 表示 final
     */
    public static boolean isFinal(Field field) {
        return field != null && Modifier.isFinal(field.getModifiers());
    }

    /**
     * 判断字段是否为 public
     *
     * @param field 字段对象
     * @return true 表示 public
     */
    public static boolean isPublic(Field field) {
        return field != null && Modifier.isPublic(field.getModifiers());
    }

    /**
     * 判断类是否为抽象类
     *
     * @param clazz 目标类
     * @return true 表示 abstract
     */
    public static boolean isAbstract(Class<?> clazz) {
        return clazz != null && Modifier.isAbstract(clazz.getModifiers());
    }

    /**
     * 判断类是否为接口
     *
     * @param clazz 目标类
     * @return true 表示接口
     */
    public static boolean isInterface(Class<?> clazz) {
        return clazz != null && clazz.isInterface();
    }

    /**
     * 判断类是否为枚举
     *
     * @param clazz 目标类
     * @return true 表示枚举
     */
    public static boolean isEnum(Class<?> clazz) {
        return clazz != null && clazz.isEnum();
    }

    /**
     * 判断类是否为数组
     *
     * @param clazz 目标类
     * @return true 表示数组类型
     */
    public static boolean isArray(Class<?> clazz) {
        return clazz != null && clazz.isArray();
    }

    /**
     * 判断类是否为基本类型（含包装类）
     *
     * @param clazz 目标类
     * @return true 表示基本类型或包装类
     */
    public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz != null && (clazz.isPrimitive() || ClassUtils.isPrimitiveWrapper(clazz));
    }

    // ==================== 内部工具方法 ====================

    /**
     * 获取参数数组对应的 Class 数组
     */
    private static Class<?>[] getParameterTypes(Object[] args) {
        if (ArrayUtils.isEmpty(args)) {
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = (args[i] != null) ? args[i].getClass() : Object.class;
        }
        return types;
    }

    /**
     * 按名称和参数类型模糊查找方法（支持参数类型向上兼容匹配）
     */
    private static Method findMatchingMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        // 优先精确匹配
        Method exact = getMethod(clazz, methodName, paramTypes);
        if (exact != null) {
            return exact;
        }
        // 模糊匹配（参数类型向上兼容）
        for (Method method : getAllMethods(clazz)) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] methodParamTypes = method.getParameterTypes();
            if (methodParamTypes.length != paramTypes.length) {
                continue;
            }
            boolean match = true;
            for (int i = 0; i < paramTypes.length; i++) {
                if (!isAssignable(paramTypes[i], methodParamTypes[i])) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return method;
            }
        }
        return null;
    }

    /**
     * 按参数类型模糊匹配构造器
     */
    private static <T> Constructor<T> findMatchingConstructor(Class<T> clazz, Class<?>[] paramTypes) {
        for (Constructor<T> constructor : getAllConstructors(clazz)) {
            Class<?>[] ctorParamTypes = constructor.getParameterTypes();
            if (ctorParamTypes.length != paramTypes.length) {
                continue;
            }
            boolean match = true;
            for (int i = 0; i < paramTypes.length; i++) {
                if (!isAssignable(paramTypes[i], ctorParamTypes[i])) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return constructor;
            }
        }
        return null;
    }

    /**
     * 将 Type 数组转为 Class 数组（泛型擦除后取原始类型）
     */
    private static Class<?>[] extractRawTypes(Type[] types) {
        if (ArrayUtils.isEmpty(types)) {
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        Class<?>[] result = new Class<?>[types.length];
        for (int i = 0; i < types.length; i++) {
            Type type = types[i];
            if (type instanceof Class) {
                result[i] = (Class<?>) type;
            } else if (type instanceof ParameterizedType) {
                result[i] = (Class<?>) ((ParameterizedType) type).getRawType();
            } else if (type instanceof WildcardType) {
                Type[] upper = ((WildcardType) type).getUpperBounds();
                result[i] = (upper.length > 0 && upper[0] instanceof Class) ? (Class<?>) upper[0] : Object.class;
            } else if (type instanceof GenericArrayType) {
                Type componentType = ((GenericArrayType) type).getGenericComponentType();
                if (componentType instanceof Class) {
                    result[i] = Array.newInstance((Class<?>) componentType, 0).getClass();
                } else {
                    result[i] = Object[].class;
                }
            } else {
                result[i] = Object.class;
            }
        }
        return result;
    }

    /**
     * 类型自动转换（将 value 转为 targetType 的兼容值）
     * <p>
     * 支持：Number → 基本数值类型互转、String → 基本类型解析、null → 基本类型默认值。
     */
    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            if (targetType.isPrimitive()) {
                if (targetType == boolean.class) return false;
                if (targetType == byte.class) return (byte) 0;
                if (targetType == short.class) return (short) 0;
                if (targetType == int.class) return 0;
                if (targetType == long.class) return 0L;
                if (targetType == float.class) return 0.0f;
                if (targetType == double.class) return 0.0d;
                if (targetType == char.class) return '\0';
            }
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        // Number 类型互转
        if (value instanceof Number) {
            Number num = (Number) value;
            if (targetType == int.class || targetType == Integer.class) return num.intValue();
            if (targetType == long.class || targetType == Long.class) return num.longValue();
            if (targetType == double.class || targetType == Double.class) return num.doubleValue();
            if (targetType == float.class || targetType == Float.class) return num.floatValue();
            if (targetType == short.class || targetType == Short.class) return num.shortValue();
            if (targetType == byte.class || targetType == Byte.class) return num.byteValue();
            if (targetType == boolean.class || targetType == Boolean.class) return num.intValue() != 0;
            if (targetType == String.class) return num.toString();
        }
        // String → 基本类型解析
        if (value instanceof CharSequence) {
            String str = value.toString().trim();
            if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(str);
            if (targetType == long.class || targetType == Long.class) return Long.parseLong(str);
            if (targetType == double.class || targetType == Double.class) return Double.parseDouble(str);
            if (targetType == float.class || targetType == Float.class) return Float.parseFloat(str);
            if (targetType == short.class || targetType == Short.class) return Short.parseShort(str);
            if (targetType == byte.class || targetType == Byte.class) return Byte.parseByte(str);
            if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(str);
            if (targetType == char.class || targetType == Character.class) return str.isEmpty() ? '\0' : str.charAt(0);
        }
        // Boolean 互转
        if (value instanceof Boolean) {
            if (targetType == int.class || targetType == Integer.class) return ((Boolean) value) ? 1 : 0;
            if (targetType == String.class) return value.toString();
        }
        return value;
    }
}
