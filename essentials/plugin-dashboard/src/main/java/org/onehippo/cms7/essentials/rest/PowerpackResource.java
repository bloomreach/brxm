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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.dashboard.config.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.ctx.DefaultPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.event.listeners.MemoryPluginEventListener;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.packaging.PowerpackPackage;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.ProjectRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.setup.ProjectSetupPlugin;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.onehippo.cms7.essentials.powerpack.BasicPowerpack;
import org.onehippo.cms7.essentials.powerpack.BasicPowerpackWithSamples;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.rest.model.PowerpackListRestful;
import org.onehippo.cms7.essentials.rest.model.PowerpackRestful;
import org.onehippo.cms7.essentials.rest.model.RestList;
import org.onehippo.cms7.essentials.rest.model.StepRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;


/**
 * @version "$Id$"
 */
@Component
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/powerpacks/")
public class PowerpackResource extends BaseResource {

    public static final String POWERPACK_NEWS_AND_EVENT_LABEL = "news-events";
    @Inject
    private EventBus eventBus;
    private static Logger log = LoggerFactory.getLogger(PowerpackResource.class);


    @Inject
    private AutowireCapableBeanFactory injector;

    @Inject
    private MemoryPluginEventListener listener;

    @GET
    @Path("/install/{id}/{sample}")
    public RestfulList<MessageRestful> installPowerpack(@Context ServletContext servletContext, @PathParam("id") String id, @PathParam("sample") final boolean sampleContent) {
        final RestfulList<MessageRestful> messageRestfulRestfulList = new RestList<>();
        if (Strings.isNullOrEmpty(id)) {
            return messageRestfulRestfulList;
        }
        final PowerpackPackage powerpackPackage;
        switch (id) {
            case POWERPACK_NEWS_AND_EVENT_LABEL:
                if (sampleContent) {
                    powerpackPackage = new BasicPowerpackWithSamples();
                } else {
                    powerpackPackage = new BasicPowerpack();
                }
                break;
            default:
                powerpackPackage = new EmptyPowerPack();
                break;
        }
        injector.autowireBean(powerpackPackage);
        final String className = ProjectSetupPlugin.class.getName();
        final PluginContext context = new DefaultPluginContext(GlobalUtils.createSession(), getPluginByClassName(className, servletContext));
        // inject project settings:
        final PluginConfigService service = context.getConfigService();

        final ProjectSettingsBean document = service.read(className, ProjectSettingsBean.class);
        if (document != null) {
            context.setBeansPackageName(document.getSelectedBeansPackage());
            context.setComponentsPackageName(document.getSelectedComponentsPackage());
            context.setRestPackageName(document.getSelectedRestPackage());
            context.setProjectNamespacePrefix(document.getProjectNamespace());
        }


        final InstructionStatus status = powerpackPackage.execute(context);
        log.info("status {}", status);
        // save status:
        if (document != null) {
            document.setSetupDone(true);
            final boolean written = service.write(document, className);
            log.info("Config saved: {}", written);
        }
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
                        "mvn -P cargo.run\n", DisplayEvent.DisplayType.PRE, true));
        //eventBus.post(new DisplayEvent("Please rebuild and restart your application:", DisplayEvent.DisplayType.STRONG, true));

        eventBus.post(new DisplayEvent("The installation of the powerpack was successfully completed. To view the changes reflected in the CMS and site, rebuild and restart your project by using following command:", DisplayEvent.DisplayType.P, true));
        final List<DisplayEvent> displayEvents = listener.consumeEvents();
        for (DisplayEvent displayEvent : displayEvents) {
            messageRestfulRestfulList.add(new MessageRestful(displayEvent.getMessage(), displayEvent.getDisplayType()));
        }
        return messageRestfulRestfulList;
    }

    @GET
    @Path("/")
    public PowerpackListRestful getPowerpacks(@Context ServletContext servletContext) {
        final PowerpackListRestful powerpacks = new PowerpackListRestful();
        final ProjectRestful projectRestful = getProjectRestful();

        final String className = ProjectSetupPlugin.class.getName();
        final PluginContext context = new DefaultPluginContext(GlobalUtils.createSession(), getPluginByClassName(className, servletContext));
        // inject project settings:
        final PluginConfigService service = context.getConfigService();

        final ProjectSettingsBean document = service.read(className, ProjectSettingsBean.class);
        if (document != null) {
            context.setBeansPackageName(document.getSelectedBeansPackage());
            context.setComponentsPackageName(document.getSelectedComponentsPackage());
            context.setRestPackageName(document.getSelectedRestPackage());
            context.setProjectNamespacePrefix(document.getProjectNamespace());
        }


        projectRestful.setNamespace(context.getProjectNamespacePrefix());
        powerpacks.setProject(projectRestful);
        // add steps:
        // TODO this should be loaded dynamically, but we still need a mechanism for powerpacks
        final StepRestful stepOne = new StepRestful();
        stepOne.setName("Select a powerpack");
        stepOne.setButtonText("Next");
        powerpacks.addStep(stepOne);
        // two:
        final StepRestful stepTwo = new StepRestful();
        stepTwo.setName("Install");
        stepTwo.setButtonText("Finish");
        powerpacks.addStep(stepTwo);

        // TODO make dynamic (read from packages)
        final PowerpackRestful pack = new PowerpackRestful();
        pack.setName("Basic News and Events site");
        pack.setValue("news-events");
        pack.setEnabled(true);
        // add dummy packs:
        final PowerpackRestful dummy1 = new PowerpackRestful();
        dummy1.setName("Headless REST based content repository.");
        dummy1.setValue("empty-rest");
        dummy1.setEnabled(false);


        // add packs
        powerpacks.addPowerpack(pack);
        powerpacks.addPowerpack(dummy1);

        return powerpacks;
    }


    private static class EmptyPowerPack implements PowerpackPackage {
        @Override
        public Instructions getInstructions() {
            return new Instructions() {
                @Override
                public int totalInstructions() {
                    return 0;
                }

                @Override
                public int totalInstructionSets() {
                    return 0;
                }

                @Override
                public Set<InstructionSet> getInstructionSets() {
                    return Collections.emptySet();
                }

                @Override
                public void setInstructionSets(Set<InstructionSet> instructionSets) {
                }
            };
        }

        @Override
        public InstructionStatus execute(PluginContext context) {
            return InstructionStatus.SUCCESS;
        }
    }
}
