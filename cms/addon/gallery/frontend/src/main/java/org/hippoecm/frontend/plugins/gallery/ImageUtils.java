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
package org.hippoecm.frontend.plugins.gallery;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
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
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;

public class ImageUtils implements GalleryProcessor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    static final Logger log = LoggerFactory.getLogger(ImageUtils.class);

    private static final int FIRST_IMAGE_IN_FILE = 0;
    
    private static final String MIME_IMAGE_PJPEG = "image/pjpeg";
    private static final String MIME_IMAGE_JPEG = "image/jpeg";

    protected IPluginConfig config;

    public ImageUtils() {
        config = new JavaPluginConfig();
    }

    public ImageUtils(IPluginConfig config) {
        this.config = config;
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
    public InputStream createThumbnail(InputStream imageData, int maxSize, String mimeType) {
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
        if(mimeType.equals(MIME_IMAGE_PJPEG)) {
            mimeType = MIME_IMAGE_JPEG;
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
            log.error(e.getMessage(), e);
        } catch (UnsupportedMimeTypeException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
            if (writer != null) {
                writer.dispose();
            }
        }
        return imageData;
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

    public class UnsupportedMimeTypeException extends Exception {
        private static final long serialVersionUID = 1L;

        public UnsupportedMimeTypeException(String mimeType) {
            super(mimeType + " mime type not supported");
        }
    }

    public static GalleryProcessor galleryProcessor(IPluginConfig pluginConfig) {
        String className = pluginConfig.getString("gallery.postprocess");
        GalleryProcessor postProcessor = null;
        try {
            if(className != null && !className.trim().equals("")) {
                Object instance = Class.forName(className).getConstructor(new Class[] { IPluginConfig.class }).newInstance(pluginConfig);
                if (instance instanceof GalleryProcessor) {
                    postProcessor = (GalleryProcessor) instance;
                } else {
                    log.warn("Incompatible image postprocessor specified "+className);
                }
            }
        } catch (NoSuchMethodException ex) {
            log.warn(ex.getMessage(), ex);
        } catch (InstantiationException ex) {
            log.warn(ex.getMessage(), ex);
        } catch (InvocationTargetException ex) {
            log.warn(ex.getMessage(), ex);
        } catch (IllegalAccessException ex) {
            log.warn(ex.getMessage(), ex);
        } catch (ClassNotFoundException ex) {
            log.warn(ex.getMessage(), ex);
        }
        if (postProcessor == null) {
            postProcessor = new ImageUtils(pluginConfig);
        }
        return postProcessor;
    }

    public void makeImage(Node node, InputStream istream, String mimeType, String filename) throws RepositoryException {
        /*
         *  we make an exception for pdf to hardcode the mapping, as firefox might post the binary with the wrong mimetype,
         *  for example application/application-pdf, see HREPTWO-3168
         */
        if (filename.endsWith(".pdf")) {
            mimeType = "application/pdf";
        }
        Node primaryChild = null;
        try {
            Item item = node.getPrimaryItem();
            if (item.isNode()) {
                primaryChild = (Node)item;
            }
        } catch (ItemNotFoundException ex) {
            // deliberate ignore
        }
        if (primaryChild != null && primaryChild.isNodeType("hippo:resource")) {
            primaryChild.setProperty("jcr:mimeType", mimeType);
            primaryChild.setProperty("jcr:data", istream);
        }
        validateResource(primaryChild, filename);
        for (NodeDefinition childDef : node.getPrimaryNodeType().getChildNodeDefinitions()) {
            if (childDef.getDefaultPrimaryType() != null && childDef.getDefaultPrimaryType().isNodeType("hippo:resource")) {
                makeRegularImage(node, childDef.getName(),
                        primaryChild.getProperty("jcr:data").getStream(),
                        primaryChild.getProperty("jcr:mimeType").getString(),
                        primaryChild.getProperty("jcr:lastModified").getDate());
            }
        }
        makeThumbnailImage(primaryChild, primaryChild.getProperty("jcr:data").getStream(), primaryChild.getProperty("jcr:mimeType").getString());
    }

    public void validateResource(Node resource, String filename) throws ValueFormatException, RepositoryException {
        try {
            String mimeType;
            try {
                mimeType = (resource.hasProperty("jcr:mimeType") ? resource.getProperty("jcr:mimeType").getString() : "");
                mimeType = mimeType.toLowerCase();
                if(mimeType.equals(MIME_IMAGE_PJPEG)) {
                    mimeType = MIME_IMAGE_JPEG;
                }
            } catch (RepositoryException ex) {
                throw new RepositoryException("unexpected error validating mime type", ex);
            }
            if (mimeType.startsWith("image/")) {
                ImageInfo imageInfo = new ImageInfo();
                try {
                    imageInfo.setInput(resource.getProperty("jcr:data").getStream());
                } catch (RepositoryException ex) {
                    throw new RepositoryException("unexpected error validating mime type", ex);
                }
                if (imageInfo.check()) {
                    String imageInfoMimeType = imageInfo.getMimeType();
                    if (imageInfoMimeType == null) {
                        throw new ValueFormatException("impermissable image type content");
                    } else {
                        if(imageInfoMimeType.equals(MIME_IMAGE_PJPEG)) {
                            imageInfoMimeType = MIME_IMAGE_JPEG;
                        }
                        if (!imageInfoMimeType.equalsIgnoreCase(mimeType)) {
                            throw new ValueFormatException("mismatch image mime type");
                        }
                    }
                } else {
                    throw new ValueFormatException("impermissable image type content");
                }
            } else if (mimeType.equals("application/pdf")) {
                String line;
                try {
                    line = new BufferedReader(new InputStreamReader(resource.getProperty("jcr:data").getStream())).readLine().toUpperCase();
                } catch (RepositoryException ex) {
                    throw new RepositoryException("unexpected error validating mime type", ex);
                }
                if (!line.startsWith("%PDF-")) {
                    throw new ValueFormatException("impermissable pdf type content");
                }
            } else if (mimeType.equals("application/postscript")) {
                String line;
                try {
                    line = new BufferedReader(new InputStreamReader(resource.getProperty("jcr:data").getStream())).readLine().toUpperCase();
                } catch (RepositoryException ex) {
                    throw new RepositoryException("unexpected error validating mime type", ex);
                }
                if (!line.startsWith("%!")) {
                    throw new ValueFormatException("impermissable postscript type content");
                }
            } else {
                // This method can be overridden to allow more such checks on content type.  if such an override
                // wants to be really strict and not allow unknown content, the following thrown exception is to be included
                // throw new ValueFormatException("impermissable unrecognized type content");
            }
        } catch (IOException ex) {
            throw new ValueFormatException("impermissable unknown type content");
        }
    }

    protected void makeRegularImage(Node node, String name, InputStream istream, String mimeType, Calendar lastModified) throws RepositoryException {
        if (!node.hasNode(name)) {
            Node child = node.addNode(name);
            child.setProperty("jcr:data", istream);
            child.setProperty("jcr:mimeType", mimeType);
            child.setProperty("jcr:lastModified", lastModified);
        }
    }

    protected void makeThumbnailImage(Node node, InputStream resourceData, String mimeType) throws RepositoryException {
        int thumbnailSize = config.getInt("gallery.thumbnail.size", Gallery.DEFAULT_THUMBNAIL_SIZE);
        if (mimeType.startsWith("image")) {
            InputStream thumbNail = new ImageUtils().createThumbnail(resourceData, thumbnailSize, mimeType);
            node.setProperty("jcr:data", thumbNail);
        } else {
            node.setProperty("jcr:data", resourceData);
        }
        node.setProperty("jcr:mimeType", mimeType);
    }
}
