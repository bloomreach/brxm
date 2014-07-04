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

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
import org.onehippo.cms7.essentials.dashboard.config.FilePluginService;
import org.onehippo.cms7.essentials.dashboard.config.InstallerDocument;
import org.onehippo.cms7.essentials.dashboard.config.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.ctx.DefaultPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.event.listeners.MemoryPluginEventListener;
import org.onehippo.cms7.essentials.dashboard.model.EssentialsDependency;
import org.onehippo.cms7.essentials.dashboard.model.Plugin;
import org.onehippo.cms7.essentials.dashboard.model.PluginRestful;
import org.onehippo.cms7.essentials.dashboard.model.ProjectSettings;
import org.onehippo.cms7.essentials.dashboard.model.Repository;
import org.onehippo.cms7.essentials.dashboard.packaging.CommonsInstructionPackage;
import org.onehippo.cms7.essentials.dashboard.packaging.InstructionPackage;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PluginModuleRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.setup.ProjectSetupPlugin;
import org.onehippo.cms7.essentials.dashboard.utils.DependencyUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.rest.client.RestClient;
import org.onehippo.cms7.essentials.rest.model.ControllerRestful;
import org.onehippo.cms7.essentials.rest.model.RestList;
import org.onehippo.cms7.essentials.rest.model.StatusRestful;
import org.onehippo.cms7.essentials.servlet.DynamicRestPointsApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
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
     * Plugin cache to avoid remote calls
     */
    private final LoadingCache<String, RestfulList<PluginRestful>> pluginCache = CacheBuilder.newBuilder()
            .expireAfterAccess(60, TimeUnit.MINUTES)
            .recordStats()
            .build(new CacheLoader<String, RestfulList<PluginRestful>>() {
                @Override
                public RestfulList<PluginRestful> load(final String url) throws Exception {
                    RestClient client = new RestClient(url);
                    return client.getPlugins();
                }


            });

    public static final int WEEK_OLD = -7;
    public static final String PLUGIN_ID = "pluginId";
    @Inject
    private EventBus eventBus;

    private DynamicRestPointsApplication application;

    @Inject
    private MemoryPluginEventListener listener;


    private boolean initialized;
    private static Logger log = LoggerFactory.getLogger(PluginResource.class);

    @SuppressWarnings("unchecked")
    @ApiOperation(
            value = "Fetches local and remote file descriptors  and checks for available Hippo Essentials plugins. " +
                    "It also registers any plugin REST endpoints which come available under /dynamic endpoint e.g. /dynamic/{pluginEndpoint}",
            notes = "Retrieves a list of PluginRestful objects",
            response = RestfulList.class)
    @GET
    @Path("/")
    public RestfulList<PluginRestful> fetchPlugins(@Context ServletContext servletContext) {
        return getAllPlugins(servletContext);
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
        Plugin myPlugin = getPluginById(pluginId, servletContext);

        if (Strings.isNullOrEmpty(pluginId) || myPlugin == null) {
            final MessageRestful resource = new MessageRestful("No valid InstructionPackage was selected");
            resource.setSuccessMessage(false);
            return resource;
        }
        final Map<String, Object> properties = new HashMap<String, Object>(values);
        final PluginContext context = new DefaultPluginContext(new PluginRestful(ProjectSettingsBean.DEFAULT_NAME));
        context.addPlaceholderData(properties);
        //############################################
        // EXECUTE SKELETON:
        //############################################
        final InstructionPackage commonPackage = new CommonsInstructionPackage();
        commonPackage.setProperties(properties);
        getInjector().autowireBean(commonPackage);
        commonPackage.execute(context);
        // execute InstructionPackage itself
        InstructionPackage instructionPackage = instructionPackageInstance(myPlugin);
        if (instructionPackage == null) {
            return new MessageRestful("Could not execute Installation package", DisplayEvent.DisplayType.STRONG);
        }
        instructionPackage.setProperties(properties);
        instructionPackage.execute(context);

        // update the plugins installer document
        try (PluginConfigService service = new FilePluginService(context)) {
            InstallerDocument document = service.read(pluginId, InstallerDocument.class);
            if (document == null) {
                // pre-installed plugins do not yet have an installer document.
                document = createPluginInstallerDocument(pluginId);
            }
            document.setDateAdded(Calendar.getInstance());
            service.write(document);
        } catch (Exception e) {
            log.error("Error in proccessing installer documents", e);
        }

        return new MessageRestful("Please rebuild and restart your application:", DisplayEvent.DisplayType.STRONG);
    }


    @ApiOperation(
            value = "Saves global project settings",
            response = KeyValueRestful.class)
    @POST
    @Path("/savesettings")
    public KeyValueRestful hideWelcomeScreen(final ProjectSettingsBean payload, @Context ServletContext servletContext) {
        try {
            final Plugin plugin = getPluginById(ProjectSetupPlugin.class.getName(), servletContext);
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
            value = "Adds a plugin to recently installed list of plugins",
            response = RestfulList.class)
    @POST
    @Path("/configure/add")
    public RestfulList<PluginRestful> addToRecentlyInstalled(@Context ServletContext servletContext, final PostPayloadRestful payload) {

        final RestfulList<PluginRestful> plugins = new RestList<>();
        final List<PluginRestful> pluginList = getPlugins(servletContext);
        for (Plugin p : pluginList) {

            final PluginRestful resource = new PluginRestful();

            resource.setTitle(p.getName());
            resource.setInstalled(isInstalled(p));
            // TODO save this list
            plugins.add(resource);
        }
        return plugins;
    }


    @ApiOperation(
            value = "Lists of all available plugins",
            notes = "Retrieves list of  PluginRestful objects",
            response = RestfulList.class)
    @GET
    @Path("/configure/list")
    public RestfulList<PluginRestful> getRecentlyInstalled(@Context ServletContext servletContext) {

        final RestfulList<PluginRestful> plugins = new RestList<>();
        final List<PluginRestful> pluginList = getPlugins(servletContext);
        for (Plugin plugin : pluginList) {
            final PluginRestful resource = new PluginRestful();
            resource.setTitle(plugin.getName());
            resource.setPluginId(plugin.getPluginId());
            resource.setInstalled(isInstalled(plugin));
            plugins.add(resource);
        }
        return plugins;
    }


    @ApiOperation(
            value = "Returns plugin descriptor file",
            notes = "Used for plugin layout etc.",
            response = PluginRestful.class)
    @ApiParam(name = PLUGIN_ID, value = "Plugin id", required = true)
    @GET
    @Path("/plugins/{pluginId}")
    public Plugin getPlugin(@Context ServletContext servletContext, @PathParam(PLUGIN_ID) String pluginId) {
        final List<PluginRestful> pluginList = getPlugins(servletContext);
        for (Plugin plugin : pluginList) {
            if (plugin.getPluginId().equals(pluginId)) {
                return plugin;
            }
        }
        return new PluginRestful();
    }

    @ApiOperation(
            value = "Checks if certain plugin is installed",
            notes = "Sets PluginRestful installed flag to true or false",
            response = PluginRestful.class)
    @ApiParam(name = PLUGIN_ID, value = "Plugin id", required = true)
    @GET
    @Path("/installstate/{pluginId}")
    public PluginRestful getInstallStateList(@Context ServletContext servletContext, @PathParam(PLUGIN_ID) String pluginId) {

        final PluginRestful resource = new PluginRestful();
        final List<PluginRestful> pluginList = getPlugins(servletContext);
        for (Plugin plugin : pluginList) {
            if (plugin.getPluginId().equals(pluginId)) {
                if (Strings.isNullOrEmpty(plugin.getPluginId())) {
                    continue;
                }
                resource.setTitle(plugin.getName());
                resource.setPluginId(plugin.getPluginId());
                resource.setInstalled(isInstalled(plugin));
                return resource;
            }


        }
        return resource;
    }

    @ApiOperation(
            value = "Installs a plugin",
            response = MessageRestful.class)
    @ApiParam(name = PLUGIN_ID, value = "Plugin  id", required = true)
    @POST
    @Path("/install/{pluginId}")
    public MessageRestful installPlugin(@Context ServletContext servletContext, @PathParam(PLUGIN_ID) String pluginId) throws Exception {

        final MessageRestful message = new MessageRestful();
        final RestfulList<PluginRestful> pluginList = getAllPlugins(servletContext);
        for (PluginRestful plugin : pluginList.getItems()) {
            final String id = plugin.getPluginId();
            if (Strings.isNullOrEmpty(id)) {
                continue;
            }
            if (pluginId.equals(id)) {
                if (isInstalled(plugin)) {
                    message.setValue("Plugin was already installed. Please rebuild and restart your application");
                    return message;
                }
                final List<EssentialsDependency> dependencies = plugin.getDependencies();
                final Collection<EssentialsDependency> dependenciesNotInstalled = new ArrayList<>();
                for (EssentialsDependency dependency : dependencies) {

                    final boolean installed = DependencyUtils.addDependency(dependency);
                    if (!installed) {
                        dependenciesNotInstalled.add(dependency);
                    }
                }
                final List<Repository> repositories = plugin.getRepositories();
                final Collection<Repository> repositoriesNotInstalled = new ArrayList<>();

                for (Repository repository : repositories) {
                    final boolean installed = DependencyUtils.addRepository(repository);
                    if (!installed) {
                        repositoriesNotInstalled.add(repository);
                    }
                }

                if (dependenciesNotInstalled.size() == 0 && repositoriesNotInstalled.size() == 0) {
                    final PluginContext context = getContext(servletContext);
                    try (PluginConfigService service = new FilePluginService(context)) {
                        service.write(createPluginInstallerDocument(id));
                    }
                    message.setValue("Plugin successfully installed. Please rebuild and restart your application");
                    return message;
                } else {
                    final StringBuilder builder = new StringBuilder();
                    if (dependenciesNotInstalled.size() > 0) {
                        builder.append("Not all dependencies were successfully installed: ");
                        for (EssentialsDependency essentialsDependency : dependenciesNotInstalled) {
                            builder.append(essentialsDependency.getGroupId()).append(':').append(essentialsDependency.getArtifactId());
                            builder.append(", ");
                        }
                    }
                    if (repositoriesNotInstalled.size() > 0) {
                        builder.append("Not all repositories were installed: ");
                        for (Repository essentialsRepository : repositoriesNotInstalled) {
                            builder.append(essentialsRepository.getUrl());
                            builder.append(", ");
                        }
                    }
                    message.setValue(builder.toString());
                    message.setSuccessMessage(false);
                    return message;

                }

            }
        }

        message.setSuccessMessage(false);
        message.setValue("Plugin was not found and could not be installed");
        return message;
    }


    @ApiOperation(
            value = "Returns list of project settings like project namespace, project path etc. ",
            notes = "Contains a list of KeyValueRestful objects",
            response = RestfulList.class)
    @GET
    @Path("/settings")
    public RestfulList<KeyValueRestful> getKeyValue(@Context ServletContext servletContext) {
        final PluginContext context = getContext(servletContext);
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
        final PluginContext context = getContext(servletContext);
        return context.getProjectSettings();
    }

    @GET
    @Path("/controllers")
    public RestfulList<ControllerRestful> getControllers(@Context ServletContext servletContext) throws Exception {

        final RestfulList<ControllerRestful> controllers = new RestList<>();
        final List<PluginRestful> plugins = getPlugins(servletContext);
        for (Plugin plugin : plugins) {
            final String pluginLink = plugin.getPluginId();
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
            final Plugin plugin = getPluginById(ProjectSetupPlugin.class.getName(), servletContext);
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
        final List<PluginRestful> plugins = getPlugins(servletContext);
        for (PluginRestful plugin : plugins) {
            final List<PluginModuleRestful.PrefixedLibrary> libraries = plugin.getLibraries();

            final String prefix = plugin.getType();
            final String pluginId = plugin.getPluginId();
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
        final PluginContext context = getContext(servletContext);
        context.addPlaceholderData(new HashMap<String, Object>(values));

        final String pluginId = values.get(PLUGIN_ID);
        final Plugin myPlugin = getPluginById(pluginId, servletContext);

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
            list.add(value);
        }
        return list;
    }


    //############################################
    // UTIL
    //############################################

    private void processPlugins(final RestfulList<PluginRestful> plugins, final Iterable<PluginRestful> items, final Collection<String> restClasses, final PluginConfigService service) {
        for (PluginRestful item : items) {
            plugins.add(item);
            final String pluginId = item.getPluginId();
            if (!isInstalled(item)) {
                item.setNeedsInstallation(true);
            }
            //############################################
            // collect endpoints
            //############################################
            final List<String> pluginRestClasses = item.getRestClasses();
            if (pluginRestClasses != null) {
                for (String clazz : pluginRestClasses) {
                    restClasses.add(clazz);
                }
            }


            // check if recently installed:
            // TODO: move to client?
            final InstallerDocument document = service.read(pluginId, InstallerDocument.class);
            if (document != null && document.getDateInstalled() != null) {
                final Calendar dateInstalled = document.getDateInstalled();
                final Calendar lastWeek = Calendar.getInstance();
                lastWeek.add(Calendar.DAY_OF_MONTH, WEEK_OLD);
                if (dateInstalled.after(lastWeek)) {
                    item.setDateInstalled(dateInstalled);
                }
            }
        }
    }

    private Plugin getPluginById(final String id, final ServletContext context) {
        if (Strings.isNullOrEmpty(id)) {
            return null;
        }

        final List<PluginRestful> plugins = getPlugins(context);
        for (final Plugin next : plugins) {
            final String pluginId = next.getPluginId();
            if (Strings.isNullOrEmpty(pluginId)) {
                continue;
            }
            if (pluginId.equals(id)) {
                return next;
            }
        }
        return null;
    }

    private InstallerDocument createPluginInstallerDocument(final String pluginId) {
        final InstallerDocument document = new InstallerDocument();

        document.setName(pluginId);
        document.setDateInstalled(Calendar.getInstance());

        return document;
    }


    private List<PluginRestful> getPlugins(final ServletContext servletContext) {
        return getAllPlugins(servletContext).getItems();
    }


    private RestfulList<PluginRestful> getAllPlugins(final ServletContext servletContext) {
        final RestfulList<PluginRestful> plugins = new RestList<>();
        final List<PluginRestful> items = getLocalPlugins(servletContext);
        final Collection<String> restClasses = new ArrayList<>();
        final PluginContext context = getContext(servletContext);
        // remote plugins
        final ProjectSettings projectSettings = getProjectSettings(servletContext);
        final Set<String> pluginRepositories = projectSettings.getPluginRepositories();
        for (String pluginRepository : pluginRepositories) {
            if (pluginRepository.startsWith("http")) {
                try {
                    final RestfulList<PluginRestful> myPlugins = pluginCache.get(pluginRepository);
                    log.debug("{}", pluginCache.stats());
                    if (myPlugins != null) {
                        final List<PluginRestful> myPluginsItems = myPlugins.getItems();
                        CollectionUtils.addAll(items, myPluginsItems.iterator());
                    }
                } catch (Exception e) {
                    log.error(MessageFormat.format("Error loading plugins from repository: {0}", pluginRepository), e);
                }
            }
        }


        try (PluginConfigService service = new FilePluginService(context)) {
            processPlugins(plugins, items, restClasses, service);
        } catch (Exception e) {
            log.error("Error processing plugins", e);
        }
        //############################################
        // Register endpoints:
        //############################################
        registerEndpoints(restClasses);
        return plugins;
    }


    private List<PluginRestful> getLocalPlugins(final ServletContext servletContext) {
        final InputStream stream = getClass().getResourceAsStream("/plugin_descriptor.json");
        final String json = GlobalUtils.readStreamAsText(stream);
        final ObjectMapper mapper = new ObjectMapper();
        try {

            @SuppressWarnings("unchecked")
            final RestfulList<PluginRestful> restfulList = mapper.readValue(json, RestfulList.class);

            postProcessPlugins(restfulList, servletContext);

            return restfulList.getItems();
        } catch (IOException e) {
            log.error("Error parsing plugins", e);
        }
        return Collections.emptyList();

    }

    private void registerEndpoints(final Collection<String> restClasses) {
        if (!initialized && !restClasses.isEmpty()) {
            initialized = true;
            final RuntimeDelegate delegate = RuntimeDelegate.getInstance();
            final Bus bus = BusFactory.getDefaultBus();
            application = new DynamicRestPointsApplication();
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


}
