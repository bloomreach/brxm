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

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Strings;

import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.model.ProjectSettings;
import org.onehippo.cms7.essentials.dashboard.model.UserFeedback;
import org.onehippo.cms7.essentials.dashboard.services.SettingsServiceImpl;
import org.onehippo.cms7.essentials.plugin.PluginStore;
import org.onehippo.cms7.essentials.rest.model.StatusRestful;
import org.onehippo.cms7.essentials.rest.model.SystemInfo;
import org.onehippo.cms7.essentials.utils.DashboardUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@CrossOriginResourceSharing(allowAllOrigins = true)
@Api(
        value = "/project",
        description = "Rest resource which provides information about the project.")
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
@Path("/project")
public class ProjectResource {

    @Inject private SettingsServiceImpl settingsService;
    @Inject private PluginStore pluginStore;


    @ApiOperation(
            value = "Retrieve project status",
            notes = "Status contains true value if one of the InstructionPackage is installed",
            response = StatusRestful.class)
    @GET
    @Path("/status")
    public StatusRestful getProjectStatus() {
        final StatusRestful status = new StatusRestful();
        final ProjectSettingsBean settings = settingsService.getModifiableSettings();
        if (settings != null && settings.getSetupDone()) {
            status.setProjectInitialized(true);
        }
        status.setPluginsInstalled(pluginStore.countInstalledPlugins());
        return status;
    }


    @ApiOperation(
            value = "Ping, returns true if application is initialized",
            response = SystemInfo.class)
    @GET
    @Path("/ping")
    public SystemInfo ping() {
        final SystemInfo systemInfo = new SystemInfo();

        // tell the pinger when to (re-)initialize the front-end.
        systemInfo.setInitialized(DashboardUtils.isInitialized());
        pluginStore.populateSystemInfo(systemInfo);

        return systemInfo;
    }


    @ApiOperation(
            value = "Retrieve project settings",
            notes = "[API] Project settings are global to the project and typically chosen once when the project is initialized.",
            response = ProjectSettings.class)
    @GET
    @Path("/settings")
    public ProjectSettings getProjectSettings(@Context HttpServletResponse response) {
        final ProjectSettings settings = settingsService.getSettings();
        if (settings == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return settings;
    }


    @ApiOperation(
            value = "Save global project settings",
            response = UserFeedback.class)
    @POST
    @Path("/settings")
    public UserFeedback saveProjectSettings(final ProjectSettingsBean projectSettings, @Context HttpServletResponse response) {
        // Remove empty plugin repository entries
        final Set<String> pluginRepositories = projectSettings.getPluginRepositories();
        final Set<String> validatedRepositories = new HashSet<>();
        if (pluginRepositories != null) {
            for (String repo : pluginRepositories) {
                if (!Strings.isNullOrEmpty(repo)) {
                    validatedRepositories.add(repo);
                }
            }
        }
        projectSettings.setPluginRepositories(validatedRepositories);
        projectSettings.setSetupDone(true);

        if (!settingsService.updateSettings(projectSettings)) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new UserFeedback().addError("Failed saving project settings. See back-end logs for more details.");
        }

        return new UserFeedback().addSuccess("Project settings saved.");
    }
}
