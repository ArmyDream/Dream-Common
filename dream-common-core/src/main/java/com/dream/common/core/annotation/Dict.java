package com.dream.common.core.annotation;

import java.lang.annotation.*;

/**
 * 字典注解
 * <p>
 * 用于实体类字段的字典值转换，支持将代码值（code）转换为显示标签（label）。
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 基础用法
 * @Dict(mapping = "0:禁用,1:启用")
 * private String status;
 * private String statusLabel;
 *
 * // 使用外部字典（需实现 DictProvider）
 * @Dict(type = "sys_yes_no")
 * private String isDeleted;
 * private String isDeletedLabel;
 *
 * // 完整格式（含颜色、图标、禁用状态）
 * @Dict(mapping = "1:正常:check:green:0,2:异常:close:red:1,3:待审核:time:orange:0")
 * private String auditStatus;
 * private String auditStatusLabel;
 * private String auditStatusColor;
 * private String auditStatusIcon;
 * private Boolean auditStatusDisabled;
 *
 * // 树形字典（mapping 中第7位为 parentValue）
 * @Dict(mapping = "1:系统管理:folder:blue:0:0:,2:用户管理:user:green:0:0:1,3:菜单管理:menu:orange:0:0:1")
 * private String menuId;
 * private String menuName;
 * }</pre>
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Dict {

    /**
     * 字典类型标识
     * <p>
     * 用于关联外部字典数据（需实现 {@link com.dream.common.tool.dict.DictProvider}）
     */
    String type() default "";

    /**
     * 目标字段名（存放转换后的 label）
     * <p>
     * 默认规则：原字段名 + "Label"
     */
    String targetField() default "";

    /**
     * 目标颜色字段名
     * <p>
     * 默认：原字段名 + "Color"
     */
    String colorField() default "";

    /**
     * 目标图标字段名
     * <p>
     * 默认：原字段名 + "Icon"
     */
    String iconField() default "";

    /**
     * 目标禁用状态字段名
     * <p>
     * 默认：原字段名 + "Disabled"
     */
    String disabledField() default "";

    /**
     * 目标终止状态字段名
     * <p>
     * 默认：原字段名 + "Stop"
     */
    String stopField() default "";

    /**
     * 额外扩展字段名
     */
    String extraField() default "";

    /**
     * 字典映射字符串（硬编码方式）
     * <p>
     * 格式：value:label:icon:color:disabled:stop:parentValue:sort
     * <p>
     * 示例："1:系统管理:folder:blue:0:0:,2:用户管理:user:green:0:0:1"
     */
    String mapping() default "";

    /**
     * 是否开启转换
     */
    boolean enable() default true;

    /**
     * 是否驼峰命名目标字段
     */
    boolean hump() default true;

    /**
     * 是否包含颜色字段
     */
    boolean includeColor() default false;

    /**
     * 是否包含图标字段
     */
    boolean includeIcon() default false;

    /**
     * 是否包含禁用状态字段
     */
    boolean includeDisabled() default false;

    /**
     * 是否包含终止状态字段
     */
    boolean includeStop() default false;
}