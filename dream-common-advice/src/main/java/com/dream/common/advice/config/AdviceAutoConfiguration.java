package com.dream.common.advice.config;

import com.dream.common.advice.handler.GlobalExceptionHandler;
import com.dream.common.advice.handler.GlobalResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 全局异常与响应处理自动装配入口
 * <p>
 * 通过 {@link Import} 显式注册 {@link GlobalExceptionHandler}（全局异常处理器）
 * 与 {@link GlobalResponseHandler}（统一响应包装器），
 * 使本模块在业务方无 ComponentScan 覆盖 {@code com.dream.common.advice} 包时也能自动生效。
 * </p>
 * <p>
 * 两个 Handler 类上均带 {@code @ConditionalOnProperty}，可在 {@code dream.advice.*} 下关闭。
 * </p>
 *
 * @author dream
 */
@Slf4j
@Configuration
@Import({GlobalExceptionHandler.class, GlobalResponseHandler.class})
public class AdviceAutoConfiguration {

    /**
     * 启动日志
     */
    public AdviceAutoConfiguration() {
        log.info("AdviceAutoConfiguration initialized (dream.advice.*)");
    }
}