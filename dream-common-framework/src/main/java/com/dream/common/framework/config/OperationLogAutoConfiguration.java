package com.dream.common.framework.config;

import com.dream.common.framework.aspect.OperationLogAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 操作日志自动装配。
 * <p>
 * 通过 {@link Import} 注册 {@link OperationLogAspect}，使 {@code @Log} 注解在业务方未
 * ComponentScan 覆盖 {@code com.dream.common.framework} 包时也能生效。
 * </p>
 * <p>
 * 切面开关为 {@code dream.framework.operation-log.enabled}（默认开启）；
 * 落库/上报由业务方注册 {@link com.dream.common.framework.api.OperationLogHandler} Bean 接管。
 * </p>
 *
 * @author dream
 */
@Slf4j
@Configuration
@Import(OperationLogAspect.class)
public class OperationLogAutoConfiguration {

    /**
     * 启动日志
     */
    public OperationLogAutoConfiguration() {
        log.info("OperationLogAutoConfiguration initialized (@Log 操作日志切面已启用)");
    }
}
