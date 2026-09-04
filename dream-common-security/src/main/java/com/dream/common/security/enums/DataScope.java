package com.dream.common.security.enums;

import lombok.Getter;

/**
 * 数据权限范围枚举。
 *
 * @author dream
 * @since 2026-08-24
 */
@Getter
public enum DataScope {

    ALL(1, "全部数据"),
    CUSTOM(2, "自定义数据"),
    DEPT(3, "本部门数据"),
    DEPT_AND_CHILD(4, "本部门及子部门数据"),
    ORG(5, "本机构数据"),
    ORG_AND_CHILD(6, "本机构及子机构数据"),
    SELF(7, "仅本人数据");

    private final int code;
    private final String description;

    DataScope(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码获取枚举实例。
     *
     * @param code 编码
     * @return 对应的枚举，不存在返回 {@code null}
     */
    public static DataScope of(Integer code) {
        if (code == null) {
            return null;
        }
        for (DataScope scope : values()) {
            if (scope.code == code) {
                return scope;
            }
        }
        return null;
    }
}