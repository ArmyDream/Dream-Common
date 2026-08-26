package com.dream.common.core.annotation;

import com.dream.common.core.enums.PermissionType;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * <p>
 * 标注在 Controller 方法上，用于声明访问该接口所需的权限标识。
 * 框架通过 AOP 切面（或 Sa-Token/Stomp 等权限框架）拦截并校验当前登录用户是否拥有对应权限，
 * 无权限时抛出异常并返回 403。
 * </p>
 *
 * <h2>权限标识规范</h2>
 * <p>
 * 采用 {@code 模块:实体:操作} 三段式命名，全小写，冒号分隔。例如：
 * </p>
 * <ul>
 *   <li>{@code system:user:list} —— 系统模块-用户-列表查询</li>
 *   <li>{@code system:user:add} —— 系统模块-用户-新增</li>
 *   <li>{@code system:role:edit} —— 系统模块-角色-修改</li>
 *   <li>{@code monitor:online:force} —— 监控模块-在线用户-强退</li>
 * </ul>
 *
 * <h2>多权限校验</h2>
 * <p>
 * 当 {@link #value()} 配置多个权限标识时，通过 {@link #logical()} 指定逻辑关系：
 * </p>
 * <ul>
 *   <li>{@link Logical#AND}：必须拥有全部权限才允许访问（默认）</li>
 *   <li>{@link Logical#OR}：拥有任意一个权限即允许访问</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 单个权限
 * @Permission("system:user:list")
 * @GetMapping("/list")
 * public R<List<User>> list() { ... }
 *
 * // 新增权限
 * @Permission("system:user:add")
 * @Log(title = "用户管理", operationType = OperationType.INSERT)
 * @PostMapping("/add")
 * public R<Void> add(@RequestBody User user) { ... }
 *
 * // 多权限（满足其一即可）
 * @Permission(value = {"system:user:edit", "system:user:add"}, logical = Logical.OR)
 * @PostMapping("/save")
 * public R<Void> save(@RequestBody User user) { ... }
 *
 * // 指定按钮权限类型（用于前端按钮权限渲染）
 * @Permission(value = "system:user:add", type = PermissionType.ADD)
 * @PostMapping("/add")
 * public R<Void> add(@RequestBody User user) { ... }
 * }</pre>
 *
 * @author dream
 * @since 2026-08-24
 * @see Log
 * @see PermissionType
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Permission {

    /**
     * 权限标识数组
     * <p>
     * 采用 {@code 模块:实体:操作} 格式，如 {@code "system:user:list"}。
     * 配置多个时通过 {@link #logical()} 指定逻辑关系。
     * </p>
     *
     * @return 权限标识数组
     */
    String[] value();

    /**
     * 多权限之间的逻辑关系
     * <p>
     * 默认 {@link Logical#AND}，即必须拥有全部权限才允许访问。
     * </p>
     *
     * @return 逻辑运算符
     */
    Logical logical() default Logical.AND;

    /**
     * 权限按钮类型（可选）
     * <p>
     * 用于前端按钮权限渲染标识，不影响后端校验逻辑。
     * 留空表示不指定类型。
     * </p>
     *
     * @return 权限按钮类型
     */
    PermissionType type() default PermissionType.VIEW;

    /**
     * 多权限逻辑运算符
     * <p>
     * 定义当 {@link #value()} 配置多个权限标识时的校验逻辑。
     * </p>
     *
     * @author dream
     */
    enum Logical {
        /**
         * 与（默认）：必须拥有全部权限才允许访问
         */
        AND,
        /**
         * 或：拥有任意一个权限即允许访问
         */
        OR
    }
}
