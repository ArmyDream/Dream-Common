package com.dream.common.core.enums;

/**
 * 操作类型枚举
 * <p>
 * 用于操作日志记录，配合 @Log 注解使用
 */
public enum OperationType implements BaseEnum<String> {

    // ==================== 基础操作 ====================
    OTHER("other", "Other", "其他"),
    INSERT("insert", "Insert", "新增"),
    UPDATE("update", "Update", "修改"),
    DELETE("delete", "Delete", "删除"),
    VIEW("view", "View", "查看"),
    QUERY("query", "Query", "查询"),

    // ==================== 数据操作 ====================
    EXPORT("export", "Export", "导出"),
    IMPORT("import", "Import", "导入"),
    CLEAN("clean", "Clean", "清空"),
    COPY("copy", "Copy", "复制"),
    MOVE("move", "Move", "移动"),
    RESTORE("restore", "Restore", "恢复"),

    // ==================== 权限操作 ====================
    GRANT("grant", "Grant", "授权"),
    REVOKE("revoke", "Revoke", "撤销授权"),
    FORCE("force", "Force", "强退"),
    LOCK("lock", "Lock", "锁定"),
    UNLOCK("unlock", "Unlock", "解锁"),
    RESET_PASSWORD("reset_password", "Reset Password", "重置密码"),

    // ==================== 审批/审核 ====================
    APPROVE("approve", "Approve", "审批通过"),
    REJECT("reject", "Reject", "审批拒绝"),
    CANCEL("cancel", "Cancel", "取消"),
    SUBMIT("submit", "Submit", "提交"),

    // ==================== 代码/生成 ====================
    GENCODE("gencode", "Generate Code", "生成代码"),
    GENERATE("generate", "Generate", "生成"),

    // ==================== 系统操作 ====================
    REFRESH("refresh", "Refresh", "刷新缓存"),
    SYNC("sync", "Sync", "同步"),
    CONFIG("config", "Config", "配置"),
    DEPLOY("deploy", "Deploy", "部署"),
    START("start", "Start", "启动"),
    STOP("stop", "Stop", "停止"),
    RESTART("restart", "Restart", "重启"),

    // ==================== 文件操作 ====================
    UPLOAD("upload", "Upload", "上传"),
    DOWNLOAD("download", "Download", "下载"),
    PREVIEW("preview", "Preview", "预览"),

    // ==================== 登录/认证 ====================
    LOGIN("login", "Login", "登录"),
    LOGOUT("logout", "Logout", "注销"),
    REGISTER("register", "Register", "注册"),

    // ==================== 支付/订单 ====================
    PAY("pay", "Pay", "支付"),
    REFUND("refund", "Refund", "退款"),
    SHIP("ship", "Ship", "发货"),
    RECEIVE("receive", "Receive", "收货");

    private final String value;
    private final String labelEn;
    private final String labelZh;

    OperationType(String value, String labelEn, String labelZh) {
        this.value = value;
        this.labelEn = labelEn;
        this.labelZh = labelZh;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return labelZh;
    }

    public String getLabelEn() {
        return labelEn;
    }

    public String getLabelZh() {
        return labelZh;
    }

    /**
     * 根据语言获取标签
     *
     * @param lang 语言：zh / en
     * @return 对应语言的标签
     */
    public String getLabel(String lang) {
        if ("en".equalsIgnoreCase(lang)) {
            return labelEn;
        }
        return labelZh;
    }

    @Override
    public String getColor() {
        switch (this) {
            case INSERT:
            case REGISTER:
            case UPLOAD:
            case SUBMIT:
            case APPROVE:
                return "#52C41A"; // 绿色
            case UPDATE:
            case IMPORT:
            case MOVE:
            case COPY:
            case RESET_PASSWORD:
                return "#1890FF"; // 蓝色
            case DELETE:
            case CLEAN:
            case REJECT:
            case CANCEL:
            case REFUND:
                return "#FF4D4F"; // 红色
            case VIEW:
            case QUERY:
            case PREVIEW:
            case DOWNLOAD:
                return "#8C8C8C"; // 灰色
            case EXPORT:
            case GENCODE:
            case GENERATE:
                return "#722ED1"; // 紫色
            case GRANT:
            case REVOKE:
            case LOCK:
            case UNLOCK:
            case FORCE:
                return "#FAAD14"; // 橙色
            case LOGIN:
            case LOGOUT:
            case START:
            case STOP:
            case RESTART:
            case REFRESH:
                return "#13C2C2"; // 青色
            default:
                return "#8C8C8C";
        }
    }

    // ==================== 业务判断 ====================

    public boolean isWriteOperation() {
        return this == INSERT || this == UPDATE || this == DELETE
                || this == IMPORT || this == CLEAN || this == MOVE
                || this == GRANT || this == REVOKE || this == RESET_PASSWORD;
    }

    public boolean isReadOperation() {
        return this == VIEW || this == QUERY || this == PREVIEW;
    }

    public boolean isLoginOperation() {
        return this == LOGIN || this == LOGOUT || this == REGISTER;
    }

    // ==================== 静态方法 ====================

    public static OperationType fromValue(String value) {
        return BaseEnum.fromValue(OperationType.class, value);
    }

    public static boolean exists(String value) {
        return BaseEnum.exists(OperationType.class, value);
    }
}