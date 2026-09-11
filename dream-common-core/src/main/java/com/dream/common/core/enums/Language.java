package com.dream.common.core.enums;

import java.util.Locale;

/**
 * 语言枚举
 * <p>
 * 用于国际化（i18n）场景，支持多语言切换
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 获取当前语言
 * LanguageEnum lang = LanguageEnum.fromCode("zh-CN");
 *
 * // 获取 Locale 对象（用于 Spring 国际化）
 * Locale locale = LanguageEnum.ZH_CN.getLocale();
 *
 * // 前端下拉框
 * List<BaseEnum.EnumOption<String>> options = BaseEnum.options(LanguageEnum.class);
 * }</pre>
 *
 * @author dream
 */
public enum Language implements BaseEnum<String> {

    // ==================== 中文 ====================
    ZH_CN("zh-CN", "简体中文", "简体中文", "zh", "CN"),
    ZH_TW("zh-TW", "繁体中文", "繁體中文", "zh", "TW"),
    ZH_HK("zh-HK", "繁体中文（香港）", "繁體中文（香港）", "zh", "HK"),

    // ==================== 英文 ====================
    EN_US("en-US", "English (US)", "英语（美国）", "en", "US"),
    EN_GB("en-GB", "English (UK)", "英语（英国）", "en", "GB"),

    // ==================== 日韩 ====================
    JA_JP("ja-JP", "日本語", "日语", "ja", "JP"),
    KO_KR("ko-KR", "한국어", "韩语", "ko", "KR"),

    // ==================== 欧洲语言 ====================
    DE_DE("de-DE", "Deutsch", "德语", "de", "DE"),
    FR_FR("fr-FR", "Français", "法语", "fr", "FR"),
    ES_ES("es-ES", "Español", "西班牙语", "es", "ES"),
    PT_BR("pt-BR", "Português (BR)", "葡萄牙语（巴西）", "pt", "BR"),
    IT_IT("it-IT", "Italiano", "意大利语", "it", "IT"),
    RU_RU("ru-RU", "Русский", "俄语", "ru", "RU"),

    // ==================== 东南亚语言 ====================
    TH_TH("th-TH", "ภาษาไทย", "泰语", "th", "TH"),
    VI_VN("vi-VN", "Tiếng Việt", "越南语", "vi", "VN"),
    MS_MY("ms-MY", "Bahasa Melayu", "马来语", "ms", "MY"),
    ID_ID("id-ID", "Bahasa Indonesia", "印尼语", "id", "ID"),

    // ==================== 中东语言 ====================
    AR_SA("ar-SA", "العربية", "阿拉伯语", "ar", "SA"),

    // ==================== 自动检测 ====================
    AUTO("auto", "Auto Detect", "自动检测", null, null);

    private final String value;
    private final String labelEn;
    private final String labelZh;
    private final String language;
    private final String country;

    Language(String value, String labelEn, String labelZh, String language, String country) {
        this.value = value;
        this.labelEn = labelEn;
        this.labelZh = labelZh;
        this.language = language;
        this.country = country;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return labelZh;
    }

    public String getLabelEn() {
        return labelEn;
    }

    public String getLabelZh() {
        return labelZh;
    }

    /**
     * 根据语言获取标签
     *
     * @param lang 语言：zh / en
     * @return 对应语言的标签
     */
    public String getLabel(String lang) {
        if ("en".equalsIgnoreCase(lang)) {
            return labelEn;
        }
        return labelZh;
    }

    public String getLanguage() {
        return language;
    }

    public String getCountry() {
        return country;
    }

    /**
     * 获取 Java Locale 对象
     *
     * @return Locale 对象
     */
    public Locale getLocale() {
        if (language == null || country == null) {
            return Locale.getDefault();
        }
        return new Locale(language, country);
    }

    /**
     * 获取 BCP 47 语言标签（如 "zh-CN"）
     */
    public String getBcp47Tag() {
        return value;
    }

    @Override
    public String getDescription() {
        return labelEn + " (" + value + ")";
    }

    @Override
    public String getColor() {
        switch (this) {
            case ZH_CN:
            case ZH_TW:
            case ZH_HK:
                return "#FF4D4F"; // 红色
            case EN_US:
            case EN_GB:
                return "#1890FF"; // 蓝色
            case JA_JP:
                return "#FAAD14"; // 橙色
            case KO_KR:
                return "#52C41A"; // 绿色
            case AR_SA:
                return "#722ED1"; // 紫色
            default:
                return "#8C8C8C";
        }
    }

    @Override
    public String getIcon() {
        switch (this) {
            case ZH_CN:
            case ZH_TW:
            case ZH_HK:
                return "🇨🇳";
            case EN_US:
                return "🇺🇸";
            case EN_GB:
                return "🇬🇧";
            case JA_JP:
                return "🇯🇵";
            case KO_KR:
                return "🇰🇷";
            case DE_DE:
                return "🇩🇪";
            case FR_FR:
                return "🇫🇷";
            case ES_ES:
                return "🇪🇸";
            case IT_IT:
                return "🇮🇹";
            case RU_RU:
                return "🇷🇺";
            case PT_BR:
                return "🇧🇷";
            case TH_TH:
                return "🇹🇭";
            case VI_VN:
                return "🇻🇳";
            case AR_SA:
                return "🇸🇦";
            case ID_ID:
                return "🇮🇩";
            case MS_MY:
                return "🇲🇾";
            default:
                return "🌐";
        }
    }

    // ==================== 业务方法 ====================

    /**
     * 判断是否为中文（简体或繁体）
     */
    public boolean isChinese() {
        return this == ZH_CN || this == ZH_TW || this == ZH_HK;
    }

    /**
     * 判断是否为英文
     */
    public boolean isEnglish() {
        return this == EN_US || this == EN_GB;
    }

    /**
     * 获取语言族（zh / en / ja / ko 等）
     */
    public String getLanguageFamily() {
        return language;
    }

    /**
     * 根据 Accept-Language 头解析语言
     * <p>
     * 支持格式：zh-CN, zh, en-US, en, *
     *
     * @param acceptLanguage Accept-Language 头
     * @return 匹配的语言枚举，未匹配返回 ZH_CN
     */
    public static Language fromAcceptLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isEmpty()) {
            return ZH_CN;
        }

        // 按优先级排序，取第一个
        String[] languages = acceptLanguage.split(",");
        for (String lang : languages) {
            String trimmed = lang.trim().split(";")[0];
            for (Language languageEnum : values()) {
                if (languageEnum.value.equals(trimmed)) {
                    return languageEnum;
                }
                // 也尝试匹配语言族（如 zh 匹配 zh-CN）
                if (trimmed.length() == 2 && languageEnum.language != null
                        && languageEnum.language.equals(trimmed)) {
                    // 返回第一个匹配的语言族（优先返回对应的具体语言）
                    for (Language le : values()) {
                        if (le.language != null && le.language.equals(trimmed) && le.country != null) {
                            return le;
                        }
                    }
                }
            }
        }
        return ZH_CN;
    }

}