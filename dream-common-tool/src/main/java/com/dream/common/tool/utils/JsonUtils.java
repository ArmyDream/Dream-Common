package com.dream.common.tool.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JSON 工具类（基于 Fastjson2）
 * <p>
 * 封装 {@link JSON} 核心 API，提供安全、便捷的 JSON 序列化/反序列化能力。
 * 所有方法均做 null 安全和异常保护，解析失败时记录日志并返回 null/空值。
 * <p>
 * 核心功能：
 * <ul>
 *   <li>{@link #toJSON(Object)} —— 对象序列化为 JSON 字符串</li>
 *   <li>{@link #toJSONPretty(Object)} —— 格式化 JSON 输出</li>
 *   <li>{@link #parseObject(String, Class)} —— JSON 反序列化为对象</li>
 *   <li>{@link #parseList(String, Class)} —— JSON 反序列化为 List</li>
 *   <li>{@link #parseMap(String)} —— JSON 反序列化为 Map</li>
 *   <li>{@link #isValidJSON(String)} —— JSON 格式校验</li>
 *   <li>{@link #getString(String, String)} —— 从 JSON 字符串中提取值</li>
 * </ul>
 *
 */
@Slf4j
@SuppressWarnings("all")
public final class JsonUtils {

    // ==================== 常量 ====================

    /** 默认日期格式 */
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /** 默认字符集 */
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    // ==================== 默认特性配置 ====================

    /**
     * 默认序列化特性
     * <p>
     * 注意：Fastjson2 默认不输出 null 值
     */
    private static final JSONWriter.Feature[] DEFAULT_WRITE_FEATURES = new JSONWriter.Feature[0];

    /** 美化输出特性 */
    private static final JSONWriter.Feature[] PRETTY_WRITE_FEATURES = {
            JSONWriter.Feature.PrettyFormat
    };

    /** 保留 null 值特性 */
    private static final JSONWriter.Feature[] WRITE_NULL_FEATURES = {
            JSONWriter.Feature.WriteMapNullValue,
            JSONWriter.Feature.WriteNullStringAsEmpty,
//            JSONWriter.Feature.WriteNullNumberAsZero,
//            JSONWriter.Feature.WriteNullBooleanAsFalse,
            JSONWriter.Feature.WriteNullListAsEmpty
    };

    /** 浏览器兼容特性（Long → String） */
    private static final JSONWriter.Feature[] BROWSER_FEATURES = {
            JSONWriter.Feature.BrowserCompatible
    };

    /** 默认反序列化特性 */
    private static final JSONReader.Feature[] DEFAULT_READ_FEATURES = new JSONReader.Feature[0];

    private JsonUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 序列化 ====================

    /**
     * 对象序列化为 JSON 字符串
     *
     * @param obj 待序列化对象
     * @return JSON 字符串；obj 为 null 返回 null
     */
    public static String toJSON(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            log.error("JSON 序列化失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 对象序列化为 JSON 字符串（自定义序列化特性）
     *
     * @param obj      待序列化对象
     * @param features 序列化特性
     * @return JSON 字符串；obj 为 null 返回 null
     */
    public static String toJSON(Object obj, JSONWriter.Feature... features) {
        if (obj == null) {
            return null;
        }
        try {
            if (features == null || features.length == 0) {
                return JSON.toJSONString(obj);
            }
            return JSON.toJSONString(obj, features);
        } catch (Exception e) {
            log.error("JSON 序列化失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 对象序列化为格式化 JSON 字符串（带缩进）
     *
     * @param obj 待序列化对象
     * @return 格式化 JSON 字符串；obj 为 null 返回 null
     */
    public static String toJSONPretty(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return JSON.toJSONString(obj, JSONWriter.Feature.PrettyFormat);
        } catch (Exception e) {
            log.error("JSON 格式化序列化失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 对象序列化为 JSON 字符串（保留 null 值字段）
     *
     * @param obj 待序列化对象
     * @return JSON 字符串（含 null 字段）；obj 为 null 返回 null
     */
    public static String toJSONWithNull(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return JSON.toJSONString(obj, WRITE_NULL_FEATURES);
        } catch (Exception e) {
            log.error("JSON 序列化（保留null）失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 对象序列化为 JSON 字符串（浏览器兼容模式）
     * <p>
     * 将 Long 类型转为 String，避免 JavaScript 精度丢失。
     *
     * @param obj 待序列化对象
     * @return JSON 字符串；obj 为 null 返回 null
     */
    public static String toJSONForBrowser(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return JSON.toJSONString(obj, JSONWriter.Feature.BrowserCompatible);
        } catch (Exception e) {
            log.error("JSON 序列化（浏览器兼容）失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 对象序列化为 JSON 字节数组（UTF-8）
     *
     * @param obj 待序列化对象
     * @return JSON 字节数组；obj 为 null 返回 null
     */
    public static byte[] toJSONBytes(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return JSON.toJSONBytes(obj);
        } catch (Exception e) {
            log.error("JSON 序列化字节失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 对象序列化为 JSON 字节数组（指定字符集）
     *
     * @param obj     待序列化对象
     * @param charset 字符集
     * @return JSON 字节数组；obj 为 null 返回 null
     */
    public static byte[] toJSONBytes(Object obj, Charset charset) {
        if (obj == null) {
            return null;
        }
        try {
            Charset cs = charset != null ? charset : DEFAULT_CHARSET;
            return JSON.toJSONBytes(obj, cs);
        } catch (Exception e) {
            log.error("JSON 序列化字节失败: {}", e.getMessage(), e);
            return null;
        }
    }

    // ==================== 反序列化：对象 ====================

    /**
     * JSON 字符串反序列化为对象
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 反序列化对象；json 为空或解析失败返回 null
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json) || clazz == null) {
            return null;
        }
        try {
            return JSON.parseObject(json, clazz);
        } catch (Exception e) {
            log.error("JSON 反序列化失败 [{}]: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字符串反序列化为对象（指定 Type）
     *
     * @param json JSON 字符串
     * @param type 目标类型
     * @param <T>  目标类型泛型
     * @return 反序列化对象；json 为空或解析失败返回 null
     */
    public static <T> T parseObject(String json, Type type) {
        if (StringUtils.isBlank(json) || type == null) {
            return null;
        }
        try {
            return JSON.parseObject(json, type);
        } catch (Exception e) {
            log.error("JSON 反序列化失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字符串反序列化为对象（TypeReference，支持泛型）
     *
     * @param json          JSON 字符串
     * @param typeReference 类型引用
     * @param <T>           目标类型泛型
     * @return 反序列化对象；json 为空或解析失败返回 null
     */
    public static <T> T parseObject(String json, TypeReference<T> typeReference) {
        if (StringUtils.isBlank(json) || typeReference == null) {
            return null;
        }
        try {
            return JSON.parseObject(json, typeReference);
        } catch (Exception e) {
            log.error("JSON 反序列化失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字符串反序列化为对象（自定义反序列化特性）
     *
     * @param json     JSON 字符串
     * @param clazz    目标类型
     * @param features 反序列化特性
     * @param <T>      目标类型泛型
     * @return 反序列化对象；json 为空或解析失败返回 null
     */
    public static <T> T parseObject(String json, Class<T> clazz, JSONReader.Feature... features) {
        if (StringUtils.isBlank(json) || clazz == null) {
            return null;
        }
        try {
            if (features == null || features.length == 0) {
                return JSON.parseObject(json, clazz);
            }
            return JSON.parseObject(json, clazz, features);
        } catch (Exception e) {
            log.error("JSON 反序列化失败 [{}]: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字节数组反序列化为对象
     *
     * @param bytes JSON 字节数组
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 反序列化对象；bytes 为空或解析失败返回 null
     */
    public static <T> T parseObject(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0 || clazz == null) {
            return null;
        }
        try {
            return JSON.parseObject(bytes, clazz);
        } catch (Exception e) {
            log.error("JSON 反序列化失败 [{}]: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * JSON 字节数组反序列化为对象（TypeReference，支持泛型）
     *
     * @param bytes         JSON 字节数组
     * @param typeReference 类型引用
     * @param <T>           目标类型泛型
     * @return 反序列化对象；bytes 为空或解析失败返回 null
     */
    public static <T> T parseObject(byte[] bytes, TypeReference<T> typeReference) {
        if (bytes == null || bytes.length == 0 || typeReference == null) {
            return null;
        }
        try {
            return JSON.parseObject(bytes, typeReference.getType());
        } catch (Exception e) {
            log.error("JSON 反序列化失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 反序列化：集合 ====================

    /**
     * JSON 字符串反序列化为 List
     *
     * @param json  JSON 数组字符串
     * @param clazz 元素类型
     * @param <T>   元素类型泛型
     * @return List 实例；json 为空或解析失败返回空列表
     */
    public static <T> List<T> parseList(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json) || clazz == null) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(json, clazz);
        } catch (Exception e) {
            log.error("JSON 数组反序列化失败 [{}]: {}", clazz.getSimpleName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * JSON 字符串反序列化为 Map
     *
     * @param json JSON 对象字符串
     * @return Map 实例；json 为空或解析失败返回空 Map
     */
    public static Map<String, Object> parseMap(String json) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyMap();
        }
        try {
            return JSON.parseObject(json);
        } catch (Exception e) {
            log.error("JSON Map 反序列化失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * JSON 字符串反序列化为指定类型的 Map
     *
     * @param json      JSON 对象字符串
     * @param valueType Map 值类型
     * @param <V>       值类型泛型
     * @return Map 实例；json 为空或解析失败返回空 Map
     */
    public static <V> Map<String, V> parseMap(String json, Class<V> valueType) {
        if (StringUtils.isBlank(json) || valueType == null) {
            return Collections.emptyMap();
        }
        try {
            return JSON.parseObject(json, new TypeReference<Map<String, V>>(valueType) {});
        } catch (Exception e) {
            log.error("JSON Map 反序列化失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ==================== 解析为 JSONObject / JSONArray ====================

    /**
     * 解析为 JSONObject
     *
     * @param json JSON 字符串
     * @return JSONObject 实例；解析失败返回 null
     */
    public static JSONObject parseJSONObject(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return JSON.parseObject(json);
        } catch (Exception e) {
            log.error("JSONObject 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析为 JSONArray
     *
     * @param json JSON 字符串
     * @return JSONArray 实例；解析失败返回 null
     */
    public static JSONArray parseJSONArray(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return JSON.parseArray(json);
        } catch (Exception e) {
            log.error("JSONArray 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析为 Object（自动判断类型：JSONObject / JSONArray / 基本类型）
     *
     * @param json JSON 字符串
     * @return 解析后的对象；json 为空或解析失败返回 null
     */
    public static Object parse(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return JSON.parse(json);
        } catch (Exception e) {
            log.error("JSON 解析失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 值提取 ====================

    /**
     * 从 JSON 字符串中获取指定 key 的 String 值
     *
     * @param json JSON 字符串
     * @param key  键名
     * @return 值；不存在返回 null
     */
    public static String getString(String json, String key) {
        JSONObject obj = parseJSONObject(json);
        return obj != null ? obj.getString(key) : null;
    }

    /**
     * 从 JSON 字符串中获取指定 key 的 Integer 值
     *
     * @param json JSON 字符串
     * @param key  键名
     * @return 值；不存在返回 null
     */
    public static Integer getInteger(String json, String key) {
        JSONObject obj = parseJSONObject(json);
        return obj != null ? obj.getInteger(key) : null;
    }

    /**
     * 从 JSON 字符串中获取指定 key 的 Long 值
     *
     * @param json JSON 字符串
     * @param key  键名
     * @return 值；不存在返回 null
     */
    public static Long getLong(String json, String key) {
        JSONObject obj = parseJSONObject(json);
        return obj != null ? obj.getLong(key) : null;
    }

    /**
     * 从 JSON 字符串中获取指定 key 的 Boolean 值
     *
     * @param json JSON 字符串
     * @param key  键名
     * @return 值；不存在返回 null
     */
    public static Boolean getBoolean(String json, String key) {
        JSONObject obj = parseJSONObject(json);
        return obj != null ? obj.getBoolean(key) : null;
    }

    /**
     * 从 JSON 字符串中获取指定 key 的 JSONObject
     *
     * @param json JSON 字符串
     * @param key  键名
     * @return JSONObject；不存在返回 null
     */
    public static JSONObject getJSONObject(String json, String key) {
        JSONObject obj = parseJSONObject(json);
        return obj != null ? obj.getJSONObject(key) : null;
    }

    /**
     * 从 JSON 字符串中获取指定 key 的 JSONArray
     *
     * @param json JSON 字符串
     * @param key  键名
     * @return JSONArray；不存在返回 null
     */
    public static JSONArray getJSONArray(String json, String key) {
        JSONObject obj = parseJSONObject(json);
        return obj != null ? obj.getJSONArray(key) : null;
    }

    // ==================== JSON 校验 ====================

    /**
     * 判断字符串是否为合法 JSON
     *
     * @param json 待校验字符串
     * @return true 表示合法 JSON
     */
    public static boolean isValidJSON(String json) {
        if (StringUtils.isBlank(json)) {
            return false;
        }
        try {
            JSON.parse(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为合法 JSON 对象
     *
     * @param json 待校验字符串
     * @return true 表示合法 JSON 对象
     */
    public static boolean isJSONObject(String json) {
        if (StringUtils.isBlank(json)) {
            return false;
        }
        try {
            Object parsed = JSON.parse(json);
            return parsed instanceof JSONObject;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为合法 JSON 数组
     *
     * @param json 待校验字符串
     * @return true 表示合法 JSON 数组
     */
    public static boolean isJSONArray(String json) {
        if (StringUtils.isBlank(json)) {
            return false;
        }
        try {
            Object parsed = JSON.parse(json);
            return parsed instanceof JSONArray;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== JSON 格式化 ====================

    /**
     * 格式化 JSON 字符串（美化输出）
     *
     * @param json JSON 字符串
     * @return 格式化后的 JSON 字符串；非法 JSON 返回原值
     */
    public static String format(String json) {
        if (StringUtils.isBlank(json)) {
            return json;
        }
        try {
            Object parsed = JSON.parse(json);
            return JSON.toJSONString(parsed, JSONWriter.Feature.PrettyFormat);
        } catch (Exception e) {
            return json;
        }
    }

    /**
     * 压缩 JSON 字符串（去除空白字符）
     *
     * @param json JSON 字符串
     * @return 压缩后的 JSON 字符串；非法 JSON 返回原值
     */
    public static String compress(String json) {
        if (StringUtils.isBlank(json)) {
            return json;
        }
        try {
            Object parsed = JSON.parse(json);
            return JSON.toJSONString(parsed);
        } catch (Exception e) {
            return json;
        }
    }

    // ==================== 字段过滤 ====================

    /**
     * 序列化时排除指定字段
     *
     * @param obj    待序列化对象
     * @param fields 要排除的字段名
     * @return JSON 字符串；obj 为 null 返回 null
     */
    public static String toJSONExclude(Object obj, String... fields) {
        if (obj == null) {
            return null;
        }
        if (fields == null || fields.length == 0) {
            return toJSON(obj);
        }
        // 使用 JSONObject 转换为 Map 后删除字段
        try {
            JSONObject jsonObject = JSONObject.from(obj);
            for (String field : fields) {
                jsonObject.remove(field);
            }
            return jsonObject.toJSONString();
        } catch (Exception e) {
            log.error("JSON 序列化（排除字段）失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 序列化时仅包含指定字段
     *
     * @param obj    待序列化对象
     * @param fields 要包含的字段名
     * @return JSON 字符串；obj 为 null 返回 null
     */
    public static String toJSONInclude(Object obj, String... fields) {
        if (obj == null) {
            return null;
        }
        if (fields == null || fields.length == 0) {
            return toJSON(obj);
        }
        try {
            JSONObject jsonObject = JSONObject.from(obj);
            Set<String> includeSet = new HashSet<>(Arrays.asList(fields));
            JSONObject result = new JSONObject();
            for (String field : includeSet) {
                if (jsonObject.containsKey(field)) {
                    result.put(field, jsonObject.get(field));
                }
            }
            return result.toJSONString();
        } catch (Exception e) {
            log.error("JSON 序列化（包含字段）失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 深拷贝 ====================

    /**
     * 深拷贝对象（通过 JSON 序列化/反序列化实现）
     *
     * @param obj   原对象
     * @param clazz 目标类
     * @param <T>   目标类型泛型
     * @return 深拷贝后的新对象；拷贝失败返回 null
     */
    public static <T> T deepCopy(T obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        try {
            String json = JSON.toJSONString(obj);
            return JSON.parseObject(json, clazz);
        } catch (Exception e) {
            log.error("深拷贝对象失败 [{}]: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * 深拷贝对象（支持泛型）
     *
     * @param obj           原对象
     * @param typeReference 类型引用
     * @param <T>           目标类型泛型
     * @return 深拷贝后的新对象；拷贝失败返回 null
     */
    public static <T> T deepCopy(T obj, TypeReference<T> typeReference) {
        if (obj == null) {
            return null;
        }
        try {
            String json = JSON.toJSONString(obj);
            return JSON.parseObject(json, typeReference);
        } catch (Exception e) {
            log.error("深拷贝对象失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 合并 ====================

    /**
     * 合并两个 JSON 对象（后者覆盖前者）
     *
     * @param json1 第一个 JSON
     * @param json2 第二个 JSON（优先级更高）
     * @return 合并后的 JSON 字符串
     */
    public static String merge(String json1, String json2) {
        JSONObject obj1 = parseJSONObject(json1);
        JSONObject obj2 = parseJSONObject(json2);
        if (obj1 == null && obj2 == null) {
            return null;
        }
        if (obj1 == null) {
            return toJSON(obj2);
        }
        if (obj2 == null) {
            return toJSON(obj1);
        }
        obj1.putAll(obj2);
        return toJSON(obj1);
    }
}