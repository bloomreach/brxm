/*
 *  Copyright 2010-2011 Hippo.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.editor.type.PlainJcrTypeStore;
import org.hippoecm.frontend.editor.plugins.resource.ResourceException;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageMetaData;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageMetadataException;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;
import org.hippoecm.frontend.plugins.gallery.imageutil.UnsupportedImageException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gallery processor that puts a resized version of the image in the primary item and provides hooks to
 * initialize additional properties and the other resource child nodes.
 */
public abstract class AbstractGalleryProcessor implements GalleryProcessor {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(AbstractGalleryProcessor.class);
    private static final Object conversionLock = new Object();

    protected static final String MIMETYPE_IMAGE_PREFIX = "image";

    public AbstractGalleryProcessor() {
        // do nothing
    }

    public void makeImage(Node node, InputStream stream, String mimeType, String fileName) throws GalleryException,
            RepositoryException {
        long time = System.currentTimeMillis();

        Node resourceNode = getPrimaryChild(node);
        if (!resourceNode.isNodeType(HippoNodeType.NT_RESOURCE)) {
            throw new GalleryException("Resource node not of primaryType " + HippoNodeType.NT_RESOURCE);
        }

        Binary binary = ResourceHelper.getValueFactory(resourceNode).createBinary(stream);
        ImageMetaData metadata = new ImageMetaData(mimeType, fileName);
        try {
            metadata.parse(binary.getStream());
        } catch (ImageMetadataException e) {
            throw new GalleryException(e.getMessage());
        }

        if (metadata.getColorModel().equals(ImageMetaData.ColorModel.UNKNOWN)) {
            throw new GalleryException("Unknown color profile for " + metadata.toString());
        }

        if (!metadata.getColorModel().equals(ImageMetaData.ColorModel.RGB)) {
            try {
                synchronized(conversionLock) {
                    InputStream converted = ImageUtils.convertToRGB(binary.getStream(), metadata.getColorModel());
                    binary.dispose();
                    binary = ResourceHelper.getValueFactory(resourceNode).createBinary(converted);
                }
            } catch (IOException e) {
                log.error("Error during conversion to RGB", e);
                throw new GalleryException("Error during conversion to RGB for " + metadata.toString(), e);
            } catch (UnsupportedImageException e) {
                log.error("Image can't be converted to RGB", e);
                throw new GalleryException("Image can't be converted to RGB for " + metadata.toString(), e);
            }
        }

        log.debug("Setting JCR data of primary resource");
        ResourceHelper.setDefaultResourceProperties(resourceNode, mimeType, binary);

        //TODO: this should be removed as validation has been done prior to calling makeImage()
        validateResource(resourceNode, fileName);

        //TODO: do we need the InputStream here?
        InputStream isTemp = binary.getStream();
        try {
            initGalleryNode(node, isTemp, metadata.getMimeType(), fileName);
        } finally {
            IOUtils.closeQuietly(isTemp);
        }

        IStore<ITypeDescriptor> store = new PlainJcrTypeStore(node.getSession());
        ITypeDescriptor type;
        try {
            type = store.load(node.getPrimaryNodeType().getName());
        } catch (StoreException e) {
            throw new GalleryException("Could not load primary node type of " + node.getName() + ", cannot create imageset variants", e);
        }

        //create the primary resource node
        log.debug("Creating primary resource {}", resourceNode.getPath());
        Calendar lastModified = resourceNode.getProperty(JcrConstants.JCR_LASTMODIFIED).getDate();
        initGalleryResource(resourceNode, binary.getStream(), metadata.getMimeType(), fileName, lastModified);

        // create all resource variant nodes
        for (IFieldDescriptor field : type.getFields().values()) {
            if (field.getTypeDescriptor().isType(HippoGalleryNodeType.IMAGE)) {
                String variantPath = field.getPath();
                if (!node.hasNode(variantPath)) {
                    log.debug("creating variant resource {}", variantPath);
                    Node variantNode = node.addNode(variantPath, field.getTypeDescriptor().getType());
                    initGalleryResource(variantNode, binary.getStream(), metadata.getMimeType(), fileName, lastModified);
                }
            }
        }

        binary.dispose();

        time = System.currentTimeMillis() - time;
        log.debug("Processing image '{}' took {} ms.", fileName, time);
    }

    protected Node getPrimaryChild(Node node) throws RepositoryException, GalleryException {
        Item result = null;
        try {
            result = JcrHelper.getPrimaryItem(node);
        } catch (ItemNotFoundException ignored) {
            // ignore
        }

        if (result == null || !result.isNode()) {
            throw new GalleryException("Primary item is not a node");
        }

        return (Node) result;
    }

    /**
     * Validates a resource node. When validation of the primary child fails, the main gallery node is not
     * initialized further and the other resource nodes are left untouched.
     *
     * @param node the resource node to validate
     * @param fileName the file name of the uploaded resource
     *
     * @throws GalleryException when the node is not a valid resource node
     * @throws RepositoryException when repository access failed
     */
    public void validateResource(Node node, String fileName) throws GalleryException,
            RepositoryException {
        try {
            ResourceHelper.validateResource(node, fileName);
        } catch (ResourceException e) {
            throw new GalleryException("Invalid resource: " + fileName, e);
        }
    }

    /**
     * Checks whether the given MIME type indicates an image.
     *
     * @param mimeType the MIME type to check
     * @return true if the given MIME type indicates an image, false otherwise.
     */
    protected boolean isImageMimeType(String mimeType) {
        return mimeType.startsWith(MIMETYPE_IMAGE_PREFIX);
    }

    /**
     * Initializes properties of the main gallery node.
     *
     * @param node the main gallery node
     * @param data the uploaded data
     * @param mimeType the MIME type of the uploaded data
     * @param fileName the file name of the uploaded data
     *
     * @throws RepositoryException when repository access failed
     */
    public abstract void initGalleryNode(Node node, InputStream data, String mimeType, String fileName)
            throws RepositoryException;

    /**
     * Initializes a hippo:resource node of an the main gallery node. Such initialization happens at two times:
     * when a new image is uploaded to the gallery, and when an image in an existing imageset is replaced
     * by another image.
     *
     * @param node the hippo:resource node
     * @param data the uploaded data
     * @param mimeType the MIME type of the uploaded data
     * @param fileName the file name of the uploaded data
     *
     * @throws RepositoryException when repository access failed.
     */
    public abstract void initGalleryResource(Node node, InputStream data, String mimeType, String fileName,
            Calendar lastModified) throws GalleryException, RepositoryException;

    /**
     * Checks whether upscaling is enabled for a particular Node.
     * This implementation returns always true
     *
     * @param node the hippo:resource node     *
     *
     * @throws RepositoryException when repository access failed.
     */
    public boolean isUpscalingEnabled(Node node) throws GalleryException, RepositoryException {
        return true;
    }

}
