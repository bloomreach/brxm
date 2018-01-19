/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.BooleanUtils;
import org.onehippo.cms7.essentials.plugin.sdk.services.ContentBeansService;
import org.onehippo.cms7.essentials.sdk.api.rest.UserFeedback;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.onehippo.cms7.essentials.sdk.api.service.RebuildService;


/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("beanwriter/")
public class BeanWriterResource {

    @Inject private RebuildService rebuildService;
    @Inject private JcrService jcrService;
    @Inject private ContentBeansService contentBeansService;

    @POST
    public UserFeedback runBeanWriter(final Map<String, Object> parameters) throws Exception {
        final UserFeedback feedback = new UserFeedback();
        final String imageSet = (String) parameters.get("imageSet");

        contentBeansService.createBeans(jcrService, feedback, imageSet);
        if (BooleanUtils.isTrue((Boolean) parameters.get("updateImageMethods"))) {
            contentBeansService.convertImageMethodsForClassname(imageSet, feedback);
        }
        contentBeansService.cleanupMethods(jcrService, feedback);

        if (feedback.getFeedbackMessages().isEmpty()) {
            feedback.addSuccess("All beans were up to date");
        } else {
            rebuildService.requestRebuild("beanwriter");
            feedback.addSuccess("Hippo Beans are changed, project rebuild needed");
        }
        return feedback;
    }

    @GET
    @Path("/imagesets")
    public Set<String> getImageSets() throws Exception {
        return contentBeansService.getExistingImageTypes().keySet();
    }
}
