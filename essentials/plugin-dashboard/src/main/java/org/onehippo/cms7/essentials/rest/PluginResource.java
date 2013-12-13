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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.installer.InstallState;
import org.onehippo.cms7.essentials.dashboard.installer.InstallablePlugin;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.rest.model.MessageRestful;
import org.onehippo.cms7.essentials.rest.model.PluginRestful;
import org.onehippo.cms7.essentials.rest.model.PostPayloadRestful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/plugins/")
public class PluginResource extends BaseResource {

    @Inject
    private EventBus eventBus;
    private static Logger log = LoggerFactory.getLogger(PluginResource.class);


    @POST
    @Path("/configure/add")
    public RestfulList<PluginRestful> addToRecentlyInstalled(@Context ServletContext servletContext, final PostPayloadRestful payload) {

        log.info("servletContext {}");

        final RestfulList<PluginRestful> plugins = new RestfulList<>();
        final List<Plugin> pluginList = getPlugins(servletContext);
        for (Plugin p : pluginList) {

            final PluginRestful resource = new PluginRestful();

            resource.setTitle(p.getName());
            resource.setInstalled(checkInstalled(p));

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
            resource.setInstalled(checkInstalled(plugin));
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
            if (Strings.isNullOrEmpty(plugin.getPluginLink())) {
                log.error("Plugin has no pluginLink defined, please check plugin.xml file: {}", plugin);
                continue;
            }
            resource.setTitle(plugin.getName());
            resource.setPluginLink(plugin.getPluginLink());
            resource.setInstalled(checkInstalled(plugin));
            plugins.add(resource);
        }
        return plugins;
    }
    @GET
    @Path("/installstate/{className}")
    public PluginRestful getPluginList(@Context ServletContext servletContext, @PathParam("className") String className) {

        final PluginRestful resource = new PluginRestful();
        final List<Plugin> pluginList = getPlugins(servletContext);
        for (Plugin plugin : pluginList) {
            if(plugin.getPluginClass().equals(className)){
                if(Strings.isNullOrEmpty(plugin.getPluginLink())){
                    log.error("Plugin has no pluginLink defined, please check plugin.xml file: {}", plugin);
                    continue;
                }
                resource.setTitle(plugin.getName());
                resource.setPluginLink(plugin.getPluginLink());
                resource.setInstalled(checkInstalled(plugin));
                return resource;
            }


        }
        return resource;
    }

    @GET
    @Path("/install/{className}")
    public MessageRestful installPlugin(@Context ServletContext servletContext, @PathParam("className") String className) {

        final MessageRestful message = new MessageRestful();
        final List<Plugin> pluginList = getPlugins(servletContext);
        for (Plugin plugin : pluginList) {
            if(plugin.getPluginClass().equals(className)){
                final boolean installed = installPlugin(plugin);
                if(installed){
                    message.setValue("Plugin successfully installed");
                }
            }


        }
        message.setValue("Plugin could not be installed");
        return message;
    }
}
