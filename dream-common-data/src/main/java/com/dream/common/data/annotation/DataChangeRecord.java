package com.dream.common.data.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据变更记录注解，标注于实体类上，声明该实体在新增/修改/删除时需记录字段级变更明细。
 * <p>
 * 记录内容写入 {@link com.dream.common.data.entity.DataChangeRecordEntity}，
 * 可选择仅记录日志或持久化入库。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Data
 * @TableName("sys_user")
 * @DataChangeRecord(enable = true, tableName = "sys_user", tableDesc = "用户表", persistent = true)
 * public class User extends BaseEntity {
 *     private String username;
 * }
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataChangeRecord {

    /**
     * 是否启用
     */
    boolean enable() default true;

    /**
     * 表名
     */
    String tableName() default "";

    /**
     * 表描述
     */
    String tableDesc() default "";

    /**
     * 是否入库
     */
    boolean persistent() default false;

    /**
     * 字段值写入变更记录前的截断长度，默认 2000。
     * <p>
     * 该值受落库列宽约束：{@code sys_data_change_record.old_value / new_value} 为 {@code VARCHAR(2000)}，
     * 调大必须同步放宽列宽，否则插入报 {@code Data too long for column}。
     * </p>
     */
    int maxValueLength() default 2000;

    /**
     * 不记录变更明细的审计字段名（按实体 Java 字段名匹配）。
     * <p>
     * 留空时使用框架默认的 {@code createBy / createTime / updateBy / updateTime}
     * （变更记录行自身已携带审计信息）；一旦显式配置则<b>整体替换</b>默认集合，
     * 想在此基础上增减就要把要保留的默认名一并写上。
     * </p>
     */
    String[] excludeAuditFields() default {};
}
