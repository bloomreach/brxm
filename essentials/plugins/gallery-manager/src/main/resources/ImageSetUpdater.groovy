package org.onehippo.cms7.essentials.rest.model
/*
 * Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.commons.io.IOUtils
import org.apache.jackrabbit.JcrConstants
import org.hippoecm.frontend.plugins.gallery.processor.ScalingGalleryProcessor
import org.hippoecm.frontend.plugins.gallery.imageutil.ScalingParameters
import org.hippoecm.repository.gallery.HippoGalleryNodeType
import org.onehippo.repository.update.BaseNodeUpdateVisitor

import javax.jcr.Node
import javax.jcr.NodeIterator
import javax.jcr.RepositoryException
import javax.jcr.Session
import javax.jcr.query.Query
import javax.jcr.query.QueryManager
import javax.jcr.query.QueryResult

/**
 * Groovy script to update image sets
 *
 * Query: content/gallery//element(*,hippogallery:imageset)
 *
 */
class ImageSetUpdater extends BaseNodeUpdateVisitor {

    public static final String HIPPO_CONFIGURATION_GALLERY_PROCESSOR_SERVICE = "hippo:configuration/hippo:frontend/cms/cms-services/galleryProcessorService";

    private static final int IMAGE_PROPERTIES_HEIGHT_INDEX = 1;
    private static final int IMAGE_PROPERTIES_WIDTH_INDEX = 0;

    protected static final String CONFIG_PARAM_WIDTH = "width";
    protected static final String CONFIG_PARAM_HEIGHT = "height";
    protected static final Long DEFAULT_WIDTH = 0L;
    protected static final Long DEFAULT_HEIGHT = 0L;

    public final Map<String, List<Long>> imageVariants = new HashMap<String, List<Long>>();

    private boolean overwrite = true;
    private boolean upscaling = true;
    private final String[] names = ["overwrite", "upscaling"];
    private final Object[] objects = [overwrite, upscaling];

    public void initialize(Session session) throws RepositoryException {
        try {
            Node configNode = session.getRootNode().getNode(HIPPO_CONFIGURATION_GALLERY_PROCESSOR_SERVICE);
            getImageVariants(configNode);

        } catch (RepositoryException e) {
            log.error("Exception while retrieving image set variants configuration", e);
        }

        printInit(names, objects);
    }


    boolean doUpdate(Node node) {
        try {
            processImageSet(node);
            return true;
        } catch (RepositoryException e) {
            log.error("Failed in generating image variants", e);
            node.getSession().refresh(false)
        }
        return false;
    }

    @Override
    boolean undoUpdate(final Node node) throws RepositoryException, UnsupportedOperationException {
        return false
    }

    private void processImageSet(Node node) throws RepositoryException {
        Node original;
        if (node.hasNode(HippoGalleryNodeType.IMAGE_SET_ORIGINAL)) {
            original = node.getNode(HippoGalleryNodeType.IMAGE_SET_ORIGINAL);
        } else {
            original = node.getNode(HippoGalleryNodeType.IMAGE_SET_THUMBNAIL);
        }

        for (String variantName : imageVariants.keySet()) {
            try {
                processVariant(node, original, variantName);
            } catch (RepositoryException e) {
                log.error("Failed in generating image variant " + variantName, e);
                node.getSession().refresh(false)
            }
        }
    }

    private void processVariant(Node node, Node original, String variantName) throws RepositoryException {
        /* hippogallery:thumbnail is the only required image variant, not hippogalley:original according to the hippogallery cnd */
        if (!HippoGalleryNodeType.IMAGE_SET_THUMBNAIL.equals(variantName)) {

            final List<Long> dimensions = imageVariants.get(variantName);
            if (dimensions == null) {
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

            Long width = dimensions.get(IMAGE_PROPERTIES_WIDTH_INDEX);
            Long height = dimensions.get(IMAGE_PROPERTIES_HEIGHT_INDEX);

            createImageVariant(node, original, variant, width, height);

            node.getSession().save();
        }
    }

    private void createImageVariant(Node node, Node original, Node variant, Long width, Long height) throws RepositoryException {

        InputStream dataInputStream = null;

        try {
            dataInputStream = original.getProperty(JcrConstants.JCR_DATA).getBinary().getStream();
            String mimeType = original.getProperty(JcrConstants.JCR_MIMETYPE).getString();

            ScalingParameters scalingParameters = new ScalingParameters(width.intValue(), height.intValue(), upscaling);
            ScalingGalleryProcessor scalingGalleryProcessor = new ScalingGalleryProcessor();

            scalingGalleryProcessor.addScalingParameters(variant.getName(), scalingParameters);
            scalingGalleryProcessor.initGalleryResource(variant, dataInputStream, mimeType, "", Calendar.getInstance());

            log.info("Image variant {} generated for node {}", variant.getName(), node.getPath());
        } finally {
            IOUtils.closeQuietly(dataInputStream);
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

                Long width = variantNode.hasProperty(CONFIG_PARAM_WIDTH) ? variantNode.getProperty(CONFIG_PARAM_WIDTH).getLong() : DEFAULT_WIDTH;
                Long height = variantNode.hasProperty(CONFIG_PARAM_HEIGHT) ? variantNode.getProperty(CONFIG_PARAM_HEIGHT).getLong() : DEFAULT_HEIGHT;

                Object[] objects = [variantName, width, height];


                log.info("Registered image set variant '{}' with width {} and height {}",
                        objects);

                final List<Long> dimensions = new ArrayList<Long>(2);
                dimensions.add(width);
                dimensions.add(height);

                imageVariants.put(variantName, dimensions);
            }
        }
    }

    protected void printInit(final String[] configNames, final Object[] configObjects) {

        StringBuilder sb = new StringBuilder("### Initialized runner plugin ").append(this.getClass().getName()).append("\n");

        if ((configNames != null) && configObjects != null) {
            if (configNames.length != configObjects.length) {
                throw new IllegalArgumentException("Lengths of configNames and configObjects do not match: " + configNames.length + " and " + configObjects.length);
            }

            if (configNames.length > 0) {
                sb.append("Initialization parameters:\n");
                for (int i = 0; i < configNames.length; i++) {
                    sb.append("  ").append(configNames[i]).append(" = ").append(configObjects[i]).append("\n");
                }
            }
        }

        log.info(sb.toString());
    }

}