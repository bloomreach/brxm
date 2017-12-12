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

package org.onehippo.cms7.essentials.rest;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import com.google.common.collect.Multimap;

import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptor;
import org.onehippo.cms7.essentials.dashboard.model.ProjectSettings;
import org.onehippo.cms7.essentials.dashboard.packaging.CommonsInstructionPackage;
import org.onehippo.cms7.essentials.dashboard.packaging.InstructionPackage;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.ErrorMessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PluginModuleRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.HstUtils;
import org.onehippo.cms7.essentials.dashboard.utils.inject.ApplicationModule;
import org.onehippo.cms7.essentials.plugin.InstallState;
import org.onehippo.cms7.essentials.plugin.Plugin;
import org.onehippo.cms7.essentials.plugin.PluginException;
import org.onehippo.cms7.essentials.plugin.PluginStore;
import org.onehippo.cms7.essentials.project.ProjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@CrossOriginResourceSharing(allowAllOrigins = true)
@Api(value = "/plugins", description = "Rest resource which provides information about plugins: e.g. installed or available plugins")
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/plugins")
public class PluginResource extends BaseResource {

    public static final String PLUGIN_ID = "pluginId";
    private static Logger log = LoggerFactory.getLogger(PluginResource.class);
    private static final Lock setupLock = new ReentrantLock();

    @Inject
    private PluginStore pluginStore;

    @Inject
    private PluginContextFactory contextFactory;


    @SuppressWarnings("unchecked")
    @ApiOperation(
            value = "Fetch list of all plugin descriptors. " +
                    "For all plugins with dynamic REST endpoints, these get registered at /dynamic/{pluginEndpoint}",
            notes = "Retrieve a list of PluginDescriptorRestful objects",
            response = RestfulList.class)
    @GET
    @Path("/")
    public RestfulList<PluginDescriptor> getAllPlugins() {
        final RestfulList<PluginDescriptor> restfulList = new RestfulList<>();
        final List<Plugin> plugins = pluginStore.getAllPlugins();

        for (Plugin plugin : plugins) {
            final PluginDescriptor descriptor = plugin.getDescriptor();
            descriptor.setInstallState(plugin.getInstallState().toString());
            restfulList.add(descriptor);
        }

        return restfulList;
    }


    @ApiOperation(
            value = "Return plugin descriptor.",
            notes = "[API] plugin descriptor augmented with plugin's installState.",
            response = PluginDescriptor.class)
    @ApiParam(name = PLUGIN_ID, value = "Plugin ID", required = true)
    @GET
    @Path("/{" + PLUGIN_ID + '}')
    public PluginDescriptor getPlugin(@PathParam(PLUGIN_ID) final String pluginId) {
        final Plugin plugin = pluginStore.getPluginById(pluginId);
        if (plugin == null) {
            return null;
        }
        final PluginDescriptor descriptor = plugin.getDescriptor();
        descriptor.setInstallState(plugin.getInstallState().toString());
        return descriptor;
    }


    @ApiOperation(
            value = "Return a list of changes made by the plugin during setup, given certain parameter values.",
            notes = "[API] Messages are only indication what might change, because operations may be skipped, "
                    + "e.g. file copy is not executed if file already exists.",
            response = RestfulList.class)
    @ApiParam(name = PLUGIN_ID, value = "Plugin ID", required = true)
    @POST
    @Path("/{" + PLUGIN_ID + "}/changes")
    public RestfulList<MessageRestful> getInstructionPackageChanges(@PathParam(PLUGIN_ID) final String pluginId,
                                                                    final PostPayloadRestful payload) throws Exception {
        final RestfulList<MessageRestful> list = new RestfulList<>();

        final Plugin plugin = pluginStore.getPluginById(pluginId);
        if (plugin == null) {
            list.add(new ErrorMessageRestful("Setup Changes: Plugin with ID '" + pluginId + "' was not found."));
            return list;
        }

        final InstructionPackage instructionPackage = plugin.makeInstructionPackageInstance();
        if (instructionPackage == null) {
            list.add(new ErrorMessageRestful("Failed to create instructions package"));
            return list;
        }

        final Map<String, Object> properties = makeSetupPropertiesFromValues(payload.getValues());
        instructionPackage.setProperties(properties);
        final PluginContext context = contextFactory.getContext();
        context.addPlaceholderData(properties);

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


    @ApiOperation(
            value = "Install a plugin into Essentials",
            response = MessageRestful.class)
    @ApiParam(name = PLUGIN_ID, value = "Plugin ID", required = true)
    @POST
    @Path("/{" + PLUGIN_ID + "}/install")
    public MessageRestful installPlugin(@PathParam(PLUGIN_ID) String pluginId) throws Exception {
        final Plugin plugin = pluginStore.getPluginById(pluginId);
        if (plugin == null) {
            return new ErrorMessageRestful("Installation failed: Plugin with ID '" + pluginId + "' was not found.");
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
            value = "Trigger a generalized setup (by means of executing an instructions package).",
            notes = "[API] generalized setup may be executed automatically for installed plugins," +
                    "depending on the project settings.",
            response = MessageRestful.class)
    @ApiParam(name = PLUGIN_ID, value = "Plugin ID", required = true)
    @POST
    @Path("/{" + PLUGIN_ID + "}/setup")
    public MessageRestful setupPluginGeneralized(@PathParam(PLUGIN_ID) final String pluginId,
                                                 final PostPayloadRestful payloadRestful) {
        final Plugin plugin = pluginStore.getPluginById(pluginId);
        if (plugin == null) {
            return new ErrorMessageRestful("Setup failed: Plugin with ID '" + pluginId + "' was not found.");
        }

        final Map<String, Object> properties = makeSetupPropertiesFromValues(payloadRestful.getValues());
        final String msg = setupPlugin(plugin, properties);
        if (msg != null) {
            return new ErrorMessageRestful(msg);
        }

        return new MessageRestful("Plugin <strong>" + plugin + "</strong> successfully set up.");
    }


    @ApiOperation(
            value = "Signal to the dashboard that the plugin's setup phase has completed.",
            notes = "[API] To be used if the plugin comes with a non-generalized (i.e. custom) setup phase.")
    @ApiParam(name = PLUGIN_ID, value = "Plugin ID", required = true)
    @POST
    @Path("/{" + PLUGIN_ID + "}/setupcomplete")
    public void signalPluginSetupComplete(@PathParam(PLUGIN_ID) final String pluginId) {
        updateInstallStateAfterSetup(pluginStore.getPluginById(pluginId));
    }


    @ApiOperation(
            value = "Check for each plugin if its setup phase can be triggered.",
            response = MessageRestful.class
    )
    @POST
    @Path("/autosetup")
    public MessageRestful autoSetupPlugins(@Context ServletContext servletContext) {
        final StringBuilder builder = new StringBuilder();
        // We lock the ping to avoid concurrent auto-setup. Concurrent auto-setup may happen
        // if the user(s) has/have two browsers pointed at the restarting Essentials WAR. Not
        // locking would lead to duplicate setup/bootstrapping.
        if (!setupLock.tryLock()) {
            log.warn("WARNING: You appear to be using two dashboards at the same time. Essentials doesn't support that." +
                    " Check if you have multiple tabs open, pointing at Essentials, and if so, close all except for one.");
            setupLock.lock();
        }
        try {
            for (Plugin plugin : pluginStore.getAllPlugins()) {
                plugin.promote();
                final String msg = autoSetupIfPossible(plugin);
                if (msg != null) {
                    builder.append(msg).append(" - ");
                }
            }
            ProjectUtils.setInitialized(true);
        } finally {
            setupLock.unlock();
        }
        return (builder.length() > 0) ? new ErrorMessageRestful(builder.toString()) : null;
    }


    @ApiOperation(
            value = "Clears plugin cache",
            notes = "Remote Plugin descriptors are cached for 1 hour. " +
                    "[DEBUG] This method clears plugin cache and plugins are fetched again on next requests",
            response = MessageRestful.class)
    @GET
    @Path("/clearcache")
    public MessageRestful clearCache(@Context ServletContext servletContext) {
        pluginStore.clearCache();
        return new MessageRestful("Plugin Cache invalidated");
    }


    @ApiOperation(
            value = "Return list of plugin Javascript modules",
            notes = "Modules are prefixed with 'tool' or 'feature', depending on their plugin type. "
                    + "This method is only used outside of the front-end's AngularJS application.",
            response = PluginModuleRestful.class)
    @GET
    @Path("/modules")
    public PluginModuleRestful getModules() {
        final PluginModuleRestful modules = new PluginModuleRestful();
        pluginStore.getAllPlugins().forEach(p -> modules.addFiles(p.getDescriptor()));
        return modules;
    }


    private String autoSetupIfPossible(final Plugin plugin) {
        if (plugin.getInstallState() != InstallState.ONBOARD || !plugin.hasGeneralizedSetUp()) {
            return null; // auto-setup not possible
        }

        final ProjectSettings settings = pluginStore.getProjectSettings();
        if (settings.isConfirmParams()) {
            return null; // skip auto-setup if per-plugin parameters are requested and the plugin actually has parameters
        }

        final Map<String, Object> properties = new HashMap<>();
        preProcessSetupProperties(properties);

        return setupPlugin(plugin, properties);
    }

    private String setupPlugin(final Plugin plugin, final Map<String, Object> properties) {
        final PluginContext context = contextFactory.getContext();
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

    private void preProcessSetupProperties(final Map<String, Object> properties) {
        final ProjectSettings settings = pluginStore.getProjectSettings();

        if (!properties.containsKey(EssentialConst.PROP_SAMPLE_DATA)) {
            properties.put(EssentialConst.PROP_SAMPLE_DATA, settings.isUseSamples());
        }
        if (!properties.containsKey(EssentialConst.PROP_TEMPLATE_NAME)) {
            properties.put(EssentialConst.PROP_TEMPLATE_NAME, settings.getTemplateLanguage());
        }
        if (!properties.containsKey(EssentialConst.PROP_EXTRA_TEMPLATES)) {
            properties.put(EssentialConst.PROP_EXTRA_TEMPLATES, settings.isExtraTemplates());
        }
    }

    /**
     * Convert String values to Booleans if possible.
     * Doing this here helps avoiding additional conversions downstream.
     *
     * TODO: check if we can refactor the PostPayloadRestful so this happens automatically?
     */
    private Map<String, Object> makeSetupPropertiesFromValues(final Map<String, String> values) {
        final Map<String, Object> properties = new HashMap<>();

        for (Map.Entry<String, String> entry : values.entrySet()) {
            if ("true".equalsIgnoreCase(entry.getValue()) ||
                "false".equalsIgnoreCase(entry.getValue())) {
                properties.put(entry.getKey(), Boolean.valueOf(entry.getValue()));
            } else {
                properties.put(entry.getKey(), entry.getValue());
            }
        }

        preProcessSetupProperties(properties);

        return properties;
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
