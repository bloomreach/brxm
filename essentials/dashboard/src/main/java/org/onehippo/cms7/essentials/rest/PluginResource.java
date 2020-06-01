/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;
import org.onehippo.cms7.essentials.dashboard.install.InstallService;
import org.onehippo.cms7.essentials.dashboard.install.InstallStateMachine;
import org.onehippo.cms7.essentials.plugin.PluginSet;
import org.onehippo.cms7.essentials.plugin.PluginStore;
import org.onehippo.cms7.essentials.plugin.sdk.packaging.DefaultInstructionPackage;
import org.onehippo.cms7.essentials.plugin.sdk.rest.ChangeMessage;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;
import org.onehippo.cms7.essentials.rest.model.ApplicationData;
import org.onehippo.cms7.essentials.sdk.api.model.rest.PluginDescriptor;
import org.onehippo.cms7.essentials.sdk.api.model.rest.UserFeedback;
import org.onehippo.cms7.essentials.sdk.api.service.SettingsService;
import org.onehippo.cms7.essentials.sdk.api.model.ProjectSettings;
import org.onehippo.cms7.essentials.utils.DashboardUtils;
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
public class PluginResource {

    private static final String PLUGIN_ID = "pluginId";
    private static Logger log = LoggerFactory.getLogger(PluginResource.class);
    private static final Lock setupLock = new ReentrantLock();

    private final PluginStore pluginStore;
    private final SettingsService settingsService;
    private final InstallService installService;
    private final InstallStateMachine installStateMachine;

    @Inject
    public PluginResource(final PluginStore pluginStore, final SettingsService settingsService,
                          final InstallService installService, final InstallStateMachine installStateMachine) {
        this.pluginStore = pluginStore;
        this.settingsService = settingsService;
        this.installService = installService;
        this.installStateMachine = installStateMachine;
    }

    @ApiOperation(
            value = "Fetch list of all plugin descriptors. " +
                    "For all plugins with dynamic REST endpoints, these get registered at /dynamic/{pluginEndpoint}",
            response = List.class)
    @GET
    @Path("/")
    public List<PluginDescriptor> getAllPlugins() {
        return new ArrayList<>(pluginStore.loadPlugins().getPlugins());
    }


    @ApiOperation(
            value = "Fetch plugin descriptor for the specified plugin.",
            response = PluginDescriptor.class)
    @ApiParam(name = PLUGIN_ID, value = "Plugin ID", required = true)
    @GET
    @Path("/{" + PLUGIN_ID + '}')
    public PluginDescriptor getPlugin(@PathParam(PLUGIN_ID) final String pluginId) {
        return pluginStore.loadPlugins().getPlugin(pluginId);
    }


    @ApiOperation(
            value = "Return a list of changes made during installation of the plugin, based on installation parameters.",
            notes = "Messages only indicate what might change, operations could be skipped, "
                  + "e.g. file copy is not executed if file already exists.",
            response = List.class)
    @ApiParam(name = PLUGIN_ID, value = "Plugin ID", required = true)
    @GET
    @Path("/{" + PLUGIN_ID + "}/changes")
    public List<ChangeMessage> getInstructionPackageChanges(@PathParam(PLUGIN_ID) final String pluginId,
                                                            @Context final UriInfo uriInfo,
                                                            @Context HttpServletResponse response) throws Exception {
        final PluginDescriptor plugin = pluginStore.loadPlugins().getPlugin(pluginId);
        if (plugin == null) {
            response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
            return Collections.emptyList();
        }

        final DefaultInstructionPackage instructionPackage = installService.makeInstructionPackageInstance(plugin);
        if (instructionPackage == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return Collections.emptyList();
        }

        final Map<String, Object> parameters = extractInstallationParametersFromQueryParameters(uriInfo.getQueryParameters());
        return instructionPackage.getInstructionsMessages(parameters);
    }


    @ApiOperation(
            value = "Install a plugin into the project.",
            response = UserFeedback.class)
    @ApiParam(name = PLUGIN_ID, value = "Plugin ID", required = true)
    @POST
    @Path("/{" + PLUGIN_ID + "}/install")
    public synchronized UserFeedback installPlugin(@PathParam(PLUGIN_ID) String pluginId,
                                                   @Context HttpServletResponse response) {
        final PluginSet pluginSet = pluginStore.loadPlugins();
        final Map<String, Object> parameters = createDefaultInstallationParameters();
        final UserFeedback feedback = new UserFeedback();

        parameters.put(EssentialConst.PROP_PLUGIN_DESCRIPTOR, pluginSet.getPlugin(pluginId));
        if (!installStateMachine.tryBoarding(pluginId, pluginSet, parameters, feedback)) {
            response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
        }

        return feedback;
    }


    @ApiOperation(
            value = "Confirm the installation of a plugin with an explicit set of installation parameters.",
            notes = "Only used if the global settings instruct Essentials to have the installation parameters confirmed.",
            response = UserFeedback.class)
    @ApiParam(name = PLUGIN_ID, value = "Plugin ID", required = true)
    @POST
    @Path("/{" + PLUGIN_ID + "}/setup")
    public UserFeedback setupPluginGeneralized(@PathParam(PLUGIN_ID) final String pluginId,
                                               final Map<String, Object> parameters,
                                               @Context HttpServletResponse response) {
        final PluginSet pluginSet = pluginStore.loadPlugins();
        final UserFeedback feedback = new UserFeedback();
        ensureGenericInstallationParameters(parameters);
        parameters.put(EssentialConst.PROP_PLUGIN_DESCRIPTOR, pluginSet.getPlugin(pluginId));

        if (!installStateMachine.tryInstallation(pluginId, pluginSet, parameters, feedback)) {
            response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
        }

        return feedback;
    }


    @ApiOperation(
            value = "Signal a restart of Essentials, so the installation of plugins can continue.",
            response = UserFeedback.class
    )
    @POST
    @Path("/autosetup")
    public UserFeedback autoSetupPlugins(@Context ServletContext servletContext) {
        final UserFeedback feedback = new UserFeedback();
        // We lock the ping to avoid concurrent auto-setup. Concurrent auto-setup may happen
        // if the user(s) has/have two browsers pointed at the restarting Essentials WAR. Not
        // locking would lead to duplicate setup/bootstrapping.
        if (!setupLock.tryLock()) {
            log.warn("WARNING: You appear to be using two dashboards at the same time. Essentials doesn't support that." +
                    " Check if you have multiple tabs open, pointing at Essentials, and if so, close all except for one.");
            setupLock.lock();
        }
        try {
            final PluginSet pluginSet = pluginStore.loadPlugins();
            final Map<String, Object> parameters = createDefaultInstallationParameters();
            installStateMachine.signalRestart(pluginSet, parameters, feedback);
            DashboardUtils.setInitialized(true);
        } finally {
            setupLock.unlock();
        }
        return feedback;
    }


    @ApiOperation(
            value = "Clears plugin cache",
            notes = "Remote Plugin descriptors are cached for 1 hour. Trigger this endpoint manually to flush the cache earlier.",
            response = UserFeedback.class)
    @GET
    @Path("/clearcache")
    public UserFeedback clearCache(@Context ServletContext servletContext) {
        pluginStore.clearCache();
        return new UserFeedback().addSuccess("Plugin Cache invalidated");
    }


    @ApiOperation(
            value = "Fetch list of to-be-loaded Javascript resources",
            response = ApplicationData.class)
    @GET
    @Path("/modules")
    public ApplicationData getModules() {
        final ApplicationData applicationData = new ApplicationData();
        pluginStore.loadPlugins().getPlugins().forEach(applicationData::addFiles);
        return applicationData;
    }


    private Map<String, Object> createDefaultInstallationParameters() {
        return ensureGenericInstallationParameters(new HashMap<>());
    }

    private Map<String, Object> extractInstallationParametersFromQueryParameters(final MultivaluedMap<String, String> queryParams) {
        final Map<String, Object> parameters = new HashMap<>();

        for (String key : queryParams.keySet()) {
            final String value = queryParams.getFirst(key);
            if ("true".equals(value) || "false".equals(value)) {
                parameters.put(key, Boolean.valueOf(value));
            } else {
                parameters.put(key, value);
            }
        }

        return ensureGenericInstallationParameters(parameters);
    }

    /**
     * Make sure that the generic setup parameters are set.
     */
    private Map<String, Object> ensureGenericInstallationParameters(final Map<String, Object> parameters) {
        final ProjectSettings settings = settingsService.getSettings();

        final Object templateName = parameters.get(EssentialConst.PROP_TEMPLATE_NAME);
        final String templateLanguage = (templateName instanceof String)
                ? (String) templateName
                : settings.getTemplateLanguage();
        parameters.put(templateLanguage, true);

        final Object sampleData = parameters.get(EssentialConst.PROP_SAMPLE_DATA);
        if (!(sampleData instanceof Boolean)) {
            parameters.put(EssentialConst.PROP_SAMPLE_DATA, settings.isUseSamples());
        }

        final Object extraTemplates = parameters.get(EssentialConst.PROP_EXTRA_TEMPLATES);
        if (!(extraTemplates instanceof Boolean)) {
            parameters.put(EssentialConst.PROP_EXTRA_TEMPLATES, settings.isExtraTemplates());
        }

        return parameters;
    }
}
