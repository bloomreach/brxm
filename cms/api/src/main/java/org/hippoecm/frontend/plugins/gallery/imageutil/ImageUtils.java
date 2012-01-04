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
package org.hippoecm.frontend.plugins.gallery.imageutil;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;

/**
 * Generic image manipulation utility methods.
 */
public class ImageUtils {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * Prevent instantiation
     */
    protected ImageUtils() {
        // do nothing
    }

    /**
     * Fixes issues for certain image MIME types.
     *
     * @param mimeType the MIME type to fix
     *
     * @return the fixed MIME type
     */
    public static String fixMimeType(String mimeType) {
        if (mimeType.equals(ResourceHelper.MIME_IMAGE_PJPEG)) {
            // IE uploads JPEG files with the non-standard MIME type image/pjpeg for which ImageIO
            // doesn't have an ImageReader. Simply replacing the MIME type with image/jpeg solves this.
            // For more info see http://www.iana.org/assignments/media-types/image/ and
            // http://groups.google.com/group/comp.infosystems.www.authoring.images/msg/7706603e4bd1d9d4?hl=en
            return ResourceHelper.MIME_IMAGE_JPEG;
        } else {
            // nothing to fix
            return mimeType;
        }
    }

    /**
     * Returns an image reader for a MIME type.
     *
     * @param mimeType MIME type
     * @return an image reader for the given MIME type, or <code>null</code> if no image reader could be created
     * for the given MIME type.
     */
    public static ImageReader getImageReader(String mimeType) {
        String fixedMimeType = ImageUtils.fixMimeType(mimeType);
        Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType(fixedMimeType);
        if (readers == null || !readers.hasNext()) {
            return null;
        }
        return readers.next();
    }

    /**
     * Returns an image writer for a MIME type.
     *
     * @param mimeType MIME type
     * @return an image writer for the given MIME type, or <code>null</code> if no image writer could be created
     * for the given MIME type.
     */
    public static ImageWriter getImageWriter(String mimeType) {
        String fixedMimeType = ImageUtils.fixMimeType(mimeType);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(fixedMimeType);
        if (writers == null || !writers.hasNext()) {
            return null;
        }
        return writers.next();
    }

    /**
     * Returns the data of a {@link BufferedImage} as a binary output stream. If the image is <code>null</code>,
     * a stream of zero bytes is returned.
     *
     * @param writer the writer to use for writing the image data.
     * @param image the image to write.
     *
     * @return an output stream with the data of the given image.
     *
     * @@throws IOException when creating the binary output stream failed.
     */
    public static ByteArrayOutputStream writeImage(ImageWriter writer, BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        if (image != null) {
            ImageOutputStream ios = null;
            try {
                ios = ImageIO.createImageOutputStream(out);
                writer.setOutput(ios);
                writer.write(image);
                ios.flush();
            } finally {
                if (ios != null) {
                    ios.close();
                }
            }
        }

        return out;
    }

    /**
     * Returns a scaled instance of the provided {@link BufferedImage}.
     *
     * @param img
     *            the original image to be scaled
     * @param targetWidth
     *            the desired width of the scaled instance, in pixels
     * @param targetHeight
     *            the desired height of the scaled instance, in pixels
     * @param hint
     *            one of the rendering hints that corresponds to {@link RenderingHints#KEY_INTERPOLATION}
     *            (e.g. {@link RenderingHints#VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *            {@link RenderingHints#VALUE_INTERPOLATION_BILINEAR},
     *            {@link RenderingHints#VALUE_INTERPOLATION_BICUBIC})
     * @param highQuality
     *            if true, this method will use a multi-step scaling technique that provides higher quality than the
     *            usual one-step technique (only useful in downscaling cases, where {@code targetWidth} or
     *            {@code targetHeight} is smaller than the original dimensions, and generally only when the
     *            {@code BILINEAR} hint is specified)
     *
     * @return a scaled version of the original {@code BufferedImage}, or <code>null</code> if either
     * the target width or target height is 0 or less.
     */
    public static BufferedImage scaleImage(BufferedImage img, int targetWidth, int targetHeight, Object hint,
                                           boolean highQuality) {
        return scaleImage(img, 0, 0, img.getWidth(), img.getHeight(), targetWidth, targetHeight, hint, highQuality);
    }

    public static BufferedImage scaleImage(BufferedImage img, int xOffset, int yOffset, int sourceWidth, int sourceHeight, int targetWidth, int targetHeight, Object hint,
                                           boolean highQuality) {

        if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) {
            return null;
        }

        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;

        BufferedImage result = img;
        if (xOffset != 0 || yOffset != 0 || sourceWidth != img.getWidth() || sourceHeight != img.getHeight()) {
            result = result.getSubimage(xOffset, yOffset, sourceWidth, sourceHeight);
        }

        int width, height;

        if (highQuality) {
            // Use the multiple step technique: start with original size, then scale down in multiple passes with
            // drawImage() until the target size is reached
            width = img.getWidth();
            height = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original size to target size with a single drawImage() call
            width = targetWidth;
            height = targetHeight;
        }

        do {
            if (highQuality && width > targetWidth) {
                width /= 2;
            }
            width = Math.max(width, targetWidth);

            if (highQuality && height > targetHeight) {
                height /= 2;
            }
            height = Math.max(height, targetHeight);

            BufferedImage tmp = new BufferedImage(width, height, type);

            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(result, 0, 0, width, height, null);
            g2.dispose();

            result = tmp;
        } while (width != targetWidth || height != targetHeight);

        return result;
    }
}
