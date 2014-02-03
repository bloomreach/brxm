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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;


import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.CndUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TranslationUtils;
import org.onehippo.cms7.essentials.rest.model.PropertyRestful;
import org.onehippo.cms7.essentials.rest.model.TranslationRestful;
import org.onehippo.cms7.essentials.rest.model.gallery.ImageProcessorRestful;
import org.onehippo.cms7.essentials.rest.model.gallery.ImageSetRestful;
import org.onehippo.cms7.essentials.rest.model.gallery.ImageSetsRestful;
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
    public List<ImageProcessorRestful> getImageProcessor() {
        final ImageProcessorRestful processorRestful = new ImageProcessorRestful();
        // TODO verify the use and creation of the plugin context
        final PluginContext pluginContext = getPluginContext();

        final Session session = pluginContext.getSession();

        try {
            final Node processorNode = session.getNode(GALLERY_PROCESSOR_SERVICE_PATH);
            processorRestful.setPath(processorNode.getPath());
            processorRestful.setClassName(processorNode.getProperty("plugin.class").getString());
            processorRestful.setId(processorNode.getProperty("gallery.processor.id").getString());

            final Map<String, ImageVariantRestful> variantMap = fetchImageProcessorVariants(session, processorNode);
            processorRestful.addVariants(variantMap.values());


            populateImageSetsInVariants(session, variantMap.values());


        } catch (RepositoryException e) {
            log.error("Exception while trying to retrieve document types from repository {}", e);
        }
        List<ImageProcessorRestful> processors = new ArrayList<>();
        processors.add(processorRestful);

        return processors;
    }

    @PUT
    @Path("/save")
    public ImageProcessorRestful saveImageProcessor(@Context HttpServletRequest request) {
        try {
            final JAXBContext context = JAXBContext.newInstance(ImageProcessorRestful.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            unmarshaller.setProperty(UnmarshallerProperties.MEDIA_TYPE, "application/json");
            //unmarshaller.setProperty(UnmarshallerProperties.JSON_INCLUDE_ROOT, false);
            final Object object = unmarshaller.unmarshal(request.getInputStream());
            if (object instanceof ImageProcessorRestful) {
                ImageProcessorRestful imageProcessor = (ImageProcessorRestful) object;

                final PluginContext pluginContext = getPluginContext();
                final Session session = pluginContext.getSession();
                try {
                    saveImageProcessor(session, imageProcessor);
                } catch (RepositoryException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    // TODO
                }
                return imageProcessor;
            }
        } catch (JAXBException | IOException e) {
            log.error("Error parsing JSON", e);
        }
        return null;
    }

    private boolean saveImageProcessor(final Session session, final ImageProcessorRestful imageProcessor) throws RepositoryException {
        final Node processorNode = session.getNode(imageProcessor.getPath());
        // Remove all old non used variants
        final NodeIterator variantIterator = processorNode.getNodes();
        while (variantIterator.hasNext()) {
            final Node variantNode = variantIterator.nextNode();
            // TODO deteremine whether to check on id or on namespace/name combination
            final ImageVariantRestful variant = imageProcessor.getVariant(variantNode.getIdentifier());
            if (variant == null) {
                // TODO or add to list of nodes to delete
                //variantNode.remove();
                log.info("Remove " + variantNode.getPath());
            }
        }
        // save all variants
        for (ImageVariantRestful variant : imageProcessor.getVariants()) {
            final Node variantNode;
            if (processorNode.hasNode(variant.getNodeType())) {
                variantNode = processorNode.getNode(variant.getNodeType());
            } else {
                variantNode = processorNode.addNode(variant.getNodeType(), "frontend:pluginconfig");
            }
            updateVariantNode(variantNode, variant);
        }
        // TODO check
        // TODO save translations
        //session.save();
        return true;
    }

    private void updateVariantNode(final Node variantNode, final ImageVariantRestful variant) throws RepositoryException {
        variantNode.setProperty("height", variant.getHeight());
        variantNode.setProperty("width", variant.getWidth());

        if (variant.getProperty("upscaling") == null) {
            removeProperty(variantNode, "upscaling");
        }
        // TODO other properties
        if (variant.getProperty("upscaling") == null) {
            removeProperty(variantNode, "upscaling");
        }
        if (variant.getProperty("upscaling") == null) {
            removeProperty(variantNode, "upscaling");
        }

        for (final PropertyRestful property : variant.getProperties()) {
            setProperty(variantNode, property);
        }
    }

    private void setProperty(final Node node, final PropertyRestful property) throws RepositoryException {
        switch (property.getType()) {
            case BOOLEAN:
                node.setProperty(property.getName(), Boolean.valueOf(property.getValue()).booleanValue());
            default:
                node.setProperty(property.getName(), property.getValue());
                break;
        }
    }

    private void removeProperty(final Node node, final String propertyName) throws RepositoryException {
        if (node.hasProperty(propertyName)) {
            final Property property = node.getProperty(propertyName);
            property.remove();
        }
    }

    private Map<String, ImageVariantRestful> fetchImageProcessorVariants(final Session session, final Node processorNode) throws RepositoryException {
        final Map<String, ImageVariantRestful> variants = new HashMap<>();
        final Map<String, Map<String, TranslationRestful>> variantTranslationsMap = getVariantTranslationsMap(session);

        final NodeIterator variantNodes = processorNode.getNodes();
        while (variantNodes.hasNext()) {
            final Node variantNode = variantNodes.nextNode();
            final ImageVariantRestful variantRestful = new ImageVariantRestful();
            variantRestful.setId(variantNode.getIdentifier());
            final String variantName = variantNode.getName();
            variantRestful.setNamespace(HippoNodeUtils.getPrefixFromType(variantName));
            variantRestful.setName(HippoNodeUtils.getNameFromType(variantName));
            if (variantNode.hasProperty("width")) {
                variantRestful.setWidth((int) variantNode.getProperty("width").getLong());
            }
            if (variantNode.hasProperty("height")) {
                variantRestful.setHeight((int) variantNode.getProperty("height").getLong());
            }
            if (variantNode.hasProperty("upscaling")) {
                final PropertyRestful property = new PropertyRestful();
                property.setName("upscaling");
                property.setValue(variantNode.getProperty("upscaling").getString());
                property.setType(PropertyRestful.PropertyType.BOOLEAN);
                variantRestful.addProperty(property);
            }
            if (variantTranslationsMap.get(variantName) != null) {
                log.info("Translations for " + variantName + ": " + variantTranslationsMap.get(variantName).size());
            } else {
                log.info("No translations for " + variantName);
            }
            variantRestful.addTranslations(variantTranslationsMap.get(variantName).values());

            for (String t : variantTranslationsMap.keySet()) {
                log.info("Map entry " + t);
            }

            variants.put(variantRestful.getNodeType(), variantRestful);
        }
        return variants;
    }

    private void fetchImageSet(final Session session, final String type) {
        //GalleryUtils.
    }

    private Map<String, Map<String, TranslationRestful>> getVariantTranslationsMap(final Session session) throws RepositoryException {
        final Map<String, Map<String, TranslationRestful>> map = new HashMap<>();
        for (final Node node : fetchVariantTranslations(session)) {
            final TranslationRestful translation = new TranslationRestful();
            translation.setLocale(TranslationUtils.getHippoLanguage(node));
            translation.setMessage(TranslationUtils.getHippoMessage(node));

            final String propertyName = TranslationUtils.getHippoProperty(node);
            if (Strings.isNullOrEmpty(propertyName)) {
                continue;
            }
            if (!map.containsKey(propertyName)) {
                map.put(propertyName, new HashMap<String, TranslationRestful>());
            }
            map.get(propertyName).put(translation.getLocale(), translation);
        }
        return map;
    }

    @PUT
    @Path("/imagesets/save")
    public ImageSetsRestful saveImageSets(@Context HttpServletRequest request) throws RepositoryException {
        try {
            final JAXBContext context = JAXBContext.newInstance(ImageProcessorRestful.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            unmarshaller.setProperty(UnmarshallerProperties.MEDIA_TYPE, "application/json");
            //unmarshaller.setProperty(UnmarshallerProperties.JSON_INCLUDE_ROOT, false);
            final Object object = unmarshaller.unmarshal(request.getInputStream());
            if (object instanceof ImageSetsRestful) {
                ImageSetsRestful imageSets = (ImageSetsRestful) object;

                final PluginContext pluginContext = getPluginContext();
                final Session session = pluginContext.getSession();
                for (final ImageSetRestful imageSet : imageSets.getImageSets()) {
                    try {
                        saveImageSet(session, imageSet);
                    } catch (RepositoryException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        // TODO
                    }
                }
                session.save();
                return imageSets;
            }
        } catch (JAXBException e) {
            log.error("Error parsing JSON", e);
        } catch (IOException e) {
            log.error("Error parsing JSON", e);
        }
        return null;
    }

    private boolean saveImageSet(final Session session, final ImageSetRestful imageSet) throws RepositoryException {
        final Node imageSetNode = session.getNode(imageSet.getPath());

        // Remove all old non used variants
        final List<Node> nodes = fetchFieldsFromNamespaceNode(imageSetNode, "hippogallery:image");
        for (final Node variantFieldNode : nodes) {
            //final String documentType = JcrUtils.getStringProperty(node, "hipposysedit:path", null);
            // TODO deteremine whether to check on id or on namespace/name combination
            final ImageVariantRestful variant = imageSet.getVariant(variantFieldNode.getIdentifier());
            if (variant == null) {
                // TODO or add to list of nodes to delete
                //variantFieldNode.remove();
                log.info("Remove " + variantFieldNode.getPath());
            }
            if (imageSetNode.hasNode("editor:templates/_default_/" + variant.getName())) {
                final Node templateNode = imageSetNode.getNode("editor:templates/_default_/" + variantFieldNode.getName());
                //templateNode.remove();
                log.info("Remove " + templateNode.getPath());
            }
        }

        // TODO register type
        // create fetch template
        // update fields and template nodes

        // TODO check
        // TODO save translations
        session.save();
        return true;
    }

    protected void storeImageSetTranslations(final Node imageSetNode, final ImageSetRestful imageSet) {
        // TODO
        // save variant translations as well??
    }


    @GET
    @Path("/imagesets/")
    public List<ImageSetRestful> fetchImageSets() throws RepositoryException {

        final ImageProcessorRestful processorRestful = new ImageProcessorRestful();
        // TODO verify the use and creation of the plugin context
        final PluginContext pluginContext = getPluginContext();

        final Session session = pluginContext.getSession();


        final Node processorNode = session.getNode(GALLERY_PROCESSOR_SERVICE_PATH);
        final List<ImageSetRestful> imageSets = fetchImageSets(session);
        populateVariantsInImageSets(session, imageSets, processorNode);

        return imageSets;
    }

    private void populateImageSetsInVariants(final Session session, final Collection<ImageVariantRestful> variants) throws RepositoryException {
        final List<ImageSetRestful> imageSets = fetchImageSets(session);
        for (final ImageVariantRestful variant : variants) {
            populateImageSetsInVariant(variant, imageSets);
        }
    }

    private void populateImageSetsInVariant(final ImageVariantRestful variant, final List<ImageSetRestful> availableImageSets) throws RepositoryException {
        final List<ImageSetRestful> imageSets = new ArrayList<>();
        for (final ImageSetRestful imageSet : availableImageSets) {
            if (imageSet.hasVariant(variant.getNamespace(), variant.getName())) {
                imageSets.add(imageSet);
            }
        }
        variant.setImageSets(imageSets);
    }


    private void populateVariantsInImageSets(final Session session, final List<ImageSetRestful> imageSets, final Node processorNode) throws RepositoryException {
        final Map<String, ImageVariantRestful> availableVariants = fetchImageProcessorVariants(session, processorNode);
        for (final ImageSetRestful imageSet : imageSets) {
            populateVariantsInImageSet(imageSet, availableVariants);
        }
    }

    private void populateVariantsInImageSet(ImageSetRestful imageSet, final Map<String, ImageVariantRestful> availableVariants) throws RepositoryException {
        final List<ImageVariantRestful> variants = new ArrayList<>();
        for (final ImageVariantRestful tempVariant : imageSet.getVariants()) {
            final ImageVariantRestful variant = availableVariants.get(tempVariant.getNodeType());
            variants.add(variant);
        }
        imageSet.setVariants(variants);
    }


    private List<ImageSetRestful> fetchImageSets(final Session session) throws RepositoryException {
        final List<ImageSetRestful> imageSets = new ArrayList<>();
        final PluginContext pluginContext = getPluginContext();
        final List<Node> nodes = fetchImageSetNamespaceNodes(session, listImageSetTypes(pluginContext));
        for (final Node node : nodes) {
            final ImageSetRestful imageSet = new ImageSetRestful();
            imageSet.setId(node.getIdentifier());
            imageSet.setPath(node.getPath());
            imageSet.setName(node.getName());
            imageSet.setNamespace(node.getParent().getName());
            imageSet.setVariants(getVariantsForImageSetNamespaceNode(node));
            imageSet.setTranslations(getImageSetTranslations(node));

            imageSets.add(imageSet);
        }
        return imageSets;
    }

    private List<TranslationRestful> getImageSetTranslations(final Node imageSet) throws RepositoryException {
        final List<TranslationRestful> translations = new ArrayList<>();

        for (final Node node : TranslationUtils.getTranslationsFromNode(imageSet)) {
            final TranslationRestful translation = new TranslationRestful();
            translation.setLocale(TranslationUtils.getHippoLanguage(node));
            translation.setMessage(TranslationUtils.getHippoMessage(node));

            final String propertyName = TranslationUtils.getHippoProperty(node);
            if (!StringUtils.isBlank(propertyName)) {
                log.info("Skipping translation: " + node.getPath());
                continue;
            } else {
                log.info("Adding translation: " + node.getPath());
            }
            translations.add(translation);
        }
        return translations;
    }


    private List<Node> fetchFieldsFromNamespaceNode(final Node namespaceNode, final String fieldType) throws RepositoryException {
        if (!namespaceNode.isNodeType("hipposysedit:templatetype")) {
            return Collections.emptyList();
        }

        final Node nodeTypeHandle = JcrUtils.getNodeIfExists(namespaceNode, "hipposysedit:nodetype");
        if (nodeTypeHandle == null) {
            return Collections.emptyList();
        }

        final Node nodeTypeNode = JcrUtils.getNodeIfExists(nodeTypeHandle, "hipposysedit:nodetype");
        if (nodeTypeNode == null) {
            return Collections.emptyList();
        }

        final List<Node> fields = new ArrayList<>();
        final NodeIterator iterator = nodeTypeNode.getNodes();
        while (iterator.hasNext()) {
            final Node node = iterator.nextNode();
            if (fieldType == null || fieldType.equals(JcrUtils.getStringProperty(node, "hipposysedit:type", null))) {
                fields.add(node);
            }
        }
        return fields;
    }

    private List<ImageVariantRestful> getVariantsForImageSetNamespaceNode(final Node imageSetNode) throws RepositoryException {
        final List<ImageVariantRestful> imageSets = new ArrayList<>();
        final List<Node> nodes = fetchFieldsFromNamespaceNode(imageSetNode, "hippogallery:image");
        for (final Node node : nodes) {
            final String documentType = JcrUtils.getStringProperty(node, "hipposysedit:path", null);
            imageSets.add(new ImageVariantRestful(HippoNodeUtils.getPrefixFromType(documentType), HippoNodeUtils.getNameFromType(documentType)));
        }
        return imageSets;
    }


    private List<Node> fetchVariantTranslations(final Session session) throws RepositoryException {
        final List<Node> variantTranslations = new ArrayList<>();
        final PluginContext pluginContext = getPluginContext();
        final List<Node> nodes = fetchImageSetNamespaceNodes(session, listImageSetTypes(pluginContext));
        log.info("Image set nodes: " + nodes.size());
        for (final Node imageSetNSNode : nodes) {
            log.info("Image set node: " + imageSetNSNode.getPath());
            variantTranslations.addAll(TranslationUtils.getTranslationsFromNode(imageSetNSNode));
        }
        return variantTranslations;
    }

    private List<Node> fetchImageSetNamespaceNodes(final Session session, final List<String> imageSets) throws RepositoryException {
        final List<Node> nodes = new ArrayList<>();
        for (final String imageSet : imageSets) {
            log.info("Fetch Image set NS node for: " + imageSet);
            nodes.add(fetchImageSetNamespaceNode(session, imageSet));
        }
        return nodes;
    }

    private Node fetchImageSetNamespaceNode(final Session session, final String imageSet) throws RepositoryException {
        return session.getNode(getPathToNamespaceNode(imageSet));
    }

    private String getPathToNamespaceNode(final String documentType) {
        return "/hippo:namespaces/" + HippoNodeUtils.getPrefixFromType(documentType) + '/' + HippoNodeUtils.getNameFromType(documentType);
    }

    private List<String> listImageSetTypes(final PluginContext pluginContext) {
        try {
            return CndUtils.getNodeTypesOfType(pluginContext, HippoGalleryNodeType.IMAGE_SET, true);
        } catch (RepositoryException e) {
            log.warn("Unable to retrieve node types", e);
        }
        return Collections.emptyList();

    }

    private PluginContext getPluginContext() {
        return new DashboardPluginContext(GlobalUtils.createSession(), null);
    }


}
