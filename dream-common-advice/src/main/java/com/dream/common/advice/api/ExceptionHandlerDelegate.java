package com.dream.common.advice.api;

import com.dream.common.core.utils.R;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.ServletException;

/**
 * 全局异常处理委托接口。
 * <p>
 * 业务方可实现此接口以自定义异常处理逻辑（如告警、上报、审计等）。
 * 框架不强制实现，未注册时使用默认处理。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Component
 * public class MyExceptionHandler implements ExceptionHandlerDelegate {
 *     @Override
 *     public void onException(Exception ex, WebRequest request) {
 *         alertService.send("异常：" + ex.getMessage());
 *     }
 *     @Override
 *     public R<?> handleBusinessException(BusinessException ex, WebRequest request) {
 *         // 返回自定义响应，否则返回 null 使用默认
 *         return R.fail(1001, "业务异常自定义提示");
 *     }
 * }
 * }</pre>
 *
 * @author dream
 */
public interface ExceptionHandlerDelegate {

    /**
     * 异常发生时的回调（用于日志、告警、上报等副作用操作）。
     * 在正式处理异常之前调用，不影响最终响应。
     *
     * @param ex      异常
     * @param request 当前请求
     */
    default void onException(Exception ex, WebRequest request) {
        // 默认空实现
    }

    /**
     * 自定义处理业务异常
     *
     * @param ex      业务异常
     * @param request 当前请求
     * @return 统一响应对象，返回 {@code null} 表示使用默认处理
     */
    default R<?> handleBusinessException(com.dream.common.core.exception.BusinessException ex, WebRequest request) {
        return null;
    }

    /**
     * 自定义处理参数校验异常（包含 {@code @Valid} 和 {@code @RequestParam} 校验失败）
     *
     * @param ex      校验异常
     * @param request 当前请求
     * @return 统一响应对象，返回 {@code null} 表示使用默认处理
     */
    default R<?> handleValidationException(Exception ex, WebRequest request) {
        return null;
    }

    /**
     * 自定义处理 Servlet 异常（如文件上传超限、404 等）
     *
     * @param ex      Servlet 异常
     * @param request 当前请求
     * @return 统一响应对象，返回 {@code null} 表示使用默认处理
     */
    default R<?> handleServletException(ServletException ex, WebRequest request) {
        return null;
    }

    /**
     * 自定义处理其他未知异常
     *
     * @param ex      未知异常
     * @param request 当前请求
     * @return 统一响应对象，返回 {@code null} 表示使用默认处理
     */
    default R<?> handleUnknownException(Exception ex, WebRequest request) {
        return null;
    }
}