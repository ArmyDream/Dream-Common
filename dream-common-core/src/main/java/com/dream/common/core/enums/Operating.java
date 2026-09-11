package com.dream.common.core.enums;


/**
 * 操作系统枚举
 *
 * @author dream
 */
public enum Operating implements BaseEnum<String> {

    WINDOWS("windows", "Windows"),
    LINUX("linux", "Linux"),
    MAC("mac", "Mac"),
    OTHER("other", "其他");

    private final String value;
    private final String label;

    Operating(String value, String label) {
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

    /**
     * 获取当前操作系统
     */
    public static Operating current() {
        String osName = SystemProperty.OS_NAME.getValue();
        if (osName == null || osName.isEmpty()) {
            return OTHER;
        }
        String os = osName.toLowerCase();
        if (os.contains("windows")) {
            return WINDOWS;
        } else if (os.contains("win")) {
            return WINDOWS;
        } else if (os.contains("linux")) {
            return LINUX;
        } else if (os.contains("mac")) {
            return MAC;
        }
        return OTHER;
    }

    public boolean isWindows() {
        return this == WINDOWS;
    }

    public boolean isLinux() {
        return this == LINUX;
    }

    public boolean isMac() {
        return this == MAC;
    }

    public boolean isOther() {
        return this == OTHER;
    }

    @Override
    public String getColor() {
        switch (this) {
            case WINDOWS:
                return "#0078D4";
            case LINUX:
                return "#FCC624";
            case MAC:
                return "#8C8C8C";
            default:
                return "#13C2C2";
        }
    }

}