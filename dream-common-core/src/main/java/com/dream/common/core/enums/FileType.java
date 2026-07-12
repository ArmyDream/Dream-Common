package com.dream.common.core.enums;

import java.util.*;

/**
 * 文件类型枚举
 * <p>
 * 基于文件魔数（Magic Number）识别文件真实类型，防止文件类型伪造攻击。
 * </p>
 * <p>
 * 支持的文件类型包括：图片、文档、压缩包、音视频、可执行文件等常见格式。
 * 对于纯文本类文件（如 txt、json、xml、html）无固定魔数，仅通过扩展名识别。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 根据扩展名获取文件类型
 * FileType type = FileType.fromExtension("jpg");
 *
 * // 2. 获取文件类型的魔数
 * String magic = FileType.JPEG.getMagicHex();
 *
 * // 3. 判断文件类型分类
 * boolean isImage = FileType.JPEG.isImage();
 * boolean isDocument = FileType.PDF.isDocument();
 *
 * // 4. 获取下拉选项
 * List<BaseEnum.EnumOption<String>> options = BaseEnum.options(FileType.class);
 * }</pre>
 *
 * @author dream
 * @see BaseEnum
 * @see FileCat
 */
public enum FileType implements BaseEnum<String> {

    // ==================== 图片类型 ====================
    JPEG("jpg", "FFD8FF", "JPEG图片", "image/jpeg", FileCat.IMAGE),
    PNG("png", "89504E47", "PNG图片", "image/png", FileCat.IMAGE),
    GIF("gif", "47494638", "GIF图片", "image/gif", FileCat.IMAGE),
    BMP("bmp", "424D", "BMP位图", "image/bmp", FileCat.IMAGE),
    WEBP("webp", "52494646", "WEBP图片", "image/webp", FileCat.IMAGE),
    SVG("svg", "3C3F786D6C", "SVG矢量图", "image/svg+xml", FileCat.IMAGE),
    ICO("ico", "00000100", "ICO图标", "image/x-icon", FileCat.IMAGE),

    // ==================== 文档类型 ====================
    PDF("pdf", "25504446", "PDF文档", "application/pdf", FileCat.DOCUMENT),
    DOC("doc", "D0CF11E0", "Word文档(97-2003)", "application/msword", FileCat.DOCUMENT),
    DOCX("docx", "504B0304", "Word文档", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", FileCat.DOCUMENT),
    XLS("xls", "D0CF11E0", "Excel表格(97-2003)", "application/vnd.ms-excel", FileCat.DOCUMENT),
    XLSX("xlsx", "504B0304", "Excel表格", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", FileCat.DOCUMENT),
    PPT("ppt", "D0CF11E0", "PowerPoint(97-2003)", "application/vnd.ms-powerpoint", FileCat.DOCUMENT),
    PPTX("pptx", "504B0304", "PowerPoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation", FileCat.DOCUMENT),
    TXT("txt", null, "文本文件", "text/plain", FileCat.DOCUMENT),
    RTF("rtf", "7B5C727466", "RTF富文本", "application/rtf", FileCat.DOCUMENT),

    // ==================== 压缩包类型 ====================
    ZIP("zip", "504B0304", "ZIP压缩包", "application/zip", FileCat.ARCHIVE),
    RAR("rar", "52617221", "RAR压缩包", "application/x-rar-compressed", FileCat.ARCHIVE),
    SEVEN_Z("7z", "377ABCAF271C", "7z压缩包", "application/x-7z-compressed", FileCat.ARCHIVE),
    GZIP("gz", "1F8B08", "GZIP压缩文件", "application/gzip", FileCat.ARCHIVE),
    TAR("tar", "7573746172", "TAR归档文件", "application/x-tar", FileCat.ARCHIVE),

    // ==================== 音视频类型 ====================
    MP3("mp3", "494433", "MP3音频", "audio/mpeg", FileCat.MEDIA),
    MP4("mp4", null, "MP4视频", "video/mp4", FileCat.MEDIA),
    AVI("avi", "52494646", "AVI视频", "video/x-msvideo", FileCat.MEDIA),
    MOV("mov", "6D6F6F76", "MOV视频", "video/quicktime", FileCat.MEDIA),
    WAV("wav", "52494646", "WAV音频", "audio/wav", FileCat.MEDIA),
    FLV("flv", "464C56", "FLV视频", "video/x-flv", FileCat.MEDIA),
    MKV("mkv", "1A45DFA3", "MKV视频", "video/x-matroska", FileCat.MEDIA),
    RMVB("rmvb", "2E524D46", "RMVB视频", "video/vnd.rn-realvideo", FileCat.MEDIA),

    // ==================== 可执行文件类型 ====================
    EXE("exe", "4D5A", "可执行文件", "application/x-msdownload", FileCat.EXECUTABLE),
    DLL("dll", "4D5A", "动态链接库", "application/x-msdownload", FileCat.EXECUTABLE),
    SO("so", "7F454C46", "共享库", "application/x-sharedlib", FileCat.EXECUTABLE),

    // ==================== 代码/数据文件类型 ====================
    CLASS("class", "CAFEBABE", "Java类文件", "application/java-vm", FileCat.CODE),
    JAR("jar", "504B0304", "JAR包", "application/java-archive", FileCat.CODE),
    XML("xml", "3C3F786D6C", "XML文档", "application/xml", FileCat.DATA),
    JSON("json", null, "JSON数据文件", "application/json", FileCat.DATA),
    HTML("html", "3C21444F", "HTML网页", "text/html", FileCat.DATA),
    CSS("css", null, "CSS样式", "text/css", FileCat.DATA),
    JS("js", null, "JavaScript文件", "application/javascript", FileCat.DATA),

    // ==================== 其他类型 ====================
    TORRENT("torrent", "6431303A637265617465", "BT种子文件", "application/x-bittorrent", FileCat.OTHER),
    ;

    /** 文件扩展名（主要） */
    private final String extension;

    /** 魔数十六进制字符串（可为 null，表示无固定魔数） */
    private final String magicHex;

    /** 中文描述 */
    private final String label;

    /** MIME 类型 */
    private final String mimeType;

    /** 文件分类 */
    private final FileCat category;

    /** 所有扩展名（包括别名），使用 List 便于扩展 */
    private final List<String> allExtensions;

    /**
     * 构造方法（单个扩展名）
     */
    FileType(String extension, String magicHex, String label, String mimeType, FileCat category) {
        this.extension = extension;
        this.magicHex = magicHex;
        this.label = label;
        this.mimeType = mimeType;
        this.category = category;
        this.allExtensions = Collections.singletonList(extension);
    }

    /**
     * 构造方法（多个扩展名）
     */
    FileType(String extension, String magicHex, String label, String mimeType, FileCat category, String... extraExtensions) {
        this.extension = extension;
        this.magicHex = magicHex;
        this.label = label;
        this.mimeType = mimeType;
        this.category = category;
        List<String> list = new ArrayList<>();
        list.add(extension);
        if (extraExtensions != null) {
            for (String ext : extraExtensions) {
                if (!list.contains(ext)) {
                    list.add(ext);
                }
            }
        }
        this.allExtensions = Collections.unmodifiableList(list);
    }

    // ==================== Getter 方法 ====================

    public String getExtension() {
        return extension;
    }

    public String getMagicHex() {
        return magicHex;
    }

    @Override
    public String getValue() {
        return extension;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public String getMimeType() {
        return mimeType;
    }

    public FileCat getCategory() {
        return category;
    }

    /**
     * 获取所有扩展名列表
     *
     * @return 扩展名列表（不可变）
     */
    public List<String> getAllExtensions() {
        return allExtensions;
    }

    @Override
    public String getColor() {
        return category != null ? category.getColor() : "#D9D9D9";
    }

    @Override
    public String getDescription() {
        if (category != null) {
            return category.getLabel() + "：" + label;
        }
        return label;
    }

    // ==================== 分类判断方法 ====================

    public boolean isImage() {
        return category == FileCat.IMAGE;
    }

    public boolean isDocument() {
        return category == FileCat.DOCUMENT;
    }

    public boolean isArchive() {
        return category == FileCat.ARCHIVE;
    }

    public boolean isMedia() {
        return category == FileCat.MEDIA;
    }

    public boolean isExecutable() {
        return category == FileCat.EXECUTABLE;
    }

    public boolean isCode() {
        return category == FileCat.CODE;
    }

    public boolean isData() {
        return category == FileCat.DATA;
    }

    public boolean isOther() {
        return category == FileCat.OTHER;
    }

    public boolean hasMagic() {
        return magicHex != null && !magicHex.isEmpty();
    }

    public boolean isTextLike() {
        return this == TXT || this == JSON || this == XML || this == HTML || this == CSS || this == JS;
    }

    /**
     * 判断是否属于指定分类
     *
     * @param fileCat 文件分类
     * @return 属于返回 true
     */
    public boolean belongsTo(FileCat fileCat) {
        return this.category == fileCat;
    }

    /**
     * 判断是否属于指定分类之一
     *
     * @param fileCats 文件分类数组
     * @return 属于返回 true
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

    // ==================== 静态工具方法 ====================

    /**
     * 根据扩展名查找文件类型
     *
     * @param extension 文件扩展名（小写，不含点号）
     * @return 匹配的文件类型，未找到返回 null
     */
    public static FileType fromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return null;
        }
        String lowerExt = extension.toLowerCase();
        return EXTENSION_MAP.get(lowerExt);
    }

    /**
     * 根据扩展名查找文件类型，找不到返回默认值
     *
     * @param extension    文件扩展名
     * @param defaultValue 默认值
     * @return 匹配的文件类型或默认值
     */
    public static FileType fromExtensionOrDefault(String extension, FileType defaultValue) {
        FileType type = fromExtension(extension);
        return type != null ? type : defaultValue;
    }

    /**
     * 根据魔数查找文件类型
     *
     * @param magicHex 魔数十六进制字符串
     * @return 匹配的文件类型，未找到返回 null
     */
    public static FileType fromMagic(String magicHex) {
        if (magicHex == null || magicHex.isEmpty()) {
            return null;
        }
        String upperMagic = magicHex.toUpperCase();
        return MAGIC_MAP.get(upperMagic);
    }

    /**
     * 判断指定扩展名是否存在
     *
     * @param extension 文件扩展名
     * @return 存在返回 true
     */
    public static boolean existsExtension(String extension) {
        return fromExtension(extension) != null;
    }

    /**
     * 获取所有扩展名列表（去重）
     *
     * @return 所有扩展名列表
     */
    public static List<String> getAllExtensionsList() {
        return ALL_EXTENSIONS;
    }

    /**
     * 获取所有扩展名数组（去重）
     *
     * @return 所有扩展名数组
     */
    public static String[] getAllExtensionsArray() {
        return ALL_EXTENSIONS.toArray(new String[0]);
    }

    /**
     * 获取所有 MIME 类型映射
     *
     * @return 扩展名 → MIME 类型的映射
     */
    public static Map<String, String> getMimeTypeMap() {
        return MIME_TYPE_MAP;
    }

    /**
     * 根据 MIME 类型查找文件类型
     *
     * @param mimeType MIME 类型
     * @return 匹配的文件类型，未找到返回 null
     */
    public static FileType fromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return null;
        }
        return MIME_TYPE_TO_TYPE.get(mimeType);
    }

    /**
     * 根据分类获取所有文件类型
     *
     * @param category 文件分类
     * @return 该分类下的所有文件类型列表
     */
    public static List<FileType> getTypesByCategory(FileCat category) {
        if (category == null) {
            return Collections.emptyList();
        }
        List<FileType> result = new ArrayList<>();
        for (FileType type : values()) {
            if (type.getCategory() == category) {
                result.add(type);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 获取指定分类的扩展名列表（去重）
     *
     * @param category 文件分类
     * @return 扩展名列表
     */
    public static List<String> getExtensionsByCategory(FileCat category) {
        if (category == null) {
            return Collections.emptyList();
        }
        Set<String> extSet = new LinkedHashSet<>();
        for (FileType type : values()) {
            if (type.getCategory() == category) {
                extSet.addAll(type.getAllExtensions());
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(extSet));
    }

    // ==================== 静态映射 ====================

    /** 扩展名 → 文件类型映射（缓存，提升性能） */
    private static final Map<String, FileType> EXTENSION_MAP;

    /** 魔数 → 文件类型映射（缓存，提升性能） */
    private static final Map<String, FileType> MAGIC_MAP;

    /** 扩展名 → MIME 类型映射 */
    private static final Map<String, String> MIME_TYPE_MAP;

    /** MIME 类型 → 文件类型映射 */
    private static final Map<String, FileType> MIME_TYPE_TO_TYPE;

    /** 所有扩展名列表（去重） */
    private static final List<String> ALL_EXTENSIONS;

    static {
        // 构建扩展名映射
        Map<String, FileType> extMap = new LinkedHashMap<>();
        Map<String, String> mimeMap = new LinkedHashMap<>();
        Set<String> allExtSet = new LinkedHashSet<>();

        for (FileType type : values()) {
            for (String ext : type.getAllExtensions()) {
                // 只保留第一个匹配的类型
                if (!extMap.containsKey(ext)) {
                    extMap.put(ext, type);
                }
                // MIME 映射
                if (!mimeMap.containsKey(ext)) {
                    mimeMap.put(ext, type.getMimeType());
                }
                // 收集所有扩展名
                allExtSet.add(ext);
            }
        }

        EXTENSION_MAP = Collections.unmodifiableMap(extMap);
        MIME_TYPE_MAP = Collections.unmodifiableMap(mimeMap);
        ALL_EXTENSIONS = Collections.unmodifiableList(new ArrayList<>(allExtSet));

        // 构建魔数映射
        Map<String, FileType> magicMap = new LinkedHashMap<>();
        for (FileType type : values()) {
            if (type.hasMagic()) {
                magicMap.put(type.getMagicHex(), type);
            }
        }
        MAGIC_MAP = Collections.unmodifiableMap(magicMap);

        // 构建 MIME 类型映射
        Map<String, FileType> mimeToTypeMap = new LinkedHashMap<>();
        for (FileType type : values()) {
            if (!mimeToTypeMap.containsKey(type.getMimeType())) {
                mimeToTypeMap.put(type.getMimeType(), type);
            }
        }
        MIME_TYPE_TO_TYPE = Collections.unmodifiableMap(mimeToTypeMap);
    }
}