/*
 *  Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.gallery.processor;

import java.awt.Dimension;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.frontend.editor.plugins.resource.MimeTypeHelper;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageOperation;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageOperationResult;
import org.hippoecm.frontend.plugins.gallery.imageutil.ScaleImageOperationFactory;
import org.hippoecm.frontend.plugins.gallery.imageutil.ScalingParameters;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.util.ImageGalleryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gallery processor that puts a scaled version of an image in each resource node. How to scale the image
 * in each resource node is specified by an associated {@link org.hippoecm.frontend.plugins.gallery.imageutil.ScalingParameters} object. Also stores the file name
 * of the image in the main node, and the width and height of each scaled image in the resource nodes.
 */
public class ScalingGalleryProcessor extends AbstractGalleryProcessor {

    private static final Logger log = LoggerFactory.getLogger(ScalingGalleryProcessor.class);

    /**
     * Map of JCR node names to scaling parameters.
     */
    protected final Map<String, ScalingParameters> scalingParametersMap;

    public ScalingGalleryProcessor() {
        scalingParametersMap = new HashMap<>();
    }

    /**
     * Adds the scaling parameters for a resource node. Existing scaling parameters for this node are replaced and
     * returned.
     *
     * @param nodeName the name of the resource node.
     * @param parameters the scaling parameters to use when creating a scaled version of the image in the resource node
     *
     * @return the previous scaling parameters of the resource node with the given name, or <code>null</code> if no
     * previous scaling parameters existed.
     */
    public ScalingParameters addScalingParameters(String nodeName, ScalingParameters parameters) {
        return scalingParametersMap.put(nodeName, parameters);
    }

    /**
     * Stores the given file name in the given node.
     */
    @Override
    public void initGalleryNode(Node node, InputStream data, String mimeType, String fileName) throws RepositoryException {
        ImageGalleryUtils.setImageFileName(node, fileName);
    }

    /**
     * Stores a scaled version of the image data in the given node, together with the scaled width and height.
     * If no scaling parameters have been added for the given node, or the data does not define an image, the
     * original data is stored instead.
     */
    @Override
    public void initGalleryResource(final Node node, final InputStream data, final String mimeType,
                                    final String fileName, final Calendar lastModified) throws RepositoryException {

        node.setProperty(JcrConstants.JCR_MIMETYPE, mimeType);
        node.setProperty(JcrConstants.JCR_LASTMODIFIED, lastModified);

        if (!MimeTypeHelper.isImageMimeType(mimeType)) {
            log.debug("Unknown image MIME type: {}, using raw data", mimeType);
            ImageGalleryUtils.saveImageNode(node, data, 0, 0);
            return;
        }

        final ScalingParameters parameters = getScalingParameters(node);
        if (parameters == null) {
            log.debug("No scaling parameters specified for {}, using original image", node.getName());
            ImageGalleryUtils.saveImageNode(node, data, 0, 0);
            return;
        }

        try {
            final ImageOperation operation = ScaleImageOperationFactory.getOperation(parameters, mimeType);
            final ImageOperationResult result = operation.run(data, mimeType);
            ImageGalleryUtils.saveImageNode(node, result.getData(), result.getWidth(), result.getHeight());
        } catch (GalleryException e) {
            log.warn("Scaling failed, using original image instead", e);
            ImageGalleryUtils.saveImageNode(node, data, 0, 0);
        }
    }

    public Dimension getDesiredResourceDimension(final Node resource) throws GalleryException, RepositoryException {
        final String nodeName = resource.getName();
        final ScalingParameters parameters = scalingParametersMap.get(nodeName);
        if (parameters != null) {
            return new Dimension(parameters.getWidth(), parameters.getHeight());
        } else {
            log.warn("No scaling parameters found for: {}.",nodeName);
            return null;
        }
    }

    @Override
    public boolean isUpscalingEnabled(final Node node) throws GalleryException, RepositoryException {
        final String nodeName = node.getName();
        final ScalingParameters parameters = scalingParametersMap.get(nodeName);
        return parameters == null || parameters.isUpscaling();
    }

    @Override
    public Map<String, ScalingParameters> getScalingParametersMap() {
        return scalingParametersMap;
    }

    @Override
    public ScalingParameters getScalingParameters(final Node variantNode) {
        final String nodeName;
        try {
            nodeName = variantNode.getName();
            return scalingParametersMap.getOrDefault(nodeName, null);
        } catch (RepositoryException e) {
            log.warn("Unable to get image variant name for retrieving the scaling parameters", e);
            return null;
        }
    }
}
