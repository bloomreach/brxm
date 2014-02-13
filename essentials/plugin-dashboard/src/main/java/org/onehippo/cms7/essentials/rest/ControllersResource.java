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

import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.rest.model.ControllerRestful;
import org.onehippo.cms7.essentials.rest.model.RestList;


import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/controllers/")
public class ControllersResource extends BaseResource {


    @GET
    @Path("/")
    public RestfulList<ControllerRestful> getControllers(@Context ServletContext servletContext) {

        final RestfulList<ControllerRestful> controllers = new RestList<>();
        final List<Plugin> plugins = getPlugins(servletContext);
        for (Plugin plugin : plugins) {
            final String pluginLink = plugin.getPluginLink();
            if (Strings.isNullOrEmpty(pluginLink)) {
                continue;
            }
            controllers.add(new ControllerRestful(pluginLink, String.format("%sCtrl", pluginLink), String.format("plugins/%s/index.html", pluginLink)));

        }
        // TODO load from remote

        return controllers;

    }
}
