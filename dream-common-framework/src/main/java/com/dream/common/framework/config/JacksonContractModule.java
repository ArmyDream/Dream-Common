package com.dream.common.framework.config;

import com.dream.common.core.enums.BaseEnum;
import com.dream.common.core.utils.R;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Objects;

/**
 * 统一响应契约 Jackson 模块。
 * <p>
 * 解决两类"注解声明与实际行为不符"的契约断链（挂载于 {@link JacksonObjectMapper}）：
 * </p>
 *
 * <h2>1. 响应体 R：以内部 Map 为唯一序列化事实源</h2>
 * <p>
 * 默认按 getter 序列化时，{@code R.put("extra", v)} 添加的自定义字段不会出现在 JSON 中。
 * 本模块将 {@link R}（及其子类）序列化为 {@link R#toMap()} 内容，
 * 与 {@code @JsonAnyGetter} 语义等价，保证链式扩展字段可上送前端。
 * </p>
 *
 * <h2>2. 枚举 BaseEnum ⇄ value</h2>
 * <p>
 * 实现 {@link BaseEnum} 的枚举默认会被 Jackson 按常量名（{@code name()}）序列化，
 * 与接口契约（value 为存储/传输值）不符。本模块统一改为：
 * </p>
 * <ul>
 *   <li>序列化：输出 {@link BaseEnum#getValue()}</li>
 *   <li>反序列化：按 value 匹配（{@code String.valueOf} 归一化，兼容 1 与 "1"）；
 *       未命中时回退按常量名匹配；两者都失败抛异常</li>
 * </ul>
 *
 * @author dream
 * @see JacksonObjectMapper
 */
public class JacksonContractModule extends Module {

    @Override
    public String getModuleName() {
        return "dream-contract-module";
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new ContractSerializers());
        context.addDeserializers(new ContractDeserializers());
    }

    // ==================== 序列化 ====================

    /**
     * 契约序列化器查找：R → 内部 Map；BaseEnum 实现枚举 → value。
     */
    static final class ContractSerializers extends Serializers.Base {

        @Override
        public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type,
                                               BeanDescription beanDesc) {
            Class<?> raw = type.getRawClass();
            if (R.class.isAssignableFrom(raw)) {
                return RSerializer.INSTANCE;
            }
            if (raw.isEnum() && BaseEnum.class.isAssignableFrom(raw)) {
                return BaseEnumSerializer.INSTANCE;
            }
            return null;
        }
    }

    /**
     * R 序列化器：直接写出 {@link R#toMap()}，put() 的扩展字段随 Map 一并输出。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static final class RSerializer extends StdSerializer<R> {

        static final RSerializer INSTANCE = new RSerializer();

        private static final long serialVersionUID = 1L;

        RSerializer() {
            super(R.class);
        }

        @Override
        public void serialize(R value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writePOJO(value.toMap());
        }
    }

    /**
     * BaseEnum 序列化器：输出 getValue()，由 provider 按值的实际类型选择序列化器。
     */
    @SuppressWarnings("unchecked")
    static final class BaseEnumSerializer extends StdSerializer {

        static final BaseEnumSerializer INSTANCE = new BaseEnumSerializer();

        private static final long serialVersionUID = 1L;

        BaseEnumSerializer() {
            super((Class) BaseEnum.class);
        }

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            Object v = ((BaseEnum<?>) value).getValue();
            if (v == null) {
                gen.writeNull();
            } else {
                provider.defaultSerializeValue(v, gen);
            }
        }
    }

    // ==================== 反序列化 ====================

    /**
     * 契约反序列化器查找：实现 BaseEnum 的枚举按 value 解析。
     */
    static final class ContractDeserializers extends Deserializers.Base {

        @Override
        public JsonDeserializer<?> findEnumDeserializer(Class<?> type, DeserializationConfig config,
                                                        BeanDescription beanDesc) {
            if (BaseEnum.class.isAssignableFrom(type)) {
                @SuppressWarnings("unchecked")
                Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) type;
                return new BaseEnumDeserializer(enumType);
            }
            return null;
        }
    }

    /**
     * BaseEnum 反序列化器：value 优先，常量名兜底。
     */
    static final class BaseEnumDeserializer extends StdDeserializer<Enum<?>> {

        private static final long serialVersionUID = 1L;

        private final Class<? extends Enum<?>> enumType;

        @SuppressWarnings("unchecked")
        BaseEnumDeserializer(Class<? extends Enum<?>> enumType) {
            super((Class) enumType);
            this.enumType = enumType;
        }

        @Override
        public Enum<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() == JsonToken.VALUE_NULL) {
                return null;
            }
            String raw = p.getValueAsString();
            if (raw == null) {
                return null;
            }
            Enum<?>[] constants = enumType.getEnumConstants();
            if (constants == null) {
                return null;
            }
            // 1. 按 value 匹配（与 BaseEnum.fromValue 的 String.valueOf 归一化语义一致）
            for (Enum<?> constant : constants) {
                if (constant instanceof BaseEnum) {
                    Object v = ((BaseEnum<?>) constant).getValue();
                    if (v != null && Objects.equals(String.valueOf(v), raw)) {
                        return constant;
                    }
                }
            }
            // 2. 兼容按常量名传输的历史报文
            for (Enum<?> constant : constants) {
                if (constant.name().equals(raw)) {
                    return constant;
                }
            }
            throw ctxt.weirdStringException(raw, enumType, "不是有效的枚举 value 或常量名");
        }
    }
}
