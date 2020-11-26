/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.config.bootstrap;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.config.configcenter.DynamicConfigurationFactory;
import org.apache.dubbo.common.config.configcenter.wrapper.CompositeDynamicConfiguration;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.lang.ShutdownHookCallback;
import org.apache.dubbo.common.lang.ShutdownHookCallbacks;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.concurrent.ScheduledCompletableFuture;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.DubboShutdownHook;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.ServiceConfigBase;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.config.bootstrap.builders.ApplicationBuilder;
import org.apache.dubbo.config.bootstrap.builders.ConsumerBuilder;
import org.apache.dubbo.config.bootstrap.builders.ProtocolBuilder;
import org.apache.dubbo.config.bootstrap.builders.ProviderBuilder;
import org.apache.dubbo.config.bootstrap.builders.ReferenceBuilder;
import org.apache.dubbo.config.bootstrap.builders.RegistryBuilder;
import org.apache.dubbo.config.bootstrap.builders.ServiceBuilder;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.metadata.ConfigurableMetadataServiceExporter;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.event.GenericEventListener;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.MetadataServiceExporter;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.report.MetadataReportFactory;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;
import org.apache.dubbo.registry.client.metadata.MetadataUtils;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.registry.client.metadata.store.InMemoryWritableMetadataService;
import org.apache.dubbo.registry.client.metadata.store.RemoteMetadataServiceImpl;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.dubbo.common.config.ConfigurationUtils.parseProperties;
import static org.apache.dubbo.common.config.configcenter.DynamicConfiguration.getDynamicConfiguration;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;
import static org.apache.dubbo.common.function.ThrowableAction.execute;
import static org.apache.dubbo.common.utils.StringUtils.isEmpty;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;
import static org.apache.dubbo.metadata.MetadataConstants.DEFAULT_METADATA_PUBLISH_DELAY;
import static org.apache.dubbo.metadata.MetadataConstants.METADATA_PUBLISH_DELAY_KEY;
import static org.apache.dubbo.metadata.WritableMetadataService.getDefaultExtension;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.calInstanceRevision;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.setMetadataStorageType;
import static org.apache.dubbo.registry.support.AbstractRegistryFactory.getServiceDiscoveries;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;

/**
 * See {@link ApplicationModel} and {@link ExtensionLoader} for why this class is designed to be singleton.
 * <p>
 * The bootstrap class of Dubbo
 * <p>
 * Get singleton instance by calling static method {@link #getInstance()}.
 * Designed as singleton because some classes inside Dubbo, such as ExtensionLoader, are designed only for one instance per process.
 *
 * @since 2.7.5
 */
public class DubboBootstrap extends GenericEventListener {

    public static final String DEFAULT_REGISTRY_ID = "REGISTRY#DEFAULT";

    public static final String DEFAULT_PROTOCOL_ID = "PROTOCOL#DEFAULT";

    public static final String DEFAULT_SERVICE_ID = "SERVICE#DEFAULT";

    public static final String DEFAULT_REFERENCE_ID = "REFERENCE#DEFAULT";

    public static final String DEFAULT_PROVIDER_ID = "PROVIDER#DEFAULT";

    public static final String DEFAULT_CONSUMER_ID = "CONSUMER#DEFAULT";

    private static final String NAME = DubboBootstrap.class.getSimpleName();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static volatile DubboBootstrap instance;

    private final AtomicBoolean awaited = new AtomicBoolean(false);

    private final Lock lock = new ReentrantLock();

    private final Condition condition = lock.newCondition();

    private final Lock destroyLock = new ReentrantLock();

    private final ExecutorService executorService = newSingleThreadExecutor();

    private final EventDispatcher eventDispatcher = EventDispatcher.getDefaultExtension();

    private final ExecutorRepository executorRepository = getExtensionLoader(ExecutorRepository.class).getDefaultExtension();

    private final ConfigManager configManager;

    private final Environment environment;

    private ReferenceConfigCache cache;

    private volatile boolean exportAsync;

    private volatile boolean referAsync;

    private AtomicBoolean initialized = new AtomicBoolean(false);

    private AtomicBoolean started = new AtomicBoolean(false);

    private AtomicBoolean ready = new AtomicBoolean(true);

    private AtomicBoolean destroyed = new AtomicBoolean(false);

    private volatile ServiceInstance serviceInstance;

    private volatile MetadataService metadataService;

    private volatile MetadataServiceExporter metadataServiceExporter;

    private List<ServiceConfigBase<?>> exportedServices = new ArrayList<>();

    private List<Future<?>> asyncExportingFutures = new ArrayList<>();

    private List<CompletableFuture<Object>> asyncReferringFutures = new ArrayList<>();

    /**
     * See {@link ApplicationModel} and {@link ExtensionLoader} for why DubboBootstrap is designed to be singleton.
     */
    public static DubboBootstrap getInstance() {
        if (instance == null) {
            synchronized (DubboBootstrap.class) {
                if (instance == null) {
                    instance = new DubboBootstrap();
                }
            }
        }
        return instance;
    }

    private DubboBootstrap() {
        configManager = ApplicationModel.getConfigManager();
        environment = ApplicationModel.getEnvironment();

        DubboShutdownHook.getDubboShutdownHook().register();
        ShutdownHookCallbacks.INSTANCE.addCallback(new ShutdownHookCallback() {
            @Override
            public void callback() throws Throwable {
                DubboBootstrap.this.destroy();
            }
        });
    }

    public void unRegisterShutdownHook() {
        DubboShutdownHook.getDubboShutdownHook().unregister();
    }

    private boolean isOnlyRegisterProvider() {
        Boolean registerConsumer = getApplication().getRegisterConsumer();
        return registerConsumer == null || !registerConsumer;
    }

    private String getMetadataType() {
        String type = getApplication().getMetadataType();
        if (StringUtils.isEmpty(type)) {
            type = DEFAULT_METADATA_STORAGE_TYPE;
        }
        return type;
    }

    public DubboBootstrap metadataReport(MetadataReportConfig metadataReportConfig) {
        configManager.addMetadataReport(metadataReportConfig);
        return this;
    }

    public DubboBootstrap metadataReports(List<MetadataReportConfig> metadataReportConfigs) {
        if (CollectionUtils.isEmpty(metadataReportConfigs)) {
            return this;
        }

        configManager.addMetadataReports(metadataReportConfigs);
        return this;
    }

    // {@link ApplicationConfig} correlative methods

    /**
     * Set the name of application
     *
     * @param name the name of application
     * @return current {@link DubboBootstrap} instance
     */
    public DubboBootstrap application(String name) {
        return application(name, builder -> {
            // DO NOTHING
        });
    }

    /**
     * Set the name of application and it's future build
     *
     * @param name            the name of application
     * @param consumerBuilder {@link ApplicationBuilder}
     * @return current {@link DubboBootstrap} instance
     */
    public DubboBootstrap application(String name, Consumer<ApplicationBuilder> consumerBuilder) {
        ApplicationBuilder builder = createApplicationBuilder(name);
        consumerBuilder.accept(builder);
        return application(builder.build());
    }

    /**
     * Set the {@link ApplicationConfig}
     *
     * @param applicationConfig the {@link ApplicationConfig}
     * @return current {@link DubboBootstrap} instance
     */
    public DubboBootstrap application(ApplicationConfig applicationConfig) {
        configManager.setApplication(applicationConfig);
        return this;
    }


    // {@link RegistryConfig} correlative methods

    /**
     * Add an instance of {@link RegistryConfig} with {@link #DEFAULT_REGISTRY_ID default ID}
     *
     * @param consumerBuilder the {@link Consumer} of {@link RegistryBuilder}
     * @return current {@link DubboBootstrap} instance
     */
    public DubboBootstrap registry(Consumer<RegistryBuilder> consumerBuilder) {
        return registry(DEFAULT_REGISTRY_ID, consumerBuilder);
    }

    /**
     * Add an instance of {@link RegistryConfig} with the specified ID
     *
     * @param id              the {@link RegistryConfig#getId() id}  of {@link RegistryConfig}
     * @param consumerBuilder the {@link Consumer} of {@link RegistryBuilder}
     * @return current {@link DubboBootstrap} instance
     */
    public DubboBootstrap registry(String id, Consumer<RegistryBuilder> consumerBuilder) {
        RegistryBuilder builder = createRegistryBuilder(id);
        consumerBuilder.accept(builder);
        return registry(builder.build());
    }

    /**
     * Add an instance of {@link RegistryConfig}
     *
     * @param registryConfig an instance of {@link RegistryConfig}
     * @return current {@link DubboBootstrap} instance
     */
    public DubboBootstrap registry(RegistryConfig registryConfig) {
        configManager.addRegistry(registryConfig);
        return this;
    }

    /**
     * Add an instance of {@link RegistryConfig}
     *
     * @param registryConfigs the multiple instances of {@link RegistryConfig}
     * @return current {@link DubboBootstrap} instance
     */
    public DubboBootstrap registries(List<RegistryConfig> registryConfigs) {
        if (CollectionUtils.isEmpty(registryConfigs)) {
            return this;
        }
        registryConfigs.forEach(this::registry);
        return this;
    }


    // {@link ProtocolConfig} correlative methods
    public DubboBootstrap protocol(Consumer<ProtocolBuilder> consumerBuilder) {
        return protocol(DEFAULT_PROTOCOL_ID, consumerBuilder);
    }

    public DubboBootstrap protocol(String id, Consumer<ProtocolBuilder> consumerBuilder) {
        ProtocolBuilder builder = createProtocolBuilder(id);
        consumerBuilder.accept(builder);
        return protocol(builder.build());
    }

    public DubboBootstrap protocol(ProtocolConfig protocolConfig) {
        return protocols(asList(protocolConfig));
    }

    public DubboBootstrap protocols(List<ProtocolConfig> protocolConfigs) {
        if (CollectionUtils.isEmpty(protocolConfigs)) {
            return this;
        }
        configManager.addProtocols(protocolConfigs);
        return this;
    }

    // {@link ServiceConfig} correlative methods
    public <S> DubboBootstrap service(Consumer<ServiceBuilder<S>> consumerBuilder) {
        return service(DEFAULT_SERVICE_ID, consumerBuilder);
    }

    public <S> DubboBootstrap service(String id, Consumer<ServiceBuilder<S>> consumerBuilder) {
        ServiceBuilder builder = createServiceBuilder(id);
        consumerBuilder.accept(builder);
        return service(builder.build());
    }

    public DubboBootstrap service(ServiceConfig<?> serviceConfig) {
        configManager.addService(serviceConfig);
        return this;
    }

    public DubboBootstrap services(List<ServiceConfig> serviceConfigs) {
        if (CollectionUtils.isEmpty(serviceConfigs)) {
            return this;
        }
        serviceConfigs.forEach(configManager::addService);
        return this;
    }

    // {@link Reference} correlative methods
    public <S> DubboBootstrap reference(Consumer<ReferenceBuilder<S>> consumerBuilder) {
        return reference(DEFAULT_REFERENCE_ID, consumerBuilder);
    }

    public <S> DubboBootstrap reference(String id, Consumer<ReferenceBuilder<S>> consumerBuilder) {
        ReferenceBuilder builder = createReferenceBuilder(id);
        consumerBuilder.accept(builder);
        return reference(builder.build());
    }

    public DubboBootstrap reference(ReferenceConfig<?> referenceConfig) {
        configManager.addReference(referenceConfig);
        return this;
    }

    public DubboBootstrap references(List<ReferenceConfig> referenceConfigs) {
        if (CollectionUtils.isEmpty(referenceConfigs)) {
            return this;
        }

        referenceConfigs.forEach(configManager::addReference);
        return this;
    }

    // {@link ProviderConfig} correlative methods
    public DubboBootstrap provider(Consumer<ProviderBuilder> builderConsumer) {
        return provider(DEFAULT_PROVIDER_ID, builderConsumer);
    }

    public DubboBootstrap provider(String id, Consumer<ProviderBuilder> builderConsumer) {
        ProviderBuilder builder = createProviderBuilder(id);
        builderConsumer.accept(builder);
        return provider(builder.build());
    }

    public DubboBootstrap provider(ProviderConfig providerConfig) {
        return providers(asList(providerConfig));
    }

    public DubboBootstrap providers(List<ProviderConfig> providerConfigs) {
        if (CollectionUtils.isEmpty(providerConfigs)) {
            return this;
        }

        providerConfigs.forEach(configManager::addProvider);
        return this;
    }

    // {@link ConsumerConfig} correlative methods
    public DubboBootstrap consumer(Consumer<ConsumerBuilder> builderConsumer) {
        return consumer(DEFAULT_CONSUMER_ID, builderConsumer);
    }

    public DubboBootstrap consumer(String id, Consumer<ConsumerBuilder> builderConsumer) {
        ConsumerBuilder builder = createConsumerBuilder(id);
        builderConsumer.accept(builder);
        return consumer(builder.build());
    }

    public DubboBootstrap consumer(ConsumerConfig consumerConfig) {
        return consumers(asList(consumerConfig));
    }

    public DubboBootstrap consumers(List<ConsumerConfig> consumerConfigs) {
        if (CollectionUtils.isEmpty(consumerConfigs)) {
            return this;
        }

        consumerConfigs.forEach(configManager::addConsumer);
        return this;
    }

    // {@link ConfigCenterConfig} correlative methods
    public DubboBootstrap configCenter(ConfigCenterConfig configCenterConfig) {
        return configCenters(asList(configCenterConfig));
    }

    public DubboBootstrap configCenters(List<ConfigCenterConfig> configCenterConfigs) {
        if (CollectionUtils.isEmpty(configCenterConfigs)) {
            return this;
        }
        configManager.addConfigCenters(configCenterConfigs);
        return this;
    }

    public DubboBootstrap monitor(MonitorConfig monitor) {
        configManager.setMonitor(monitor);
        return this;
    }

    public DubboBootstrap metrics(MetricsConfig metrics) {
        configManager.setMetrics(metrics);
        return this;
    }

    public DubboBootstrap module(ModuleConfig module) {
        configManager.setModule(module);
        return this;
    }

    public DubboBootstrap ssl(SslConfig sslConfig) {
        configManager.setSsl(sslConfig);
        return this;
    }

    public DubboBootstrap cache(ReferenceConfigCache cache) {
        this.cache = cache;
        return this;
    }

    public ReferenceConfigCache getCache() {
        if (cache == null) {
            cache = ReferenceConfigCache.getCache();
        }
        return cache;
    }

    public DubboBootstrap exportAsync() {
        this.exportAsync = true;
        return this;
    }

    public DubboBootstrap referAsync() {
        this.referAsync = true;
        return this;
    }

    @Deprecated
    public void init() {
        initialize();
    }

    /**
     * Initialize
     */
    public void initialize() {
        //---CAS方法----乐观锁--需要复习一下悲观锁和乐观锁--
        //java并发---AtomicBoolean的方法compareAndSet（仅执行一次）。
        /* AtomicBoolean initialized  可用在应用程序中（如以原子方式更新的标志），但不能用于替换 Boolean。
        即能够保证在高并发的情况下只有一个线程能够访问这个属性值
        一般情况下，我们使用 AtomicBoolean 高效并发处理 “只初始化一次” 的功能要求*/
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        //初始化框架的代码，这里是静态类，使用的是SPI的加载方式（可以了解一下Java Spi机制）
        /*java spi：
            按照双亲委派模型不能解决所有的类加载问题，
            Java中定义了许多SPI（服务者提供接口，ServiceProviderInterface的缩写），
            这些接口并未实现，由第三方进行实现，例如JDBC、JNDI等。
            依据双亲委派模型，SPI的接口是Java核心库的一部分，
            由BootStrapClassLoader加载的；
            SPI实现的Java类一般是由AppClassLoader来加载。
            Spring中有类似JDK的SPI机制，通过SpringFactoriesLoader代替JDK中的ServiceLoader，
            通过META-INF/spring.factories文件代替META-INF/service目录下的描述文件
        */
        //初始化FrameworkExt类,遍历执行配置管理器,环境等的初始化方法
        ApplicationModel.initFrameworkExts();
        //配置管理器读取并加载配置中心的配置信息,并准备环境,最后存入environment中
        startConfigCenter();
        //加载远程配置
        loadRemoteConfigs();
        //检查全局配置
        checkGlobalConfigs();

        // @since 2.7.8
        startMetadataCenter();
        //初始化元数据服务,加载本地元数据服务提供者类
        initMetadataService();
        //初始化事件监听器
        initEventListener();

        if (logger.isInfoEnabled()) {
            logger.info(NAME + " has been initialized!");
        }
    }

    private void checkGlobalConfigs() {
        // check Application
        ConfigValidationUtils.validateApplicationConfig(getApplication());

        // check Metadata
        Collection<MetadataReportConfig> metadatas = configManager.getMetadataConfigs();
        if (CollectionUtils.isEmpty(metadatas)) {
            MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
            metadataReportConfig.refresh();
            if (metadataReportConfig.isValid()) {
                configManager.addMetadataReport(metadataReportConfig);
                metadatas = configManager.getMetadataConfigs();
            }
        }
        if (CollectionUtils.isNotEmpty(metadatas)) {
            for (MetadataReportConfig metadataReportConfig : metadatas) {
                metadataReportConfig.refresh();
                ConfigValidationUtils.validateMetadataConfig(metadataReportConfig);
            }
        }

        // check Provider
        Collection<ProviderConfig> providers = configManager.getProviders();
        if (CollectionUtils.isEmpty(providers)) {
            configManager.getDefaultProvider().orElseGet(() -> {
                ProviderConfig providerConfig = new ProviderConfig();
                configManager.addProvider(providerConfig);
                providerConfig.refresh();
                return providerConfig;
            });
        }
        for (ProviderConfig providerConfig : configManager.getProviders()) {
            ConfigValidationUtils.validateProviderConfig(providerConfig);
        }
        // check Consumer
        Collection<ConsumerConfig> consumers = configManager.getConsumers();
        if (CollectionUtils.isEmpty(consumers)) {
            configManager.getDefaultConsumer().orElseGet(() -> {
                ConsumerConfig consumerConfig = new ConsumerConfig();
                configManager.addConsumer(consumerConfig);
                consumerConfig.refresh();
                return consumerConfig;
            });
        }
        for (ConsumerConfig consumerConfig : configManager.getConsumers()) {
            ConfigValidationUtils.validateConsumerConfig(consumerConfig);
        }

        // check Monitor
        ConfigValidationUtils.validateMonitorConfig(getMonitor());
        // check Metrics
        ConfigValidationUtils.validateMetricsConfig(getMetrics());
        // check Module
        ConfigValidationUtils.validateModuleConfig(getModule());
        // check Ssl
        ConfigValidationUtils.validateSslConfig(getSsl());
    }

    private void startConfigCenter() {

        useRegistryAsConfigCenterIfNecessary();

        Collection<ConfigCenterConfig> configCenters = configManager.getConfigCenters();

        // check Config Center
        if (CollectionUtils.isEmpty(configCenters)) {
            ConfigCenterConfig configCenterConfig = new ConfigCenterConfig();
            configCenterConfig.refresh();
            if (configCenterConfig.isValid()) {
                configManager.addConfigCenter(configCenterConfig);
                configCenters = configManager.getConfigCenters();
            }
        } else {
            for (ConfigCenterConfig configCenterConfig : configCenters) {
                configCenterConfig.refresh();
                ConfigValidationUtils.validateConfigCenterConfig(configCenterConfig);
            }
        }

        if (CollectionUtils.isNotEmpty(configCenters)) {
            CompositeDynamicConfiguration compositeDynamicConfiguration = new CompositeDynamicConfiguration();
            for (ConfigCenterConfig configCenter : configCenters) {
                compositeDynamicConfiguration.addConfiguration(prepareEnvironment(configCenter));
            }
            environment.setDynamicConfiguration(compositeDynamicConfiguration);
        }
        configManager.refreshAll();
    }

    private void startMetadataCenter() {

        useRegistryAsMetadataCenterIfNecessary();

        ApplicationConfig applicationConfig = getApplication();

        String metadataType = applicationConfig.getMetadataType();
        // FIXME, multiple metadata config support.
        Collection<MetadataReportConfig> metadataReportConfigs = configManager.getMetadataConfigs();
        if (CollectionUtils.isEmpty(metadataReportConfigs)) {
            if (REMOTE_METADATA_STORAGE_TYPE.equals(metadataType)) {
                throw new IllegalStateException("No MetadataConfig found, Metadata Center address is required when 'metadata=remote' is enabled.");
            }
            return;
        }

        for (MetadataReportConfig metadataReportConfig : metadataReportConfigs) {
            ConfigValidationUtils.validateMetadataConfig(metadataReportConfig);
            if (!metadataReportConfig.isValid()) {
                return;
            }
            MetadataReportInstance.init(metadataReportConfig);
        }
    }

    /**
     * For compatibility purpose, use registry as the default config center when
     * there's no config center specified explicitly and
     * useAsConfigCenter of registryConfig is null or true
     */
    private void useRegistryAsConfigCenterIfNecessary() {
        // we use the loading status of DynamicConfiguration to decide whether ConfigCenter has been initiated.
        if (environment.getDynamicConfiguration().isPresent()) {
            return;
        }

        if (CollectionUtils.isNotEmpty(configManager.getConfigCenters())) {
            return;
        }

        configManager
                .getDefaultRegistries()
                .stream()
                .filter(this::isUsedRegistryAsConfigCenter)
                .map(this::registryAsConfigCenter)
                .forEach(configManager::addConfigCenter);
    }

    private boolean isUsedRegistryAsConfigCenter(RegistryConfig registryConfig) {
        return isUsedRegistryAsCenter(registryConfig, registryConfig::getUseAsConfigCenter, "config",
                DynamicConfigurationFactory.class);
    }

    private ConfigCenterConfig registryAsConfigCenter(RegistryConfig registryConfig) {
        String protocol = registryConfig.getProtocol();
        Integer port = registryConfig.getPort();
        String id = "config-center-" + protocol + "-" + port;
        ConfigCenterConfig cc = new ConfigCenterConfig();
        cc.setId(id);
        if (cc.getParameters() == null) {
            cc.setParameters(new HashMap<>());
        }
        if (registryConfig.getParameters() != null) {
            cc.getParameters().putAll(registryConfig.getParameters()); // copy the parameters
        }
        cc.getParameters().put(CLIENT_KEY, registryConfig.getClient());
        cc.setProtocol(protocol);
        cc.setPort(port);
        if (StringUtils.isNotEmpty(registryConfig.getGroup())) {
            cc.setGroup(registryConfig.getGroup());
        }
        cc.setAddress(getRegistryCompatibleAddress(registryConfig));
        cc.setNamespace(registryConfig.getGroup());
        cc.setUsername(registryConfig.getUsername());
        cc.setPassword(registryConfig.getPassword());
        if (registryConfig.getTimeout() != null) {
            cc.setTimeout(registryConfig.getTimeout().longValue());
        }
        cc.setHighestPriority(false);
        return cc;
    }

    private void useRegistryAsMetadataCenterIfNecessary() {

        Collection<MetadataReportConfig> metadataConfigs = configManager.getMetadataConfigs();

        if (CollectionUtils.isNotEmpty(metadataConfigs)) {
            return;
        }

        configManager
                .getDefaultRegistries()
                .stream()
                .filter(this::isUsedRegistryAsMetadataCenter)
                .map(this::registryAsMetadataCenter)
                .forEach(configManager::addMetadataReport);

    }

    private boolean isUsedRegistryAsMetadataCenter(RegistryConfig registryConfig) {
        return isUsedRegistryAsCenter(registryConfig, registryConfig::getUseAsMetadataCenter, "metadata",
                MetadataReportFactory.class);
    }

    /**
     * Is used the specified registry as a center infrastructure
     *
     * @param registryConfig       the {@link RegistryConfig}
     * @param usedRegistryAsCenter the configured value on
     * @param centerType           the type name of center
     * @param extensionClass       an extension class of a center infrastructure
     * @return
     * @since 2.7.8
     */
    private boolean isUsedRegistryAsCenter(RegistryConfig registryConfig, Supplier<Boolean> usedRegistryAsCenter,
                                           String centerType,
                                           Class<?> extensionClass) {
        final boolean supported;

        Boolean configuredValue = usedRegistryAsCenter.get();
        if (configuredValue != null) { // If configured, take its value.
            supported = configuredValue.booleanValue();
        } else {                       // Or check the extension existence
            String protocol = registryConfig.getProtocol();
            supported = supportsExtension(extensionClass, protocol);
            if (logger.isInfoEnabled()) {
                logger.info(format("No value is configured in the registry, the %s extension[name : %s] %s as the %s center"
                        , extensionClass.getSimpleName(), protocol, supported ? "supports" : "does not support", centerType));
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info(format("The registry[%s] will be %s as the %s center", registryConfig,
                    supported ? "used" : "not used", centerType));
        }
        return supported;
    }

    /**
     * Supports the extension with the specified class and name
     *
     * @param extensionClass the {@link Class} of extension
     * @param name           the name of extension
     * @return if supports, return <code>true</code>, or <code>false</code>
     * @since 2.7.8
     */
    private boolean supportsExtension(Class<?> extensionClass, String name) {
        if (isNotEmpty(name)) {
            ExtensionLoader extensionLoader = getExtensionLoader(extensionClass);
            return extensionLoader.hasExtension(name);
        }
        return false;
    }

    private MetadataReportConfig registryAsMetadataCenter(RegistryConfig registryConfig) {
        String protocol = registryConfig.getProtocol();
        Integer port = registryConfig.getPort();
        String id = "metadata-center-" + protocol + "-" + port;
        MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
        metadataReportConfig.setId(id);
        if (metadataReportConfig.getParameters() == null) {
            metadataReportConfig.setParameters(new HashMap<>());
        }
        if (registryConfig.getParameters() != null) {
            metadataReportConfig.getParameters().putAll(registryConfig.getParameters()); // copy the parameters
        }
        metadataReportConfig.getParameters().put(CLIENT_KEY, registryConfig.getClient());
        metadataReportConfig.setGroup(registryConfig.getGroup());
        metadataReportConfig.setAddress(getRegistryCompatibleAddress(registryConfig));
        metadataReportConfig.setUsername(registryConfig.getUsername());
        metadataReportConfig.setPassword(registryConfig.getPassword());
        metadataReportConfig.setTimeout(registryConfig.getTimeout());
        return metadataReportConfig;
    }

    private String getRegistryCompatibleAddress(RegistryConfig registryConfig) {
        String registryAddress = registryConfig.getAddress();
        String[] addresses = REGISTRY_SPLIT_PATTERN.split(registryAddress);
        if (ArrayUtils.isEmpty(addresses)) {
            throw new IllegalStateException("Invalid registry address found.");
        }
        String address = addresses[0];
        // since 2.7.8
        // Issue : https://github.com/apache/dubbo/issues/6476
        StringBuilder metadataAddressBuilder = new StringBuilder();
        URL url = URL.valueOf(address);
        String protocolFromAddress = url.getProtocol();
        if (isEmpty(protocolFromAddress)) {
            // If the protocol from address is missing, is like :
            // "dubbo.registry.address = 127.0.0.1:2181"
            String protocolFromConfig = registryConfig.getProtocol();
            metadataAddressBuilder.append(protocolFromConfig).append("://");
        }
        metadataAddressBuilder.append(address);
        return metadataAddressBuilder.toString();
    }

    private void loadRemoteConfigs() {
        // registry ids to registry configs
        List<RegistryConfig> tmpRegistries = new ArrayList<>();
        Set<String> registryIds = configManager.getRegistryIds();
        registryIds.forEach(id -> {
            if (tmpRegistries.stream().noneMatch(reg -> reg.getId().equals(id))) {
                tmpRegistries.add(configManager.getRegistry(id).orElseGet(() -> {
                    RegistryConfig registryConfig = new RegistryConfig();
                    registryConfig.setId(id);
                    registryConfig.refresh();
                    return registryConfig;
                }));
            }
        });

        configManager.addRegistries(tmpRegistries);

        // protocol ids to protocol configs
        List<ProtocolConfig> tmpProtocols = new ArrayList<>();
        Set<String> protocolIds = configManager.getProtocolIds();
        protocolIds.forEach(id -> {
            if (tmpProtocols.stream().noneMatch(prot -> prot.getId().equals(id))) {
                tmpProtocols.add(configManager.getProtocol(id).orElseGet(() -> {
                    ProtocolConfig protocolConfig = new ProtocolConfig();
                    protocolConfig.setId(id);
                    protocolConfig.refresh();
                    return protocolConfig;
                }));
            }
        });

        configManager.addProtocols(tmpProtocols);
    }


    /**
     * Initialize {@link MetadataService} from {@link WritableMetadataService}'s extension
     */
    private void initMetadataService() {
        startMetadataCenter();
        this.metadataService = getDefaultExtension();
        this.metadataServiceExporter = new ConfigurableMetadataServiceExporter(metadataService);
    }

    /**
     * Initialize {@link EventListener}
     */
    private void initEventListener() {
        // Add current instance into listeners
        addEventListener(this);
    }

    /**
     * Start the bootstrap
     */
    public DubboBootstrap start() {
        if (started.compareAndSet(false, true)) {
            ready.set(false);
            //0、初始化，比如启动配置中心、载入远程配置、检查全局配置、启动元数据中心、初始化元数据服务等等。。。。
            initialize();
            if (logger.isInfoEnabled()) {
                logger.info(NAME + " is starting...");
            }
            // 1. export Dubbo Services
            /**暴露dubbo服务提供者，最主要的是这个方法*/
            /*
                心态有点崩，简单来说
                1.根据url调用 doLocalExport 导出服务
                2.根据 register 的值决定是否注册服务
                3.向注册中心进行订阅 override 数据
            */
            //暴露服务
            exportServices();

            // Not only provider register
            // 不仅提供者注册
            if (!isOnlyRegisterProvider() || hasExportedServices()) {
                // 2. export MetadataService
                // 暴露元数据服务 如果不仅仅是服务提供者，那就发布元数据服务MetadataService
                exportMetadataService();
                //3. Register the local ServiceInstance if required
                // 如果需要,注册本地服务实例
                registerServiceInstance();
            }
            //订阅服务
            //4、引用服务，如果一个应用即是提供者也是消费者那么此处就需要服务消费，服务消费后面再进行详细分析。
            referServices();
            if (asyncExportingFutures.size() > 0) {
                new Thread(() -> {
                    try {
                        this.awaitFinish();
                    } catch (Exception e) {
                        logger.warn(NAME + " exportAsync occurred an exception.");
                    }
                    ready.set(true);
                    if (logger.isInfoEnabled()) {
                        logger.info(NAME + " is ready.");
                    }
                }).start();
            } else {
                ready.set(true);
                if (logger.isInfoEnabled()) {
                    logger.info(NAME + " is ready.");
                }
            }
            if (logger.isInfoEnabled()) {
                logger.info(NAME + " has started.");
            }
        }
        return this;
    }

    private boolean hasExportedServices() {
        return !metadataService.getExportedURLs().isEmpty();
    }

    /**
     * Block current thread to be await.
     *
     * @return {@link DubboBootstrap}
     */
    public DubboBootstrap await() {
        // if has been waited, no need to wait again, return immediately
        if (!awaited.get()) {
            if (!executorService.isShutdown()) {
                executeMutually(() -> {
                    while (!awaited.get()) {
                        if (logger.isInfoEnabled()) {
                            logger.info(NAME + " awaiting ...");
                        }
                        try {
                            condition.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            }
        }
        return this;
    }

    public DubboBootstrap awaitFinish() throws Exception {
        logger.info(NAME + " waiting services exporting / referring ...");
        if (exportAsync && asyncExportingFutures.size() > 0) {
            CompletableFuture future = CompletableFuture.allOf(asyncExportingFutures.toArray(new CompletableFuture[0]));
            future.get();
        }
        if (referAsync && asyncReferringFutures.size() > 0) {
            CompletableFuture future = CompletableFuture.allOf(asyncReferringFutures.toArray(new CompletableFuture[0]));
            future.get();
        }

        logger.info("Service export / refer finished.");
        return this;
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public boolean isStarted() {
        return started.get();
    }

    public boolean isReady() {
        return ready.get();
    }

    public DubboBootstrap stop() throws IllegalStateException {
        destroy();
        return this;
    }
    /* serve for builder apis, begin */

    private ApplicationBuilder createApplicationBuilder(String name) {
        return new ApplicationBuilder().name(name);
    }

    private RegistryBuilder createRegistryBuilder(String id) {
        return new RegistryBuilder().id(id);
    }

    private ProtocolBuilder createProtocolBuilder(String id) {
        return new ProtocolBuilder().id(id);
    }

    private ServiceBuilder createServiceBuilder(String id) {
        return new ServiceBuilder().id(id);
    }

    private ReferenceBuilder createReferenceBuilder(String id) {
        return new ReferenceBuilder().id(id);
    }

    private ProviderBuilder createProviderBuilder(String id) {
        return new ProviderBuilder().id(id);
    }

    private ConsumerBuilder createConsumerBuilder(String id) {
        return new ConsumerBuilder().id(id);
    }
    /* serve for builder apis, end */

    private DynamicConfiguration prepareEnvironment(ConfigCenterConfig configCenter) {
        if (configCenter.isValid()) {
            if (!configCenter.checkOrUpdateInited()) {
                return null;
            }
            DynamicConfiguration dynamicConfiguration = getDynamicConfiguration(configCenter.toUrl());
            String configContent = dynamicConfiguration.getProperties(configCenter.getConfigFile(), configCenter.getGroup());

            String appGroup = getApplication().getName();
            String appConfigContent = null;
            if (isNotEmpty(appGroup)) {
                appConfigContent = dynamicConfiguration.getProperties
                        (isNotEmpty(configCenter.getAppConfigFile()) ? configCenter.getAppConfigFile() : configCenter.getConfigFile(),
                                appGroup
                        );
            }
            try {
                environment.setConfigCenterFirst(configCenter.isHighestPriority());
                environment.updateExternalConfigurationMap(parseProperties(configContent));
                environment.updateAppExternalConfigurationMap(parseProperties(appConfigContent));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to parse configurations from Config Center.", e);
            }
            return dynamicConfiguration;
        }
        return null;
    }

    /**
     * Add an instance of {@link EventListener}
     *
     * @param listener {@link EventListener}
     * @return {@link DubboBootstrap}
     */
    public DubboBootstrap addEventListener(EventListener<?> listener) {
        eventDispatcher.addEventListener(listener);
        return this;
    }

    /**
     * export {@link MetadataService}
     */
    private void exportMetadataService() {
        metadataServiceExporter.export();
    }

    private void unexportMetadataService() {
        if (metadataServiceExporter != null && metadataServiceExporter.isExported()) {
            metadataServiceExporter.unexport();
        }
    }

    private void exportServices() {
        /**此方法最需要关注的是暴露服务发布方法：-----sc.export();------*/
        /*从配置管理器中获取到需要发布的服务列表，然后循环进行每一个服务发布，dubbo的每一个配置
        在触发完成后都会将其添加到configManager的configsCache的Map属性中，当然初始化的过程也是使用
        spring的一些基础组件来实现的，因此我们能够在这里通过configManager获取到我们所需要发布的服务列
        表，服务列表中都是使用一个ServiceBean来进行封装的，ServiceBean又继承了ServiceConfig抽象类，因此也
        就有了服务发布的功能。*/
        /*注意:本例中的provider是直接启用ServiceConfig类,跳过了ServiceBean:
              ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();
              service.setInterface(DemoService.class);
              service.setRef(new DemoServiceImpl());
         而在真正的调用中,服务暴露是由com.alibaba.dubbo.config.spring.ServiceBean这个类来实现的，
         这个类是spring通过解析<dubbo:service>节点创建的单例Bean，每一个<dubbo:service>都会创建一个ServiceBean。
         */

        //获取所有ServiceBean遍历调用export()  sc-->configManager.getServices()的list中的服务
        configManager.getServices().forEach(sc -> {
            // TODO, compatible with ServiceConfig.export()
            //1、先将ServiceConfigBase实例强转成父类ServiceConfig,然后设置这个ServiceConfigBase的bootstrap属性为当前的Bootstrap实例。
            ServiceConfig serviceConfig = (ServiceConfig) sc;
            serviceConfig.setBootstrap(this);
            //异步暴露服务
            if (exportAsync) {
                /*线程池开启子线程异步执行暴露服务*/
                //2、如果需要异步发布就获取一个服务发布线程池进行服务的异步发布。
                ExecutorService executor = executorRepository.getServiceExporterExecutor();
                /*获取线程结束后的执行状态、已有结果（跟踪线程已有信息），包装进asyncExportingFutures待使用*/
                Future<?> future = executor.submit(() -> {
                    //暴露服务
                    sc.export();
                    exportedServices.add(sc);
                });
                asyncExportingFutures.add(future);
            } else {
                //3、同步进行服务发布。
                sc.export();
                //4、记录已经发布好的服务。
                exportedServices.add(sc);
            }
        });
    }

    private void unexportServices() {
        exportedServices.forEach(sc -> {
            configManager.removeConfig(sc);
            sc.unexport();
        });

        asyncExportingFutures.forEach(future -> {
            if (!future.isDone()) {
                future.cancel(true);
            }
        });
        asyncExportingFutures.clear();
        exportedServices.clear();
    }

    private void referServices() {
        if (cache == null) {
            cache = ReferenceConfigCache.getCache();
        }

        configManager.getReferences().forEach(rc -> {
            // TODO, compatible with  ReferenceConfig.refer()
            ReferenceConfig referenceConfig = (ReferenceConfig) rc;
            referenceConfig.setBootstrap(this);

            if (rc.shouldInit()) {
                if (referAsync) {
                    CompletableFuture<Object> future = ScheduledCompletableFuture.submit(
                            executorRepository.getServiceExporterExecutor(),
                            () -> cache.get(rc)
                    );
                    asyncReferringFutures.add(future);
                } else {
                    cache.get(rc);
                }
            }
        });
    }

    private void unreferServices() {
        if (cache == null) {
            cache = ReferenceConfigCache.getCache();
        }

        asyncReferringFutures.forEach(future -> {
            if (!future.isDone()) {
                future.cancel(true);
            }
        });
        asyncReferringFutures.clear();
        cache.destroyAll();
    }

    private void registerServiceInstance() {
        if (CollectionUtils.isEmpty(getServiceDiscoveries())) {
            return;
        }

        ApplicationConfig application = getApplication();

        String serviceName = application.getName();

        URL exportedURL = selectMetadataServiceExportedURL();

        String host = exportedURL.getHost();

        int port = exportedURL.getPort();

        ServiceInstance serviceInstance = createServiceInstance(serviceName, host, port);

        doRegisterServiceInstance(serviceInstance);

        // scheduled task for updating Metadata and ServiceInstance
        executorRepository.nextScheduledExecutor().scheduleAtFixedRate(() -> {
            InMemoryWritableMetadataService localMetadataService = (InMemoryWritableMetadataService) WritableMetadataService.getDefaultExtension();
            localMetadataService.blockUntilUpdated();
            ServiceInstanceMetadataUtils.refreshMetadataAndInstance();
        }, 0, ConfigurationUtils.get(METADATA_PUBLISH_DELAY_KEY, DEFAULT_METADATA_PUBLISH_DELAY), TimeUnit.MICROSECONDS);
    }

    private void doRegisterServiceInstance(ServiceInstance serviceInstance) {
        //FIXME
        publishMetadataToRemote(serviceInstance);

        getServiceDiscoveries().forEach(serviceDiscovery ->
        {
            calInstanceRevision(serviceDiscovery, serviceInstance);
            // register metadata
            serviceDiscovery.register(serviceInstance);
        });
    }

    private void publishMetadataToRemote(ServiceInstance serviceInstance) {
//        InMemoryWritableMetadataService localMetadataService = (InMemoryWritableMetadataService)WritableMetadataService.getDefaultExtension();
//        localMetadataService.blockUntilUpdated();
        RemoteMetadataServiceImpl remoteMetadataService = MetadataUtils.getRemoteMetadataService();
        remoteMetadataService.publishMetadata(serviceInstance.getServiceName());
    }

    private URL selectMetadataServiceExportedURL() {

        URL selectedURL = null;

        SortedSet<String> urlValues = metadataService.getExportedURLs();

        for (String urlValue : urlValues) {
            URL url = URL.valueOf(urlValue);
            if (MetadataService.class.getName().equals(url.getServiceInterface())) {
                continue;
            }
            if ("rest".equals(url.getProtocol())) { // REST first
                selectedURL = url;
                break;
            } else {
                selectedURL = url; // If not found, take any one
            }
        }

        if (selectedURL == null && CollectionUtils.isNotEmpty(urlValues)) {
            selectedURL = URL.valueOf(urlValues.iterator().next());
        }

        return selectedURL;
    }

    private void unregisterServiceInstance() {
        if (serviceInstance != null) {
            getServiceDiscoveries().forEach(serviceDiscovery -> {
                serviceDiscovery.unregister(serviceInstance);
            });
        }
    }

    private ServiceInstance createServiceInstance(String serviceName, String host, int port) {
        this.serviceInstance = new DefaultServiceInstance(serviceName, host, port);
        setMetadataStorageType(serviceInstance, getMetadataType());

        ExtensionLoader<ServiceInstanceCustomizer> loader =
                ExtensionLoader.getExtensionLoader(ServiceInstanceCustomizer.class);
        // FIXME, sort customizer before apply
        loader.getSupportedExtensionInstances().forEach(customizer -> {
            // customizes
            customizer.customize(this.serviceInstance);
        });

        return this.serviceInstance;
    }

    public void destroy() {
        if (destroyLock.tryLock()) {
            try {
                DubboShutdownHook.destroyAll();

                if (started.compareAndSet(true, false)
                        && destroyed.compareAndSet(false, true)) {

                    unregisterServiceInstance();
                    unexportMetadataService();
                    unexportServices();
                    unreferServices();

                    destroyRegistries();
                    DubboShutdownHook.destroyProtocols();
                    destroyServiceDiscoveries();

                    clear();
                    shutdown();
                    release();
                }
            } finally {
                destroyLock.unlock();
            }
        }
    }

    private void destroyRegistries() {
        AbstractRegistryFactory.destroyAll();
    }

    private void destroyServiceDiscoveries() {
        getServiceDiscoveries().forEach(serviceDiscovery -> {
            execute(serviceDiscovery::destroy);
        });
        if (logger.isDebugEnabled()) {
            logger.debug(NAME + "'s all ServiceDiscoveries have been destroyed.");
        }
    }

    private void clear() {
        clearConfigs();
        clearApplicationModel();
    }

    private void clearApplicationModel() {

    }

    private void clearConfigs() {
        configManager.destroy();
        if (logger.isDebugEnabled()) {
            logger.debug(NAME + "'s configs have been clear.");
        }
    }

    private void release() {
        executeMutually(() -> {
            while (awaited.compareAndSet(false, true)) {
                if (logger.isInfoEnabled()) {
                    logger.info(NAME + " is about to shutdown...");
                }
                condition.signalAll();
            }
        });
    }

    private void shutdown() {
        if (!executorService.isShutdown()) {
            // Shutdown executorService
            executorService.shutdown();
        }
    }

    private void executeMutually(Runnable runnable) {
        try {
            lock.lock();
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    public ApplicationConfig getApplication() {
        ApplicationConfig application = configManager
                .getApplication()
                .orElseGet(() -> {
                    ApplicationConfig applicationConfig = new ApplicationConfig();
                    configManager.setApplication(applicationConfig);
                    return applicationConfig;
                });

        application.refresh();
        return application;
    }

    private MonitorConfig getMonitor() {
        MonitorConfig monitor = configManager
                .getMonitor()
                .orElseGet(() -> {
                    MonitorConfig monitorConfig = new MonitorConfig();
                    configManager.setMonitor(monitorConfig);
                    return monitorConfig;
                });

        monitor.refresh();
        return monitor;
    }

    private MetricsConfig getMetrics() {
        MetricsConfig metrics = configManager
                .getMetrics()
                .orElseGet(() -> {
                    MetricsConfig metricsConfig = new MetricsConfig();
                    configManager.setMetrics(metricsConfig);
                    return metricsConfig;
                });
        metrics.refresh();
        return metrics;
    }

    private ModuleConfig getModule() {
        ModuleConfig module = configManager
                .getModule()
                .orElseGet(() -> {
                    ModuleConfig moduleConfig = new ModuleConfig();
                    configManager.setModule(moduleConfig);
                    return moduleConfig;
                });

        module.refresh();
        return module;
    }

    private SslConfig getSsl() {
        SslConfig ssl = configManager
                .getSsl()
                .orElseGet(() -> {
                    SslConfig sslConfig = new SslConfig();
                    configManager.setSsl(sslConfig);
                    return sslConfig;
                });

        ssl.refresh();
        return ssl;
    }
}
