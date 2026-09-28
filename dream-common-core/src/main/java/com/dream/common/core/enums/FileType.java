package com.dream.common.core.enums;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 文件类型枚举。
 *
 * <p>基于文件魔数（Magic Number）识别文件真实类型，防止文件类型伪造攻击。</p>
 *
 * <p>支持的文件类型包括：图片、文档、压缩包、音视频、可执行文件等常见格式。
 * 对于纯文本类文件（如 txt、json、css、js、svg）无固定魔数，仅通过扩展名识别；
 * {@link #XML} 与 {@link #HTML} 记录的是常见起始声明（{@code <?xml}、{@code <!DOCTYPE}），
 * 并非所有文件都必然包含。</p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 根据扩展名获取文件类型（不区分大小写）
 * FileType type = FileType.fromExtension("jpg");
 *
 * // 2. 根据魔数获取文件类型
 * FileType type2 = FileType.fromMagic("FFD8FF");
 *
 * // 3. 判断文件类型分类
 * boolean isImage = FileType.JPEG.isImage();
 *
 * // 4. 获取某个分类下的所有类型
 * List<FileType> images = FileType.getTypesByCategory(FileCat.IMAGE);
 *
 * // 5. 获取下拉选项
 * List<BaseEnum.Option<String>> options = BaseEnum.options(FileType.class);
 * }</pre>
 *
 * @author dream
 * @since 2026-09-11
 * @see BaseEnum
 * @see FileCat
 */
public enum FileType implements BaseEnum<String> {

    // ==================== 图片类型 ====================
    /** JPEG 图片 */
    JPEG("jpg", "FFD8FF", "JPEG图片", "image/jpeg", FileCat.IMAGE, "jpeg"),

    /** PNG 图片 */
    PNG("png", "89504E47", "PNG图片", "image/png", FileCat.IMAGE),

    /** GIF 图片 */
    GIF("gif", "47494638", "GIF图片", "image/gif", FileCat.IMAGE),

    /** BMP 位图 */
    BMP("bmp", "424D", "BMP位图", "image/bmp", FileCat.IMAGE),

    /** WEBP 图片 */
    WEBP("webp", "52494646", "WEBP图片", "image/webp", FileCat.IMAGE),

    /** SVG 矢量图（文本格式，无固定字节签名，仅按扩展名识别） */
    SVG("svg", null, "SVG矢量图", "image/svg+xml", FileCat.IMAGE),

    /** ICO 图标 */
    ICO("ico", "00000100", "ICO图标", "image/x-icon", FileCat.IMAGE),

    // ==================== 文档类型 ====================
    /** PDF 文档 */
    PDF("pdf", "25504446", "PDF文档", "application/pdf", FileCat.DOCUMENT),

    /** Word 文档（97-2003） */
    DOC("doc", "D0CF11E0", "Word文档(97-2003)", "application/msword", FileCat.DOCUMENT),

    /** Word 文档（OOXML） */
    DOCX("docx", "504B0304", "Word文档",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            FileCat.DOCUMENT),

    /** Excel 表格（97-2003） */
    XLS("xls", "D0CF11E0", "Excel表格(97-2003)", "application/vnd.ms-excel", FileCat.DOCUMENT),

    /** Excel 表格（OOXML） */
    XLSX("xlsx", "504B0304", "Excel表格",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            FileCat.DOCUMENT),

    /** PowerPoint 演示（97-2003） */
    PPT("ppt", "D0CF11E0", "PowerPoint(97-2003)", "application/vnd.ms-powerpoint", FileCat.DOCUMENT),

    /** PowerPoint 演示（OOXML） */
    PPTX("pptx", "504B0304", "PowerPoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            FileCat.DOCUMENT),

    /** 纯文本 */
    TXT("txt", null, "文本文件", "text/plain", FileCat.DOCUMENT),

    /** RTF 富文本 */
    RTF("rtf", "7B5C727466", "RTF富文本", "application/rtf", FileCat.DOCUMENT),

    // ==================== 压缩包类型 ====================
    /** ZIP 压缩包 */
    ZIP("zip", "504B0304", "ZIP压缩包", "application/zip", FileCat.ARCHIVE),

    /** RAR 压缩包 */
    RAR("rar", "52617221", "RAR压缩包", "application/x-rar-compressed", FileCat.ARCHIVE),

    /** 7z 压缩包 */
    SEVEN_Z("7z", "377ABCAF271C", "7z压缩包", "application/x-7z-compressed", FileCat.ARCHIVE),

    /** GZIP 压缩文件 */
    GZIP("gz", "1F8B08", "GZIP压缩文件", "application/gzip", FileCat.ARCHIVE),

    /** TAR 归档文件 */
    TAR("tar", "7573746172", "TAR归档文件", "application/x-tar", FileCat.ARCHIVE),

    // ==================== 音视频类型 ====================
    /** MP3 音频 */
    MP3("mp3", "494433", "MP3音频", "audio/mpeg", FileCat.MEDIA),

    /** MP4 视频 */
    MP4("mp4", null, "MP4视频", "video/mp4", FileCat.MEDIA),

    /** AVI 视频 */
    AVI("avi", "52494646", "AVI视频", "video/x-msvideo", FileCat.MEDIA),

    /** MOV 视频 */
    MOV("mov", "6D6F6F76", "MOV视频", "video/quicktime", FileCat.MEDIA),

    /** WAV 音频 */
    WAV("wav", "52494646", "WAV音频", "audio/wav", FileCat.MEDIA),

    /** FLV 视频 */
    FLV("flv", "464C56", "FLV视频", "video/x-flv", FileCat.MEDIA),

    /** MKV 视频 */
    MKV("mkv", "1A45DFA3", "MKV视频", "video/x-matroska", FileCat.MEDIA),

    /** RMVB 视频 */
    RMVB("rmvb", "2E524D46", "RMVB视频", "video/vnd.rn-realvideo", FileCat.MEDIA),

    // ==================== 可执行文件类型 ====================
    /** Windows 可执行文件 */
    EXE("exe", "4D5A", "可执行文件", "application/x-msdownload", FileCat.EXECUTABLE),

    /** Windows 动态链接库 */
    DLL("dll", "4D5A", "动态链接库", "application/x-msdownload", FileCat.EXECUTABLE),

    /** Linux 共享库 */
    SO("so", "7F454C46", "共享库", "application/x-sharedlib", FileCat.EXECUTABLE),

    // ==================== 代码/数据文件类型 ====================
    /** Java 类文件 */
    CLASS("class", "CAFEBABE", "Java类文件", "application/java-vm", FileCat.CODE),

    /** Java 归档包 */
    JAR("jar", "504B0304", "JAR包", "application/java-archive", FileCat.CODE),

    /** XML 文档 */
    XML("xml", "3C3F786D6C", "XML文档", "application/xml", FileCat.DATA),

    /** JSON 数据文件 */
    JSON("json", null, "JSON数据文件", "application/json", FileCat.DATA),

    /** HTML 网页 */
    HTML("html", "3C21444F", "HTML网页", "text/html", FileCat.DATA),

    /** CSS 样式 */
    CSS("css", null, "CSS样式", "text/css", FileCat.DATA),

    /** JavaScript 文件 */
    JS("js", null, "JavaScript文件", "application/javascript", FileCat.DATA),

    // ==================== 其他类型 ====================
    /** BT 种子文件 */
    TORRENT("torrent", "6431303A637265617465", "BT种子文件", "application/x-bittorrent", FileCat.OTHER);

    /** 主扩展名 */
    private final String extension;

    /** 魔数十六进制（可为 {@code null}，表示无固定魔数） */
    private final String magicHex;

    /** 中文描述 */
    private final String label;

    /** MIME 类型 */
    private final String mimeType;

    /** 文件分类 */
    private final FileCat category;

    /** 所有扩展名（含别名，不可变） */
    private final List<String> allExtensions;

    /**
     * 构造器（单扩展名）。
     *
     * @param extension 主扩展名
     * @param magicHex  魔数十六进制，可为 {@code null}
     * @param label     中文描述
     * @param mimeType  MIME 类型
     * @param category  文件分类
     */
    FileType(String extension, String magicHex, String label, String mimeType, FileCat category) {
        this(extension, magicHex, label, mimeType, category, new String[0]);
    }

    /**
     * 构造器（多扩展名）。
     *
     * @param extension       主扩展名
     * @param magicHex        魔数十六进制，可为 {@code null}
     * @param label           中文描述
     * @param mimeType        MIME 类型
     * @param category        文件分类
     * @param extraExtensions 扩展的别名（如 {@code jpeg} 是 {@code jpg} 的别名）
     */
    FileType(String extension, String magicHex, String label, String mimeType,
             FileCat category, String... extraExtensions) {
        this.extension = extension;
        this.magicHex = magicHex;
        this.label = label;
        this.mimeType = mimeType;
        this.category = category;

        List<String> list = new ArrayList<>();
        list.add(extension);
        if (extraExtensions != null) {
            for (String ext : extraExtensions) {
                if (ext != null && !ext.isEmpty() && !list.contains(ext)) {
                    list.add(ext);
                }
            }
        }
        this.allExtensions = Collections.unmodifiableList(list);
    }

    // ==================== Getter ====================

    /**
     * 获取主扩展名。
     *
     * @return 主扩展名（如 {@code jpg}）
     */
    public String getExtension() {
        return extension;
    }

    /**
     * 获取魔数十六进制字符串。
     *
     * @return 魔数；无固定魔数时返回 {@code null}
     */
    public String getMagicHex() {
        return magicHex;
    }

    /**
     * 获取枚举值（等价于 {@link #getExtension()}）。
     *
     * @return 主扩展名
     */
    @Override
    public String getValue() {
        return extension;
    }

    /**
     * 获取中文描述。
     *
     * @return 中文描述
     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     * 获取 MIME 类型。
     *
     * @return MIME 类型
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * 获取文件分类。
     *
     * @return 文件分类
     */
    public FileCat getCategory() {
        return category;
    }

    /**
     * 获取所有扩展名（含别名）。
     *
     * @return 扩展名列表（不可变）
     */
    public List<String> getAllExtensions() {
        return allExtensions;
    }

    /**
     * 获取 UI 颜色（沿用分类的颜色）。
     *
     * @return 十六进制颜色值
     */
    @Override
    public String getColor() {
        return category != null ? category.getColor() : "#D9D9D9";
    }

    /**
     * 获取详细描述（分类 + 名称）。
     *
     * @return 详细描述
     */
    @Override
    public String getDescription() {
        return category != null ? category.getLabel() + "：" + label : label;
    }

    // ==================== 分类判断 ====================

    /**
     * 是否为图片类型。
     *
     * @return 是返回 {@code true}
     */
    public boolean isImage() {
        return category == FileCat.IMAGE;
    }

    /**
     * 是否为文档类型。
     *
     * @return 是返回 {@code true}
     */
    public boolean isDocument() {
        return category == FileCat.DOCUMENT;
    }

    /**
     * 是否为压缩包类型。
     *
     * @return 是返回 {@code true}
     */
    public boolean isArchive() {
        return category == FileCat.ARCHIVE;
    }

    /**
     * 是否为音视频类型。
     *
     * @return 是返回 {@code true}
     */
    public boolean isMedia() {
        return category == FileCat.MEDIA;
    }

    /**
     * 是否为可执行文件类型。
     *
     * @return 是返回 {@code true}
     */
    public boolean isExecutable() {
        return category == FileCat.EXECUTABLE;
    }

    /**
     * 是否为代码文件类型。
     *
     * @return 是返回 {@code true}
     */
    public boolean isCode() {
        return category == FileCat.CODE;
    }

    /**
     * 是否为数据文件类型。
     *
     * @return 是返回 {@code true}
     */
    public boolean isData() {
        return category == FileCat.DATA;
    }

    /**
     * 是否为其他类型。
     *
     * @return 是返回 {@code true}
     */
    public boolean isOther() {
        return category == FileCat.OTHER;
    }

    /**
     * 是否有魔数（用于文件内容识别）。
     *
     * @return 有魔数返回 {@code true}
     */
    public boolean hasMagic() {
        return magicHex != null && !magicHex.isEmpty();
    }

    /**
     * 是否为纯文本类文件（无固定魔数，需按内容解析）。
     *
     * @return 是返回 {@code true}
     */
    public boolean isTextLike() {
        return this == TXT || this == JSON || this == XML || this == HTML || this == CSS || this == JS;
    }

    /**
     * 判断是否属于指定分类。
     *
     * @param fileCat 文件分类
     * @return 属于返回 {@code true}
     */
    public boolean belongsTo(FileCat fileCat) {
        return this.category == fileCat;
    }

    /**
     * 判断是否属于指定分类之一。
     *
     * @param fileCats 文件分类数组
     * @return 属于任一分类返回 {@code true}
     */
    public boolean belongsToAny(FileCat... fileCats) {
        if (fileCats == null || fileCats.length == 0) {
            return false;
        }
        for (FileCat cat : fileCats) {
            if (this.category == cat) {
                return true;
            }
        }
        return false;
    }

    // ==================== 静态查找（委托 BaseEnum） ====================

    /**
     * 根据扩展名查找文件类型（不区分大小写，支持别名）。
     *
     * <p>支持主扩展名和所有别名，例如 {@code jpg} 和 {@code jpeg} 都会返回
     * {@link #JPEG}。由于需要遍历 {@code allExtensions} 列表，本方法保留独立实现。</p>
     *
     * <pre>{@code
     * FileType type1 = FileType.fromExtension("jpg");   // JPEG
     * FileType type2 = FileType.fromExtension("JPEG");  // JPEG（不区分大小写）
     * FileType type3 = FileType.fromExtension(".jpg");  // 前导点号会被忽略
     * }</pre>
     *
     * @param extension 扩展名（可带或不带前导点号），可为 {@code null}
     * @return 匹配的文件类型；未找到或参数为 {@code null} 时返回 {@code null}
     */
    public static FileType fromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return null;
        }
        // 去除前导点号
        String normalized = extension.startsWith(".") ? extension.substring(1) : extension;
        if (normalized.isEmpty()) {
            return null;
        }
        for (FileType type : values()) {
            for (String ext : type.allExtensions) {
                if (ext.equalsIgnoreCase(normalized)) {
                    return type;
                }
            }
        }
        return null;
    }

    /**
     * 根据扩展名查找文件类型，未找到时返回默认值。
     *
     * @param extension    扩展名
     * @param defaultValue 默认值
     * @return 匹配的文件类型或默认值
     */
    public static FileType fromExtensionOrDefault(String extension, FileType defaultValue) {
        FileType type = fromExtension(extension);
        return type != null ? type : defaultValue;
    }

    /**
     * 根据魔数查找文件类型（不区分大小写）。
     *
     * <p>委托 {@link BaseEnum#fromFieldIgnoreCase(Class, java.util.function.Function, String)}。</p>
     *
     * <pre>{@code
     * FileType type1 = FileType.fromMagic("FFD8FF");   // JPEG
     * FileType type2 = FileType.fromMagic("ffd8ff");   // JPEG（不区分大小写）
     * }</pre>
     *
     * @param magicHex 魔数十六进制字符串，可为 {@code null}
     * @return 匹配的文件类型；未找到或参数为 {@code null} 时返回 {@code null}
     */
    public static FileType fromMagic(String magicHex) {
        return BaseEnum.fromFieldIgnoreCase(FileType.class, FileType::getMagicHex, magicHex);
    }

    /**
     * 根据 MIME 类型查找文件类型。
     *
     * <p>委托 {@link BaseEnum#fromField(Class, java.util.function.Function, Object)}。
     * 若多个枚举共享同一 MIME 类型（如 {@code DOC} 和 {@code XLS} 都是复合文档类型），
     * 返回<b>声明顺序最靠前</b>的一个。</p>
     *
     * @param mimeType MIME 类型，可为 {@code null}
     * @return 匹配的文件类型；未找到或参数为 {@code null} 时返回 {@code null}
     */
    public static FileType fromMimeType(String mimeType) {
        return BaseEnum.fromField(FileType.class, FileType::getMimeType, mimeType);
    }

    /**
     * 判断指定扩展名是否存在对应的文件类型。
     *
     * @param extension 扩展名
     * @return 存在返回 {@code true}
     */
    public static boolean existsExtension(String extension) {
        return fromExtension(extension) != null;
    }

    // ==================== 静态聚合（委托 BaseEnum） ====================

    /**
     * 获取指定分类下的所有文件类型。
     *
     * <p>委托 {@link BaseEnum#fromFieldList(Class, java.util.function.Function, Object)}，
     * 返回结果保持枚举声明顺序。</p>
     *
     * @param category 文件分类
     * @return 该分类下的所有文件类型；{@code category} 为 {@code null} 时返回空列表
     */
    public static List<FileType> getTypesByCategory(FileCat category) {
        return BaseEnum.fromFieldList(FileType.class, FileType::getCategory, category);
    }

    /**
     * 获取指定分类下的所有扩展名（含别名，去重）。
     *
     * @param category 文件分类
     * @return 扩展名列表；{@code category} 为 {@code null} 时返回空列表
     */
    public static List<String> getExtensionsByCategory(FileCat category) {
        if (category == null) {
            return Collections.emptyList();
        }
        Set<String> extSet = new LinkedHashSet<>();
        for (FileType type : values()) {
            if (type.category == category) {
                extSet.addAll(type.allExtensions);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(extSet));
    }

    /**
     * 获取所有扩展名（含别名，去重，保持声明顺序）。
     *
     * @return 所有扩展名列表
     */
    public static List<String> getAllExtensionsList() {
        Set<String> extSet = new LinkedHashSet<>();
        for (FileType type : values()) {
            extSet.addAll(type.allExtensions);
        }
        return Collections.unmodifiableList(new ArrayList<>(extSet));
    }

    /**
     * 获取所有扩展名数组。
     *
     * @return 扩展名数组
     */
    public static String[] getAllExtensionsArray() {
        List<String> list = getAllExtensionsList();
        return list.toArray(new String[0]);
    }

    /**
     * 获取 {@code 扩展名 → MIME 类型} 的映射。
     *
     * @return 扩展名 → MIME 类型
     */
    public static java.util.Map<String, String> getMimeTypeMap() {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        for (FileType type : values()) {
            for (String ext : type.allExtensions) {
                map.putIfAbsent(ext, type.mimeType);
            }
        }
        return Collections.unmodifiableMap(map);
    }
}