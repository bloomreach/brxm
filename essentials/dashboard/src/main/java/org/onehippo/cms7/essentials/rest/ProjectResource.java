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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
import org.onehippo.cms7.essentials.dashboard.model.ProjectSettings;
import org.onehippo.cms7.essentials.dashboard.rest.ErrorMessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.plugin.PluginStore;
import org.onehippo.cms7.essentials.project.ProjectUtils;
import org.onehippo.cms7.essentials.rest.model.RestList;
import org.onehippo.cms7.essentials.rest.model.StatusRestful;
import org.onehippo.cms7.essentials.rest.model.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@CrossOriginResourceSharing(allowAllOrigins = true)
@Api(
        value = "/project",
        description = "Rest resource which provides information about the project.")
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
@Path("/project")
public class ProjectResource {

    private static Logger log = LoggerFactory.getLogger(PluginResource.class);

    @Inject private PluginStore pluginStore;
    @Inject private PluginContextFactory contextFactory;


    @ApiOperation(
            value = "Retrieve project status",
            notes = "Status contains true value if one of the InstructionPackage is installed",
            response = StatusRestful.class)
    @GET
    @Path("/status")
    public StatusRestful getProjectStatus() {
        final StatusRestful status = new StatusRestful();
        final PluginContext context = contextFactory.getContext();
        final ProjectSettings settings = ProjectUtils.loadSettings(context);
        if (settings != null && settings.getSetupDone()) {
            status.setProjectInitialized(true);
        }
        status.setPluginsInstalled(pluginStore.countInstalledPlugins());
        return status;
    }


    @ApiOperation(
            value = "Ping, returns true if application is initialized",
            response = boolean.class)
    @GET
    @Path("/ping")
    public SystemInfo ping(@Context ServletContext servletContext) {
        final SystemInfo systemInfo = new SystemInfo();

        // tell the pinger when to (re-)initialize the front-end.
        systemInfo.setInitialized(ProjectUtils.isInitialized());
        pluginStore.populateSystemInfo(systemInfo);

        return systemInfo;
    }


    @ApiOperation(
            value = "Retrieve project settings",
            notes = "[API] Project settings are global to the project and typically chosen once when the project is initialized.",
            response = ProjectSettings.class)
    @GET
    @Path("/settings")
    public ProjectSettings getProjectSettings() {
        final PluginContext context = contextFactory.getContext();
        return ProjectUtils.loadSettings(context);
    }


    @ApiOperation(
            value = "Save global project settings",
            response = MessageRestful.class)
    @POST
    @Path("/settings")
    public MessageRestful saveProjectSettings(final ProjectSettingsBean projectSettings) {
        final PluginContext context = contextFactory.getContext();
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

        try {
            ProjectUtils.persistSettings(context, projectSettings);
        } catch (Exception e) {
            log.error("Error persisting project settings", e);
            return new ErrorMessageRestful("Error saving project settings");
        }

        return new MessageRestful("Project settings saved.");
    }


    @ApiOperation(
            value = "Return list of project coordinates like project namespace, project path etc.",
            notes = "[API]",
            response = RestfulList.class)
    @GET
    @Path("/coordinates")
    public RestfulList<KeyValueRestful> getProjectCoordinates() {
        final PluginContext context = contextFactory.getContext();
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
}
