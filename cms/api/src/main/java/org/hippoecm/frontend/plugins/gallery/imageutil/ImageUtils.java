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
package org.hippoecm.frontend.plugins.gallery.imageutil;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.io.IOUtils;
import org.hippoecm.frontend.editor.plugins.resource.MimeTypeHelper;
import org.imgscalr.Scalr;

/**
 * Generic image manipulation utility methods.
 */
public class ImageUtils {

    /**
     * The available strategies for scaling images.
     */
    public enum ScalingStrategy {
        /**
         * Automatically determine the best strategy to get the nicest looking image in the least amount of time.
         */
        AUTO(Scalr.Method.AUTOMATIC),
        /**
         * Scale as fast as possible. For smaller images (800px in size) this can result in noticeable aliasing but it
         * can be a few order of magnitudes faster than using the {@link #QUALITY} method.
         */
        SPEED(Scalr.Method.SPEED),
        /**
         * Scale faster than when using {@link #QUALITY} but get better results than when using {@link #SPEED}.
         */
        SPEED_AND_QUALITY(Scalr.Method.BALANCED),
        /**
         * Create a nice scaled version of an image at the cost of more processing time. This strategy is most important
         * for smaller pictures (800px or smaller) and less important for larger pictures, as the difference between
         * this strategy and the {@link #SPEED} strategy become less and less noticeable as the source-image size
         * increases.
         */
        QUALITY(Scalr.Method.QUALITY),
        /**
         * Do everything possible to make the scaled image exceptionally good, regardless of the processing time needed.
         * Use this strategy when even @{link #QUALITY} results in bad-looking images.
         */
        BEST_QUALITY(Scalr.Method.ULTRA_QUALITY);

        private Scalr.Method scalrMethod;

        ScalingStrategy(Scalr.Method scalrMethod) {
            this.scalrMethod = scalrMethod;
        }

        private Scalr.Method getScalrMethod() {
            return scalrMethod;
        }
    }

    /**
     * Prevent instantiation
     */
    protected ImageUtils() {
        // do nothing
    }

    /**
     * Returns an image reader for a MIME type.
     *
     * @param aMimeType MIME type
     * @return an image reader for the given MIME type, or <code>null</code> if no image reader could be created for the
     * given MIME type.
     */
    public static ImageReader getImageReader(String aMimeType) {
        String mimeType = MimeTypeHelper.sanitizeMimeType(aMimeType);
        Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType(mimeType);
        if (readers == null || !readers.hasNext()) {
            return null;
        }
        return readers.next();
    }

    /**
     * Returns an image writer for a MIME type.
     *
     * @param aMimeType MIME type
     * @return an image writer for the given MIME type, or <code>null</code> if no image writer could be created for the
     * given MIME type.
     */
    public static ImageWriter getImageWriter(String aMimeType) {
        String mimeType = MimeTypeHelper.sanitizeMimeType(aMimeType);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(mimeType);
        if (writers == null || !writers.hasNext()) {
            return null;
        }
        return writers.next();
    }

    /**
     * Returns the data of a {@link BufferedImage} as a binary output stream. If the image is <code>null</code>, a
     * stream of zero bytes is returned.
     *
     * @param writer             the writer to use for writing the image data.
     * @param image              the image to write.
     * @param compressionQuality a float between 0 and 1 that indicates the desired compression quality. Values lower
     *                           than 0 will be interpreted as 0, values higher than 1 will be interpreted as 1.
     * @return an output stream with the data of the given image.
     * @throws IOException when creating the binary output stream failed.
     */
    public static ByteArrayOutputStream writeImage(ImageWriter writer, BufferedImage image, float compressionQuality) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        if (image != null) {
            ImageOutputStream ios = null;
            try {
                ios = ImageIO.createImageOutputStream(out);
                writer.setOutput(ios);

                // write compressed images with high quality
                final ImageWriteParam writeParam = writer.getDefaultWriteParam();
                if (writeParam.canWriteCompressed()) {
                    String[] compressionTypes = writeParam.getCompressionTypes();
                    if (compressionTypes != null && compressionTypes.length > 0) {
                        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        writeParam.setCompressionType(compressionTypes[0]);

                        // ensure a compression quality between 0 and 1
                        float trimmedCompressionQuality = Math.max(compressionQuality, 0);
                        trimmedCompressionQuality = Math.min(trimmedCompressionQuality, 1f);
                        writeParam.setCompressionQuality(trimmedCompressionQuality);
                    }
                }

                final IIOImage iioImage = new IIOImage(image, null, null);
                writer.write(null, iioImage, writeParam);
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
     * Returns the data of a {@link BufferedImage} as a binary output stream. If the image is <code>null</code>, a
     * stream of zero bytes is returned. The data is written with a compression quality of 1.
     *
     * @param writer the writer to use for writing the image data.
     * @param image  the image to write.
     * @return an output stream with the data of the given image.
     * @throws IOException when creating the binary output stream failed.
     */
    public static ByteArrayOutputStream writeImage(ImageWriter writer, BufferedImage image) throws IOException {
        return writeImage(writer, image, 1f);
    }

    /**
     * Returns a scaled instance of the provided {@link BufferedImage}.
     *
     * @param img          the original image to be scaled
     * @param targetWidth  the desired width of the scaled instance, in pixels
     * @param targetHeight the desired height of the scaled instance, in pixels
     * @param strategy     the strategy to use for scaling the image
     * @return a scaled version of the original {@code BufferedImage}, or <code>null</code> if either the target width
     * or target height is 0 or less.
     */
    public static BufferedImage scaleImage(BufferedImage img, int targetWidth, int targetHeight, ScalingStrategy strategy) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            return null;
        }

        return Scalr.resize(img, strategy.getScalrMethod(), Scalr.Mode.FIT_EXACT, targetWidth, targetHeight);
    }
    
    /**
     * Returns a scaled instance of the provided {@link BufferedImage}.
     *
     * @param original     the original image to be scaled
     * @param rectangle    the rectangle to use for the scaled instance
     * @param targetDimension  the desired dimensions of the scaled instance, in pixels
     * @param hint         one of the rendering hints that corresponds to {@link RenderingHints#KEY_INTERPOLATION} (e.g.
     *                     {@link RenderingHints#VALUE_INTERPOLATION_NEAREST_NEIGHBOR}, {@link
     *                     RenderingHints#VALUE_INTERPOLATION_BILINEAR}, {@link RenderingHints#VALUE_INTERPOLATION_BICUBIC})
     * @param highQuality  if true, this method will use a multi-step scaling technique that provides higher quality
     *                     than the usual one-step technique (only useful in downscaling cases, where {@code
     *                     targetWidth} or {@code targetHeight} is smaller than the original dimensions, and generally
     *                     only when the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}, or <code>null</code> if either the target width
     * or target height is 0 or less.
     */
    public static BufferedImage scaleImage(final BufferedImage original, final Rectangle rectangle,
                                           final Dimension targetDimension, final Object hint, boolean highQuality) {

        if (invalidSizes(rectangle, targetDimension)) {
            return null;
        }

        int type = (original.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;

        BufferedImage result = original;
        if (mustMakeSubImage(original, rectangle)) {
            result = result.getSubimage(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        }

        int width, height;

        if (highQuality) {
            // Use the multiple step technique: start with original size, then scale down in multiple passes with
            // drawImage() until the target size is reached
            width = original.getWidth();
            height = original.getHeight();
        } else {
            // Use one-step technique: scale directly from original size to target size with a single drawImage() call
            width = targetDimension.width;
            height = targetDimension.height;
        }

        do {
            if (highQuality && width > targetDimension.width) {
                width /= 2;
            }
            width = Math.max(width, targetDimension.width);

            if (highQuality && height > targetDimension.height) {
                height /= 2;
            }
            height = Math.max(height, targetDimension.height);

            BufferedImage tmp = new BufferedImage(width, height, type);

            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(result, 0, 0, width, height, null);
            g2.dispose();

            result = tmp;
        } while (width != targetDimension.width || height != targetDimension.height);

        return result;

    }

    private static boolean invalidSizes(final Rectangle rectangle, final Dimension targetDimension) {
        return rectangle.width <= 0 
                || rectangle.height <= 0 
                || targetDimension.width <= 0 
                || targetDimension.height <= 0;
    }

    private static boolean mustMakeSubImage(final BufferedImage original, final Rectangle rectangle) {
        return rectangle.x != 0
                || rectangle.y != 0
                || rectangle.width != original.getWidth()
                || rectangle.height != original.getHeight();
    }

    /**
     * If height or width in the thumbnailDimension is equal to 0 it is a special case. The value 0 represents a value
     * that according to the dimension of the original image.
     * <p>
     * With this function a new dimension is created according to the original dimension
     *
     * @param original dimension of the original image
     * @param variant  dimension of the thumbnail image
     * @return scaled dimension based on width or height value
     */
    public static Dimension handleZeroDimension(final Dimension original, final Dimension variant) {
        final Dimension normalized = new Dimension(variant);
        if (variant.height == 0) {
            int height = (int) ((variant.getWidth() / original.getWidth()) * original.getHeight());
            normalized.setSize(variant.width, height);
        }
        if (variant.width == 0) {
            int width = (int) ((variant.getHeight() / original.getHeight()) * original.getWidth());
            normalized.setSize(width, variant.height);
        }
        return normalized;
    }

    /**
     * Determine if high quality scaling can be performed based on the original and crop area dimensions.
     *
     * @param cropArea size of the area to keep after cropping
     * @param reader   the original image
     * @return true if high quality cropping can be performed
     * @throws IOException when reading the original's sizes fails
     */
    public static boolean isCropHighQuality(final Rectangle cropArea, final ImageReader reader) throws IOException {
        return Math.min(cropArea.width / reader.getWidth(0), cropArea.height / reader.getHeight(0)) < 1.0;
    }

    /**
     * Returns a scaled instance of the provided {@link BufferedImage}.
     *
     * @param img          the original image to be scaled
     * @param xOffset      the X-coordinate of the top-left corner in the source image, in pixels, to use for the scaled
     *                     instance.
     * @param yOffset      the Y-coordinate of the top-left corner in the source image, in pixels, to use for the scaled
     *                     instance.
     * @param sourceWidth  the width of the source image, in pixels relative to xOffset, to use for the scaled
     *                     instance.
     * @param sourceHeight the height of the source image, in pixels relative to yOffset, to use for the scaled
     *                     instance.
     * @param targetWidth  the desired width of the scaled instance, in pixels
     * @param targetHeight the desired height of the scaled instance, in pixels
     * @param hint         one of the rendering hints that corresponds to {@link RenderingHints#KEY_INTERPOLATION} (e.g.
     *                     {@link RenderingHints#VALUE_INTERPOLATION_NEAREST_NEIGHBOR}, {@link
     *                     RenderingHints#VALUE_INTERPOLATION_BILINEAR}, {@link RenderingHints#VALUE_INTERPOLATION_BICUBIC})
     * @param highQuality  if true, this method will use a multi-step scaling technique that provides higher quality
     *                     than the usual one-step technique (only useful in downscaling cases, where {@code
     *                     targetWidth} or {@code targetHeight} is smaller than the original dimensions, and generally
     *                     only when the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}, or <code>null</code> if either the target width
     * or target height is 0 or less.
     * @deprecated Use {@link #scaleImage(BufferedImage, Rectangle, Dimension, Object, boolean)} instead.
     */
    @Deprecated
    public static BufferedImage scaleImage(BufferedImage img, int xOffset, int yOffset, int sourceWidth, int sourceHeight, int targetWidth, int targetHeight, Object hint,
                                           boolean highQuality) {

        return scaleImage(img, new Rectangle(xOffset, yOffset, sourceWidth, sourceHeight), new Dimension(targetWidth, targetHeight), hint, highQuality);
    }

    /**
     * Converts image raster data to a JPEG with RGB color space. Only images with color space CMYK and YCCK are
     * converted, other images are left untouched.
     * <p>
     * Rationale: Java's ImageIO can't process 4-component images and Java2D can't apply AffineTransformOp either, so we
     * have to convert raster data to a JPG with RGB color space.
     * <p>
     * The technique used in this method is due to Mark Stephens, and free for any use. See
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4799903 or http://www.mail-archive.com/java2d-interest@capra.eng.sun.com/msg03247.html
     *
     * @param is         the image data
     * @param colorModel the color model of the image
     * @return the RGB version of the supplied image
     */
    public static InputStream convertToRGB(InputStream is, ColorModel colorModel) throws IOException, UnsupportedImageException {
        if (colorModel != ColorModel.CMYK && colorModel != ColorModel.YCCK) {
            return is;
        }

        // Get an ImageReader.
        ImageInputStream input = ImageIO.createImageInputStream(is);

        try {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (readers == null || !readers.hasNext()) {
                throw new UnsupportedImageException("No ImageReaders found");
            }

            ImageReader reader = readers.next();
            reader.setInput(input);
            Raster raster = reader.readRaster(0, reader.getDefaultReadParam());

            int w = raster.getWidth();
            int h = raster.getHeight();
            byte[] rgb = new byte[w * h * 3];

            switch (colorModel) {
                case YCCK: {
                    float[] Y = raster.getSamples(0, 0, w, h, 0, (float[]) null);
                    float[] Cb = raster.getSamples(0, 0, w, h, 1, (float[]) null);
                    float[] Cr = raster.getSamples(0, 0, w, h, 2, (float[]) null);
                    float[] K = raster.getSamples(0, 0, w, h, 3, (float[]) null);

                    for (int i = 0, imax = Y.length, base = 0; i < imax; i++, base += 3) {
                        float k = 220 - K[i], y = 255 - Y[i], cb = 255 - Cb[i], cr = 255 - Cr[i];

                        double val = y + 1.402 * (cr - 128) - k;
                        val = (val - 128) * .65f + 128;
                        rgb[base] = val < 0.0 ? (byte) 0 : val > 255.0 ? (byte) 0xff : (byte) (val + 0.5);

                        val = y - 0.34414 * (cb - 128) - 0.71414 * (cr - 128) - k;
                        val = (val - 128) * .65f + 128;
                        rgb[base + 1] = val < 0.0 ? (byte) 0 : val > 255.0 ? (byte) 0xff : (byte) (val + 0.5);

                        val = y + 1.772 * (cb - 128) - k;
                        val = (val - 128) * .65f + 128;
                        rgb[base + 2] = val < 0.0 ? (byte) 0 : val > 255.0 ? (byte) 0xff : (byte) (val + 0.5);
                    }
                    break;
                }
                case CMYK: {
                    int[] C = raster.getSamples(0, 0, w, h, 0, (int[]) null);
                    int[] M = raster.getSamples(0, 0, w, h, 1, (int[]) null);
                    int[] Y = raster.getSamples(0, 0, w, h, 2, (int[]) null);
                    int[] K = raster.getSamples(0, 0, w, h, 3, (int[]) null);

                    for (int i = 0, imax = C.length, base = 0; i < imax; i++, base += 3) {
                        int c = 255 - C[i];
                        int m = 255 - M[i];
                        int y = 255 - Y[i];
                        int k = 255 - K[i];
                        float kk = k / 255f;

                        rgb[base] = (byte) (255 - Math.min(255f, c * kk + k));
                        rgb[base + 1] = (byte) (255 - Math.min(255f, m * kk + k));
                        rgb[base + 2] = (byte) (255 - Math.min(255f, y * kk + k));
                    }
                    break;
                }
            }

            // from other image types we know InterleavedRaster's can be
            // manipulated by AffineTransformOp, so create one of those.
            raster = Raster.createInterleavedRaster(
                    new DataBufferByte(rgb, rgb.length),
                    w,
                    h,
                    w * 3,
                    3,
                    new int[]{0, 1, 2},
                    null);

            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
            java.awt.image.ColorModel cm = new ComponentColorModel(cs, false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            BufferedImage convertedImage = new BufferedImage(cm, (WritableRaster) raster, true, null);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(convertedImage, "jpg", os);

            return new ByteArrayInputStream(os.toByteArray());
        } finally {
            IOUtils.closeQuietly(is);
            if (input != null) {
                input.close();
            }
        }
    }
}
