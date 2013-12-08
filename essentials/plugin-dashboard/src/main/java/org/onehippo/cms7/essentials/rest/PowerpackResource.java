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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.event.listeners.MemoryPluginEventListener;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.packaging.PowerpackPackage;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.powerpack.BasicPowerpack;
import org.onehippo.cms7.essentials.powerpack.BasicPowerpackWithSamples;
import org.onehippo.cms7.essentials.rest.model.MessageRestful;
import org.onehippo.cms7.essentials.rest.model.PowerpackListRestful;
import org.onehippo.cms7.essentials.rest.model.PowerpackRestful;
import org.onehippo.cms7.essentials.rest.model.ProjectRestful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;
import org.onehippo.cms7.essentials.rest.model.StepRestful;
import org.onehippo.cms7.essentials.setup.panels.SelectPowerpackStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
@Path("/powerpack/")
public class PowerpackResource extends BaseResource {

    @Inject
    private EventBus eventBus;
    private static Logger log = LoggerFactory.getLogger(PowerpackResource.class);

    @Inject
    private MemoryPluginEventListener listener;

    @GET
    @Path("/install/{id}/{sample}")
    public RestfulList<MessageRestful> installPowerpack(@PathParam("id") String id, @PathParam("sample") final boolean sampleContent) {
        final RestfulList<MessageRestful> messageRestfulRestfulList = new RestfulList<>();
        if (Strings.isNullOrEmpty(id)) {
            return messageRestfulRestfulList;
        }
        final PowerpackPackage powerpackPackage;
        switch (id) {
            case SelectPowerpackStep.POWERPACK_NEWS_AND_EVENT_LABEL:
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
        final InstructionStatus status = powerpackPackage.execute(new DashboardPluginContext(GlobalUtils.createSession(), null));
        switch (status) {
            case SUCCESS:
                eventBus.post(new DisplayEvent("Power Pack successfully installed"));
                break;
            case FAILED:
                eventBus.post("Not all parts of Power Pack were installed");

        }

        // add documentation messages:
        eventBus.post(new DisplayEvent("<h3>Please rebuild and restart your application:<h3>\n" +
                "<pre>\n" +
                "mvn clean package\n" +
                "mvn -P cargo.run\n" +
                "</pre>"));
        eventBus.post(new DisplayEvent("<p><a href=\"http://www.onehippo.org\">Read more about Hippo Essentials</a></p>"));
        final List<DisplayEvent> displayEvents = listener.consumeEvents();
        for (DisplayEvent displayEvent : displayEvents) {
            messageRestfulRestfulList.add(new MessageRestful(displayEvent.getMessage()));
        }
        return messageRestfulRestfulList;
    }

    @GET
    @Path("/")
    public PowerpackListRestful getPowerpacks() {
        final PowerpackListRestful powerpacks = new PowerpackListRestful();
        final ProjectRestful projectRestful = getProjectRestful();
        // TODO enable:
        projectRestful.setNamespace("marketplace");
        powerpacks.setProject(projectRestful);
        // add steps:
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
        dummy1.setName("A REST only site that contains only REST services and no pages.");
        dummy1.setValue("empty-rest");

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
