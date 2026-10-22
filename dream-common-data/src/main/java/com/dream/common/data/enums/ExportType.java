package com.dream.common.data.enums;

/**
 * 导出类型枚举，定义数据导出的范围策略。
 * <p>
 * 配合通用导出功能使用，通过 code 标识导出范围，
 * 可通过 {@link #fromCode(String)} 根据 code 获取枚举实例。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * ExportType type = ExportType.fromCode("all");
 * // type -> ALL（导出全部）
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
public enum ExportType {

    /**
     * 导出当前页
     */
    CURRENT_PAGE("current", "导出当前页"),

    /**
     * 导出全部
     */
    ALL("all", "导出全部"),

    /**
     * 导出选中
     */
    SELECTED("selected", "导出选中"),

    /**
     * 导出模板
     */
    TEMPLATE("template", "导出模板");

    /** 类型 code */
    private final String code;
    /** 类型描述 */
    private final String desc;

    /**
     * 构造方法。
     *
     * @param code 类型 code
     * @param desc 类型描述
     */
    ExportType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取类型 code。
     *
     * @return 类型 code
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取类型描述。
     *
     * @return 类型描述
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 根据 code 获取枚举实例。
     *
     * @param code 类型 code（如 current、all、selected、template），大小写不敏感
     * @return 匹配的枚举值；未匹配时默认返回 {@link #CURRENT_PAGE}
     */
    public static ExportType fromCode(String code) {
        for (ExportType type : ExportType.values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return CURRENT_PAGE;
    }
}
