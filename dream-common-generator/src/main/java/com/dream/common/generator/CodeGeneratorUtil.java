package com.dream.common.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.TemplateType;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import com.dream.common.data.controller.CommonController;
import com.dream.common.data.entity.BaseIdEntity;
import com.dream.common.data.mapper.CommonMapper;
import com.dream.common.data.service.CommonService;
import com.dream.common.data.service.impl.CommonServiceImpl;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

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
 *   <li>实体类继承 {@link BaseIdEntity}</li>
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
 *        .setTableNames("sys_user", "sys_role")
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
 * </dependency>
 * }</pre>
 *
 * @author dream
 * @since 2026-08-21
 */
@Slf4j
public class CodeGeneratorUtil {

    /**
     * 私有构造方法，防止实例化。
     * <p>本类为工具类，所有方法均为静态方法，不提供实例化能力。</p>
     */
    private CodeGeneratorUtil() {
    }

    /**
     * 根据配置生成代码。
     * <p>
     * 根据传入的 {@link GeneratorConfig} 配置，调用 MyBatis-Plus 的
     * {@link FastAutoGenerator} 生成 Entity、Mapper、Service、ServiceImpl、Controller
     * 等代码文件，并支持通过开关控制是否生成对应层次的代码。
     * </p>
     *
     * @param config 生成器配置，不能为 {@code null}
     */
    public static void generate(GeneratorConfig config) {
        log.info("开始生成代码，作者={}, 输出目录={}, 表名={}",
                config.getAuthor(), config.getOutputDir(), String.join(",", config.getTableNames()));

        FastAutoGenerator.create(config.getUrl(), config.getUsername(), config.getPassword())
                .globalConfig(builder -> {
                    builder.author(config.getAuthor())
                            .outputDir(config.getOutputDir())
                            .dateType(DateType.TIME_PACK)
                            .commentDate("yyyy-MM-dd")
                            .disableOpenDir();
                })
                .packageConfig(builder -> {
                    Map<OutputFile, String> pathInfo = new HashMap<>();
                    if (config.getXmlOutputDir() != null) {
                        pathInfo.put(OutputFile.xml, config.getXmlOutputDir());
                    }

                    builder.parent(config.getParentPackage())
                            .entity(config.getEntityPackage())
                            .mapper(config.getMapperPackage())
                            .service(config.getServicePackage())
                            .serviceImpl(config.getServiceImplPackage())
                            .controller(config.getControllerPackage())
                            .xml(config.getXmlPackage())
                            .pathInfo(pathInfo);
                })
                .strategyConfig(builder -> {
                    builder.addInclude(config.getTableNames())
                            .addTablePrefix(config.getTablePrefix());

                    // 实体类策略配置
                    builder.entityBuilder()
                            .superClass(config.getSuperEntityClass())
                            .enableLombok()
                            .enableTableFieldAnnotation()
                            .versionColumnName(config.getVersionColumnName())
                            .logicDeleteColumnName(config.getLogicDeleteColumnName())
                            .formatFileName("%s");

                    // Mapper 策略配置
                    if (config.isGenerateMapper()) {
                        builder.mapperBuilder()
                                .superClass(config.getSuperMapperClass())
                                .enableBaseResultMap()
                                .enableBaseColumnList()
                                .formatMapperFileName("%sMapper")
                                .formatXmlFileName("%sMapper");
                    } else {
                        builder.mapperBuilder().disable();
                    }

                    // Service 策略配置
                    if (config.isGenerateService()) {
                        builder.serviceBuilder()
                                .superServiceClass(config.getSuperServiceClass())
                                .superServiceImplClass(config.getSuperServiceImplClass())
                                .formatServiceFileName("%sService")
                                .formatServiceImplFileName("%sServiceImpl");
                    } else {
                        builder.serviceBuilder().disable();
                    }

                    // Controller 策略配置
                    if (config.isGenerateController()) {
                        Class<?> controllerSuper = config.getSuperControllerClass();
                        if (controllerSuper == null && config.isInheritCommonController()) {
                            controllerSuper = CommonController.class;
                        }
                        builder.controllerBuilder()
                                .superClass(controllerSuper)
                                .enableRestStyle()
                                .formatFileName("%sController");
                    } else {
                        builder.controllerBuilder().disable();
                    }
                })
                .templateConfig(builder -> {
                    // 设置不生成的模板
                    if (!config.isGenerateController()) {
                        builder.disable(TemplateType.CONTROLLER);
                    }
                    if (!config.isGenerateService()) {
                        builder.disable(TemplateType.SERVICE, TemplateType.SERVICE_IMPL);
                    }
                    if (!config.isGenerateMapper()) {
                        builder.disable(TemplateType.MAPPER);
                    }
                })
                .templateEngine(new VelocityTemplateEngine())
                .execute();

        log.info("代码生成完成，输出目录={}", config.getOutputDir());
    }

    /**
     * 生成器配置类。
     * <p>
     * 用于配置代码生成器的各项参数，包括数据库连接信息、包名、表名、
     * 各层次父类、字段名、生成开关等。配合 {@link #generate(GeneratorConfig)} 使用。
     * </p>
     * <p>
     * 使用 Lombok {@link Data} 自动生成 getter/setter，可通过链式调用配置参数。
     * </p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * GeneratorConfig config = new GeneratorConfig();
     * config.setUrl("jdbc:mysql://localhost:3306/test")
     *        .setUsername("root")
     *        .setPassword("123456")
     *        .setParentPackage("com.dream.example")
     *        .setTableNames("sys_user", "sys_role")
     *        .setGenerateController(false);
     * }</pre>
     *
     * @author dream
     * @since 2026-08-21
     */
    @Data
    public static class GeneratorConfig {

        // ==================== 数据库连接 ====================

        /**
         * 数据库连接 URL
         */
        private String url;
        /**
         * 数据库用户名
         */
        private String username;
        /**
         * 数据库密码
         */
        private String password;

        // ==================== 全局配置 ====================

        /**
         * 代码作者（默认 {@code "generator"}）
         */
        private String author = "generator";

        /**
         * Java 文件输出目录（默认当前项目 {@code src/main/java}）
         */
        private String outputDir = System.getProperty("user.dir") + "/src/main/java";

        /**
         * XML Mapper 输出目录（默认 {@code src/main/resources/mapper}）
         */
        private String xmlOutputDir = System.getProperty("user.dir") + "/src/main/resources/mapper";

        // ==================== 包配置 ====================

        /**
         * 父包名（默认 {@code com.example}）
         */
        private String parentPackage = "com.example";
        /**
         * 实体类包名
         */
        private String entityPackage = "entity";
        /**
         * Mapper 接口包名
         */
        private String mapperPackage = "mapper";
        /**
         * Service 接口包名
         */
        private String servicePackage = "service";
        /**
         * Service 实现类包名
         */
        private String serviceImplPackage = "service.impl";
        /**
         * Controller 包名
         */
        private String controllerPackage = "controller";
        /**
         * XML Mapper 包名
         */
        private String xmlPackage = "mapper.xml";

        // ==================== 表配置 ====================

        /**
         * 表名数组（支持多张表）
         */
        private String[] tableNames;

        /**
         * 表前缀数组（生成代码时会自动去除，默认 {@code t_}、{@code sys_}）
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
         * 实体类父类（默认 {@link BaseIdEntity}）
         */
        private Class<?> superEntityClass = BaseIdEntity.class;

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
         * Controller 父类
         * <p>默认 {@code null}，即不继承任何父类。
         * 若 {@link #inheritCommonController} 为 {@code true}，则自动设置为 {@link CommonController}。</p>
         */
        private Class<?> superControllerClass = null;

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
         * 是否继承 CommonController（默认 {@code true}）
         * <p>当为 {@code true} 且 {@link #isGenerateController()} 也为 {@code true} 时，
         * 生成的 Controller 将继承 {@link CommonController}。</p>
         */
        private boolean inheritCommonController = true;

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
         * ServiceImpl 自定义模板路径（{@code null} 表示使用默认模板）
         */
        private String serviceImplTemplate;
        /**
         * Controller 自定义模板路径（{@code null} 表示使用默认模板）
         */
        private String controllerTemplate;
    }
}
