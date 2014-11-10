/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.commons.collections.CollectionUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;
import org.codehaus.jackson.map.ObjectMapper;
import org.onehippo.cms7.essentials.dashboard.config.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.config.PluginInstallationState;
import org.onehippo.cms7.essentials.dashboard.config.PluginParameterService;
import org.onehippo.cms7.essentials.dashboard.config.PluginParameterServiceFactory;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.ctx.DefaultPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.event.RebuildEvent;
import org.onehippo.cms7.essentials.dashboard.event.listeners.MemoryPluginEventListener;
import org.onehippo.cms7.essentials.dashboard.event.listeners.RebuildProjectEventListener;
import org.onehippo.cms7.essentials.dashboard.model.EssentialsDependency;
import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptor;
import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptorRestful;
import org.onehippo.cms7.essentials.dashboard.model.ProjectSettings;
import org.onehippo.cms7.essentials.dashboard.model.Repository;
import org.onehippo.cms7.essentials.dashboard.packaging.CommonsInstructionPackage;
import org.onehippo.cms7.essentials.dashboard.packaging.InstructionPackage;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.ErrorMessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PluginModuleRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.setup.ProjectSetupPlugin;
import org.onehippo.cms7.essentials.dashboard.utils.DependencyUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.HstUtils;
import org.onehippo.cms7.essentials.rest.client.RestClient;
import org.onehippo.cms7.essentials.rest.model.ControllerRestful;
import org.onehippo.cms7.essentials.rest.model.RestList;
import org.onehippo.cms7.essentials.rest.model.StatusRestful;
import org.onehippo.cms7.essentials.rest.model.SystemInfo;
import org.onehippo.cms7.essentials.rest.plugin.InstallStateMachine;
import org.onehippo.cms7.essentials.servlet.DynamicRestPointsApplication;
import org.onehippo.cms7.essentials.utils.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.ContextLoader;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;


/**
 * @version "$Id$"
 */

@CrossOriginResourceSharing(allowAllOrigins = true)
@Api(value = "/plugins", description = "Rest resource which provides information about plugins: e.g. installed or available plugins")
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/plugins")
public class PluginResource extends BaseResource {


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
     * @see ResourceUtils
     */
    private final LoadingCache<String, RestfulList<PluginDescriptorRestful>> pluginCache = CacheBuilder.newBuilder()
            .expireAfterAccess(60, TimeUnit.MINUTES)
            .recordStats()
            .build(new CacheLoader<String, RestfulList<PluginDescriptorRestful>>() {
                @Override
                public RestfulList<PluginDescriptorRestful> load(final String url) throws Exception {
                    if (url.startsWith("http")) {
                        RestClient client = new RestClient(url);
                        return client.getPlugins();
                    } else {
                        final URI resourceUri = URI.create(url);
                        try {
                            final File file = ResourceUtils.getFile(resourceUri);
                            final String pluginDescriptor = GlobalUtils.readStreamAsText(new FileInputStream(file));
                            return RestUtils.parsePlugins(pluginDescriptor);
                        } catch (Exception e) {
                            log.error(MessageFormat.format("Error loading plugins from repository: {0}", url), e);
                        }
                        return new RestfulList<>();
                    }

                }


            });

    public static final String PLUGIN_ID = "pluginId";
    @Inject
    private EventBus eventBus;

    private DynamicRestPointsApplication application;

    @Inject
    private MemoryPluginEventListener listener;

    @Inject
    private RebuildProjectEventListener rebuildListener;


    private boolean initialized;
    private static Logger log = LoggerFactory.getLogger(PluginResource.class);
    private static final Lock pingLock = new ReentrantLock();

    @SuppressWarnings("unchecked")
    @ApiOperation(
            value = "Fetches local and remote file descriptors  and checks for available Hippo Essentials plugins. " +
                    "It also registers any plugin REST endpoints which come available under /dynamic endpoint e.g. /dynamic/{pluginEndpoint}",
            notes = "Retrieves a list of PluginRestful objects",
            response = RestfulList.class)
    @GET
    @Path("/")
    public RestfulList<PluginDescriptorRestful> fetchPlugins(@Context ServletContext servletContext) {
        return getAllPlugins(servletContext);
    }


    @ApiOperation(
            value = "Check for each plugin if its setup phase can be triggered.",
            response = MessageRestful.class
    )
    @POST
    @Path("/autosetup")
    public MessageRestful autoSetupPlugins(@Context ServletContext servletContext) {
        for (PluginDescriptor plugin : getPlugins(servletContext)) {
            InstallStateMachine.promote(plugin);
            autoSetupIfPossible(plugin);
        }

        return null;
    }


    @ApiOperation(
            value = "Ping, returns true if application is initialized",
            response = boolean.class)
    @GET
    @Path("/ping")
    public SystemInfo ping(@Context ServletContext servletContext) {
        final SystemInfo systemInfo = new SystemInfo();

        // We lock the ping to avoid concurrent setup short-circuiting.
        if (!pingLock.tryLock()) {
            log.warn("WARNING: You appear to be using two dashboards at the same time. Essentials doesn't support that." +
                    " Check if you have multiple tabs open, pointing at Essentials, and if so, close all except for one.");
            pingLock.lock();
        }
        try {
            systemInfo.setInitialized(initialized);
            final List<PluginDescriptorRestful> plugins = getPlugins(servletContext);
            for (PluginDescriptorRestful plugin : plugins) {
                systemInfo.incrementPlugins();
                final String installState = plugin.getInstallState();
                final boolean isTool = "tool".equals(plugin.getType());
                if (isTool) {
                    systemInfo.incrementTools();
                }
                final boolean isFeature = "feature".equals(plugin.getType());
                if (isFeature && !PluginInstallationState.DISCOVERED.equals(installState)) {
                    systemInfo.incrementInstalledFeatures();
                }
                if (!isTool && !Strings.isNullOrEmpty(installState)) {
                    if (installState.equals(PluginInstallationState.BOARDING)
                            || installState.equals(PluginInstallationState.INSTALLING)) {
                        systemInfo.addRebuildPlugin(plugin);
                        systemInfo.setNeedsRebuild(true);
                    } else if (installState.equals(PluginInstallationState.ONBOARD)) {
                        systemInfo.incrementConfigurablePlugins();
                    }
                }
            }
            // check if we have external rebuild events:
            final List<RebuildEvent> rebuildEvents = rebuildListener.pollEvents();
            for (RebuildEvent rebuildEvent : rebuildEvents) {
                systemInfo.setNeedsRebuild(true);
                final List<PluginDescriptor> rebuildPlugins = systemInfo.getRebuildPlugins();
                // skip duplicate names:
                boolean containsPlugin = false;
                final String pluginName = rebuildEvent.getPluginName();
                for (PluginDescriptor rebuildPlugin : rebuildPlugins) {
                    if (rebuildPlugin.getName().equals(pluginName)) {
                        containsPlugin = true;
                    }
                }
                if (!containsPlugin) {
                    final PluginDescriptor pluginDescriptor = new PluginDescriptorRestful(pluginName);
                    pluginDescriptor.setType(rebuildEvent.getPluginType());
                    systemInfo.addRebuildPlugin(pluginDescriptor);
                }
            }
        } finally {
            pingLock.unlock();
        }

        return systemInfo;
    }


    @ApiOperation(
            value = "Clears plugin cache",
            notes = "Remote Plugin descriptors are cached for 1 hour. This method clears plugin cache and plugins are fetched again on next requests",
            response = MessageRestful.class)
    @GET
    @Path("/clearcache")
    public MessageRestful clearCache(@Context ServletContext servletContext) throws Exception {
        pluginCache.invalidateAll();
        return new MessageRestful("Plugin Cache invalidated");
    }


    @ApiOperation(
            value = "Installs selected instruction package",
            notes = "Use PostPayloadRestful and set InstructionPackage id property (pluginId)",
            response = MessageRestful.class)
    @POST
    @Path("/install/package")
    public MessageRestful installInstructionPackage(final PostPayloadRestful payloadRestful, @Context ServletContext servletContext) {

        final Map<String, String> values = payloadRestful.getValues();
        final String pluginId = String.valueOf(values.get(PLUGIN_ID));
        final PluginDescriptorRestful myPlugin = getPluginById(pluginId, servletContext);

        if (Strings.isNullOrEmpty(pluginId) || myPlugin == null) {
            final MessageRestful resource = new MessageRestful("No valid InstructionPackage was selected");
            resource.setSuccessMessage(false);
            return resource;
        }
        final Map<String, Object> properties = new HashMap<String, Object>(values);

        final ErrorMessageRestful errorMessage = setupPlugin(myPlugin, properties);

        return errorMessage == null
                ? new MessageRestful("Successfully installed " + myPlugin.getName(), DisplayEvent.DisplayType.STRONG)
                : errorMessage;
    }

    @ApiOperation(value = "Signal to the dashboard that the plugin's setup phase has completed.")
    @ApiParam(name = PLUGIN_ID, value = "Plugin id", required = true)
    @POST
    @Path("/setup/{pluginId}")
    public void signalSetup(@PathParam(PLUGIN_ID) String pluginId, @Context ServletContext servletContext) {
        final PluginDescriptorRestful plugin = getPluginById(pluginId, servletContext);

        updateInstallStateAfterSetup(plugin);
    }

    @ApiOperation(
            value = "Saves global project settings",
            response = KeyValueRestful.class)
    @POST
    @Path("/savesettings")
    public KeyValueRestful hideWelcomeScreen(final ProjectSettingsBean payload, @Context ServletContext servletContext) {
        try {
            final PluginDescriptor plugin = getPluginById(ProjectSetupPlugin.class.getName(), servletContext);
            final PluginContext context = new DefaultPluginContext(plugin);
            try (PluginConfigService configService = context.getConfigService()) {
                final Set<String> pluginRepositories = payload.getPluginRepositories();
                if (pluginRepositories != null) {
                    final Iterator<String> iterator = pluginRepositories.iterator();
                    for (; iterator.hasNext(); ) {
                        final String next = iterator.next();
                        if (Strings.isNullOrEmpty(next)) {
                            iterator.remove();
                        }
                    }
                }
                payload.setSetupDone(true);
                configService.write(payload);
                return new KeyValueRestful("message", "Saved property for welcome screen");
            }
        } catch (Exception e) {

            log.error("Error checking InstructionPackage status", e);
        }

        return new KeyValueRestful("message", "Error saving welcome screen setting");
    }

    @ApiOperation(
            value = "Returns plugin descriptor file",
            notes = "Used for plugin layout etc.",
            response = PluginDescriptorRestful.class)
    @ApiParam(name = PLUGIN_ID, value = "Plugin id", required = true)
    @GET
    @Path("/plugins/{pluginId}")
    public PluginDescriptor getPlugin(@Context ServletContext servletContext, @PathParam(PLUGIN_ID) String pluginId) {
        final List<PluginDescriptorRestful> pluginList = getPlugins(servletContext);
        for (PluginDescriptor plugin : pluginList) {
            if (plugin.getId().equals(pluginId)) {
                return plugin;
            }
        }
        return new PluginDescriptorRestful();
    }

    @ApiOperation(
            value = "Installs a plugin",
            response = MessageRestful.class)
    @ApiParam(name = PLUGIN_ID, value = "Plugin  id", required = true)
    @POST
    @Path("/install/{pluginId}")
    public MessageRestful installPlugin(@Context ServletContext servletContext, @PathParam(PLUGIN_ID) String pluginId) throws Exception {
        final MessageRestful message = new MessageRestful();

        final PluginDescriptor plugin = getPluginById(pluginId, servletContext);
        if (plugin == null) {
            message.setValue("Plugin was not found and could not be installed");
            message.setSuccessMessage(false);
            return message;
        }

        String error = installRepositoriesForPlugin(plugin);
        if (error.length() > 0) {
            message.setValue(error);
            message.setSuccessMessage(false);
            return message;
        }

        error = installDependenciesForPlugin(plugin);
        if (error.length() > 0) {
            message.setValue(error);
            message.setSuccessMessage(false);
            return message;
        }

        InstallStateMachine.install(plugin);
        ErrorMessageRestful errorMessage = autoSetupIfPossible(plugin);
        if (errorMessage != null) {
            return errorMessage;
        }

        message.setValue("Plugin <strong>" + plugin.getName() + "</strong> successfully installed.");
        return message;
    }

    @ApiOperation(
            value = "Returns list of project settings like project namespace, project path etc. ",
            notes = "Contains a list of KeyValueRestful objects",
            response = RestfulList.class)
    @GET
    @Path("/settings")
    public RestfulList<KeyValueRestful> getKeyValue(@Context ServletContext servletContext) {
        final PluginContext context = PluginContextFactory.getContext();
        final Map<String, Object> placeholderData = context.getPlaceholderData();
        final RestfulList<KeyValueRestful> list = new RestList<>();
        for (Map.Entry<String, Object> entry : placeholderData.entrySet()) {
            final Object value = entry.getValue();
            if (value instanceof String) {
                final KeyValueRestful keyValueRestful = new KeyValueRestful(entry.getKey(), (String) value);
                list.add(keyValueRestful);
            }
        }
        return list;

    }

    @ApiOperation(
            value = "returns project settings",
            notes = "Contains a list of all predefined project settings and project setup preferences",
            response = ProjectSettings.class)
    @GET
    @Path("/projectsettings")
    public ProjectSettings getProjectSettings(@Context ServletContext servletContext) {
        final PluginContext context = PluginContextFactory.getContext();
        return context.getProjectSettings();
    }

    @GET
    @Path("/controllers")
    public RestfulList<ControllerRestful> getControllers(@Context ServletContext servletContext) throws Exception {

        final RestfulList<ControllerRestful> controllers = new RestList<>();
        final List<PluginDescriptorRestful> plugins = getPlugins(servletContext);
        for (PluginDescriptor plugin : plugins) {
            final String pluginLink = plugin.getId();
            if (Strings.isNullOrEmpty(pluginLink)) {
                continue;
            }
            controllers.add(new ControllerRestful(pluginLink, String.format("%sCtrl", pluginLink), String.format("plugins/%s/index.html", pluginLink)));

        }
        // TODO load from remote

        return controllers;

    }


    @ApiOperation(
            value = "Populated StatusRestful object",
            notes = "Status contains true value if one of the InstructionPackage is installed",
            response = StatusRestful.class)
    @GET
    @Path("/status/package")
    public StatusRestful getMenu(@Context ServletContext servletContext) {
        final StatusRestful status = new StatusRestful();
        try {
            final PluginDescriptor plugin = getPluginById(ProjectSetupPlugin.class.getName(), servletContext);
            final PluginContext context = new DefaultPluginContext(plugin);
            try (PluginConfigService configService = context.getConfigService()) {
                final ProjectSettingsBean document = configService.read(ProjectSettingsBean.DEFAULT_NAME, ProjectSettingsBean.class);
                if (document != null && document.getSetupDone()) {
                    status.setStatus(true);
                    return status;
                }
            }


        } catch (Exception e) {

            log.error("Error checking InstructionPackage status", e);
        }
        return status;
    }

    @ApiOperation(
            value = "Returns list of plugin Javascript modules",
            notes = "Modules are prefixed with tool, plugin or InstructionPackage dependent on their plugin type",
            response = PluginModuleRestful.class)
    @GET
    @Path("/modules")
    public PluginModuleRestful getModule(@Context ServletContext servletContext) throws Exception {
        final PluginModuleRestful modules = new PluginModuleRestful();
        final List<PluginDescriptorRestful> plugins = getPlugins(servletContext);
        for (PluginDescriptorRestful plugin : plugins) {
            final List<PluginModuleRestful.PrefixedLibrary> libraries = plugin.getLibraries();

            final String prefix = plugin.getType();
            final String pluginId = plugin.getId();
            if (libraries != null) {
                for (PluginModuleRestful.PrefixedLibrary library : libraries) {
                    // prefix libraries by plugin id:
                    library.setPrefix(prefix);
                    modules.addLibrary(pluginId, library);
                }
            }
        }
        return modules;
    }


    @ApiOperation(
            value = "Returns a list of messages about the changes plugin would made for specific choice",
            notes = "Messages are only indication what might change, because a lot of operations are not executed, e.g. file copy if is not executed" +
                    "if file already exists.",
            response = PluginModuleRestful.class)
    @POST
    @Path("/changes/")
    public RestfulList<MessageRestful> getInstructionPackageChanges(final PostPayloadRestful payload, @Context ServletContext servletContext) throws Exception {
        final Map<String, String> values = payload.getValues();
        final PluginContext context = PluginContextFactory.getContext();
        context.addPlaceholderData(new HashMap<String, Object>(values));

        final String pluginId = values.get(PLUGIN_ID);
        final PluginDescriptor myPlugin = getPluginById(pluginId, servletContext);

        final RestfulList<MessageRestful> list = new RestfulList<>();
        if (Strings.isNullOrEmpty(pluginId) || myPlugin == null) {
            final MessageRestful resource = new MessageRestful("No valid InstructionPackage was selected");
            resource.setSuccessMessage(false);
            list.add(resource);
            return list;
        }
        final InstructionPackage instructionPackage = instructionPackageInstance(myPlugin);
        if (instructionPackage == null) {
            final MessageRestful resource = new MessageRestful("Could not create Instruction Package");
            resource.setSuccessMessage(false);
            list.add(resource);
            return list;
        }

        instructionPackage.setProperties(new HashMap<String, Object>(values));

        @SuppressWarnings("unchecked")
        final Multimap<MessageGroup, MessageRestful> messages = (Multimap<MessageGroup, MessageRestful>) instructionPackage.getInstructionsMessages(context);
        final Collection<Map.Entry<MessageGroup, MessageRestful>> entries = messages.entries();
        for (Map.Entry<MessageGroup, MessageRestful> entry : entries) {
            final MessageRestful value = entry.getValue();
            value.setGroup(entry.getKey());
            value.setGlobalMessage(false);
            list.add(value);
        }
        return list;
    }


    //############################################
    // UTIL
    //############################################

    private void processPlugins(final RestfulList<PluginDescriptorRestful> plugins, final Iterable<PluginDescriptorRestful> items,
                                final Collection<String> restClasses) {
        for (PluginDescriptorRestful plugin : items) {
            plugins.add(plugin);

            final InstallStateMachine.State state = InstallStateMachine.getState(plugin);
            if (state == InstallStateMachine.State.ONBOARD
                    || state == InstallStateMachine.State.INSTALLING
                    || state == InstallStateMachine.State.INSTALLED)
            {
                final PluginParameterService params = PluginParameterServiceFactory.getParameterService(plugin);
                plugin.setHasConfiguration(params.hasConfiguration());
            }

            //############################################
            // collect endpoints
            //############################################
            final List<String> pluginRestClasses = plugin.getRestClasses();
            if (pluginRestClasses != null) {
                for (String clazz : pluginRestClasses) {
                    restClasses.add(clazz);
                }
            }
        }
    }

    private ErrorMessageRestful autoSetupIfPossible(final PluginDescriptor plugin) {
        final InstallStateMachine.State state = InstallStateMachine.getState(plugin);
        final PluginParameterService pluginParameters = PluginParameterServiceFactory.getParameterService(plugin);
        final ProjectSettings settings = PluginContextFactory.getContext(plugin).getProjectSettings();

        if (state != InstallStateMachine.State.ONBOARD
            || !hasGeneralizedSetUp(plugin)
            || (settings.isConfirmParams() && pluginParameters.hasGeneralizedSetupParameters()))
        {
            // auto-setup not possible
            return null;
        }

        final Map<String, Object> properties = new HashMap<>();

        properties.put("sampleData", Boolean.valueOf(settings.isUseSamples()).toString());
        properties.put("templateName", settings.getTemplateLanguage());

        return setupPlugin(plugin, properties);
    }

    private ErrorMessageRestful setupPlugin(final PluginDescriptor plugin, final Map<String, Object> properties) {
        final PluginContext context = new DefaultPluginContext(new PluginDescriptorRestful(ProjectSettingsBean.DEFAULT_NAME));
        context.addPlaceholderData(properties);

        HstUtils.erasePreview(context);

        // execute skeleton
        final InstructionPackage commonPackage = new CommonsInstructionPackage();
        commonPackage.setProperties(properties);
        getInjector().autowireBean(commonPackage);
        commonPackage.execute(context);

        // execute InstructionPackage itself
        final InstructionPackage instructionPackage = instructionPackageInstance(plugin);
        if (instructionPackage == null) {
            return new ErrorMessageRestful("Could not execute Installation package: " + plugin.getPackageFile(), DisplayEvent.DisplayType.STRONG);
        }
        instructionPackage.setProperties(properties);
        instructionPackage.execute(context);

        return updateInstallStateAfterSetup(plugin);
    }

    private ErrorMessageRestful updateInstallStateAfterSetup(final PluginDescriptor plugin) {
        ErrorMessageRestful msg = null; // no error message, signals success.

        try {
            InstallStateMachine.setup(plugin);
        } catch (InstallStateMachine.StateException e) {
            msg = new ErrorMessageRestful("There was an error in processing " + plugin.getName()
                                        + " Please see the error logs for more details");
        }

        return msg;
    }

    private PluginDescriptorRestful getPluginById(final String id, final ServletContext context) {
        if (Strings.isNullOrEmpty(id)) {
            return null;
        }

        for (final PluginDescriptorRestful plugin : getPlugins(context)) {
            if (id.equals(plugin.getId())) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Fetch all plugins.
     * @param servletContext context
     * @return list of plugins
     */
    private List<PluginDescriptorRestful> getPlugins(final ServletContext servletContext) {
        return getAllPlugins(servletContext).getItems();
    }


    /**
     * Loads plugin descriptors from different resources. Current support is:
     * <p>HTTP (remote plugin descriptor)</p>
     * <p>Class path (built in/bundled plugins)</p>
     * <p>File system: starting with file://</p>
     * <p>Classpath: starting with classpath://</p>
     *
     * @param servletContext
     * @return
     */
    private RestfulList<PluginDescriptorRestful> getAllPlugins(final ServletContext servletContext) {
        final RestfulList<PluginDescriptorRestful> plugins = new RestList<>();
        final List<PluginDescriptorRestful> items = getLocalPlugins();
        final Collection<String> restClasses = new ArrayList<>();
        // remote plugins
        final ProjectSettings projectSettings = getProjectSettings(servletContext);
        final Set<String> pluginRepositories = projectSettings.getPluginRepositories();
        for (String pluginRepository : pluginRepositories) {
            try {
                final RestfulList<PluginDescriptorRestful> myPlugins = pluginCache.get(pluginRepository);
                log.debug("{}", pluginCache.stats());
                if (myPlugins != null) {
                    final List<PluginDescriptorRestful> myPluginsItems = myPlugins.getItems();
                    CollectionUtils.addAll(items, myPluginsItems.iterator());
                }
            } catch (Exception e) {
                log.error(MessageFormat.format("Error loading plugins from repository: {0}", pluginRepository), e);
            }

        }

        processPlugins(plugins, items, restClasses);

        //############################################
        // Register endpoints:
        //############################################
        registerEndpoints(restClasses);
        return plugins;
    }


    private List<PluginDescriptorRestful> getLocalPlugins() {
        final InputStream stream = getClass().getResourceAsStream("/plugin_descriptor.json");
        final String json = GlobalUtils.readStreamAsText(stream);
        final ObjectMapper mapper = new ObjectMapper();
        try {
            @SuppressWarnings("unchecked")
            final RestfulList<PluginDescriptorRestful> restfulList = mapper.readValue(json, RestfulList.class);
            return restfulList.getItems();
        } catch (IOException e) {
            log.error("Error parsing plugins", e);
        }
        return Collections.emptyList();
    }

    private void registerEndpoints(final Collection<String> restClasses) {
        if (!initialized && !restClasses.isEmpty()) {
            //eventBus.register(rebuildListener);
            initialized = true;
            final RuntimeDelegate delegate = RuntimeDelegate.getInstance();
            final Bus bus = BusFactory.getDefaultBus();
            application = new DynamicRestPointsApplication();
            getInjector().autowireBean(application);
            addClasses(restClasses);
            // register:
            final ApplicationContext applicationContext = ContextLoader.getCurrentWebApplicationContext();
            final Object jsonProvider = applicationContext.getBean("jsonProvider");
            final JAXRSServerFactoryBean factoryBean = delegate.createEndpoint(application, JAXRSServerFactoryBean.class);
            factoryBean.setProvider(jsonProvider);
            factoryBean.setBus(bus);
            final Server server = factoryBean.create();
            server.start();
        } else {
            addClasses(restClasses);
        }
    }

    private void addClasses(final Iterable<String> restClasses) {
        for (String restClass : restClasses) {
            final Class<?> endpointClass = GlobalUtils.loadCLass(restClass);
            if (endpointClass == null) {
                log.error("Invalid application class: {}", restClass);
                continue;
            }
            final Set<Class<?>> classes = application.getClasses();
            if (classes.contains(endpointClass)) {
                log.debug("Class already loaded {}", restClass);
                continue;
            }
            application.addClass(endpointClass);
            log.info("Adding dynamic REST (plugin) endpoint {}", endpointClass.getName());

        }
    }

    private String installRepositoriesForPlugin(final PluginDescriptor plugin) {
        final StringBuilder builder = new StringBuilder();

        for (Repository repository : plugin.getRepositories()) {
            if (!DependencyUtils.addRepository(repository)) {
                if (builder.length() == 0) {
                    builder.append("Not all repositories were installed: ");
                } else {
                    builder.append(", ");
                }
                builder.append(repository.getUrl());
            }
        }

        return builder.toString();
    }

    private String installDependenciesForPlugin(final PluginDescriptor plugin) {
        final StringBuilder builder = new StringBuilder();

        for (EssentialsDependency dependency : plugin.getDependencies()) {
            if (!DependencyUtils.addDependency(dependency)) {
                if (builder.length() == 0) {
                    builder.append("Not all dependencies were installed: ");
                } else {
                    builder.append(", ");
                }
                builder.append(dependency.getGroupId()).append(':').append(dependency.getArtifactId());
            }
        }

        return builder.toString();
    }
}
