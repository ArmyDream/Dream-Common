package com.dream.common.core.constant;

import java.util.concurrent.TimeUnit;

/**
 * 业务常量
 */
public final class Business {
    private Business() {
    }

    // ==================== 默认值 ====================
    public static final String DEFAULT_ADMIN = "admin";
    public static final String DEFAULT_PASSWORD = "dream123456!";
    public static final String DEFAULT_AVATAR = "/avatar/default.jpg";

    /** 默认时间单位：秒 */
    public static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    /** 初始容量 */
    public static final int CACHE_INITIAL_CAPACITY = 250;

    /** 最大缓存条目数 10 万条 */
    public static final long CACHE_MAXIMUM_SIZE = 100_000L;

    // ==================== 缓存过期时间（秒） ====================
    public static final long CACHE_EXPIRE_1_MIN = 60;
    public static final long CACHE_EXPIRE_2_MIN = 120;
    public static final long CACHE_EXPIRE_3_MIN = 180;
    public static final long CACHE_EXPIRE_5_MIN = 300;
    public static final long CACHE_EXPIRE_10_MIN = 600;
    public static final long CACHE_EXPIRE_15_MIN = 900;
    public static final long CACHE_EXPIRE_30_MIN = 1800;
    public static final long CACHE_EXPIRE_1_HOUR = 3600;
    public static final long CACHE_EXPIRE_2_HOUR = 7200;
    public static final long CACHE_EXPIRE_6_HOUR = 21600;
    public static final long CACHE_EXPIRE_12_HOUR = 43200;
    public static final long CACHE_EXPIRE_24_HOUR = 86400;
    public static final long CACHE_EXPIRE_7_DAY = 604800;
    public static final long CACHE_EXPIRE_30_DAY = 2592000;
    public static final long CACHE_EXPIRE_MAX = Long.MAX_VALUE;
    public static final long CACHE_NEVER_EXPIRE = -1;

    // ==================== 通用状态值 ====================
    public static final String STATUS_ENABLE = "0";
    public static final String STATUS_DISABLE = "1";
    public static final String YES = "1";
    public static final String NO = "0";

    // ==================== 文件上传限制 ====================
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    public static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB
    public static final int MAX_FILENAME_LENGTH = 200;

    // ==================== 验证码 ====================
    public static final int CAPTCHA_LENGTH = 4;
    public static final int CAPTCHA_EXPIRE = 300; // 5分钟
    public static final int CAPTCHA_MAX_RETRY = 5;

    // ==================== Token 相关 ====================
    public static final int TOKEN_EXPIRE_HOUR = 2;
    public static final int TOKEN_REFRESH_EXPIRE_DAY = 7;

    // ==================== 密码规则 ====================
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 20;
    public static final int PASSWORD_HISTORY_COUNT = 5; // 不能与最近N次密码相同

    // ==================== 登录限制 ====================
    public static final int LOGIN_MAX_RETRY = 5;
    public static final int LOGIN_LOCK_MINUTES = 30;

}
