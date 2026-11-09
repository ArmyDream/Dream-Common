package com.dream.common.data.service.impl;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;

import com.dream.common.core.annotation.Dict;
import com.dream.common.core.enums.OperationType;
import com.dream.common.core.exception.BusinessException;
import com.dream.common.data.annotation.DataChangeRecord;
import com.dream.common.data.annotation.DataIntegrity;
import com.dream.common.data.annotation.Encrypt;
import com.dream.common.data.annotation.QueryCondition;
import com.dream.common.data.annotation.Sensitive;
import com.dream.common.data.api.DataChangeRecordHandler;
import com.dream.common.data.entity.CommonPageQuery;
import com.dream.common.data.entity.CommonQuery;
import com.dream.common.data.entity.DataChangeRecordEntity;
import com.dream.common.data.enums.EncryptType;
import com.dream.common.data.enums.QueryType;
import com.dream.common.data.mapper.CommonMapper;
import com.dream.common.data.service.CommonService;
import com.dream.common.data.utils.EncryptUtil;
import com.dream.common.data.utils.MybatisPlusUtils;
import com.dream.common.data.utils.SensitiveUtil;
import com.dream.common.tool.utils.dict.DictItem;
import com.dream.common.tool.utils.dict.DictUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 抽象通用 Service 实现类。
 * <p>
 * 基于 MyBatis-Plus 的 {@link ServiceImpl}，封装了通用的 CRUD、字段加密/解密、脱敏、
 * 字典转换、数据完整性校验、数据变更记录等能力。子类需要继承本类并指定
 * Mapper 类型 {@code M}（必须继承 {@link CommonMapper}）与实体类型 {@code T}。
 * </p>
 *
 * <p>主要能力：</p>
 * <ul>
 *   <li>查询：条件查询、分页查询，自动根据 {@link QueryCondition} 注解构建 QueryWrapper</li>
 *   <li>写入：新增/修改/批量新增，支持字段加密、MD5 完整性自动计算</li>
 *   <li>读取后处理：自动解密、脱敏、字典标签填充（含父类字段）</li>
 *   <li>数据变更记录：基于 {@link DataChangeRecord} 注解记录字段级前后变更</li>
 *   <li>数据完整性：基于 {@link DataIntegrity} 注解进行 MD5 防篡改校验</li>
 *   <li>扩展点：查询/新增/修改/删除的前后置钩子，集中定义在类尾「生命周期钩子」分区，子类按需重写</li>
 * </ul>
 * <p>实体写入会原地写入密文、MD5 以及 MyBatis-Plus 自动填充值；布尔型标准写入方法不回查。
 * 任意 Wrapper 更新、链式更新和 Map/对象投影保留 MyBatis-Plus 原始语义，不承诺实体级处理。</p>
 *
 * @param <M> Mapper 类型，必须继承 {@link CommonMapper}
 * @param <T> 实体类型
 * @author dream
 */
@Slf4j
@Service
public abstract class CommonServiceImpl<M extends CommonMapper<T>, T> extends ServiceImpl<M, T> implements CommonService<T> {

    /**
     * 变更记录字段值的最大长度（对齐 sys_data_change_record 表 VARCHAR(2000)），超出截断
     */
    private static final int DEFAULT_MAX_CHANGE_VALUE_LENGTH = 2000;

    /**
     * 变更记录默认排除的框架审计字段（变更记录行自身已携带 createBy/createTime 等审计信息），
     * 实体可用 {@link DataChangeRecord#excludeAuditFields()} 整体覆盖
     */
    private static final Set<String> DEFAULT_CHANGE_RECORD_AUDIT_FIELDS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList("createBy", "createTime", "updateBy", "updateTime")));

    /**
     * 数据变更记录处理器（可选依赖）。
     * 通过 Setter 注入 {@link ObjectProvider}：容器中注册了 {@link DataChangeRecordHandler}
     * 实现 Bean 则由其接管批量分发（落库 / 上报），未注册时仅打印 INFO 日志，
     * 行为与 {@code OperationLogHandler} 的默认策略一致。
     */
    private ObjectProvider<DataChangeRecordHandler> changeRecordHandlerProvider;

    /**
     * 注入可选的数据变更记录处理器。
     *
     * @param changeRecordHandlerProvider 处理器提供者，不会为 {@code null}
     */
    @Autowired
    public void setChangeRecordHandlerProvider(ObjectProvider<DataChangeRecordHandler> changeRecordHandlerProvider) {
        this.changeRecordHandlerProvider = changeRecordHandlerProvider;
    }

    /** 实体字段元数据缓存；缓存不可变列表，由 {@link #getAllFields(Class)} 返回独立副本。 */
    private static final ClassValue<List<Field>> ENTITY_FIELDS_CACHE = new ClassValue<List<Field>>() {
        @Override
        protected List<Field> computeValue(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            Class<?> current = type;
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    if (seen.add(field.getName())) {
                        field.setAccessible(true);
                        fields.add(field);
                    }
                }
                current = current.getSuperclass();
            }
            return Collections.unmodifiableList(fields);
        }
    };

    /** 单次结果处理范围内的字典标签缓存，处理后立即清理，不跨请求共享。 */
    private static final ThreadLocal<Map<Dict, Map<String, String>>> DICT_LABEL_CACHE = new ThreadLocal<>();

    /**
     * 获取实体类类型，运行期复用 MyBatis-Plus 对多层泛型的解析结果。
     *
     * @return 实体类 Class；无 Mapper 的离线场景沿用直接父类解析，无法解析时返回 Object.class
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getEntityClassType() {
        if (baseMapper != null) {
            return getEntityClass();
        }
        return (Class<T>) MybatisPlusUtils.getClassByType(this.getClass(), 1);
    }

    /**
     * 根据条件查询单个实体。
     * <p>只查询一页一条且不统计总数；业务排序之后追加主键升序作为稳定次序。</p>
     *
     * @param query 查询条件实体
     * @return 匹配的首条实体；无匹配返回 {@code null}
     */
    @Override
    public T findOne(T query) {
        QueryWrapper<T> queryWrapper = buildQueryWrapper(query);
        beforeQuery(queryWrapper, query);
        queryWrapper.orderByAsc(resolvePrimaryKeyColumn());
        Page<T> page = baseMapper.selectPage(new Page<>(1, 1, false), queryWrapper);
        List<T> records = page.getRecords();
        processResultList(records);
        afterQuery(records, query);
        return records.isEmpty() ? null : records.get(0);
    }

    /**
     * 根据条件查询实体列表。
     * <p>内部使用 {@link #buildQueryWrapper} 构建条件，依次经过
     * {@link #beforeQuery} → 执行 SQL → 解密/脱敏/字典处理 → {@link #afterQuery}。</p>
     *
     * @param query 查询条件实体，为 {@code null} 时返回全量
     * @return 匹配的实体列表，不会为 {@code null}
     */
    @Override
    public List<T> findList(T query) {
        QueryWrapper<T> queryWrapper = buildQueryWrapper(query);
        beforeQuery(queryWrapper, query);
        List<T> list = baseMapper.selectList(queryWrapper);
        processResultList(list);
        afterQuery(list, query);
        return list;
    }

    /**
     * 分页查询。
     * <p>支持排序、自动构建查询条件，依次经过 {@link #beforeQuery} → 执行 SQL →
     * 解密/脱敏/字典处理 → {@link #afterQuery} → {@link #processPageResult}。</p>
     * <p>{@code pageSize} 超过 {@link #resolveMaxPageSize()} 时按上限截断，避免一次拉取全表。</p>
     *
     * @param pageQuery 分页查询参数（含查询条件与排序字段）
     * @return 分页结果
     */
    @Override
    public IPage<T> findPage(CommonPageQuery<T> pageQuery) {
        int maxPageSize = resolveMaxPageSize();
        if (pageQuery.getPageSize() != null && pageQuery.getPageSize() > maxPageSize) {
            log.warn("pageSize={} 超过上限 {}，已按上限截断", pageQuery.getPageSize(), maxPageSize);
            pageQuery.setPageSize(maxPageSize);
        }
        Page<T> page = pageQuery.toPage();
        QueryWrapper<T> queryWrapper = buildQueryWrapper(pageQuery.getParams());
        beforeQuery(queryWrapper, pageQuery.getParams());

        // 处理排序
        if (pageQuery.getOrderFields() != null && !pageQuery.getOrderFields().isEmpty()) {
            for (CommonQuery.OrderField orderField : pageQuery.getOrderFields()) {
                String column = orderField.getColumn();
                if (pageQuery.getCamelToUnderscore()) {
                    column = camelToUnderline(column);
                }
                if (orderField.getTableAlias() != null && !orderField.getTableAlias().isEmpty()) {
                    column = orderField.getTableAlias() + "." + column;
                }
                // 排序列名白名单校验，防止 ORDER BY 注入
                column = validateColumnName(column);
                queryWrapper.orderBy(true, orderField.isAsc(), column);
            }
        }

        IPage<T> resultPage = baseMapper.selectPage(page, queryWrapper);

        // 对查询结果进行后续处理(解密、脱敏等)
        processResultList(resultPage.getRecords());

        // 查询后置钩子（业务补充关联数据等），再交给分页结果后处理
        afterQuery(resultPage.getRecords(), pageQuery.getParams());

        // 调用分页结果后处理方法
        processPageResult(resultPage, pageQuery);

        return resultPage;
    }

    /**
     * 分页大小上限，默认 {@link com.dream.common.core.constant.Page#MAX_PAGE_SIZE}（1000）。
     * <p>业务确需更大单页数据量（如导出）时在子类覆写本方法，不要直接放开前端传参。</p>
     *
     * @return 单页最大条数，必须为正数
     */
    protected int resolveMaxPageSize() {
        return com.dream.common.core.constant.Page.MAX_PAGE_SIZE;
    }

    /**
     * 新增实体（带前置/后置处理）。
     * <p>前置处理包括字段加密、数据完整性 MD5 计算；新增后会触发数据变更记录。</p>
     *
     * @param entity 待新增实体
     * @return 新增后查询出的完整实体（已解密/脱敏/字典处理）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public T addEntity(T entity) {
        save(entity);
        return findById(getEntityId(entity));
    }

    /**
     * MyBatis-Plus 标准新增入口，与 {@link #addEntity} 共用加密、MD5 和审计链路。
     * <p>方法不回查；输入实体会原地写入密文、MD5、主键及自动填充值。</p>
     *
     * @param entity 待新增实体
     * @return 插入影响行数大于零时返回 {@code true}
     * @throws BusinessException 前置校验、MD5 计算失败或插入影响行数为零时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(T entity) {
        beforeAdd(entity);
        calculateAndSetDataMd5(entity);

        int insertedRows = baseMapper.insert(entity);
        if (insertedRows <= 0) {
            throw new BusinessException("数据新增失败");
        }

        afterAdd(entity);
        return true;
    }

    /**
     * 批量新增实体。
     * <p>
     * 相较于循环调用 {@link #addEntity}，本方法仅做一次前置处理循环（加密、MD5 计算），
     * 再通过 MyBatis-Plus 的 {@link #saveBatch} 进行批量插入，避免逐条插入与逐条回查的开销；
     * 插入成功后逐条触发 {@link #afterAdd}，与单条新增保持一致的变更记录语义。
     * </p>
     *
     * 返回输入列表本身，不逐条回查或解密；实体保留写库后的密文、MD5、主键及自动填充值。
     *
     * @param entityList 待新增实体列表
     * @return 输入实体列表（含已生成的主键），不会为 {@code null}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<T> addBatch(List<T> entityList) {
        return doAddBatch(entityList, DEFAULT_BATCH_SIZE);
    }

    /**
     * 按默认批次大小新增实体，并执行完整的单条前置、后置处理。
     *
     * @param entityList 待新增实体集合
     * @return 集合非空且新增成功时返回 {@code true}；空集合返回 {@code false}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<T> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return false;
        }
        addBatch(new ArrayList<>(entityList));
        return true;
    }

    /**
     * 按指定批次大小新增实体，并执行加密、MD5 和数据变更记录链路。
     * <p>输入实体会原地写入持久化值，方法不回查或解密。</p>
     *
     * @param entityList 待新增实体集合
     * @param batchSize  每批写入条数，必须为正数
     * @return 集合非空且新增成功时返回 {@code true}；空集合返回 {@code false}
     * @throws BusinessException 批量新增失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<T> entityList, int batchSize) {
        if (entityList == null || entityList.isEmpty()) {
            return false;
        }
        doAddBatch(new ArrayList<>(entityList), batchSize);
        return true;
    }

    /**
     * 执行批量新增的公共实现，供自定义与标准 API 共用。
     *
     * @param entityList 待新增实体列表
     * @param batchSize  每批写入条数
     * @return 输入列表；空输入返回新的空列表
     */
    private List<T> doAddBatch(List<T> entityList, int batchSize) {
        if (entityList == null || entityList.isEmpty()) {
            return new ArrayList<>();
        }
        for (T entity : entityList) {
            beforeAdd(entity);
            calculateAndSetDataMd5(entity);
        }
        boolean success = super.saveBatch(entityList, batchSize);
        if (!success) {
            throw new BusinessException("数据批量新增失败");
        }
        for (T entity : entityList) {
            afterAdd(entity);
        }
        return entityList;
    }

    /**
     * 修改实体（带前置/后置处理）。
     * <p>
     * 会先校验主键 ID 是否存在、原数据是否存在，并对原数据做完整性校验；
     * 前置处理包括字段加密与 MD5 计算，变更记录在实际更新成功后采集。
     * </p>
     *
     * @param entity 待修改实体，必须包含主键 ID
     * @return 修改后查询出的完整实体（已解密/脱敏/字典处理）
     * @throws BusinessException 当实体缺少 ID 或原数据不存在时抛出（并记录日志）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public T updateEntity(T entity) {
        String id = doUpdateEntity(entity);
        return findById(id);
    }

    /**
     * MyBatis-Plus 标准更新入口，与 {@link #updateEntity} 共用完整处理链路。
     * <p>方法不回查；输入实体保留加密、MD5 和自动填充后的持久化值。</p>
     *
     * @param entity 待更新实体，必须包含主键
     * @return 更新影响行数大于零时返回 {@code true}
     * @throws BusinessException 主键缺失、原数据不存在、完整性校验失败或更新影响行数为零时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(T entity) {
        doUpdateEntity(entity);
        return true;
    }

    /**
     * 查询原数据后执行实体更新。
     *
     * @param entity 待更新实体
     * @return 主键字符串
     */
    private String doUpdateEntity(T entity) {
        return doUpdateEntity(entity, null);
    }

    /**
     * 基于同一版本原数据执行完整性校验、前置处理、更新、审计和后置钩子。
     *
     * @param entity    待更新实体
     * @param oldEntity 已查询的原数据；为 {@code null} 时按主键查询
     * @return 主键字符串
     */
    private String doUpdateEntity(T entity, T oldEntity) {
        String id = getEntityId(entity);
        if (id == null) {
            log.error("更新操作必须包含ID，实体类型：{}", entity.getClass().getName());
            throw new BusinessException("更新操作必须包含ID");
        }

        if (oldEntity == null) {
            oldEntity = baseMapper.selectById(id);
        }
        if (oldEntity == null) {
            log.error("数据不存在，ID：{}", id);
            throw new BusinessException("数据不存在");
        }

        checkDataIntegrityBeforeUpdate(oldEntity);
        beforeUpdate(oldEntity, entity);
        calculateAndSetDataMd5(oldEntity, entity);

        int updatedRows = baseMapper.updateById(entity);
        if (updatedRows <= 0) {
            throw new BusinessException("数据更新失败，数据可能已被修改或删除");
        }

        recordDataChange(entity, OperationType.UPDATE, oldEntity, entity);
        afterUpdate(entity);
        return id;
    }

    /**
     * 新增或修改实体。
     * <p>根据 ID 是否存在决定执行新增或修改：ID 为空或数据库中不存在时新增，否则修改。</p>
     *
     * @param entity 待保存实体
     * @return 保存后的完整实体（已解密/脱敏/字典处理）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public T saveOrUpdateEntity(T entity) {
        doSaveOrUpdate(entity);
        return findById(getEntityId(entity));
    }

    /**
     * 根据主键及数据库存在性选择新增或更新，已查原数据直接复用，避免重复 SELECT。
     *
     * @param entity 待保存实体
     */
    private void doSaveOrUpdate(T entity) {
        String id = getEntityId(entity);
        if (id == null || id.isEmpty()) {
            save(entity);
            return;
        }
        T existing = baseMapper.selectById(id);
        if (existing == null) {
            save(entity);
        } else {
            doUpdateEntity(entity, existing);
        }
    }

    /**
     * MyBatis-Plus 标准新增或更新入口，不回查保存后的实体。
     *
     * @param entity 待保存实体
     * @return 新增或更新成功时返回 {@code true}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdate(T entity) {
        doSaveOrUpdate(entity);
        return true;
    }

    /**
     * 逐条执行完整实体更新链，保证每条记录独立进行完整性校验、MD5 合并和字段级审计。
     * <p>{@code batchSize} 仅保留 MyBatis-Plus 方法签名兼容；当前实现不使用批量 SQL，
     * 因此该参数不影响处理分组。</p>
     *
     * @param entityList 待更新实体集合
     * @param batchSize  MyBatis-Plus 批次大小参数，当前不生效
     * @return 集合非空且全部更新成功时返回 {@code true}；空集合返回 {@code false}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchById(Collection<T> entityList, int batchSize) {
        if (entityList == null || entityList.isEmpty()) {
            return false;
        }
        for (T entity : entityList) {
            doUpdateEntity(entity);
        }
        return true;
    }

    /**
     * 逐条执行新增或更新，复用存在性查询结果且不额外回查完整实体。
     * <p>{@code batchSize} 仅保留接口兼容，当前实现按实体逐条处理。</p>
     *
     * @param entityList 待保存实体集合
     * @param batchSize  MyBatis-Plus 批次大小参数，当前不生效
     * @return 集合非空且全部保存成功时返回 {@code true}；空集合返回 {@code false}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateBatch(Collection<T> entityList, int batchSize) {
        if (entityList == null || entityList.isEmpty()) {
            return false;
        }
        for (T entity : entityList) {
            doSaveOrUpdate(entity);
        }
        return true;
    }

    /**
     * 根据 ID 删除实体（带前后钩子与数据变更记录）。
     * <p>
     * 覆盖 MyBatis-Plus 默认实现：删除前先查询原数据，依次调用 {@link #beforeDelete}、执行删除、
     * {@link #afterDelete}（默认记录 DELETE 变更，仅当实体标注 {@link DataChangeRecord} 时生效）。
     * 逻辑删除实体同样适用；ID 查不到原数据时两个钩子都不触发。
     * </p>
     *
     * @param id 主键 ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        T oldEntity = baseMapper.selectById(id);
        if (oldEntity != null) {
            beforeDelete(oldEntity);
        }

        boolean success = super.removeById(id);

        if (success && oldEntity != null) {
            afterDelete(oldEntity);
        }
        return success;
    }

    /**
     * 根据 ID 列表批量删除实体（带批量前后钩子与数据变更记录）。
     * <p>
     * 删除前一次性批量查询原数据，依次调用 {@link #beforeDeleteBatch}、执行删除、{@link #afterDeleteBatch}；
     * 后置钩子默认合并全部实体的 DELETE 变更记录后统一分发，避免逐实体调用处理器。
     * </p>
     * <p>
     * 参数使用原生 {@link Collection} 类型：MyBatis-Plus 3.5.17 中
     * {@code extension.repository.IRepository#removeByIds(Collection)} 与
     * {@code spring.service.IService#removeByIds(Collection<? extends Serializable>)}
     * 双接口签名并存，任何泛型参数化都会造成名称冲突，原生类型擦除后可同时覆盖两者。
     * </p>
     *
     * @param ids 主键 ID 列表
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean removeByIds(Collection ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        List<T> oldEntities = baseMapper.selectByIds(ids);
        boolean hasOld = oldEntities != null && !oldEntities.isEmpty();
        if (hasOld) {
            beforeDeleteBatch(oldEntities);
        }

        boolean success = super.removeByIds(ids);

        if (success && hasOld) {
            afterDeleteBatch(oldEntities);
        }
        return success;
    }

    /**
     * 根据主键删除实体，并按 MyBatis-Plus 语义控制逻辑删除时是否执行自动填充。
     * <p>删除前查询原数据用于前后钩子和 DELETE 变更记录；原数据不存在时不触发钩子。</p>
     *
     * @param id      主键 ID
     * @param useFill 是否启用删除时的字段自动填充
     * @return 删除影响行数大于零时返回 {@code true}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id, boolean useFill) {
        T oldEntity = baseMapper.selectById(id);
        if (oldEntity != null) {
            beforeDelete(oldEntity);
        }
        boolean success = baseMapper.deleteById(id, useFill) > 0;
        if (success && oldEntity != null) {
            afterDelete(oldEntity);
        }
        return success;
    }

    /**
     * 根据实体删除数据，保留实体中的乐观锁版本等删除条件。
     * <p>删除前按主键查询原数据用于前后钩子和 DELETE 变更记录；实际删除使用完整实体，
     * 不会退化为仅按主键删除。</p>
     *
     * @param entity 待删除实体，必须包含主键，可携带乐观锁版本
     * @return 删除影响行数大于零时返回 {@code true}；实体或主键为 {@code null} 时返回 {@code false}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(T entity) {
        if (entity == null) {
            return false;
        }
        String id = getEntityId(entity);
        if (id == null) {
            return false;
        }
        T oldEntity = baseMapper.selectById(id);
        if (oldEntity != null) {
            beforeDelete(oldEntity);
        }
        boolean success = baseMapper.deleteById(entity) > 0;
        if (success && oldEntity != null) {
            afterDelete(oldEntity);
        }
        return success;
    }

    /**
     * 按主键集合批量删除，并控制逻辑删除时是否执行自动填充。
     * <p>删除前一次性查询原数据，删除成功后通过批量后置钩子统一汇总变更记录。</p>
     *
     * @param ids     主键集合，元素须为 MyBatis-Plus 支持的主键类型
     * @param useFill 是否启用删除时的字段自动填充
     * @return 删除影响行数大于零时返回 {@code true}；空集合返回 {@code false}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("unchecked")
    public boolean removeByIds(Collection<?> ids, boolean useFill) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        List<T> oldEntities = baseMapper.selectByIds((Collection<? extends Serializable>) ids);
        boolean hasOld = oldEntities != null && !oldEntities.isEmpty();
        if (hasOld) {
            beforeDeleteBatch(oldEntities);
        }
        boolean success = baseMapper.deleteByIds(ids, useFill) > 0;
        if (success && hasOld) {
            afterDeleteBatch(oldEntities);
        }
        return success;
    }

    /**
     * 按 Wrapper 条件删除实体，并保留批量删除前后钩子与 DELETE 变更记录。
     * <p>删除前使用相同条件查询原数据；没有匹配数据时不触发钩子。</p>
     *
     * @param queryWrapper 删除条件
     * @return MyBatis-Plus 删除成功时返回 {@code true}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(Wrapper<T> queryWrapper) {
        List<T> oldEntities = baseMapper.selectList(queryWrapper);
        boolean hasOld = oldEntities != null && !oldEntities.isEmpty();
        if (hasOld) {
            beforeDeleteBatch(oldEntities);
        }
        boolean success = super.remove(queryWrapper);
        if (success && hasOld) {
            afterDeleteBatch(oldEntities);
        }
        return success;
    }

    /**
     * 按数据库列等值条件批量删除，复用 {@link #remove(Wrapper)} 的完整处理链。
     *
     * @param columnMap 数据库列名与等值条件的映射
     * @return 删除成功时返回 {@code true}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByMap(Map<String, Object> columnMap) {
        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
        queryWrapper.allEq(columnMap);
        return remove(queryWrapper);
    }

    /**
     * 根据 ID 查询实体，并对结果进行解密、脱敏、字典标签填充。
     *
     * @param id 主键 ID
     * @return 实体；不存在返回 {@code null}
     */
    @Override
    public T findById(String id) {
        T entity = baseMapper.selectById(id);
        if (entity != null) {
            // 解密处理
            decryptEntity(entity);
            // 脱敏处理
            desensitizeEntity(entity);
            // 字典处理
            processDict(entity);
        }
        return entity;
    }

    /**
     * MyBatis-Plus 标准主键查询入口，复用 {@link #findById(String)} 的解密、脱敏和字典处理。
     *
     * @param id 主键 ID
     * @return 处理后的实体；不存在或主键为 {@code null} 时返回 {@code null}
     */
    @Override
    public T getById(Serializable id) {
        return findById(id == null ? null : String.valueOf(id));
    }

    /**
     * 按主键查询并返回 Optional，实体后处理规则与 {@link #getById(Serializable)} 一致。
     *
     * @param id 主键 ID
     * @return 包含查询结果的 Optional；无结果时返回 {@link Optional#empty()}
     */
    @Override
    public Optional<T> getOptById(Serializable id) {
        return Optional.ofNullable(getById(id));
    }

    /**
     * 查询全部实体，复用 {@link #findList(Object)} 的条件构建及查询前后钩子。
     *
     * @return 经过解密、脱敏、字典和业务后置处理的实体列表
     */
    @Override
    public List<T> list() {
        return findList(null);
    }

    /**
     * 按调用方提供的 Wrapper 查询列表。
     * <p>不根据实体注解重建条件，也不调用 {@link #beforeQuery}；查询后执行解密、脱敏、
     * 字典处理及 {@link #afterQuery}，其中原始查询实体参数为 {@code null}。</p>
     *
     * @param queryWrapper 查询条件
     * @return 处理后的实体列表
     */
    @Override
    public List<T> list(Wrapper<T> queryWrapper) {
        return processStandardQueryResult(baseMapper.selectList(queryWrapper));
    }

    /**
     * 按主键集合批量查询，并执行统一的结果后处理和查询后置钩子。
     *
     * @param idList 主键集合
     * @return 处理后的实体列表
     */
    @Override
    public List<T> listByIds(Collection<? extends Serializable> idList) {
        return processStandardQueryResult(baseMapper.selectByIds(idList));
    }

    /**
     * 按数据库列等值条件查询，并执行统一的结果后处理和查询后置钩子。
     *
     * @param columnMap 数据库列名与等值条件的映射
     * @return 处理后的实体列表
     */
    @Override
    public List<T> listByMap(Map<String, Object> columnMap) {
        return processStandardQueryResult(baseMapper.selectByMap(columnMap));
    }

    /**
     * 按 Wrapper 分页查询并返回当前页记录列表。
     * <p>分页大小超过 {@link #resolveMaxPageSize()} 时会原地截断分页参数。</p>
     *
     * @param page         分页参数
     * @param queryWrapper 查询条件
     * @return 当前页中经过统一后处理的实体列表
     */
    @Override
    public List<T> list(IPage<T> page, Wrapper<T> queryWrapper) {
        clampStandardPageSize(page);
        return processStandardQueryResult(super.list(page, queryWrapper));
    }

    /**
     * 按 Wrapper 查询分页结果，并处理当前页记录。
     * <p>分页大小超过 {@link #resolveMaxPageSize()} 时会原地截断分页参数；
     * 分页总数及其他分页属性保持 MyBatis-Plus 原始语义。</p>
     *
     * @param page         分页参数及结果载体
     * @param queryWrapper 查询条件
     * @param <E>          分页对象类型
     * @return 当前页记录已处理后返回的分页对象
     */
    @Override
    public <E extends IPage<T>> E page(E page, Wrapper<T> queryWrapper) {
        clampStandardPageSize(page);
        E resultPage = super.page(page, queryWrapper);
        processStandardQueryResult(resultPage.getRecords());
        return resultPage;
    }

    /**
     * 按 Wrapper 查询单个实体，并执行统一的结果后处理和查询后置钩子。
     *
     * @param queryWrapper 查询条件
     * @param throwEx      查询到多条记录时是否按 MyBatis-Plus 语义抛出异常
     * @return 处理后的实体；无匹配记录时返回 {@code null}
     */
    @Override
    public T getOne(Wrapper<T> queryWrapper, boolean throwEx) {
        T entity = super.getOne(queryWrapper, throwEx);
        if (entity != null) {
            List<T> records = new ArrayList<>(1);
            records.add(entity);
            processStandardQueryResult(records);
        }
        return entity;
    }

    /**
     * 按 Wrapper 查询单个实体并返回 Optional，处理规则与 {@link #getOne(Wrapper, boolean)} 一致。
     *
     * @param queryWrapper 查询条件
     * @param throwEx      查询到多条记录时是否按 MyBatis-Plus 语义抛出异常
     * @return 包含查询结果的 Optional；无结果时返回 {@link Optional#empty()}
     */
    @Override
    public Optional<T> getOneOpt(Wrapper<T> queryWrapper, boolean throwEx) {
        return Optional.ofNullable(getOne(queryWrapper, throwEx));
    }

    /**
     * 将标准分页入口的页大小限制在框架上限内，并原地修改分页参数。
     *
     * @param page 分页参数，允许为 {@code null}
     */
    private void clampStandardPageSize(IPage<T> page) {
        int maxPageSize = resolveMaxPageSize();
        if (page != null && page.getSize() > maxPageSize) {
            log.warn("pageSize={} 超过上限 {}，已按上限截断", page.getSize(), maxPageSize);
            page.setSize(maxPageSize);
        }
    }

    /**
     * 对标准查询入口的记录执行框架后处理和业务查询后置钩子。
     *
     * @param records 查询记录集合
     * @return 原记录集合，便于调用方直接返回
     */
    private List<T> processStandardQueryResult(List<T> records) {
        processResultList(records);
        afterQuery(records, null);
        return records;
    }

    /**
     * 检查指定 ID 对应实体的数据完整性。
     * <p>
     * 通过对比存储的 MD5 与当前计算的 MD5 判断数据是否被篡改。
     * 启用校验后，MD5 字段不存在、MD5 为空、计算异常或摘要不一致均视为校验失败；
     * 默认返回 {@code false}，{@link DataIntegrity#throwOnFailure()} 为 {@code true} 时抛出
     * {@link BusinessException}。只有未启用校验时才直接通过。
     * </p>
     *
     * @param id 主键 ID
     * @return {@code true} 表示完整或未启用校验；{@code false} 表示记录不存在、MD5 缺失、数据被篡改或校验异常
     * @throws BusinessException 当检测到篡改且配置为失败时抛出
     */
    @Override
    public boolean checkDataIntegrity(String id) {
        try {
            return checkDataIntegrity(baseMapper.selectById(id));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("检查数据完整性失败，ID：{}", id, e);
            return false;
        }
    }

    /**
     * 直接校验已查询实体，更新链路复用本方法可避免按 ID 重复查询及跨版本检查。
     *
     * @param entity 数据库实体
     * @return 是否通过完整性校验
     */
    protected boolean checkDataIntegrity(T entity) {
        if (entity == null) {
            return false;
        }
        DataIntegrity integrity = entity.getClass().getAnnotation(DataIntegrity.class);
        if (integrity == null || !integrity.enable()) {
            return true;
        }

        String storedMd5;
        String currentMd5;
        try {
            storedMd5 = getDataMd5FromEntity(entity, integrity.md5Field());
            if (storedMd5 == null || storedMd5.isEmpty()) {
                throw new BusinessException("数据完整性MD5为空");
            }
            String md5FieldName = resolveMd5FieldName(entity.getClass(), integrity.md5Field());
            String excludes = integrity.excludeFields() + "," + md5FieldName;
            currentMd5 = calculateMd5(entity, excludes);
        } catch (Exception e) {
            String id = getEntityId(entity);
            log.error("检查数据完整性失败，ID：{}", id, e);
            if (integrity.throwOnFailure()) {
                throw new BusinessException("数据完整性校验失败：" + e.getMessage());
            }
            return false;
        }

        boolean isValid = currentMd5.equals(storedMd5);
        if (!isValid && integrity.throwOnFailure()) {
            log.error("数据完整性校验失败，数据可能已被篡改，ID：{}", getEntityId(entity));
            throw new BusinessException("数据完整性校验失败，数据可能已被篡改");
        }
        return isValid;
    }

    /**
     * 根据查询条件实体构建 MyBatis-Plus QueryWrapper。
     * <p>
     * 扫描查询对象（含父类）所有带 {@link QueryCondition} 注解的字段，
     * 按注解配置的 {@link QueryType} 映射到 MyBatis-Plus 对应的条件方法。
     * </p>
     *
     * <p>取值规则：</p>
     * <ul>
     *   <li>值为 {@code null}、空白字符串、空集合或空数组时跳过该条件；
     *       {@link QueryType#EQ_OR_IS_NULL} 例外，它以 {@code null} 表达 {@code IS NULL}。</li>
     *   <li>多值条件（IN / NOT_IN / BETWEEN / NOT_BETWEEN）统一接受集合与数组；
     *       字符串只有在 {@code splitToIn=true} 或范围类条件下才按分隔符切割。</li>
     *   <li>切割后为空集合时跳过，不会生成 {@code IN ()} 这类非法 SQL。</li>
     *   <li>需要拼接原始 SQL 的条件（{@code inSql / exists / apply} 等）刻意不进本注解白名单，
     *       由后端在 {@link #beforeQuery} 中自行构造。</li>
     * </ul>
     *
     * @param query 查询条件实体，为 {@code null} 时返回空 Wrapper
     * @return 构建好的 QueryWrapper
     */
    @Override
    public QueryWrapper<T> buildQueryWrapper(T query) {
        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
        if (query == null) {
            return queryWrapper;
        }

        List<Field> fields = getAllFields(query.getClass());
        for (Field field : fields) {
            QueryCondition condition = field.getAnnotation(QueryCondition.class);
            if (condition == null || condition.ignore()) {
                continue;
            }

            field.setAccessible(true);
            try {
                Object value = field.get(query);
                QueryType type = condition.type();
                // 历史语义：splitToIn=true 且未显式换掉默认的 EQ 时按 IN 处理，保持对外兼容
                if (condition.splitToIn() && type == QueryType.EQ) {
                    type = QueryType.IN;
                }
                if (type == QueryType.EQ_OR_IS_NULL) {
                    if (isBlankValue(value)) {
                        // 未赋值（null / 空白串 / 空集合）统一表达为 IS NULL
                        value = null;
                    }
                } else if (isBlankValue(value)) {
                    continue;
                }

                String column = getColumnName(field, condition);
                // splitToIn 只负责「字符串切成多值」，落到 in 还是 notIn 仍由 type 决定
                boolean splitString = condition.splitToIn()
                        || type == QueryType.BETWEEN || type == QueryType.NOT_BETWEEN;
                List<Object> values = toConditionValues(value, condition.splitSeparator(), splitString);

                applyCondition(queryWrapper, column, type, value, values);
            } catch (IllegalAccessException e) {
                log.error("构建查询条件失败 | field={}", field.getName(), e);
            }
        }

        return queryWrapper;
    }

    /**
     * 将单个查询条件按类型落到 Wrapper 上。
     *
     * @param queryWrapper 目标 Wrapper
     * @param column       数据库列名
     * @param type         查询类型
     * @param value        字段原始值
     * @param values       归一化后的多值集合
     */
    private void applyCondition(QueryWrapper<T> queryWrapper, String column, QueryType type,
                                Object value, List<Object> values) {
        switch (type) {
            case EQ:
                queryWrapper.eq(column, value);
                break;
            case EQ_OR_IS_NULL:
                queryWrapper.eqOrIsNull(column, value);
                break;
            case NE:
                queryWrapper.ne(column, value);
                break;
            case GT:
                queryWrapper.gt(column, value);
                break;
            case GE:
                queryWrapper.ge(column, value);
                break;
            case LT:
                queryWrapper.lt(column, value);
                break;
            case LE:
                queryWrapper.le(column, value);
                break;
            case LIKE:
                queryWrapper.like(column, value);
                break;
            case LIKE_LEFT:
                queryWrapper.likeLeft(column, value);
                break;
            case LIKE_RIGHT:
                queryWrapper.likeRight(column, value);
                break;
            case NOT_LIKE:
                queryWrapper.notLike(column, value);
                break;
            case NOT_LIKE_LEFT:
                queryWrapper.notLikeLeft(column, value);
                break;
            case NOT_LIKE_RIGHT:
                queryWrapper.notLikeRight(column, value);
                break;
            case IN:
                if (!values.isEmpty()) {
                    queryWrapper.in(column, values);
                }
                break;
            case NOT_IN:
                if (!values.isEmpty()) {
                    queryWrapper.notIn(column, values);
                }
                break;
            case BETWEEN:
            case NOT_BETWEEN:
                if (values.size() >= 2) {
                    if (type == QueryType.BETWEEN) {
                        queryWrapper.between(column, values.get(0), values.get(1));
                    } else {
                        queryWrapper.notBetween(column, values.get(0), values.get(1));
                    }
                } else {
                    log.warn("范围查询需要两个值，条件已忽略 | column={} 值个数={}", column, values.size());
                }
                break;
            case IS_NULL:
                if (isSwitchOn(value)) {
                    queryWrapper.isNull(column);
                }
                break;
            case IS_NOT_NULL:
                if (isSwitchOn(value)) {
                    queryWrapper.isNotNull(column);
                }
                break;
            default:
                log.warn("未支持的查询类型，条件已忽略 | column={} type={}", column, type);
                break;
        }
    }

    /**
     * 将字段值归一化为条件值集合：集合与数组原样展开，字符串按需按分隔符切割，其余作为单值；
     * 同时去除 {@code null} 与空白字符串元素并 trim。
     *
     * @param value       字段原始值
     * @param separator   分隔符，为空时按逗号处理
     * @param splitString 是否对字符串值执行分隔符切割
     * @return 归一化后的值集合，不会为 {@code null}
     */
    protected List<Object> toConditionValues(Object value, String separator, boolean splitString) {
        List<Object> raw = new ArrayList<>();
        if (value == null) {
            return raw;
        }

        if (value instanceof Collection) {
            raw.addAll((Collection<?>) value);
        } else if (value.getClass().isArray()) {
            for (int i = 0, len = Array.getLength(value); i < len; i++) {
                raw.add(Array.get(value, i));
            }
        } else if (value instanceof String && splitString) {
            String sep = (separator == null || separator.isEmpty()) ? "," : separator;
            Collections.addAll(raw, ((String) value).split(Pattern.quote(sep)));
        } else {
            raw.add(value);
        }

        List<Object> values = new ArrayList<>(raw.size());
        for (Object item : raw) {
            if (item instanceof String) {
                String text = ((String) item).trim();
                if (!text.isEmpty()) {
                    values.add(text);
                }
            } else if (item != null) {
                values.add(item);
            }
        }
        return values;
    }

    /**
     * 判断值是否应视为「未赋值」：{@code null}、空白字符串、空集合或空数组。
     *
     * @param value 字段值
     * @return {@code true} 表示应跳过该条件
     */
    protected boolean isBlankValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        }
        return false;
    }

    /**
     * IS NULL / IS NOT NULL 的开关语义：值为 {@link Boolean} 时以 {@code true} 为准，
     * 其他类型非空即生效。
     *
     * @param value 字段值
     * @return {@code true} 表示追加该条件
     */
    protected boolean isSwitchOn(Object value) {
        return !(value instanceof Boolean) || (Boolean) value;
    }

    /**
     * 获取字段对应的数据库列名。
     * <p>优先使用注解显式配置的列名，否则按驼峰转下划线规则生成；支持追加表别名。</p>
     *
     * @param field     字段对象
     * @param condition 字段上的查询条件注解
     * @return 数据库列名（可能含表别名）
     */
    protected String getColumnName(Field field, QueryCondition condition) {
        String column = condition.column();
        if (column == null || column.isEmpty()) {
            column = condition.camelToUnderscore() ? camelToUnderline(field.getName()) : field.getName();
        }

        String tableAlias = condition.tableAlias();
        if (tableAlias != null && !tableAlias.isEmpty()) {
            column = tableAlias + "." + column;
        }

        return column;
    }

    /**
     * 驼峰命名转下划线命名（如 userName → user_name）。
     *
     * @param str 原始字符串
     * @return 转换后的字符串；入参为 {@code null} 或空时原样返回
     */
    protected String camelToUnderline(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append("_").append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 校验列名是否为合法的数据库标识符（表别名.列名 或 列名）。
     * <p>
     * 仅允许字母、数字、下划线及单个点号（表别名分隔符），
     * 从源头阻断 ORDER BY 等位置的 SQL 注入（MyBatis-Plus 对排序列不做参数化转义）。
     * </p>
     *
     * @param column 原始列名
     * @return 校验通过的列名
     * @throws IllegalArgumentException 列名非法时抛出
     */
    protected String validateColumnName(String column) {
        if (column == null || column.isEmpty()) {
            throw new IllegalArgumentException("排序列名不能为空");
        }
        if (!column.matches("[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)?")
                || column.contains("..") || column.endsWith(".")) {
            throw new IllegalArgumentException("非法的排序列名: " + column);
        }
        return column;
    }

    /**
     * 更新前检查原数据完整性。
     * <p>仅记录日志、不阻断更新流程；但 {@link DataIntegrity#throwOnFailure()} 为 {@code true} 时
     * 抛出的 {@link BusinessException} 会原样上抛，由事务回滚本次更新。</p>
     *
     * @param oldEntity 原数据
     */
    protected void checkDataIntegrityBeforeUpdate(T oldEntity) {
        try {
            if (oldEntity != null) {
                checkDataIntegrity(oldEntity);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("数据完整性检查失败", e);
        }
    }

    /**
     * 计算并设置数据完整性 MD5。
     * <p>仅当实体类标注了 {@link DataIntegrity} 注解且启用自动计算时生效。</p>
     *
     * @param entity 待处理实体
     */
    protected void calculateAndSetDataMd5(T entity) {
        calculateAndSetDataMd5(null, entity);
    }

    /**
     * 根据最终持久化快照计算并设置 MD5。
     * <p>更新时以数据库旧行为底，仅覆盖本次非空更新字段，与默认 {@code updateById} 策略保持一致；
     * 主键、自动填充字段、不可查询字段及非持久化字段不参与摘要。</p>
     *
     * @param oldEntity 数据库旧实体；新增时传 {@code null}
     * @param newEntity 待写库实体
     */
    protected void calculateAndSetDataMd5(T oldEntity, T newEntity) {
        if (newEntity == null) {
            return;
        }

        DataIntegrity integrity = newEntity.getClass().getAnnotation(DataIntegrity.class);
        if (integrity == null || !integrity.enable() || !integrity.autoCalculate()) {
            return;
        }

        try {
            String md5FieldName = resolveMd5FieldName(newEntity.getClass(), integrity.md5Field());
            if (md5FieldName == null) {
                throw new BusinessException("数据完整性MD5字段不存在：" + integrity.md5Field());
            }
            String excludes = integrity.excludeFields() + "," + md5FieldName;
            String md5 = oldEntity == null
                    ? calculateMd5(newEntity, excludes)
                    : calculateMd5ForUpdate(oldEntity, newEntity, excludes);
            setDataMd5ToEntity(newEntity, md5FieldName, md5);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("计算数据完整性MD5失败", e);
            throw new BusinessException("计算数据完整性MD5失败：" + e.getMessage());
        }
    }

    /**
     * 从实体中获取数据 MD5 值。
     *
     * @param entity   实体对象
     * @param md5Field MD5 字段名
     * @return MD5 值；字段值尚未写入时为 {@code null}
     * @throws BusinessException 字段不存在或读取失败时抛出
     */
    protected String getDataMd5FromEntity(T entity, String md5Field) {
        String fieldName = resolveMd5FieldName(entity.getClass(), md5Field);
        if (fieldName == null) {
            throw new BusinessException("数据完整性MD5字段不存在：" + md5Field);
        }
        Field field = findField(entity.getClass(), fieldName);
        if (field == null) {
            throw new BusinessException("数据完整性MD5字段不存在：" + fieldName);
        }
        try {
            field.setAccessible(true);
            Object value = field.get(entity);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            throw new BusinessException("获取数据完整性MD5失败：" + e.getMessage());
        }
    }

    /**
     * 设置数据 MD5 值到实体指定字段。
     *
     * @param entity   实体对象
     * @param md5Field MD5 字段名
     * @param md5Value MD5 值
     * @throws BusinessException 字段不存在、类型不兼容或写入失败时抛出
     */
    protected void setDataMd5ToEntity(T entity, String md5Field, String md5Value) {
        String fieldName = resolveMd5FieldName(entity.getClass(), md5Field);
        if (fieldName == null) {
            throw new BusinessException("数据完整性MD5字段不存在：" + md5Field);
        }
        Field field = findField(entity.getClass(), fieldName);
        if (field == null || field.getType() != String.class) {
            throw new BusinessException("数据完整性MD5字段必须为String：" + fieldName);
        }
        try {
            field.setAccessible(true);
            field.set(entity, md5Value);
        } catch (Exception e) {
            throw new BusinessException("设置数据完整性MD5失败：" + e.getMessage());
        }
    }

    /**
     * 将 {@link DataIntegrity#md5Field()} 解析为实体上真实存在的 Java 字段名。
     * <p>
     * 注解允许写数据库列名（如下划线风格的 {@code data_md5}）或 Java 字段名（{@code dataMd5}），
     * 这里按「字段名直配 → 驼峰转下划线后匹配」的顺序解析，命中后返回 Java 字段名。
     * 历史默认值为 {@code data_md5}，若不做此解析，MD5 会因找不到字段而被静默跳过。
     * </p>
     *
     * @param entityClass 实体类
     * @param md5Field    注解声明的 MD5 字段名或列名
     * @return 实体上的 Java 字段名；未配置或找不到对应字段时返回 {@code null}，由调用方按失败处理
     */
    protected String resolveMd5FieldName(Class<?> entityClass, String md5Field) {
        if (md5Field == null || md5Field.trim().isEmpty()) {
            return null;
        }
        String configured = md5Field.trim();
        for (Field field : getAllFields(entityClass)) {
            if (field.getName().equals(configured) || camelToUnderline(field.getName()).equals(configured)) {
                return field.getName();
            }
        }
        log.warn("数据完整性MD5字段在实体中不存在 | entity={} | md5Field={}",
                entityClass.getName(), configured);
        return null;
    }

    /**
     * 查找指定名称的字段（沿父类链向上查找，优先子类）。
     *
     * @param clazz     目标类
     * @param fieldName 字段名称
     * @return Field 对象；未找到返回 {@code null}
     */
    protected Field findField(Class<?> clazz, String fieldName) {
        if (clazz == null || fieldName == null) {
            return null;
        }
        for (Field field : ENTITY_FIELDS_CACHE.get(clazz)) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    /**
     * 获取类及其所有父类（不含 Object）声明的字段，子类同名字段优先。
     * <p>元数据按类缓存；每次返回独立列表，调用方修改列表不会污染缓存。</p>
     *
     * @param clazz 目标类
     * @return 字段列表；入参为 {@code null} 时返回空列表
     */
    protected List<Field> getAllFields(Class<?> clazz) {
        if (clazz == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(ENTITY_FIELDS_CACHE.get(clazz));
    }

    /**
     * 加密实体字段。
     * <p>扫描实体（含父类）所有带 {@link Encrypt} 注解的字段，对其值执行加密。</p>
     *
     * @param entity 实体对象
     */
    protected void encryptEntity(T entity) {
        if (entity == null) {
            return;
        }

        List<Field> fields = getAllFields(entity.getClass());
        for (Field field : fields) {
            Encrypt encrypt = field.getAnnotation(Encrypt.class);
            if (encrypt == null) {
                continue;
            }

            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value == null) {
                    continue;
                }

                String encrypted = EncryptUtil.encrypt(value.toString(), encrypt.type(), encrypt.key());
                field.set(entity, encrypted);
            } catch (IllegalAccessException e) {
                log.error("加密字段失败", e);
            }
        }
    }

    /**
     * 解密实体字段。
     * <p>扫描实体（含父类）所有带 {@link Encrypt} 注解的字段，对其值执行解密。</p>
     *
     * @param entity 实体对象
     */
    protected void decryptEntity(T entity) {
        if (entity == null) {
            return;
        }

        List<Field> fields = getAllFields(entity.getClass());
        for (Field field : fields) {
            Encrypt encrypt = field.getAnnotation(Encrypt.class);
            if (encrypt == null) {
                continue;
            }

            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value == null) {
                    continue;
                }

                if (encrypt.type() == EncryptType.MD5) {
                    field.set(entity, null);
                    continue;
                }
                String decrypted = EncryptUtil.decrypt(value.toString(), encrypt.type(), encrypt.key());
                field.set(entity, decrypted);
            } catch (IllegalAccessException e) {
                log.error("解密字段失败", e);
            }
        }
    }

    /**
     * 脱敏实体字段。
     * <p>扫描实体（含父类）所有带 {@link Sensitive} 注解的字段，对其值执行脱敏处理。</p>
     *
     * @param entity 实体对象
     */
    protected void desensitizeEntity(T entity) {
        if (entity == null) {
            return;
        }

        List<Field> fields = getAllFields(entity.getClass());
        for (Field field : fields) {
            Sensitive sensitive = field.getAnnotation(Sensitive.class);
            if (sensitive == null) {
                continue;
            }

            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value == null) {
                    continue;
                }

                String desensitized = SensitiveUtil.desensitize(
                        value.toString(),
                        sensitive.type(),
                        sensitive.rule(),
                        sensitive.replaceChar()
                );
                field.set(entity, desensitized);
            } catch (IllegalAccessException e) {
                log.error("脱敏字段失败", e);
            }
        }
    }

    /**
     * 处理字典字段。
     * <p>
     * 扫描实体（含父类）所有带 {@link Dict} 注解的字段并设置标签到目标字段
     * （目标字段名由 {@link DictUtils#getTargetField(Dict, String)} 决定，默认为字段名 + "Label"）。
     * </p>
     *
     * @param entity 实体对象
     */
    protected void processDict(T entity) {
        if (entity == null) {
            return;
        }
        try {
            List<Field> fields = getAllFields(entity.getClass());
            for (Field field : fields) {
                Dict dict = field.getAnnotation(Dict.class);
                if (dict == null || !dict.enable()) {
                    continue;
                }

                field.setAccessible(true);
                Object value = field.get(entity);
                if (value == null) {
                    continue;
                }

                String fieldValue = value.toString();
                String label = resolveDictLabels(dict).getOrDefault(fieldValue, fieldValue);
                String targetFieldName = DictUtils.getTargetField(dict, field.getName());
                Field targetField = findField(entity.getClass(), targetFieldName);
                if (targetField != null) {
                    targetField.setAccessible(true);
                    targetField.set(entity, label);
                }
            }
        } catch (Exception e) {
            log.error("字典处理失败", e);
        }
    }

    /**
     * 获取字典值到标签的映射；批量结果处理期间优先复用当前线程的临时缓存。
     *
     * @param dict 字典注解配置
     * @return 字典值到显示标签的映射，不会为 {@code null}
     */
    private Map<String, String> resolveDictLabels(Dict dict) {
        Map<Dict, Map<String, String>> cache = DICT_LABEL_CACHE.get();
        if (cache != null) {
            return cache.computeIfAbsent(dict, this::loadDictLabels);
        }
        return loadDictLabels(dict);
    }

    /**
     * 从当前字典 Provider 加载标签映射，不建立跨请求缓存。
     * <p>同一字典值出现多次时保留第一个非空标签，与字典工具默认查找语义一致。</p>
     *
     * @param dict 字典注解配置
     * @return 字典值到显示标签的映射；无字典项时返回空映射
     */
    private Map<String, String> loadDictLabels(Dict dict) {
        List<DictItem> items = DictUtils.getDictItems(dict);
        if (items == null || items.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> labels = new HashMap<>();
        for (DictItem item : items) {
            if (item != null && item.getValue() != null && item.getLabel() != null) {
                labels.putIfAbsent(item.getValue(), item.getLabel());
            }
        }
        return labels;
    }

    /**
     * 处理结果列表：对每个实体依次执行解密、脱敏、字典处理。
     * <p>同一批结果复用字典标签，不跨请求缓存，业务 Provider 的字典刷新语义不变。</p>
     *
     * @param list 实体列表
     */
    protected void processResultList(List<T> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        Map<Dict, Map<String, String>> previousCache = DICT_LABEL_CACHE.get();
        if (previousCache == null) {
            DICT_LABEL_CACHE.set(new HashMap<>());
        }
        try {
            for (T entity : list) {
                decryptEntity(entity);
                desensitizeEntity(entity);
                processDict(entity);
            }
        } finally {
            if (previousCache == null) {
                DICT_LABEL_CACHE.remove();
            }
        }
    }

    /**
     * 记录数据变更。
     * <p>
     * 仅当实体类标注了 {@link DataChangeRecord} 注解且 {@link DataChangeRecord#enable()}
     * 为 {@code true} 时，扫描实体（含父类）字段并生成字段级前后值记录；
     * 汇总后一次性分发给 {@link DataChangeRecordHandler}（容器中存在且
     * {@link DataChangeRecord#persistent()} 为 {@code true} 时），否则打印 INFO 日志。
     * </p>
     * <p>
     * {@link OperationType#INSERT} 额外受 {@link DataChangeRecord#recordInsert()} 控制，
     * 默认不记录（新增的旧值恒为空，记录等于抄一遍整行，且「谁新增」已由操作日志覆盖）。
     * 业务直接调用本方法记录 INSERT 时同样受该开关约束。
     * </p>
     *
     * @param entity        实体对象（用于读取类上的注解），删除场景传原数据
     * @param operationType 操作类型（{@link OperationType#INSERT}/{@link OperationType#UPDATE}/{@link OperationType#DELETE}）
     * @param oldEntity     原数据（新增时为 {@code null}）
     * @param newEntity     新数据（删除时为 {@code null}）
     */
    protected void recordDataChange(T entity, OperationType operationType, T oldEntity, T newEntity) {
        if (entity == null) {
            return;
        }
        DataChangeRecord annotation = resolveChangeRecordAnnotation(entity.getClass());
        if (annotation == null) {
            return;
        }
        if (operationType == OperationType.INSERT && !annotation.recordInsert()) {
            return;
        }
        List<DataChangeRecordEntity> records =
                buildChangeRecords(entity.getClass(), annotation, operationType, oldEntity, newEntity);
        dispatchChangeRecords(annotation, records);
    }

    /**
     * 解析实体类上的 {@link DataChangeRecord} 注解。
     *
     * @param entityClass 实体类
     * @return 注解实例；未标注或 {@code enable()=false} 时返回 {@code null}
     */
    protected DataChangeRecord resolveChangeRecordAnnotation(Class<?> entityClass) {
        if (entityClass == null) {
            return null;
        }
        DataChangeRecord annotation = entityClass.getAnnotation(DataChangeRecord.class);
        if (annotation == null || !annotation.enable()) {
            return null;
        }
        return annotation;
    }

    /**
     * 构建字段级变更记录列表。
     * <p>
     * 字段过滤规则：排除 static / transient 字段、{@code @TableField(exist=false)} 非持久化字段、
     * 主键字段（{@code dataId} 已承载）、审计字段（默认 createBy/createTime/updateBy/updateTime，
     * 变更记录行自身已带审计信息；实体可用 {@link DataChangeRecord#excludeAuditFields()} 覆盖）、
     * {@link DataIntegrity} 声明的 MD5 派生字段。
     * </p>
     * <p>
     * 变更值按 {@link DataChangeRecord#maxValueLength()} 截断（默认 2000，与落库列宽一致）。
     * </p>
     * <p>
     * 变更判定规则：INSERT 记录全部非空新值（仅当 {@link DataChangeRecord#recordInsert()} 打开时才会走到这里）；
     * DELETE 记录全部非空旧值；
     * UPDATE 仅记录"新值非空且与旧值不等"的字段（对齐 MyBatis-Plus updateById 只更新非空字段的语义）。
     * </p>
     *
     * @param entityClass   实体类
     * @param annotation    变更记录注解
     * @param operationType 操作类型（落库存储其 {@link OperationType#getValue()}，与字典 sys_oper_type 对齐）
     * @param oldEntity     原数据（可为 {@code null}）
     * @param newEntity     新数据（可为 {@code null}）
     * @return 变更记录列表，不会为 {@code null}
     */
    protected List<DataChangeRecordEntity> buildChangeRecords(Class<?> entityClass, DataChangeRecord annotation,
                                                            OperationType operationType, T oldEntity, T newEntity) {
        List<DataChangeRecordEntity> records = new ArrayList<>();
        String tableName = annotation.tableName();
        if (tableName == null || tableName.isEmpty()) {
            tableName = MybatisPlusUtils.getTableName(entityClass);
        }
        String dataId = newEntity != null ? getEntityId(newEntity) : (oldEntity != null ? getEntityId(oldEntity) : null);
        String md5Field = resolveMd5Field(entityClass);
        Set<String> auditFields = resolveAuditExcludedFields(annotation);
        int maxValueLength = resolveMaxChangeValueLength(annotation);

        List<Field> fields = getAllFields(entityClass);
        for (Field field : fields) {
            if (isSkippedChangeField(field, md5Field, auditFields)) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object oldRawValue = oldEntity != null ? field.get(oldEntity) : null;
                Object newRawValue = newEntity != null ? field.get(newEntity) : null;
                String oldComparisonValue = toChangeComparisonValue(field, oldRawValue);
                String newComparisonValue = toChangeComparisonValue(field, newRawValue);
                if (!isChanged(operationType, oldComparisonValue, newComparisonValue)) {
                    continue;
                }

                DataChangeRecordEntity recordEntity = new DataChangeRecordEntity();
                recordEntity.setTableName(tableName);
                recordEntity.setTableDesc(annotation.tableDesc());
                recordEntity.setDataId(dataId);
                recordEntity.setOperationType(operationType.getValue());
                recordEntity.setFieldName(field.getName());
                recordEntity.setFieldDesc(resolveChangeFieldDesc(field));
                recordEntity.setFieldKey(field.getName());
                recordEntity.setOldValue(toChangeDisplayValue(field, oldRawValue, maxValueLength));
                recordEntity.setNewValue(toChangeDisplayValue(field, newRawValue, maxValueLength));
                recordEntity.setCreateTime(LocalDateTime.now());
                records.add(recordEntity);
            } catch (IllegalAccessException e) {
                log.error("记录数据变更失败", e);
            }
        }
        return records;
    }

    /**
     * 获取变更判定值。可逆加密字段先解密，避免随机 IV 让相同明文被误判为变化；
     * 不在这里截断，确保长字段仅尾部变化也能被识别。
     *
     * @param field 实体字段，用于读取加密配置
     * @param value 字段原始值，可为 {@code null}
     * @return 用于完整比较的业务值字符串；原值为 {@code null} 时返回 {@code null}
     */
    protected String toChangeComparisonValue(Field field, Object value) {
        Object plainValue = toPlainFieldValue(field, value);
        return plainValue == null ? null : String.valueOf(plainValue);
    }

    /**
     * 获取可写入审计的展示值。加密字段只保留非空标记，脱敏字段保存掩码，
     * 其他字段沿用 {@link #toChangeValue(Object, int)} 的序列化与截断规则。
     *
     * @param field          实体字段，用于读取加密与脱敏配置
     * @param value          字段原始值，可为 {@code null}
     * @param maxValueLength 审计值最大长度
     * @return 安全的审计展示值；原值为 {@code null} 时返回 {@code null}，非空加密值返回 {@code "***"}
     */
    protected String toChangeDisplayValue(Field field, Object value, int maxValueLength) {
        if (value == null) {
            return null;
        }
        Encrypt encrypt = field.getAnnotation(Encrypt.class);
        if (encrypt != null) {
            return "***";
        }
        Object plainValue = toPlainFieldValue(field, value);
        Sensitive sensitive = field.getAnnotation(Sensitive.class);
        if (sensitive != null) {
            String masked = SensitiveUtil.desensitize(String.valueOf(plainValue), sensitive.type(),
                    sensitive.rule(), sensitive.replaceChar());
            return toChangeValue(masked, maxValueLength);
        }
        return toChangeValue(plainValue, maxValueLength);
    }

    /**
     * 分发变更记录。事务存在时延迟到 {@code afterCommit}，回滚不会产生审计或日志；
     * 无事务时保持直接分发，便于离线调用与非事务场景使用。
     *
     * @param annotation 实体生效的变更记录配置
     * @param records    待分发记录；为 {@code null} 或空集合时直接返回
     */
    private void dispatchChangeRecords(DataChangeRecord annotation, List<DataChangeRecordEntity> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            List<DataChangeRecordEntity> committedRecords = new ArrayList<>(records);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatchChangeRecordsNow(annotation, committedRecords);
                }
            });
            return;
        }
        dispatchChangeRecordsNow(annotation, records);
    }

    /**
     * 立即分发一批变更记录。
     * <p>启用持久化且存在处理器时交由业务处理器接管，并隔离处理器异常；
     * 否则仅输出汇总日志，不逐条输出敏感字段值。</p>
     *
     * @param annotation 实体生效的变更记录配置
     * @param records    待分发记录，调用前须确认非空
     */
    private void dispatchChangeRecordsNow(DataChangeRecord annotation, List<DataChangeRecordEntity> records) {
        DataChangeRecordHandler handler = changeRecordHandlerProvider != null
                ? changeRecordHandlerProvider.getIfAvailable() : null;
        if (annotation.persistent() && handler != null) {
            try {
                handler.handle(records);
            } catch (Exception e) {
                log.error("数据变更记录处理失败: table={}", records.get(0).getTableName(), e);
            }
            return;
        }
        DataChangeRecordEntity first = records.get(0);
        log.info("数据变更记录: table={}, dataId={}, operationType={}, 变更字段数={}",
                first.getTableName(), first.getDataId(), first.getOperationType(), records.size());
    }

    /**
     * 判断字段值是否构成一次需要记录的变更。
     *
     * @param operationType 操作类型
     * @param oldValue      旧值（已转字符串）
     * @param newValue      新值（已转字符串）
     * @return true 表示需要记录
     */
    private boolean isChanged(OperationType operationType, String oldValue, String newValue) {
        if (OperationType.INSERT == operationType) {
            return newValue != null;
        }
        if (OperationType.DELETE == operationType) {
            return oldValue != null;
        }
        // UPDATE：仅记录新值非空且与旧值不等的字段
        return newValue != null && !newValue.equals(oldValue);
    }

    /**
     * 字段是否无需记录（静态 / 非持久化 / 主键 / 审计字段 / MD5 派生字段）。
     *
     * @param field       字段对象
     * @param md5Field    {@link DataIntegrity} 声明的 MD5 字段名，可为 {@code null}
     * @param auditFields 本实体生效的审计字段排除集合，见 {@link #resolveAuditExcludedFields}
     * @return true 表示跳过
     */
    protected boolean isSkippedChangeField(Field field, String md5Field, Set<String> auditFields) {
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
            return true;
        }
        TableField tableField = field.getAnnotation(TableField.class);
        if (tableField != null && !tableField.exist()) {
            return true;
        }
        String name = field.getName();
        if (field.isAnnotationPresent(TableId.class) || "id".equals(name)) {
            return true;
        }
        if (auditFields.contains(name)) {
            return true;
        }
        return md5Field != null && md5Field.equals(name);
    }

    /**
     * 解析变更记录中的字段描述（{@code field_desc}），默认取 Java 字段名。
     * <p>业务需要中文标签或列注释时在子类覆写本方法；{@code field_name} 始终为 Java 字段名，
     * 供程序匹配使用，不受本方法影响。</p>
     *
     * @param field 实体字段
     * @return 字段描述
     */
    protected String resolveChangeFieldDesc(Field field) {
        return field.getName();
    }

    /**
     * 解析实体生效的审计字段排除集合：{@link DataChangeRecord#excludeAuditFields()} 非空时整体替换，
     * 未配置时用框架默认集合。
     *
     * @param annotation 变更记录注解
     * @return 排除的字段名集合，不会为 {@code null}
     */
    protected Set<String> resolveAuditExcludedFields(DataChangeRecord annotation) {
        String[] configured = annotation.excludeAuditFields();
        if (configured == null || configured.length == 0) {
            return DEFAULT_CHANGE_RECORD_AUDIT_FIELDS;
        }
        Set<String> auditFields = new HashSet<>(configured.length * 2);
        for (String field : configured) {
            if (field != null && !field.trim().isEmpty()) {
                auditFields.add(field.trim());
            }
        }
        return auditFields;
    }

    /**
     * 解析字段值截断长度：注解配置值非正数时退回框架默认长度。
     *
     * @param annotation 变更记录注解
     * @return 截断长度
     */
    protected int resolveMaxChangeValueLength(DataChangeRecord annotation) {
        return annotation.maxValueLength() > 0 ? annotation.maxValueLength() : DEFAULT_MAX_CHANGE_VALUE_LENGTH;
    }

    /**
     * 解析 {@link DataIntegrity} 声明的 MD5 字段名。
     *
     * @param entityClass 实体类
     * @return MD5 字段名；未启用完整性注解时返回 {@code null}
     */
    private String resolveMd5Field(Class<?> entityClass) {
        DataIntegrity integrity = entityClass.getAnnotation(DataIntegrity.class);
        if (integrity == null || !integrity.enable()) {
            return null;
        }
        return resolveMd5FieldName(entityClass, integrity.md5Field());
    }

    /**
     * 字段值转字符串（null 安全 + 超长截断）。
     *
     * @param value          字段值
     * @param maxValueLength 截断长度，见 {@link #resolveMaxChangeValueLength(DataChangeRecord)}
     * @return 字符串形式；入参为 {@code null} 时返回 {@code null}
     */
    protected String toChangeValue(Object value, int maxValueLength) {
        if (value == null) {
            return null;
        }
        String str = String.valueOf(value);
        return str.length() > maxValueLength ? str.substring(0, maxValueLength) : str;
    }

    /**
     * 计算实体的 MD5（仅使用框架默认排除规则）。
     *
     * @param entity 实体对象
     * @return MD5 字符串
     * @throws Exception 计算过程中发生异常时抛出
     */
    protected String calculateMd5(T entity) throws Exception {
        return calculateMd5(entity, null);
    }

    /**
     * 根据实体的持久化字段计算 MD5。
     * <p>字段按 Java 字段名排序，字段名和值均使用长度前缀编码，避免分隔符或反射顺序影响结果。
     * 主键、自动填充字段、不可查询字段、static/transient/非持久化字段及框架审计字段不参与摘要。</p>
     *
     * @param entity        实体对象
     * @param excludeFields 额外排除的 Java 字段名（多个用逗号分隔）
     * @return MD5 字符串
     * @throws Exception 计算过程中发生异常时抛出
     */
    protected String calculateMd5(T entity, String excludeFields) throws Exception {
        return doCalculateMd5(null, entity, excludeFields);
    }

    /**
     * 根据更新后的完整持久化快照计算 MD5。
     * <p>默认以新实体的非空字段覆盖旧实体；{@code updateStrategy=ALWAYS} 的字段即使为
     * {@code null} 也使用新值，与 MyBatis-Plus 更新策略保持一致。</p>
     *
     * @param oldEntity     数据库旧实体
     * @param newEntity     待更新实体
     * @param excludeFields 额外排除的 Java 字段名
     * @return MD5 字符串
     * @throws Exception 计算过程中发生异常时抛出
     */
    protected String calculateMd5ForUpdate(T oldEntity, T newEntity, String excludeFields) throws Exception {
        if (oldEntity == null) {
            return calculateMd5(newEntity, excludeFields);
        }
        return doCalculateMd5(oldEntity, newEntity, excludeFields);
    }

    /**
     * 按字段名稳定排序并计算持久化快照的 MD5。
     * <p>存在旧实体时，新实体中为 {@code null} 且非强制更新的字段继承旧值；
     * 字段名和值均使用长度前缀编码，避免拼接歧义。</p>
     *
     * @param oldEntity     数据库旧实体；新增时传 {@code null}
     * @param newEntity     待写库实体
     * @param excludeFields 额外排除的 Java 字段名，多个字段用逗号分隔
     * @return MD5 字符串
     * @throws Exception 反射读取字段或摘要计算失败时抛出
     */
    private String doCalculateMd5(T oldEntity, T newEntity, String excludeFields) throws Exception {
        Set<String> excludes = new HashSet<>();
        if (excludeFields != null) {
            for (String fieldName : excludeFields.split(",")) {
                if (fieldName != null && !fieldName.trim().isEmpty()) {
                    excludes.add(fieldName.trim());
                }
            }
        }

        List<Field> fields = getAllFields(newEntity.getClass());
        fields.sort(Comparator.comparing(Field::getName));
        StringBuilder content = new StringBuilder();
        for (Field field : fields) {
            if (!isMd5IncludedField(field, excludes)) {
                continue;
            }
            Object newValue = field.get(newEntity);
            Object value = newValue;
            if (oldEntity != null && newValue == null && !isAlwaysUpdateField(field)) {
                value = field.get(oldEntity);
            }
            appendMd5Token(content, field.getName());
            appendMd5Token(content, toMd5Value(value));
        }
        return EncryptUtil.encrypt(content.toString(), EncryptType.MD5, null);
    }

    /**
     * 判断字段是否配置为即使值为 {@code null} 也参与 UPDATE。
     *
     * @param field 实体字段
     * @return {@code @TableField(updateStrategy=ALWAYS)} 时返回 {@code true}
     */
    private boolean isAlwaysUpdateField(Field field) {
        TableField tableField = field.getAnnotation(TableField.class);
        return tableField != null && tableField.updateStrategy() == FieldStrategy.ALWAYS;
    }

    /**
     * 判断字段是否属于可稳定读取的持久化摘要字段。
     *
     * @param field    实体字段
     * @param excludes 额外排除的 Java 字段名
     * @return true 表示参与 MD5
     */
    protected boolean isMd5IncludedField(Field field, Set<String> excludes) {
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
            return false;
        }
        String fieldName = field.getName();
        if (excludes.contains(fieldName) || DEFAULT_CHANGE_RECORD_AUDIT_FIELDS.contains(fieldName)
                || field.isAnnotationPresent(TableId.class) || "id".equals(fieldName)) {
            return false;
        }
        TableField tableField = field.getAnnotation(TableField.class);
        return tableField == null || tableField.exist() && tableField.select() && tableField.fill() == FieldFill.DEFAULT;
    }

    /**
     * 将字段值转换为稳定的摘要文本。业务自定义持久化类型可覆写本方法提供规范序列化。
     *
     * @param value 字段值
     * @return 规范文本；{@code null} 保持为 {@code null}
     */
    protected String toMd5Value(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).stripTrailingZeros().toPlainString();
        }
        if (value instanceof Double || value instanceof Float) {
            double number = ((Number) value).doubleValue();
            if (!Double.isNaN(number) && !Double.isInfinite(number)) {
                return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
            }
        } else if (value instanceof Number) {
            try {
                return new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
            } catch (NumberFormatException ignored) {
                return value.toString();
            }
        }
        if (value instanceof Date) {
            return String.valueOf(((Date) value).getTime());
        }
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }
        if (value.getClass().isArray()) {
            List<Object> items = new ArrayList<>();
            for (int i = 0, length = Array.getLength(value); i < length; i++) {
                items.add(Array.get(value, i));
            }
            return toMd5Sequence(items);
        }
        if (value instanceof Collection) {
            return toMd5Sequence((Collection<?>) value);
        }
        if (value instanceof Map) {
            List<String> entries = new ArrayList<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                StringBuilder item = new StringBuilder();
                appendMd5Token(item, toMd5Value(entry.getKey()));
                appendMd5Token(item, toMd5Value(entry.getValue()));
                entries.add(item.toString());
            }
            Collections.sort(entries);
            return toMd5Sequence(entries);
        }
        return String.valueOf(value);
    }

    /**
     * 按集合迭代顺序拼接元素的规范化摘要文本。
     *
     * @param values 待编码元素集合
     * @return 各元素经长度前缀编码后的拼接结果
     */
    private String toMd5Sequence(Collection<?> values) {
        StringBuilder result = new StringBuilder();
        for (Object value : values) {
            appendMd5Token(result, toMd5Value(value));
        }
        return result.toString();
    }

    /**
     * 向摘要内容追加一个长度前缀令牌，{@code null} 使用独立标记以区别于空字符串。
     *
     * @param target 摘要内容构建器
     * @param value  待编码文本，可为 {@code null}
     */
    private static void appendMd5Token(StringBuilder target, String value) {
        if (value == null) {
            target.append("-1:");
        } else {
            target.append(value.length()).append(':').append(value);
        }
    }

    /**
     * 获取实体主键值，主键属性优先取 MyBatis-Plus TableInfo，其次识别 {@link TableId}，最后回退到 "id"。
     *
     * @param entity 实体对象
     * @return 主键值字符串；不存在或为 {@code null} 时返回 {@code null}
     */
    protected String getEntityId(T entity) {
        return getEntityId(entity, resolvePrimaryKeyProperty());
    }

    /**
     * 解析主键数据库列名，用于框架内部稳定排序。
     *
     * @return 主键列名；无法解析实体元数据时回退到 "id"
     */
    protected String resolvePrimaryKeyColumn() {
        Class<T> entityType = getEntityClassType();
        TableInfo tableInfo = entityType == null ? null : TableInfoHelper.getTableInfo(entityType);
        if (tableInfo != null && tableInfo.getKeyColumn() != null && !tableInfo.getKeyColumn().isEmpty()) {
            return tableInfo.getKeyColumn();
        }
        Field idField = resolvePrimaryKeyField(entityType);
        if (idField != null) {
            TableId tableId = idField.getAnnotation(TableId.class);
            if (tableId != null && !tableId.value().isEmpty()) {
                return tableId.value();
            }
            TableField tableField = idField.getAnnotation(TableField.class);
            if (tableField != null && !tableField.value().isEmpty()) {
                return tableField.value();
            }
            return camelToUnderline(idField.getName());
        }
        return "id";
    }

    /**
     * 解析实体主键对应的 Java 属性名。
     *
     * @return MyBatis-Plus 元数据中的主键属性；无元数据时读取 {@link TableId} 字段，最终回退到 {@code "id"}
     */
    private String resolvePrimaryKeyProperty() {
        Class<T> entityType = getEntityClassType();
        TableInfo tableInfo = entityType == null ? null : TableInfoHelper.getTableInfo(entityType);
        if (tableInfo != null && tableInfo.getKeyProperty() != null && !tableInfo.getKeyProperty().isEmpty()) {
            return tableInfo.getKeyProperty();
        }
        Field idField = resolvePrimaryKeyField(entityType);
        return idField == null ? "id" : idField.getName();
    }

    /**
     * 沿实体父类链查找标注 {@link TableId} 的主键字段，子类同名字段优先。
     *
     * @param entityType 实体类型，可为 {@code null}
     * @return 主键字段；类型为 {@code null} 或未标注 {@link TableId} 时返回 {@code null}
     */
    private Field resolvePrimaryKeyField(Class<?> entityType) {
        if (entityType == null) {
            return null;
        }
        for (Field field : getAllFields(entityType)) {
            if (field.getAnnotation(TableId.class) != null) {
                return field;
            }
        }
        return null;
    }

    /**
     * 获取实体主键值。
     * <p>主键可能为非 String 类型，使用 {@link String#valueOf(Object)} 转换；值为 {@code null} 时返回 {@code null}。</p>
     *
     * @param entity      实体对象
     * @param idFieldName 主键字段名称；为 {@code null}/空/空白时使用 "id"
     * @return 主键值字符串；字段不存在或值为 {@code null} 时返回 {@code null}
     */
    protected String getEntityId(T entity, String idFieldName) {
        try {
            if (idFieldName == null || idFieldName.isEmpty() || idFieldName.trim().isEmpty()) {
                idFieldName = "id";
            }
            Field idField = findField(entity.getClass(), idFieldName);
            if (idField != null) {
                idField.setAccessible(true);
                Object value = idField.get(entity);
                return value != null ? String.valueOf(value) : null;
            }
        } catch (Exception e) {
            log.error("获取实体ID失败", e);
        }
        return null;
    }

    /* ================================================================================
     * 生命周期钩子：子类按需重写。重写带默认实现的钩子（beforeAdd / afterAdd /
     * beforeUpdate / afterDelete / afterDeleteBatch）时若仍需加密与数据变更记录，要调用 super。
     *
     *   findList / findOne   beforeQuery → SQL → processResultList → afterQuery
     *   findPage             beforeQuery → SQL → processResultList → afterQuery → processPageResult
     *   findById             无钩子（不构建 Wrapper，只做内联解密/脱敏/字典）
     *   addEntity / addBatch beforeAdd → 写库 → afterAdd
     *   updateEntity         beforeUpdate(old, new) → 写库 → afterUpdate
     *   saveOrUpdateEntity   透传到 addEntity / updateEntity，钩子同上
     *   removeById           beforeDelete → 删库 → afterDelete
     *   removeByIds          beforeDeleteBatch → 删库 → afterDeleteBatch
     *
     * 业务后置钩子在 SQL 成功后、当前事务提交前执行，不代表 afterCommit；
     * 仅数据变更记录的分发会延迟到事务提交后，回滚不分发。
     ================================================================================ */

    /**
     * 查询前置钩子：注解条件构建完成后、执行 SQL 前调用，子类可往 Wrapper 追加条件。
     * <p>
     * 数据范围、跨表子查询这类需要 {@code inSql / exists / apply} 的条件在这里由后端构造，
     * 规则见 {@link com.dream.common.data.annotation.QueryCondition} 的说明。
     * </p>
     *
     * @param queryWrapper 已按注解白名单构建好的 Wrapper，可直接追加条件
     * @param query        原始查询条件实体（分页时为 {@code params}），可能为 {@code null}
     */
    protected void beforeQuery(QueryWrapper<T> queryWrapper, T query) {
        // 子类可重写：追加后端构造的查询条件
    }

    /**
     * 查询后置钩子：框架解密/脱敏/字典处理完成后、结果返回调用方前调用。
     * <p>
     * 与 {@link #processPageResult} 的分工：本钩子对列表与分页两条路径都生效、入参统一是记录集合，
     * 适合「批量补关联名称」这类与分页无关的加工；{@code processPageResult} 只在分页路径触发且能拿到分页参数。
     * </p>
     *
     * @param records 待返回的记录集合（分页时为当前页记录），可直接原地修改
     * @param query   原始查询条件实体，可能为 {@code null}
     */
    protected void afterQuery(List<T> records, T query) {
        // 子类可重写：对查询结果做业务加工
    }

    /**
     * 分页结果后处理钩子：{@link #afterQuery} 之后调用，子类可改 total、追加聚合字段等。
     *
     * @param page      分页结果
     * @param pageQuery 原始分页查询参数
     */
    @Override
    public void processPageResult(IPage<T> page, CommonPageQuery<T> pageQuery) {
        // 子类可重写此方法实现自定义处理
    }

    /**
     * 新增前钩子：默认执行字段加密。
     *
     * @param entity 待新增实体
     */
    protected void beforeAdd(T entity) {
        encryptEntity(entity);
    }

    /**
     * 新增后钩子：默认仅在 {@link DataChangeRecord#recordInsert()} 为 {@code true} 时记录 INSERT 数据变更。
     * <p>{@link #addEntity} 与 {@link #addBatch} 均会触发；批量新增时逐条调用。</p>
     *
     * @param entity 已新增实体
     */
    protected void afterAdd(T entity) {
        recordDataChange(entity, OperationType.INSERT, null, entity);
    }

    /**
     * 更新前钩子：默认还原脱敏回显并对新值加密。
     * <p>UPDATE 变更记录由 {@link #updateEntity} 在确认影响行数后统一采集，
     * 避免后续业务校验失败或更新影响 0 行时留下假记录。</p>
     *
     * @param oldEntity 原数据
     * @param newEntity 新数据
     */
    protected void beforeUpdate(T oldEntity, T newEntity) {
        restoreDesensitizedFields(oldEntity, newEntity);
        encryptEntity(newEntity);
    }

    /**
     * 更新前还原脱敏字段，防止把查询结果里的掩码值写回数据库。
     * <p>使用旧明文按字段规则计算真实脱敏结果，只有新值与该结果精确相等时才判定为回显；
     * 还原的是旧明文，后续加密流程会且只会再加密一次。旧值本身包含占位符或客户端合法提交
     * 含占位符的其他值，均不影响判断。</p>
     *
     * @param oldEntity 数据库原数据（加密字段尚未解密）
     * @param newEntity 待修改新数据
     */
    protected void restoreDesensitizedFields(T oldEntity, T newEntity) {
        if (oldEntity == null || newEntity == null) {
            return;
        }
        for (Field field : getAllFields(newEntity.getClass())) {
            Sensitive sensitive = field.getAnnotation(Sensitive.class);
            if (sensitive == null || !sensitive.ignoreOnUpdate()) {
                continue;
            }
            String replaceChar = sensitive.replaceChar();
            if (replaceChar == null || replaceChar.isEmpty()) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object newValue = field.get(newEntity);
                Object oldValue = field.get(oldEntity);
                if (!(newValue instanceof String) || !(oldValue instanceof String)) {
                    continue;
                }
                Encrypt encrypt = field.getAnnotation(Encrypt.class);
                if (encrypt != null && encrypt.type() == EncryptType.MD5) {
                    continue;
                }
                String oldPlainText = (String) toPlainFieldValue(field, oldValue);
                String expectedMasked = SensitiveUtil.desensitize(
                        oldPlainText, sensitive.type(), sensitive.rule(), replaceChar);
                if (newValue.equals(expectedMasked)) {
                    field.set(newEntity, oldPlainText);
                }
            } catch (IllegalAccessException e) {
                log.error("还原脱敏字段失败", e);
            }
        }
    }

    /**
     * 将字段持久化值转换为业务明文值。
     * <p>普通字段和 MD5 单向摘要字段保持原值；可逆加密字段按字段注解解密。</p>
     *
     * @param field 实体字段，用于读取加密配置
     * @param value 字段原始值，可为 {@code null}
     * @return 解密后的业务值或原始值；入参为 {@code null} 时返回 {@code null}
     */
    private Object toPlainFieldValue(Field field, Object value) {
        if (value == null) {
            return null;
        }
        Encrypt encrypt = field.getAnnotation(Encrypt.class);
        if (encrypt == null || encrypt.type() == EncryptType.MD5) {
            return value;
        }
        return EncryptUtil.decrypt(value.toString(), encrypt.type(), encrypt.key());
    }

    /**
     * 更新后钩子：默认空实现。
     *
     * @param entity 已更新实体
     */
    protected void afterUpdate(T entity) {
        // 子类可重写：更新成功后的业务处理
    }

    /**
     * 删除前钩子：原数据已查出、尚未删除时调用；ID 查不到数据时不触发。
     *
     * @param entity 待删除的原数据
     */
    protected void beforeDelete(T entity) {
        // 子类可重写：删除前的校验、关联数据清理等
    }

    /**
     * 删除后钩子：删除成功后调用，默认记录 DELETE 数据变更。
     *
     * @param entity 已删除的原数据
     */
    protected void afterDelete(T entity) {
        recordDataChange(entity, OperationType.DELETE, entity, null);
    }

    /**
     * 批量删除前钩子：原数据已批量查出、尚未删除时调用；默认空实现。
     *
     * @param entities 待删除的原数据集合，非空
     */
    protected void beforeDeleteBatch(List<T> entities) {
        // 子类可重写：批量删除前的业务处理
    }

    /**
     * 批量删除后钩子：删除成功后调用，默认合并全部实体的 DELETE 变更记录后一次性分发。
     *
     * @param entities 已删除的原数据集合，非空
     */
    protected void afterDeleteBatch(List<T> entities) {
        DataChangeRecord annotation = resolveChangeRecordAnnotation(entities.get(0).getClass());
        if (annotation == null) {
            return;
        }
        List<DataChangeRecordEntity> allRecords = new ArrayList<>();
        for (T oldEntity : entities) {
            allRecords.addAll(buildChangeRecords(oldEntity.getClass(), annotation, OperationType.DELETE, oldEntity, null));
        }
        dispatchChangeRecords(annotation, allRecords);
    }

}
