/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.codehaus.jackson.map.ObjectMapper;
import org.onehippo.cms7.essentials.dashboard.config.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.ctx.DefaultPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.model.EssentialsDependency;
import org.onehippo.cms7.essentials.dashboard.model.Plugin;
import org.onehippo.cms7.essentials.dashboard.model.PluginRestful;
import org.onehippo.cms7.essentials.dashboard.setup.ProjectSetupPlugin;
import org.onehippo.cms7.essentials.dashboard.utils.DependencyUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;

/**
 * @version "$Id$"
 */
public class BaseResource {

    private static Logger log = LoggerFactory.getLogger(BaseResource.class);

    @Inject
    private AutowireCapableBeanFactory injector;

    private ApplicationContext applicationContext;


    protected boolean isInstalled(final Plugin plugin) {
        final List<EssentialsDependency> dependencies = plugin.getDependencies();
        for (EssentialsDependency dependency : dependencies) {
            if (!DependencyUtils.hasDependency(dependency)) {
                return false;
            }
        }
        return true;
    }


    protected ProjectRestful getProjectRestful() {
        final PluginContext context = new DefaultPluginContext(null);
        final ProjectRestful projectRestful = new ProjectRestful();
        // inject project settings:
        try (final PluginConfigService configService = context.getConfigService()) {

            final ProjectSettingsBean document = configService.read(ProjectSetupPlugin.class.getName(), ProjectSettingsBean.class);

            if (document != null) {
                projectRestful.setNamespace(document.getProjectNamespace());
            }


        } catch (Exception e) {
            log.error("Error reading project settings", e);
        }
        return projectRestful;
    }

    protected List<PluginRestful> getPlugins(final ServletContext servletContext) {
        final InputStream stream = getClass().getResourceAsStream("/plugin_descriptor.json");
        final String json = GlobalUtils.readStreamAsText(stream);
        final ObjectMapper mapper = new ObjectMapper();
        try {

            @SuppressWarnings("unchecked")
            final RestfulList<PluginRestful> restfulList = mapper.readValue(json, RestfulList.class);
            return restfulList.getItems();
        } catch (IOException e) {
            log.error("Error parsing plugins", e);
        }
        return Collections.emptyList();
    }

    public PluginContext getContext(ServletContext servletContext) {
        final String className = ProjectSetupPlugin.class.getName();
        final PluginContext context = new DefaultPluginContext(new PluginRestful(className));

        try (final PluginConfigService service = context.getConfigService()) {
            final ProjectSettingsBean document = service.read(className, ProjectSettingsBean.class);
            if (document != null) {
                context.setBeansPackageName(document.getSelectedBeansPackage());
                context.setComponentsPackageName(document.getSelectedComponentsPackage());
                context.setRestPackageName(document.getSelectedRestPackage());
                context.setProjectNamespacePrefix(document.getProjectNamespace());
            }
        } catch (Exception e) {
            log.error("Error getting context", e);
        }
        return context;
    }


    protected void addRestartInformation(final EventBus eventBus) {
        eventBus.post(new DisplayEvent(DisplayEvent.DisplayType.BR.name(), DisplayEvent.DisplayType.BR, true));


        String filePath = ProjectUtils.getBaseProjectDirectory() + "/target/tomcat6x/logs/hippo-setup.log";

        eventBus.post(new DisplayEvent(new File(filePath).getAbsolutePath(), DisplayEvent.DisplayType.A, true));
        eventBus.post(new DisplayEvent(DisplayEvent.DisplayType.BR.name(), DisplayEvent.DisplayType.BR, true));
        eventBus.post(new DisplayEvent("Below, you can see an overview of what has been installed, this overview is also saved at:", DisplayEvent.DisplayType.P, true));
        eventBus.post(new DisplayEvent(DisplayEvent.DisplayType.BR.name(), DisplayEvent.DisplayType.BR, true));

        eventBus.post(new DisplayEvent(DisplayEvent.DisplayType.BR.name(), DisplayEvent.DisplayType.BR, true));
        eventBus.post(new DisplayEvent("http://www.onehippo.org/7_8/trails/essentials-trail/hippo-developer-essentials-and-power-packs", DisplayEvent.DisplayType.A, true));

        eventBus.post(new DisplayEvent(DisplayEvent.DisplayType.BR.name(), DisplayEvent.DisplayType.BR, true));
        eventBus.post(new DisplayEvent("After that, you are all set to start customizing your application. For more information, also see: ", DisplayEvent.DisplayType.P, true));
        eventBus.post(new DisplayEvent(DisplayEvent.DisplayType.BR.name(), DisplayEvent.DisplayType.BR, true));


        // add documentation messages:
        eventBus.post(new DisplayEvent(
                "mvn clean package\n" +
                        "mvn -P cargo.run\n", DisplayEvent.DisplayType.PRE, true
        ));
        //eventBus.post(new DisplayEvent("Please rebuild and restart your application:", DisplayEvent.DisplayType.STRONG, true));

        eventBus.post(new DisplayEvent("The installation of the Power Pack was successfully completed. To view the changes reflected in the CMS and site, first stop Tomcat and then rebuild and restart your project using following command:", DisplayEvent.DisplayType.P, true));
    }


    protected Plugin getPluginById(final String id, final ServletContext context) {
        if (Strings.isNullOrEmpty(id)) {
            return null;
        }
        final List<PluginRestful> plugins = getPlugins(context);
        for (final Plugin next : plugins) {
            final String pluginId = next.getPluginId();
            if (Strings.isNullOrEmpty(pluginId)) {
                continue;
            }
            if (pluginId.equals(id)) {
                return next;
            }
        }
        return null;
    }

    public AutowireCapableBeanFactory getInjector() {
        if (injector == null) {
            if (applicationContext == null) {
                applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
            }
            injector = applicationContext.getAutowireCapableBeanFactory();
        }
        return injector;

    }
}
