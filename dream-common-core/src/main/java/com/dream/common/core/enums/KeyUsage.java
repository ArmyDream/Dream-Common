package com.dream.common.core.enums;

/**
 * 密钥用途枚举
 */
public enum KeyUsage implements BaseEnum<String> {

    SIGN("sign", "签名"),
    ENCRYPT("encrypt", "加密"),
    DECRYPT("decrypt", "解密"),
    VERIFY("verify", "验签");

    private final String value;

    private final String label;

    KeyUsage(String value, String label) {
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

    public static KeyUsage fromValue(String value) {
        return BaseEnum.fromValue(KeyUsage.class, value);
    }
}