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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.onehippo.cms7.essentials.dashboard.contentblocks.matcher.HasProviderMatcher;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.CndUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GalleryUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.onehippo.cms7.essentials.rest.model.KeyValueRestful;
import org.onehippo.cms7.essentials.rest.model.PluginRestful;
import org.onehippo.cms7.essentials.rest.model.PropertyRestful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;
import org.onehippo.cms7.essentials.rest.model.contentblocks.DocumentTypes;
import org.onehippo.cms7.essentials.rest.model.gallery.ImageProcessorRestful;
import org.onehippo.cms7.essentials.rest.model.gallery.ImageVariantRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/imagegallery/")
public class ImageGalleryResource extends BaseResource {

    @Inject
    private EventBus eventBus;
    private static Logger log = LoggerFactory.getLogger(ImageGalleryResource.class);

    private static final String GALLERY_PROCESSOR_SERVICE_PATH = "/hippo:configuration/hippo:frontend/cms/cms-services/galleryProcessorService";

    @GET
    @Path("/")
    public ImageProcessorRestful getImageProcessor() {

        final ImageProcessorRestful processorRestful = new ImageProcessorRestful();
        // TODO verify the use and creation of the plugin context
        final PluginContext pluginContext = getPluginContext();

        final Session session = pluginContext.getSession();
        try {
            final Node processorNode = session.getNode(GALLERY_PROCESSOR_SERVICE_PATH);
            processorRestful.setPath(processorNode.getPath());
            processorRestful.setClassName(processorNode.getProperty("plugin.class").getString());
            processorRestful.setId(processorNode.getProperty("gallery.processor.id").getString());

            final NodeIterator variantNodes = processorNode.getNodes();
            while(variantNodes.hasNext()) {
                final Node variantNode = variantNodes.nextNode();
                final ImageVariantRestful variantRestful = new ImageVariantRestful();
                variantRestful.setId(variantNode.getIdentifier());
                final String variantName = variantNode.getName();
                variantRestful.setNamespace(HippoNodeUtils.getPrefixFromType(variantName));
                variantRestful.setName(HippoNodeUtils.getNameFromType(variantName));
                if(variantNode.hasProperty("width")) {
                    variantRestful.setWidth((int)variantNode.getProperty("width").getLong());
                }
                if(variantNode.hasProperty("height")) {
                    variantRestful.setHeight((int) variantNode.getProperty("height").getLong());
                }
                if(variantNode.hasProperty("upscaling")) {
                    final PropertyRestful property = new PropertyRestful();
                    property.setName("upscaling");
                    property.setValue(variantNode.getProperty("upscaling").getString());
                    property.setType(PropertyRestful.PropertyType.BOOLEAN);
                    variantRestful.addProperty(property);
                }
                processorRestful.addVariant(variantRestful);
            }
        } catch (RepositoryException e) {
            log.error("Exception while trying to retrieve document types from repository {}", e);
        }
        return processorRestful;
    }

    private void test() {
        final PluginContext pluginContext = getPluginContext();
        for(final String imageSetType : listImageSetTypes(pluginContext)) {

        }
    }

    private void fetchImageSet(final Session session, final String type) {
        //GalleryUtils.
    }

    private List<String> listImageSetTypes(final PluginContext pluginContext) {
        try {
            return CndUtils.getNodeTypesOfType(pluginContext, HippoGalleryNodeType.IMAGE_SET, false);
        } catch (RepositoryException e) {
            log.warn("Unable to retrieve node types", e);
        }
        return Collections.emptyList();

    }

    private PluginContext getPluginContext() {
        return new DashboardPluginContext(GlobalUtils.createSession(), null);
    }


}
