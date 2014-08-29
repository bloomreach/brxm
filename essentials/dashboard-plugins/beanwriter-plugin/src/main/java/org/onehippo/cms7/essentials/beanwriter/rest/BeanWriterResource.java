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

import org.onehippo.cms7.essentials.dashboard.ctx.DefaultPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.RebuildEvent;
import org.onehippo.cms7.essentials.dashboard.model.PluginRestful;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.ErrorMessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.services.ContentBeansService;
import org.onehippo.cms7.essentials.dashboard.setup.ProjectSetupPlugin;
import org.onehippo.cms7.essentials.dashboard.utils.BeanWriterUtils;
import org.onehippo.cms7.essentials.dashboard.utils.JavaSourceUtils;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.HippoEssentialsGeneratedObject;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;


/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("beanwriter/")
public class BeanWriterResource extends BaseResource {


    @Inject
    private EventBus eventBus;

    @POST
    public RestfulList<MessageRestful> runBeanWriter(final PostPayloadRestful payload, @Context ServletContext servletContext) throws Exception {
        final String className = ProjectSetupPlugin.class.getName();
        final PluginContext context = new DefaultPluginContext(new PluginRestful(className));
        //############################################
        // USE SERVICES
        //############################################
        final RestfulList<MessageRestful> messages = new MyRestList();
        final Map<String, String> values = payload.getValues();
        final String imageSet = values.get("imageSet");
        final String updateImageMethods = values.get("updateImageMethods");
        final ContentBeansService contentBeansService = new ContentBeansService(context, eventBus);
        // check if we are using custom image set:
        java.nio.file.Path path = null;
        if (!Strings.isNullOrEmpty(imageSet) || !imageSet.equals(ContentBeansService.HIPPO_GALLERY_IMAGE_SET_BEAN)) {
            final Map<String, java.nio.file.Path> existingImageTypes = contentBeansService.getExistingImageTypes();
            path = existingImageTypes.get(imageSet);
            if (path != null) {
                context.addPluginContextData(ContentBeansService.CONTEXT_BEAN_IMAGE_SET, path);

            }
        }
        // create beans
        contentBeansService.createBeans();
        // check if we need to upgrade beans
        if (!Strings.isNullOrEmpty(imageSet) && !Strings.isNullOrEmpty(updateImageMethods) && updateImageMethods.equals("true")) {
            if(path !=null){
                final HippoEssentialsGeneratedObject annotation = JavaSourceUtils.getHippoGeneratedAnnotation(path);
                if (annotation == null || Strings.isNullOrEmpty(annotation.getInternalName())) {
                    messages.add(new ErrorMessageRestful("Could not find selected image set: " + imageSet));
                } else {
                    contentBeansService.convertImageMethods(annotation.getInternalName());
                }
            }
            else if(imageSet.equals(ContentBeansService.HIPPO_GALLERY_IMAGE_SET_BEAN)){
                contentBeansService.convertImageMethods(imageSet);
            }
        }
        BeanWriterUtils.populateBeanwriterMessages(context, messages);
        if (messages.getItems().size() == 0) {
            messages.add(new MessageRestful("All beans were up to date"));
        } else {
            final String message = "HST Beans changed, project rebuild needed";
            eventBus.post(new RebuildEvent("Beanwriter", "tool", message));
            messages.add(new MessageRestful(message));
        }
        return messages;
    }


    @GET
    @Path("/imagesets")
    public Set<String> getImageSets(@Context ServletContext servletContext) throws Exception {
        final String className = ProjectSetupPlugin.class.getName();
        final PluginContext context = new DefaultPluginContext(new PluginRestful(className));
        final ContentBeansService contentBeansService = new ContentBeansService(context, eventBus);
        final Map<String, java.nio.file.Path> existingImageTypes = contentBeansService.getExistingImageTypes();
        return existingImageTypes.keySet();
    }


}
