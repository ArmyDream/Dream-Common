package com.dream.common.core.utils;

import com.dream.common.core.enums.ResponseCode;
import com.dream.common.core.constant.SysKey;
import com.dream.common.core.enums.StringPool;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 统一响应结果集
 * <p>
 * 使用 Map 存储数据，防止 key 修改导致业务大面积改动
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 成功响应
 * R.ok();
 * R.ok(data);
 * R.ok(data, total);
 *
 * // 失败响应
 * R.fail();
 * R.fail("用户不存在");
 * R.fail(ResponseCodeEnum.USER_NOT_FOUND);
 *
 * // 自定义
 * R.error(500, "系统错误");
 * }</pre>
 *
 * @author dream
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 常量 ====================

    /**
     * 响应码 key
     */
    private static final String KEY_CODE = SysKey.RESPONSE_CODE;

    /**
     * 响应消息 key
     */
    private static final String KEY_MSG = SysKey.MSG;

    /**
     * 异常信息 key
     */
    private static final String KEY_EXCEPTION = SysKey.EXCEPTION;

    /**
     * 数据 key
     */
    private static final String KEY_DATA = SysKey.DATA;

    /**
     * 总记录数 key
     */
    private static final String KEY_TOTAL = SysKey.TOTAL;

    /**
     * 成功状态码
     */
    private static final Integer CODE_SUCCESS = ResponseCode.OK.getValue();

    // ==================== 存储容器 ====================

    /**
     * 存储实际数据的 Map（使用 LinkedHashMap 保证字段顺序）
     */
    private final LinkedHashMap<String, Object> map = new LinkedHashMap<>();

    // ==================== 私有构造方法 ====================

    private R() {
    }

    private R(ResponseCode responseCode, String msg, String exception, T data, Long total) {
        this(responseCode.getValue(), msg == null ? responseCode.getLabel() : msg, exception, data, total);
    }

    private R(Integer code, String msg, String exception, T data) {
        this(code, msg, exception, data, null);
    }

    private R(Integer code, String msg, String exception, T data, Long total) {
        map.put(KEY_CODE, code);
        map.put(KEY_MSG, msg != null ? msg : "");

        if (exception != null && !exception.isEmpty()) {
            map.put(KEY_EXCEPTION, exception);
        }

        map.put(KEY_DATA, data);

        if (total != null) {
            map.put(KEY_TOTAL, total);
        }
        //时间
        map.put(SysKey.TIMESTAMP, System.currentTimeMillis());
    }

    // ==================== 成功响应 ====================

    /**
     * 成功响应（无数据）
     */
    public static R<Void> ok() {
        return ok(ResponseCode.OK.getLabel());
    }

    /**
     * 成功响应（自定义消息，无数据）
     */
    public static R<Void> ok(String msg) {
        return new R<>(CODE_SUCCESS, msg, null, null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> R<T> ok(T data) {
        return ok(ResponseCode.OK.getLabel(), data);
    }

    /**
     * 成功响应（自定义消息，带数据）
     */
    public static <T> R<T> ok(String msg, T data) {
        return new R<>(CODE_SUCCESS, msg, null, data);
    }

    /**
     * 成功响应（分页数据）
     */
    public static <T> R<T> ok(T data, Long total) {
        return ok(ResponseCode.OK.getLabel(), data, total);
    }

    /**
     * 成功响应（自定义消息，分页数据）
     */
    public static <T> R<T> ok(String msg, T data, Long total) {
        return new R<>(CODE_SUCCESS, msg, null, data, total);
    }

    /**
     * 使用 ResponseCodeEnum 成功响应
     */
    public static <T> R<T> ok(ResponseCode responseCode, T data) {
        return new R<>(responseCode.getValue(), responseCode.getLabel(), null, data);
    }

    /**
     * 使用 ResponseCodeEnum 成功响应（分页数据）
     */
    public static <T> R<T> ok(ResponseCode responseCode, T data, Long total) {
        return new R<>(responseCode.getValue(), responseCode.getLabel(), null, data, total);
    }

    // ==================== 失败响应 ====================

    /**
     * 失败响应（默认）
     */
    public static R<Void> fail() {
        return fail(ResponseCode.FAIL);
    }

    /**
     * 失败响应（自定义消息）
     */
    public static R<Void> fail(String msg) {
        return fail(ResponseCode.FAIL, msg);
    }

    /**
     * 失败响应（自定义消息 + 异常）
     */
    public static R<Void> fail(String msg, String exception) {
        return new R<>(ResponseCode.FAIL.getValue(), msg, exception, null);
    }

    /**
     * 失败响应（自定义消息 + 异常）
     */
    public static R<Void> fail(ResponseCode responseCode, String msg, String exception) {
        return new R<>(responseCode, msg, exception, null, null);
    }

    /**
     * 失败响应（使用 ResponseCodeEnum）
     */
    public static R<Void> fail(ResponseCode responseCode) {
        return fail(responseCode, responseCode.getLabel());
    }

    /**
     * 失败响应（使用 ResponseCodeEnum + 自定义消息）
     */
    public static R<Void> fail(ResponseCode responseCode, String msg) {
        return new R<>(responseCode.getValue(), msg, null, null);
    }

    /**
     * 失败响应（使用 ResponseCodeEnum + 参数替换）
     */
    public static R<Void> fail(ResponseCode responseCode, Object... args) {
        String msg = responseCode.getMessage(args);
        return new R<>(responseCode.getValue(), msg, null, null);
    }

    // ==================== 自定义响应 ====================

    /**
     * 自定义响应
     */
    public static <T> R<T> result(ResponseCode responseCode, String msg, T data) {
        return new R<>(responseCode, msg, null, data, null);
    }

    /**
     * 自定义响应（带异常信息）
     */
    public static <T> R<T> result(ResponseCode responseCode, String msg, String exception, T data) {
        return new R<>(responseCode, msg, exception, data, null);
    }

    /**
     * 自定义响应（分页数据）
     */
    public static <T> R<T> result(ResponseCode responseCode, String msg, T data, Long total) {
        return new R<>(responseCode, msg, null, data, total);
    }


    // ==================== 快捷失败响应（基于 responseCode） ====================

    /**
     * 参数错误
     */
    public static R<Void> badRequest() {
        return fail(ResponseCode.BAD_REQUEST);
    }

    /**
     * 参数错误（自定义消息）
     */
    public static R<Void> badRequest(String msg) {
        return fail(ResponseCode.BAD_REQUEST, msg);
    }

    /**
     * 未认证
     */
    public static R<Void> unauthorized() {
        return fail(ResponseCode.UNAUTHORIZED);
    }

    /**
     * 未认证（自定义消息）
     */
    public static R<Void> unauthorized(String msg) {
        return fail(ResponseCode.UNAUTHORIZED, msg);
    }

    /**
     * 无权限
     */
    public static R<Void> forbidden() {
        return fail(ResponseCode.FORBIDDEN);
    }

    /**
     * 无权限（自定义消息）
     */
    public static R<Void> forbidden(String msg) {
        return fail(ResponseCode.FORBIDDEN, msg);
    }

    /**
     * 资源不存在
     */
    public static R<Void> notFound() {
        return fail(ResponseCode.NOT_FOUND);
    }

    /**
     * 资源不存在（自定义消息）
     */
    public static R<Void> notFound(String msg) {
        return fail(ResponseCode.NOT_FOUND, msg);
    }

    /**
     * 数据已存在
     */
    public static R<Void> alreadyExists() {
        return fail(ResponseCode.DATA_ALREADY_EXISTS);
    }

    /**
     * 数据已存在（自定义消息）
     */
    public static R<Void> alreadyExists(String msg) {
        return fail(ResponseCode.DATA_ALREADY_EXISTS, msg);
    }

    /**
     * 内部错误
     */
    public static R<Void> internalError() {
        return fail(ResponseCode.INTERNAL_ERROR);
    }

    /**
     * 内部错误（自定义消息）
     */
    public static R<Void> internalError(String msg) {
        return fail(ResponseCode.INTERNAL_ERROR, msg);
    }

    /**
     * 自定义错误响应
     */
    public static R<Void> error(Integer code, String msg) {
        return new R<>(code, msg, null, null);
    }

    // ==================== 公共方法 ====================

    /**
     * 获取响应码
     */
    public Integer getCode() {
        return (Integer) map.get(KEY_CODE);
    }

    /**
     * 获取响应消息
     */
    public String getMsg() {
        return (String) map.get(KEY_MSG);
    }

    /**
     * 获取异常信息
     */
    public String getException() {
        return (String) map.get(KEY_EXCEPTION);
    }

    /**
     * 获取数据
     */
    @SuppressWarnings("unchecked")
    public T getData() {
        return (T) map.get(KEY_DATA);
    }

    /**
     * 获取总记录数
     */
    public Long getTotal() {
        return (Long) map.get(KEY_TOTAL);
    }

    /**
     * 当前时间戳
     */
    public Long getTimestamp() {
        return (Long) map.get(SysKey.TIMESTAMP);
    }

    /**
     * 路径
     */
    public String getPath() {
        if (map.get(SysKey.PATH) != null) {
            return (String) map.get(SysKey.PATH);
        }
        return StringPool.EMPTY.getValue();
    }

    /**
     * 转换为 Map
     */
    public LinkedHashMap<String, Object> toMap() {
        return new LinkedHashMap<>(map);
    }

    /**
     * 获取原始 Map（返回防御性副本，防止外部直接修改内部数据）
     */
    protected LinkedHashMap<String, Object> getMap() {
        return new LinkedHashMap<>(map);
    }

    /**
     * 链式调用：添加自定义字段
     * <p>
     * 使用示例：R.ok().put("extra", value)
     */
    public R<T> put(String key, Object value) {
        if (key != null && !key.isEmpty()) {
            map.put(key, value);
        }
        return this;
    }

    /**
     * 链式调用：批量添加自定义字段
     */
    public R<T> putAll(Map<String, Object> extraMap) {
        if (extraMap != null && !extraMap.isEmpty()) {
            map.putAll(extraMap);
        }
        return this;
    }

    @Override
    public String toString() {
        return map.toString();
    }
}