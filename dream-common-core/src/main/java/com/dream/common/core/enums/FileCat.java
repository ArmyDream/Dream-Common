package com.dream.common.core.enums;

import java.util.Arrays;

/**
 * 文件分类枚举
 * <p>
 * 对文件类型进行归类，便于前端展示和权限控制。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 获取分类信息
 * FileCat cat = FileCat.IMAGE;
 * String label = cat.getLabel();  // "图片文件"
 * String color = cat.getColor();  // "#52C41A"
 *
 * // 2. 获取下拉选项
 * List<BaseEnum.EnumOption<String>> options = BaseEnum.options(FileCat.class);
 *
 * // 3. 根据值查找
 * FileCat cat = FileCat.fromValue("image");
 * }</pre>
 *
 * @author dream
 */
public enum FileCat implements BaseEnum<String> {

    /** 图片文件 */
    IMAGE("image", "图片文件", "#52C41A", "JPEG、PNG、GIF、BMP、WEBP、SVG 等图片格式"),

    /** 文档文件 */
    DOCUMENT("document", "文档文件", "#1890FF", "PDF、Word、Excel、PowerPoint、TXT 等文档格式"),

    /** 压缩文件 */
    ARCHIVE("archive", "压缩文件", "#FAAD14", "ZIP、RAR、7z、GZIP、TAR 等压缩格式"),

    /** 音视频文件 */
    MEDIA("media", "音视频文件", "#722ED1", "MP3、MP4、AVI、MOV、WAV、FLV、MKV 等音视频格式"),

    /** 可执行文件 */
    EXECUTABLE("executable", "可执行文件", "#FF4D4F", "EXE、DLL、SO 等可执行文件"),

    /** 代码文件 */
    CODE("code", "代码文件", "#13C2C2", "Java、JAR、CLASS 等代码文件"),

    /** 数据文件 */
    DATA("data", "数据文件", "#8C8C8C", "XML、JSON、HTML、CSS、JS 等数据文件"),

    /** 其他文件 */
    OTHER("other", "其他文件", "#D9D9D9", "其他未分类的文件类型"),
    ;

    /** 分类标识（唯一值） */
    private final String value;

    /** 分类名称（中文描述） */
    private final String label;

    /** 前端展示颜色 */
    private final String color;

    /** 分类详细描述 */
    private final String description;

    FileCat(String value, String label, String color, String description) {
        this.value = value;
        this.label = label;
        this.color = color;
        this.description = description;
    }

    // ==================== BaseEnum 接口实现 ====================

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getColor() {
        return color;
    }

    @Override
    public String getDescription() {
        return description;
    }

}