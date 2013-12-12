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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.utils.PluginScanner;
import org.onehippo.cms7.essentials.rest.model.PluginRestful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/plugins/")
public class PluginResource extends BaseResource{

    @Inject
    private EventBus eventBus;
    private static Logger log = LoggerFactory.getLogger(PluginResource.class);



    @POST
    @Path("/configure/add")
    public RestfulList<PluginRestful> addToRecentlyInstalled(@Context ServletContext servletContext, final PluginRestful plugin) {

        log.info("servletContext {}");

        final RestfulList<PluginRestful> plugins = new RestfulList<>();
        final List<Plugin> pluginList = getPlugins(servletContext);
        for (Plugin p : pluginList) {
            final PluginRestful resource = new PluginRestful();
            resource.setTitle(p.getName());
            plugins.add(resource);
        }
        return plugins;
    }
    @GET
    @Path("/configure/list")
    public RestfulList<PluginRestful> getRecentlyInstalled(@Context ServletContext servletContext) {

        final RestfulList<PluginRestful> plugins = new RestfulList<>();
        final List<Plugin> pluginList = getPlugins(servletContext);
        for (Plugin plugin : pluginList) {
            final PluginRestful resource = new PluginRestful();
            resource.setTitle(plugin.getName());
            resource.setPluginLink(plugin.getPluginLink());
            plugins.add(resource);
        }
        return plugins;
    }

    @GET
    @Path("/")
    public RestfulList<PluginRestful> getPluginList(@Context ServletContext servletContext) {


        final RestfulList<PluginRestful> plugins = new RestfulList<>();
        final List<Plugin> pluginList = getPlugins(servletContext);
        for (Plugin plugin : pluginList) {
            final PluginRestful resource = new PluginRestful();
            resource.setTitle(plugin.getName());
            plugins.add(resource);
        }
        return plugins;
    }
}
