/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.ws.WebServiceException;

import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.setup.ProjectSetupPlugin;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.rest.model.StatusRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

/**
 * @version "$Id$"
 */
@Path("/status/")
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
public class StatusResource extends BaseResource {

    @Inject
    private EventBus eventBus;
    private static Logger log = LoggerFactory.getLogger(StatusResource.class);


    @GET
    @Path("/powerpack")
    public StatusRestful getMenu(@Context ServletContext servletContext) {
        try {

            final Plugin plugin = getPluginByName("Settings", servletContext);
            final PluginContext context = new DashboardPluginContext(GlobalUtils.createSession(), plugin);
            ProjectSettingsBean b =new ProjectSettingsBean();
            b.setProjectNamespace("marktpla");
            context.getConfigService().write(b);
            final ProjectSettingsBean document = context.getConfigService().read(ProjectSetupPlugin.class.getName());
            final StatusRestful statusRestful = new StatusRestful();
            if (document != null && document.getSetupDone()) {
                statusRestful.setStatus(true);
                return statusRestful;
            }
            log.info("statusRestful {}", statusRestful);

            return statusRestful;
        } catch (Exception e) {


        }
        return null;
    }
}
