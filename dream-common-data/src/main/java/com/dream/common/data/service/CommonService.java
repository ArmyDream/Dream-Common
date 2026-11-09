package com.dream.common.data.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.dream.common.data.entity.CommonPageQuery;

import java.util.List;

/**
 * 通用 Service 接口，在 MyBatis-Plus {@link IService} 基础上扩展通用业务方法。
 * <p>
 * 提供基于 {@link com.dream.common.data.annotation.QueryCondition} 的动态条件查询、
 * 分页查询、带审计与加解密脱敏的增删改查、数据完整性校验等能力。
 * 实现类参考 {@link com.dream.common.data.service.impl.CommonServiceImpl}。
 * </p>
 * <p>自定义 {@code findXxx/addXxx/updateEntity/saveOrUpdateEntity} 与已重写的标准实体 CRUD
 * 共用加密、脱敏、MD5 和变更记录链路。任意 Wrapper 更新、链式更新（含 Kotlin 链式 API）以及
 * {@code listMaps/pageMaps/listObjs/getMap/getObj} 等投影仍是 MyBatis-Plus 原始入口：
 * 它们没有逐行实体上下文，不承诺自动 MD5、字段级变更记录或实体加解密，敏感业务应使用实体入口。</p>
 * <p>实体写入会原地写入密文、MD5 与自动填充值；{@code addBatch} 返回输入列表且不回查，
 * 布尔型标准写入也不回查。业务后置钩子在事务提交前执行，只有变更记录分发延迟到提交后。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * public interface UserService extends CommonService<User> {
 *     // 扩展业务方法
 * }
 *
 * // 调用分页查询
 * CommonPageQuery<UserQuery> pageQuery = new CommonPageQuery<>();
 * pageQuery.setParams(new UserQuery().setUsername("dream"));
 * IPage<User> page = userService.findPage(pageQuery);
 * }</pre>
 *
 * @author dream
 * @param <T> 实体类型
 * @since 2026-08-21
 */
public interface CommonService<T> extends IService<T> {

    /**
     * 根据条件查询单个实体。
     *
     * @param query 查询条件实体
     * @return 匹配到的单个实体，未匹配返回 null
     */
    T findOne(T query);

    /**
     * 根据条件查询列表。
     *
     * @param query 查询条件实体
     * @return 匹配到的实体列表
     */
    List<T> findList(T query);

    /**
     * 分页查询。
     *
     * @param pageQuery 分页查询参数（含业务查询条件）
     * @return 分页结果
     */
    IPage<T> findPage(CommonPageQuery<T> pageQuery);

    /**
     * 新增实体（带前置后置处理）。
     *
     * @param entity 待新增实体
     * @return 新增后的实体（含生成的主键等信息）
     */
    T addEntity(T entity);

    /**
     * 批量新增（带前置后置处理）。
     *
     * @param entityList 待新增实体列表
     * @return 新增后的实体列表
     */
    List<T> addBatch(List<T> entityList);

    /**
     * 修改实体（带前置后置处理）。
     *
     * @param entity 待修改实体
     * @return 修改后的实体
     */
    T updateEntity(T entity);

    /**
     * 新增或修改实体，根据 ID 是否存在决定新增或修改。
     *
     * @param entity 待保存实体
     * @return 保存后的实体
     */
    T saveOrUpdateEntity(T entity);

    /**
     * 根据 ID 查询（带加密解密、脱敏处理）。
     *
     * @param id 主键ID
     * @return 查询到的实体，未找到返回 null
     */
    T findById(String id);

    /**
     * 检查数据完整性（MD5 校验）。
     *
     * @param id 主键ID
     * @return true-数据完整或未启用校验；false-记录不存在、MD5 缺失、校验异常或数据已被篡改
     */
    boolean checkDataIntegrity(String id);

    /**
     * 构建查询 Wrapper。
     *
     * @param query 查询条件实体
     * @return 根据条件构建的 MyBatis-Plus 查询包装器
     */
    QueryWrapper<T> buildQueryWrapper(T query);

    /**
     * 获取实体类类型。
     *
     * @return 实体类的 Class 对象
     */
    Class<T> getEntityClassType();

    /**
     * 分页查询结果后处理（如脱敏、解密等）。
     *
     * @param page      分页结果
     * @param pageQuery 分页查询参数
     */
    void processPageResult(IPage<T> page, CommonPageQuery<T> pageQuery);
}
