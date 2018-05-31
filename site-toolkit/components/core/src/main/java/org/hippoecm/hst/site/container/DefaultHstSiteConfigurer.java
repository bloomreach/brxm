/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.site.container;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.commons.configuration.AbstractFileConfiguration;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.hippoecm.hst.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.configuration.model.ConfigurationNodesLoadingException;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.container.event.ComponentManagerBeforeReplacedEvent;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.util.PropertyParser;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.hippoecm.hst.util.ServletConfigUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.ServletContextRegistry;
import org.onehippo.repository.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DefaultHstSiteConfigurer, implementing {@link HstSiteConfigurer}.
 * 
 * This is responsible for initializing all the components that can be accessed via HstServices
 * from the each HST-based applications.
 * <P>
 * The configuration could be set by a properties file or an xml file.
 * If you would set the configuration by a properties file, you can set an init parameter 
 * named 'hst-config-properties' for the servlet context. 
 * </P>
 * <P>
 * <EM>The parameter value for the properties file or the xml file is regarded as a web application
 * context relative path or file system relative path if the path does not start with 'file:'.
 * So, you should use a 'file:' prefixed URI for the path parameter value if you want to set an absolute path.
 * When the path starts with a leading slash ('/'), the path is regarded as a servlet context relative path
 * or an absolute file path if the servlet context relative resource is not found.
 * If the path does not start with 'file:' nor with a leading slash ('/'), it is regarded as a relative path of the file system.
 * </EM>
 * </P>
 * <P>
 * For example, you can set context init parameter instead of the config init parameter like the following: 
 * <PRE><CODE>
 *  &lt;context-param>
 *    &lt;param-name>hst-config-properties&lt;/param-name>
 *    &lt;param-value>/WEB-INF/hst-config.properties&lt;/param-value>
 *  &lt;/context-param>
 * </CODE></PRE>
 * <BR/>
 * If you don't provide the init parameter named 'hst-config-properties' at all, the value is set to 
 * '/WEB-INF/hst-config.properties' by default.
 * </P>
 * <P>
 * Also, the configuration can be set by an XML file which is of the XML configuration format of
 * <A href="http://commons.apache.org/configuration/">Apache Commons Configuration</A>.
 * If you want to set the configuration by the Apache Commons Configuration XML file, you should provide
 * an parameter named 'hst-configuration' for servlet context. For example,
 * <PRE><CODE>
 *  &lt;context-param>
 *    &lt;param-name>hst-configuration&lt;/param-name>
 *    &lt;param-value>/WEB-INF/hst-configuration.xml&lt;/param-value>
 *  &lt;/context-param>
 * </CODE></PRE>
 * <BR/>
 * For your information, you can configure the <CODE>/WEB-INF/hst-configuration.xml</CODE> file like the following example.
 * In this example, you can see that system properties can be aggregated, multiple properties files can be added and 
 * system property values can be used to configure other properties file paths as well: 
 * <PRE><CODE>
 * &lt;?xml version='1.0'?>
 * &lt;configuration>
 *   &lt;system/>
 *   &lt;properties fileName='${catalina.home}/conf/hst-config-1.properties'/>
 *   &lt;properties fileName='${catalina.home}/conf/hst-config-2.properties'/>
 * &lt;/configuration>
 * </CODE></PRE>
 * <EM>Please refer to the documentation of <A href="http://commons.apache.org/configuration/">Apache Commons Configuration</A> for details.</EM>
 * <BR/>
 * If you don't provide the parameter named 'hst-config-properties' at all, the value is set to 
 * '/WEB-INF/hst-configuration.xml' by default.
 * <BR/>
 * <EM>The parameter value for the properties file or the xml file is regarded as a web application
 * context relative path or file system relative path if the path does not start with 'file:'.
 * So, you should use a 'file:' prefixed URI for the path parameter value if you want to set an absolute path.
 * When the path starts with a leading slash ('/'), the path is regarded as a servlet context relative path.
 * If the path does not start with 'file:' nor with a leading slash ('/'), it is regarded as a relative path of the file system.
 * </EM>
 * </P>
 */
public class DefaultHstSiteConfigurer implements HstSiteConfigurer {

    private final static Logger log = LoggerFactory.getLogger(DefaultHstSiteConfigurer.class);

    private static final long serialVersionUID = 1L;

    private static final String HST_CONFIGURATION_XML = "hst-configuration.xml";

    private static final String HST_CONFIG_PROPERTIES = "hst-config.properties";

    private static final String HST_CONFIG_ENV_PROPERTIES = "${catalina.base}/conf/hst.properties";

    private static final String HST_CONFIGURATION_REFRESH_DELAY_PARAM = "hst-config-refresh-delay";

    private static final String FORCEFUL_REINIT_PARAM = "forceful.reinit";

    private static final String ASSEMBLY_OVERRIDES_CONFIGURATIONS_PARAM = "assembly.overrides";

    private static final long DEFAULT_CONFIGURATION_REFRESH_DELAY = 0L;

    private String [] assemblyOverridesConfigurations = { "META-INF/hst-assembly/overrides/*.xml" };

    private boolean initialized;

    private boolean forcefulReinitialization;

    private Configuration configuration;

    private long configurationRefreshDelay;

    private boolean hstSystemPropertiesOverride = true;

    private boolean lazyHstConfigurationLoading = false;

    private String hstConfigEnvProperties = HST_CONFIG_ENV_PROPERTIES;

    // -------------------------------------------------------------------
    // I N I T I A L I Z A T I O N
    // -------------------------------------------------------------------
    private static final String INIT_START_MSG = "HstSiteConfigurer Starting Initialization...";
    private static final String INIT_DONE_MSG = "HstSiteConfigurer Initialization complete, Ready to service requests.";

    private HstSiteConfigurationChangesChecker hstSiteConfigurationChangesCheckerThread;

    private ServletContext servletContext;

    private Thread initThread;

    public DefaultHstSiteConfigurer() {
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public void initialize() throws ContainerException {
        if (getServletContext() == null) {
            throw new ContainerException("No ServletContext available.");
        }

        ServletContextRegistry.register(getServletContext(), ServletContextRegistry.WebAppType.HST);
        log.debug("Registered servlet context '{}' at {}",
                getServletContext().getContextPath(), ServletContextRegistry.class.getName());

        // If the forceful re-initialization option is not turned on
        // and the component manager were initialized in other web application,
        // then just skip the following.
        // If this servlet is initialized inside a site web application 
        // and other web application already initialized the component manager,
        // then the followings are to be skipped.
        forcefulReinitialization = Boolean.parseBoolean(getConfigOrContextInitParameter(FORCEFUL_REINIT_PARAM, null));
        if (!forcefulReinitialization && HstServices.isAvailable()) {
            return;
        }

        lazyHstConfigurationLoading = BooleanUtils.toBoolean(getConfigOrContextInitParameter(HST_LAZY_HST_CONFIGURATION_LOADING_PARAM, "false"));

        hstSystemPropertiesOverride = BooleanUtils.toBoolean(getConfigOrContextInitParameter(HST_SYSTEM_PROPERTIES_OVERRIDE_PARAM, "true"));
        hstConfigEnvProperties = getConfigOrContextInitParameter(HST_CONFIG_ENV_PROPERTIES_PARAM, HST_CONFIG_ENV_PROPERTIES);

        configurationRefreshDelay = NumberUtils.toLong(getConfigOrContextInitParameter(HST_CONFIGURATION_REFRESH_DELAY_PARAM, null), DEFAULT_CONFIGURATION_REFRESH_DELAY);
        this.configuration = getConfiguration();

        if (configurationRefreshDelay > 0L) {
            setUpFileConfigurationReloadingStrategies();
        }

        if (this.configuration.containsKey(ASSEMBLY_OVERRIDES_CONFIGURATIONS_PARAM)) {
            assemblyOverridesConfigurations = this.configuration.getStringArray(ASSEMBLY_OVERRIDES_CONFIGURATIONS_PARAM);
        }

        if (HippoServiceRegistry.getService(RepositoryService.class) != null) {
            initializeComponentManager();
        } else {
            initThread = new Thread(() -> {
                boolean retry = true;
                while (retry && HippoServiceRegistry.getService(RepositoryService.class) == null) {
                    log.info("Waiting for the RepositoryService to become available before initializing the HST component manager.");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.info("Waiting for the RepositoryService got interrupted. Quiting");
                        retry = false;
                        Thread.currentThread().interrupt();
                    }
                }
                if (HippoServiceRegistry.getService(RepositoryService.class) != null) {
                    log.info("RepositoryService is available. Initializing the HST component manager now");
                    initializeComponentManager();
                }
            });
            // stop this init thread when the jvm exits without this init thread to finish, hence make is a daemon
            initThread.setDaemon(true);
            initThread.start();
        }
    }

    protected synchronized boolean isInitialized() {
        return initialized;
    }

    private synchronized void initializeComponentManager() {
        SpringComponentManager componentManager = null;
        ComponentManager oldComponentManager = HstServices.getComponentManager();

        if (oldComponentManager != null) {
            log.info("HstSiteConfigServlet will re-initialize the Component manager...");
            oldComponentManager.publishEvent(new ComponentManagerBeforeReplacedEvent(oldComponentManager));
        }

        try {
            log.info(INIT_START_MSG);

            log.info("HstSiteConfigServlet attempting to create the Component manager...");
            componentManager = new SpringComponentManager(getServletContext(), configuration);
            log.info("HSTSiteServlet attempting to start the Component Manager...");

            if (assemblyOverridesConfigurations != null && assemblyOverridesConfigurations.length > 0) {
                String [] configurations = componentManager.getConfigurationResources();
                configurations = (String []) ArrayUtils.addAll(configurations, assemblyOverridesConfigurations);
                componentManager.setConfigurationResources(configurations);
            }

            List<ModuleDefinition> addonModuleDefinitions = ModuleDescriptorUtils.collectAllModuleDefinitions();
            if (addonModuleDefinitions != null && !addonModuleDefinitions.isEmpty()) {
                componentManager.setAddonModuleDefinitions(addonModuleDefinitions);
            }

            componentManager.initialize();
            componentManager.start();
            log.info("HstSiteConfigServlet has successfuly started the Component Manager....");

            HstServices.setComponentManager(componentManager);

            if (oldComponentManager != null) {
                log.info("HstSiteConfigServlet attempting to stop the old component manager...");
                try {
                    oldComponentManager.stop();
                    oldComponentManager.close();
                } catch (Exception ce) {
                    log.warn("Old Component Manager stopping/closing error", ce);
                }
            }

            if (!HstServices.isHstConfigurationNodesLoaded() && !lazyHstConfigurationLoading) {
                log.info("Trigger HST Configuration nodes to be loaded");
                final long start = System.currentTimeMillis();
                final HstNodeLoadingCache hstNodeLoadingCache = componentManager.getComponent(HstNodeLoadingCache.class);
                // triggers the loading of all the hst configuration nodes
                HstNode root = null;
                while (root == null) {
                    try {
                        root = hstNodeLoadingCache.getNode(hstNodeLoadingCache.getRootPath());
                    } catch (ConfigurationNodesLoadingException e) {
                        if (log.isDebugEnabled()) {
                            log.info("Exception while trying to load the HST configuration nodes. Try again.", e);
                        } else {
                            log.info("Exception while trying to load the HST configuration nodes. Try again. Reason: {}", e.getMessage());
                        }
                    }
                }
                log.info("Loaded all HST Configuraion JCR nodes in {} ms.", (System.currentTimeMillis() - start));
            }
            log.info(INIT_DONE_MSG);
            this.initialized = true;

        } catch (Exception e) {
            log.error("HstSiteConfigServlet: ComponentManager initialization failed.", e);

            if (componentManager != null) {
                try { 
                    componentManager.stop();
                    componentManager.close();
                } catch (Exception ce) {
                    if (log.isDebugEnabled()) {
                        log.warn("Exception occurred during stopping componentManager.", e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Exception occurred during stopping componentManager. {}", e.toString());
                    }
                }
            }
        }
    }

    @Override
    public synchronized void destroy() {
        log.info("Shutting down!");
        if (initThread != null && initThread.isAlive()) {
            initThread.interrupt();
            try {
                initThread.join();
            } catch (InterruptedException e) {
                log.error("Interrupted while stopping initThread", e);
                initThread.interrupt();
            }
        }
        ServletContextRegistry.unregister(getServletContext());
        log.debug("Unregistered servlet context '{}' from {}",
                getServletContext().getContextPath(), ServletContextRegistry.class.getName());
        destroyHstSiteConfigurationChangesCheckerThread();

        ComponentManager componentManager = HstServices.getComponentManager();
        // componentManager can be null if HstSiteConfigServlet didn't finish the initialization yet.
        if (componentManager != null) {
            try {
                componentManager.stop();
                componentManager.close();
            } catch (Exception e) {
                log.warn("Component Manager stopping/closing error", e);
            } finally {
                HstServices.setComponentManager(null);
            }
        }
        log.info("Done Shutting down!");
    }

    /**
     * <p>The goal of this method is to load the configuration using parameters provided by servlet config parameters,
     * and/or system parameters. Some sane defaults are available as well.</p>
     * <p>You can use the commons configuration xml config, which by default should be in the WEB-INF folder. You can
     * also configure the location of this file in init-params of the servlet. Check the example</p>
     * <p>Another way to configure commons configuration is to use a properties file. Again the location by default is
     * in the WEB-INF folder, but you can change location and name of the file using init-params of the servlet</p>
     * <p/>
     *
     * <strong>Example - using xml configuration and a system parameter</strong>
     * <pre>-DConfig.dir=/usr/local/tomcat/config</pre><br/>
     * <pre>hst-configuration.xml</pre> (in the WEB-INF folder)
     * <pre>
     * &lt;configuration>
     *     &lt;properties fileName="${Config.dir}/site/hst-config.properties"/&gt;
     * &lt;/configuration>
     * </pre>
     *
     * <strong>Example - using servlet init param for hst.properties</strong>
     * <pre>web.xml</pre>
     * <pre>
     * &lt;init-param>
     *     &lt;param-name>hst-config-properties&lt;/param-name>
     * &lt;param-value>/usr/local/tomcat/config/site/hst-config.properties&lt;/param-value>
     * &lt;/init-param>
     * </pre>
     *
     * @return Configuration containing all the params found in the system, jndi and the config file found
     * @throws ContainerException thrown if file's cannot be found or configuration problems arise.
     */
    protected Configuration getConfiguration() throws ContainerException {
        try {
            Configuration [] configs = loadFileConfigurations();
            CompositeConfiguration configuration = new CompositeConfiguration();

            if (hstSystemPropertiesOverride) {
                configuration.addConfiguration(new SystemConfiguration());
                log.info("Adding System Properties to HST Configuration.");
            }

            for (Configuration config : configs) {
                configuration.addConfiguration(config);
            }

            Configuration defaultHstConf = loadDefaultHstConfiguration();

            if (defaultHstConf != null) {
                configuration.addConfiguration(defaultHstConf);
            }

            return configuration;
        } catch (Exception e) {
            throw new ContainerException(e);
        }
    }

    private Configuration loadDefaultHstConfiguration() {
        Configuration defaultHstConfiguration = null;

        try {
            URL defaultContainerPropsUrl = Thread.currentThread().getContextClassLoader().getResource(StringUtils.replace(SpringComponentManager.class.getName(), ".", "/") + ".properties");
            defaultHstConfiguration = new PropertiesConfiguration(defaultContainerPropsUrl);
        } catch (Exception e) {
            log.warn("Failed to load the default container properties.", e);
        }

        return defaultHstConfiguration;
    }

    private void setUpFileConfigurationReloadingStrategies() {
        boolean reloadingStrategySet = false;

        int configSize = ((CompositeConfiguration) configuration).getNumberOfConfigurations();

        for (int i = 0; i < configSize; i++) {
            Configuration config = ((CompositeConfiguration) configuration).getConfiguration(i);

            if (config instanceof AbstractFileConfiguration) {
                ((AbstractFileConfiguration) config).setReloadingStrategy(new FileChangedReloadingStrategy() {
                    @Override
                    public void reloadingPerformed() {
                        super.reloadingPerformed();
                        log.warn("HstSiteConfigServlet is trying to reinitialize component manager on configuration change in {}.", getFile());
                        try {
                            initializeComponentManager();
                            log.warn("HstSiteConfigServlet has completed reinitializing component manager on configuration change in {}.", getFile());
                        } catch (Exception e) {
                            log.error("HstSiteConfigServlet failed to reinitialize component manager on configuration change in " + getFile(), e);
                        }
                    }
                });

                reloadingStrategySet = true;
            }
        }

        if (reloadingStrategySet) {
            log.info("HstSiteConfigServlet enables component manager reloading on configuration change with refreshing delay = {} ms", configurationRefreshDelay);
            destroyHstSiteConfigurationChangesCheckerThread();
            hstSiteConfigurationChangesCheckerThread = new HstSiteConfigurationChangesChecker();
            hstSiteConfigurationChangesCheckerThread.start();
        }
    }

    /**
     * Loads all the file configurations.
     * <P>
     * Tries to load configuration defined in context descriptor first.
     * Afterward, falls back to {@link ServletConfig} level configuration if available. 
     * </P>
     * @return <code>Configuration</code> array
     * @throws ConfigurationException if configuration loading fails
     */
    protected Configuration [] loadFileConfigurations() throws ConfigurationException {
        List<Configuration> configs = new ArrayList<Configuration>();

        String fileParam = ServletConfigUtils.getInitParameter(null, getServletContext(), HST_CONFIGURATION_PARAM, null);

        if (StringUtils.isNotBlank(fileParam)) {
            Configuration config = loadConfigurationFromDefinitionXml(getResourceFile(fileParam, true));
            if (config != null) {
                log.info("Adding Configurarion file to HST Configuration: {}", fileParam);
                configs.add(config);
            }
        } else {
            fileParam = ServletConfigUtils.getInitParameter(null, getServletContext(), HST_CONFIG_PROPERTIES_PARAM, null);
            Configuration config = loadConfigurationFromProperties(getResourceFile(fileParam, true));
            if (config != null) {
                log.info("Adding Configurarion file to HST Configuration: {}", fileParam);
                configs.add(config);
            }
        }

        {
            Configuration config = loadConfigurationFromProperties(getResourceFile(hstConfigEnvProperties, true));
            if (config != null) {
                log.info("Adding Configurarion file to HST Configuration: {}", fileParam);
                configs.add(config);
            }
        }

        fileParam = "/WEB-INF/" + HST_CONFIGURATION_XML;

        Configuration config = loadConfigurationFromDefinitionXml(getResourceFile(fileParam, true));
        if (config != null) {
            log.info("Adding Configurarion file to HST Configuration: {}", fileParam);
            configs.add(config);
        } else {
            fileParam = "/WEB-INF/" + HST_CONFIG_PROPERTIES;
            config = loadConfigurationFromProperties(getResourceFile(fileParam, true));
            if (config != null) {
                log.info("Adding Configurarion file to HST Configuration: {}", fileParam);
                configs.add(config);
            }
        }
        return configs.toArray(new Configuration[configs.size()]);
    }

    protected String getConfigOrContextInitParameter(String paramName, String defaultValue) {
        String value = ServletConfigUtils.getInitParameter(null, getServletContext(), paramName, defaultValue);
        return (value != null ? value.trim() : null);
    }

    /**
     * Returns the physical resource file object.
     * <P>
     * <UL>
     * <LI>When the resourcePath starts with 'file:', it is assumed as an absolute file URI path.</LI>
     * <LI>When the resourcePath starts with a leading slash ('/'), it is assumed as a servlet context relative path.</LI>
     * <LI>When the resourcePath does not starts with 'file' nor with a leading slash ('/'), it is assumed as a relative path of the file system.</LI>  
     * </UL>
     * </P>
     * @param resourcePath
     * @return
     */
    protected File getResourceFile(String resourcePath) {
        return getResourceFile(resourcePath, false);
    }

    /**
     * Returns the physical resource file object with resolving system properties which are in the form "${var}"
     * if <code>resolveSysProps</code> is true.
     * <P>
     * <UL>
     * <LI>When the resourcePath starts with 'file:', it is assumed as an absolute file URI path.</LI>
     * <LI>When the resourcePath starts with a leading slash ('/'), it is assumed as a servlet context relative path.</LI>
     * <LI>When the resourcePath does not starts with 'file' nor with a leading slash ('/'), it is assumed as a relative path of the file system.</LI>  
     * </UL>
     * </P>
     * @param resourcePath
     * @param resolveSysProps
     * @return
     */
    protected File getResourceFile(String resourcePath, boolean resolveSysProps) {
        if (StringUtils.isBlank(resourcePath)) {
            return null;
        }

        if (resolveSysProps) {
            PropertyParser propParser = 
                    new PropertyParser(System.getProperties(),
                            PropertyParser.DEFAULT_PLACEHOLDER_PREFIX,
                            PropertyParser.DEFAULT_PLACEHOLDER_SUFFIX,
                            PropertyParser.DEFAULT_VALUE_SEPARATOR,
                            true);
            resourcePath = (String) propParser.resolveProperty("resourceFile", StringUtils.trim(resourcePath));
        }

        File resourceFile = null;

        if (resourcePath.startsWith("file:")) {
            try {
                resourceFile = new File(URI.create(resourcePath));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid URI for file: {}", resourcePath);
            } catch (Exception e) {
                log.warn("Failed to create a file from URI: {}. {}", resourcePath, e);
            }
        } else if (resourcePath.startsWith("/")) {
            String realPath = null;

            try {
                URL resourceUrl = getServletContext().getResource(resourcePath);

                // if resourcePath is found in the web resources, then try to get the real path from the context relative url
                // otherwise, use the resource path as absolute path.

                if (resourceUrl != null) {
                    realPath = getServletContext().getRealPath(resourcePath);
                } else {
                    realPath = resourcePath;
                }
            } catch (Exception re) {
            }

            if (realPath != null) {
                resourceFile = new File(realPath);
            }
        } else {
            resourceFile = new File(resourcePath);
        }

        return resourceFile;
    }

    private Configuration loadConfigurationFromDefinitionXml(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }

        try {
            DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
            builder.setFile(file);
            return builder.getConfiguration();
        } catch (ConfigurationException e) {
            log.warn("Configuration error from: " + file, e);
        }

        return null;
    }

    private Configuration loadConfigurationFromProperties(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }

        try {
            return new PropertiesConfiguration(file);
        } catch (ConfigurationException e) {
            log.warn("Configuration error from: " + file, e);
        }

        return null;
    }

    private void destroyHstSiteConfigurationChangesCheckerThread() {
        try {
            if (hstSiteConfigurationChangesCheckerThread != null) {
                if (hstSiteConfigurationChangesCheckerThread.isAlive()) {
                    hstSiteConfigurationChangesCheckerThread.setStopped(true);
                    hstSiteConfigurationChangesCheckerThread.interrupt();
                    hstSiteConfigurationChangesCheckerThread.join(10000L);
                }
            }
        } catch (Exception e) {
            log.warn("HstSiteConfigurationChangesCheckerThread interruption error", e);
        } finally {
            hstSiteConfigurationChangesCheckerThread = null;
        }
    }

    private class HstSiteConfigurationChangesChecker extends Thread {

        private boolean stopped;

        private HstSiteConfigurationChangesChecker() {
            super("HstSiteConfigurationChangesChecker");
            setDaemon(true);
        }

        public void setStopped(boolean stopped) {
            this.stopped = stopped;
        }

        public void run() {
            while (!this.stopped) {

                // NOTE: when trying to read configuration, commons-configuration will
                //       check if the configuration has been modified
                //       per each refresh delay
                configuration.getString("development.mode");

                synchronized (this) {
                    try {
                        wait(configurationRefreshDelay);
                    } catch (InterruptedException e) {
                        if (this.stopped) {
                            break;
                        }
                    }
                }
            }
        }
    }

}
