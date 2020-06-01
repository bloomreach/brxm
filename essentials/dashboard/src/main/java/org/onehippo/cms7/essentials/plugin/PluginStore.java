/*
 * Copyright 2014-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.onehippo.cms7.essentials.WebUtils;
import org.onehippo.cms7.essentials.dashboard.install.InstallService;
import org.onehippo.cms7.essentials.filters.EssentialsContextListener;
import org.onehippo.cms7.essentials.plugin.sdk.packaging.DefaultInstructionPackage;
import org.onehippo.cms7.essentials.plugin.sdk.rest.PluginDescriptorList;
import org.onehippo.cms7.essentials.plugin.sdk.services.SettingsServiceImpl;
import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;
import org.onehippo.cms7.essentials.rest.client.RestClient;
import org.onehippo.cms7.essentials.sdk.api.model.rest.InstallState;
import org.onehippo.cms7.essentials.sdk.api.model.rest.PluginDescriptor;
import org.onehippo.cms7.essentials.servlet.DynamicRestPointsApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.ContextLoader;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Created by tjeger on 10/11/14.
 */
@Component
public class PluginStore {

    private static Logger log = LoggerFactory.getLogger(PluginStore.class);

    private final DynamicRestPointsApplication application;
    private final SettingsServiceImpl settingsService;
    private final InstallService installService;
    private final AutowireCapableBeanFactory injector;

    /**
     * Plugin cache to avoid remote calls, loads from following protocols:
     * <p/>
     * <p>Remote url "http(s):"</p>
     * Below are ones supported by  {@code ResourceUtils}
     * <p>CLASSPATH_URL_PREFIX  Pseudo URL prefix for loading from the class path: "classpath:"</p>
     * <p>FILE_URL_PREFIX  URL prefix for loading from the file system: "file:"</p>
     * <p>JAR_URL_SEPARATOR  Separator between JAR URL and file path within the JAR</p>
     * <p>URL_PROTOCOL_CODE_SOURCE  URL protocol for an entry from an OC4J jar file: "code-source"</p>
     * <p>URL_PROTOCOL_FILE  URL protocol for a file in the file system: "file"</p>
     * <p>URL_PROTOCOL_JAR  URL protocol for an entry from a jar file: "jar"</p>
     * <p>URL_PROTOCOL_VFSZIP  URL protocol for an entry from a JBoss jar file: "vfszip"</p>
     * <p>URL_PROTOCOL_WSJAR   URL protocol for an entry from a WebSphere jar file: "wsjar"</p>
     * <p>URL_PROTOCOL_ZIP  URL protocol for an entry from a zip file: "zip"</p>
     *
     * @see org.springframework.util.ResourceUtils
     */
    private final LoadingCache<String, List<PluginDescriptor>> pluginCache = CacheBuilder.newBuilder()
            .expireAfterAccess(60, TimeUnit.MINUTES)
            .recordStats()
            .build(new CacheLoader<String, List<PluginDescriptor>>() {
                @Override
                public List<PluginDescriptor> load(final String url) {
                    String pluginJson = null;

                    if (url.startsWith("http")) {
                        RestClient client = new RestClient(url);
                        pluginJson = client.getJson();
                    } else {
                        final URI resourceUri = URI.create(url);
                        try {
                            final File file = ResourceUtils.getFile(resourceUri);
                            pluginJson = GlobalUtils.readStreamAsText(new FileInputStream(file));
                        } catch (Exception e) {
                            log.error("Error loading plugins from repository '{}'", url, e);
                        }
                    }

                    return parsePlugins(pluginJson);
                }
            });

    @Inject
    public PluginStore(final DynamicRestPointsApplication application, final SettingsServiceImpl settingsService,
                       final InstallService installService, final AutowireCapableBeanFactory injector) {
        this.application = application;
        this.settingsService = settingsService;
        this.installService = installService;
        this.injector = injector;
    }


    public synchronized PluginSet loadPlugins() {
        final PluginSet pluginSet = new PluginSet();

        // Read local (packaged) descriptors
        getLocalDescriptors().forEach(pluginSet::add);

        // Read remote descriptors
        final Set<String> pluginRepositories = settingsService.getModifiableSettings().getPluginRepositories();
        for (String pluginRepository : pluginRepositories) {
            try {
                final List<PluginDescriptor> descriptors = pluginCache.get(pluginRepository);
                log.debug("{}", pluginCache.stats());
                if (descriptors != null) {
                    descriptors.forEach(pluginSet::add);
                }
            } catch (Exception e) {
                log.error("Error loading plugins from repository '{}'", pluginRepository, e);
            }
        }

        processPlugins(pluginSet);
        return pluginSet;
    }

    public void clearCache() {
        pluginCache.invalidateAll();
    }

    private List<PluginDescriptor> getLocalDescriptors() {
        final List<PluginDescriptor> descriptors = new ArrayList<>();
        final Collection<PluginDescriptor> values = EssentialsContextListener.getPluginCache().asMap().values();
        descriptors.addAll(values);
        descriptors.addAll(loadPluginDescriptorsFromResource());

        return descriptors;
    }

    private List<PluginDescriptor> loadPluginDescriptorsFromResource() {
        final InputStream stream = PluginStore.class.getResourceAsStream("/project_plugin_descriptor.json");
        final String json = GlobalUtils.readStreamAsText(stream);

        return parsePlugins(json);
    }

    private List<PluginDescriptor> parsePlugins(final String jsonString) {
        if (!Strings.isNullOrEmpty(jsonString)) {
            try {
                final PluginDescriptorList pluginDescriptorList
                        = WebUtils.fromJson(jsonString, PluginDescriptorList.class);
                if (pluginDescriptorList != null) {
                    return pluginDescriptorList.getItems();
                }
            } catch (Exception e) {
                log.error("Error parsing plugins", e);
            }
        }
        return Collections.emptyList();
    }

    private boolean serverStarted;

    private void processPlugins(final PluginSet pluginSet) {
        final Set<String> restClasses = new HashSet<>();

        for (PluginDescriptor plugin : pluginSet.getPlugins()) {
            if (plugin.getType().equals("feature")) {
                // load the plugin's current installation state
                installService.loadInstallStateFromFileSystem(plugin);

                plugin.setDependencySummary(makeDependencySummary(plugin, pluginSet));
            } else {
                // 'tools' are always considered installed.
                plugin.setState(InstallState.INSTALLED);
            }

            // extract all REST classes to setup the dynamic endpoints
            if (plugin.getRestClasses() != null) {
                restClasses.addAll(plugin.getRestClasses());
            }

            // set flag if plugin declares changes for default parameters
            final DefaultInstructionPackage instructionPackage = installService.makeInstructionPackageInstance(plugin);
            if (instructionPackage != null) {
                final Map<String, Object> parameters = installService.ensureGenericInstallationParameters(new HashMap<>());
                if (!instructionPackage.getInstructionsMessages(parameters).isEmpty()) {
                    plugin.setHasMessages(true);
                }
            }
        }

        restClasses.forEach(application::addSingleton);

        if (!restClasses.isEmpty() && !serverStarted) {
            injector.autowireBean(application);

            final ApplicationContext applicationContext = ContextLoader.getCurrentWebApplicationContext();
            final RuntimeDelegate delegate = RuntimeDelegate.getInstance();
            final JAXRSServerFactoryBean factoryBean = delegate.createEndpoint(application, JAXRSServerFactoryBean.class);

            factoryBean.setProvider(applicationContext.getBean("jsonProvider"));
            factoryBean.setBus(BusFactory.getDefaultBus());

            final Server server = factoryBean.create();
            server.start();
            serverStarted = true;
        }
    }

    private String makeDependencySummary(final PluginDescriptor plugin, final PluginSet pluginSet) {
        final List<PluginDescriptor.Dependency> dependencies = plugin.getPluginDependencies();
        if (dependencies == null) {
            return null;
        }
        
        final List<String> dependentPluginIds = new ArrayList<>();
        for (PluginDescriptor.Dependency dependency : dependencies) {
            final String pluginId = dependency.getPluginId();
            if (StringUtils.isNotBlank(pluginId)) {
                final PluginDescriptor pluginDescriptor = pluginSet.getPlugin(pluginId);
                if (pluginDescriptor != null) {
                    dependentPluginIds.add(pluginDescriptor.getName());
                } else {
                    dependentPluginIds.add(pluginId + " (missing)");
                }
            }
        }

        if (!dependentPluginIds.isEmpty()) {
            final String csv = String.join(", ", dependentPluginIds);
            return String.format("Depends on feature%s: %s.", dependentPluginIds.size() > 1 ? "s" : "", csv);
        }

        return null;
    }
}
