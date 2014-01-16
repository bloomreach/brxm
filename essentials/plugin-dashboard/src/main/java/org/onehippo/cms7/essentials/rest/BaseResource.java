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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.servlet.ServletContext;

import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.config.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.installer.InstallState;
import org.onehippo.cms7.essentials.dashboard.installer.InstallablePlugin;
import org.onehippo.cms7.essentials.dashboard.setup.ProjectSetupPlugin;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.PluginScanner;
import org.onehippo.cms7.essentials.rest.model.ProjectRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public class BaseResource {

    private static Logger log = LoggerFactory.getLogger(BaseResource.class);

    @SuppressWarnings("InstanceofInterfaces")
    protected boolean installPlugin(final Plugin plugin) {

        final String pluginClass = plugin.getPluginClass();
        final DashboardPlugin dashboardPlugin = instantiatePlugin(plugin, new DashboardPluginContext(GlobalUtils.createSession(), plugin), pluginClass);
        if (dashboardPlugin instanceof InstallablePlugin) {
            final InstallablePlugin<?> installable = (InstallablePlugin<?>) dashboardPlugin;
            installable.getInstaller().install();
            return true;
        }
        return false;
    }

    @SuppressWarnings("InstanceofInterfaces")
    protected boolean checkInstalled(final Plugin plugin) {

        final String pluginClass = plugin.getPluginClass();
        final DashboardPlugin dashboardPlugin = instantiatePlugin(plugin, new DashboardPluginContext(GlobalUtils.createSession(), plugin), pluginClass);
        if (dashboardPlugin instanceof InstallablePlugin) {
            final InstallablePlugin<?> installable = (InstallablePlugin<?>) dashboardPlugin;
            final InstallState installState = installable.getInstallState();
            if (installState == InstallState.INSTALLED_AND_RESTARTED) {
                return true;
            }
        }
        return false;
    }

    public static DashboardPlugin instantiatePlugin(final Plugin plugin, final PluginContext context, final String pluginClass) {
        try {
            @SuppressWarnings("unchecked")
            final Class<DashboardPlugin> clazz = (Class<DashboardPlugin>) Class.forName(pluginClass);
            final Constructor<DashboardPlugin> constructor = clazz.getConstructor(String.class, Plugin.class, PluginContext.class);
            return constructor.newInstance("plugin", plugin, context);
        } catch (ClassNotFoundException e) {
            log.error("Couldn't find plugin class", e);
        } catch (InstantiationException e) {
            log.error("Error instantiating plugin", e);
        } catch (IllegalAccessException e) {
            log.error("Error instantiating plugin/access", e);
        } catch (NoSuchMethodException e) {
            log.error("Invalid constructor called", e);
        } catch (InvocationTargetException e) {
            log.error("Error constructing plugin", e);
        }
        return null;
    }

    protected ProjectRestful getProjectRestful(){
        final PluginContext context = new DashboardPluginContext(GlobalUtils.createSession(), null);
        // inject project settings:
        final ProjectSettingsBean document = context.getConfigService().read(ProjectSetupPlugin.class.getName());
        final ProjectRestful projectRestful = new ProjectRestful();
        if (document != null) {
            projectRestful.setNamespace(document.getProjectNamespace());
        }

        return projectRestful;



    }

    public PluginContext getContext(ServletContext servletContext) {
        final String className = ProjectSetupPlugin.class.getName();
        final PluginContext context = new DashboardPluginContext(GlobalUtils.createSession(), getPluginByClassName(className, servletContext));
        final PluginConfigService service = context.getConfigService();

        final ProjectSettingsBean document = service.read(className);
        if (document != null) {
            context.setBeansPackageName(document.getSelectedBeansPackage());
            context.setComponentsPackageName(document.getSelectedComponentsPackage());
            context.setRestPackageName(document.getSelectedRestPackage());
            context.setProjectNamespacePrefix(document.getProjectNamespace());
        }
        return context;
    }

    protected List<Plugin> getPlugins(final ServletContext context) {
        final String libPath = context.getRealPath("/WEB-INF/lib");
        log.debug("Scanning path for essentials: {}", libPath);
        final PluginScanner scanner = new PluginScanner();
        return scanner.scan(libPath);
    }


    protected Plugin getPluginByName(final String name, final ServletContext context) {
        final List<Plugin> plugins = getPlugins(context);
        for (final Plugin next : plugins) {
            if (next.getName().equals(name)) {
                return next;
            }
        }
        return null;
    }
    protected Plugin getPluginByClassName(final String name, final ServletContext context) {
        final List<Plugin> plugins = getPlugins(context);
        for (final Plugin next : plugins) {
            final String pluginClass = next.getPluginClass();
            if(Strings.isNullOrEmpty(pluginClass)){
                continue;
            }
            if (pluginClass.equals(name)) {
                return next;
            }
        }
        return null;
    }


}
