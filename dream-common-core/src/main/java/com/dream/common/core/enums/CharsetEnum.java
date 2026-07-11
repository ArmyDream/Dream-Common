package com.dream.common.core.enums;


import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 字符编码枚举
 * <p>
 * 统一管理项目中使用的字符编码
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 获取编码名称
 * String charset = CharsetEnum.UTF_8.getValue();
 *
 * // 获取 Java 标准 Charset 对象
 * Charset cs = CharsetEnum.UTF_8.getCharset();
 *
 * // 使用编码
 * byte[] bytes = "Hello".getBytes(CharsetEnum.UTF_8.getCharset());
 * }</pre>
 *
 */
public enum CharsetEnum implements BaseEnum<String> {

    /**
     * UTF-8（最常用，推荐）
     */
    UTF_8("UTF-8", "UTF-8", StandardCharsets.UTF_8),

    /**
     * GBK（中文Windows默认）
     */
    GBK("GBK", "GBK", Charset.forName("GBK")),

    /**
     * GB2312（中文简体）
     */
    GB2312("GB2312", "GB2312", Charset.forName("GB2312")),

    /**
     * ISO-8859-1（Latin-1，西欧语言）
     */
    ISO_8859_1("ISO-8859-1", "ISO-8859-1", StandardCharsets.ISO_8859_1),

    /**
     * US-ASCII（7位ASCII）
     */
    US_ASCII("US-ASCII", "US-ASCII", StandardCharsets.US_ASCII),

    /**
     * UTF-16（Unicode，每个字符16位）
     */
    UTF_16("UTF-16", "UTF-16", StandardCharsets.UTF_16),

    /**
     * UTF-16BE（大端序）
     */
    UTF_16BE("UTF-16BE", "UTF-16BE", StandardCharsets.UTF_16BE),

    /**
     * UTF-16LE（小端序）
     */
    UTF_16LE("UTF-16LE", "UTF-16LE", StandardCharsets.UTF_16LE),

    /**
     * GB18030（中文最新国家标准，兼容GBK）
     */
    GB18030("GB18030", "GB18030", Charset.forName("GB18030")),

    /**
     * Big5（繁体中文）
     */
    BIG5("Big5", "Big5", Charset.forName("Big5")),

    /**
     * EUC-KR（韩文）
     */
    EUC_KR("EUC-KR", "EUC-KR", Charset.forName("EUC-KR")),

    /**
     * Shift_JIS（日文）
     */
    SHIFT_JIS("Shift_JIS", "Shift_JIS", Charset.forName("Shift_JIS")),

    /**
     * Windows-1252（西欧）
     */
    WINDOWS_1252("Windows-1252", "Windows-1252", Charset.forName("Windows-1252"));

    private final String value;
    private final String label;
    private final Charset charset;

    CharsetEnum(String value, String label, Charset charset) {
        this.value = value;
        this.label = label;
        this.charset = charset;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    /**
     * 获取 Java 标准 Charset 对象
     *
     * @return Charset 对象
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * 获取编码名称（等价于 getValue()）
     */
    public String getCharsetName() {
        return value;
    }

    @Override
    public String getColor() {
        switch (this) {
            case UTF_8:
                return "#1890FF";
            case GBK:
            case GB2312:
            case GB18030:
                return "#52C41A";
            case ISO_8859_1:
            case US_ASCII:
                return "#FAAD14";
            case UTF_16:
            case UTF_16BE:
            case UTF_16LE:
                return "#722ED1";
            case BIG5:
                return "#FF7A45";
            case EUC_KR:
                return "#13C2C2";
            case SHIFT_JIS:
                return "#FF4D4F";
            default:
                return "#8C8C8C";
        }
    }

    /**
     * 根据编码名称获取枚举
     */
    public static CharsetEnum fromValue(String value) {
        return BaseEnum.fromValue(CharsetEnum.class, value);
    }

    /**
     * 根据 Java Charset 对象获取枚举
     */
    public static CharsetEnum fromCharset(Charset charset) {
        if (charset == null) {
            return null;
        }
        String name = charset.name();
        for (CharsetEnum charsetEnum : values()) {
            if (charsetEnum.charset.name().equals(name)) {
                return charsetEnum;
            }
        }
        return null;
    }

    /**
     * 获取所有支持的编码列表
     */
    public static String[] getNames() {
        CharsetEnum[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].value;
        }
        return names;
    }
}