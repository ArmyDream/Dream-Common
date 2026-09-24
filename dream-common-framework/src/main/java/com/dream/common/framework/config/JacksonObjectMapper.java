package com.dream.common.framework.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 全局 Jackson 对象映射器
 * <p>
 * 统一配置序列化 / 反序列化规则，解决以下问题：
 * <ul>
 *   <li>Long / BigInteger / BigDecimal 序列化为 String（防止前端 JavaScript 精度丢失）</li>
 *   <li>JSR-310 日期时间格式化（LocalDateTime / LocalDate / LocalTime）</li>
 *   <li>未知属性反序列化容错（前后端字段不一致时不报错）</li>
 *   <li>空 Bean / 空对象序列化容错</li>
 *   <li>统一响应契约（{@link JacksonContractModule}）：R 按内部 Map 输出、BaseEnum ⇄ value</li>
 * </ul>
 * </p>
 *
 * <h2>使用方式</h2>
 * <pre>
 * // 1. 直接使用
 * ObjectMapper mapper = new JacksonObjectMapper();
 * String json = mapper.writeValueAsString(user);
 *
 * // 2. Spring MVC 自动注入
 * {@code @Configuration}
 * public class WebConfig implements WebMvcConfigurer {
 *     {@code @Override}
 *     public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
 *         converters.add(0, new MappingJackson2HttpMessageConverter(new JacksonObjectMapper()));
 *     }
 * }
 * </pre>
 *
 * @author dream
 */
public class JacksonObjectMapper extends ObjectMapper {

    /** 默认日期时间格式 */
    private static final String DEFAULT_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** 默认日期格式 */
    private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";

    /** 默认时间格式 */
    private static final String DEFAULT_TIME_PATTERN = "HH:mm:ss";

    public JacksonObjectMapper() {
        super();

        // ==================== 反序列化配置 ====================
        // 1. 忽略未知属性（前后端字段不一致时不报错）
        this.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 2. 基本类型为 null 时不报错（int → null 不抛异常）
        this.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        // 3. 忽略被标记为忽略的属性
        this.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);

        // ==================== 序列化配置 ====================
        // 4. 空 Bean 不报错
        this.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // 5. 日期不输出时间戳（使用字符串格式）
        this.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 6. 禁用尾随小数零（1.0 → 1）
        this.disable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);

        // 7. 忽略 null 值字段（减少响应体积）
//        this.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 配置 java.util.Date 格式化
        this.setDateFormat(new SimpleDateFormat(DEFAULT_DATE_TIME_PATTERN));

        // ==================== 注册模块 ====================
        this.registerModule(javaTimeModule());
        this.registerModule(precisionSafeModule());
        // 统一响应契约：R 按内部 Map 输出（put() 字段不丢失）、BaseEnum ⇄ value
        this.registerModule(new JacksonContractModule());
    }

    /**
     * Java 8 时间类型模块
     * <p>
     * 统一格式化 LocalDateTime / LocalDate / LocalTime
     * </p>
     */
    private JavaTimeModule javaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();

        // LocalDateTime
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_PATTERN);
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

        // LocalDate
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_PATTERN);
        module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));

        // LocalTime
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(DEFAULT_TIME_PATTERN);
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

        return module;
    }

    /**
     * 精度安全序列化模块
     * <p>
     * Long / BigInteger / BigDecimal → String
     * </p>
     * <p>
     * 原因：
     * <ul>
     *   <li>JavaScript Number 安全整数上限为 2^53 - 1（≈ 9e15）</li>
     *   <li>Java Long 为 64 位（-2^63 ~ 2^63-1），超出 JS 安全范围</li>
     *   <li>BigInteger 无限制，JS 无法精确表示</li>
     *   <li>BigDecimal 存在浮点数精度丢失问题（0.1+0.2 ≠ 0.3）</li>
     * </ul>
     * </p>
     */
    private SimpleModule precisionSafeModule() {
        SimpleModule module = new SimpleModule("precisionSafeModule");

        // Long → String
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);

        // BigInteger → String
        module.addSerializer(BigInteger.class, ToStringSerializer.instance);

        // BigDecimal → String
        module.addSerializer(BigDecimal.class, ToStringSerializer.instance);

        return module;
    }
}