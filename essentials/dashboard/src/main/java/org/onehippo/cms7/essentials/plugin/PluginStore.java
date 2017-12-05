/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ext.RuntimeDelegate;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.onehippo.cms7.essentials.WebUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
import org.onehippo.cms7.essentials.dashboard.event.RebuildEvent;
import org.onehippo.cms7.essentials.dashboard.event.listeners.RebuildProjectEventListener;
import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptor;
import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptorRestful;
import org.onehippo.cms7.essentials.dashboard.model.ProjectSettings;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.inject.ApplicationModule;
import org.onehippo.cms7.essentials.filters.EssentialsContextListener;
import org.onehippo.cms7.essentials.rest.client.RestClient;
import org.onehippo.cms7.essentials.rest.model.SystemInfo;
import org.onehippo.cms7.essentials.servlet.DynamicRestPointsApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.ContextLoader;

/**
 * Created by tjeger on 10/11/14.
 */
@Component
@Singleton
public class PluginStore {

    private static Logger log = LoggerFactory.getLogger(PluginStore.class);
    private DynamicRestPointsApplication application = new DynamicRestPointsApplication();

    @Inject private RebuildProjectEventListener rebuildListener;
    @Inject private PluginContextFactory contextFactory;

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
    private final LoadingCache<String, RestfulList<PluginDescriptorRestful>> pluginCache = CacheBuilder.newBuilder()
            .expireAfterAccess(60, TimeUnit.MINUTES)
            .recordStats()
            .build(new CacheLoader<String, RestfulList<PluginDescriptorRestful>>() {
                @Override
                public RestfulList<PluginDescriptorRestful> load(final String url) throws Exception {
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
                            log.error(MessageFormat.format("Error loading plugins from repository '{0}'", url), e);
                        }
                    }

                    return parsePlugins(pluginJson);
                }
            });


    public List<Plugin> getAllPlugins() {
        final List<Plugin> plugins = new ArrayList<>();
        final PluginContext context = contextFactory.getContext();

        // Read local descriptors
        final List<PluginDescriptorRestful> localDescriptors = getLocalDescriptors();
        for (PluginDescriptorRestful descriptor : localDescriptors) {
            plugins.add(new Plugin(context, descriptor));
        }

        // Read remote descriptors

        final Set<String> pluginRepositories = getProjectSettings().getPluginRepositories();
        for (String pluginRepository : pluginRepositories) {
            try {
                final RestfulList<PluginDescriptorRestful> descriptors = pluginCache.get(pluginRepository);
                log.debug("{}", pluginCache.stats());
                if (descriptors != null) {
                    for (PluginDescriptorRestful descriptor : descriptors.getItems()) {
                        plugins.add(new Plugin(context, descriptor));
                    }
                }
            } catch (Exception e) {
                log.error(MessageFormat.format("Error loading plugins from repository '{0}'", pluginRepository), e);
            }
        }

        processPlugins(plugins);
        return plugins;
    }

    public Plugin getPluginById(final String id) {
        if (Strings.isNullOrEmpty(id)) {
            return null;
        }

        for (final Plugin plugin : getAllPlugins()) {
            if (id.equals(plugin.getId())) {
                return plugin;
            }
        }
        return null;
    }

    public int countInstalledPlugins() {
        int installedPlugins = 0;

        for (Plugin plugin : getAllPlugins()) {
            if (plugin.getInstallState() != InstallState.DISCOVERED) {
                installedPlugins++;
            }
        }

        return installedPlugins;
    }

    public void clearCache() {
        pluginCache.invalidateAll();
    }

    public void populateSystemInfo(final SystemInfo systemInfo) {
        final List<Plugin> plugins = getAllPlugins();
        for (Plugin plugin : plugins) {
            systemInfo.incrementPlugins();
            final InstallState installState = plugin.getInstallState();
            final String pluginType = plugin.getDescriptor().getType();
            final boolean isTool = "tool".equals(pluginType);
            if (isTool) {
                systemInfo.incrementTools();
            }
            final boolean isFeature = "feature".equals(pluginType);
            if (isFeature && installState != InstallState.DISCOVERED) {
                systemInfo.incrementInstalledFeatures();
            }
            if (!isTool) {
                if (installState == InstallState.BOARDING || installState == InstallState.INSTALLING) {
                    systemInfo.addRebuildPlugin(plugin.getDescriptor());
                    systemInfo.setNeedsRebuild(true);
                } else if (installState == InstallState.ONBOARD) {
                    systemInfo.incrementConfigurablePlugins();
                }
            }
        }

        // check if we have external rebuild events:
        final List<RebuildEvent> rebuildEvents = rebuildListener.pollEvents();
        for (RebuildEvent rebuildEvent : rebuildEvents) {
            final String pluginId = rebuildEvent.getPluginId();
            for (Plugin plugin : plugins) {
                if (plugin.getId().equals(pluginId)) {
                    systemInfo.setNeedsRebuild(true);
                    systemInfo.addRebuildPlugin(plugin.getDescriptor());
                    break;
                }
            }
        }
    }

    // TODO: this is not a *plugin* utility! Move to better place.
    public ProjectSettings getProjectSettings() {
        return contextFactory.getContext().getProjectSettings();
    }

    private List<PluginDescriptorRestful> getLocalDescriptors() {
        final List<PluginDescriptorRestful> descriptors = new ArrayList<>();
        final Collection<PluginDescriptorRestful> values = EssentialsContextListener.getPluginCache().asMap().values();
        descriptors.addAll(values);
        descriptors.addAll(loadPluginDescriptorsFromResource("/project_plugin_descriptor.json"));

        return descriptors;
    }

    private List<PluginDescriptorRestful> loadPluginDescriptorsFromResource(final String resource) {
        final InputStream stream = PluginStore.class.getResourceAsStream(resource);
        final String json = GlobalUtils.readStreamAsText(stream);

        return parsePlugins(json).getItems();
    }

    @SuppressWarnings("unchecked")
    private RestfulList<PluginDescriptorRestful> parsePlugins(final String jsonString) {
        if (!Strings.isNullOrEmpty(jsonString)) {
            try {
                return WebUtils.fromJson(jsonString, RestfulList.class);
            } catch (Exception e) {
                log.error("Error parsing plugins", e);
            }
        }
        return new RestfulList<>();
    }

    private static final Semaphore serverSemaphore = new Semaphore(1);

    private void processPlugins(final List<Plugin> plugins) {
        plugins.forEach(p -> registerEndPoints(p.getDescriptor()));

        // Make sure we only attempt starting the server once!
        if (application.getClasses().size() > 0 && serverSemaphore.drainPermits() > 0) {
            startServer();
        }
    }

    private void registerEndPoints(final PluginDescriptor descriptor) {
        final List<String> pluginRestClasses = descriptor.getRestClasses();
        if (pluginRestClasses != null) {
            for (String restClassName : pluginRestClasses) {
                final Class<?> endpointClass = GlobalUtils.loadCLass(restClassName);
                if (endpointClass == null) {
                    log.error("Failed to load REST endpoint class: {}", restClassName);
                    continue;
                }
                final Set<Class<?>> classes = application.getClasses();
                if (classes.contains(endpointClass)) {
                    log.debug("REST endpoint class already loaded {}", restClassName);
                    continue;
                }
                application.addClass(endpointClass);
                log.info("Adding dynamic REST (plugin) endpoint {}", endpointClass.getName());
            }
        }
    }

    private void startServer() {
        ApplicationModule.getInjector().autowireBean(application);

        final ApplicationContext applicationContext = ContextLoader.getCurrentWebApplicationContext();
        final RuntimeDelegate delegate = RuntimeDelegate.getInstance();
        final JAXRSServerFactoryBean factoryBean = delegate.createEndpoint(application, JAXRSServerFactoryBean.class);

        factoryBean.setProvider(applicationContext.getBean("jsonProvider"));
        factoryBean.setBus(BusFactory.getDefaultBus());

        final Server server = factoryBean.create();
        server.start();
    }
}
