package com.dream.common.data.enums;

/**
 * 导入策略枚举，定义数据导入时对已存在数据的处理策略。
 * <p>
 * 配合通用导入功能使用，通过 code 标识处理策略，
 * 可通过 {@link #fromCode(String)} 根据 code 获取枚举实例。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * ImportStrategy strategy = ImportStrategy.fromCode("upsert");
 * // strategy -> UPSERT（存在修改不存在新增）
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
public enum ImportStrategy {

    /**
     * 只新增，已存在则忽略
     */
    INSERT_ONLY("insert_only", "只新增"),

    /**
     * 只修改，不存在则忽略
     */
    UPDATE_ONLY("update_only", "只修改"),

    /**
     * 存在则修改，不存在则新增
     */
    UPSERT("upsert", "存在修改不存在新增"),

    /**
     * 覆盖（先删除后新增）
     */
    OVERRIDE("override", "覆盖");

    /** 策略 code */
    private final String code;
    /** 策略描述 */
    private final String desc;

    /**
     * 构造方法。
     *
     * @param code 策略 code
     * @param desc 策略描述
     */
    ImportStrategy(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取策略 code。
     *
     * @return 策略 code
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取策略描述。
     *
     * @return 策略描述
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 根据 code 获取枚举实例。
     *
     * @param code 策略 code（如 insert_only、update_only、upsert、override），大小写不敏感
     * @return 匹配的枚举值；未匹配时默认返回 {@link #UPSERT}
     */
    public static ImportStrategy fromCode(String code) {
        for (ImportStrategy type : ImportStrategy.values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return UPSERT;
    }
}
