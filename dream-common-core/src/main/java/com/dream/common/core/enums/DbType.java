package com.dream.common.core.enums;

/**
 * 数据库类型枚举
 * <p>
 * 涵盖主流关系型数据库、国产数据库、云原生数据库、列式存储/时序数据库等
 * <p>
 * 用于：多数据源切换、代码生成、SQL方言适配、分页插件配置等场景
 *
 * @author dream
 */
@SuppressWarnings("all")
public enum DbType implements BaseEnum<String> {

    // ==================== 主流关系型数据库 ====================

    /**
     * MySQL 5.x / 8.x
     */
    MYSQL("mysql", "MySQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://{host}:{port}/{database}"),

    /**
     * MariaDB 5.5+ / 10.x / 11.x
     */
    MARIADB("mariadb", "MariaDB", "org.mariadb.jdbc.Driver", "jdbc:mariadb://{host}:{port}/{database}"),

    /**
     * PostgreSQL 9.x ~ 16.x
     */
    POSTGRESQL("postgresql", "PostgreSQL", "org.postgresql.Driver", "jdbc:postgresql://{host}:{port}/{database}"),

    /**
     * Oracle 11g / 12c / 18c / 19c / 21c / 23ai
     */
    ORACLE("oracle", "Oracle", "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@{host}:{port}:{sid}"),

    /**
     * SQL Server 2008 / 2012 / 2016 / 2017 / 2019 / 2022
     */
    SQL_SERVER("sqlserver", "SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://{host}:{port};databaseName={database}"),

    /**
     * IBM DB2 9.7 / 10.1 / 10.5 / 11.1 / 11.5
     */
    DB2("db2", "IBM DB2", "com.ibm.db2.jcc.DB2Driver", "jdbc:db2://{host}:{port}/{database}"),

    /**
     * H2 1.4.x / 2.x
     */
    H2("h2", "H2", "org.h2.Driver", "jdbc:h2:~/test"),

    /**
     * SQLite 3.x
     */
    SQLITE("sqlite", "SQLite", "org.sqlite.JDBC", "jdbc:sqlite:{database}.db"),

    /**
     * Apache Derby 10.11+
     */
    DERBY("derby", "Derby", "org.apache.derby.jdbc.ClientDriver", "jdbc:derby://{host}:{port}/{database}"),

    /**
     * Sybase ASE / IQ
     */
    SYBASE("sybase", "Sybase", "com.sybase.jdbc4.jdbc.SybDriver", "jdbc:sybase:Tds:{host}:{port}/{database}"),

    /**
     * Firebird 2.5 / 3.0 / 4.0
     */
    FIREBIRD("firebird", "Firebird", "org.firebirdsql.jdbc.FBDriver", "jdbc:firebirdsql:{host}:{port}/{database}"),

    /**
     * Informix 14.10
     */
    INFORMIX("informix", "Informix", "com.informix.jdbc.IfxDriver", "jdbc:informix-sqli://{host}:{port}/{database}"),

    /**
     * CUBRID 9.x / 10.x / 11.x
     */
    CUBRID("cubrid", "CUBRID", "cubrid.jdbc.driver.CUBRIDDriver", "jdbc:cubrid:{host}:{port}:{database}"),

    // ==================== 国产数据库 ====================

    /**
     * 达梦 DM 7.x / 8.x
     */
    DM("dm", "达梦", "dm.jdbc.driver.DmDriver", "jdbc:dm://{host}:{port}/{database}"),

    /**
     * 人大金仓 KingbaseES V8 / V9
     */
    KINGBASE("kingbase", "人大金仓", "com.kingbase8.Driver", "jdbc:kingbase://{host}:{port}/{database}"),

    /**
     * 南大通用 GBase 8a / 8s
     */
    GBASE("gbase", "南大通用", "com.gbase.jdbc.Driver", "jdbc:gbase://{host}:{port}/{database}"),

    /**
     * 神州通用 OSCAR V5 / V7
     */
    OSCAR("oscar", "神州通用", "com.oscar.Driver", "jdbc:oscar://{host}:{port}/{database}"),

    /**
     * 华为 GaussDB 100 / 200 / 300 / 500
     */
    GAUSSDB("gaussdb", "华为高斯", "com.huawei.gauss.jdbc.ZenithDriver", "jdbc:postgresql://{host}:{port}/{database}"),

    /**
     * 瀚高 HighGoDB V6 / V7
     */
    HIGHGO("highgo", "瀚高", "com.highgo.jdbc.Driver", "jdbc:highgo://{host}:{port}/{database}"),

    /**
     * 虚谷 Xugu V11
     */
    XUGU("xugu", "虚谷", "com.xugu.cloudjdbc.Driver", "jdbc:xugu://{host}:{port}/{database}"),

    /**
     * 优炫 UXDB V2.1
     */
    UXDB("uxdb", "优炫", "com.uxsino.uxjdbc.Driver", "jdbc:uxdb://{host}:{port}/{database}"),

    /**
     * 星瑞格 SinoDB
     */
    SINODB("sinodb", "星瑞格", "com.sinodb.jdbc.Driver", "jdbc:sinodb://{host}:{port}/{database}"),

    /**
     * 海量数据 Vastbase G100
     */
    VASTBASE("vastbase", "海量数据", "com.vastbase.jdbc.Driver", "jdbc:postgresql://{host}:{port}/{database}"),

    /**
     * 中兴 GoldenDB
     */
    GOLDENDB("goldendb", "中兴GoldenDB", "com.tencentcloud.tdsql.mysql.cj.jdbc.Driver", "jdbc:mysql://{host}:{port}/{database}"),

    // ==================== 云原生/分布式数据库 ====================

    /**
     * 阿里 OceanBase 3.x / 4.x
     */
    OCEANBASE("oceanbase", "OceanBase", "com.oceanbase.jdbc.Driver", "jdbc:oceanbase://{host}:{port}/{database}"),

    /**
     * 阿里 PolarDB（兼容 MySQL）
     */
    POLARDB("polardb", "阿里云PolarDB", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://{host}:{port}/{database}"),

    /**
     * 腾讯 TDSQL（兼容 MySQL）
     */
    TDSQL("tdsql", "腾讯TDSQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://{host}:{port}/{database}"),

    /**
     * Amazon Redshift
     */
    REDSHIFT("redshift", "Amazon Redshift", "com.amazon.redshift.jdbc.Driver", "jdbc:redshift://{host}:{port}/{database}"),

    /**
     * Amazon Aurora（兼容 MySQL）
     */
    AURORA("aurora", "Amazon Aurora", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://{host}:{port}/{database}"),

    /**
     * 蚂蚁集团 ZettaDB（OceanBase 前身）
     */
    ZETTADB("zettadb", "ZettaDB", "com.zettadb.jdbc.Driver", "jdbc:zettadb://{host}:{port}/{database}"),

    // ==================== 列式存储 / OLAP 数据库 ====================

    /**
     * ClickHouse 20.x ~ 24.x
     */
    CLICKHOUSE("clickhouse", "ClickHouse", "com.clickhouse.jdbc.ClickHouseDriver", "jdbc:clickhouse://{host}:{port}/{database}"),

    /**
     * Apache Doris 1.x / 2.x
     */
    DORIS("doris", "Apache Doris", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://{host}:{port}/{database}"),

    /**
     * StarRocks 2.x / 3.x
     */
    STARROCKS("starrocks", "StarRocks", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://{host}:{port}/{database}"),

    // ==================== 时序数据库 ====================

    /**
     * TDengine 2.x / 3.x
     */
    TDENGINE("tdengine", "TDengine", "com.taosdata.jdbc.TSDBDriver", "jdbc:taos://{host}:{port}/{database}"),

    /**
     * Goldilocks（时序数据库）
     */
    GOLDILOCKS("goldilocks", "Goldilocks", "com.goldilocks.jdbc.Driver", "jdbc:goldilocks://{host}:{port}/{database}"),

    /**
     * InfluxDB（通过 JDBC 驱动）
     */
    INFLUXDB("influxdb", "InfluxDB", "org.influxdb.InfluxDB", "jdbc:influxdb://{host}:{port}/{database}"),

    // ==================== 嵌入式/分析型数据库 ====================

    /**
     * DuckDB 0.3x ~ 0.7x
     */
    DUCKDB("duckdb", "DuckDB", "org.duckdb.DuckDBDriver", "jdbc:duckdb:{database}.db"),

    /**
     * HSQLDB
     */
    HSQL("hsql", "HSQLDB", "org.hsqldb.jdbc.JDBCDriver", "jdbc:hsqldb:mem:{database}"),

    // ==================== 其他 ====================

    /**
     * 未知类型
     */
    UNKNOWN("unknown", "未知", null, null);

    // ==================== 字段 ====================

    private final String value;
    private final String label;
    private final String driverClassName;
    private final String jdbcUrlTemplate;

    // ==================== 构造器 ====================

    DbType(String value, String label, String driverClassName, String jdbcUrlTemplate) {
        this.value = value;
        this.label = label;
        this.driverClassName = driverClassName;
        this.jdbcUrlTemplate = jdbcUrlTemplate;
    }

    // ==================== BaseEnum 实现 ====================

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    // ==================== 扩展方法 ====================

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getJdbcUrlTemplate() {
        return jdbcUrlTemplate;
    }

    /**
     * 生成 JDBC URL（替换占位符）
     *
     * @param host     主机地址
     * @param port     端口
     * @param database 数据库名
     * @return 完整的 JDBC URL
     */
    public String buildJdbcUrl(String host, int port, String database) {
        if (jdbcUrlTemplate == null) {
            return null;
        }
        String url = jdbcUrlTemplate
                .replace("{host}", host)
                .replace("{port}", String.valueOf(port))
                .replace("{database}", database)
                .replace("{sid}", database);
        return url;
    }

    /**
     * 生成 JDBC URL（SID 模式，Oracle 专用）
     */
    public String buildJdbcUrlWithSid(String host, int port, String sid) {
        if (this != ORACLE || jdbcUrlTemplate == null) {
            return buildJdbcUrl(host, port, sid);
        }
        return jdbcUrlTemplate
                .replace("{host}", host)
                .replace("{port}", String.valueOf(port))
                .replace("{sid}", sid);
    }

    @Override
    public String getDescription() {
        return label + " (" + value + ")";
    }

    @Override
    public String getColor() {
        switch (this) {
            case MYSQL:
            case MARIADB:
            case POLARDB:
            case TDSQL:
            case AURORA:
            case GOLDENDB:
                return "#00758F";
            case OCEANBASE:
                return "#00A3E0";
            case POSTGRESQL:
            case REDSHIFT:
            case GAUSSDB:
            case VASTBASE:
            case HIGHGO:
                return "#336791";
            case ORACLE:
                return "#CC0000";
            case SQL_SERVER:
                return "#CC292B";
            case DB2:
                return "#005A9C";
            case DM:
                return "#E60012";
            case KINGBASE:
                return "#005BAC";
            case GBASE:
                return "#1A8CFF";
            case OSCAR:
                return "#FF6B00";
            case XUGU:
                return "#4CAF50";
            case UXDB:
                return "#FF5722";
            case SINODB:
                return "#9C27B0";
            case CLICKHOUSE:
                return "#FFCC00";
            case DORIS:
                return "#1890FF";
            case STARROCKS:
                return "#FF6B00";
            case TDENGINE:
                return "#00B4D8";
            case GOLDILOCKS:
                return "#FFD700";
            case DUCKDB:
                return "#FFD700";
            case H2:
            case SQLITE:
            case DERBY:
            case HSQL:
                return "#8C8C8C";
            case SYBASE:
                return "#FF6600";
            case FIREBIRD:
                return "#FF0000";
            case INFORMIX:
                return "#0066CC";
            case CUBRID:
                return "#FF6600";
            case ZETTADB:
                return "#00A3E0";
            case INFLUXDB:
                return "#22ADF6";
            default:
                return "#8C8C8C";
        }
    }

    // ==================== 业务判断方法 ====================

    /**
     * MySQL 兼容系列
     */
    public boolean isMysqlCompatible() {
        return this == MYSQL || this == MARIADB
                || this == POLARDB || this == TDSQL || this == AURORA
                || this == GOLDENDB || this == OCEANBASE
                || this == DORIS || this == STARROCKS;
    }

    /**
     * PostgreSQL 兼容系列
     */
    public boolean isPostgresqlCompatible() {
        return this == POSTGRESQL || this == REDSHIFT || this == GAUSSDB
                || this == VASTBASE || this == HIGHGO;
    }

    /**
     * 国产数据库
     */
    public boolean isDomestic() {
        return this == DM || this == KINGBASE || this == GBASE
                || this == OSCAR || this == GAUSSDB || this == HIGHGO
                || this == XUGU || this == UXDB || this == SINODB
                || this == VASTBASE || this == GOLDENDB;
    }

    /**
     * 列式存储/OLAP
     */
    public boolean isOlap() {
        return this == CLICKHOUSE || this == DORIS || this == STARROCKS;
    }

    /**
     * 时序数据库
     */
    public boolean isTimeSeries() {
        return this == TDENGINE || this == GOLDILOCKS || this == INFLUXDB;
    }

    /**
     * 嵌入式数据库
     */
    public boolean isEmbedded() {
        return this == H2 || this == SQLITE || this == DERBY || this == HSQL || this == DUCKDB;
    }

    /**
     * 是否支持分页
     */
    public boolean supportPagination() {
        return this != UNKNOWN;
    }

    // ==================== 静态方法 ====================

    public static DbType fromValue(String value) {
        return BaseEnum.fromValue(DbType.class, value);
    }

    public static DbType fromValueOrDefault(String value, DbType defaultValue) {
        return BaseEnum.fromValueOrDefault(DbType.class, value, defaultValue);
    }

    public static boolean exists(String value) {
        return BaseEnum.exists(DbType.class, value);
    }

    /**
     * 根据 JDBC URL 自动识别数据库类型
     */
    public static DbType fromJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            return UNKNOWN;
        }
        String url = jdbcUrl.toLowerCase();
        if (url.contains("mysql")) {
            if (url.contains("oceanbase")) return OCEANBASE;
            if (url.contains("polardb")) return POLARDB;
            if (url.contains("tdsql")) return TDSQL;
            if (url.contains("aurora")) return AURORA;
            if (url.contains("goldendb")) return GOLDENDB;
            if (url.contains("doris")) return DORIS;
            if (url.contains("starrocks")) return STARROCKS;
            return MYSQL;
        }
        if (url.contains("mariadb")) return MARIADB;
        if (url.contains("oracle")) return ORACLE;
        if (url.contains("postgresql")) {
            if (url.contains("redshift")) return REDSHIFT;
            if (url.contains("gaussdb")) return GAUSSDB;
            if (url.contains("vastbase")) return VASTBASE;
            if (url.contains("highgo")) return HIGHGO;
            return POSTGRESQL;
        }
        if (url.contains("sqlserver")) return SQL_SERVER;
        if (url.contains("db2")) return DB2;
        if (url.contains("dm")) return DM;
        if (url.contains("kingbase")) return KINGBASE;
        if (url.contains("gbase")) return GBASE;
        if (url.contains("oscar")) return OSCAR;
        if (url.contains("xugu")) return XUGU;
        if (url.contains("uxdb")) return UXDB;
        if (url.contains("sinodb")) return SINODB;
        if (url.contains("zettadb")) return ZETTADB;
        if (url.contains("clickhouse")) return CLICKHOUSE;
        if (url.contains("taos")) return TDENGINE;
        if (url.contains("goldilocks")) return GOLDILOCKS;
        if (url.contains("influxdb")) return INFLUXDB;
        if (url.contains("sybase")) return SYBASE;
        if (url.contains("firebird")) return FIREBIRD;
        if (url.contains("informix")) return INFORMIX;
        if (url.contains("cubrid")) return CUBRID;
        if (url.contains("duckdb")) return DUCKDB;
        if (url.contains("h2")) return H2;
        if (url.contains("sqlite")) return SQLITE;
        if (url.contains("derby")) return DERBY;
        if (url.contains("hsqldb")) return HSQL;
        return UNKNOWN;
    }

    /**
     * 根据驱动类名自动识别数据库类型
     */
    public static DbType fromDriverClassName(String driverClassName) {
        if (driverClassName == null || driverClassName.isEmpty()) {
            return UNKNOWN;
        }
        String driver = driverClassName.toLowerCase();
        if (driver.contains("mysql") || driver.contains("c.j")) {
            if (driver.contains("oceanbase")) return OCEANBASE;
            if (driver.contains("polardb")) return POLARDB;
            if (driver.contains("tdsql")) return TDSQL;
            if (driver.contains("aurora")) return AURORA;
            if (driver.contains("goldendb")) return GOLDENDB;
            if (driver.contains("doris")) return DORIS;
            if (driver.contains("starrocks")) return STARROCKS;
            return MYSQL;
        }
        if (driver.contains("mariadb")) return MARIADB;
        if (driver.contains("oracle")) return ORACLE;
        if (driver.contains("postgresql")) {
            if (driver.contains("redshift")) return REDSHIFT;
            if (driver.contains("gauss")) return GAUSSDB;
            if (driver.contains("vastbase")) return VASTBASE;
            if (driver.contains("highgo")) return HIGHGO;
            return POSTGRESQL;
        }
        if (driver.contains("sqlserver")) return SQL_SERVER;
        if (driver.contains("db2")) return DB2;
        if (driver.contains("dm")) return DM;
        if (driver.contains("kingbase")) return KINGBASE;
        if (driver.contains("gbase")) return GBASE;
        if (driver.contains("oscar")) return OSCAR;
        if (driver.contains("xugu")) return XUGU;
        if (driver.contains("uxdb")) return UXDB;
        if (driver.contains("sinodb")) return SINODB;
        if (driver.contains("zettadb")) return ZETTADB;
        if (driver.contains("clickhouse")) return CLICKHOUSE;
        if (driver.contains("taos")) return TDENGINE;
        if (driver.contains("goldilocks")) return GOLDILOCKS;
        if (driver.contains("influxdb")) return INFLUXDB;
        if (driver.contains("sybase")) return SYBASE;
        if (driver.contains("firebird")) return FIREBIRD;
        if (driver.contains("informix")) return INFORMIX;
        if (driver.contains("cubrid")) return CUBRID;
        if (driver.contains("duckdb")) return DUCKDB;
        if (driver.contains("h2")) return H2;
        if (driver.contains("sqlite")) return SQLITE;
        if (driver.contains("derby")) return DERBY;
        if (driver.contains("hsqldb")) return HSQL;
        return UNKNOWN;
    }
}