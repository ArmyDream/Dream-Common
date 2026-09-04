package com.dream.common.core.api;

import com.dream.common.core.exception.BusinessException;
import com.dream.common.core.utils.R;

/**
 * 全局异常处理委托接口。
 * <p>
 * 业务方可实现此接口以自定义异常处理逻辑（如告警、上报、审计等）。
 * 框架不强制实现，未注册时使用默认处理。
 * </p>
 *
 * <h2>纯 Java 设计说明</h2>
 * <p>
 * 本接口位于 {@code dream-common-core} 模块，刻意不依赖 Spring Web、Servlet API、
 * Validation API 等任何第三方库。所有「请求上下文」参数使用 {@link Object} 类型，
 * 下游模块（如 dream-common-advice）会传入具体的 {@code WebRequest / HttpServletRequest}
 * 等对象，业务实现可按需强转使用：
 * <pre>{@code
 * if (request instanceof WebRequest) {
 *     String uri = ((WebRequest) request).getAttribute(FORWARD_URI_SCOPE_REQUEST);
 *     ...
 * }
 * }</pre>
 * 同样地，参数校验 / Servlet 等异常也以 {@link Exception} 形式传参，业务实现
 * 可在方法内部自行 {@code instanceof} 判断具体类型。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Component
 * public class MyExceptionHandler implements ExceptionHandlerDelegate {
 *     @Override
 *     public void onException(Exception ex, Object request) {
 *         alertService.send("异常：" + ex.getMessage());
 *     }
 *     @Override
 *     public R<?> handleBusinessException(BusinessException ex, Object request) {
 *         // 返回自定义响应，否则返回 null 使用默认处理
 *         return R.fail(1001, "业务异常自定义提示");
 *     }
 * }
 * }</pre>
 *
 * @author dream
 * @since 2026-08-24
 */
public interface ExceptionHandlerDelegate {

    /**
     * 异常发生时的回调（用于日志、告警、上报等副作用操作）。
     * 在正式处理异常之前调用，不影响最终响应。
     *
     * @param ex      异常
     * @param request 请求上下文（通常是 Spring Web 的 {@code WebRequest}，
     *                业务侧可 {@code instanceof} 判断具体类型后转换使用）
     *                可能为空
     */
    default void onException(Exception ex, Object request) {
        // 默认空实现
    }

    /**
     * 自定义处理业务异常
     *
     * @param ex      业务异常
     * @param request 请求上下文（通常是 Spring Web 的 {@code WebRequest}，
     *                业务侧可 {@code instanceof} 判断具体类型后转换使用）
     * @return 统一响应对象，返回 {@code null} 表示使用默认处理
     */
    default R<?> handleBusinessException(BusinessException ex, Object request) {
        return null;
    }

    /**
     * 自定义处理参数校验异常（包含 {@code @Valid} 绑定异常、{@code @RequestParam}
     * 校验失败、{@code ConstraintViolationException} 等）。
     * <p>
     * 由于本接口不依赖 Validation / Bind 相关类，具体类型在方法内部自行判断：
     * <ul>
     *   <li>{@code MethodArgumentNotValidException}</li>
     *   <li>{@code BindException}</li>
     *   <li>{@code ConstraintViolationException}</li>
     * </ul>
     * </p>
     *
     * @param ex      参数校验异常
     * @param request 请求上下文（通常是 Spring Web 的 {@code WebRequest}，
     *                业务侧可 {@code instanceof} 判断具体类型后转换使用）
     * @return 统一响应对象，返回 {@code null} 表示使用默认处理
     */
    default R<?> handleValidationException(Exception ex, Object request) {
        return null;
    }

    /**
     * 自定义处理 Servlet 异常（如 {@code ServletException}、文件上传超限、
     * 404 NoHandlerFound 等）。
     * <p>
     * 由于本接口不依赖 Servlet API，具体类型在方法内部使用
     * {@code ex instanceof javax.servlet.ServletException} 自行判断。
     * </p>
     *
     * @param ex      Servlet 异常
     * @param request 请求上下文（通常是 Spring Web 的 {@code WebRequest}，
     *                业务侧可 {@code instanceof} 判断具体类型后转换使用）
     * @return 统一响应对象，返回 {@code null} 表示使用默认处理
     */
    default R<?> handleServletException(Exception ex, Object request) {
        return null;
    }

    /**
     * 自定义处理其他未知异常
     *
     * @param ex      未知异常
     * @param request 请求上下文（通常是 Spring Web 的 {@code WebRequest}，
     *                业务侧可 {@code instanceof} 判断具体类型后转换使用）
     * @return 统一响应对象，返回 {@code null} 表示使用默认处理
     */
    default R<?> handleUnknownException(Exception ex, Object request) {
        return null;
    }
}
