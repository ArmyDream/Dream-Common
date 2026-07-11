package com.dream.common.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 校验注解
 * <p>
 * 用于 Excel 导入/导出时的字段校验，支持必填、格式、长度、范围等校验规则
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 基础必填
 * @ExcelValid(fieldName = "手机号", required = true)
 * private String phone;
 *
 * // 格式校验（手机号正则）
 * @ExcelValid(fieldName = "手机号", pattern = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
 * private String phone;
 *
 * // 长度限制
 * @ExcelValid(fieldName = "用户名", maxLength = 20, message = "用户名最多20个字符")
 * private String username;
 *
 * // 读取内容转换（字典值映射）
 * @ExcelValid(fieldName = "性别", readConverterExp = "0=男,1=女,2=未知")
 * private String sex;
 *
 * // 日期格式
 * @ExcelValid(fieldName = "出生日期", dateFormat = "yyyy-MM-dd")
 * private String birthday;
 *
 * // 数值范围
 * @ExcelValid(fieldName = "年龄", min = 0, max = 150)
 * private Integer age;
 * }</pre>
 *
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ExcelValid {

    /**
     * 字段名称（用于错误提示，如"手机号"）
     * <p>
     * 留空时使用字段属性名
     */
    String fieldName() default "";

    /**
     * 是否必填
     */
    boolean required() default true;

    /**
     * 校验失败提示信息
     * <p>
     * 支持占位符：{fieldName} 会被替换为 {@link #fieldName()} 的值
     * <p>
     * 默认根据校验类型自动组装提示
     */
    String message() default "";

    // ==================== 格式校验 ====================

    /**
     * 正则表达式（格式校验）
     * <p>
     * 示例：手机号 "^1[3-9]\\d{9}$"
     * <p>
     * 留空表示不做格式校验
     */
    String pattern() default "";

    /**
     * 格式校验失败提示（留空使用 {@link #message()} 或默认提示）
     */
    String patternMessage() default "";

    // ==================== 长度/范围约束 ====================

    /**
     * 最大长度（字符串）或最大值（数值，配合 {@link #max()} 使用）
     * <p>
     * 0 表示不限制
     */
    int maxLength() default 0;

    /**
     * 最小长度（字符串）
     * <p>
     * 0 表示不限制
     */
    int minLength() default 0;

    /**
     * 数值最小值（适用于 Integer/Long/Double 等数值类型）
     * <p>
     * Double.MIN_VALUE 表示不限制（默认）
     */
    double min() default Double.MIN_VALUE;

    /**
     * 数值最大值
     * <p>
     * Double.MAX_VALUE 表示不限制（默认）
     */
    double max() default Double.MAX_VALUE;

    // ==================== 格式转换 ====================

    /**
     * 读取内容转表达式（字典值映射）
     * <p>
     * 格式：key=value,key=value
     * <p>
     * 示例："0=男,1=女,2=未知"
     * <p>
     * 如果格式不正确或为空，不做转换
     */
    String readConverterExp() default "";

    /**
     * 日期格式（适用于日期类型字段）
     * <p>
     * 示例："yyyy-MM-dd"、"yyyy-MM-dd HH:mm:ss"
     * <p>
     * 留空表示不做日期格式校验
     */
    String dateFormat() default "";

    /**
     * 数字格式（适用于金额等需要格式化的数值）
     * <p>
     * 示例："0.00"（保留两位小数）、"#,##0.00"（千分位）
     * <p>
     * 留空表示不做数字格式化
     */
    String numberFormat() default "";

    // ==================== 其他 ====================

    /**
     * 分组校验（不同场景启用不同校验规则）
     * <p>
     * 示例：import 时必填，export 时不必填
     * <p>
     * 留空表示所有场景都校验
     */
    String[] groups() default {};

    /**
     * 是否唯一（该列的值不允许重复）
     */
    boolean unique() default false;

    /**
     * 唯一性校验失败提示
     */
    String uniqueMessage() default "";
}

