package org.onehippo.cms7.essentials.rest.model

import org.apache.commons.io.IOUtils

/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.jackrabbit.JcrConstants
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils
import org.hippoecm.frontend.plugins.gallery.imageutil.ScalingParameters
import org.hippoecm.frontend.plugins.gallery.processor.ScalingGalleryProcessor
import org.hippoecm.repository.gallery.HippoGalleryNodeType
import org.onehippo.repository.update.BaseNodeUpdateVisitor

import javax.jcr.Node
import javax.jcr.NodeIterator
import javax.jcr.RepositoryException
import javax.jcr.Session
import javax.jcr.query.Query
import javax.jcr.query.QueryManager
import javax.jcr.query.QueryResult

import static groovy.json.JsonOutput.prettyPrint
import static groovy.json.JsonOutput.toJson

/**
 * Groovy script to update image sets
 *
 * Query: content/gallery//element(*, hippogallery:stdImageGallery)
 *
 */
class ImageSetUpdater extends BaseNodeUpdateVisitor {

    public static
    final String HIPPO_CONFIGURATION_GALLERY_PROCESSOR_SERVICE = "hippo:configuration/hippo:frontend/cms/cms-services/galleryProcessorService";

    private static final int IMAGE_PROPERTIES_HEIGHT_INDEX = 1;
    private static final int IMAGE_PROPERTIES_WIDTH_INDEX = 0;

    protected static final String CONFIG_PARAM_WIDTH = "width";
    protected static final String CONFIG_PARAM_HEIGHT = "height";
    protected static final String CONFIG_PARAM_UPSCALING = "upscaling";
    protected static final String CONFIG_PARAM_OPTIMIZE = "optimize";
    protected static final String CONFIG_PARAM_COMPRESSION = "compression";

    protected static final Long DEFAULT_WIDTH = 0L;
    protected static final Long DEFAULT_HEIGHT = 0L;
    protected static final boolean DEFAULT_UPSCALING = false;
    protected static final String DEFAULT_OPTIMIZE = "quality";
    protected static final double DEFAULT_COMPRESSION = 1.0;

    private static final Map<String, ImageUtils.ScalingStrategy> SCALING_STRATEGY_MAP = new LinkedHashMap<>();
    static {
        SCALING_STRATEGY_MAP.put("auto", ImageUtils.ScalingStrategy.AUTO);
        SCALING_STRATEGY_MAP.put("speed", ImageUtils.ScalingStrategy.SPEED);
        SCALING_STRATEGY_MAP.put("speed.and.quality", ImageUtils.ScalingStrategy.SPEED_AND_QUALITY);
        SCALING_STRATEGY_MAP.put("quality", ImageUtils.ScalingStrategy.QUALITY);
        SCALING_STRATEGY_MAP.put("best.quality", ImageUtils.ScalingStrategy.BEST_QUALITY);
    }

    public
    final Map<String, List<String>> imageSets = new HashMap<String, List<String>>();
    public
    final Map<String, ScalingParameters> imageVariants = new HashMap<String, ScalingParameters>();

    private boolean overwrite = true;

    public void initialize(Session session) throws RepositoryException {
        try {
            Node configNode = session.getRootNode().getNode(HIPPO_CONFIGURATION_GALLERY_PROCESSOR_SERVICE);
            getImageVariants(configNode);
            getImageSets(session);

        } catch (RepositoryException e) {
            log.error("Exception while retrieving image set variants configuration", e);
        }

        printInit();
    }


    boolean doUpdate(Node node) {
        try {
            /* hippogallery:thumbnail is the only required image variant, not hippogalley:original according to the hippogallery cnd */
            List<String> imageSet = imageSets.get(node.getPrimaryNodeType().getName());
            if (imageSet == null || imageSet.isEmpty()) {
                log.warn("Could not find image set {} for node {}", node.getPrimaryNodeType().getName(), node.getName());
            }
            processImageSet(node);
            return true;
        } catch (RepositoryException e) {
            log.error("Failed in generating image variants", e);
        }
        return false;
    }

    @Override
    boolean undoUpdate(
            final Node node) throws RepositoryException, UnsupportedOperationException {
        return false
    }

    private void processImageSet(Node node) throws RepositoryException {
        Node data;
        if (node.hasNode(HippoGalleryNodeType.IMAGE_SET_ORIGINAL)) {
            data = node.getNode(HippoGalleryNodeType.IMAGE_SET_ORIGINAL);
        } else if (node.hasNode(HippoGalleryNodeType.IMAGE_SET_THUMBNAIL)) {
            data = node.getNode(HippoGalleryNodeType.IMAGE_SET_THUMBNAIL)
        }

        for (String variantName : imageVariants.keySet()) {
            processVariant(node, data, variantName);
        }
    }

    private void processVariant(Node node, Node data, String variantName) throws RepositoryException {

        // same exception for thumbnails as when building the imageVariants
        if (!HippoGalleryNodeType.IMAGE_SET_THUMBNAIL.equals(variantName)) {

            final ScalingParameters parameters = imageVariants.get(variantName);
            if (parameters.width == 0 && parameters.height == 0) {
                log.warn("No width and height available for image variant {}. Skipping node {}", variantName, node.getPath());
                return;
            }

            Node variant;
            if (node.hasNode(variantName)) {
                if (!overwrite) {
                    log.info("Skipping existing variant {} of node {}", variantName, node.getPath());
                    return;
                }
                variant = node.getNode(variantName);
            } else {
                variant = node.addNode(variantName, HippoGalleryNodeType.IMAGE);
            }



            createImageVariant(node, data, variant, parameters);

            node.getSession().save();
        }
    }

    private void createImageVariant(Node node, Node data, Node variant, ScalingParameters parameters) throws RepositoryException {

        InputStream dataInputStream = null;

        try {
            dataInputStream = data.getProperty(JcrConstants.JCR_DATA).getBinary().getStream();
            String mimeType = data.getProperty(JcrConstants.JCR_MIMETYPE).getString();

            ScalingGalleryProcessor scalingGalleryProcessor = new ScalingGalleryProcessor();

            scalingGalleryProcessor.addScalingParameters(variant.getName(), parameters);
            scalingGalleryProcessor.initGalleryResource(variant, dataInputStream, mimeType, "", Calendar.getInstance());

            log.info("Image variant {} generated for node {}", variant.getName(), node.getPath());
        } finally {
            IOUtils.closeQuietly(dataInputStream);
        }
    }

    private void getImageInformation(Node node) {
        try {
            Session session = node.getSession();
            Node configNode = session.getRootNode().getNode(HIPPO_CONFIGURATION_GALLERY_PROCESSOR_SERVICE);

            getImageVariants(configNode);

            getImageSets(session);


        } catch (RepositoryException e) {
            log.error("Exception while retrieving image set variants configuration", e);
        }
    }

    private void getImageSets(Session session) throws RepositoryException {
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("hippo:namespaces//element(*,hippogallery:imageset)", "xpath");
        QueryResult queryResult = query.execute();
        NodeIterator nodeIterator = queryResult.getNodes();

        // looking up nodes of type hippogallery:image (or derived) in the prototype of a namespace definition
        while (nodeIterator.hasNext()) {
            Node next = nodeIterator.nextNode();
            NodeIterator children = next.getNodes();

            List<String> imageVariants = new ArrayList<String>();

            while (children.hasNext()) {
                Node child = children.nextNode();

                if (child.isNodeType(HippoGalleryNodeType.IMAGE)) {
                    imageVariants.add(child.getName());
                }
            }
            imageSets.put(next.getPrimaryNodeType().getName(), imageVariants);
        }
    }

    private void getImageVariants(Node configNode) throws RepositoryException {
        NodeIterator variantNodes = configNode.getNodes();

        while (variantNodes.hasNext()) {
            Node variantNode = variantNodes.nextNode();
            String variantName = variantNode.getName();

            // hippogallery:thumbnail is the only required image variant according to the hippogallery.cnd
            // so no regeneration for that one
            if (!HippoGalleryNodeType.IMAGE_SET_THUMBNAIL.equals(variantName)) {

                int width = variantNode.hasProperty(CONFIG_PARAM_WIDTH) ? variantNode.getProperty(CONFIG_PARAM_WIDTH).getLong() : DEFAULT_WIDTH;
                int height = variantNode.hasProperty(CONFIG_PARAM_HEIGHT) ? variantNode.getProperty(CONFIG_PARAM_HEIGHT).getLong() : DEFAULT_HEIGHT;
                boolean upscaling = variantNode.hasProperty(CONFIG_PARAM_UPSCALING) ?
                        variantNode.getProperty(CONFIG_PARAM_UPSCALING).boolean : DEFAULT_UPSCALING
                String optimize = variantNode.hasProperty(CONFIG_PARAM_OPTIMIZE) ?
                        variantNode.getProperty(CONFIG_PARAM_OPTIMIZE).string : DEFAULT_OPTIMIZE
                float compression = variantNode.hasProperty(CONFIG_PARAM_COMPRESSION) ?
                        variantNode.getProperty(CONFIG_PARAM_COMPRESSION).double : DEFAULT_COMPRESSION


                ImageUtils.ScalingStrategy strategy = SCALING_STRATEGY_MAP.get(optimize);
                if (strategy == null) {
                    log.warn "Image variant '${variantName}' specifies an unknown scaling optimization strategy " +
                            "'${optimize}'. Possible values are ${SCALING_STRATEGY_MAP.keySet()}. Falling back to" +
                            " '${DEFAULT_OPTIMIZE}' instead."
                    strategy = SCALING_STRATEGY_MAP.get(DEFAULT_OPTIMIZE);
                }




                ScalingParameters parameters = new ScalingParameters(width.intValue(), height.intValue(), upscaling, strategy, compression)

                log.info "Registered image set variant '${variantName} 'with scalingParameters '${parameters}'"

                imageVariants.put(variantName, parameters);
            }
        }
    }

    protected void printInit() {

        log.info "### Initialized runner plugin ${this.getClass().getName()}"
        log.info prettyPrint(toJson(imageSets))
        log.info prettyPrint(toJson(imageVariants))


    }

}