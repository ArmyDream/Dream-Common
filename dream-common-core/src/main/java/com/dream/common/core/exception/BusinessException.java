package com.dream.common.core.exception;

import com.dream.common.core.enums.ResponseCode;

import java.text.MessageFormat;

/**
 * 业务异常
 * <p>
 * 用于业务逻辑层抛出异常，携带错误码和参数化消息
 * <p>
 *
 * @author dream
 */
public class BusinessException extends RuntimeException {

    /**
     * 响应枚举
     */
    private ResponseCode responseCode;

    // ==================== 构造器 ====================

    /**
     * 使用响应枚举构造异常
     *
     * @param responseCode 响应枚举
     */
    public BusinessException(ResponseCode responseCode) {
        super(responseCode.getLabel());
        this.responseCode = responseCode;
    }

    /**
     * 使用响应枚举 + 参数构造异常（支持消息参数化）
     *
     * @param responseCode 响应枚举
     * @param args       消息参数
     */
    public BusinessException(ResponseCode responseCode, Object... args) {
        super(formatMessage(responseCode, args));
        this.responseCode = responseCode;
    }

    /**
     * 使用自定义消息构造异常
     *
     * @param message 自定义消息
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * 使用响应枚举 + 自定义消息构造异常
     * <p>
     * 注意：此构造器与 {@link #BusinessException(ResponseCode, Object...)} 不同，
     * 直接使用自定义消息而非格式化 label。
     *
     * @param responseCode 响应枚举
     * @param message      自定义消息
     */
    public BusinessException(ResponseCode responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
    }

    /**
     * 使用自定义消息 + 原因构造异常
     *
     * @param message 自定义消息
     * @param cause   原因
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.responseCode = ResponseCode.INTERNAL_ERROR;
    }

    /**
     * 使用响应枚举 + 原因构造异常
     *
     * @param responseCode 响应枚举
     * @param cause      原因
     */
    public BusinessException(ResponseCode responseCode, Throwable cause) {
        super(responseCode.getLabel(), cause);
        this.responseCode = responseCode;
    }

    /**
     * 使用原因构造异常
     *
     * @param cause 原因
     */
    public BusinessException(Throwable cause) {
        super(cause);
        this.responseCode = ResponseCode.INTERNAL_ERROR;
    }

    // ==================== 私有方法 ====================

    /**
     * 格式化消息
     */
    private static String formatMessage(ResponseCode responseCode, Object... args) {
        if (args == null || args.length == 0) {
            return responseCode.getLabel();
        }
        try {
            return MessageFormat.format(responseCode.getLabel(), args);
        } catch (IllegalArgumentException e) {
            return responseCode.getLabel();
        }
    }

    // ==================== 快捷工厂方法 ====================

    /**
     * 创建业务异常（参数错误）
     *
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException badRequest(String message) {
        return new BusinessException(ResponseCode.BAD_REQUEST, message);
    }

    /**
     * 创建业务异常（未认证）
     *
     * @return 业务异常
     */
    public static BusinessException unauthorized() {
        return new BusinessException(ResponseCode.UNAUTHORIZED);
    }

    /**
     * 创建业务异常（无权限）
     *
     * @return 业务异常
     */
    public static BusinessException forbidden() {
        return new BusinessException(ResponseCode.FORBIDDEN);
    }

    /**
     * 创建业务异常（资源不存在）
     *
     * @return 业务异常
     */
    public static BusinessException notFound() {
        return new BusinessException(ResponseCode.NOT_FOUND);
    }

    /**
     * 创建业务异常（资源不存在）
     *
     * @param message 自定义消息
     * @return 业务异常
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(ResponseCode.NOT_FOUND, message);
    }

    /**
     * 创建业务异常（数据已存在）
     *
     * @return 业务异常
     */
    public static BusinessException alreadyExists() {
        return new BusinessException(ResponseCode.DATA_ALREADY_EXISTS);
    }

    /**
     * 创建业务异常（数据已存在）
     *
     * @param message 自定义消息
     * @return 业务异常
     */
    public static BusinessException alreadyExists(String message) {
        return new BusinessException(ResponseCode.DATA_ALREADY_EXISTS, message);
    }

    /**
     * 创建业务异常（内部错误）
     *
     * @return 业务异常
     */
    public static BusinessException internalError() {
        return new BusinessException(ResponseCode.INTERNAL_ERROR);
    }

    /**
     * 创建业务异常（内部错误）
     *
     * @param message 自定义消息
     * @return 业务异常
     */
    public static BusinessException internalError(String message) {
        return new BusinessException(ResponseCode.INTERNAL_ERROR, message);
    }

    public ResponseCode getResultCode() {
        return responseCode;
    }

    public void setResultCode(ResponseCode responseCode) {
        this.responseCode = responseCode;
    }

    // ==================== 重写方法 ====================


    @Override
    public String toString() {
        return "BusinessException{" +
                "responseCode=" + responseCode +
                '}';
    }
}