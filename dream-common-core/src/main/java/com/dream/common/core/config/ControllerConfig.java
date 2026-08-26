package com.dream.common.core.config;

public class ControllerConfig {

    /**
     * 权限标识前缀，如 "system:user"。
     */
    private String permissionPrefix;

    /**
     * 操作日志标题（业务模块名），如 "用户管理"。
     */
    private String logTitle;

    /**
     * 是否启用操作日志记录，默认 true。
     */
    private boolean logEnabled;

    public ControllerConfig() {

    }
    public ControllerConfig(String permissionPrefix, String logTitle, boolean logEnabled) {
        this.permissionPrefix = permissionPrefix;
        this.logTitle = logTitle;
        this.logEnabled = logEnabled;
    }

    public String getPermissionPrefix() {
        return permissionPrefix;
    }

    public void setPermissionPrefix(String permissionPrefix) {
        this.permissionPrefix = permissionPrefix;
    }

    public String getLogTitle() {
        return logTitle;
    }

    public void setLogTitle(String logTitle) {
        this.logTitle = logTitle;
    }

    public boolean isLogEnabled() {
        return logEnabled;
    }

    public void setLogEnabled(boolean logEnabled) {
        this.logEnabled = logEnabled;
    }
}
