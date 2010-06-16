/*
 *  Copyright 2008 Hippo.
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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * @param imageData the original image data
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

        //IE uploads jpeg files with the non-standard mimetype image/pjpeg for which ImageIO 
        //doesn't have an ImageReader. Simply replacing the mimetype with image/jpeg solves this.
        //For more info see http://www.iana.org/assignments/media-types/image/ and 
        //http://groups.google.com/group/comp.infosystems.www.authoring.images/msg/7706603e4bd1d9d4?hl=en
        if (mimeType.equals(ResourceHelper.MIME_IMAGE_PJPEG)) {
            mimeType = ResourceHelper.MIME_IMAGE_JPEG;
        }

        ImageReader reader = null;
        ImageWriter writer = null;
        try {
            reader = getImageReader(mimeType);
            writer = getImageWriter(mimeType);

            MemoryCacheImageInputStream mciis = new MemoryCacheImageInputStream(imageData);
            reader.setInput(mciis);

            double originalWidth = reader.getWidth(FIRST_IMAGE_IN_FILE);
            double originalHeight = reader.getHeight(FIRST_IMAGE_IN_FILE);
            double resizeRatio = calcResizeRatio(maxSize, originalWidth, originalHeight);

            BufferedImage originalImage = reader.read(FIRST_IMAGE_IN_FILE);
            BufferedImage scaledImage = createScaledImage(originalWidth, originalHeight, resizeRatio, originalImage);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(out);
            writer.setOutput(ios);
            writer.write(scaledImage);
            ios.flush();
            ios.close();
            mciis.close();

            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new GalleryException("Could not resize image", e);
        } catch (UnsupportedMimeTypeException e) {
            throw new GalleryException("Could not resize image", e);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
            if (writer != null) {
                writer.dispose();
            }
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

    private BufferedImage createScaledImage(double originalWidth, double originalHeight, double ratio,
            BufferedImage originalImage) {
        AffineTransform transformation;
        BufferedImage scaledImage;
        if (ratio < 1.0d) {
            transformation = AffineTransform.getScaleInstance(ratio, ratio);
            double scaledWidth = originalWidth * ratio;
            double scaledHeight = originalHeight * ratio;
            scaledImage = new BufferedImage((int) scaledWidth, (int) scaledHeight, BufferedImage.TYPE_INT_RGB);
        } else {
            transformation = new AffineTransform();
            scaledImage = new BufferedImage((int) originalWidth, (int) originalHeight, BufferedImage.TYPE_INT_RGB);
        }

        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.drawImage(originalImage, transformation, null);
        g2d.dispose();
        return scaledImage;
    }

    public static class UnsupportedMimeTypeException extends Exception {
        private static final long serialVersionUID = 1L;

        public UnsupportedMimeTypeException(String mimeType) {
            super(mimeType + " mime type not supported");
        }
    }

    public void makeImage(Node node, InputStream istream, String mimeType, String filename) throws GalleryException,
            RepositoryException {
        Node primaryChild = null;
        try {
            Item item = node.getPrimaryItem();
            if (!item.isNode()) {
                throw new GalleryException("Primary item is not a node");
            }
            primaryChild = (Node) item;
            if (primaryChild.isNodeType("hippo:resource")) {
                primaryChild.setProperty("jcr:mimeType", mimeType);
                primaryChild.setProperty("jcr:data", istream);
            }
            ResourceHelper.validateResource(primaryChild, filename);
            for (NodeDefinition childDef : node.getPrimaryNodeType().getChildNodeDefinitions()) {
                if (childDef.getDefaultPrimaryType() != null
                        && childDef.getDefaultPrimaryType().isNodeType("hippo:resource")) {
                    makeRegularImage(node, childDef.getName(), primaryChild.getProperty("jcr:data").getStream(),
                            primaryChild.getProperty("jcr:mimeType").getString(), primaryChild.getProperty(
                                    "jcr:lastModified").getDate());
                }
            }
            makeThumbnailImage(primaryChild, primaryChild.getProperty("jcr:data").getStream(), primaryChild
                    .getProperty("jcr:mimeType").getString());
        } catch (ItemNotFoundException ex) {
            // deliberate ignore
        }
    }

    protected void makeRegularImage(Node node, String name, InputStream istream, String mimeType, Calendar lastModified)
            throws RepositoryException {
        if (!node.hasNode(name)) {
            Node child = node.addNode(name);
            child.setProperty("jcr:data", istream);
            child.setProperty("jcr:mimeType", mimeType);
            child.setProperty("jcr:lastModified", lastModified);
        }
    }

    protected void makeThumbnailImage(Node node, InputStream resourceData, String mimeType) throws RepositoryException,
            GalleryException {
        if (mimeType.startsWith("image")) {
            InputStream thumbNail = createThumbnail(resourceData, thumbnailSize, mimeType);
            node.setProperty("jcr:data", thumbNail);
        } else {
            node.setProperty("jcr:data", resourceData);
        }
        node.setProperty("jcr:mimeType", mimeType);
    }

}
