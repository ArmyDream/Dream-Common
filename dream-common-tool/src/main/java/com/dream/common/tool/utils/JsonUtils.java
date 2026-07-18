package com.dream.common.tool.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import com.alibaba.fastjson2.filter.Filter;
import com.alibaba.fastjson2.filter.SimplePropertyPreFilter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <p>
 * 封装 {@link JSON} 核心 API，提供类型安全、高性能的 JSON 序列化/反序列化能力。
 * 所有公共方法 null-safe，解析失败时记录日志并返回安全的默认值。
 * <p>
 * 核心能力：
 * <ul>
 *   <li>序列化 —— {@link #toJson(Object)} / {@link #toJsonPretty(Object)}</li>
 *   <li>反序列化 —— {@link #parseObject(String, Class)} / {@link #parseList(String, Class)}</li>
 *   <li>字段过滤 —— {@link #toJsonExclude(Object, String...)} / {@link #toJsonInclude(Object, String...)}</li>
 *   <li>值提取 —— {@link #getString(String, String)} 等</li>
 *   <li>格式校验 —— {@link #isValidJson(String)}</li>
 *   <li>深拷贝 —— {@link #deepCopy(Object, Class)}</li>
 *   <li>对象合并 —— {@link #merge(String, String)}</li>
 * </ul>
 *
 * @author dream
 */
@Slf4j
@SuppressWarnings("all")
public final class JsonUtils {

    // ==================== 常量 ====================

    /**
     * 默认日期格式
     */
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 默认字符集
     */
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * 空 JSON 对象字符串
     */
    public static final String EMPTY_JSON_OBJECT = "{}";

    /**
     * 空 JSON 数组字符串
     */
    public static final String EMPTY_JSON_ARRAY = "[]";

    // ==================== 预定义序列化特性组合 ====================

    /**
     * 默认序列化（不输出 null）
     */
    public static final JSONWriter.Feature[] FEATURE_DEFAULT = {};

    /**
     * 美化输出（带缩进，枚举使用名称）
     */
    public static final JSONWriter.Feature[] FEATURE_PRETTY = {
            JSONWriter.Feature.PrettyFormat,
            JSONWriter.Feature.WriteEnumsUsingName
    };

    /**
     * 输出 null 值（Map、String、List）
     */
    public static final JSONWriter.Feature[] FEATURE_WRITE_NULLS = {
            JSONWriter.Feature.WriteMapNullValue,
            JSONWriter.Feature.WriteNullStringAsEmpty,
            JSONWriter.Feature.WriteNullListAsEmpty
    };

    /**
     * 输出所有 null 值（含数字、布尔）
     */
    public static final JSONWriter.Feature[] FEATURE_WRITE_ALL_NULLS = {
            JSONWriter.Feature.WriteMapNullValue,
            JSONWriter.Feature.WriteNullStringAsEmpty,
            JSONWriter.Feature.WriteNullNumberAsZero,
            JSONWriter.Feature.WriteNullBooleanAsFalse,
            JSONWriter.Feature.WriteNullListAsEmpty
    };

    /**
     * 浏览器兼容模式（Long → String，枚举使用名称）
     */
    public static final JSONWriter.Feature[] FEATURE_BROWSER = {
            JSONWriter.Feature.BrowserCompatible,
            JSONWriter.Feature.WriteEnumsUsingName
    };

    /**
     * 全特性组合（null 值 + 美化 + 浏览器兼容）
     */
    public static final JSONWriter.Feature[] FEATURE_FULL = {
            JSONWriter.Feature.WriteMapNullValue,
            JSONWriter.Feature.WriteNullStringAsEmpty,
            JSONWriter.Feature.WriteNullNumberAsZero,
            JSONWriter.Feature.WriteNullBooleanAsFalse,
            JSONWriter.Feature.WriteNullListAsEmpty,
            JSONWriter.Feature.PrettyFormat,
            JSONWriter.Feature.WriteEnumsUsingName,
            JSONWriter.Feature.BrowserCompatible
    };


    private static final JSONWriter.Feature[] FEATURE_DEFAULT_SIMPLE = {
            JSONWriter.Feature.WriteMapNullValue,
            JSONWriter.Feature.WriteNullStringAsEmpty,
            JSONWriter.Feature.WriteNullListAsEmpty,
            JSONWriter.Feature.BrowserCompatible,
            JSONWriter.Feature.WriteEnumsUsingName,
    };

    // ==================== 预定义反序列化特性组合 ====================

    /**
     * 默认反序列化
     */
    public static final JSONReader.Feature[] READ_FEATURE_DEFAULT = {};

    /**
     * 宽松反序列化特性（兼容非标准 JSON）
     * <ul>
     *   <li>{@link JSONReader.Feature#AllowUnQuotedFieldNames} —— 允许字段名无引号</li>
     * </ul>
     * 注意：fastjson 2.0.62 中无 AllowSingleQuotes，已移除；SupportSmartMatch 已移除以确保兼容性。
     */
    public static final JSONReader.Feature[] READ_FEATURE_LENIENT = {
            JSONReader.Feature.AllowUnQuotedFieldNames
    };

    private JsonUtils() {
        throw new UnsupportedOperationException("JsonUtils is a utility class and cannot be instantiated");
    }

    // ==================== 序列化（核心） ====================

    /**
     * 对象序列化为 JSON 字符串（默认特性）
     *
     * @param obj 待序列化对象
     * @return JSON 字符串；obj 为 null 返回 null
     */
    public static String toJson(Object obj) {
        return toJson(obj, FEATURE_DEFAULT);
    }

    /**
     * 对象序列化为 JSON 字符串（自定义特性）
     *
     * @param obj      待序列化对象
     * @param features 序列化特性
     * @return JSON 字符串；obj 为 null 返回 null
     */
    public static String toJson(Object obj, JSONWriter.Feature... features) {
        if (obj == null) {
            return null;
        }
        try {
            final JSONWriter.Feature[] feats = defaultIfEmpty(features, FEATURE_DEFAULT);
            return JSON.toJSONString(obj, feats);
        } catch (Exception e) {
            log.error("Json serialization failed: {}", e.getMessage(), e);
            return null;
        }
    }

    // ==================== 序列化（快捷方法） ====================

    /**
     * 美化序列化（带缩进和换行）
     *
     * @param obj 待序列化对象
     * @return 美化后的 JSON 字符串；obj 为 null 返回 null
     */
    public static String toJsonPretty(Object obj) {
        return toJson(obj, FEATURE_PRETTY);
    }

    /**
     * 序列化并输出 null 值字段
     *
     * @param obj 待序列化对象
     * @return JSON 字符串（含 null 值字段）；obj 为 null 返回 null
     */
    public static String toJsonWithNulls(Object obj) {
        return toJson(obj, FEATURE_WRITE_NULLS);
    }

    /**
     * 全特性序列化（null 值 + 美化 + 浏览器兼容）
     *
     * @param obj 待序列化对象
     * @return JSON 字符串；obj 为 null 返回 null
     */
    public static String toJsonFull(Object obj) {
        return toJson(obj, FEATURE_FULL);
    }

    /**
     * 浏览器兼容序列化（Long 转 String，枚举使用名称）
     *
     * @param obj 待序列化对象
     * @return JSON 字符串；obj 为 null 返回 null
     */
    public static String toJsonForBrowser(Object obj) {
        return toJson(obj, FEATURE_BROWSER);
    }

    /**
     * 简化序列化（null 值 + 浏览器兼容 + 枚举名称）
     *
     * @param obj 待序列化对象
     * @return JSON 字符串；obj 为 null 返回 null
     */
    public static String toJsonString(Object obj) {
        return toJson(obj, FEATURE_DEFAULT_SIMPLE);
    }

    // ==================== 序列化（字节） ====================

    /**
     * 序列化为 UTF-8 字节数组
     *
     * @param obj 待序列化对象
     * @return UTF-8 字节数组；obj 为 null 返回 null
     */
    public static byte[] toJsonBytes(Object obj) {
        return toJsonBytes(obj, DEFAULT_CHARSET);
    }

    /**
     * 序列化为指定字符集字节数组
     *
     * @param obj     待序列化对象
     * @param charset 字符集（null 时使用 UTF-8）
     * @return 字节数组；obj 为 null 返回 null
     */
    public static byte[] toJsonBytes(Object obj, Charset charset) {
        if (obj == null) {
            return null;
        }
        try {
            final Charset cs = charset != null ? charset : DEFAULT_CHARSET;
            return JSON.toJSONBytes(obj, cs);
        } catch (Exception e) {
            log.error("Json to bytes failed: {}", e.getMessage(), e);
            return null;
        }
    }

    // ==================== 反序列化：对象 ====================

    /**
     * 将对象反序列化为指定类型的 Bean
     * <p>
     * 支持 String 直接解析，其他类型先序列化再反序列化。
     * </p>
     *
     * @param obj      待反序列化对象（String 或其他可序列化对象）
     * @param clazz    目标类型
     * @param features 反序列化特性（可选）
     * @param <T>      目标类型泛型
     * @return 反序列化对象；obj 为空白或 clazz 为 null 返回 null
     */
    public static <T> T toBean(Object obj, Class<T> clazz, JSONReader.Feature... features) {
        if (StringUtils.isObjectBlank(obj) || clazz == null) {
            return null;
        }
        try {
            final String json = asJsonString(obj);
            if (json == null) {
                return null;
            }
            final JSONReader.Feature[] feats = defaultIfEmpty(features, READ_FEATURE_DEFAULT);
            return JSON.parseObject(json, clazz, feats);
        } catch (Exception e) {
            log.error("Json toBean to [{}] failed: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * 将对象反序列化为指定类型的 Bean（默认特性）
     *
     * @param obj   待反序列化对象
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 反序列化对象；失败返回 null
     */
    public static <T> T toBean(Object obj, Class<T> clazz) {
        return toBean(obj, clazz, null);
    }

    /**
     * 反序列化为指定类型对象（默认特性）
     *
     * @param obj   待反序列化对象（String 或其他可序列化对象）
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 反序列化对象；obj 为空白返回 null
     */
    public static <T> T parseObject(Object obj, Class<T> clazz) {
        return parseObject(obj, clazz, READ_FEATURE_DEFAULT);
    }

    /**
     * 反序列化为指定类型对象（自定义特性）
     *
     * @param obj      待反序列化对象
     * @param clazz    目标类型
     * @param features 反序列化特性
     * @param <T>      目标类型泛型
     * @return 反序列化对象；失败返回 null
     */
    public static <T> T parseObject(Object obj, Class<T> clazz, JSONReader.Feature... features) {
        if (StringUtils.isObjectBlank(obj) || clazz == null) {
            return null;
        }
        try {
            final String json = asJsonString(obj);
            if (json == null) {
                return null;
            }
            final JSONReader.Feature[] feats = defaultIfEmpty(features, READ_FEATURE_DEFAULT);
            return JSON.parseObject(json, clazz, feats);
        } catch (Exception e) {
            log.error("Json parse to [{}] failed: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * 反序列化为指定类型对象（Type 接口，支持泛型）
     *
     * @param obj  待反序列化对象
     * @param type 目标类型
     * @param <T>  目标类型泛型
     * @return 反序列化对象；失败返回 null
     */
    public static <T> T parseObject(Object obj, Type type) {
        if (StringUtils.isObjectBlank(obj) || type == null) {
            return null;
        }
        try {
            final String json = asJsonString(obj);
            if (json == null) {
                return null;
            }
            return JSON.parseObject(json, type);
        } catch (Exception e) {
            log.error("Json parse to [{}] failed: {}", type.getTypeName(), e.getMessage());
            return null;
        }
    }

    /**
     * 反序列化为指定类型对象（TypeReference，支持泛型复合类型）
     *
     * @param obj           待反序列化对象
     * @param typeReference 类型引用
     * @param <T>           目标类型泛型
     * @return 反序列化对象；失败返回 null
     */
    public static <T> T parseObject(Object obj, TypeReference<T> typeReference) {
        if (StringUtils.isObjectBlank(obj) || typeReference == null) {
            return null;
        }
        try {
            final String json = asJsonString(obj);
            if (json == null) {
                return null;
            }
            return JSON.parseObject(json, typeReference);
        } catch (Exception e) {
            log.error("Json parse to TypeReference failed: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 反序列化：字节 → 对象 ====================

    /**
     * 字节数组反序列化为指定类型对象
     *
     * @param bytes UTF-8 JSON 字节数组
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
            log.error("Json bytes parse to [{}] failed: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * 字节数组反序列化（TypeReference，支持泛型复合类型）
     *
     * @param bytes         UTF-8 JSON 字节数组
     * @param typeReference 类型引用
     * @param <T>           目标类型泛型
     * @return 反序列化对象；失败返回 null
     */
    public static <T> T parseObject(byte[] bytes, TypeReference<T> typeReference) {
        if (bytes == null || bytes.length == 0 || typeReference == null) {
            return null;
        }
        try {
            return JSON.parseObject(bytes, typeReference.getType());
        } catch (Exception e) {
            log.error("Json bytes parse to TypeReference failed: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 反序列化：集合 ====================


    /**
     * JSON 数组反序列化为 List
     * <p>
     * 支持 String 直接解析，其他类型先序列化为 JSON 数组再解析。
     * </p>
     *
     * @param obj      待反序列化对象（JSON 数组字符串或其他可序列化对象）
     * @param clazz    元素类型
     * @param features 反序列化特性（可选）
     * @param <T>      元素类型泛型
     * @return List 集合；obj 为空白或解析失败返回空 List
     */
    public static <T> List<T> toList(Object obj, Class<T> clazz, JSONReader.Feature... features) {
        if (StringUtils.isObjectBlank(obj) || clazz == null) {
            return Collections.emptyList();
        }
        try {
            final String json = asJsonString(obj);
            if (json == null) {
                return Collections.emptyList();
            }
            final JSONReader.Feature[] feats = defaultIfEmpty(features, READ_FEATURE_DEFAULT);
            return JSON.parseArray(json, clazz, feats);
        } catch (Exception e) {
            log.error("Json toList to List<{}> failed: {}", clazz.getSimpleName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * JSON 数组反序列化为 List（默认特性）
     *
     * @param obj   待反序列化对象
     * @param clazz 元素类型
     * @param <T>   元素类型泛型
     * @return List 集合；失败返回空 List
     */
    public static <T> List<T> toList(Object obj, Class<T> clazz) {
        return toList(obj, clazz, null);
    }

    /**
     * JSON 对象反序列化为 Map&lt;String, Object&gt;
     *
     * @param obj 待反序列化对象
     * @return Map 集合；obj 为空白或解析失败返回空 Map
     */
    public static Map<String, Object> parseMap(Object obj) {
        if (StringUtils.isObjectBlank(obj)) {
            return Collections.emptyMap();
        }
        try {
            final String json = asJsonString(obj);
            if (json == null) {
                return Collections.emptyMap();
            }
            return JSON.parseObject(json);
        } catch (Exception e) {
            log.error("Json parse to Map failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * JSON 对象反序列化为 Map&lt;String, V&gt;（指定值类型）
     *
     * @param obj       待反序列化对象
     * @param valueType 值类型
     * @param <V>       值类型泛型
     * @return Map 集合；失败返回空 Map
     */
    public static <V> Map<String, V> parseMap(Object obj, Class<V> valueType) {
        if (StringUtils.isObjectBlank(obj) || valueType == null) {
            return Collections.emptyMap();
        }
        try {
            final String json = asJsonString(obj);
            if (json == null) {
                return Collections.emptyMap();
            }
            return JSON.parseObject(json, new TypeReference<Map<String, V>>(valueType) {});
        } catch (Exception e) {
            log.error("Json parse to Map<String, {}> failed: {}", valueType.getSimpleName(), e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ==================== JSONObject / JSONArray ====================

    /**
     * 安全解析为 JSONObject
     *
     * @param obj 待解析对象（String 或其他可序列化对象）
     * @return JSONObject；obj 为空白或解析失败返回 null
     */
    public static JSONObject parseJsonObject(Object obj) {
        if (StringUtils.isObjectBlank(obj)) {
            return null;
        }
        try {
            final String json = asJsonString(obj);
            return json != null ? JSON.parseObject(json) : null;
        } catch (Exception e) {
            log.error("Json parse to JSONObject failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 安全解析为 JSONArray
     *
     * @param obj 待解析对象
     * @return JSONArray；obj 为空白或解析失败返回 null
     */
    public static JSONArray parseJsonArray(Object obj) {
        if (StringUtils.isObjectBlank(obj)) {
            return null;
        }
        try {
            final String json = asJsonString(obj);
            return json != null ? JSON.parseArray(json) : null;
        } catch (Exception e) {
            log.error("Json parse to JSONArray failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 通用解析（自动推断 JSONObject 或 JSONArray）
     *
     * @param obj 待解析对象
     * @return JSONObject 或 JSONArray；obj 为空白或解析失败返回 null
     */
    public static Object parse(Object obj) {
        if (StringUtils.isObjectBlank(obj)) {
            return null;
        }
        try {
            final String json = asJsonString(obj);
            return json != null ? JSON.parse(json) : null;
        } catch (Exception e) {
            log.error("Json parse failed: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 安全值提取 ====================

    /**
     * 从 JSON 字符串中提取 String 值
     *
     * @param json JSON 字符串
     * @param key  字段名
     * @return 字段值；不存在或类型不匹配返回 null
     */
    public static String getString(String json, String key) {
        return safeExtract(json, key, obj -> obj.getString(key));
    }

    /**
     * 从 JSON 字符串中提取 Integer 值
     *
     * @param json JSON 字符串
     * @param key  字段名
     * @return 字段值；不存在或类型不匹配返回 null
     */
    public static Integer getInteger(String json, String key) {
        return safeExtract(json, key, obj -> obj.getInteger(key));
    }

    /**
     * 从 JSON 字符串中提取 Long 值
     *
     * @param json JSON 字符串
     * @param key  字段名
     * @return 字段值；不存在或类型不匹配返回 null
     */
    public static Long getLong(String json, String key) {
        return safeExtract(json, key, obj -> obj.getLong(key));
    }

    /**
     * 从 JSON 字符串中提取 Boolean 值
     *
     * @param json JSON 字符串
     * @param key  字段名
     * @return 字段值；不存在或类型不匹配返回 null
     */
    public static Boolean getBoolean(String json, String key) {
        return safeExtract(json, key, obj -> obj.getBoolean(key));
    }

    /**
     * 从 JSON 字符串中提取 Double 值
     *
     * @param json JSON 字符串
     * @param key  字段名
     * @return 字段值；不存在或类型不匹配返回 null
     */
    public static Double getDouble(String json, String key) {
        return safeExtract(json, key, obj -> obj.getDouble(key));
    }

    /**
     * 从 JSON 字符串中提取嵌套 JSONObject
     *
     * @param json JSON 字符串
     * @param key  字段名
     * @return 嵌套 JSONObject；不存在返回 null
     */
    public static JSONObject getJsonObject(String json, String key) {
        return safeExtract(json, key, obj -> obj.getJSONObject(key));
    }

    /**
     * 从 JSON 字符串中提取 JSONArray
     *
     * @param json JSON 字符串
     * @param key  字段名
     * @return JSONArray；不存在返回 null
     */
    public static JSONArray getJsonArray(String json, String key) {
        return safeExtract(json, key, obj -> obj.getJSONArray(key));
    }

    /**
     * 通用安全提取模板
     * <p>
     * 对 json 和 key 做 null 安全校验，异常时记录 debug 日志并返回 null。
     * </p>
     *
     * @param json    JSON 字符串
     * @param key     字段名（用于日志）
     * @param extract 提取逻辑
     * @param <T>     返回值类型
     * @return 提取的值；任何异常返回 null
     */
    private static <T> T safeExtract(String json, String key, Function<JSONObject, T> extract) {
        if (StringUtils.isBlank(json) || StringUtils.isBlank(key)) {
            return null;
        }
        final JSONObject obj = parseJsonObject(json);
        if (obj == null) {
            return null;
        }
        try {
            return extract.apply(obj);
        } catch (Exception e) {
            log.debug("Extract value from JSON failed: key={}", key);
            return null;
        }
    }

    // ==================== JSON 校验 ====================

    /**
     * 判断字符串是否为合法 JSON（对象或数组）
     * <p>
     * 使用 {@link JSON#isValid(String)} 轻量级校验，不创建对象。
     * </p>
     *
     * @param json 待校验字符串
     * @return true 表示合法 JSON
     */
    public static boolean isValidJson(String json) {
        return checkJson(json, false, false);
    }

    /**
     * 判断字符串是否为合法 JSON 对象（以 { 开头）
     *
     * @param json 待校验字符串
     * @return true 表示合法 JSON 对象
     */
    public static boolean isJsonObject(String json) {
        return checkJson(json, true, false);
    }

    /**
     * 判断字符串是否为合法 JSON 数组（以 [ 开头）
     *
     * @param json 待校验字符串
     * @return true 表示合法 JSON 数组
     */
    public static boolean isJsonArray(String json) {
        return checkJson(json, false, true);
    }

    /**
     * 统一校验逻辑
     *
     * @param json      待校验字符串
     * @param expectObj 是否必须为 JSONObject
     * @param expectArr 是否必须为 JSONArray
     * @return 是否合法
     */
    private static boolean checkJson(String json, boolean expectObj, boolean expectArr) {
        if (StringUtils.isBlank(json)) {
            return false;
        }
        // 简单校验：只判断是否合法 JSON，使用轻量级 isValid
        if (!expectObj && !expectArr) {
            return JSON.isValid(json);
        }
        // 需要区分类型时，必须解析
        try {
            final Object parsed = JSON.parse(json);
            if (expectObj) {
                return parsed instanceof JSONObject;
            }
            if (expectArr) {
                return parsed instanceof JSONArray;
            }
            return parsed instanceof JSONObject || parsed instanceof JSONArray;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 格式化与压缩 ====================

    /**
     * 格式化 JSON（美化输出，带缩进和换行）
     *
     * @param json JSON 字符串
     * @return 格式化后的 JSON；json 为空白或解析失败返回原值
     */
    public static String format(String json) {
        if (StringUtils.isBlank(json)) {
            return json;
        }
        try {
            final Object parsed = JSON.parse(json);
            return JSON.toJSONString(parsed, FEATURE_PRETTY);
        } catch (Exception e) {
            log.debug("Json format failed, returning original: {}", e.getMessage());
            return json;
        }
    }

    /**
     * 压缩 JSON（移除多余空白字符）
     *
     * @param json JSON 字符串
     * @return 压缩后的 JSON；json 为空白或解析失败返回原值
     */
    public static String compress(String json) {
        if (StringUtils.isBlank(json)) {
            return json;
        }
        try {
            final Object parsed = JSON.parse(json);
            return JSON.toJSONString(parsed, FEATURE_DEFAULT);
        } catch (Exception e) {
            log.debug("Json compress failed, returning original: {}", e.getMessage());
            return json;
        }
    }

    // ==================== 字段过滤（基于 Fastjson2 Filter） ====================

    /**
     * 序列化时排除指定字段（使用 SimplePropertyPreFilter，性能更优）
     *
     * @param obj           待序列化对象
     * @param excludeFields 要排除的字段名
     * @return JSON 字符串；obj 为 null 返回 null
     */
    public static String toJsonExclude(Object obj, String... excludeFields) {
        if (obj == null) {
            return null;
        }
        if (excludeFields == null || excludeFields.length == 0) {
            return toJson(obj);
        }
        try {
            final SimplePropertyPreFilter filter = new SimplePropertyPreFilter();
            filter.getExcludes().addAll(Arrays.asList(excludeFields));
            return JSON.toJSONString(obj, filter, FEATURE_DEFAULT);
        } catch (Exception e) {
            log.error("Json serialization with exclude fields failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 序列化时仅包含指定字段
     *
     * @param obj           待序列化对象
     * @param includeFields 要包含的字段名
     * @return JSON 字符串；obj 为 null 返回 null
     */
    public static String toJsonInclude(Object obj, String... includeFields) {
        if (obj == null) {
            return null;
        }
        if (includeFields == null || includeFields.length == 0) {
            return toJson(obj);
        }
        try {
            final SimplePropertyPreFilter filter = new SimplePropertyPreFilter();
            filter.getIncludes().addAll(Arrays.asList(includeFields));
            return JSON.toJSONString(obj, filter, FEATURE_DEFAULT);
        } catch (Exception e) {
            log.error("Json serialization with include fields failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 序列化时使用自定义 Filter
     *
     * @param obj     待序列化对象
     * @param filters 自定义过滤器
     * @return JSON 字符串
     */
    public static String toJsonWithFilter(Object obj, Filter... filters) {
        if (obj == null) {
            return null;
        }
        try {
            return JSON.toJSONString(obj, filters, FEATURE_DEFAULT);
        } catch (Exception e) {
            log.error("Json serialization with filter failed: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 深拷贝 ====================

    /**
     * 基于 JSON 的深拷贝（对象必须可 JSON 序列化）
     *
     * @param obj   源对象
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 深拷贝对象；失败返回 null
     */
    public static <T> T deepCopy(T obj, Class<T> clazz) {
        if (obj == null || clazz == null) {
            return null;
        }
        try {
            final String json = JSON.toJSONString(obj);
            return JSON.parseObject(json, clazz);
        } catch (Exception e) {
            log.error("Deep copy to [{}] failed: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * 深拷贝（TypeReference，支持泛型复合类型）
     *
     * @param obj           源对象
     * @param typeReference 目标类型引用
     * @param <T>           目标类型泛型
     * @return 深拷贝对象；失败返回 null
     */
    public static <T> T deepCopy(T obj, TypeReference<T> typeReference) {
        if (obj == null || typeReference == null) {
            return null;
        }
        try {
            final String json = JSON.toJSONString(obj);
            return JSON.parseObject(json, typeReference);
        } catch (Exception e) {
            log.error("Deep copy to TypeReference failed: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 合并 ====================

    /**
     * 合并两个 JSON 对象（后者覆盖前者同名字段）
     *
     * @param baseJson     基础 JSON
     * @param overrideJson 覆盖 JSON
     * @return 合并后的 JSON 字符串
     */
    public static String merge(String baseJson, String overrideJson) {
        final JSONObject base = parseJsonObject(baseJson);
        final JSONObject override = parseJsonObject(overrideJson);

        if (base == null && override == null) {
            return null;
        }
        if (base == null) {
            return toJson(override);
        }
        if (override == null) {
            return toJson(base);
        }

        base.putAll(override);
        return toJson(base);
    }

    // ==================== 条件序列化 ====================

    /**
     * 条件序列化（延迟求值，避免不必要的序列化开销）
     *
     * @param condition 条件
     * @param supplier  序列化数据提供者
     * @return 条件成立时返回 JSON，否则返回 null
     */
    public static String toJsonIf(boolean condition, Supplier<Object> supplier) {
        if (!condition || supplier == null) {
            return null;
        }
        return toJson(supplier.get());
    }

    // ==================== 内部工具 ====================

    /**
     * 将对象转为 JSON 字符串（内部辅助）
     * <p>
     * 如果 obj 本身是 String 则直接返回，否则序列化为 JSON 字符串。
     * </p>
     *
     * @param obj 待转换对象
     * @return JSON 字符串；obj 为空白时返回 null
     */
    private static String asJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            String s = (String) obj;
            return s.isEmpty() ? null : s;
        }
        return JSON.toJSONString(obj);
    }

    private static JSONWriter.Feature[] defaultIfEmpty(JSONWriter.Feature[] features, JSONWriter.Feature[] defaultFeatures) {
        return (features != null && features.length > 0) ? features : defaultFeatures;
    }

    private static JSONReader.Feature[] defaultIfEmpty(JSONReader.Feature[] features, JSONReader.Feature[] defaultFeatures) {
        return (features != null && features.length > 0) ? features : defaultFeatures;
    }
}