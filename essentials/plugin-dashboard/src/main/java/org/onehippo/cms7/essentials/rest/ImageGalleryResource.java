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

package org.onehippo.cms7.essentials.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.DefaultPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.utils.CndUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GalleryUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TranslationUtils;
import org.onehippo.cms7.essentials.rest.exc.RestException;
import org.onehippo.cms7.essentials.rest.model.TranslationRestful;
import org.onehippo.cms7.essentials.rest.model.gallery.ImageGalleryDataRestful;
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

    private static final String PROPERTY_UPSCALING = "upscaling";
    private static final String PROPERTY_OPTIMIZE = "optimize";
    private static final String PROPERTY_COMPRESSION = "compression";
    private static final String PROPERTY_HEIGHT = "height";
    private static final String PROPERTY_WIDTH = "width";
    private static final String HIPPOSYSEDIT_NODETYPE = "hipposysedit:nodetype";

    private static Logger log = LoggerFactory.getLogger(ImageGalleryResource.class);

    private static final String GALLERY_PROCESSOR_SERVICE_PATH = "/hippo:configuration/hippo:frontend/cms/cms-services/galleryProcessorService";

    private static final Boolean DEFAULT_UPSCALING_VALUE = Boolean.FALSE;
    private static final String DEFAULT_OPTIMIZATION_VALUE = "quality";
    private static final Double DEFAULT_COMPRESSION_VALUE = 1.0D;

    private static final String DEFAULT_IMAGE_PROCESSOR_ID = "service.gallery.processor";


    @GET
    @Path("/gallerycontent")
    public ImageGalleryDataRestful getImageGalleryData() {
        final ImageProcessorRestful imageProcessor = getImageProcessor(DEFAULT_IMAGE_PROCESSOR_ID);
        if (imageProcessor == null) {
            return null;
        }

        final List<ImageSetRestful> imageSets;
        try {
            imageSets = fetchImageSets();
        } catch (final RepositoryException e) {
            log.error("Error while fetching image sets", e);
            return null;
        }

        return new ImageGalleryDataRestful(imageProcessor, imageSets);
    }

    @PUT
    @Path("/gallerycontentsave")
    //@Consumes("application/json")
    //@Produces("application/json")
    public ImageGalleryDataRestful saveImageGalleryData(final ImageGalleryDataRestful imageGalleryData) {
        if (imageGalleryData == null) {
            log.error("Unable to process image gallery data");
            return null;
        }

        final ImageProcessorRestful imageProcessor = imageGalleryData.getImageProcessor();
        if (imageProcessor == null) {
            log.error("Unable to save image gallery data; no image processor available");
            return imageGalleryData;
        }

        final List<ImageSetRestful> imageSets = imageGalleryData.getImageSets();
        if (imageSets == null) {
            log.error("Unable to save image gallery data; no list with image sets available");
            return imageGalleryData;
        }

        log.info("Saving processor at {} with {} image sets", imageProcessor.getPath(), imageSets.size());

        final PluginContext pluginContext = getPluginContext();
        final Session session = pluginContext.createSession();
        try {
            saveImageProcessor(session, imageProcessor);

            saveImageSets(session, imageProcessor, imageSets);

            session.save();
            log.info("Image processor {} with {} imagesets saved", imageProcessor.getName(), imageSets.size());
        } catch (RepositoryException e) {
            log.error("Error while trying to update image processor", e);
            throw new RestException("Error while trying to update image processor", Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        return imageGalleryData;
    }

    private ImageProcessorRestful getImageProcessor(final String imageProcessorId) {
        final ImageProcessorRestful processorRestful = new ImageProcessorRestful();
        final PluginContext pluginContext = getPluginContext();

        final Session session = pluginContext.createSession();

        try {
            final Node processorNode = getImageProcessorNode(session, imageProcessorId);
            if (processorNode == null) {
                throw new RestException("Unable to find image processor node", Response.Status.INTERNAL_SERVER_ERROR);
            }
            processorRestful.setPath(processorNode.getPath());
            processorRestful.setClassName(processorNode.getProperty("plugin.class").getString());
            processorRestful.setId(processorNode.getProperty("gallery.processor.id").getString());

            final Map<String, ImageVariantRestful> variantMap = fetchImageProcessorVariants(session, processorNode);
            processorRestful.addVariants(variantMap.values());


            populateImageSetsInVariants(session, variantMap.values());


        } catch (RepositoryException e) {
            log.error("Exception while trying to retrieve document types from repository {}", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        return processorRestful;
    }

    private Node getImageProcessorNode(final Session session, final String imageProcessorId) {
        if (StringUtils.isBlank(imageProcessorId)) {
            log.error("Unable to fetch image processor node for empty id");
            return null;
        }
        final String query = new StringBuilder()
                .append("SELECT * FROM [frontend:plugin] WHERE [gallery.processor.id] = '")
                .append(imageProcessorId)
                .append("' AND ISDESCENDANTNODE([/hippo:configuration/hippo:frontend/cms/cms-services])")

                .toString();
        try {
            final QueryResult result = session.getWorkspace().getQueryManager().createQuery(query, Query.JCR_SQL2).execute();
            final NodeIterator nodeIterator = result.getNodes();
            if (nodeIterator.hasNext()) {
                return nodeIterator.nextNode();
            }
        } catch (RepositoryException e) {
            log.error("Error while fetching image procossor: {}", imageProcessorId, e);
        }
        log.warn("Unable to fetch image processor: {}", imageProcessorId);
        return null;
    }

    /**
     * Update the image processor, with the underlying variants.
     * <p/>
     * To persist the changes a session save needs to be performed.
     *
     * @param session        the JCR session
     * @param imageProcessor the image processor
     * @throws RepositoryException when an exception occurs.
     */
    private void saveImageProcessor(final Session session, final ImageProcessorRestful imageProcessor) throws RepositoryException {
        final Node processorNode = session.getNode(imageProcessor.getPath());
        // Remove all old non used variants
        deleteOldVariants(processorNode, imageProcessor);
        // save all variants
        for (final ImageVariantRestful variant : imageProcessor.getVariants()) {
            final Node variantNode;
            final String variantNodeType = HippoNodeUtils.getTypeFromPrefixAndName(variant.getNamespace(), variant.getName());
            if (processorNode.hasNode(variantNodeType)) {
                log.debug("Update variant node {}", variantNodeType);
                variantNode = processorNode.getNode(variantNodeType);
            } else {
                log.debug("Add new variant node {}", variantNodeType);
                variantNode = processorNode.addNode(variantNodeType, "frontend:pluginconfig");
            }
            updateVariantNode(variantNode, variant);
        }
    }

    private void deleteOldVariants(final Node processorNode, final ImageProcessorRestful imageProcessor) throws RepositoryException {
        final List<Node> nodesToDelete = new ArrayList<>();
        // Determine the nodes to delete
        final NodeIterator variantIterator = processorNode.getNodes();
        while (variantIterator.hasNext()) {
            final Node variantNode = variantIterator.nextNode();
            // TODO deteremine whether to check on id or on namespace/name combination
            final ImageVariantRestful variant = imageProcessor.getVariantForNodeType(variantNode.getName());
            if (variant == null) {
                // TODO check for hippogallery namespace
                if ("hippogallery".equals(HippoNodeUtils.getPrefixFromType(variantNode.getName()))) {
                    log.error("Shouldn't delete variants which belong to hippogallery namespace");
                    continue;
                }
                nodesToDelete.add(variantNode);
            }
        }
        // Delete the nodes
        for (final Node nodeToDelete : nodesToDelete) {
            log.info("Remove variant node {}", nodeToDelete.getPath());
            nodeToDelete.remove();
        }
    }

    /**
     * Update width, height and additional properties of the variant node.
     * <p/>
     * Currently the translations are not saved here, because they are stored in another location (i.e. image sets),
     * which is unrelated to the variant.
     *
     * @param variantNode the node of the variant of the image processor
     * @param variant     the variant representation with values to store
     * @throws RepositoryException when a repository exception occurs
     */
    private void updateVariantNode(final Node variantNode, final ImageVariantRestful variant) throws RepositoryException {
        variantNode.setProperty(PROPERTY_HEIGHT, variant.getHeight());
        variantNode.setProperty(PROPERTY_WIDTH, variant.getWidth());

        // Remove unused properties (default value is used)
        // Upscaling property
        final Boolean upscaling = variant.getUpscaling();
        if (upscaling == null || DEFAULT_UPSCALING_VALUE.equals(upscaling) || upscaling == null) {
            removeProperty(variantNode, PROPERTY_UPSCALING);
        } else {
            variantNode.setProperty(PROPERTY_UPSCALING, upscaling);
        }
        // Optimization property
        final String optimization = variant.getOptimization();
        if (optimization == null || DEFAULT_OPTIMIZATION_VALUE.equals(optimization) || StringUtils.isBlank(optimization)) {
            removeProperty(variantNode, PROPERTY_OPTIMIZE);
        } else {
            variantNode.setProperty(PROPERTY_OPTIMIZE, optimization);
        }
        // Compression property
        final Double compression = variant.getCompression();
        if (compression == null || DEFAULT_COMPRESSION_VALUE.equals(compression) || compression == null) {
            removeProperty(variantNode, PROPERTY_COMPRESSION);
        } else {
            variantNode.setProperty(PROPERTY_COMPRESSION, compression);
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
            if (variantNode.hasProperty(PROPERTY_WIDTH)) {
                variantRestful.setWidth((int) variantNode.getProperty(PROPERTY_WIDTH).getLong());
            }
            if (variantNode.hasProperty(PROPERTY_HEIGHT)) {
                variantRestful.setHeight((int) variantNode.getProperty(PROPERTY_HEIGHT).getLong());
            }

            // Upscaling property
            if (variantNode.hasProperty(PROPERTY_UPSCALING)) {
                variantRestful.setUpscaling(variantNode.getProperty(PROPERTY_UPSCALING).getBoolean());
            }

            // Optimize property
            if (variantNode.hasProperty(PROPERTY_OPTIMIZE)) {
                variantRestful.setOptimization(variantNode.getProperty(PROPERTY_OPTIMIZE).getString());
            }

            // Compression property
            if (variantNode.hasProperty(PROPERTY_COMPRESSION)) {
                variantRestful.setCompression(variantNode.getProperty(PROPERTY_COMPRESSION).getDouble());
            }

            if (variantTranslationsMap.get(variantName) != null) {
                log.debug("Translations for {} : ", variantName, variantTranslationsMap.get(variantName).size());
                variantRestful.addTranslations(variantTranslationsMap.get(variantName).values());
            } else {
                log.debug("No translations for {}", variantName);
            }
            if (log.isTraceEnabled()) {
                for (final String key : variantTranslationsMap.keySet()) {
                    log.debug("Has translation for {}", key);
                }
            }

            variants.put(HippoNodeUtils.getTypeFromPrefixAndName(variantRestful.getNamespace(), variantRestful.getName()), variantRestful);
        }
        return variants;
    }

    private Map<String, Map<String, TranslationRestful>> getVariantTranslationsMap(final Session session) throws RepositoryException {
        final Map<String, Map<String, TranslationRestful>> map = new HashMap<>();
        for (final Node node : fetchVariantTranslations(session)) {
            final TranslationRestful translation = new TranslationRestful();
            translation.setLocale(TranslationUtils.getHippoLanguage(node));
            translation.setMessage(TranslationUtils.getHippoMessage(node));

            final String propertyName = TranslationUtils.getHippoProperty(node);
            if (StringUtils.isBlank(propertyName)) {
                log.debug("Skipping translation: {}", node.getPath());
                continue;
            } else {
                log.debug("Adding translation: {}", node.getPath());
            }
            if (!map.containsKey(propertyName)) {
                map.put(propertyName, new HashMap<String, TranslationRestful>());
            }
            map.get(propertyName).put(translation.getLocale(), translation);
        }
        return map;
    }

    private void saveImageSets(final Session session, final ImageProcessorRestful imageProcessor, final List<ImageSetRestful> imageSets) throws RepositoryException {
        if (imageSets == null) {
            log.error("Unable to process unexisting image sets");
            return;
        }

        for (final ImageSetRestful imageSet : imageSets) {
            if ("hippogallery".equals(imageSet.getNamespace())) {
                log.info("Skip hippogallery imageset: {}", HippoNodeUtils.getTypeFromPrefixAndName(imageSet.getNamespace(), imageSet.getName()));
                continue;
            }
            saveImageSet(session, imageSet, imageProcessor);
        }
    }

    private boolean saveImageSet(final Session session, final ImageSetRestful imageSet, final ImageProcessorRestful imageProcessor) throws RepositoryException {
        final String imageSetNodeType = HippoNodeUtils.getTypeFromPrefixAndName(imageSet.getNamespace(), imageSet.getName());
        log.info("Updating image set {}", imageSetNodeType);

        final PluginContext pluginContext = getPluginContext();

        // Check namespace registry
        if (!CndUtils.namespacePrefixExists(pluginContext, imageSet.getNamespace())) {
            log.error("Unexisting namespace {} not supported", imageSet.getNamespace());
            throw new RepositoryException("Unable to save image set; namespace '" + imageSet.getNamespace() + "' does not exist");
        }

        // Get or create namespace node
        final Node namespaceNode = getNamespaceNodeForImageSet(session, imageSet);
        if (namespaceNode == null) {
            throw new RepositoryException("Unable to find namespace node for namespace " + imageSet.getNamespace());
        }

        // Check document type
        if (!CndUtils.nodeTypeExists(pluginContext, imageSetNodeType)) {
            CndUtils.registerDocumentType(pluginContext, imageSet.getNamespace(), imageSet.getName(), false, false, GalleryUtils.HIPPOGALLERY_IMAGE_SET, GalleryUtils.HIPPOGALLERY_RELAXED);
        } else if (!CndUtils.isNodeOfSuperType(pluginContext, imageSetNodeType, HippoGalleryNodeType.IMAGE_SET)) {
            log.error("Incorrect node type {}; not of type imageset", imageSetNodeType);
            throw new RepositoryException("There is already a '" + imageSetNodeType + "' node with incorrect node type");
        }

        // Check namespace node
        final Node imageSetNode;
        if (namespaceNode.hasNode(imageSet.getName())) {
            imageSetNode = namespaceNode.getNode(imageSet.getName());
            log.debug("Fetched existing image set namespace node {}", imageSetNode.getPath());
        } else {
            log.debug("Create new image set namespace node for image set {}", imageSetNodeType);
            imageSetNode = GalleryUtils.createImagesetNamespace(session, imageSet.getNamespace(), imageSet.getName());
        }

        // Remove all old non used variants
        final List<Node> nodes = fetchFieldsFromNamespaceNode(imageSetNode, HippoGalleryNodeType.IMAGE);
        for (final Node variantFieldNode : nodes) {
            if ("original".equals(variantFieldNode.getName()) || "thumbnail".equals(variantFieldNode.getName())) {
                log.debug("Do not delete original and thumbnail variants");
                continue;
            }
            log.debug("Check variant {}", variantFieldNode.getName());
            final ImageVariantRestful variant = imageSet.getVariantByName(variantFieldNode.getName());
            if (variant == null) {
                log.debug("Remove {}", variantFieldNode.getPath());
                if (imageSetNode.hasNode("editor:templates/_default_/" + variantFieldNode.getName())) {
                    final Node templateNode = imageSetNode.getNode("editor:templates/_default_/" + variantFieldNode.getName());
                    log.debug("Remove {}", templateNode.getPath());
                    templateNode.remove();
                }
                variantFieldNode.remove();
            }
        }

        if (!imageSetNode.hasNode("hipposysedit:nodetype/hipposysedit:nodetype")) {
            log.error("Node type node not available for {}", imageSetNode.getPath());
            throw new RepositoryException("Unable to save image set; no node type available at '" + imageSetNode.getPath());
        }
        final Node imageSetNodeTypeNode = imageSetNode.getNode("hipposysedit:nodetype/hipposysedit:nodetype");

        // Remove unused translations
        final List<Node> existingTranslations = TranslationUtils.getTranslationsFromNode(imageSetNode);
        for (final Node existingTranslation : existingTranslations) {


            existingTranslation.remove();
        }

        // save all variants
        for (final ImageVariantRestful variant : imageSet.getVariants()) {
            if (variant.getName() == null) {
                log.debug("Unable to process variant without name");
                continue;
            }

            // Only add node type and template for variant when there is no field available
            if (imageSetNodeTypeNode.hasNode(variant.getName())) {
                log.debug("Variant {} already defined in node type; no need to update", imageSetNode.getName());
            } else {
                setTemplateNodeTypeForVariant(session, imageSetNode, variant);
                setTemplateFieldForVariant(session, imageSetNode, variant);
            }

            // Update variant translations for image set
            final String variantNodeType = HippoNodeUtils.getTypeFromPrefixAndName(variant.getNamespace(), variant.getName());
            final ImageVariantRestful processorVariant = imageProcessor.getVariant(variant.getNamespace(), variant.getName());
            if (processorVariant != null) {

                log.debug("Store translations for variant {} on image set {}", variantNodeType, imageSetNodeType);
                for (final TranslationRestful translation : processorVariant.getTranslations()) {
                    log.debug("Store '{}' translation for variant: {}", translation.getLocale(), variantNodeType);
                    TranslationUtils.setTranslationForNode(imageSetNode, variantNodeType, translation.getLocale(), translation.getMessage());
                }
            } else {
                log.debug("No variant {} found in image processor", variantNodeType);
            }
        }

        for (final TranslationRestful translation : imageSet.getTranslations()) {
            log.debug("Store '{}' translation for image set: {}", translation.getLocale(), imageSetNodeType);
            TranslationUtils.setTranslationForNode(imageSetNode, null, translation.getLocale(), translation.getMessage());
        }
        return true;
    }

    private Node getNamespaceNodeForImageSet(final Session session, final ImageSetRestful imageSet) throws RepositoryException {
        final Node nsNode = CndUtils.getHippoNamespaceNode(session, imageSet.getNamespace());
        if (nsNode != null) {
            log.debug("Return existing namespace node {} for image set {}", imageSet.getNamespace(), imageSet.getName());
            return nsNode;
        }

        log.info("Create namespace node {} for image set {}", imageSet.getNamespace(), imageSet.getName());
        return CndUtils.createHippoNamespace(session, imageSet.getNamespace());
    }

    private void setTemplateNodeTypeForVariant(final Session session, final Node imagesetTemplate, final ImageVariantRestful variant) throws RepositoryException {
        // TODO only required to retrieve node when copy is required
        final Node original = imagesetTemplate.getNode("hipposysedit:nodetype").getNode("hipposysedit:nodetype").getNode("original");
        final String sysPath = original.getParent().getPath() + '/' + variant.getName();
        final Node copy = HippoNodeUtils.retrieveExistingNodeOrCreateCopy(session, sysPath, original);
        copy.setProperty(HippoNodeUtils.HIPPOSYSEDIT_PATH, HippoNodeUtils.getTypeFromPrefixAndName(variant.getNamespace(), variant.getName()));
        copy.setProperty(HippoNodeType.HIPPOSYSEDIT_TYPE, HippoGalleryNodeType.IMAGE);
    }

    private void setTemplateFieldForVariant(final Session session, final Node imagesetTemplate, final ImageVariantRestful variant) throws RepositoryException {
        // TODO only required to retrieve node when copy is required
        final Node original = imagesetTemplate.getNode("editor:templates").getNode("_default_").getNode("original");
        final String sysPath = original.getParent().getPath() + '/' + variant.getName();
        final Node copy = HippoNodeUtils.retrieveExistingNodeOrCreateCopy(session, sysPath, original);
        copy.setProperty("caption", variant.getName());
        copy.setProperty("field", variant.getName());

    }


    protected void storeImageSetTranslations(final Node imageSetNode, final ImageSetRestful imageSet) {
        // TODO
        // save variant translations as well??
    }


    @GET
    @Path("/imagesets/")
    public ImageSetsRestful fetchImageSetsRestful() throws RepositoryException {
        return new ImageSetsRestful(fetchImageSets());
    }

    private List<ImageSetRestful> fetchImageSets() throws RepositoryException {
        final PluginContext pluginContext = getPluginContext();
        final Session session = pluginContext.createSession();
        final List<ImageSetRestful> imageSets;
        try {
            final Node processorNode = session.getNode(GALLERY_PROCESSOR_SERVICE_PATH);
            imageSets = fetchImageSets(session);
            populateVariantsInImageSets(session, imageSets, processorNode);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        return imageSets;
    }

    private void populateImageSetsInVariants(final Session session, final Iterable<ImageVariantRestful> variants) throws RepositoryException {
        final List<ImageSetRestful> imageSets = fetchImageSets(session);
        for (final ImageVariantRestful variant : variants) {
            populateImageSetsInVariant(variant, imageSets);
        }
    }

    private void populateImageSetsInVariant(final ImageVariantRestful variant, final Iterable<ImageSetRestful> availableImageSets) {
        final List<ImageSetRestful> imageSets = new ArrayList<>();
        for (final ImageSetRestful imageSet : availableImageSets) {
            if (imageSet.hasVariant(variant.getNamespace(), variant.getName())) {
                imageSets.add(imageSet);
            }
        }
        variant.setImageSets(imageSets);
    }


    private void populateVariantsInImageSets(final Session session, final Iterable<ImageSetRestful> imageSets, final Node processorNode) throws RepositoryException {
        final Map<String, ImageVariantRestful> availableVariants = fetchImageProcessorVariants(session, processorNode);
        for (final ImageSetRestful imageSet : imageSets) {
            populateVariantsInImageSet(imageSet, availableVariants);
        }
    }

    private void populateVariantsInImageSet(ImageSetRestful imageSet, final Map<String, ImageVariantRestful> availableVariants) {
        final List<ImageVariantRestful> variants = new ArrayList<>();
        for (final ImageVariantRestful tempVariant : imageSet.getVariants()) {
            final ImageVariantRestful variant = availableVariants.get(HippoNodeUtils.getTypeFromPrefixAndName(tempVariant.getNamespace(), tempVariant.getName()));
            if (variant != null) {
                variants.add(variant);
            }
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
                log.info("Skipping translation: {}", node.getPath());
                continue;
            } else {
                log.info("Adding translation: {}", node.getPath());
            }
            translations.add(translation);
        }
        return translations;
    }


    private List<Node> fetchFieldsFromNamespaceNode(final Node namespaceNode, final String fieldType) throws RepositoryException {
        if (!namespaceNode.isNodeType("hipposysedit:templatetype")) {
            return Collections.emptyList();
        }

        final Node nodeTypeHandle = JcrUtils.getNodeIfExists(namespaceNode, HIPPOSYSEDIT_NODETYPE);
        if (nodeTypeHandle == null) {
            return Collections.emptyList();
        }

        final Node nodeTypeNode = JcrUtils.getNodeIfExists(nodeTypeHandle, HIPPOSYSEDIT_NODETYPE);
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


    private Iterable<Node> fetchVariantTranslations(final Session session) throws RepositoryException {
        final Collection<Node> variantTranslations = new ArrayList<>();
        final PluginContext pluginContext = getPluginContext();
        final List<Node> nodes = fetchImageSetNamespaceNodes(session, listImageSetTypes(pluginContext));
        log.debug("Image set nodes: {}", nodes.size());
        for (final Node imageSetNSNode : nodes) {
            log.debug("Image set node: {}", imageSetNSNode.getPath());
            variantTranslations.addAll(TranslationUtils.getTranslationsFromNode(imageSetNSNode));
        }
        return variantTranslations;
    }

    private List<Node> fetchImageSetNamespaceNodes(final Session session, final Iterable<String> imageSets) throws RepositoryException {
        final List<Node> nodes = new ArrayList<>();
        for (final String imageSet : imageSets) {
            log.debug("Fetch Image set NS node for: {}", imageSet);
            final Node node = fetchImageSetNamespaceNode(session, imageSet);
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    private Node fetchImageSetNamespaceNode(final Session session, final String imageSet) throws RepositoryException {
        if (!session.nodeExists(getPathToNamespaceNode(imageSet))) {
            log.warn("Namespace node doesn't exist for registered gallery node type: {}", imageSet);
            return null;
        }
        return session.getNode(getPathToNamespaceNode(imageSet));
    }

    private String getPathToNamespaceNode(final String documentType) {
        return "/hippo:namespaces/" + HippoNodeUtils.getPrefixFromType(documentType) + '/' + HippoNodeUtils.getNameFromType(documentType);
    }

    private Iterable<String> listImageSetTypes(final PluginContext pluginContext) {
        try {
            return CndUtils.getNodeTypesOfType(pluginContext, HippoGalleryNodeType.IMAGE_SET, true);
        } catch (RepositoryException e) {
            log.warn("Unable to retrieve node types", e);
        }
        return Collections.emptyList();

    }

    private PluginContext getPluginContext() {
        return new DefaultPluginContext(null);
    }

}