package com.dream.common.security.filter;

import com.dream.common.core.constant.Http;
import com.dream.common.security.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * 独立 CORS 跨域过滤器。
 * <p>
 * 在 Sa-Token 过滤器之前执行（{@link com.dream.common.core.constant.OrderValue#CORS_FILTER}），
 * 处理 OPTIONS 预检请求并注入 CORS 响应头。与鉴权职责严格分离。
 * </p>
 * <p>
 * 支持通过配置的 {@code allowedOriginPatterns}（Ant 风格）或 {@code allowedOrigins}
 * 精确匹配来决定是否允许请求源。
 * </p>
 *
 * @author dream
 * @since 2026-09-02
 */
@Slf4j
public class CorsFilter implements Filter {

    private final SecurityProperties.Cors cors;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 构造器注入 CORS 配置。
     *
     * @param cors CORS 配置对象
     */
    public CorsFilter(SecurityProperties.Cors cors) {
        this.cors = cors;
    }

    /**
     * 过滤器初始化，打印生效的 CORS 配置。
     *
     * @param filterConfig 过滤器配置
     */
    @Override
    public void init(FilterConfig filterConfig) {
        log.info("[CorsFilter] 初始化完成 | allowedOrigins={} | allowedOriginPatterns={} | allowCredentials={}",
                cors.getAllowedOrigins(), cors.getAllowedOriginPatterns(), cors.isAllowCredentials());
    }

    /**
     * 注入 CORS 响应头；OPTIONS 预检直接返回 200，其余放行。
     *
     * @param request  请求
     * @param response 响应
     * @param chain    过滤器链
     * @throws IOException      IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 1. 注入 CORS 响应头
        applyCorsHeaders(req, resp);

        // 2. OPTIONS 预检请求直接返回 200
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // 3. 其他请求放行
        chain.doFilter(request, response);
    }

    /**
     * 应用 CORS 响应头到当前响应。
     *
     * @param req  HTTP 请求
     * @param resp HTTP 响应
     */
    private void applyCorsHeaders(HttpServletRequest req, HttpServletResponse resp) {
        String origin = resolveOrigin(req);
        if (!StringUtils.hasText(origin)) {
            return; // 非跨域请求，跳过
        }

        String allowedOrigin = resolveAllowedOrigin(origin);
        if (allowedOrigin == null) {
            if (log.isDebugEnabled()) {
                log.debug("[CorsFilter] 拒绝不支持的 Origin: {}", origin);
            }
            return;
        }

        // 注入标准 CORS 头
        resp.setHeader(Http.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        resp.setHeader(Http.HEADER_ACCESS_CONTROL_ALLOW_METHODS, join(cors.getAllowedMethods()));
        resp.setHeader(Http.HEADER_ACCESS_CONTROL_ALLOW_HEADERS, join(cors.getAllowedHeaders()));
        resp.setHeader(Http.HEADER_ACCESS_CONTROL_MAX_AGE, String.valueOf(cors.getMaxAge()));

        if (cors.getExposedHeaders() != null && !cors.getExposedHeaders().isEmpty()) {
            resp.setHeader(Http.HEADER_ACCESS_CONTROL_EXPOSE_HEADERS, join(cors.getExposedHeaders()));
        }

        if (cors.isAllowCredentials()) {
            resp.setHeader(Http.HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        // 告知缓存服务器响应因 Origin 而异
        resp.setHeader("Vary", "Origin");
    }

    /**
     * 解析请求中的 Origin。
     * <p>优先使用 {@code Origin} 头，若不存在则回退到 {@code Referer} 头提取协议+主机+端口。</p>
     *
     * @param req HTTP 请求
     * @return 源字符串（如 {@code https://example.com}），若无法解析则返回 {@code null}
     */
    private static String resolveOrigin(HttpServletRequest req) {
        String origin = req.getHeader(Http.HEADER_ORIGIN);
        if (StringUtils.hasText(origin)) {
            return origin;
        }

        String referer = req.getHeader(Http.HEADER_REFERER);
        if (StringUtils.hasText(referer)) {
            try {
                URI uri = new URI(referer);
                String scheme = uri.getScheme();
                String host = uri.getHost();
                if (scheme != null && host != null) {
                    int port = uri.getPort();
                    if (port == -1) {
                        return scheme + "://" + host;
                    }
                    // 省略标准端口显示
                    if ((port == 80 && "http".equalsIgnoreCase(scheme)) ||
                            (port == 443 && "https".equalsIgnoreCase(scheme))) {
                        return scheme + "://" + host;
                    }
                    return scheme + "://" + host + ":" + port;
                }
            } catch (URISyntaxException ignored) {
                // 无效 Referer，忽略
            }
        }
        return null;
    }

    /**
     * 判断给定 Origin 是否被允许，并返回应写入响应头的值。
     *
     * @param origin 请求的 Origin
     * @return 允许时返回 origin 本身或 "*"；不允许返回 {@code null}
     */
    private String resolveAllowedOrigin(String origin) {
        // 优先使用 Ant 模式匹配
        List<String> patterns = cors.getAllowedOriginPatterns();
        if (patterns != null && !patterns.isEmpty()) {
            for (String pattern : patterns) {
                if (pathMatcher.match(pattern, origin)) {
                    return origin;
                }
            }
            return null;
        }

        // 精确匹配
        List<String> origins = cors.getAllowedOrigins();
        if (origins == null || origins.isEmpty()) {
            return null;
        }

        for (String allowed : origins) {
            if ("*".equals(allowed)) {
                // allowCredentials=true 时不能使用 "*"，必须回显 Origin
                return cors.isAllowCredentials() ? origin : "*";
            }
            if (allowed.equalsIgnoreCase(origin)) {
                return origin;
            }
        }
        return null;
    }

    /**
     * 将字符串列表拼接为逗号分隔的字符串。
     *
     * @param list 字符串列表
     * @return 拼接结果，若列表为空则返回空字符串
     */
    private static String join(List<String> list) {
        return list == null ? "" : String.join(",", list);
    }

    /**
     * 过滤器销毁回调，无需额外清理。
     */
    @Override
    public void destroy() {
        // 无需额外清理
    }
}
