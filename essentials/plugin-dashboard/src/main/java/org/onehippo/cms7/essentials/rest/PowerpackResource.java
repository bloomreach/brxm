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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.rest.model.PluginRestful;
import org.onehippo.cms7.essentials.rest.model.PowerpackListRestful;
import org.onehippo.cms7.essentials.rest.model.PowerpackRestful;
import org.onehippo.cms7.essentials.rest.model.ProjectRestful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;
import org.onehippo.cms7.essentials.rest.model.StepRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
@Path("/powerpack/")
public class PowerpackResource extends BaseResource{

    @Inject
    private EventBus eventBus;
    private static Logger log = LoggerFactory.getLogger(PowerpackResource.class);


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
        powerpacks.addStep(stepOne);
        // two:
        final StepRestful stepTwo = new StepRestful();
        stepTwo.setName("Validate");
        powerpacks.addStep(stepTwo);

        // TODO make dynamic (read from packages)
        final PowerpackRestful pack = new PowerpackRestful();
        pack.setName("Basic News and Events site");
        pack.setEnabled(true);
        // add dummy packs:
        final PowerpackRestful dummy1 = new PowerpackRestful();
        dummy1.setName("A REST only site that contains only REST services and no pages.");

        // add packs
        powerpacks.addPowerpack(pack);
        powerpacks.addPowerpack(dummy1);

        return powerpacks;
    }
}
