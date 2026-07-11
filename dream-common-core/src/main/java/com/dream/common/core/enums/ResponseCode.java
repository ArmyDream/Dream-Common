package com.dream.common.core.enums;

/**
 * 响应码枚举
 * <p>
 * 统一管理 HTTP 状态码和业务响应码，以 HTTP 状态码为基础
 * <p>
 * Code 范围分配：
 * <ul>
 *   <li>100~599：HTTP 标准状态码</li>
 *   <li>2000~2999：客户端业务错误</li>
 *   <li>3000~3999：服务端业务错误</li>
 *   <li>4000~4999：用户相关业务错误</li>
 *   <li>5000~5999：数据/文件相关业务错误</li>
 *   <li>6000~6999：接口/系统相关业务错误</li>
 *   <li>7000~7999：权限相关业务错误</li>
 *   <li>8000~8999：通用业务错误</li>
 * </ul>
 *
 */
@SuppressWarnings("all")
public enum ResponseCode implements BaseEnum<Integer> {

    // ==================== 1xx 信息响应 ====================
    CONTINUE(100, "Continue", "继续"),
    SWITCHING_PROTOCOLS(101, "Switching Protocols", "切换协议"),
    PROCESSING(102, "Processing", "处理中"),
    EARLY_HINTS(103, "Early Hints", "早期提示"),

    // ==================== 2xx 成功 ====================
    OK(200, "OK", "操作成功"),
    CREATED(201, "Created", "创建成功"),
    ACCEPTED(202, "Accepted", "已接受"),
    NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information", "非权威信息"),
    NO_CONTENT(204, "No Content", "无内容"),
    RESET_CONTENT(205, "Reset Content", "重置内容"),
    PARTIAL_CONTENT(206, "Partial Content", "部分内容"),
    MULTI_STATUS(207, "Multi-Status", "多状态"),
    ALREADY_REPORTED(208, "Already Reported", "已报告"),
    IM_USED(226, "IM Used", "IM 已使用"),

    // ==================== 3xx 重定向 ====================
    MULTIPLE_CHOICES(300, "Multiple Choices", "多种选择"),
    MOVED_PERMANENTLY(301, "Moved Permanently", "永久移动"),
    FOUND(302, "Found", "临时移动"),
    SEE_OTHER(303, "See Other", "查看其他位置"),
    NOT_MODIFIED(304, "Not Modified", "未修改"),
    USE_PROXY(305, "Use Proxy", "使用代理"),
    TEMPORARY_REDIRECT(307, "Temporary Redirect", "临时重定向"),
    PERMANENT_REDIRECT(308, "Permanent Redirect", "永久重定向"),

    // ==================== 4xx 客户端错误 ====================
    BAD_REQUEST(400, "Bad Request", "请求参数错误"),
    UNAUTHORIZED(401, "Unauthorized", "未认证，请重新登录"),
    PAYMENT_REQUIRED(402, "Payment Required", "需要付款"),
    FORBIDDEN(403, "Forbidden", "无权限访问"),
    NOT_FOUND(404, "Not Found", "资源不存在"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed", "请求方法不允许"),
    NOT_ACCEPTABLE(406, "Not Acceptable", "不可接受"),
    PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required", "需要代理认证"),
    REQUEST_TIMEOUT(408, "Request Timeout", "请求超时"),
    CONFLICT(409, "Conflict", "资源冲突"),
    GONE(410, "Gone", "资源已移除"),
    LENGTH_REQUIRED(411, "Length Required", "需要 Content-Length"),
    PRECONDITION_FAILED(412, "Precondition Failed", "前置条件失败"),
    PAYLOAD_TOO_LARGE(413, "Payload Too Large", "请求体过大"),
    URI_TOO_LONG(414, "URI Too Long", "URI 过长"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type", "不支持的媒体类型"),
    RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable", "范围不满足"),
    EXPECTATION_FAILED(417, "Expectation Failed", "期望失败"),
    IM_A_TEAPOT(418, "I'm a teapot", "我是茶壶"),
    MISDIRECTED_REQUEST(421, "Misdirected Request", "被误导的请求"),
    UNPROCESSABLE_ENTITY(422, "Unprocessable Entity", "不可处理的实体"),
    LOCKED(423, "Locked", "已锁定"),
    FAILED_DEPENDENCY(424, "Failed Dependency", "依赖失败"),
    TOO_EARLY(425, "Too Early", "过早请求"),
    UPGRADE_REQUIRED(426, "Upgrade Required", "需要升级"),
    PRECONDITION_REQUIRED(428, "Precondition Required", "需要前置条件"),
    TOO_MANY_REQUESTS(429, "Too Many Requests", "请求过于频繁"),
    REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large", "请求头过大"),
    UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable For Legal Reasons", "因法律原因不可用"),

    // ==================== 5xx 服务端错误 ====================
    INTERNAL_ERROR(500, "Internal Server Error", "系统内部异常，请联系管理员"),
    NOT_IMPLEMENTED(501, "Not Implemented", "未实现"),
    BAD_GATEWAY(502, "Bad Gateway", "网关错误"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable", "服务不可用"),
    GATEWAY_TIMEOUT(504, "Gateway Timeout", "网关超时"),
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported", "HTTP 版本不支持"),
    VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates", "变体协商"),
    INSUFFICIENT_STORAGE(507, "Insufficient Storage", "存储空间不足"),
    LOOP_DETECTED(508, "Loop Detected", "循环检测"),
    NOT_EXTENDED(510, "Not Extended", "未扩展"),
    NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required", "需要网络认证"),

    // ==================== 1000~1999：业务成功 ====================


    // ==================== 2000~2999：客户端业务错误 ====================
    FAIL(2000, "Fail", "调用失败"),
    PARAM_VALIDATE_ERROR(2002, "Parameter Validate Error", "参数验证异常"),
    PARAM_EMPTY(2003, "Parameter Empty", "参数为空"),
    PARAM_BIND_ERROR(2004, "Parameter Bind Error", "请求参数绑定异常"),
    PARAM_MISSING(2005, "Parameter Missing", "参数缺失"),
    PARAM_CONVERT_ERROR(2006, "Parameter Convert Error", "请求参数转换异常"),
    PARAM_INVALID(2007, "Parameter Invalid", "非法参数异常"),
    PARAM_TOO_LARGE(2008, "Parameter Too Large", "参数值过大"),
    PARAM_TOO_LONG(2009, "Parameter Too Long", "参数长度超限"),
    PARAM_FORMAT_ERROR(2010, "Parameter Format Error", "参数格式错误"),
    REJECTED(2011, "Rejected", "审批拒绝"),
    DEMO_ENVIRONMENT(2012, "Demo Environment", "演示环境，不允许此操作"),

    // 分页相关 2100~2109
    PAGE_SIZE_TOO_LARGE(2100, "Page Size Too Large", "每页查询最多[{0}]条数据"),
    BATCH_SIZE_TOO_LARGE(2101, "Batch Size Too Large", "批量操作最大[{0}]条数据"),

    // 认证/Token 相关 2200~2299
    TOKEN_EXPIRED(2201, "Token Expired", "Token 已过期"),
    TOKEN_INVALID(2202, "Token Invalid", "Token 无效"),
    TOKEN_MISSING(2203, "Token Missing", "缺少 Token"),
    TOKEN_REFRESH_FAILED(2204, "Token Refresh Failed", "Token 刷新失败"),

    // 请求相关 2400~2499
    METHOD_NOT_ALLOWED_BIZ(2401, "Method Not Allowed", "请求方式错误，该接口不支持[{0}]请求"),
    UNSUPPORTED_MEDIA_TYPE_BIZ(2402, "Unsupported Media Type", "不支持当前媒体类型"),
    TOO_MANY_REQUESTS_BIZ(2403, "Too Many Requests", "访问太频繁，请休息一会儿"),
    DUPLICATE_SUBMIT(2405, "Duplicate Submit", "请勿重复提交"),

    // ==================== 3000~3999：服务端业务错误 ====================
    NULL_POINTER(3002, "Null Pointer", "数据空指针异常"),
    IO_ERROR(3003, "IO Error", "文件IO异常"),
    PATH_NOT_FOUND(3004, "Path Not Found", "路径不存在异常"),
    SERIALIZE_ERROR(3005, "Serialize Error", "响应消息序列化异常"),
    BEAN_NOT_FOUND(3006, "Bean Not Found", "服务Bean未配置"),
    SQL_ERROR(3007, "SQL Error", "SQL语法错误异常"),
    CONFIG_ERROR(3008, "Config Error", "配置错误"),
    UNKNOWN_ERROR(3099, "Unknown Error", "未知错误"),

    // ==================== 4000~4999：用户相关 ====================
    USER_NOT_FOUND(4000, "User Not Found", "用户不存在"),
    USER_PASSWORD_ERROR(4001, "Password Error", "账号或密码错误"),
    USER_DISABLED(4002, "User Disabled", "账号已被禁用"),
    USER_ALREADY_EXISTS(4003, "User Already Exists", "用户已存在"),
    USER_LOCKED(4004, "User Locked", "用户已被锁定"),
    USER_EXPIRED(4005, "User Expired", "用户已过期"),
    USER_LOGIN_ERROR(4007, "Login Error", "登录失败"),
    USER_LOGOUT_ERROR(4008, "Logout Error", "登出失败"),
    USER_REGISTER_ERROR(4009, "Register Error", "注册失败"),
    USER_OLD_PASSWORD_ERROR(4010, "Old Password Error", "原密码错误"),
    USER_PASSWORD_INVALID(4011, "Password Invalid", "密码不符合规则"),
    USER_PASSWORD_SAME(4012, "Password Same", "新密码不能与旧密码相同"),
    USER_EMAIL_ALREADY_EXISTS(4013, "Email Already Exists", "邮箱已被使用"),
    USER_PHONE_ALREADY_EXISTS(4014, "Phone Already Exists", "手机号已被使用"),
    USER_VERIFY_CODE_ERROR(4015, "Verify Code Error", "验证码错误"),
    USER_VERIFY_CODE_EXPIRED(4016, "Verify Code Expired", "验证码已过期"),
    USER_VERIFY_CODE_SEND_FAILED(4017, "Verify Code Send Failed", "验证码发送失败"),
    USER_SESSION_EXPIRED(4018, "Session Expired", "会话已过期"),

    // ==================== 5000~5999：数据/文件相关 ====================
    DATA_NOT_FOUND(5000, "Data Not Found", "数据未找到"),
    DATA_ALREADY_EXISTS(5001, "Data Already Exists", "数据已存在"),
    DATA_REFERENCED(5002, "Data Referenced", "数据被引用，无法删除"),
    DATA_UPDATE_FAILED(5003, "Data Update Failed", "数据更新失败"),
    DATA_INSERT_FAILED(5004, "Data Insert Failed", "数据插入失败"),
    DATA_DELETE_FAILED(5005, "Data Delete Failed", "数据删除失败"),
    DATA_VERSION_CONFLICT(5009, "Data Version Conflict", "数据版本冲突"),

    FILE_NOT_FOUND(5100, "File Not Found", "文件不存在"),
    FILE_EMPTY(5101, "File Empty", "文件为空"),
    FILE_TOO_LARGE(5102, "File Too Large", "上传文件过大"),
    FILE_FORMAT_ERROR(5103, "File Format Error", "文件格式不支持"),
    FILE_UPLOAD_FAILED(5104, "File Upload Failed", "文件上传失败"),
    FILE_DOWNLOAD_FAILED(5105, "File Download Failed", "文件下载失败"),
    FILE_DELETE_FAILED(5106, "File Delete Failed", "文件删除失败"),

    EXCEL_READ_ERROR(5200, "Excel Read Error", "Excel读取失败"),
    EXCEL_WRITE_ERROR(5201, "Excel Write Error", "Excel写入失败"),
    EXCEL_DATA_ERROR(5205, "Excel Data Error", "Excel数据错误"),

    // ==================== 6000~6999：接口/系统相关 ====================
    INTERNAL_API_ERROR(6000, "Internal API Error", "内部系统接口调用异常"),
    EXTERNAL_API_ERROR(6001, "External API Error", "外部系统接口调用异常"),
    API_FORBIDDEN(6002, "API Forbidden", "该接口禁止访问"),
    API_INVALID(6003, "API Invalid", "接口地址无效"),
    API_TIMEOUT(6004, "API Timeout", "接口请求超时"),

    RATE_LIMIT(6100, "Rate Limit", "服务开启限流保护，请稍后再试"),
    DEGRADE(6101, "Degrade", "服务开启降级保护，请稍后再试"),
    HOT_PARAM_LIMIT(6102, "Hot Param Limit", "热点参数限流，请稍后再试"),

    // ==================== 7000~7999：权限相关 ====================
    PERMISSION_NOT_FOUND(7000, "Permission Not Found", "权限标识不存在"),
    PERMISSION_ALREADY_EXISTS(7001, "Permission Already Exists", "权限标识已存在"),
    PERMISSION_HAS_CHILDREN(7003, "Permission Has Children", "权限存在子节点，无法删除"),

    ROLE_NOT_FOUND(7100, "Role Not Found", "角色不存在"),
    ROLE_ALREADY_EXISTS(7101, "Role Already Exists", "角色已存在"),
    ROLE_HAS_USERS(7102, "Role Has Users", "角色已被用户绑定，无法删除"),
    ROLE_NAME_DUPLICATE(7104, "Role Name Duplicate", "角色名称重复"),

    MENU_NOT_FOUND(7200, "Menu Not Found", "菜单不存在"),
    MENU_ALREADY_EXISTS(7201, "Menu Already Exists", "菜单已存在"),
    MENU_HAS_CHILDREN(7203, "Menu Has Children", "菜单存在子节点，无法删除"),
    MENU_NAME_DUPLICATE(7205, "Menu Name Duplicate", "菜单名称重复"),
    MENU_PATH_DUPLICATE(7206, "Menu Path Duplicate", "菜单路径重复"),

    DEPT_NOT_FOUND(7300, "Department Not Found", "部门不存在"),
    DEPT_ALREADY_EXISTS(7301, "Department Already Exists", "部门已存在"),
    DEPT_HAS_CHILDREN(7303, "Department Has Children", "部门存在子节点，无法删除"),
    DEPT_HAS_USERS(7304, "Department Has Users", "部门已被用户绑定，无法删除"),
    DEPT_NAME_DUPLICATE(7306, "Department Name Duplicate", "部门名称重复"),

    // ==================== 8000~8999：通用业务错误 ====================
    CONFIG_NOT_FOUND(8000, "Config Not Found", "配置不存在"),
    CONFIG_ALREADY_EXISTS(8001, "Config Already Exists", "配置已存在"),

    DICT_NOT_FOUND(8100, "Dictionary Not Found", "字典不存在"),
    DICT_ALREADY_EXISTS(8101, "Dictionary Already Exists", "字典已存在"),
    DICT_HAS_ITEMS(8102, "Dictionary Has Items", "字典存在子项，无法删除"),
    DICT_ITEM_NOT_FOUND(8103, "Dictionary Item Not Found", "字典项不存在"),
    DICT_ITEM_ALREADY_EXISTS(8104, "Dictionary Item Already Exists", "字典项已存在"),

    TENANT_NOT_FOUND(8200, "Tenant Not Found", "租户不存在"),
    TENANT_DISABLED(8201, "Tenant Disabled", "租户已禁用"),
    TENANT_ALREADY_EXISTS(8202, "Tenant Already Exists", "租户已存在"),

    NOTIFY_SEND_FAILED(8300, "Notify Send Failed", "通知发送失败"),
    NOTIFY_TEMPLATE_NOT_FOUND(8301, "Notify Template Not Found", "通知模板不存在"),

    JOB_NOT_FOUND(8400, "Job Not Found", "定时任务不存在"),
    JOB_RUNNING(8401, "Job Running", "定时任务正在运行"),
    JOB_STOPPED(8402, "Job Stopped", "定时任务已停止"),
    JOB_ALREADY_EXISTS(8404, "Job Already Exists", "定时任务已存在"),

    ORDER_NOT_FOUND(8500, "Order Not Found", "订单不存在"),
    ORDER_STATUS_ERROR(8501, "Order Status Error", "订单状态异常"),
    ORDER_CANCEL_FAILED(8502, "Order Cancel Failed", "订单取消失败"),
    ORDER_PAY_FAILED(8503, "Order Pay Failed", "订单支付失败"),
    ORDER_ALREADY_PAID(8504, "Order Already Paid", "订单已支付"),
    ORDER_CLOSED(8505, "Order Closed", "订单已关闭"),
    ORDER_NO_STOCK(8509, "No Stock", "库存不足"),

    CAPTCHA_ERROR(8600, "Captcha Error", "验证码错误"),
    CAPTCHA_EXPIRED(8601, "Captcha Expired", "验证码已过期"),
    CAPTCHA_SEND_FAILED(8602, "Captcha Send Failed", "验证码发送失败");

    // ==================== 字段 ====================

    private final Integer value;
    private final String labelEn;
    private final String labelZh;

    // ==================== 构造器 ====================

    ResponseCode(Integer value, String labelEn, String labelZh) {
        this.value = value;
        this.labelEn = labelEn;
        this.labelZh = labelZh;
    }

    // ==================== BaseEnum 实现 ====================

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return labelZh;
    }

    // ==================== 扩展方法 ====================

    public String getLabelEn() {
        return labelEn;
    }

    public String getLabelZh() {
        return labelZh;
    }

    public String getLabel(String lang) {
        if ("en".equalsIgnoreCase(lang)) {
            return labelEn;
        }
        return labelZh;
    }

    public String getMessage(Object... params) {
        if (params == null || params.length == 0) {
            return labelZh;
        }
        String message = labelZh;
        for (int i = 0; i < params.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(params[i]));
        }
        return message;
    }

    @Override
    public String getColor() {
        if (value >= 100 && value < 200) return "#8C8C8C";
        if (value >= 200 && value < 300) return "#52C41A";
        if (value >= 300 && value < 400) return "#1890FF";
        if (value >= 400 && value < 500) return "#FAAD14";
        if (value >= 500 && value < 600) return "#FF4D4F";
        if (value >= 1000 && value < 2000) return "#52C41A";
        if (value >= 2000 && value < 3000) return "#FAAD14";
        if (value >= 3000 && value < 4000) return "#FF4D4F";
        if (value >= 4000 && value < 5000) return "#FF7A45";
        if (value >= 5000 && value < 6000) return "#722ED1";
        if (value >= 6000 && value < 7000) return "#13C2C2";
        if (value >= 7000 && value < 8000) return "#2F54EB";
        if (value >= 8000 && value < 9000) return "#8C8C8C";
        return "#8C8C8C";
    }

    @Override
    public String getDescription() {
        return labelEn + " (" + value + ")";
    }

    // ==================== 静态方法 ====================

    public static ResponseCode fromValue(Integer value) {
        return BaseEnum.fromValue(ResponseCode.class, value);
    }

    public static ResponseCode fromValueOrDefault(Integer value, ResponseCode defaultValue) {
        return BaseEnum.fromValueOrDefault(ResponseCode.class, value, defaultValue);
    }

    public static boolean exists(Integer value) {
        return BaseEnum.exists(ResponseCode.class, value);
    }

}