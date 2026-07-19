package com.dream.common.framework.config;

import com.dream.common.core.enums.SystemProperty;
import com.dream.common.core.utils.SystemUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 自定义启动 Banner 打印器
 * <p>
 * 在 Spring Boot 默认 Banner 之后，额外打印带 Dream 图案的自定义 Banner。
 * 通过 {@code spring.banner.custom.enabled=true} 控制是否启用（默认启用）。
 * </p>
 * <pre>
 *   ██████╗ ██████╗ ███████╗ █████╗ ███╗   ███╗
 *   ██╔══██╗██╔══██╗██╔════╝██╔══██╗████╗ ████║
 *   ██║  ██║██████╔╝█████╗  ███████║██╔████╔██║
 *   ██║  ██║██╔══██╗██╔══╝  ██╔══██║██║╚██╔╝██║
 *   ██████╔╝██║  ██║███████╗██║  ██║██║ ╚═╝ ██║
 *   ╚═════╝ ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚═╝     ╚═╝
 *
 *   ================================================================
 *   Dream Application v1.0.0 已启动成功！
 *   Profile   : dev
 *   HostName  : macbook-pro.local
 *   IP        : 192.168.1.100
 *   PID       : 12345
 *   System    : Mac OS X
 *   Started   : 2026-07-18 10:30:15
 *   ================================================================
 *   ❤  Powered by Dream Framework
 * </pre>
 *
 * @author dream
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "spring.banner.custom.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class CustomBannerRunner implements ApplicationRunner {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String DEFAULT_PROFILE = "default";
    private static final String DEFAULT_PORT = "8080";

    /**
     * Banner 内容宽度
     */
    private static final int CONTENT_WIDTH = 62;

    /**
     * Dream ASCII 图案
     */
    private static final String DREAM_ASCII =
            "  ██████╗ ██████╗ ███████╗ █████╗ ███╗   ███╗\n" +
                    "  ██╔══██╗██╔══██╗██╔════╝██╔══██╗████╗ ████║\n" +
                    "  ██║  ██║██████╔╝█████╗  ███████║██╔████╔██║\n" +
                    "  ██║  ██║██╔══██╗██╔══╝  ██╔══██║██║╚██╔╝██║\n" +
                    "  ██████╔╝██║  ██║███████╗██║  ██║██║ ╚═╝ ██║\n" +
                    "  ╚═════╝ ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚═╝     ╚═╝";

    /**
     * 分隔线
     */
    private static final String SEPARATOR_LINE = repeatChar('=', CONTENT_WIDTH);

    private final Environment environment;

    public CustomBannerRunner(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String appName = SystemProperty.DREAM_NAME.getProperty();
            String version = SystemProperty.DREAM_VERSION.getProperty();
            String profile = environment.getProperty("spring.profiles.active", DEFAULT_PROFILE);
            String port = environment.getProperty("server.port", DEFAULT_PORT);
            String hostname = SystemUtils.getHostName();
            String ip = SystemUtils.getHostIp();
            String pid = SystemUtils.getCurrentPidStr();
            String os = SystemProperty.OS_NAME.getProperty();
            String started = LocalDateTime.now().format(DATE_FORMATTER);
            String banner = buildBanner(appName, version, profile, hostname, ip, port, pid, os, started);
            log.info(banner);
        } catch (Exception e) {
            log.debug("Failed to print custom banner: {}", e.getMessage());
        }
    }

    /**
     * 构建 Banner 字符串
     */
    private String buildBanner(String appName, String version, String profile,
                               String hostname, String ip, String port, String pid,
                               String os, String started) {

        // 标题：🚀 appName vversion 已启动成功！
        String title = "  🚀 " + appName + "  (v" + version + ") ";

        // 信息行
        String profileLine = formatInfo("Profile", profile);
        String hostnameLine = formatInfo("HostName", hostname);
        String ipLine = formatInfo("IP", ip);
        String portLine = formatInfo("Port", port);
        String pidLine = formatInfo("PID", pid);
        String osLine = formatInfo("System", os);
        String startedLine = formatInfo("Started", started);

        // 底部
        String footer = "  ❤  Powered by Dream Framework";

        return "\n"
                + DREAM_ASCII + "\n"
                + "\n"
                + "  " + SEPARATOR_LINE + "\n"
                + title + "\n"
                + profileLine + "\n"
                + hostnameLine + "\n"
                + ipLine + "\n"
                + portLine + "\n"
                + pidLine + "\n"
                + osLine + "\n"
                + startedLine + "\n"
                + "  " + SEPARATOR_LINE + "\n"
                + footer + "\n";
    }

    /**
     * 格式化信息行（左对齐，固定宽度）
     *
     * @param label 标签
     * @param value 值
     * @return 格式化后的行
     */
    private String formatInfo(String label, String value) {
        String displayValue = value != null ? value : "unknown";
        String content = "  " + label + " : " + displayValue;
        int padding = CONTENT_WIDTH - content.length() + 2;
        padding = Math.max(padding, 0);
        return content + repeatChar(' ', padding);
    }

    /**
     * 重复字符 n 次
     */
    private static String repeatChar(char ch, int count) {
        if (count <= 0) {
            return "";
        }
        char[] chars = new char[count];
        for (int i = 0; i < count; i++) {
            chars[i] = ch;
        }
        return new String(chars);
    }
}