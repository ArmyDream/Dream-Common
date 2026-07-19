package com.dream.common.framework.utils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Spring 上下文工具类
 * <p>
 * 持有 {@link ApplicationContext} 和 {@link Environment} 的静态引用，
 * 提供在任意位置获取 Spring Bean、发布事件、读取配置的能力，无需注入即可使用。
 * </p>
 * <p>
 * <b>注意：</b>本类依赖 Spring 容器管理，使用时请确保 Spring 已启动完成。
 * 对于非 Spring 管理的类（如 new 创建的对象），可使用本类获取 Spring Bean。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>
 * // 1. 获取 Bean
 * UserService userService = SpringUtils.getBean(UserService.class);
 * DataSource ds = SpringUtils.getBean("dataSource", DataSource.class);
 *
 * // 2. 判断 Bean 是否存在
 * boolean exists = SpringUtils.containsBean("myService");
 *
 * // 3. 发布事件
 * SpringUtils.publishEvent(new MyEvent(source));
 * SpringUtils.publishEvent(someObject);  // Spring 4.2+
 *
 * // 4. 读取配置
 * String profile = SpringUtils.getProperty("spring.profiles.active");
 * int port = SpringUtils.getProperty("server.port", Integer.class, 8080);
 *
 * // 5. 环境判断
 * boolean isDev = SpringUtils.isDev();
 * boolean isMicro = SpringUtils.isMicro();
 *
 * // 6. 获取应用信息
 * String appName = SpringUtils.getApplicationName();
 * String activeProfile = SpringUtils.getActiveProfile();
 * </pre>
 *
 * @author dream
 * @see ApplicationContext
 * @see Environment
 */
@Slf4j
@Component
@Lazy(false)
@SuppressWarnings({"unused", "WeakerAccess"})
public final class SpringUtils implements ApplicationContextAware, EnvironmentAware, DisposableBean {

    /** Spring 应用上下文
     * -- GETTER --
     *  获取 ApplicationContext
     *
     * @return ApplicationContext；未初始化时返回 null
     */
    @Getter
    private static ApplicationContext applicationContext;

    /** Spring 环境配置
     * -- GETTER --
     *  获取 Environment
     *
     * @return Environment；未初始化时返回 null
     */
    @Getter
    private static Environment environment;

    // ==================== 私有构造器 ====================
    /**
     * 无参构造器（供 Spring 实例化）
     */
    public SpringUtils() {
    }

    // ==================== Aware 回调 ====================

    /**
     * 实现 {@link ApplicationContextAware} 接口，注入 ApplicationContext 到静态变量
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        SpringUtils.applicationContext = applicationContext;
        log.info("SpringUtils initialized, ApplicationContext: {}", applicationContext.getApplicationName());
    }

    /**
     * 实现 {@link EnvironmentAware} 接口，注入 Environment 到静态变量
     */
    @Override
    public void setEnvironment(@NonNull Environment environment) {
        SpringUtils.environment = environment;
    }

    /**
     * 实现 {@link DisposableBean} 接口，容器关闭时清理静态引用
     */
    @Override
    public void destroy() {
        log.debug("SpringUtils clearing ApplicationContext");
        SpringUtils.applicationContext = null;
        SpringUtils.environment = null;
    }

    // ==================== 初始化状态 ====================

    /**
     * 判断 ApplicationContext 是否已初始化
     *
     * @return true=已初始化
     */
    public static boolean isInitialized() {
        return applicationContext != null;
    }

    // ==================== Bean 获取 ====================

    /**
     * 根据 Bean 名称获取 Bean
     *
     * @param name Bean 名称
     * @param <T>  Bean 类型（自动推断）
     * @return Bean 实例
     * @throws IllegalStateException 如果 ApplicationContext 未初始化
     * @throws org.springframework.beans.BeansException 获取失败时抛出
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        checkContext();
        return (T) applicationContext.getBean(name);
    }

    /**
     * 根据 Bean 名称和类型获取 Bean
     *
     * @param name  Bean 名称
     * @param clazz Bean 类型
     * @param <T>   Bean 类型
     * @return Bean 实例
     * @throws IllegalStateException 如果 ApplicationContext 未初始化
     * @throws org.springframework.beans.BeansException 获取失败时抛出
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        checkContext();
        return applicationContext.getBean(name, clazz);
    }

    /**
     * 根据类型获取 Bean
     *
     * @param clazz Bean 类型
     * @param <T>   Bean 类型
     * @return Bean 实例
     * @throws IllegalStateException 如果 ApplicationContext 未初始化
     * @throws org.springframework.beans.BeansException 获取失败时抛出
     */
    public static <T> T getBean(Class<T> clazz) {
        checkContext();
        return applicationContext.getBean(clazz);
    }

    /**
     * 判断是否包含指定名称的 Bean
     *
     * @param name Bean 名称
     * @return true=存在；ApplicationContext 未初始化返回 false
     */
    public static boolean containsBean(String name) {
        if (applicationContext == null) {
            return false;
        }
        return applicationContext.containsBean(name);
    }

    /**
     * 判断指定类型的 Bean 是否存在
     *
     * @param clazz Bean 类型
     * @return true=存在；ApplicationContext 未初始化返回 false
     */
    public static boolean containsBean(Class<?> clazz) {
        if (applicationContext == null) {
            return false;
        }
        return applicationContext.getBeanNamesForType(clazz).length > 0;
    }

    /**
     * 获取指定类型的所有 Bean（含子类）
     *
     * @param clazz Bean 类型
     * @param <T>   Bean 类型
     * @return Bean 名称 → Bean 实例的映射；未找到返回空 Map
     * @throws IllegalStateException 如果 ApplicationContext 未初始化
     */
    public static <T> java.util.Map<String, T> getBeansOfType(Class<T> clazz) {
        checkContext();
        return applicationContext.getBeansOfType(clazz);
    }

    /**
     * 判断 Bean 是否为单例
     *
     * @param name Bean 名称
     * @return true=单例；ApplicationContext 未初始化返回 false
     */
    public static boolean isSingleton(String name) {
        if (applicationContext == null) {
            return false;
        }
        return applicationContext.isSingleton(name);
    }

    /**
     * 获取 Bean 的类型
     *
     * @param name Bean 名称
     * @return Bean 类型；未找到返回 null
     */
    public static Class<?> getBeanType(String name) {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getType(name);
    }

    // ==================== 事件发布 ====================

    /**
     * 发布 Spring 应用事件
     *
     * @param event 应用事件
     */
    public static void publishEvent(ApplicationEvent event) {
        if (applicationContext == null) {
            log.warn("publishEvent: ApplicationContext 未初始化，事件未发布");
            return;
        }
        applicationContext.publishEvent(event);
        if (log.isDebugEnabled()) {
            log.debug("publishEvent: {}", event);
        }
    }

    /**
     * 发布任意对象事件（Spring 4.2+ 支持，自动包装为 PayloadApplicationEvent）
     *
     * @param event 事件对象
     */
    public static void publishEvent(Object event) {
        if (applicationContext == null) {
            log.warn("publishEvent: ApplicationContext 未初始化，事件未发布");
            return;
        }
        applicationContext.publishEvent(event);
        if (log.isDebugEnabled()) {
            log.debug("publishEvent: {}", event);
        }
    }

    // ==================== 配置读取 ====================

    /**
     * 读取配置属性（字符串）
     *
     * @param key 配置键，如 {@code spring.datasource.url}
     * @return 配置值；不存在或 Environment 未初始化返回 null
     */
    public static String getProperty(String key) {
        if (environment == null) {
            return null;
        }
        return environment.getProperty(key);
    }

    /**
     * 读取配置属性（带默认值）
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值；不存在或 Environment 未初始化返回默认值
     */
    public static String getProperty(String key, String defaultValue) {
        if (environment == null) {
            return defaultValue;
        }
        return environment.getProperty(key, defaultValue);
    }

    /**
     * 读取配置属性（指定类型）
     *
     * @param key   配置键
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 配置值；不存在或 Environment 未初始化返回 null
     */
    public static <T> T getProperty(String key, Class<T> clazz) {
        if (environment == null) {
            return null;
        }
        return environment.getProperty(key, clazz);
    }

    /**
     * 读取配置属性（指定类型，带默认值）
     *
     * @param key          配置键
     * @param clazz        目标类型
     * @param defaultValue 默认值
     * @param <T>          目标类型
     * @return 配置值；不存在或 Environment 未初始化返回默认值
     */
    public static <T> T getProperty(String key, Class<T> clazz, T defaultValue) {
        if (environment == null) {
            return defaultValue;
        }
        return environment.getProperty(key, clazz, defaultValue);
    }

    /**
     * 读取必需的配置属性（不存在时抛出异常）
     *
     * @param key   配置键
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 配置值
     * @throws IllegalStateException 如果 Environment 未初始化或配置不存在
     */
    public static <T> T getRequiredProperty(String key, Class<T> clazz) {
        if (environment == null) {
            throw new IllegalStateException("Environment is not initialized");
        }
        return environment.getRequiredProperty(key, clazz);
    }

    // ==================== 应用信息 ====================

    /**
     * 获取应用名称（spring.application.name）
     *
     * @return 应用名称；未初始化时返回空字符串
     */
    public static String getApplicationName() {
        if (applicationContext == null) {
            return "";
        }
        return applicationContext.getApplicationName();
    }

    /**
     * 获取当前激活的 Profile（第一个）
     *
     * @return Profile 名称；未激活时返回 "default"
     */
    public static String getActiveProfile() {
        if (environment == null) {
            return "unknown";
        }
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length > 0) {
            return profiles[0];
        }
        String[] defaultProfiles = environment.getDefaultProfiles();
        return defaultProfiles.length > 0 ? defaultProfiles[0] : "default";
    }

    /**
     * 获取所有激活的 Profile
     *
     * @return Profile 数组；未初始化时返回空数组
     */
    public static String[] getActiveProfiles() {
        if (environment == null) {
            return new String[0];
        }
        return environment.getActiveProfiles();
    }

    /**
     * 判断指定 Profile 是否激活
     *
     * @param profile Profile 名称
     * @return true=已激活
     */
    public static boolean hasProfile(String profile) {
        if (environment == null || profile == null) {
            return false;
        }
        return environment.acceptsProfiles(profile);
    }

    /**
     * 判断是否为开发环境（dev）
     *
     * @return true=开发环境
     */
    public static boolean isDev() {
        return hasProfile("dev");
    }

    /**
     * 判断是否为测试环境（test）
     *
     * @return true=测试环境
     */
    public static boolean isTest() {
        return hasProfile("test");
    }

    /**
     * 判断是否为生产环境（prod）
     *
     * @return true=生产环境
     */
    public static boolean isProd() {
        return hasProfile("prod");
    }

    /**
     * 判断是否为微服务环境
     * <p>
     * 通过检查 Spring Cloud Nacos Discovery 是否启用来判断
     * </p>
     *
     * @return true=微服务环境；Environment 未初始化时返回 true（保守默认）
     */
    public static boolean isMicro() {
        if (environment == null) {
            log.warn("isMicro: Environment is null, default to true");
            return true;
        }
        return environment.getProperty("spring.cloud.nacos.discovery.enabled", Boolean.class, true);
    }

    /**
     * 判断是否为微服务环境（带默认值）
     *
     * @param defaultValue 默认值
     * @return true=微服务环境
     */
    public static boolean isMicro(boolean defaultValue) {
        if (environment == null) {
            log.warn("isMicro: Environment is null, using default: {}", defaultValue);
            return defaultValue;
        }
        return environment.getProperty("spring.cloud.nacos.discovery.enabled", Boolean.class, defaultValue);
    }

    // ==================== 缓存清理 ====================

    /**
     * 清除持有的静态引用（仅供测试或容器销毁时使用）
     */
    public static void clear() {
        if (log.isDebugEnabled()) {
            log.debug("Clearing SpringUtils cache");
        }
        applicationContext = null;
        environment = null;
    }

    // ==================== 内部方法 ====================

    /**
     * 检查 ApplicationContext 是否已初始化
     *
     * @throws IllegalStateException 未初始化时抛出
     */
    private static void checkContext() {
        if (applicationContext == null) {
            throw new IllegalStateException(
                    "ApplicationContext 未初始化，请确保 SpringUtils 已被 Spring 容器管理"
            );
        }
    }
}