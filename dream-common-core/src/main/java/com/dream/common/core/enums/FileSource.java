package com.dream.common.core.enums;

/**
 * 文件来源枚举
 *
 */
public enum FileSource implements BaseEnum<String> {

    /**
     * 用户上传
     */
    UPLOAD("upload", "用户上传"),

    /**
     * 系统生成
     */
    GENERATE("generate", "系统生成"),

    /**
     * 数据导入
     */
    IMPORT("import", "数据导入"),

    /**
     * 数据导出
     */
    EXPORT("export", "数据导出"),

    /**
     * 程序下载（从远程下载）
     */
    DOWNLOAD("download", "程序下载"),

    /**
     * 复制/克隆
     */
    COPY("copy", "复制克隆"),

    /**
     * 临时文件
     */
    TEMP("temp", "临时文件"),

    /**
     * 模板文件
     */
    TEMPLATE("template", "模板文件"),

    /**
     * 附件（邮件、消息等）
     */
    ATTACHMENT("attachment", "附件"),

    /**
     * 截图/快照
     */
    SCREENSHOT("screenshot", "截图快照");

    private final String value;
    private final String label;

    FileSource(String value, String label) {
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

    @Override
    public String getColor() {
        switch (this) {
            case UPLOAD:
                return "#1890FF";
            case GENERATE:
                return "#52C41A";
            case IMPORT:
            case EXPORT:
                return "#722ED1";
            case DOWNLOAD:
                return "#FAAD14";
            case COPY:
                return "#13C2C2";
            case TEMP:
                return "#8C8C8C";
            case TEMPLATE:
                return "#FF7A45";
            case ATTACHMENT:
                return "#2F54EB";
            case SCREENSHOT:
                return "#FF4D4F";
            default:
                return "#8C8C8C";
        }
    }

    public static FileSource fromValue(String value) {
        return BaseEnum.fromValue(FileSource.class, value);
    }
}