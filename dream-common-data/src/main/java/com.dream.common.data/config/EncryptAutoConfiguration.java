package com.dream.common.data.config;

import com.dream.common.data.utils.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 加密密钥注入自动装配。
 * <p>
 * 读取 {@code dream.encrypt.*} 配置并注入 {@link EncryptUtil}。
 * {@link EncryptUtil} 不提供任何内置默认密钥/默认盐：未配置对应项时，
 * 首次使用该算法会抛出 {@link IllegalStateException}（fail-closed），
 * 而不是静默使用一个人人皆知的常量。
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
                properties.getDesKey(),
                properties.getMd5Salt());
        log.debug("EncryptAutoConfiguration initialized (dream.encrypt.*)");
    }
}