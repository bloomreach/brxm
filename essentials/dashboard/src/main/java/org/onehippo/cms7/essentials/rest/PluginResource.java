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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import org.onehippo.cms7.essentials.utils.DashboardUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CrossOriginResourceSharing(allowAllOrigins = true)
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

    @Operation(
            summary = "Fetch list of all plugin descriptors. " +
                    "For all plugins with dynamic REST endpoints, these get registered at /dynamic/{pluginEndpoint}",
            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = List.class)))})
    @GET
    @Path("/")
    public List<PluginDescriptor> getAllPlugins() {
        return new ArrayList<>(pluginStore.loadPlugins().getPlugins());
    }


    @Operation(
            summary = "Fetch plugin descriptor for the specified plugin.",
            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = PluginDescriptor.class)))})
    @Parameter(name = PLUGIN_ID, description = "Plugin ID", required = true)
    @GET
    @Path("/{" + PLUGIN_ID + '}')
    public PluginDescriptor getPlugin(@PathParam(PLUGIN_ID) final String pluginId) {
        return pluginStore.loadPlugins().getPlugin(pluginId);
    }


    @Operation(
            summary = "Return a list of changes made during installation of the plugin, based on installation parameters.",
            description = "Messages only indicate what might change, operations could be skipped, "
                  + "e.g. file copy is not executed if file already exists.",
            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = List.class)))})
    @Parameter(name = PLUGIN_ID, description = "Plugin ID", required = true)
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


    @Operation(
            summary = "Install a plugin into the project.",
            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = UserFeedback.class)))})
    @Parameter(name = PLUGIN_ID, description = "Plugin ID", required = true)
    @POST
    @Path("/{" + PLUGIN_ID + "}/install")
    public synchronized UserFeedback installPlugin(@PathParam(PLUGIN_ID) String pluginId,
                                                   final Map<String, Object> parameters,
                                                   @Context HttpServletResponse response) {
        final PluginSet pluginSet = pluginStore.loadPlugins();
        final UserFeedback feedback = new UserFeedback();

        if (!parameters.isEmpty()) {
            installService.ensureGenericInstallationParameters(parameters);
            parameters.put(EssentialConst.PROP_PLUGIN_DESCRIPTOR, pluginSet.getPlugin(pluginId));
            if (!installStateMachine.installWithParameters(pluginId, pluginSet, parameters, feedback)) {
                response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
            }
        } else {
            final Map<String, Object> defaultParams = createDefaultInstallationParameters();
            defaultParams.put(EssentialConst.PROP_PLUGIN_DESCRIPTOR, pluginSet.getPlugin(pluginId));
            if (!installStateMachine.installWithDependencies(pluginId, pluginSet, defaultParams, feedback)) {
                response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
            }
        }

        return feedback;
    }


    @Operation(
            summary = "Signal a restart of Essentials, so the installation of plugins can continue.",
            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = UserFeedback.class)))}
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


    @Operation(
            summary = "Clears plugin cache",
            description = "Remote Plugin descriptors are cached for 1 hour. Trigger this endpoint manually to flush the cache earlier.",
            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = UserFeedback.class)))})
    @GET
    @Path("/clearcache")
    public UserFeedback clearCache(@Context ServletContext servletContext) {
        pluginStore.clearCache();
        return new UserFeedback().addSuccess("Plugin Cache invalidated");
    }


    @Operation(
            summary = "Fetch list of to-be-loaded Javascript resources",
            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = ApplicationData.class)))})
    @GET
    @Path("/modules")
    public ApplicationData getModules() {
        final ApplicationData applicationData = new ApplicationData();
        pluginStore.loadPlugins().getPlugins().forEach(applicationData::addFiles);
        return applicationData;
    }


    private Map<String, Object> createDefaultInstallationParameters() {
        return installService.ensureGenericInstallationParameters(new HashMap<>());
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

        return installService.ensureGenericInstallationParameters(parameters);
    }
}
