/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.beanwriter.rest;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.eventbus.EventBus;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
import org.onehippo.cms7.essentials.dashboard.event.RebuildEvent;
import org.onehippo.cms7.essentials.dashboard.model.UserFeedback;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.services.ContentBeansService;


/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("beanwriter/")
public class BeanWriterResource {

    @Inject private EventBus eventBus;
    @Inject private ContentBeansService contentBeansService;
    @Inject private PluginContextFactory contextFactory;

    @POST
    public UserFeedback runBeanWriter(final PostPayloadRestful payload, @Context ServletContext servletContext) throws Exception {
        final PluginContext context = contextFactory.getContext();
        final UserFeedback feedback = new UserFeedback();
        final Map<String, String> values = payload.getValues();
        final String imageSet = values.get("imageSet");

        contentBeansService.createBeans(context, feedback, imageSet);
        if ("true".equals(values.get("updateImageMethods"))) {
            contentBeansService.convertImageMethodsForClassname(imageSet, context, feedback);
        }
        contentBeansService.cleanupMethods(context, feedback);

        if (feedback.getFeedbackMessages().isEmpty()) {
            feedback.addSuccess("All beans were up to date");
        } else {
            final String message = "Hippo Beans are changed, project rebuild needed";
            eventBus.post(new RebuildEvent("beanwriter", message));
            feedback.addSuccess(message);
        }
        return feedback;
    }

    @GET
    @Path("/imagesets")
    public Set<String> getImageSets() throws Exception {
        final PluginContext context = contextFactory.getContext();
        return contentBeansService.getExistingImageTypes(context).keySet();
    }
}
