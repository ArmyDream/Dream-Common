package com.dream.common.generator.utils;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import com.dream.common.data.controller.CommonController;
import com.dream.common.data.entity.BaseEntity;
import com.dream.common.data.mapper.CommonMapper;
import com.dream.common.data.service.CommonService;
import com.dream.common.data.service.impl.CommonServiceImpl;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 代码生成器工具类
 * <p>
 * 基于 MyBatis-Plus 的 {@link FastAutoGenerator} 封装，支持灵活的参数配置，
 * 可一键生成 Entity、Mapper、Service、ServiceImpl、Controller 等代码文件。
 * </p>
 * <p>
 * 默认使用 Velocity 模板引擎（{@link VelocityTemplateEngine}），
 * 生成的代码默认继承 dream-common-data 提供的基础类：
 * <ul>
 *   <li>实体类继承 {@link BaseEntity}</li>
 *   <li>Mapper 继承 {@link CommonMapper}</li>
 *   <li>Service 继承 {@link CommonService}</li>
 *   <li>ServiceImpl 继承 {@link CommonServiceImpl}</li>
 *   <li>Controller 继承 {@link CommonController}（可选）</li>
 * </ul>
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * CodeGeneratorUtil.GeneratorConfig config = new CodeGeneratorUtil.GeneratorConfig();
 * config.setUrl("jdbc:mysql://localhost:3306/test")
 *        .setUsername("root")
 *        .setPassword("123456")
 *        .setDriverClassName("com.mysql.cj.jdbc.Driver")
 *        .setTableNames(new String[]{"sys_user", "sys_role"})
 *        .setParentPackage("com.dream.example");
 * CodeGeneratorUtil.generate(config);
 * }</pre>
 *
 * <h2>Maven 依赖</h2>
 * <p>使用前需确保项目中已引入以下依赖：</p>
 * <pre>{@code
 * <dependency>
 *     <groupId>com.dream</groupId>
 *     <artifactId>dream-common-generator</artifactId>
 *     <scope>provided</scope>  <!-- 如果仅在开发期使用，可设置为 provided -->
 * </dependency>
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
@Slf4j
public class CodeGeneratorUtils {

    /**
     * 私有构造方法，防止实例化。
     * <p>本类为工具类，所有方法均为静态方法，不提供实例化能力。</p>
     */
    private CodeGeneratorUtils() {
    }

    /**
     * 根据配置生成代码。
     * <p>
     * 根据传入的 {@link GeneratorConfig} 配置，调用 MyBatis-Plus 的
     * {@link FastAutoGenerator} 生成 Entity、Mapper、Service、ServiceImpl、Controller
     * 等代码文件，并支持通过开关控制是否生成对应层次的代码。
     * </p>
     * <p>
     * <b>注意：</b>若配置中的 {@code driverClassName} 未指定，默认使用
     * {@code com.mysql.cj.jdbc.Driver}（适用于 MySQL 8.x）。
     * </p>
     *
     * @param config 生成器配置，不能为 {@code null}
     * @throws IllegalArgumentException 如果必要参数（URL、用户名、表名）为空
     * @throws RuntimeException         如果代码生成过程中发生错误（如数据库连接失败）
     */
    public static void generate(GeneratorConfig config) {
        // 1. 参数校验
        validateConfig(config);

        log.info("开始生成代码，配置信息：{}", config);
        log.info("待生成表：{}", String.join(", ", config.getTableNames()));

        // 2. 构建数据源配置（显式指定驱动类名，避免类加载器问题）
        DataSourceConfig.Builder builderConfig = new DataSourceConfig.Builder(
                config.getUrl(),
                config.getUsername(),
                config.getPassword()
        )
                .driverClassName(config.getDriverClassName())   // 关键：显式设置驱动

        ;
        // 3. 创建快速生成器并配置
        FastAutoGenerator generator = FastAutoGenerator.create(builderConfig);

        // 全局配置
        generator.globalConfig(builder -> {
            builder.author(config.getAuthor())
                    .outputDir(config.getOutputDir())
                    .dateType(DateType.TIME_PACK)      // 使用 java.time 包下的日期类型
                    .commentDate("yyyy-MM-dd")         // 注释日期格式
                    .disableOpenDir();                 // 生成后不自动打开输出目录
        });

        // 包配置
        generator.packageConfig(builder -> {
            Map<OutputFile, String> pathInfo = new HashMap<>();
            if (config.getXmlOutputDir() != null && !config.getXmlOutputDir().trim().isEmpty()) {
                pathInfo.put(OutputFile.xml, config.getXmlOutputDir());
            }else {
                String packagePath = config.getParentPackage().replace('.', '/');
                String mapperPath = config.getMapperPackage(); // 默认为 "mapper"
                String xmlOutputDir = config.getOutputDir() + "/" + packagePath + "/" + mapperPath + "/xml";
                log.info("未指定 xmlOutputDir，自动设置为：{}", xmlOutputDir);
                pathInfo.put(OutputFile.xml, xmlOutputDir);
            }

            builder.parent(config.getParentPackage())
                    .entity(config.getEntityPackage())
                    .mapper(config.getMapperPackage())
                    .service(config.getServicePackage())
                    .serviceImpl(config.getServiceImplPackage())
                    .controller(config.getControllerPackage())
                    .xml(config.getXmlPackage())
                    .pathInfo(pathInfo);
        });

        // 策略配置
        generator.strategyConfig(builder -> {
            // 表策略
            builder.addInclude(config.getTableNames())
                    .addTablePrefix(config.getTablePrefix());

            // 实体策略
            builder.entityBuilder()
                    .superClass(config.getSuperEntityClass())
                    .enableLombok()                     // 启用 Lombok 注解
                    .enableTableFieldAnnotation()      // 启用 @TableField 注解
                    .versionColumnName(config.getVersionColumnName())
                    .logicDeleteColumnName(config.getLogicDeleteColumnName())
                    .formatFileName("%s");             // 实体类文件名格式（不添加后缀）

            // Mapper 策略
            if (config.isGenerateMapper()) {
                builder.mapperBuilder()
                        .superClass(config.getSuperMapperClass())
                        .enableBaseResultMap()          // 生成 BaseResultMap
                        .enableBaseColumnList()         // 生成 Base_Column_List
                        .formatMapperFileName("%sMapper")
                        .formatXmlFileName("%sMapper");
            } else {
                builder.mapperBuilder().disable();
            }

            // Service 策略
            if (config.isGenerateService()) {
                builder.serviceBuilder()
                        .superServiceClass(config.getSuperServiceClass())
                        .superServiceImplClass(config.getSuperServiceImplClass())
                        .serviceImplTemplate(config.getServiceImplTemplate())
                        .formatServiceFileName("%sService")
                        .formatServiceImplFileName("%sServiceImpl");
            } else {
                builder.serviceBuilder().disable();
            }

            // Controller 策略
            if (config.isGenerateController()) {
                builder.controllerBuilder()
                        .superClass(config.getSuperControllerClass())
                        .template(config.getControllerTemplate())
                        .enableRestStyle()              // 生成 @RestController 风格的 Controller
                        .formatFileName("%sController");
            } else {
                builder.controllerBuilder().disable();
            }
        });

        // 模板配置（根据开关禁用对应模板）  过期 ,已 使用 strategyConfig 配置
      /*  generator.templateConfig(builder -> {
            if (!config.isGenerateController()) {
                builder.disable(TemplateType.CONTROLLER);
            }
            if (!config.isGenerateService()) {
                builder.disable(TemplateType.SERVICE, TemplateType.SERVICE_IMPL);
            }
            if (!config.isGenerateMapper()) {
                builder.disable(TemplateType.MAPPER);
            }
            // 注意：Entity 模板默认始终启用，暂无开关控制
        });*/

        // 注入自定义属性（用于模板中访问配置值）
        Map<String, Object> customMap = new HashMap<>();
        customMap.put("baseRequestMapping", config.getBaseRequestMapping());
        customMap.put("basePermissionPrefix", config.getBasePermissionPrefix());

        // ⚠️ 注入父类全限定名（用于模板中判断）
        Class<?> superControllerClass = config.getSuperControllerClass();
        customMap.put("superControllerClassName", superControllerClass != null ?
                superControllerClass.getName() : "");

        Class<?> superServiceImplClass = config.getSuperServiceImplClass();
        customMap.put("superServiceImplClassName", superServiceImplClass != null ?
                superServiceImplClass.getName() : "");


        generator.injectionConfig(builder -> builder.customMap(customMap));

        // 设置模板引擎（Velocity）
        generator.templateEngine(new VelocityTemplateEngine());

        // 4. 执行生成
        try {
            generator.execute();
            log.info("✅ 代码生成完成，文件输出目录：{}", config.getOutputDir());
        } catch (Exception e) {
            log.error("❌ 代码生成失败", e);
            throw new RuntimeException("代码生成失败，请检查配置和数据库连接", e);
        }
    }

    /**
     * 校验配置参数的有效性
     *
     * @param config 配置对象
     * @throws IllegalArgumentException 如果必要参数缺失
     */
    private static void validateConfig(GeneratorConfig config) {
        Objects.requireNonNull(config, "生成配置不能为空");

        if (config.getUrl() == null || config.getUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("数据库连接 URL 不能为空");
        }
        if (config.getUsername() == null || config.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("数据库用户名不能为空");
        }
        // 密码允许为空（某些数据库允许无密码）
        if (config.getTableNames() == null || config.getTableNames().length == 0) {
            throw new IllegalArgumentException("至少指定一个表名");
        }
        // 检查输出目录是否存在（或创建）
        // 此处不强制要求，因为 MyBatis-Plus 会尝试创建目录
        log.debug("配置校验通过");
    }

    /**
     * 生成器配置类。
     * <p>
     * 用于配置代码生成器的各项参数，包括数据库连接信息、包名、表名、
     * 各层次父类、字段名、生成开关等。配合 {@link #generate(GeneratorConfig)} 使用。
     * </p>
     * <p>
     * 使用 Lombok {@link Data} 和 {@link Accessors} 支持链式调用，方便配置。
     * </p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * GeneratorConfig config = new GeneratorConfig();
     * config.setUrl("jdbc:mysql://localhost:3306/test")
     *        .setUsername("root")
     *        .setPassword("123456")
     *        .setDriverClassName("com.mysql.cj.jdbc.Driver")
     *        .setParentPackage("com.dream.example")
     *        .setTableNames(new String[]{"sys_user", "sys_role"})
     *        .setGenerateController(false);
     * }</pre>
     *
     * @author dream
     * @since 2026-08-21
     */
    @Data
    @Accessors(chain = true)
    public static class GeneratorConfig {

        // ==================== 数据库连接 ====================

        /**
         * 数据库连接 URL（必填）
         * <p>示例：{@code jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf8}</p>
         */
        private String url;

        /**
         * 数据库用户名（必填）
         */
        private String username;

        /**
         * 数据库密码（可以为空）
         */
        private String password;

        /**
         * JDBC 驱动类名（可选，默认根据 URL 自动检测）
         * <p>推荐显式设置以避免类加载器问题，例如：{@code com.mysql.cj.jdbc.Driver}</p>
         */
        private String driverClassName = "com.mysql.cj.jdbc.Driver";

        // ==================== 全局配置 ====================

        /**
         * 代码作者（默认 {@code "generator"}）
         */
        private String author = "generator";

        /**
         * Java 文件输出目录（默认当前项目 {@code src/main/java}）
         * <p>建议使用绝对路径，避免相对路径不确定性</p>
         */
        private String outputDir = System.getProperty("user.dir") + "/src/main/java";

        /**
         * XML Mapper 输出目录（默认 {@code mapper.xml}）
         */
//        private String xmlOutputDir = System.getProperty("user.dir") + "/src/main/resources/mapper";
        private String xmlOutputDir;

        // ==================== 包配置 ====================

        /**
         * 父包名（默认 {@code com.example}）
         */
        private String parentPackage = "com.example";

        /**
         * 实体类包名（默认 {@code entity}）
         */
        private String entityPackage = "entity";

        /**
         * Mapper 接口包名（默认 {@code mapper}）
         */
        private String mapperPackage = "mapper";

        /**
         * Service 接口包名（默认 {@code service}）
         */
        private String servicePackage = "service";

        /**
         * Service 实现类包名（默认 {@code service.impl}）
         */
        private String serviceImplPackage = "service.impl";

        /**
         * Controller 包名（默认 {@code controller}）
         */
        private String controllerPackage = "controller";

        /**
         * XML Mapper 包名（默认 {@code mapper.xml}）
         * <p>仅用于包路径，实际文件位置由 {@link #xmlOutputDir} 决定</p>
         */
        private String xmlPackage = "mapper.xml";

        /**
         * 基础请求映射路径（如 "/api" 或 "/api/v1"）
         * <p>最终路径 = baseRequestMapping + "/" + 表名（实体名小写）</p>
         * <p>示例：baseRequestMapping="/api"，表 User → 路径为 "/api/user"</p>
         * <p>若未指定，默认使用 "/" + 表名</p>
         */
        private String baseRequestMapping;

        /**
         * 基础权限前缀（如 "system" 或 "system:user" 的前缀部分）
         * <p>最终权限 = basePermissionPrefix + ":" + 表名（实体名小写）</p>
         * <p>示例：basePermissionPrefix="system"，表 User → 权限为 "system:user"</p>
         * <p>若未指定，默认使用表名</p>
         */
        private String basePermissionPrefix;

        // ==================== 表配置 ====================

        /**
         * 要生成代码的表名数组（必填）
         */
        private String[] tableNames;

        /**
         * 表前缀数组（生成类名时会自动去除，默认 {@code t_}、{@code sys_}）
         */
        private String[] tablePrefix = new String[]{"t_", "sys_"};

        // ==================== 生成开关 ====================

        /**
         * 是否生成 Mapper（默认 {@code true}）
         */
        private boolean generateMapper = true;

        /**
         * 是否生成 Service（默认 {@code true}）
         */
        private boolean generateService = true;

        /**
         * 是否生成 Controller（默认 {@code true}）
         */
        private boolean generateController = true;

        // ==================== 父类配置 ====================

        /**
         * 实体类父类（默认 {@link BaseEntity}）
         */
        private Class<?> superEntityClass = BaseEntity.class;

        /**
         * Mapper 父接口（默认 {@link CommonMapper}）
         */
        private Class<?> superMapperClass = CommonMapper.class;

        /**
         * Service 父接口（默认 {@link CommonService}）
         */
        private Class<?> superServiceClass = CommonService.class;

        /**
         * Service 实现类父类（默认 {@link CommonServiceImpl}）
         */
        private Class<?> superServiceImplClass = CommonServiceImpl.class;

        /**
         * Controller 父类（默认 {@code CommonController}）
         */
        private Class<?> superControllerClass = CommonController.class;

        // ==================== 字段配置 ====================

        /**
         * 乐观锁字段名（默认 {@code version}）
         */
        private String versionColumnName = "version";

        /**
         * 逻辑删除字段名（默认 {@code deleted}）
         */
        private String logicDeleteColumnName = "deleted";

        // ==================== 扩展配置 ====================

        /**
         * 实体类自定义模板路径（{@code null} 表示使用默认模板）
         */
        private String entityTemplate;

        /**
         * Mapper 自定义模板路径（{@code null} 表示使用默认模板）
         */
        private String mapperTemplate;

        /**
         * Service 自定义模板路径（{@code null} 表示使用默认模板）
         */
        private String serviceTemplate;

        /**
         * ServiceImpl 自定义模板路径（{@code /templates/serviceImpl.java.vm } 为空 表示使用默认模板）
         */
        private String serviceImplTemplate = "/templates/serviceImpl.java.vm";

        /**
         * Controller 自定义模板路径（{@code /templates/controller.java.vm }  为空 表示使用默认模板）
         */
        private String controllerTemplate = "/templates/controller.java.vm";

        @Override
        public String toString() {
            // 避免打印敏感信息（密码）
            return "GeneratorConfig{" +
                    "url='" + url + '\'' +
                    ", username='" + username + '\'' +
                    ", driverClassName='" + driverClassName + '\'' +
                    ", author='" + author + '\'' +
                    ", outputDir='" + outputDir + '\'' +
                    ", xmlOutputDir='" + xmlOutputDir + '\'' +
                    ", parentPackage='" + parentPackage + '\'' +
                    ", tableNames=" + (tableNames == null ? "null" : String.join(",", tableNames)) +
                    ", generateMapper=" + generateMapper +
                    ", generateService=" + generateService +
                    ", generateController=" + generateController +
                    ", versionColumnName='" + versionColumnName + '\'' +
                    ", logicDeleteColumnName='" + logicDeleteColumnName + '\'' +
                    '}';
        }
    }
}