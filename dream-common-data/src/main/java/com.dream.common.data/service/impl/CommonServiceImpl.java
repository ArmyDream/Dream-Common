package com.dream.common.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;

import com.dream.common.core.annotation.Dict;
import com.dream.common.core.exception.BusinessException;
import com.dream.common.data.annotation.DataChangeRecord;
import com.dream.common.data.annotation.DataIntegrity;
import com.dream.common.data.annotation.Encrypt;
import com.dream.common.data.annotation.QueryCondition;
import com.dream.common.data.annotation.Sensitive;
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
import com.dream.common.tool.utils.dict.DictUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
 * </ul>
 *
 * @param <M> Mapper 类型，必须继承 {@link CommonMapper}
 * @param <T> 实体类型
 * @author dream
 */
@Slf4j
public abstract class CommonServiceImpl<M extends CommonMapper<T>, T> extends ServiceImpl<M, T> implements CommonService<T> {

    /**
     * 当无法获取到当前操作人时使用的默认值
     */
    private static final String DEFAULT_OPERATOR = "system";

    /**
     * 数据变更记录 Mapper。
     * <p>
     * 通过构造器注入，若子类构造时未传入（传 {@code null}），则该字段为 {@code null}，
     * 此时 {@link #recordDataChange} 不会进行持久化，仅跳过变更记录写入。
     * </p>
     */
    private final CommonMapper<DataChangeRecordEntity> dataChangeRecordMapper;

    /**
     * 实体类类型缓存
     */
    private final Map<String, Class<T>> entityClassTypeCache = new ConcurrentHashMap<>();
    private Class<T> entityClass;

    /**
     * 构造通用 Service 实现。
     * <p>
     * 子类可选择是否传入数据变更记录 Mapper。若传入 {@code null}，
     * 则 {@link #dataChangeRecordMapper} 为 {@code null}，数据变更记录功能将不生效（不会持久化）。
     * </p>
     *
     * @param dataChangeRecordMapper 数据变更记录 Mapper，可为 {@code null}
     */
    protected CommonServiceImpl(@Nullable CommonMapper<DataChangeRecordEntity> dataChangeRecordMapper) {
        this.dataChangeRecordMapper = dataChangeRecordMapper;
    }

    /**
     * 获取实体类类型（解析子类继承时声明的泛型参数）。
     * <p>
     * 结果会按子类全限定名缓存，避免重复反射解析。
     * </p>
     *
     * @return 实体类 Class；若无法解析返回 Object.class
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getEntityClassType() {
        if (entityClass != null) {
            return entityClass;
        }

        String key = this.getClass().getName();
        if (!entityClassTypeCache.containsKey(key)) {
            entityClass = (Class<T>) MybatisPlusUtils.getClassByType(this.getClass(), 1);
            entityClassTypeCache.put(key, entityClass);
        }
        return entityClassTypeCache.get(key);
    }

    /**
     * 根据条件查询单个实体。
     * <p>内部调用 {@link #findList}，返回首条记录，无结果返回 {@code null}。</p>
     *
     * @param query 查询条件实体
     * @return 匹配的首条实体；无匹配返回 {@code null}
     */
    @Override
    public T findOne(T query) {
        List<T> list = findList(query);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 根据条件查询实体列表。
     * <p>内部使用 {@link #buildQueryWrapper} 构建条件，并对结果进行解密/脱敏/字典处理。</p>
     *
     * @param query 查询条件实体，为 {@code null} 时返回全量
     * @return 匹配的实体列表，不会为 {@code null}
     */
    @Override
    public List<T> findList(T query) {
        QueryWrapper<T> queryWrapper = buildQueryWrapper(query);
        List<T> list = baseMapper.selectList(queryWrapper);
        processResultList(list);
        return list;
    }

    /**
     * 分页查询。
     * <p>支持排序、自动构建查询条件，并对分页结果进行解密/脱敏/字典处理，
     * 最后调用 {@link #processPageResult} 供子类做后处理。</p>
     *
     * @param pageQuery 分页查询参数（含查询条件与排序字段）
     * @return 分页结果
     */
    @Override
    public IPage<T> findPage(CommonPageQuery<T> pageQuery) {
        Page<T> page = pageQuery.toPage();
        QueryWrapper<T> queryWrapper = buildQueryWrapper(pageQuery.getParams());

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
                queryWrapper.orderBy(true, orderField.isAsc(), column);
            }
        }

        IPage<T> resultPage = baseMapper.selectPage(page, queryWrapper);

        // 对查询结果进行后续处理(解密、脱敏等)
        processResultList(resultPage.getRecords());

        // 调用分页结果后处理方法
        processPageResult(resultPage, pageQuery);

        return resultPage;
    }

    /**
     * 分页查询结果后处理钩子，子类可重写以实现自定义逻辑。
     *
     * @param page      分页结果
     * @param pageQuery 原始分页查询参数
     */
    @Override
    public void processPageResult(IPage<T> page, CommonPageQuery<T> pageQuery) {
        // 子类可重写此方法实现自定义处理
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
        // 新增前处理
        beforeAdd(entity);

        // 计算数据完整性MD5
        calculateAndSetDataMd5(entity);

        // 执行新增
        baseMapper.insert(entity);

        // 新增后处理
        afterAdd(entity);

        // 查询返回完整数据
        return findById(getEntityId(entity));
    }

    /**
     * 批量新增实体。
     * <p>
     * 相较于循环调用 {@link #addEntity}，本方法仅做一次前置处理循环（加密、MD5 计算），
     * 再通过 MyBatis-Plus 的 {@link #saveBatch} 进行批量插入，避免逐条插入与逐条回查的开销。
     * </p>
     *
     * @param entityList 待新增实体列表
     * @return 新增后的实体列表（含已生成的主键），不会为 {@code null}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<T> addBatch(List<T> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return new ArrayList<>();
        }
        // 前置处理：加密、MD5 计算
        for (T entity : entityList) {
            beforeAdd(entity);
            calculateAndSetDataMd5(entity);
        }
        // 批量插入
        saveBatch(entityList);
        return entityList;
    }

    /**
     * 修改实体（带前置/后置处理）。
     * <p>
     * 会先校验主键 ID 是否存在、原数据是否存在，并对原数据做完整性校验；
     * 前置处理包括字段加密、MD5 计算与变更记录。
     * </p>
     *
     * @param entity 待修改实体，必须包含主键 ID
     * @return 修改后查询出的完整实体（已解密/脱敏/字典处理）
     * @throws RuntimeException 当实体缺少 ID 或原数据不存在时抛出（并记录日志）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public T updateEntity(T entity) {
        String id = getEntityId(entity);
        if (id == null) {
            log.error("更新操作必须包含ID，实体类型：{}", entity.getClass().getName());
            throw new RuntimeException("更新操作必须包含ID");
        }

        // 查询原数据
        T oldEntity = baseMapper.selectById(id);
        if (oldEntity == null) {
            log.error("数据不存在，ID：{}", id);
            throw new RuntimeException("数据不存在");
        }

        // 检查数据完整性（可选）
        checkDataIntegrityBeforeUpdate(oldEntity);

        // 更新前处理(记录变更)
        beforeUpdate(oldEntity, entity);

        // 计算新的数据完整性MD5
        calculateAndSetDataMd5(entity);

        // 执行更新
        baseMapper.updateById(entity);

        // 更新后处理
        afterUpdate(entity);

        // 查询返回完整数据
        return findById(id);
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
        String id = getEntityId(entity);
        if (id == null || id.isEmpty()) {
            // ID不存在，执行新增
            return addEntity(entity);
        } else {
            // 检查ID是否存在
            T existing = baseMapper.selectById(id);
            if (existing == null) {
                // ID不存在，执行新增
                return addEntity(entity);
            } else {
                // ID存在，执行修改
                return updateEntity(entity);
            }
        }
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
     * 检查指定 ID 对应实体的数据完整性。
     * <p>
     * 通过对比存储的 MD5 与当前计算的 MD5 判断数据是否被篡改。
     * 计算过程中的异常会被捕获并返回 {@code false}；当检测到篡改且
     * {@link DataIntegrity#throwOnFailure()} 为 {@code true} 时，抛出
     * {@link BusinessException} 以通知调用方。
     * </p>
     *
     * @param id 主键 ID
     * @return {@code true} 表示完整/未启用校验/无 MD5；{@code false} 表示数据被篡改或校验过程异常
     * @throws BusinessException 当检测到篡改且配置为失败时抛出
     */
    @Override
    public boolean checkDataIntegrity(String id) {
        DataIntegrity integrity;
        String storedMd5;
        String currentMd5;
        try {
            Class<T> entityClass = getEntityClassType();
            // 查询当前数据
            T entity = baseMapper.selectById(id);
            if (entity == null) {
                return false;
            }

            integrity = entityClass.getAnnotation(DataIntegrity.class);
            if (integrity == null || !integrity.enable()) {
                return true; // 未启用完整性检查
            }

            // 获取存储的MD5值
            storedMd5 = getDataMd5FromEntity(entity, integrity.md5Field());
            if (storedMd5 == null || storedMd5.isEmpty()) {
                return true; // 没有MD5值，认为通过
            }

            // 计算当前MD5（排除MD5字段本身）
            currentMd5 = calculateMd5(entity, integrity.excludeFields() + "," + integrity.md5Field());
        } catch (Exception e) {
            log.error("检查数据完整性失败，ID：{}", id, e);
            return false;
        }

        boolean isValid = currentMd5.equals(storedMd5);

        if (!isValid && integrity.throwOnFailure()) {
            log.error("数据完整性校验失败，数据可能已被篡改，ID：{}", id);
            throw new BusinessException("数据完整性校验失败，数据可能已被篡改");
        }

        return isValid;
    }

    /**
     * 根据查询条件实体构建 MyBatis-Plus QueryWrapper。
     * <p>
     * 扫描查询对象（含父类）所有带 {@link QueryCondition} 注解的字段，
     * 根据注解配置的 {@link QueryType} 构建对应的查询条件。
     * </p>
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
                if (value == null) {
                    continue;
                }

                String column = getColumnName(field, condition);
                QueryType type = condition.type();

                // 处理字符串切割为IN查询
                if (condition.splitToIn() && value instanceof String) {
                    String[] values = ((String) value).split(condition.splitSeparator());
                    queryWrapper.in(column, (Object[]) values);
                    continue;
                }

                // 根据查询类型构建条件
                switch (type) {
                    case EQ:
                        queryWrapper.eq(column, value);
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
                    case IN:
                        if (value instanceof List) {
                            queryWrapper.in(column, (List<?>) value);
                        }
                        break;
                    case NOT_IN:
                        if (value instanceof List) {
                            queryWrapper.notIn(column, (List<?>) value);
                        }
                        break;
                    case BETWEEN:
                        // 值为 List 时取前两个元素作为范围；值为 String 时按逗号分割
                        if (value instanceof List) {
                            List<?> range = (List<?>) value;
                            if (range.size() >= 2) {
                                queryWrapper.between(column, range.get(0), range.get(1));
                            }
                        } else if (value instanceof String) {
                            String[] parts = ((String) value).split(condition.splitSeparator());
                            if (parts.length >= 2) {
                                queryWrapper.between(column, parts[0].trim(), parts[1].trim());
                            }
                        }
                        break;
                    case IS_NULL:
                        queryWrapper.isNull(column);
                        break;
                    case IS_NOT_NULL:
                        queryWrapper.isNotNull(column);
                        break;
                }
            } catch (IllegalAccessException e) {
                log.error("构建查询条件失败", e);
            }
        }

        return queryWrapper;
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
     * 新增前处理：对实体字段执行加密。
     *
     * @param entity 待新增实体
     */
    protected void beforeAdd(T entity) {
        // 加密处理
        encryptEntity(entity);
    }

    /**
     * 新增后处理：记录数据变更。
     *
     * @param entity 已新增实体
     */
    protected void afterAdd(T entity) {
        // 记录数据变更
        recordDataChange(entity, "INSERT", null, entity);
    }

    /**
     * 更新前处理：对新值执行加密，并记录字段级变更。
     *
     * @param oldEntity 原数据
     * @param newEntity 新数据
     */
    protected void beforeUpdate(T oldEntity, T newEntity) {
        // 加密处理
        encryptEntity(newEntity);

        // 记录变更
        recordDataChange(newEntity, "UPDATE", oldEntity, newEntity);
    }

    /**
     * 更新后处理钩子，子类可重写以实现自定义逻辑。
     *
     * @param entity 已更新实体
     */
    protected void afterUpdate(T entity) {
        // 后续处理
    }

    /**
     * 更新前检查原数据完整性。
     * <p>检查失败时仅记录日志，不阻断更新流程。</p>
     *
     * @param oldEntity 原数据
     */
    protected void checkDataIntegrityBeforeUpdate(T oldEntity) {
        try {
            String id = getEntityId(oldEntity);
            if (id != null) {
                checkDataIntegrity(id);
            }
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
        if (entity == null) {
            return;
        }

        DataIntegrity integrity = entity.getClass().getAnnotation(DataIntegrity.class);
        if (integrity == null || !integrity.enable() || !integrity.autoCalculate()) {
            return;
        }

        try {
            String md5 = calculateMd5(entity, integrity.excludeFields());
            setDataMd5ToEntity(entity, integrity.md5Field(), md5);
        } catch (Exception e) {
            log.error("计算数据完整性MD5失败", e);
        }
    }

    /**
     * 从实体中获取数据 MD5 值。
     *
     * @param entity   实体对象
     * @param md5Field MD5 字段名
     * @return MD5 值；字段不存在或读取失败返回 {@code null}
     */
    protected String getDataMd5FromEntity(T entity, String md5Field) {
        try {
            Field field = findField(entity.getClass(), md5Field);
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(entity);
                return value != null ? value.toString() : null;
            }
        } catch (Exception e) {
            log.error("获取数据MD5失败", e);
        }
        return null;
    }

    /**
     * 设置数据 MD5 值到实体指定字段。
     *
     * @param entity   实体对象
     * @param md5Field MD5 字段名
     * @param md5Value MD5 值
     */
    protected void setDataMd5ToEntity(T entity, String md5Field, String md5Value) {
        try {
            Field field = findField(entity.getClass(), md5Field);
            if (field != null) {
                field.setAccessible(true);
                field.set(entity, md5Value);
            }
        } catch (Exception e) {
            log.error("设置数据MD5失败", e);
        }
    }

    /**
     * 查找指定名称的字段（沿父类链向上查找，优先子类）。
     *
     * @param clazz     目标类
     * @param fieldName 字段名称
     * @return Field 对象；未找到返回 {@code null}
     */
    protected Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 获取类及其所有父类（不含 Object）声明的字段，子类同名字段优先。
     * <p>会调用 {@link Field#setAccessible(boolean)} 以便后续直接读写。</p>
     *
     * @param clazz 目标类
     * @return 字段列表；入参为 {@code null} 时返回空列表
     */
    protected List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        if (clazz == null) {
            return fields;
        }
        Set<String> seen = new HashSet<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (seen.add(field.getName())) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
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
     * 扫描实体（含父类）所有带 {@link Dict} 注解的字段，获取其值，
     * 调用 {@link DictUtils#getLabel(Dict, String)} 获取标签，并设置到目标字段
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

                String label = DictUtils.getLabel(dict, value.toString());
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
     * 处理结果列表：对每个实体依次执行解密、脱敏、字典处理。
     *
     * @param list 实体列表
     */
    protected void processResultList(List<T> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        for (T entity : list) {
            decryptEntity(entity);
            desensitizeEntity(entity);
            processDict(entity);
        }
    }

    /**
     * 记录数据变更。
     * <p>
     * 仅当实体类标注了 {@link DataChangeRecord} 注解且 {@link DataChangeRecord#enable()} 与
     * {@link DataChangeRecord#persistent()} 均为 {@code true}，且 {@link #dataChangeRecordMapper}
     * 不为 {@code null} 时，才会扫描实体（含父类）字段并记录前后值变化。
     * </p>
     *
     * @param entity        实体对象（用于读取类上的注解）
     * @param operationType 操作类型（INSERT/UPDATE/DELETE）
     * @param oldEntity     原数据（新增时可为 {@code null}）
     * @param newEntity     新数据（删除时可为 {@code null}）
     */
    protected void recordDataChange(T entity, String operationType, T oldEntity, T newEntity) {
        DataChangeRecord record = entity.getClass().getAnnotation(DataChangeRecord.class);
        if (record == null || !record.enable()) {
            return;
        }

        if (!record.persistent() || dataChangeRecordMapper == null) {
            return;
        }

        String tableName = record.tableName();
        if (tableName == null || tableName.isEmpty()) {
            tableName = MybatisPlusUtils.getTableName(entity.getClass());
        }

        String dataId = getEntityId(newEntity);

        List<Field> fields = getAllFields(entity.getClass());
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                String oldValue = oldEntity != null ? String.valueOf(field.get(oldEntity)) : null;
                String newValue = newEntity != null ? String.valueOf(field.get(newEntity)) : null;

                // 如果值没有变化，不记录
                if (oldValue != null && oldValue.equals(newValue)) {
                    continue;
                }

                DataChangeRecordEntity recordEntity = new DataChangeRecordEntity();
                recordEntity.setTableName(tableName);
                recordEntity.setTableDesc(record.tableDesc());
                recordEntity.setDataId(dataId);
                recordEntity.setOperationType(operationType);
                recordEntity.setFieldName(field.getName());
                recordEntity.setFieldDesc(field.getName());
                recordEntity.setFieldKey("");
                recordEntity.setOldValue(oldValue);
                recordEntity.setNewValue(newValue);
                recordEntity.setCreateTime(LocalDateTime.now());

                dataChangeRecordMapper.insert(recordEntity);
            } catch (IllegalAccessException e) {
                log.error("记录数据变更失败", e);
            }
        }
    }

    /**
     * 计算实体的 MD5（不排除任何字段）。
     *
     * @param entity 实体对象
     * @return MD5 字符串
     * @throws Exception 计算过程中发生异常时抛出
     */
    protected String calculateMd5(T entity) throws Exception {
        return calculateMd5(entity, null);
    }

    /**
     * 计算实体的 MD5（排除指定字段）。
     * <p>扫描实体（含父类）所有字段，按字段名=字段值拼接后计算 MD5。</p>
     *
     * @param entity        实体对象
     * @param excludeFields 需排除的字段名（多个用逗号分隔），为 {@code null} 时不排除
     * @return MD5 字符串
     * @throws Exception 计算过程中发生异常时抛出
     */
    protected String calculateMd5(T entity, String excludeFields) throws Exception {
        StringBuilder sb = new StringBuilder();
        List<Field> fields = getAllFields(entity.getClass());

        String[] excludes = excludeFields != null ? excludeFields.split(",") : new String[0];

        for (Field field : fields) {
            // 检查是否排除
            boolean exclude = false;
            for (String e : excludes) {
                if (field.getName().equals(e.trim())) {
                    exclude = true;
                    break;
                }
            }
            if (exclude) {
                continue;
            }

            field.setAccessible(true);
            Object value = field.get(entity);
            sb.append(field.getName()).append("=").append(value).append("|");
        }

        return EncryptUtil.encrypt(sb.toString(), EncryptType.MD5, null);
    }

    /**
     * 获取实体主键值（默认字段名 "id"）。
     *
     * @param entity 实体对象
     * @return 主键值字符串；不存在或为 {@code null} 时返回 {@code null}
     */
    protected String getEntityId(T entity) {
        return getEntityId(entity, "id");
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

}
