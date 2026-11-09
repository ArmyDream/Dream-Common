package com.dream.common.data.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据完整性注解，标注于实体类上，基于 MD5 校验实现数据防篡改。
 * <p>
 * 每张表使用单独字段存储全量数据的 MD5 值，新增/修改时自动计算并写入，
 * 查询时可校验数据是否被非法篡改。通常排除 MD5 字段本身、逻辑删除字段、乐观锁字段等。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Data
 * @TableName("sys_user")
 * @DataIntegrity(enable = true, md5Field = "data_md5", autoCalculate = true, throwOnFailure = true)
 * public class User extends BaseEntity {
 *     private String username;
 *     private String dataMd5;
 * }
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataIntegrity {

    /**
     * 是否启用
     */
    boolean enable() default true;

    /**
     * 需要排除的字段(多个用逗号分隔)
     * 通常排除MD5字段本身、逻辑删除字段、乐观锁字段等
     */
    String excludeFields() default "deleted,version,dataMd5";

    /**
     * MD5字段名，默认data_md5。
     * <p>既可填数据库列名（{@code data_md5}），也可填 Java 字段名（{@code dataMd5}），
     * 框架会解析为实体上真实存在的字段；两者都匹配不到时记录告警并跳过 MD5 处理。</p>
     */
    String md5Field() default "data_md5";

    /**
     * 新增/修改时是否自动计算并设置MD5值
     */
    boolean autoCalculate() default true;

    /**
     * 校验失败时是否抛出异常
     */
    boolean throwOnFailure() default false;
}



