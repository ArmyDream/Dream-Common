package com.dream.common.core.enums;

/**
 * 字符串池枚举
 * <p>
 * 统一管理项目中常用的字符和字符串常量
 * <p>
 * 注意：部分常量存在 value 相同的情况（如 DASH 和 MINUS 均为 "-"），
 * {@link #fromValue(String)} 方法会返回第一个匹配的枚举。
 * 如果需要精确匹配，请直接引用枚举常量而非使用 fromValue()。
 *
 */
public enum StringPool implements BaseEnum<String> {

    // ==================== 基础 ====================

    /**
     * 空字符串
     */
    EMPTY("", "空字符串"),

    /**
     * 空格
     */
    SPACE(" ", "空格"),

    // ==================== 标点符号 ====================

    /**
     * 逗号
     */
    COMMA(",", "逗号"),

    /**
     * 句号/点
     */
    DOT(".", "句号"),

    /**
     * 冒号
     */
    COLON(":", "冒号"),

    /**
     * 分号
     */
    SEMICOLON(";", "分号"),

    /**
     * 问号
     */
    QUESTION("?", "问号"),

    /**
     * 感叹号
     */
    EXCLAMATION("!", "感叹号"),

    /**
     * 下划线
     */
    UNDERSCORE("_", "下划线"),

    /**
     * 中划线/减号
     */
    DASH("-", "中划线"),

    /**
     * 斜杠
     */
    SLASH("/", "斜杠"),

    /**
     * 反斜杠
     */
    BACKSLASH("\\", "反斜杠"),

    /**
     * 竖线
     */
    PIPE("|", "竖线"),

    /**
     * & 符号
     */
    AMPERSAND("&", "&符号"),

    /**
     * 星号
     */
    STAR("*", "星号"),

    /**
     * at 符号
     */
    AT("@", "at符号"),

    /**
     * 井号
     */
    HASH("#", "井号"),

    /**
     * 百分号
     */
    PERCENT("%", "百分号"),

    /**
     * 波浪号
     */
    TILDE("~", "波浪号"),

    /**
     * 美元符号
     */
    DOLLAR("$", "美元符号"),

    /**
     * 人民币符号
     */
    YUAN("￥", "人民币符号"),

    /**
     * 脱字符
     */
    CARET("^", "脱字符"),

    // ==================== 运算符 ====================

    /**
     * 等于号
     */
    EQUALS("=", "等于号"),

    /**
     * 加号
     */
    PLUS("+", "加号"),

    /**
     * 减号（与 DASH 值相同，作为运算符语义使用）
     */
    // MINUS("-", "减号"), // 与 DASH 重复，直接引用 DASH 即可

    /**
     * 不等于
     */
    NOT_EQUALS("!=", "不等于"),

    /**
     * 大于号
     */
    GT(">", "大于号"),

    /**
     * 小于号
     */
    LT("<", "小于号"),

    /**
     * 大于等于
     */
    GE(">=", "大于等于"),

    /**
     * 小于等于
     */
    LE("<=", "小于等于"),

    /**
     * AND 运算符（双）
     */
    AND("&&", "AND运算符"),

    /**
     * OR 运算符（双）
     */
    OR("||", "OR运算符"),

    // ==================== 括号 ====================

    /**
     * 左括号
     */
    LEFT_BRACKET("(", "左括号"),

    /**
     * 右括号
     */
    RIGHT_BRACKET(")", "右括号"),

    /**
     * 左方括号
     */
    LEFT_SQUARE("[", "左方括号"),

    /**
     * 右方括号
     */
    RIGHT_SQUARE("]", "右方括号"),

    /**
     * 左花括号
     */
    LEFT_BRACE("{", "左花括号"),

    /**
     * 右花括号
     */
    RIGHT_BRACE("}", "右花括号"),

    // 注意：LEFT_ANGLE 和 RIGHT_ANGLE 与 LT / GT 值相同，不再单独定义
    // 如需使用尖括号语义，直接引用 LT("<") 和 GT(">")

    // ==================== 引号 ====================

    /**
     * 双引号
     */
    DOUBLE_QUOTE("\"", "双引号"),

    /**
     * 单引号
     */
    SINGLE_QUOTE("'", "单引号"),

    /**
     * 反引号
     */
    BACKTICK("`", "反引号"),

    // ==================== 换行/缩进 ====================

    /**
     * 换行符
     */
    NEWLINE("\n", "换行符"),

    /**
     * 回车符
     */
    CR("\r", "回车符"),

    /**
     * 回车换行
     */
    CRLF("\r\n", "回车换行"),

    /**
     * Tab 制表符
     */
    TAB("\t", "Tab制表符"),

    // ==================== 组合字符 ====================

    /**
     * 双点
     */
    DOUBLE_DOT("..", "双点"),

    /**
     * 双斜杠
     */
    DOUBLE_SLASH("//", "双斜杠"),

    /**
     * 点斜杠
     */
    DOT_SLASH("./", "点斜杠"),

    /**
     * 双点斜杠（上级目录）
     */
    DOUBLE_DOT_SLASH("../", "上级目录"),

    /**
     * 换行逗号
     */
    NEWLINE_COMMA(",\n", "换行逗号"),

    /**
     * 换行 Tab
     */
    NEWLINE_TAB("\n\t", "换行Tab"),

    /**
     * 逗号空格
     */
    COMMA_SPACE(", ", "逗号空格"),

    /**
     * 冒号空格
     */
    COLON_SPACE(": ", "冒号空格"),

    /**
     * 分号空格
     */
    SEMICOLON_SPACE("; ", "分号空格"),

    // ==================== 模板/占位符 ====================

    /**
     * ${ 占位符开始
     */
    DOLLAR_LEFT_BRACE("${", "${占位符开始"),

    /**
     * #{ 占位符开始（SpEL）
     */
    HASH_LEFT_BRACE("#{", "#{占位符开始"),

    /**
     * %s（格式化占位符）
     */
    PLACEHOLDER_S("%s", "字符串占位符"),

    /**
     * %d（格式化占位符）
     */
    PLACEHOLDER_D("%d", "数字占位符"),

    /**
     * {0}（消息占位符）
     */
    PLACEHOLDER_0("{0}", "消息占位符0"),

    /**
     * {1}（消息占位符）
     */
    PLACEHOLDER_1("{1}", "消息占位符1"),

    // ==================== 文件路径 ====================

    /**
     * classpath 前缀
     */
    CLASSPATH("classpath:", "classpath前缀"),

    // ==================== HTML 实体 ====================

    /**
     * HTML 空格（非断行空格）
     */
    HTML_NBSP("&nbsp;", "HTML空格"),

    /**
     * HTML & 符号
     */
    HTML_AMP("&amp;", "HTML &符号"),

    /**
     * HTML 双引号
     */
    HTML_QUOTE("&quot;", "HTML双引号"),

    /**
     * HTML 小于号
     */
    HTML_LT("&lt;", "HTML小于号"),

    /**
     * HTML 大于号
     */
    HTML_GT("&gt;", "HTML大于号"),

    /**
     * HTML 单引号（撇号）
     */
    HTML_APOS("&apos;", "HTML单引号"),

    /**
     * HTML 乘号
     */
    HTML_TIMES("&times;", "HTML乘号"),

    /**
     * HTML 除号
     */
    HTML_DIVIDE("&divide;", "HTML除号"),

    /**
     * HTML 版权符号
     */
    HTML_COPY("&copy;", "HTML版权符号"),

    /**
     * HTML 注册商标符号
     */
    HTML_REG("&reg;", "HTML注册商标"),

    /**
     * HTML 商标符号
     */
    HTML_TRADE("&trade;", "HTML商标符号"),

    /**
     * HTML 欧元符号
     */
    HTML_EURO("&euro;", "HTML欧元符号"),

    /**
     * HTML 英镑符号
     */
    HTML_POUND("&pound;", "HTML英镑符号"),

    /**
     * HTML 日元符号（也用作人民币符号）
     */
    HTML_YEN("&yen;", "HTML日元符号"),

    /**
     * HTML 破折号
     */
    HTML_MDASH("&mdash;", "HTML破折号"),

    /**
     * HTML 短破折号
     */
    HTML_NDASH("&ndash;", "HTML短破折号"),

    /**
     * HTML 省略号
     */
    HTML_HELLIP("&hellip;", "HTML省略号"),

    /**
     * HTML 左双引号
     */
    HTML_LDQUO("&ldquo;", "HTML左双引号"),

    /**
     * HTML 右双引号
     */
    HTML_RDQUO("&rdquo;", "HTML右双引号"),

    /**
     * HTML 左单引号
     */
    HTML_LSQUO("&lsquo;", "HTML左单引号"),

    /**
     * HTML 右单引号
     */
    HTML_RSQUO("&rsquo;", "HTML右单引号"),

    /**
     * HTML 中间点
     */
    HTML_BULLET("&bull;", "HTML中间点"),

    /**
     * HTML 左箭头
     */
    HTML_LARR("&larr;", "HTML左箭头"),

    /**
     * HTML 右箭头
     */
    HTML_RARR("&rarr;", "HTML右箭头"),

    /**
     * HTML 上箭头
     */
    HTML_UARR("&uarr;", "HTML上箭头"),

    /**
     * HTML 下箭头
     */
    HTML_DARR("&darr;", "HTML下箭头"),

    /**
     * HTML 心形
     */
    HTML_HEART("&hearts;", "HTML心形"),

    /**
     * HTML 星号
     */
    HTML_STAR("&star;", "HTML星号"),

    /**
     * HTML 笑脸
     */
    HTML_SMILE("&smile;", "HTML笑脸"),

    /**
     * HTML 尖音符
     */
    HTML_ACUTE("&acute;", "HTML尖音符"),

    /**
     * HTML 抑音符
     */
    HTML_GRAVE("&grave;", "HTML抑音符"),

    /**
     * HTML 上标2
     */
    HTML_SUP2("&sup2;", "HTML上标2"),

    /**
     * HTML 下标
     */
    HTML_SUB("&sub;", "HTML下标"),

    /**
     * HTML 上标
     */
    HTML_SUP("&sup;", "HTML上标"),

    ;

    private final String value;
    private final String label;

    StringPool(String value, String label) {
        this.value = value;
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

    @Override
    public String getColor() {
        switch (this) {
            case EMPTY:
            case SPACE:
                return "#8C8C8C";

            case COMMA:
            case DOT:
            case COLON:
            case SEMICOLON:
                return "#1890FF";

            case QUESTION:
            case EXCLAMATION:
                return "#FAAD14";

            case UNDERSCORE:
            case DASH:
                return "#52C41A";

            case SLASH:
            case BACKSLASH:
            case DOUBLE_SLASH:
                return "#13C2C2";

            case PIPE:
            case AMPERSAND:
            case PERCENT:
                return "#2F54EB";

            case STAR:
            case AT:
            case HASH:
            case TILDE:
            case DOLLAR:
            case YUAN:
            case CARET:
                return "#722ED1";

            case LEFT_BRACKET:
            case RIGHT_BRACKET:
            case LEFT_SQUARE:
            case RIGHT_SQUARE:
            case LEFT_BRACE:
            case RIGHT_BRACE:
                return "#FF7A45";

            case DOUBLE_QUOTE:
            case SINGLE_QUOTE:
            case BACKTICK:
                return "#FF4D4F";

            case EQUALS:
            case NOT_EQUALS:
            case GT:
            case LT:
            case GE:
            case LE:
            case AND:
            case OR:
                return "#FF6B00";

            case PLUS:
                return "#52C41A";

            case NEWLINE:
            case CR:
            case CRLF:
            case TAB:
                return "#8C8C8C";

            case DOUBLE_DOT:
            case DOT_SLASH:
            case DOUBLE_DOT_SLASH:
            case NEWLINE_COMMA:
            case NEWLINE_TAB:
            case COMMA_SPACE:
            case COLON_SPACE:
            case SEMICOLON_SPACE:
                return "#1890FF";

            case DOLLAR_LEFT_BRACE:
            case HASH_LEFT_BRACE:
            case PLACEHOLDER_S:
            case PLACEHOLDER_D:
            case PLACEHOLDER_0:
            case PLACEHOLDER_1:
                return "#FAAD14";

            case CLASSPATH:
                return "#52C41A";

            // HTML 实体 → 红色/紫色系
            case HTML_NBSP:
            case HTML_AMP:
            case HTML_QUOTE:
            case HTML_LT:
            case HTML_GT:
            case HTML_APOS:
            case HTML_TIMES:
            case HTML_DIVIDE:
            case HTML_COPY:
            case HTML_REG:
            case HTML_TRADE:
            case HTML_EURO:
            case HTML_POUND:
            case HTML_YEN:
            case HTML_MDASH:
            case HTML_NDASH:
            case HTML_HELLIP:
            case HTML_LDQUO:
            case HTML_RDQUO:
            case HTML_LSQUO:
            case HTML_RSQUO:
            case HTML_BULLET:
            case HTML_LARR:
            case HTML_RARR:
            case HTML_UARR:
            case HTML_DARR:
            case HTML_HEART:
            case HTML_STAR:
            case HTML_SMILE:
            case HTML_ACUTE:
            case HTML_GRAVE:
            case HTML_SUP2:
            case HTML_SUB:
            case HTML_SUP:
                return "#FF4D4F";

            default:
                return "#8C8C8C";
        }
    }

    public static StringPool fromValue(String value) {
        return BaseEnum.fromValue(StringPool.class, value);
    }
}