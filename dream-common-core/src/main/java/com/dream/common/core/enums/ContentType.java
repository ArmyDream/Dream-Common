package com.dream.common.core.enums;

/**
 * HTTP 内容类型枚举
 *
 * @author dream
 */
public enum ContentType implements BaseEnum<String> {

    /**
     * JSON
     */
    JSON("application/json", "JSON"),

    /**
     * JSON（UTF-8）
     */
    JSON_UTF8("application/json;charset=UTF-8", "JSON(UTF-8)"),

    /**
     * XML
     */
    XML("application/xml", "XML"),

    /**
     * XML（UTF-8）
     */
    XML_UTF8("application/xml;charset=UTF-8", "XML(UTF-8)"),

    /**
     * 表单（URL编码）
     */
    FORM("application/x-www-form-urlencoded", "表单"),

    /**
     * 表单（URL编码，UTF-8）
     */
    FORM_UTF8("application/x-www-form-urlencoded;charset=UTF-8", "表单(UTF-8)"),

    /**
     * 文件上传（Multipart）
     */
    MULTIPART("multipart/form-data", "文件上传"),

    /**
     * 纯文本
     */
    TEXT("text/plain", "纯文本"),

    /**
     * 纯文本（UTF-8）
     */
    TEXT_UTF8("text/plain;charset=UTF-8", "纯文本(UTF-8)"),

    /**
     * HTML
     */
    HTML("text/html", "HTML"),

    /**
     * HTML（UTF-8）
     */
    HTML_UTF8("text/html;charset=UTF-8", "HTML(UTF-8)"),

    /**
     * 二进制流
     */
    OCTET_STREAM("application/octet-stream", "二进制流"),

    /**
     * PDF
     */
    PDF("application/pdf", "PDF"),

    /**
     * ZIP
     */
    ZIP("application/zip", "ZIP"),

    /**
     * Excel 2003
     */
    EXCEL_XLS("application/vnd.ms-excel", "Excel 2003"),

    /**
     * Excel 2007+
     */
    EXCEL_XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel 2007+"),

    /**
     * Word 2003
     */
    WORD_DOC("application/msword", "Word 2003"),

    /**
     * Word 2007+
     */
    WORD_DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Word 2007+"),

    /**
     * PPT 2003
     */
    PPT("application/vnd.ms-powerpoint", "PPT 2003"),

    /**
     * PPT 2007+
     */
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", "PPT 2007+"),

    /**
     * JPEG
     */
    JPEG("image/jpeg", "JPEG"),

    /**
     * PNG
     */
    PNG("image/png", "PNG"),

    /**
     * GIF
     */
    GIF("image/gif", "GIF"),

    /**
     * WEBP
     */
    WEBP("image/webp", "WEBP"),

    /**
     * SVG
     */
    SVG("image/svg+xml", "SVG"),

    /**
     * MP4 视频
     */
    MP4("video/mp4", "MP4"),

    /**
     * MP3 音频
     */
    MP3("audio/mpeg", "MP3"),

    /**
     * 事件流（SSE）
     */
    EVENT_STREAM("text/event-stream", "事件流");

    private final String value;
    private final String label;

    ContentType(String value, String label) {
        this.value = value;
        this.label = label;
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
     * 是否为文本类型
     */
    public boolean isText() {
        return this == TEXT || this == TEXT_UTF8 || this == HTML || this == HTML_UTF8;
    }

    /**
     * 是否为图片类型
     */
    public boolean isImage() {
        return this == JPEG || this == PNG || this == GIF || this == WEBP || this == SVG;
    }

    /**
     * 是否为 Office 文档类型
     */
    public boolean isOffice() {
        return this == EXCEL_XLS || this == EXCEL_XLSX
                || this == WORD_DOC || this == WORD_DOCX
                || this == PPT || this == PPTX || this == PDF;
    }

    @Override
    public String getColor() {
        switch (this) {
            case JSON:
            case JSON_UTF8:
                return "#1890FF";
            case XML:
            case XML_UTF8:
                return "#FAAD14";
            case FORM:
            case FORM_UTF8:
            case MULTIPART:
                return "#52C41A";
            case TEXT:
            case TEXT_UTF8:
            case HTML:
            case HTML_UTF8:
                return "#8C8C8C";
            case OCTET_STREAM:
                return "#8C8C8C";
            case PDF:
                return "#FF4D4F";
            case EXCEL_XLS:
            case EXCEL_XLSX:
                return "#217346";
            case WORD_DOC:
            case WORD_DOCX:
                return "#2B579A";
            case PPT:
            case PPTX:
                return "#D24726";
            case JPEG:
            case PNG:
            case GIF:
            case WEBP:
            case SVG:
                return "#722ED1";
            case MP4:
                return "#FF6B00";
            case MP3:
                return "#13C2C2";
            default:
                return "#8C8C8C";
        }
    }

    public static ContentType fromValue(String value) {
        return BaseEnum.fromValue(ContentType.class, value);
    }
}