package com.dream.common.data.config;

import com.dream.common.data.utils.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 加密密钥注入自动装配。
 * <p>
 * 读取 {@code dream.encrypt.*} 配置并注入 {@link EncryptUtil}，
 * 避免默认密钥硬编码风险。未配置时 {@link EncryptUtil} 回退到内置默认值并输出 WARN。
 * </p>
 *
 * @author dream
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EncryptProperties.class)
public class EncryptAutoConfiguration {

    /**
     * 构造时将配置密钥注入 {@link EncryptUtil}
     *
     * @param properties 加密配置
     */
    public EncryptAutoConfiguration(EncryptProperties properties) {
        if (properties == null) {
            return;
        }
        EncryptUtil.configure(
                properties.getAesKey(),
                properties.getAesIv(),
                properties.getDesKey(),
                properties.getDesIv(),
                properties.getMd5Salt());
        log.debug("EncryptAutoConfiguration initialized (dream.encrypt.*)");
    }
}