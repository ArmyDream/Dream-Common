package com.dream.common.data.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dream.common.core.annotation.Log;
import com.dream.common.core.annotation.Permission;
import com.dream.common.core.config.ControllerConfig;
import com.dream.common.core.enums.OperationType;
import com.dream.common.core.enums.PermissionType;
import com.dream.common.core.exception.BusinessException;
import com.dream.common.core.utils.R;
import com.dream.common.data.entity.CommonPageQuery;
import com.dream.common.data.service.CommonService;
import com.dream.common.data.utils.MybatisPlusUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通用 Controller 基类。
 * <p>
 * 提供基础的 CRUD 接口（分页查询、列表查询、单个查询、根据 ID 查询、批量查询、
 * 新增、批量新增、修改、新增或修改、删除、批量删除、数据完整性校验），
 * 子类继承本类并指定 Service 与实体类型即可复用这些接口，无需重复编写。
 * </p>
 * <p>
 * 使用 {@link R} 作为统一响应格式。Controller 层<b>不捕获异常</b>，
 * 所有异常（业务异常、参数校验异常、唯一键冲突、系统异常等）统一上抛，
 * 由 {@code GlobalExceptionHandler} 全局处理器拦截并转换为标准失败响应，
 * 保证 HTTP 状态码语义正确（400/403/500 等）。
 * </p>
 *
 * <h2>权限控制</h2>

 * <p>
 * 权限解析规则（由 AOP 切面执行）：
 * <ul>
 *   <li>当 {@link Permission#value()} 不含冒号（如 {@code "list"}），完整权限 = {@code getPermissionPrefix() + ":" + value}</li>
 *   <li>当 {@link Permission#value()} 含冒号（如 {@code "system:user:list"}），直接作为完整权限使用</li>
 * </ul>
 * </p>
 *
 * <h2>操作日志</h2>
 * <p>
 * 各方法标注 {@link Log} 注解，声明操作类型（{@link OperationType}），
 * AOP 切面自动记录请求参数、响应结果、执行耗时等信息。
 * </p>
 * <p>
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/user")
 * public class UserController extends CommonController<UserService, User> {
 *
 *     public UserController(UserService baseService) {
 *         super(baseService);
 *     }
 *
 *     // 重写权限前缀，各方法权限将解析为 system:user:list / system:user:add / ...
 *     @Override
 *     protected ControllerConfig getControllerConfig() {
 *         return new ControllerConfig("system:user", "用户管理", true);
 *     }
 * }
 * }</pre>
 *
 * @param <S> Service 类型，必须继承 {@link CommonService}
 * @param <T> 实体类型
 * @author dream
 */
@Slf4j
@RestController
public abstract class CommonController<S extends CommonService<T>, T> {

    /**
     * 基础 Service，由子类通过构造器注入
     */
    protected final S baseService;

    /**
     * 实体类类型（懒加载缓存）
     */
    private Class<T> entityClass;

    /**
     * 构造通用 Controller。
     *
     * @param baseService 基础 Service，不能为 {@code null}
     */
    protected CommonController(S baseService) {
        this.baseService = baseService;
    }

    /**
     * 获取实体类类型（通过泛型解析，结果会缓存）。
     * <p>
     * 默认通过 {@link MybatisPlusUtils#getClassByType(Class, int)} 解析子类继承时声明的
     * 第二个泛型参数（即 {@code T}）。子类可重写此方法手动指定实体类型。
     * </p>
     *
     * @return 实体类类型
     * @throws IllegalStateException 当无法解析实体类型时抛出
     */
    @SuppressWarnings("unchecked")
    protected Class<T> getEntityClass() {
        if (entityClass == null) {
            entityClass = (Class<T>) MybatisPlusUtils.getClassByType(this.getClass(), 1);
            if (entityClass == null || entityClass == Object.class) {
                throw new IllegalStateException("无法解析实体类类型，请检查泛型参数或重写 getEntityClass() 方法");
            }
            log.debug("解析到实体类类型: {}", entityClass.getName());
        }
        return entityClass;
    }

    /**
     *  ControllerConfig 配置
     *
     * @return  ControllerConfig
     */
    protected ControllerConfig getControllerConfig() {
        return new ControllerConfig("", "", true);
    }

    // ==================== 基础查询接口 ====================

    /**
     * 分页查询。
     *
     * @param pageQuery 分页查询参数（含查询条件与排序字段）
     * @return 分页结果
     */
    @Permission(value = "list", type = PermissionType.LIST)
    @Log(operationType = OperationType.QUERY)
    @PostMapping("/page")
    public R<IPage<T>> page(@RequestBody CommonPageQuery<T> pageQuery) {
        return R.ok(baseService.findPage(pageQuery));
    }

    /**
     * 列表查询。
     *
     * @param query 查询条件实体
     * @return 数据列表
     */
    @Permission(value = "list", type = PermissionType.LIST)
    @Log(operationType = OperationType.QUERY, isSaveResponseData = false)
    @PostMapping("/list")
    public R<List<T>> list(@RequestBody T query) {
        return R.ok(baseService.findList(query));
    }

    /**
     * 查询单个实体（根据条件返回首条记录）。
     *
     * @param query 查询条件实体
     * @return 单个实体；无匹配返回 {@code null}
     */
    @Permission(value = "query", type = PermissionType.DETAIL)
    @Log(operationType = OperationType.QUERY)
    @PostMapping("/one")
    public R<T> one(@RequestBody T query) {
        return R.ok(baseService.findOne(query));
    }

    /**
     * 根据 ID 查询实体。
     *
     * @param id 主键 ID
     * @return 实体；不存在返回 {@code null}
     */
    @Permission(value = "query", type = PermissionType.DETAIL)
    @Log(operationType = OperationType.VIEW)
    @GetMapping("/get")
    public R<T> getById(@RequestParam String id) {
        return R.ok(baseService.findById(id));
    }

    /**
     * 根据 ID 列表批量查询。
     *
     * @param ids ID列表
     * @return 实体列表
     */
    @Permission(value = "query", type = PermissionType.DETAIL)
    @Log(operationType = OperationType.QUERY, isSaveResponseData = false)
    @PostMapping("/get/batch")
    public R<List<T>> getByIds(@RequestBody List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.badRequest("ID列表不能为空");
        }
        return R.ok(baseService.listByIds(ids));
    }

    // ==================== 新增接口 ====================

    /**
     * 新增实体。
     *
     * @param entity 实体对象
     * @return 新增后的实体（含已生成的主键）
     */
    @Permission(value = "add", type = PermissionType.ADD)
    @Log(operationType = OperationType.INSERT)
    @PostMapping("/add")
    public R<T> add(@RequestBody T entity) {
        return R.ok("新增成功", baseService.addEntity(entity));
    }

    /**
     * 批量新增实体。
     *
     * @param entityList 实体列表
     * @return 新增后的实体列表（含已生成的主键）
     */
    @Permission(value = "add", type = PermissionType.ADD)
    @Log(operationType = OperationType.INSERT, isSaveResponseData = false)
    @PostMapping("/add/batch")
    public R<List<T>> addBatch(@RequestBody List<T> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            throw BusinessException.badRequest("数据列表不能为空");
        }
        return R.ok("批量新增成功", baseService.addBatch(entityList));
    }

    // ==================== 修改接口 ====================

    /**
     * 修改实体。
     *
     * @param entity 实体对象（必须包含主键 ID）
     * @return 修改后的实体
     */
    @Permission(value = "edit", type = PermissionType.EDIT)
    @Log(operationType = OperationType.UPDATE)
    @PostMapping("/update")
    public R<T> update(@RequestBody T entity) {
        return R.ok("修改成功", baseService.updateEntity(entity));
    }

    // ==================== 新增或修改接口 ====================

    /**
     * 新增或修改实体（根据 ID 是否存在自动判断）。
     *
     * @param entity 实体对象
     * @return 保存后的实体
     */
    @Permission(value = {"add", "edit"}, logical = Permission.Logical.OR, type = PermissionType.EDIT)
    @Log(operationType = OperationType.UPDATE)
    @PostMapping("/save")
    public R<T> save(@RequestBody T entity) {
        return R.ok("保存成功", baseService.saveOrUpdateEntity(entity));
    }

    // ==================== 删除接口 ====================

    /**
     * 根据 ID 删除。
     *
     * @param id 主键 ID
     * @return 是否删除成功
     */
    @Permission(value = "delete", type = PermissionType.DELETE)
    @Log(operationType = OperationType.DELETE)
    @PostMapping("/delete")
    public R<Boolean> deleteById(@RequestParam String id) {
        boolean success = baseService.removeById(id);
        return R.ok(success ? "删除成功" : "删除失败", success);
    }

    /**
     * 批量删除。
     *
     * @param ids ID列表
     * @return 是否删除成功
     */
    @Permission(value = "delete", type = PermissionType.DELETE)
    @Log(operationType = OperationType.DELETE)
    @PostMapping("/delete/batch")
    public R<Boolean> deleteBatch(@RequestBody List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.badRequest("ID列表不能为空");
        }
        boolean success = baseService.removeByIds(ids);
        return R.ok(success ? "批量删除成功" : "批量删除失败", success);
    }

    // ==================== 数据完整性接口 ====================

    /**
     * 检查数据完整性（防篡改校验）。
     *
     * @param id 数据 ID
     * @return 是否完整（未被篡改）
     */
    @Permission(value = "query", type = PermissionType.DETAIL)
    @Log(operationType = OperationType.VIEW)
    @GetMapping("/check/integrity")
    public R<Boolean> checkIntegrity(@RequestParam String id) {
        return R.ok(baseService.checkDataIntegrity(id));
    }
}
