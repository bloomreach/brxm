/*
 *  Copyright 2010 Hippo.
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
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.frontend.plugins.gallery.imageutil.ScaleImageOperation;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gallery processor that puts a scaled version of an image in each resource node. How to scale the image
 * in each resource node is specified by an associated {@link ScalingParameters} object. Also stores the file name
 * of the image in the main node, and the width and height of each scaled image in the resource nodes.
 */
public class ScalingGalleryProcessor extends AbstractGalleryProcessor {

    private static final Logger log = LoggerFactory.getLogger(ScalingGalleryProcessor.class);
    private static final long serialVersionUID = 1L;

    protected static final ScalingParameters INDIVIDUAL_UPLOAD_SCALING_PARAMETERS = new ScalingParameters(0, 0, false);

    /**
     * Map of JCR node names to scaling parameters.
     */
    protected final Map<String, ScalingParameters> scalingParametersMap;

    public ScalingGalleryProcessor() {
        scalingParametersMap = new HashMap<String, ScalingParameters>();
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
        node.setProperty(HippoGalleryNodeType.IMAGE_SET_FILE_NAME, fileName);
    }

    /**
     * Stores a scaled version of the image data in the given node, together with the scaled width and height.
     * If no scaling parameters have been added for the given node, or the data does not define an image, the
     * original data is stored instead.
     */
    @Override
    public void initGalleryResource(Node node, InputStream data, String mimeType, String fileName, Calendar lastModified)
            throws RepositoryException {
        node.setProperty(JcrConstants.JCR_MIMETYPE, mimeType);
        node.setProperty(JcrConstants.JCR_LASTMODIFIED, lastModified);

        InputStream stored = data;
        int width = 0;
        int height = 0;

        if (isImageMimeType(mimeType)) {
            final String nodeName = node.getName();
            final ScalingParameters p = scalingParametersMap.get(nodeName);
            if (p != null) {
                try {
                    final ScaleImageOperation scaleOperation = new ScaleImageOperation(p.getWidth(), p.getHeight(),
                            p.getUpscaling(), p.getStrategy(), p.getCompressionQuality());

                    scaleOperation.execute(data, mimeType);

                    stored = scaleOperation.getScaledData();
                    width = scaleOperation.getScaledWidth();
                    height = scaleOperation.getScaledHeight();
                } catch (GalleryException e) {
                    log.warn("Scaling failed, using original image instead", e);
                }
            } else {
                log.debug("No scaling parameters specified for {}, using original image", nodeName);
            }
        } else {
            log.debug("Unknown image MIME type: {}, using raw data", mimeType);
        }

        node.setProperty(JcrConstants.JCR_DATA, ResourceHelper.getValueFactory(node).createBinary(stored));
        node.setProperty(HippoGalleryNodeType.IMAGE_WIDTH, width);
        node.setProperty(HippoGalleryNodeType.IMAGE_HEIGHT, height);
    }

    public Dimension getDesiredResourceDimension(Node resource) throws GalleryException, RepositoryException {
        String nodeName = resource.getName();
        ScalingParameters scaleOperation = scalingParametersMap.get(nodeName);
        if (scaleOperation != null) {
            int width = scaleOperation.getWidth();
            int height = scaleOperation.getHeight();
            return new Dimension(width, height);
        } else {
            return null;
        }
    }

    public boolean isUpscalingEnabled(Node resource) throws GalleryException, RepositoryException {
        String nodeName = resource.getName();
        ScalingParameters scaleOperation = scalingParametersMap.get(nodeName);
        return scaleOperation == null || scaleOperation.getUpscaling();
    }
}
