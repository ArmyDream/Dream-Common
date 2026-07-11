package com.dream.common.core.enums;

/**
 * 比较运算符枚举
 * <p>
 * 完全对齐 MyBatis-Plus 条件构造器（Wrapper）的方法命名规范。
 * 参考：AbstractWrapper 中定义的所有条件方法
 * <p>
 * 使用场景：动态查询构建、规则引擎、前端查询条件解析等
 *
 */
public enum ConditionType implements BaseEnum<String> {

    // ==================== 基础比较 ====================

    /**
     * 等于（=）
     * MP 方法：eq
     */
    EQ("eq", "=", "等于"),

    /**
     * 不等于（!=）
     * MP 方法：ne
     */
    NE("ne", "!=", "不等于"),

    /**
     * 大于（>）
     * MP 方法：gt
     */
    GT("gt", ">", "大于"),

    /**
     * 大于等于（>=）
     * MP 方法：ge
     */
    GE("ge", ">=", "大于等于"),

    /**
     * 小于（<）
     * MP 方法：lt
     */
    LT("lt", "<", "小于"),

    /**
     * 小于等于（<=）
     * MP 方法：le
     */
    LE("le", "<=", "小于等于"),

    // ==================== 范围查询 ====================

    /**
     * 区间（BETWEEN ... AND ...）
     * MP 方法：between
     */
    BETWEEN("between", "BETWEEN", "区间"),

    /**
     * 不在区间（NOT BETWEEN ... AND ...）
     * MP 方法：notBetween
     */
    NOT_BETWEEN("notBetween", "NOT BETWEEN", "不在区间"),

    // ==================== 集合查询 ====================

    /**
     * 包含于（IN）
     * MP 方法：in
     */
    IN("in", "IN", "包含于"),

    /**
     * 不包含于（NOT IN）
     * MP 方法：notIn
     */
    NOT_IN("notIn", "NOT IN", "不包含于"),

    /**
     * IN 子查询
     * MP 方法：inSql
     */
    IN_SQL("inSql", "IN (SQL)", "IN子查询"),

    /**
     * NOT IN 子查询
     * MP 方法：notInSql
     */
    NOT_IN_SQL("notInSql", "NOT IN (SQL)", "NOT IN子查询"),

    // ==================== 模糊查询 ====================

    /**
     * 模糊匹配（%value%）
     * MP 方法：like
     */
    LIKE("like", "LIKE", "模糊匹配"),

    /**
     * 不包含（NOT LIKE）
     * MP 方法：notLike
     */
    NOT_LIKE("notLike", "NOT LIKE", "不包含"),

    /**
     * 左模糊（%value）
     * MP 方法：likeLeft
     */
    LIKE_LEFT("likeLeft", "LIKE LEFT", "左模糊"),

    /**
     * 右模糊（value%）
     * MP 方法：likeRight
     */
    LIKE_RIGHT("likeRight", "LIKE RIGHT", "右模糊"),

    /**
     * 不左模糊（NOT LIKE '%value'）
     * MP 方法：notLikeLeft
     */
    NOT_LIKE_LEFT("notLikeLeft", "NOT LIKE LEFT", "不左模糊"),

    /**
     * 不右模糊（NOT LIKE 'value%'）
     * MP 方法：notLikeRight
     */
    NOT_LIKE_RIGHT("notLikeRight", "NOT LIKE RIGHT", "不右模糊"),

    // ==================== 空值判断 ====================

    /**
     * 为空（IS NULL）
     * MP 方法：isNull
     */
    IS_NULL("isNull", "IS NULL", "为空"),

    /**
     * 非空（IS NOT NULL）
     * MP 方法：isNotNull
     */
    IS_NOT_NULL("isNotNull", "IS NOT NULL", "非空"),

    // ==================== 存在性查询（MP 3.5+） ====================

    /**
     * 存在（EXISTS）
     * MP 方法：exists
     */
    EXISTS("exists", "EXISTS", "存在"),

    /**
     * 不存在（NOT EXISTS）
     * MP 方法：notExists
     */
    NOT_EXISTS("notExists", "NOT EXISTS", "不存在"),

    // ==================== 🔥 补充：MP 中不存在的常用运算 ====================

    /**
     * 不等于（!=）— SQL 标准写法
     * 对应 MP 的 ne
     */
    NE_SQL("neSql", "<>", "不等于(标准)"),

    /**
     * 大于（>）— 别名
     */
    GT_ALIAS("gtAlias", ">", "大于(别名)"),

    /**
     * 以...开头（右模糊的明确语义）
     * 对应 MP 的 likeRight
     */
    STARTS_WITH("startsWith", "LIKE", "以...开头"),

    /**
     * 以...结尾（左模糊的明确语义）
     * 对应 MP 的 likeLeft
     */
    ENDS_WITH("endsWith", "LIKE", "以...结尾"),

    /**
     * 包含（模糊匹配的明确语义）
     * 对应 MP 的 like
     */
    CONTAINS("contains", "LIKE", "包含"),

    /**
     * 非空（IS NOT NULL）— 别名
     * 对应 MP 的 isNotNull
     */
    NOT_EMPTY("notEmpty", "IS NOT NULL", "非空(别名)");

    private final String value;
    private final String symbol;
    private final String label;

    ConditionType(String value, String symbol, String label) {
        this.value = value;
        this.symbol = symbol;
        this.label = label;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public String getSymbol() {
        return symbol;
    }

    // ==================== 业务判断方法 ====================

    /**
     * 是否为比较运算符（需要两个值的比较）
     */
    public boolean isComparison() {
        return this == EQ || this == NE || this == NE_SQL
                || this == GT || this == GT_ALIAS || this == GE
                || this == LT || this == LE;
    }

    /**
     * 是否为范围运算符（需要两个值）
     */
    public boolean isRange() {
        return this == BETWEEN || this == NOT_BETWEEN;
    }

    /**
     * 是否为集合运算符（需要集合值）
     */
    public boolean isCollection() {
        return this == IN || this == NOT_IN || this == IN_SQL || this == NOT_IN_SQL;
    }

    /**
     * 是否为模糊匹配运算符
     */
    public boolean isLike() {
        return this == LIKE || this == NOT_LIKE
                || this == LIKE_LEFT || this == LIKE_RIGHT
                || this == NOT_LIKE_LEFT || this == NOT_LIKE_RIGHT
                || this == STARTS_WITH || this == ENDS_WITH || this == CONTAINS;
    }

    /**
     * 是否为空判断
     */
    public boolean isNullCheck() {
        return this == IS_NULL || this == IS_NOT_NULL || this == NOT_EMPTY;
    }

    /**
     * 是否为存在性判断
     */
    public boolean isExists() {
        return this == EXISTS || this == NOT_EXISTS;
    }

    /**
     * 是否只需要一个值
     */
    public boolean isSingleValue() {
        return isComparison() || isNullCheck() || isExists()
                || this == LIKE || this == NOT_LIKE
                || this == LIKE_LEFT || this == LIKE_RIGHT
                || this == NOT_LIKE_LEFT || this == NOT_LIKE_RIGHT
                || this == STARTS_WITH || this == ENDS_WITH || this == CONTAINS;
    }

    /**
     * 是否只需要一个集合值
     */
    public boolean isCollectionValue() {
        return this == IN || this == NOT_IN;
    }

    /**
     * 是否只需要两个值（范围）
     */
    public boolean isTwoValues() {
        return this == BETWEEN || this == NOT_BETWEEN;
    }

    @Override
    public String getColor() {
        switch (this) {
            case EQ:
                return "#1890FF";
            case NE:
            case NE_SQL:
                return "#FF4D4F";
            case GT:
            case GT_ALIAS:
            case GE:
            case LT:
            case LE:
                return "#FAAD14";
            case BETWEEN:
            case NOT_BETWEEN:
                return "#13C2C2";
            case IN:
            case NOT_IN:
            case IN_SQL:
            case NOT_IN_SQL:
                return "#722ED1";
            case LIKE:
            case NOT_LIKE:
            case LIKE_LEFT:
            case LIKE_RIGHT:
            case NOT_LIKE_LEFT:
            case NOT_LIKE_RIGHT:
            case STARTS_WITH:
            case ENDS_WITH:
            case CONTAINS:
                return "#52C41A";
            case IS_NULL:
            case IS_NOT_NULL:
            case NOT_EMPTY:
                return "#8C8C8C";
            case EXISTS:
            case NOT_EXISTS:
                return "#FF7A45";
            default:
                return "#8C8C8C";
        }
    }

    // ==================== 静态方法 ====================

    public static ConditionType fromValue(String value) {
        return BaseEnum.fromValue(ConditionType.class, value);
    }

    public static ConditionType fromSymbol(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return null;
        }
        for (ConditionType operator : values()) {
            if (operator.symbol.equals(symbol)) {
                return operator;
            }
        }
        return null;
    }
}