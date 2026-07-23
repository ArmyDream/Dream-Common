package com.dream.common.advice.handler;

import com.dream.common.advice.api.ExceptionHandlerDelegate;
import com.dream.common.core.enums.ResponseCode;
import com.dream.common.core.exception.BusinessException;
import com.dream.common.core.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLSyntaxErrorException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * <p>
 * 统一捕获 Controller 层异常并转换为标准 {@link R} 格式。
 * 支持通过 {@link ExceptionHandlerDelegate} 扩展自定义逻辑。
 * </p>
 * <p>
 * 已覆盖的异常类型包括：
 * <ul>
 *   <li>业务异常（{@link BusinessException}）</li>
 *   <li>参数校验异常（{@link MethodArgumentNotValidException}、{@link BindException}）</li>
 *   <li>请求参数异常（缺失、类型不匹配、JSON 解析失败等）</li>
 *   <li>HTTP 协议异常（方法不支持、媒体类型不支持、404）</li>
 *   <li>文件上传超限（{@link MaxUploadSizeExceededException}）</li>
 *   <li>Servlet 异常（{@link ServletException}）</li>
 *   <li>通用非法参数（{@link IllegalArgumentException}）</li>
 *   <li>空指针（{@link NullPointerException}）</li>
 *   <li>IO 异常（{@link IOException}、{@link FileNotFoundException}）</li>
 *   <li>响应序列化异常（{@link HttpMessageNotWritableException}）</li>
 *   <li>其他未知异常（{@link Exception}）</li>
 * </ul>
 * </p>
 *
 * @author dream
 */
@Slf4j
@RestControllerAdvice
@ConditionalOnProperty(prefix = "dream.advice.exception", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GlobalExceptionHandler {

    private final ExceptionHandlerDelegate delegate;

    public GlobalExceptionHandler(@Nullable ExceptionHandlerDelegate delegate) {
        this.delegate = delegate;
    }

    // ==================== 业务异常 ====================

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<?> handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("业务异常: uri={}, code={}, msg={}",
                getRequestUri(request),
                ex.getResultCode() != null ? ex.getResultCode().getValue() : null,
                ex.getMessage());

        if (delegate != null) {
            delegate.onException(ex, request);
            R<?> result = delegate.handleBusinessException(ex, request);
            if (result != null) {
                return result;
            }
        }

        ResponseCode code = ex.getResultCode() != null ? ex.getResultCode() : ResponseCode.FAIL;
        return R.fail(code, ex.getMessage());
    }

    // ==================== 参数校验异常 ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request) {
        String message = extractValidationMessage(ex.getBindingResult().getFieldErrors());
        log.warn("参数校验失败: uri={}, msg={}", getRequestUri(request), message);

        if (delegate != null) {
            delegate.onException(ex, request);
            R<?> result = delegate.handleValidationException(ex, request);
            if (result != null) {
                return result;
            }
        }

        return R.fail(ResponseCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleBindException(BindException ex, WebRequest request) {
        String message = extractValidationMessage(ex.getBindingResult().getFieldErrors());
        log.warn("表单绑定校验失败: uri={}, msg={}", getRequestUri(request), message);

        if (delegate != null) {
            delegate.onException(ex, request);
            R<?> result = delegate.handleValidationException(ex, request);
            if (result != null) {
                return result;
            }
        }

        return R.fail(ResponseCode.BAD_REQUEST, message);
    }

    // ==================== 请求参数异常 ====================

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleMissingParamException(MissingServletRequestParameterException ex, WebRequest request) {
        log.warn("缺少必填参数: uri={}, param={}", getRequestUri(request), ex.getParameterName());
        return R.fail(ResponseCode.BAD_REQUEST, "缺少必填参数: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String message = "参数类型错误: " + ex.getName() + " 应为 " +
                (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "未知类型");
        log.warn("参数类型错误: uri={}, param={}", getRequestUri(request), ex.getName());
        return R.fail(ResponseCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleMessageNotReadableException(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("请求体解析失败: uri={}, msg={}", getRequestUri(request), ex.getMessage());
        return R.fail(ResponseCode.BAD_REQUEST, "请求体格式错误，请检查 JSON 格式");
    }

    // ==================== HTTP 协议异常 ====================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<?> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        log.warn("请求方法不支持: uri={}, method={}", getRequestUri(request), ex.getMethod());
        return R.fail(ResponseCode.METHOD_NOT_ALLOWED, "不支持 " + ex.getMethod() + " 请求方法");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public R<?> handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex, WebRequest request) {
        log.warn("Content-Type 不支持: uri={}, contentType={}", getRequestUri(request), ex.getContentType());
        return R.fail(ResponseCode.UNSUPPORTED_MEDIA_TYPE, "不支持的 Content-Type: " + ex.getContentType());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<?> handleNoHandlerFoundException(NoHandlerFoundException ex, WebRequest request) {
        log.warn("资源不存在: uri={}", ex.getRequestURL());
        return R.fail(ResponseCode.NOT_FOUND);
    }

    // ==================== 文件上传异常 ====================

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, WebRequest request) {
        log.warn("文件上传超限: uri={}, maxSize={}", getRequestUri(request), ex.getMaxUploadSize());
        return R.fail(ResponseCode.FILE_TOO_LARGE);
    }

    // ==================== Servlet 异常 ====================

    @ExceptionHandler(ServletException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<?> handleServletException(ServletException ex, WebRequest request) {
        log.error("Servlet 异常: uri={}", getRequestUri(request), ex);

        if (delegate != null) {
            delegate.onException(ex, request);
            R<?> result = delegate.handleServletException(ex, request);
            if (result != null) {
                return result;
            }
        }

        return R.fail(ResponseCode.INTERNAL_ERROR);
    }

    // ==================== 新增：通用非法参数异常 ====================

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("非法参数异常: uri={}, msg={}", getRequestUri(request), ex.getMessage());
        if (delegate != null) {
            delegate.onException(ex, request);
            R<?> result = delegate.handleUnknownException(ex, request);
            if (result != null) {
                return result;
            }
        }
        return R.fail(ResponseCode.BAD_REQUEST, "非法参数: " + ex.getMessage());
    }

    // ==================== 新增：空指针异常 ====================

    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<?> handleNullPointerException(NullPointerException ex, WebRequest request) {
        log.error("空指针异常: uri={}", getRequestUri(request), ex);
        if (delegate != null) {
            delegate.onException(ex, request);
            R<?> result = delegate.handleUnknownException(ex, request);
            if (result != null) {
                return result;
            }
        }
        return R.fail(ResponseCode.NULL_POINTER);
    }

    // ==================== 新增：IO 异常 ====================

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<?> handleIOException(IOException ex, WebRequest request) {
        log.error("IO异常: uri={}, msg={}", getRequestUri(request), ex.getMessage(), ex);
        if (delegate != null) {
            delegate.onException(ex, request);
            R<?> result = delegate.handleUnknownException(ex, request);
            if (result != null) {
                return result;
            }
        }
        return R.fail(ResponseCode.IO_ERROR);
    }

    @ExceptionHandler(FileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<?> handleFileNotFoundException(FileNotFoundException ex, WebRequest request) {
        log.warn("文件未找到: uri={}, msg={}", getRequestUri(request), ex.getMessage());
        if (delegate != null) {
            delegate.onException(ex, request);
            R<?> result = delegate.handleUnknownException(ex, request);
            if (result != null) {
                return result;
            }
        }
        return R.fail(ResponseCode.NOT_FOUND, "文件不存在: " + ex.getMessage());
    }

    // ==================== 新增：数据库相关异常 ====================

    @ExceptionHandler(SQLSyntaxErrorException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<?> handleSQLSyntaxErrorException(SQLSyntaxErrorException ex, WebRequest request) {
        log.error("SQL语法错误: uri={}, msg={}", getRequestUri(request), ex.getMessage(), ex);
        if (delegate != null) {
            delegate.onException(ex, request);
            R<?> result = delegate.handleUnknownException(ex, request);
            if (result != null) {
                return result;
            }
        }
        return R.fail(ResponseCode.SQL_ERROR);
    }

    // ==================== 新增：响应序列化异常 ====================

    @ExceptionHandler(HttpMessageNotWritableException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<?> handleHttpMessageNotWritableException(HttpMessageNotWritableException ex, WebRequest request) {
        log.error("响应序列化失败: uri={}, msg={}", getRequestUri(request), ex.getMessage(), ex);
        if (delegate != null) {
            delegate.onException(ex, request);
            R<?> result = delegate.handleUnknownException(ex, request);
            if (result != null) {
                return result;
            }
        }
        return R.fail(ResponseCode.SERIALIZE_ERROR);
    }

    // ==================== 兜底异常 ====================

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<?> handleException(Exception ex, WebRequest request) {
        // 判断是否为数据库唯一键冲突
        Throwable cause = ex.getCause();
        if (cause != null && cause.getMessage() != null
                && (cause.getMessage().contains("Duplicate entry") || cause.getMessage().contains("UNIQUE constraint"))) {
            log.warn("数据唯一键冲突: uri={}", getRequestUri(request));

            if (delegate != null) {
                delegate.onException(ex, request);
            }

            return R.fail(ResponseCode.CONFLICT, "数据重复，请检查唯一性约束");
        }

        log.error("系统未知异常: uri={}, type={}, msg={}",
                getRequestUri(request), ex.getClass().getName(), ex.getMessage(), ex);

        if (delegate != null) {
            delegate.onException(ex, request);
            R<?> result = delegate.handleUnknownException(ex, request);
            if (result != null) {
                return result;
            }
        }

        return R.fail(ResponseCode.SYSTEM_BUSY);
    }

    // ==================== 内部方法 ====================

    private String extractValidationMessage(List<FieldError> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) {
            return "参数校验失败";
        }
        return fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }

    private String getRequestUri(WebRequest request) {
        if (request instanceof NativeWebRequest) {
            HttpServletRequest httpReq = ((NativeWebRequest) request).getNativeRequest(HttpServletRequest.class);
            if (httpReq != null) {
                String uri = httpReq.getRequestURI();
                String qs = httpReq.getQueryString();
                return qs != null ? uri + "?" + qs : uri;
            }
        }
        if (request != null && request.getDescription(false) != null) {
            String desc = request.getDescription(false);
            return desc.startsWith("uri=") ? desc.substring(4) : desc;
        }
        return "unknown";
    }
}