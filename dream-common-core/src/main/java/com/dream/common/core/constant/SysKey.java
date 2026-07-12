package com.dream.common.core.constant;

/**
 * 系统 Key 常量
 * <p>
 * 统一管理系统中常用的键名常量
 * <p>
 *
 */
public final class SysKey {

    private SysKey() {
        // 防止实例化
    }

    // ==================== 分页相关 ====================

    /**
     * 分页当前页 key
     */
    public static final String PAGE_NUM = "pageNum";

    /**
     * 分页大小 key
     */
    public static final String PAGE_SIZE = "pageSize";

    /**
     * 总记录数 key
     */
    public static final String TOTAL = "total";

    /**
     * 总页数 key
     */
    public static final String PAGES = "pages";

    /**
     * 分页列表数据 key
     */
    public static final String LIST = "list";

    // ==================== 响应数据相关 ====================

    /**
     * 数据 key（单个对象）
     */
    public static final String DATA = "data";

    /**
     * 列表数据 key（数组）
     */
    public static final String ROWS = "rows";

    /**
     * 数据 ID key
     */
    public static final String ID = "id";

    /**
     * 数据编码 key
     */
    public static final String CODE = "code";

    /**
     * 数据名称 key
     */
    public static final String NAME = "name";

    /**
     * 数据标签 key
     */
    public static final String LABEL = "label";

    /**
     * 数据值 key
     */
    public static final String VALUE = "value";

    /**
     * 数据状态 key
     */
    public static final String STATUS = "status";

    /**
     * 数据类型 key
     */
    public static final String TYPE = "type";

    /**
     * 数据备注 key
     */
    public static final String REMARK = "remark";

    /**
     * 数据描述 key
     */
    public static final String DESCRIPTION = "description";

    /**
     * 数据排序 key
     */
    public static final String SORT = "sort";

    /**
     * 数据顺序 key
     */
    public static final String ORDER = "order";

    /**
     * 创建时间 key
     */
    public static final String CREATE_TIME = "createTime";

    /**
     * 更新时间 key
     */
    public static final String UPDATE_TIME = "updateTime";

    /**
     * 删除时间 key
     */
    public static final String DELETE_TIME = "deleteTime";

    /**
     * 审核时间
     */
    public static final String AUDIT_TIME = "auditTime";

    /**
     * 数据审核人 key
     */
    public static final String AUDIT_BY = "auditBy";

    /**
     * 数据审核人名称 key
     */
    public static final String AUDIT_NAME = "auditName";

    /**
     * 创建人 key
     */
    public static final String CREATE_BY = "createBy";

    /**
     * 创建人名称 key
     */
    public static final String CREATE_NAME = "createName";

    /**
     * 更新人 key
     */
    public static final String UPDATE_BY = "updateBy";

    /**
     * 更新人名称 key
     */
    public static final String UPDATE_NAME = "updateName";

    /**
     * 删除标识 key
     */
    public static final String DEL_FLAG = "deleted";

    /**
     * 版本号 key
     */
    public static final String VERSION = "version";

    // ==================== 响应状态相关 ====================

    /**
     * 响应码 key
     */
    public static final String RESPONSE_CODE = "code";

    /**
     * 响应消息 key
     */
    public static final String MSG = "msg";

    /**
     * 响应消息 key（别名）
     */
    public static final String MESSAGE = "message";

    /**
     * 响应成功标识 key
     */
    public static final String SUCCESS = "success";

    /**
     * 响应失败标识 key
     */
    public static final String FAIL = "fail";

    /**
     * 响应时间戳 key
     */
    public static final String TIMESTAMP = "timestamp";

    /**
     * 异常信息 key
     */
    public static final String EXCEPTION = "exception";


    /**
     * 路径 key
     */
    public static final String PATH = "path";

    // ==================== Token / 认证相关 ====================

    /**
     * 用户 ID key
     */
    public static final String USER_ID = "userId";

    /**
     * 用户名 key
     */
    public static final String USER_NAME = "userName";

    /**
     * 用户昵称 key
     */
    public static final String NICK_NAME = "nickName";

    /**
     * 用户头像 key
     */
    public static final String AVATAR = "avatar";

    /**
     * 用户邮箱 key
     */
    public static final String EMAIL = "email";

    /**
     * 用户手机号 key
     */
    public static final String PHONE = "phone";

    /**
     * 角色 key
     */
    public static final String ROLE = "role";

    /**
     * 角色列表 key
     */
    public static final String ROLES = "roles";

    /**
     * 权限 key
     */
    public static final String PERMISSION = "permission";

    /**
     * 权限列表 key
     */
    public static final String PERMISSIONS = "permissions";

    /**
     * 登录时间 key
     */
    public static final String LOGIN_TIME = "loginTime";

    /**
     * 登录 IP key
     */
    public static final String LOGIN_IP = "loginIp";

    /**
     * 登录地址 key
     */
    public static final String LOGIN_LOCATION = "loginLocation";

    /**
     * 浏览器 key
     */
    public static final String BROWSER = "browser";

    /**
     * 操作系统 key
     */
    public static final String OS = "os";

    // ==================== 字典相关 ====================

    /**
     * 字典类型 key
     */
    public static final String DICT_TYPE = "dictType";

    /**
     * 字典标签 key
     */
    public static final String DICT_LABEL = "dictLabel";

    /**
     * 字典值 key
     */
    public static final String DICT_VALUE = "dictValue";

    /**
     * 字典编码 key
     */
    public static final String DICT_CODE = "dictCode";


    // ==================== 租户相关 ====================

    /**
     * 租户 ID key
     */
    public static final String TENANT_ID = "tenantId";

    /**
     * 租户名称 key
     */
    public static final String TENANT_NAME = "tenantName";

    /**
     * 租户编码 key
     */
    public static final String TENANT_CODE = "tenantCode";

    /**
     * 租户状态 key
     */
    public static final String TENANT_STATUS = "tenantStatus";

    // ==================== 树相关 ====================


    /**
     * 树根节点父 ID（默认为 ""）
     */
    public static final String TREE_ROOT_NODE_VALUE = "";

    /**
     * 树根节点父 ID 为空值标识（用于表示根节点）
     * 包括：null、空字符串、空白字符串、0、-1
     */
    public static final String TREE_ROOT_EMPTY = "root_empty";

    /**
     * 树节点 ID key
     */
    public static final String TREE_ID = "id";

    /**
     * 树节点标签 key
     */
    public static final String TREE_LABEL = "label";

    /**
     * 树节点名称 key
     */
    public static final String TREE_NAME = "name";

    /**
     * 树节点父 ID key
     */
    public static final String TREE_PARENT_ID = "parentId";

    /**
     * 树节点子列表 key
     */
    public static final String TREE_CHILDREN = "children";

    /**
     * 树节点是否展开 key
     */
    public static final String TREE_EXPAND = "expand";

    /**
     * 树节点是否选中 key
     */
    public static final String TREE_CHECKED = "checked";

    /**
     * 树节点层级 key
     */
    public static final String TREE_LEVEL = "level";

    /**
     * 排除树节点字段
     */
    public static final String TREE_EXCLUDE_FIELDS = "serialVersionUID";

    /**
     * 是否禁用 key
     */
    public static final String DISABLED = "disabled";


    // ==================== 请求相关 ====================

    /**
     * 请求 ID key（用于链路追踪）
     */
    public static final String REQUEST_ID = "requestId";

    /**
     * 请求 IP key
     */
    public static final String REQUEST_IP = "requestIp";

    /**
     * 请求 URL key
     */
    public static final String REQUEST_URL = "requestUrl";

    /**
     * 请求方法 key
     */
    public static final String REQUEST_METHOD = "requestMethod";

    /**
     * 请求参数 key
     */
    public static final String REQUEST_PARAMS = "requestParams";

    /**
     * 请求头 key
     */
    public static final String REQUEST_HEADERS = "requestHeaders";

    /**
     * User-Agent key
     */
    public static final String USER_AGENT = "userAgent";

    /**
     * Referer key
     */
    public static final String REFERER = "referer";

    /**
     * Accept-Language key
     */
    public static final String ACCEPT_LANGUAGE = "acceptLanguage";

    // ==================== 国际化相关 ====================

    /**
     * 语言 key
     */
    public static final String LANG = "lang";

    /**
     * 语言（中文）
     */
    public static final String LANG_ZH = "zh-CN";

    /**
     * 语言（英文）
     */
    public static final String LANG_EN = "en-US";


    // ==================== 缓存相关 ====================

    /**
     * 缓存名称 key
     */
    public static final String CACHE_NAME = "cacheName";

    /**
     * 缓存 Key
     */
    public static final String CACHE_KEY = "cacheKey";

    /**
     * 缓存值 key
     */
    public static final String CACHE_VALUE = "cacheValue";

    // ==================== 其他 ====================

    /**
     * 空值
     */
    public static final String NULL = "null";

    /**
     * 空字符串
     */
    public static final String EMPTY = "";

    /**
     * 默认值
     */
    public static final String DEFAULT = "default";
}