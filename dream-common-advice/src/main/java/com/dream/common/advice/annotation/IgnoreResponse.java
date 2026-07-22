package com.dream.common.advice.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注该注解后，该接口的响应将<strong>不进行</strong>统一包装。
 * <p>可标注在类或方法上：</p>
 * <ul>
 *   <li>类级别：该 Controller 所有方法均忽略包装</li>
 *   <li>方法级别：仅当前方法忽略包装</li>
 * </ul>
 * 适用场景：文件下载、纯文本回调、Swagger 文档端点等。
 *
 * @author dream
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreResponse {
}