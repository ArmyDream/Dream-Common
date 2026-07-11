package com.dream.common.core.enums;

/**
 * 运行环境枚举
 */
public enum Env implements BaseEnum<String> {

    DEV("dev", "开发环境", true),
    TEST("test", "测试环境", true),
    SIT("sit", "集成测试环境", true),
    UAT("uat", "用户验收环境", true),
    PRE("pre", "预发布环境", true),
    PROD("prod", "生产环境", true),
    DEMO("demo", "演示环境", false);
    private final String value;

    private final String label;

    /**
     * 是否可以敏感操作
     */
    private final boolean sensitive;


    Env(String value, String label, boolean sensitive) {
        this.value = value;
        this.label = label;
        this.sensitive = sensitive;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public boolean getSensitive() {
        return sensitive;
    }

    public boolean isDev() {
        return this == DEV;
    }

    public boolean isProd() {
        return this == PROD;
    }

    public boolean isTest() {
        return this == TEST || this == SIT || this == UAT;
    }

    public boolean isPre() {
        return this == PRE;
    }

    public boolean isDemo() {
        return this == DEMO;
    }

    public static Env fromValue(String value) {
        return BaseEnum.fromValue(Env.class, value);
    }

}
