package com.dream.common.data.enums;

/**
 * 导出文件格式枚举，定义数据导出所支持的文件格式。
 * <p>
 * 配合通用导出功能使用，通过 code（文件后缀）标识具体格式，
 * 可通过 {@link #fromCode(String)} 根据后缀获取枚举实例。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * ExportFormat format = ExportFormat.fromCode("xlsx");
 * // format -> EXCEL_2007
 * String code = format.getCode();   // xlsx
 * String desc = format.getDesc();   // Excel 2007
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
public enum ExportFormat {

    /** Excel 2003 格式（.xls） */
    EXCEL_2003("xls", "Excel 2003"),
    /** Excel 2007 格式（.xlsx） */
    EXCEL_2007("xlsx", "Excel 2007"),
    /** CSV 格式（.csv） */
    CSV("csv", "CSV"),
    /** PDF 格式（.pdf） */
    PDF("pdf", "PDF");

    /** 文件后缀 code */
    private final String code;
    /** 格式描述 */
    private final String desc;

    /**
     * 构造方法。
     *
     * @param code 文件后缀
     * @param desc 格式描述
     */
    ExportFormat(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取文件后缀 code。
     *
     * @return 文件后缀
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取格式描述。
     *
     * @return 格式描述
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 根据文件后缀 code 获取枚举实例。
     *
     * @param code 文件后缀（如 xls、xlsx、csv、pdf），大小写不敏感
     * @return 匹配的枚举值；未匹配时默认返回 {@link #EXCEL_2007}
     */
    public static ExportFormat fromCode(String code) {
        for (ExportFormat type : ExportFormat.values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return EXCEL_2007;
    }
}
