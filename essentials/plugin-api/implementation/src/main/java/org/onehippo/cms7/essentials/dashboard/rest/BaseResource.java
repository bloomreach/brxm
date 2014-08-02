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
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.onehippo.cms7.essentials.dashboard.config.FilePluginService;
import org.onehippo.cms7.essentials.dashboard.config.InstallerDocument;
import org.onehippo.cms7.essentials.dashboard.config.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.config.ResourcePluginService;
import org.onehippo.cms7.essentials.dashboard.ctx.DefaultPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.model.EssentialsDependency;
import org.onehippo.cms7.essentials.dashboard.model.Plugin;
import org.onehippo.cms7.essentials.dashboard.model.PluginRestful;
import org.onehippo.cms7.essentials.dashboard.model.Repository;
import org.onehippo.cms7.essentials.dashboard.packaging.InstructionPackage;
import org.onehippo.cms7.essentials.dashboard.packaging.TemplateSupportInstructionPackage;
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



    public MessageRestful createErrorMessage(final String message, final HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
        return new ErrorMessageRestful(message);
    }

    /**
     * Instantiate InstructionPackage for plugin.
     *
     * @param plugin Plugin instance
     * @return null if packageClass & packageFile are null or empty
     */
    protected InstructionPackage instructionPackageInstance(final Plugin plugin) {
        final String packageClass = plugin.getPackageClass();
        final String packageFile = plugin.getPackageFile();
        InstructionPackage instructionPackage;
        if (Strings.isNullOrEmpty(packageClass)) {
            if (Strings.isNullOrEmpty(packageFile)) {
                log.warn("Package class and Package file were not defined for plugin: {}/{}", plugin.getName(), plugin.getId());
                return null;
            }
            instructionPackage = GlobalUtils.newInstance(TemplateSupportInstructionPackage.class);
            instructionPackage.setInstructionPath(packageFile);
        } else {
            instructionPackage = GlobalUtils.newInstance(packageClass);
        }
        getInjector().autowireBean(instructionPackage);
        return instructionPackage;
    }

    /**
     * This function determines by the fact that all dependencies and repositories specified in a plugin's descriptor
     * are present in the relevant POM files if the plugin was packaged with Essentials, or pulled-in from a marketplace.
     *
     * The return value of this function is only valid while the plugin is in its "discovered" installation state.
     *
     * @param plugin the plugin under consideration
     * @return true if the plugin was packages with Essentials, false otherwise.
     */
    protected boolean isPackaged(final Plugin plugin) {
        final List<EssentialsDependency> dependencies = plugin.getDependencies();
        for (EssentialsDependency dependency : dependencies) {
            if (!DependencyUtils.hasDependency(dependency)) {
                return false;
            }
        }
        final List<Repository>  repositories = plugin.getRepositories();
        for (Repository repository : repositories) {
            if (!DependencyUtils.hasRepository(repository)) {
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

            final ProjectSettingsBean document = configService.read(ProjectSettingsBean.DEFAULT_NAME, ProjectSettingsBean.class);
            if (document != null) {
                projectRestful.setNamespace(document.getProjectNamespace());
            }

        } catch (Exception e) {
            log.error("Error reading project settings", e);
        }
        return projectRestful;
    }

    protected void addRestartInformation(final EventBus eventBus) {
        eventBus.post(new DisplayEvent(DisplayEvent.DisplayType.BR.name(), DisplayEvent.DisplayType.BR, true));


        String filePath = ProjectUtils.getBaseProjectDirectory() + "/target/tomcat6x/logs/hippo-setup.log";

        eventBus.post(new DisplayEvent(new File(filePath).getAbsolutePath(), DisplayEvent.DisplayType.A, true));
        eventBus.post(new DisplayEvent(DisplayEvent.DisplayType.BR.name(), DisplayEvent.DisplayType.BR, true));
        eventBus.post(new DisplayEvent("Below, you can see an overview of what has been installed, this overview is also saved at:", DisplayEvent.DisplayType.P, true));
        eventBus.post(new DisplayEvent(DisplayEvent.DisplayType.BR.name(), DisplayEvent.DisplayType.BR, true));

        eventBus.post(new DisplayEvent(DisplayEvent.DisplayType.BR.name(), DisplayEvent.DisplayType.BR, true));
        eventBus.post(new DisplayEvent("http://www.onehippo.org/trails/essentials-trail/hippo-developer-essentials-and-power-packs", DisplayEvent.DisplayType.A, true));

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
