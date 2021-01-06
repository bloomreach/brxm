/*
 *  Copyright 2008-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.model;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeDefinition;

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.util.io.IOUtils;
import org.hippoecm.frontend.editor.plugins.resource.MimeTypeHelper;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.imageutil.ScalingParameters;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_TEXT;

/**
 * Gallery processor that puts a resized version of the image in the primary item and populates
 * other resource child nodes with the original.
 */
public class DefaultGalleryProcessor implements GalleryProcessor {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DefaultGalleryProcessor.class);

    private static final int FIRST_IMAGE_IN_FILE = 0;

    public static final int DEFAULT_THUMBNAIL_SIZE = 60;

    private int thumbnailSize = DEFAULT_THUMBNAIL_SIZE;

 public void setThumbnailSize(int thumbnailSize) {
        this.thumbnailSize = thumbnailSize;
    }

    public int getThumbnailSize() {
        return thumbnailSize;
    }

    /**
     * Creates a thumbnail version of an image.
     * The maxsize parameter determines how the image is scaled: it is the maximum size
     * of both the width and height. If the original height is greater than the original
     * width, then the height of the scaled image will be equal to max size. The same
     * goes for the original width: if it is greater than the original height, the width of
     * the scaled image is equal to maxsize.
     * @param imageData the original image data; the input stream is closed by this method.
     * @param maxSize the maximum height or width of the scaled image
     * @param mimeType the mime type of the image
     * @return the scaled image stream
     */
    public InputStream createThumbnail(InputStream imageData, int maxSize, String mimeType) throws GalleryException {
        if (maxSize < 1) {
            throw new IllegalArgumentException("A maximum size smaller than 1 is not supported");
        }
        if (mimeType == null || "".equals(mimeType.trim())) {
            throw new IllegalArgumentException("A mime type should be provided");
        }
        if (imageData == null) {
            throw new IllegalArgumentException("We cannot create a thumbnail for a NULL input stream");
        }

        mimeType = MimeTypeHelper.sanitizeMimeType(mimeType);

        ImageReader reader = null;
        ImageWriter writer = null;
        try (MemoryCacheImageInputStream mciis = new MemoryCacheImageInputStream(imageData)){
            reader = getImageReader(mimeType);
            writer = getImageWriter(mimeType);
            reader.setInput(mciis);

            double originalWidth = reader.getWidth(FIRST_IMAGE_IN_FILE);
            double originalHeight = reader.getHeight(FIRST_IMAGE_IN_FILE);
            double resizeRatio = calcResizeRatio(maxSize, originalWidth, originalHeight);

            int resizeWidth = (int) (originalWidth * resizeRatio);
            int resizeHeight = (int) (originalHeight * resizeRatio);

            BufferedImage originalImage = reader.read(FIRST_IMAGE_IN_FILE);
            BufferedImage scaledImage;
            if (resizeRatio < 1.0d) {
                scaledImage = getScaledInstance(originalImage, resizeWidth, resizeHeight,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
            } else {
                scaledImage = getScaledInstance(originalImage, resizeWidth, resizeHeight,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR, false);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(out);
            writer.setOutput(ios);
            writer.write(scaledImage);
            ios.flush();
            ios.close();

            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException | UnsupportedMimeTypeException e) {
            throw new GalleryException("Could not resize image", e);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
            if (writer != null) {
                writer.dispose();
            }
            IOUtils.closeQuietly(imageData);
        }
    }

    private double calcResizeRatio(int maxSize, double originalWidth, double originalHeight) {
        double resizeRatio;
        if (originalHeight > originalWidth) {
            resizeRatio = maxSize / originalHeight;
        } else {
            resizeRatio = maxSize / originalWidth;
        }
        return resizeRatio;
    }

    private ImageReader getImageReader(String mimeType) throws UnsupportedMimeTypeException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType(mimeType);
        if (readers == null || !readers.hasNext()) {
            log.warn("Unsupported mimetype, cannot read: {}", mimeType);
            throw new UnsupportedMimeTypeException(mimeType);
        }
        return readers.next();
    }

    private ImageWriter getImageWriter(String mimeType) throws UnsupportedMimeTypeException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(mimeType);
        if (writers == null || !writers.hasNext()) {
            log.warn("Unsupported mimetype, cannot write: {}", mimeType);
            throw new UnsupportedMimeTypeException(mimeType);
        }
        return writers.next();
    }

    /**
    * Convenience method that returns a scaled instance of the provided {@code
    * BufferedImage}.
    *
    * @param img
    *            the original image to be scaled
    * @param targetWidth
    *            the desired width of the scaled instance, in pixels
    * @param targetHeight
    *            the desired height of the scaled instance, in pixels
    * @param hint
    *            one of the rendering hints that corresponds to {@code
    *            RenderingHints.KEY_INTERPOLATION} (e.g. {@code
    *            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR}, {@code
    *            RenderingHints.VALUE_INTERPOLATION_BILINEAR}, {@code
    *            RenderingHints.VALUE_INTERPOLATION_BICUBIC})
    * @param higherQuality
    *            if true, this method will use a multi-step scaling technique
    *            that provides higher quality than the usual one-step technique
    *            (only useful in downscaling cases, where {@code targetWidth}
    *            or {@code targetHeight} is smaller than the original
    *            dimensions, and generally only when the {@code BILINEAR} hint
    *            is specified)
    * @return a scaled version of the original {@code BufferedImage}
    */
    public BufferedImage getScaledInstance(BufferedImage img, int targetWidth, int targetHeight, Object hint,
            boolean higherQuality) {

        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;

        BufferedImage ret = img;

        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }

    @Override
    public Dimension getDesiredResourceDimension(Node node) throws RepositoryException {
        if(node.isNodeType(HippoNodeType.NT_RESOURCE)) {
            try {
                InputStream imageData = node.getProperty(JcrConstants.JCR_DATA).getBinary().getStream();
                ImageReader reader = getImageReader(node.getProperty(JcrConstants.JCR_MIMETYPE).getString());
                MemoryCacheImageInputStream imageInputStream = new MemoryCacheImageInputStream(imageData);
                reader.setInput(imageInputStream);
                double originalWidth = reader.getWidth(FIRST_IMAGE_IN_FILE);
                double originalHeight = reader.getHeight(FIRST_IMAGE_IN_FILE);
                double resizeRatio = calcResizeRatio(thumbnailSize, originalWidth, originalHeight);
                int resizeWidth = (int)(originalWidth * resizeRatio);
                int resizeHeight = (int)(originalHeight * resizeRatio);
                return new Dimension(resizeWidth, resizeHeight);
            } catch (IOException | UnsupportedMimeTypeException ex) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static class UnsupportedMimeTypeException extends Exception {
        private static final long serialVersionUID = 1L;

        public UnsupportedMimeTypeException(String mimeType) {
            super(mimeType + " mime type not supported");
        }
    }

    @Override
    public void makeImage(Node node, InputStream istream, String mimeType, String fileName) throws GalleryException,
            RepositoryException {
        try {
            Item item = JcrHelper.getPrimaryItem(node);
            if (!item.isNode()) {
                throw new GalleryException("Primary item is not a node");
            }
            Node primaryChild = (Node) item;
            if (primaryChild.isNodeType("hippo:resource")) {
                ResourceHelper.setDefaultResourceProperties(primaryChild, mimeType, istream, fileName);
                if (MimeTypeHelper.isPdfMimeType(mimeType)) {
                    InputStream dataInputStream = primaryChild.getProperty(JcrConstants.JCR_DATA).getBinary().getStream();
                    ResourceHelper.handlePdfAndSetHippoTextProperty(primaryChild,dataInputStream);
                } else if (node.hasProperty(HIPPO_TEXT)) {
                    node.getProperty(HIPPO_TEXT).remove();
                }
            }
            for (NodeDefinition childDef : node.getPrimaryNodeType().getChildNodeDefinitions()) {
                if (childDef.getDefaultPrimaryType() != null
                        && childDef.getDefaultPrimaryType().isNodeType("hippo:resource")) {
                    makeRegularImage(node, childDef.getName(), primaryChild.getProperty(JcrConstants.JCR_DATA).getBinary().getStream(),
                            primaryChild.getProperty(JcrConstants.JCR_MIMETYPE).getString(), primaryChild.getProperty(
                                    JcrConstants.JCR_LASTMODIFIED).getDate());
                }
            }
            makeThumbnailImage(primaryChild, primaryChild.getProperty(JcrConstants.JCR_DATA).getBinary().getStream(), primaryChild
                    .getProperty(JcrConstants.JCR_MIMETYPE).getString());
        } catch (ItemNotFoundException ex) {
            // deliberate ignore
        }
    }

    protected void makeRegularImage(Node node, String name, InputStream istream, String mimeType, Calendar lastModified)
            throws RepositoryException {
        if (!node.hasNode(name)) {
            Node child = node.addNode(name);
            child.setProperty(JcrConstants.JCR_DATA, getValueFactory(node).createBinary(istream));
            child.setProperty(JcrConstants.JCR_MIMETYPE, mimeType);
            child.setProperty(JcrConstants.JCR_LASTMODIFIED, lastModified);
        }
    }

    protected void makeThumbnailImage(Node node, InputStream resourceData, String mimeType) throws RepositoryException,
            GalleryException {
        if (mimeType.startsWith("image")) {
            InputStream thumbNail = createThumbnail(resourceData, thumbnailSize, mimeType);
            node.setProperty(JcrConstants.JCR_DATA, getValueFactory(node).createBinary(thumbNail));
        } else {
            node.setProperty(JcrConstants.JCR_DATA, getValueFactory(node).createBinary(resourceData));
        }
        node.setProperty(JcrConstants.JCR_MIMETYPE, mimeType);
    }

    @Override
    public void initGalleryResource(Node node, InputStream data, String mimeType, String fileName, Calendar lastModified)
            throws GalleryException, RepositoryException {
        node.setProperty(JcrConstants.JCR_MIMETYPE, mimeType);
        node.setProperty(JcrConstants.JCR_DATA, getValueFactory(node).createBinary(data));
        node.setProperty(JcrConstants.JCR_LASTMODIFIED, lastModified);
    }

    private ValueFactory getValueFactory(Node node) throws RepositoryException {
        return ResourceHelper.getValueFactory(node);
    }

    @Override
    public boolean isUpscalingEnabled(Node node) throws GalleryException, RepositoryException {
        return true;
    }

    @Override
    public Map<String, ScalingParameters> getScalingParametersMap() {
        return new HashMap<>();
    }


    /**
     * Get the gallery processor service identified by the parameter {@link GalleryProcessor#GALLERY_PROCESSOR_ID} in
     * the plugin config. If no service id configuration is found, the service with id
     * {@link GalleryProcessor#DEFAULT_GALLERY_PROCESSOR_ID} is used.
     */
    public static GalleryProcessor getGalleryProcessor(final IPluginContext pluginContext,
                                                       final IPluginConfig pluginConfig) {
        return getGalleryProcessor(pluginContext, pluginConfig, DEFAULT_GALLERY_PROCESSOR_ID);
    }

    /**
     * Get the gallery processor service specified by the parameter {@link GalleryProcessor#GALLERY_PROCESSOR_ID} in the
     * plugin config. If no service id configuration is found, the service with id <code>defaultServiceId</code> is used.
     * If the service with this default id is not found, a new instance of the {@link DefaultGalleryProcessor} is returned.
     */
    public static GalleryProcessor getGalleryProcessor(final IPluginContext pluginContext,
                                                       final IPluginConfig pluginConfig,
                                                       final String defaultServiceId) {

        String serviceId = pluginConfig.getString(GALLERY_PROCESSOR_ID, defaultServiceId);
        GalleryProcessor processor = pluginContext.getService(serviceId, GalleryProcessor.class);

        if (processor == null) {
            processor = new DefaultGalleryProcessor();
            log.warn("Cannot load gallery processor service with id '{}', using the default service '{}'",
                    serviceId, processor.getClass().getName());
        }
        return processor;

    }
}
