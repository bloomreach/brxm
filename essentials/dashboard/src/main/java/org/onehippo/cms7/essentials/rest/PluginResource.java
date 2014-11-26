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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;
import org.onehippo.cms7.essentials.dashboard.config.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.config.PluginParameterService;
import org.onehippo.cms7.essentials.plugin.PluginParameterServiceFactory;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.event.RebuildEvent;
import org.onehippo.cms7.essentials.dashboard.event.listeners.RebuildProjectEventListener;
import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptor;
import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptorRestful;
import org.onehippo.cms7.essentials.dashboard.model.ProjectSettings;
import org.onehippo.cms7.essentials.plugin.InstallState;
import org.onehippo.cms7.essentials.plugin.Plugin;
import org.onehippo.cms7.essentials.plugin.PluginException;
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
import org.onehippo.cms7.essentials.dashboard.utils.HstUtils;
import org.onehippo.cms7.essentials.dashboard.utils.inject.ApplicationModule;
import org.onehippo.cms7.essentials.rest.model.ControllerRestful;
import org.onehippo.cms7.essentials.rest.model.RestList;
import org.onehippo.cms7.essentials.rest.model.StatusRestful;
import org.onehippo.cms7.essentials.rest.model.SystemInfo;
import org.onehippo.cms7.essentials.plugin.PluginStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
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

    public static final String PLUGIN_ID = "pluginId";
    private static Logger log = LoggerFactory.getLogger(PluginResource.class);
    private static final Lock setupLock = new ReentrantLock();
    private boolean frontendInitialized;

    @Inject
    private RebuildProjectEventListener rebuildListener;

    @Inject
    private PluginStore pluginStore;

    @SuppressWarnings("unchecked")
    @ApiOperation(
            value = "Fetches local and remote file descriptors  and checks for available Hippo Essentials plugins. " +
                    "It also registers any plugin REST endpoints which come available under /dynamic endpoint e.g. /dynamic/{pluginEndpoint}",
            notes = "Retrieves a list of PluginRestful objects",
            response = RestfulList.class)
    @GET
    @Path("/")
    public RestfulList<PluginDescriptorRestful> fetchPlugins(@Context ServletContext servletContext) {
        final RestfulList<PluginDescriptorRestful> restfulList = new RestfulList<>();
        final List<Plugin> plugins = pluginStore.getAllPlugins();

        for (Plugin plugin : plugins) {
            final PluginDescriptorRestful descriptor = (PluginDescriptorRestful)plugin.getDescriptor();
            descriptor.setInstallState(plugin.getInstallState().toString());
            restfulList.add(descriptor);
        }

        return restfulList;
    }


    @ApiOperation(
            value = "Check for each plugin if its setup phase can be triggered.",
            response = MessageRestful.class
    )
    @POST
    @Path("/autosetup")
    public MessageRestful autoSetupPlugins(@Context ServletContext servletContext) {
        frontendInitialized = true;
        final StringBuilder builder = new StringBuilder();

        // We lock the ping to avoid concurrent auto-setup. Concurrent auto-setup may happen
        // if the user(s) has/have two browsers pointed at the restarting Essentials WAR. Not
        // locking would lead to duplicate setup/bootstrapping.
        if (!setupLock.tryLock()) {
            log.warn("WARNING: You appear to be using two dashboards at the same time. Essentials doesn't support that." +
                    " Check if you have multiple tabs open, pointing at Essentials, and if so, close all except for one.");
            setupLock.lock();
        }
        for (Plugin plugin : pluginStore.getAllPlugins()) {
            plugin.promote();
            final String msg = autoSetupIfPossible(plugin);
            if (msg != null) {
                builder.append(msg).append(" - ");
            }
        }
        setupLock.unlock();

        return (builder.length() > 0) ? new ErrorMessageRestful(builder.toString()) : null;
    }


    @ApiOperation(
            value = "Ping, returns true if application is initialized",
            response = boolean.class)
    @GET
    @Path("/ping")
    public SystemInfo ping(@Context ServletContext servletContext) {
        final SystemInfo systemInfo = new SystemInfo();

        try {
            // tell the pinger when to (re-)initialize the front-end.
            systemInfo.setInitialized(frontendInitialized);

            final List<Plugin> plugins = pluginStore.getAllPlugins();
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
                    if (installState == InstallState.BOARDING
                            || installState == InstallState.INSTALLING) {
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
        } catch (Exception e) {
            log.error("pinger had exception!", e);
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
        pluginStore.clearCache();
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
        final Plugin plugin = pluginStore.getPluginById(pluginId);
        if (plugin == null) {
            return new ErrorMessageRestful("No valid InstructionPackage was selected");
        }

        final Map<String, Object> properties = new HashMap<String, Object>(values);
        final String msg = setupPlugin(plugin, properties);
        if (msg != null) {
            return new ErrorMessageRestful(msg);
        }

        return new MessageRestful("Successfully installed " + plugin, DisplayEvent.DisplayType.STRONG);
    }

    @ApiOperation(value = "Signal to the dashboard that the plugin's setup phase has completed.")
    @ApiParam(name = PLUGIN_ID, value = "Plugin id", required = true)
    @POST
    @Path("/setup/{pluginId}")
    public void signalSetup(@PathParam(PLUGIN_ID) String pluginId) {
        updateInstallStateAfterSetup(pluginStore.getPluginById(pluginId));
    }

    @ApiOperation(
            value = "Saves global project settings",
            response = KeyValueRestful.class)
    @POST
    @Path("/savesettings")
    public KeyValueRestful hideWelcomeScreen(final ProjectSettingsBean payload, @Context ServletContext servletContext) {
        try {
            final PluginContext context = PluginContextFactory.getContext();
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
        final Plugin plugin = pluginStore.getPluginById(pluginId);
        if (plugin == null) {
            return new PluginDescriptorRestful();
        }
        final PluginDescriptor descriptor = plugin.getDescriptor();
        descriptor.setInstallState(plugin.getInstallState().toString());
        return descriptor;
    }

    @ApiOperation(
            value = "Installs a plugin",
            response = MessageRestful.class)
    @ApiParam(name = PLUGIN_ID, value = "Plugin  id", required = true)
    @POST
    @Path("/install/{pluginId}")
    public MessageRestful installPlugin(@Context ServletContext servletContext, @PathParam(PLUGIN_ID) String pluginId) throws Exception {
        final Plugin plugin = pluginStore.getPluginById(pluginId);
        if (plugin == null) {
            return new ErrorMessageRestful("Plugin was not found and could not be installed");
        }

        try {
            plugin.install();
        } catch (PluginException e) {
            log.error(e.getMessage(), e);
            return new ErrorMessageRestful(e.getMessage());
        }

        String msg = autoSetupIfPossible(plugin);
        if (msg != null) {
            return new ErrorMessageRestful(msg);
        }

        return new MessageRestful("Plugin <strong>" + plugin + "</strong> successfully installed.");
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
    public ProjectSettings getProjectSettings() {
        return pluginStore.getProjectSettings();
    }

    @GET
    @Path("/controllers")
    public RestfulList<ControllerRestful> getControllers(@Context ServletContext servletContext) throws Exception {

        final RestfulList<ControllerRestful> controllers = new RestList<>();
        for (Plugin plugin : pluginStore.getAllPlugins()) {
            final String pluginId = plugin.getId();
            if (Strings.isNullOrEmpty(pluginId)) {
                continue;
            }
            controllers.add(new ControllerRestful(pluginId, String.format("%sCtrl", pluginId), String.format("plugins/%s/index.html", pluginId)));
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
            final PluginContext context = PluginContextFactory.getContext();
            try (PluginConfigService configService = context.getConfigService()) {
                final ProjectSettingsBean document = configService.read(ProjectSettingsBean.DEFAULT_NAME, ProjectSettingsBean.class);
                if (document != null && document.getSetupDone()) {
                    status.setStatus(true);
                }
            }
        } catch (Exception e) {
            log.error("Error checking InstructionPackage status", e);
        }
        status.setPluginsInstalled(pluginStore.countInstalledPlugins());
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
        final List<Plugin> plugins = pluginStore.getAllPlugins();
        for (Plugin plugin : plugins) {
            // TODO: why is getLibraries not part of the descriptor interface?
            final PluginDescriptorRestful descriptor = (PluginDescriptorRestful)plugin.getDescriptor();
            final List<PluginModuleRestful.PrefixedLibrary> libraries = descriptor.getLibraries();

            final String prefix = descriptor.getType();
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
        final Plugin plugin = pluginStore.getPluginById(pluginId);

        final RestfulList<MessageRestful> list = new RestfulList<>();
        if (Strings.isNullOrEmpty(pluginId) || plugin == null) {
            final MessageRestful resource = new MessageRestful("No valid InstructionPackage was selected");
            resource.setSuccessMessage(false);
            list.add(resource);
            return list;
        }
        final InstructionPackage instructionPackage = plugin.makeInstructionPackageInstance();
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

    private String autoSetupIfPossible(final Plugin plugin) {
        final PluginParameterService pluginParameters = PluginParameterServiceFactory.getParameterService(plugin);
        final ProjectSettings settings = pluginStore.getProjectSettings();

        if (plugin.getInstallState() != InstallState.ONBOARD
            || !plugin.hasGeneralizedSetUp()
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

    private String setupPlugin(final Plugin plugin, final Map<String, Object> properties) {
        final PluginContext context = PluginContextFactory.getContext();
        context.addPlaceholderData(properties);

        HstUtils.erasePreview(context);

        // execute skeleton
        final InstructionPackage commonPackage = new CommonsInstructionPackage();
        commonPackage.setProperties(properties);
        getInjector().autowireBean(commonPackage);
        commonPackage.execute(context);

        // execute InstructionPackage itself
        final InstructionPackage instructionPackage = plugin.makeInstructionPackageInstance();
        if (instructionPackage != null) {
            ApplicationModule.getInjector().autowireBean(instructionPackage);
            instructionPackage.setProperties(properties);
            instructionPackage.execute(context);
        }

        return updateInstallStateAfterSetup(plugin);
    }

    private String updateInstallStateAfterSetup(final Plugin plugin) {
        String msg = null; // no error message, signals success.

        try {
            plugin.setup();
        } catch (PluginException e) {
            log.error("Error setting up plugin '" + plugin + "'.", e);
            msg = "There was an error in processing " + plugin + " Please see the error logs for more details";
        }

        return msg;
    }
}
