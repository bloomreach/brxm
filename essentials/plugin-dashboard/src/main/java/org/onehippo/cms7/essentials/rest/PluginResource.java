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

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.dashboard.EssentialsPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.config.DefaultDocumentManager;
import org.onehippo.cms7.essentials.dashboard.config.DocumentManager;
import org.onehippo.cms7.essentials.dashboard.config.InstallerDocument;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.installer.InstallState;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.rest.client.RestClient;
import org.onehippo.cms7.essentials.rest.model.MessageRestful;
import org.onehippo.cms7.essentials.rest.model.PluginRestful;
import org.onehippo.cms7.essentials.rest.model.PostPayloadRestful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;
import org.onehippo.cms7.essentials.rest.model.tmp.Gist;
import org.onehippo.cms7.essentials.rest.model.tmp.GistFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

/**
 * Rest resource which provides information about plugins: e.g. installed or available plugins
 *
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/plugins/")
public class PluginResource extends BaseResource {

    public static final int WEEK_OLD = -7;
    @Inject
    private EventBus eventBus;
    private static Logger log = LoggerFactory.getLogger(PluginResource.class);

    /**
     * Fetches a (remote) service and checks for available Hippo Essentials plugins
     *
     * @param servletContext servlet context
     * @return JSON file of available plugins
     */
    @GET
    @Path("/")
    public RestfulList<PluginRestful> getPluginList(@Context ServletContext servletContext) {
        final RestfulList<PluginRestful> plugins = new RestfulList<>();


        List<PluginRestful> items = parseGist();
        // GIST may not be available (too many requests)
        if (items == null || items.size() == 0) {
            final InputStream stream = getClass().getResourceAsStream("/plugin_descriptor.json");
            final String json = GlobalUtils.readStreamAsText(stream);
            final Gson gson = new Gson();
            final Type listType = new TypeToken<RestfulList<PluginRestful>>() {
            }.getType();
            final RestfulList<PluginRestful> restfulList = gson.fromJson(json, listType);
            items = restfulList.getItems();

        }

        final DocumentManager manager = new DefaultDocumentManager(getContext(servletContext));
        for (PluginRestful item : items) {
            plugins.add(item);
            final String pluginClass = item.getPluginClass();
            final boolean hasClass = !Strings.isNullOrEmpty(pluginClass);
            if (!hasClass) {
                continue;
            }
            if (item.isNeedsInstallation()) {
                try {
                    @SuppressWarnings("unchecked")
                    final Class<EssentialsPlugin> clazz = (Class<EssentialsPlugin>) Class.forName(pluginClass);
                    final Constructor<EssentialsPlugin> constructor = clazz.getConstructor(Plugin.class, PluginContext.class);
                    final org.onehippo.cms7.essentials.dashboard.model.EssentialsPlugin dummy = new org.onehippo.cms7.essentials.dashboard.model.EssentialsPlugin();
                    final EssentialsPlugin instance = constructor.newInstance(dummy, new DashboardPluginContext(GlobalUtils.createSession(), dummy));
                    final InstallState installState = instance.getInstallState();
                    if (installState == InstallState.INSTALLED_AND_RESTARTED) {
                        item.setNeedsInstallation(false);
                    }
                } catch (Exception e) {
                    log.error("Error checking install state", e);
                }

            }
            // check if recently installed:
            // TODO: move to client?
            final InstallerDocument document = manager.fetchDocument(GlobalUtils.getFullConfigPath(pluginClass), InstallerDocument.class);
            if (document != null && document.getDateInstalled() != null) {
                final Calendar dateInstalled = document.getDateInstalled();
                final Calendar lastWeek = Calendar.getInstance();
                lastWeek.add(Calendar.DAY_OF_MONTH, WEEK_OLD);
                if (dateInstalled.after(lastWeek)) {
                    item.setDateInstalled(dateInstalled);
                }
            }

        }
        return plugins;
    }

    public static List<PluginRestful> parseGist() {
        try {
            final RestClient client = new RestClient("https://api.github.com/gists/8453217");
            final String pluginList = client.getPluginList();
            final Gson gson = new Gson();
            final Gist gist = gson.fromJson(pluginList, Gist.class);
            final Map<String, GistFile> files = gist.getFiles();
            final GistFile gistFile = files.get("gistfile1.json");
            final String json = gistFile.getContent();

            final Type listType = new TypeToken<RestfulList<PluginRestful>>() {
            }.getType();
            final RestfulList<PluginRestful> restfulList = gson.fromJson(json, listType);
            return restfulList.getItems();
        } catch (Exception e) {
            log.error("Error parsing gist", e);
        }
        return Collections.emptyList();

    }


    @POST
    @Path("/configure/add")
    public RestfulList<PluginRestful> addToRecentlyInstalled(@Context ServletContext servletContext, final PostPayloadRestful payload) {

        final RestfulList<PluginRestful> plugins = new RestfulList<>();
        final List<Plugin> pluginList = getPlugins(servletContext);
        for (Plugin p : pluginList) {

            final PluginRestful resource = new PluginRestful();

            resource.setTitle(p.getName());
            resource.setInstalled(checkInstalled(p));
            // TODO save this list
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
    @Path("/installstate/{className}")
    public PluginRestful getPluginList(@Context ServletContext servletContext, @PathParam("className") String className) {

        final PluginRestful resource = new PluginRestful();
        final List<Plugin> pluginList = getPlugins(servletContext);
        for (Plugin plugin : pluginList) {
            if (plugin.getPluginClass().equals(className)) {
                if (Strings.isNullOrEmpty(plugin.getPluginLink())) {
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

    @POST
    @Path("/install/{className}")
    public MessageRestful installPlugin(@Context ServletContext servletContext, @PathParam("className") String className) {

        final MessageRestful message = new MessageRestful();
        final RestfulList<PluginRestful> pluginList = getPluginList(servletContext);
        for (PluginRestful plugin : pluginList.getItems()) {
            final String pluginClass = plugin.getPluginClass();
            if (Strings.isNullOrEmpty(pluginClass)) {
                continue;
            }
            if (pluginClass.equals(className)) {
                final Plugin p = new org.onehippo.cms7.essentials.dashboard.model.EssentialsPlugin();
                p.setDescription(plugin.getIntroduction());
                p.setPluginClass(pluginClass);
                if (checkInstalled(p)) {
                    message.setValue("Plugin was already installed. Please rebuild and restart your application");
                    return message;
                }


                final boolean installed = installPlugin(p);
                if (installed) {
                    final DocumentManager manager = new DefaultDocumentManager(getContext(servletContext));
                    final InstallerDocument document = new InstallerDocument();
                    document.setParentPath(GlobalUtils.getParentConfigPath(pluginClass));
                    document.setName(GlobalUtils.getClassName(pluginClass));
                    document.setDateInstalled(Calendar.getInstance());
                    document.setPluginClass(pluginClass);
                    manager.saveDocument(document);
                    message.setValue("Plugin successfully installed. Please rebuild and restart your application");
                    return message;
                }
            }
        }

        message.setSuccessMessage(false);
        message.setValue("Plugin was not found and could not be installed");
        return message;
    }
}
