package com.dream.common.core.utils;

import com.dream.common.core.api.ExceptionHandlerDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.BiFunction;

/**
 * 处理工具类
 *
 * @author dream
 * @since 2026-09-04
 */
public class HandleUtils {

    private static final Logger log = LoggerFactory.getLogger(HandleUtils.class);

    /**
     * 通用委托异常处理
     *
     * @param request 请求上下文（通常是 Spring Web 的 {@code WebRequest}，
     *                业务侧可 {@code instanceof} 判断具体类型后转换使用）
     * @param handler 具体的处理方法，如 delegate::handleValidationException
     * @return 如果委托处理成功，返回响应结果；否则返回 null 继续下一个处理
     */
    public static R<?> handleWithDelegate(ExceptionHandlerDelegate delegate,
                                          Exception ex,
                                          Object request,
                                          boolean apply,
                                          BiFunction<Exception, Object, R<?>> handler) {
        if (delegate == null || ex == null) {
            log.warn("ExceptionHandlerDelegate 未实现 或 ex 为空，不处理异常");
            return null;
        }
        try {
            delegate.onException(ex, request);
        } catch (Exception e) {
            log.warn("onException 回调失败 ==> {}", e.getMessage(), e);
        }
        if (handler == null) {
            return null;
        }
        try {
            if (apply) {
                return handler.apply(ex, request);
            }
        } catch (Exception e) {
            log.warn("委托异常 回调失败 ==> {}", e.getMessage(), e);
            return null;
        }
        return null;
    }

    /**
     * 将异常堆栈转换为字符串。
     *
     * @param ex 异常
     * @return 堆栈字符串
     */
    public static String stackTraceToString(Throwable ex) {
        if (ex == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            ex.printStackTrace(pw);
        }
        return sw.toString();
    }
}
