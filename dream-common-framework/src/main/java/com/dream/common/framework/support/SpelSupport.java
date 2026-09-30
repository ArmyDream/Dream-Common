package com.dream.common.framework.support;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.context.expression.MethodBasedEvaluationContext;

import java.lang.reflect.Method;

/**
 * SpEL 共享设施：解析器、参数名发现器与方法上下文构造。
 * <p>
 * 框架内多个切面（查询缓存、延时双删、防重复提交、操作日志）都要解析 SpEL 并按方法参数名
 * 求值，本类把这部分**无状态、可复用**的东西收在一处：
 * </p>
 * <ul>
 *   <li>{@link SpelExpressionParser} 线程安全且内置表达式缓存，全框架共用一个实例</li>
 *   <li>{@link DefaultParameterNameDiscoverer} 同理共用，避免每个切面各 new 一份</li>
 *   <li>{@link #newMethodContext} 统一"以方法参数名 + {@code #args} 求值"的上下文构造</li>
 * </ul>
 * <p>
 * <b>刻意不统一的部分</b>：求值上下文的安全级别。缓存类注解面向自家代码，用
 * {@link MethodBasedEvaluationContext}（能力完整）；防重复提交的 {@code spel} 可能出现在
 * 更开放的接口上，继续使用 {@code SimpleEvaluationContext} 只读绑定，不经由本类放宽。
 * </p>
 *
 * @author dream
 * @since 2026-09-29
 */
public final class SpelSupport {

    /** 全框架共用的 SpEL 解析器（线程安全，内置表达式缓存） */
    private static final ExpressionParser PARSER = new SpelExpressionParser();

    /** 全框架共用的方法参数名发现器（兼容 -parameters 与调试信息两种编译方式） */
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private SpelSupport() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 共用 SpEL 解析器
     *
     * @return 解析器实例
     */
    public static ExpressionParser getParser() {
        return PARSER;
    }

    /**
     * 共用方法参数名发现器
     *
     * @return 发现器实例
     */
    public static ParameterNameDiscoverer getNameDiscoverer() {
        return NAME_DISCOVERER;
    }

    /**
     * 构建"方法参数可直接按名字引用"的求值上下文，并额外注入 {@code #args} 数组。
     *
     * @param method 目标方法
     * @param args   方法实参
     * @return 求值上下文
     */
    public static MethodBasedEvaluationContext newMethodContext(Method method, Object[] args) {
        MethodBasedEvaluationContext context =
                new MethodBasedEvaluationContext(null, method, args, NAME_DISCOVERER);
        context.setVariable("args", args);
        return context;
    }
}
