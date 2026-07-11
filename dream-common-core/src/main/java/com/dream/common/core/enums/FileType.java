package com.dream.common.core.enums;

/**
 * 文件类型枚举
 */
public enum FileType implements BaseEnum<String> {

    IMAGE("image", "图片", "jpg,jpeg,png,gif,bmp,svg,webp,ico"),
    DOCUMENT("document", "文档", "pdf,doc,docx,xls,xlsx,ppt,pptx,txt"),
    VIDEO("video", "视频", "mp4,avi,mov,wmv,flv,mkv"),
    AUDIO("audio", "音频", "mp3,wav,aac,flac,ogg,wma"),
    ARCHIVE("archive", "压缩包", "zip,rar,7z,tar,gz,bz2"),
    CODE("code", "代码", "java,class,xml,json,yml,properties,js,css,html"),
    OTHER("other", "其他", "");

    private final String value;

    private final String label;

    private final String extensions;

    FileType(String value, String label, String extensions) {
        this.value = value;
        this.label = label;
        this.extensions = extensions;
    }

    public String getExtensions() {
        return extensions;
    }

    /**
     * 根据文件后缀获取文件类型
     */
    public static FileType fromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return OTHER;
        }
        String ext = extension.toLowerCase();
        for (FileType type : values()) {
            if (type.extensions.contains(ext)) {
                return type;
            }
        }
        return OTHER;
    }

    /**
     * 判断是否为图片
     */
    public boolean isImage() {
        return this == IMAGE;
    }

    /**
     * 判断是否为文档
     */
    public boolean isDocument() {
        return this == DOCUMENT;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public static FileType fromValue(String value) {
        return BaseEnum.fromValue(FileType.class, value);
    }
}