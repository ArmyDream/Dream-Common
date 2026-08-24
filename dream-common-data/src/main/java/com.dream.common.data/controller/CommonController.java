package com.dream.common.data.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
 * 使用 {@link R} 作为统一响应格式，异常时返回 {@link R#fail()} 系列失败响应。
 * 路径前缀由子类通过 {@link RequestMapping} 指定。
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * @RestController
 * @RequestMapping("/user")
 * public class UserController extends CommonController<UserService, User> {
 *     public UserController(UserService baseService) {
 *         super(baseService);
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
     * 将 {@code R<Void>} 适配为任意类型的 {@code R}。
     * <p>
     * 由于 {@link R#fail(String)}、{@link R#badRequest(String)} 等方法返回
     * {@code R<Void>}，而本类各接口的返回类型为 {@code R<T>}、{@code R<List<T>>} 等，
     * 此处通过泛型强转进行适配。{@link R} 内部使用 Map 存储数据，强转后调用
     * {@link R#getData()} 返回 {@code null}，符合失败响应无数据的语义。
     * </p>
     *
     * @param r   原始失败响应
     * @param <E> 目标数据类型
     * @return 适配后的失败响应
     */
    @SuppressWarnings("unchecked")
    private <E> R<E> adapt(R<Void> r) {
        return (R<E>) (R<?>) r;
    }

    // ==================== 基础查询接口 ====================

    /**
     * 分页查询。
     *
     * @param pageQuery 分页查询参数（含查询条件与排序字段）
     * @return 分页结果
     */
    @PostMapping("/page")
    public R<IPage<T>> page(@RequestBody CommonPageQuery<T> pageQuery) {
        try {
            IPage<T> page = baseService.findPage(pageQuery);
            return R.ok(page);
        } catch (Exception e) {
            log.error("分页查询失败", e);
            return adapt(R.fail("分页查询失败：" + e.getMessage()));
        }
    }

    /**
     * 列表查询。
     *
     * @param query 查询条件实体
     * @return 数据列表
     */
    @PostMapping("/list")
    public R<List<T>> list(@RequestBody T query) {
        try {
            List<T> list = baseService.findList(query);
            return R.ok(list);
        } catch (Exception e) {
            log.error("列表查询失败", e);
            return adapt(R.fail("列表查询失败：" + e.getMessage()));
        }
    }

    /**
     * 查询单个实体（根据条件返回首条记录）。
     *
     * @param query 查询条件实体
     * @return 单个实体；无匹配返回 {@code null}
     */
    @PostMapping("/one")
    public R<T> one(@RequestBody T query) {
        try {
            T entity = baseService.findOne(query);
            return R.ok(entity);
        } catch (Exception e) {
            log.error("查询单个实体失败", e);
            return adapt(R.fail("查询单个实体失败：" + e.getMessage()));
        }
    }

    /**
     * 根据 ID 查询实体。
     *
     * @param id 主键 ID
     * @return 实体；不存在返回 {@code null}
     */
    @GetMapping("/get/{id}")
    public R<T> getById(@PathVariable String id) {
        try {
            T entity = baseService.findById(id);
            return R.ok(entity);
        } catch (Exception e) {
            log.error("根据ID查询失败，ID：{}", id, e);
            return adapt(R.fail("根据ID查询失败：" + e.getMessage()));
        }
    }

    /**
     * 根据 ID 列表批量查询。
     *
     * @param ids ID列表
     * @return 实体列表
     */
    @PostMapping("/get/batch")
    public R<List<T>> getByIds(@RequestBody List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return adapt(R.badRequest("ID列表不能为空"));
        }
        try {
            List<T> list = baseService.listByIds(ids);
            return R.ok(list);
        } catch (Exception e) {
            log.error("批量查询失败", e);
            return adapt(R.fail("批量查询失败：" + e.getMessage()));
        }
    }

    // ==================== 新增接口 ====================

    /**
     * 新增实体。
     *
     * @param entity 实体对象
     * @return 新增后的实体（含已生成的主键）
     */
    @PostMapping("/add")
    public R<T> add(@RequestBody T entity) {
        try {
            T result = baseService.addEntity(entity);
            return R.ok("新增成功", result);
        } catch (Exception e) {
            log.error("新增失败", e);
            return adapt(R.fail("新增失败：" + e.getMessage()));
        }
    }

    /**
     * 批量新增实体。
     *
     * @param entityList 实体列表
     * @return 新增后的实体列表（含已生成的主键）
     */
    @PostMapping("/add/batch")
    public R<List<T>> addBatch(@RequestBody List<T> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return adapt(R.badRequest("数据列表不能为空"));
        }
        try {
            List<T> resultList = baseService.addBatch(entityList);
            return R.ok("批量新增成功", resultList);
        } catch (Exception e) {
            log.error("批量新增失败", e);
            return adapt(R.fail("批量新增失败：" + e.getMessage()));
        }
    }

    // ==================== 修改接口 ====================

    /**
     * 修改实体。
     *
     * @param entity 实体对象（必须包含主键 ID）
     * @return 修改后的实体
     */
    @PutMapping("/update")
    public R<T> update(@RequestBody T entity) {
        try {
            T result = baseService.updateEntity(entity);
            return R.ok("修改成功", result);
        } catch (Exception e) {
            log.error("修改失败", e);
            return adapt(R.fail("修改失败：" + e.getMessage()));
        }
    }

    // ==================== 新增或修改接口 ====================

    /**
     * 新增或修改实体（根据 ID 是否存在自动判断）。
     *
     * @param entity 实体对象
     * @return 保存后的实体
     */
    @PostMapping("/save")
    public R<T> save(@RequestBody T entity) {
        try {
            T result = baseService.saveOrUpdateEntity(entity);
            return R.ok("保存成功", result);
        } catch (Exception e) {
            log.error("保存失败", e);
            return adapt(R.fail("保存失败：" + e.getMessage()));
        }
    }

    // ==================== 删除接口 ====================

    /**
     * 根据 ID 删除。
     *
     * @param id 主键 ID
     * @return 是否删除成功
     */
    @DeleteMapping("/delete/{id}")
    public R<Boolean> deleteById(@PathVariable String id) {
        try {
            boolean success = baseService.removeById(id);
            return R.ok(success ? "删除成功" : "删除失败", success);
        } catch (Exception e) {
            log.error("删除失败，ID：{}", id, e);
            return adapt(R.fail("删除失败：" + e.getMessage()));
        }
    }

    /**
     * 批量删除。
     *
     * @param ids ID列表
     * @return 是否删除成功
     */
    @DeleteMapping("/delete/batch")
    public R<Boolean> deleteBatch(@RequestBody List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return adapt(R.badRequest("ID列表不能为空"));
        }
        try {
            boolean success = baseService.removeByIds(ids);
            return R.ok(success ? "批量删除成功" : "批量删除失败", success);
        } catch (Exception e) {
            log.error("批量删除失败", e);
            return adapt(R.fail("批量删除失败：" + e.getMessage()));
        }
    }

    // ==================== 数据完整性接口 ====================

    /**
     * 检查数据完整性（防篡改校验）。
     *
     * @param id 数据 ID
     * @return 是否完整（未被篡改）
     */
    @GetMapping("/check/integrity/{id}")
    public R<Boolean> checkIntegrity(@PathVariable String id) {
        try {
            boolean result = baseService.checkDataIntegrity(id);
            return R.ok(result);
        } catch (Exception e) {
            log.error("数据完整性检查失败，ID：{}", id, e);
            return adapt(R.fail("数据完整性检查失败：" + e.getMessage()));
        }
    }
}
