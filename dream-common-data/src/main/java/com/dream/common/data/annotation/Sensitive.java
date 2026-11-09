package com.dream.common.data.annotation;


import com.dream.common.data.enums.SensitiveType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据脱敏注解，标注于实体字段上，声明该字段在接口返回时需进行脱敏处理。
 * <p>
 * 配合 {@link com.dream.common.data.enums.SensitiveType} 枚举使用，
 * 支持自定义脱敏规则与替换字符。
 * </p>
 * <p>
 * 执行时机：查询返回阶段由 {@code CommonServiceImpl#desensitizeEntity} 扫描并就地替换字段值，
 * 因此脱敏后的实体不可直接回写数据库（见 {@link #ignoreOnUpdate()}）。
 * 掩码算法统一由 core 模块 {@code DesensitizeUtils} 实现。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * public class User extends BaseEntity {
 *     @Sensitive(type = SensitiveType.MOBILE)
 *     private String phone;
 *
 *     @Sensitive(type = SensitiveType.ID_CARD)
 *     private String idCard;
 *
 *     @Sensitive(type = SensitiveType.CUSTOM, rule = "2-4", replaceChar = "#")
 *     private String licenseNo;
 * }
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sensitive {

    /**
     * 脱敏类型
     */
    SensitiveType type() default SensitiveType.CUSTOM;

    /**
     * 脱敏规则，如：3-4 表示前3后4保留，中间用替换字符填充
     * <p>仅 {@code type = SensitiveType.CUSTOM} 时生效，其余类型按内置固定规则处理。</p>
     */
    String rule() default "";

    /**
     * 替换字符
     */
    String replaceChar() default "*";

    /**
     * 更新时是否忽略该字段(防止将脱敏数据更新到数据库)
     * <p>
     * 为 {@code true}（默认）时，{@code updateEntity} 在写库前会识别「脱敏回显」：
     * 新值含 {@link #replaceChar()} 而原值不含时，判定为查询结果被原样回传，自动用原值覆盖，
     * 避免把 {@code 138****1234} 之类的掩码写库；客户端传入不含占位符的完整新值仍可正常更新。
     * 设为 {@code false} 则完全交给业务自行处理。
     * </p>
     */
    boolean ignoreOnUpdate() default true;
}
