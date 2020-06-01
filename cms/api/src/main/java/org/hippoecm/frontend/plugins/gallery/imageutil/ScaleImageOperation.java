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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import org.apache.commons.io.IOUtils;
import org.hippoecm.frontend.editor.plugins.resource.MimeTypeHelper;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p> Creates a scaled version of an image. The given scaling parameters define a bounding box with a certain width
 * and height. Images that do not fit in this box (i.e. are too large) are always scaled down such that they do fit. If
 * the aspect ratio of the original image differs from that of the bounding box, either the width or the height of
 * scaled image will be less than that of the box.</p>
 * <p>Unless of course cropping is set to true, then the original images is cropped to fill the bounding box.</p>
 * <p> Smaller images are scaled up in the same way as large images are
 * scaled down, but only if upscaling is true. When upscaling is false and the image is smaller than the bounding box,
 * the scaled image will be equal to the original.</p> <p> If the width or height of the scaling parameters is 0 or
 * less, that side of the bounding box does not exist (i.e. is unbounded). If both sides of the bounding box are
 * unbounded, the scaled image will be equal to the original.</p>
 */
public class ScaleImageOperation extends AbstractScaleImageOperation {

    private static final Logger log = LoggerFactory.getLogger(ScaleImageOperation.class);

    private static final Object scalingLock = new Object();

    /**
     * Creates a scaling operation from a complete set of scaling parameters.
     *
     * @param parameters the parameters to use for the scaling
     */
    public ScaleImageOperation(final ScalingParameters parameters) {
        super(parameters);
    }

    /**
     * Creates a image scaling operation, defined by the bounding box of a certain width and height. The strategy will
     * be set to {@link org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils.ScalingStrategy#QUALITY}, and the
     * maximum memory usage to zero (i.e. undefined).
     *
     * @param width     the width of the bounding box in pixels
     * @param height    the height of the bounding box in pixels
     * @param upscaling whether to enlarge images that are smaller than the bounding box
     * @deprecated Use {@link ScalingParameters.Builder} instead.
     */
    @Deprecated
    public ScaleImageOperation(int width, int height, boolean upscaling) {
        this(width, height, upscaling, ImageUtils.ScalingStrategy.QUALITY);
    }

    /**
     * Creates a image scaling operation, defined by the bounding box of a certain width and height.
     *
     * @param width     the width of the bounding box in pixels
     * @param height    the height of the bounding box in pixels
     * @param upscaling whether to enlarge images that are smaller than the bounding box
     * @param strategy  the strategy to use for scaling the image (e.g. optimize for speed, quality, a trade-off between
     *                  these two, etc.)
     * @deprecated Use {@link ScalingParameters.Builder} instead.
     */
    @Deprecated
    public ScaleImageOperation(int width, int height, boolean upscaling, ImageUtils.ScalingStrategy strategy) {
        this(width, height, upscaling, strategy, 1f);
    }

    /**
     * Creates a image scaling operation, defined by the bounding box of a certain width and height.
     *
     * @param width              the width of the bounding box in pixels
     * @param height             the height of the bounding box in pixels
     * @param upscaling          whether to enlarge images that are smaller than the bounding box
     * @param strategy           the strategy to use for scaling the image (e.g. optimize for speed, quality, a
     *                           trade-off between these two, etc.)
     * @param compressionQuality a float between 0 and 1 indicating the compression quality to use for writing the
     *                           scaled image data.
     * @deprecated Use {@link ScalingParameters.Builder} instead.
     */
    @Deprecated
    public ScaleImageOperation(int width, int height, boolean upscaling, ImageUtils.ScalingStrategy strategy,
                               float compressionQuality) {
        this(width, height, upscaling, false, strategy, compressionQuality);
    }

    /**
     * Creates a image scaling operation, defined by the bounding box of a certain width and height.
     *
     * @param width              the width of the bounding box in pixels
     * @param height             the height of the bounding box in pixels
     * @param upscaling          whether to enlarge images that are smaller than the bounding box
     * @param cropping           whether to crop to original to fill the bounding box
     * @param strategy           the strategy to use for scaling the image (e.g. optimize for speed, quality, a
     *                           trade-off between these two, etc.)
     * @param compressionQuality a float between 0 and 1 indicating the compression quality to use for writing the
     *                           scaled image data.
     * @deprecated Use {@link ScalingParameters.Builder} instead.
     */
    @Deprecated
    public ScaleImageOperation(final int width, final int height, final boolean upscaling, final boolean cropping,
                               final ImageUtils.ScalingStrategy strategy, final float compressionQuality) {
        super(new ScalingParameters(width, height, upscaling, cropping, strategy, compressionQuality));
    }

    @Override
    public ImageOperationResult run(final InputStream data, final String mimeType) throws GalleryException {
        if (MimeTypeHelper.isSvgMimeType(mimeType)) {
            // TODO: Here for backwards compatibility, remove in v14
            log.warn("Deprecation: SVG scaling should be handled by the '{}' operation. " +
                            "In future versions the '{}' will no longer support scaling SVG's.",
                    ScaleSvgOperation.class.getName(), ScaleImageOperation.class.getName());
            final ScaleSvgOperation svgOperation = new ScaleSvgOperation(getParameters());
            return svgOperation.run(data, mimeType);
        }

        return super.run(data, mimeType);
    }

    private boolean isOriginalVariant() {
        final ScalingParameters params = getParameters();
        return params.getWidth() <= 0 && params.getHeight() <= 0;
    }

    /**
     * Creates a scaled version of an image. The given scaling parameters define a bounding box with a certain width and
     * height. Images that do not fit in this box (i.e. are too large) are always scaled down such that they do fit. If
     * the aspect ratio of the original image differs from that of the bounding box, either the width or the height of
     * scaled image will be less than that of the box.</p> <p> Smaller images are scaled up in the same way as large
     * images are scaled down, but only if upscaling is true. When upscaling is false and the image is smaller than the
     * bounding box, the scaled image will be equal to the original.</p> <p> If the width or height of the scaling
     * parameters is 0 or less, that side of the bounding box does not exist (i.e. is unbounded). If both sides of the
     * bounding box are unbounded, the scaled image will be equal to the original.</p>
     *
     * @param data   the original image data
     * @param reader reader for the image data
     * @param writer writer for the image data
     */
    public void execute(InputStream data, ImageReader reader, ImageWriter writer) throws IOException {
        // save the image data in a temporary file so we can reuse the original data as-is if needed without
        // putting all the data into memory
        final File tmpFile = writeToTmpFile(data);
        boolean deleteTmpFile = true;
        log.debug("Stored uploaded image in temporary file {}", tmpFile);

        InputStream dataInputStream = null;
        ImageInputStream imageInputStream = null;

        try {
            dataInputStream = new FileInputStream(tmpFile);
            imageInputStream = new MemoryCacheImageInputStream(dataInputStream);
            reader.setInput(imageInputStream);

            final int originalWidth = reader.getWidth(0);
            final int originalHeight = reader.getHeight(0);
            final long originalSize = tmpFile.length();

            if (isOriginalVariant()) {
                setResult(new AutoDeletingTmpFileInputStream(tmpFile), originalWidth, originalHeight);
                deleteTmpFile = false;
            } else {
                synchronized (scalingLock) {
                    final BufferedImage newImage = processImage(reader, originalWidth, originalHeight);
                    final ByteArrayOutputStream newImageOutputStream = ImageUtils.writeImage(writer, newImage,
                            getParameters().getCompressionQuality());

                    final int newWidth = newImage.getWidth();
                    final int newHeight = newImage.getHeight();
                    final int newSize = newImageOutputStream.size();

                    final InputStream newData;
                    // if the scaled image dimensions equals to the original image dimensions and
                    // scaled image weight is bigger than the original image weight, use original image
                    if (newWidth == originalWidth && newHeight == originalHeight && newSize > originalSize) {
                        newData = new AutoDeletingTmpFileInputStream(tmpFile);
                        deleteTmpFile = false;
                    } else {
                        newData = new ByteArrayInputStream(newImageOutputStream.toByteArray());
                    }
                    setResult(newData, newWidth, newHeight);
                }
            }
        } finally {
            if (imageInputStream != null) {
                imageInputStream.close();
            }
            IOUtils.closeQuietly(dataInputStream);
            if (deleteTmpFile) {
                log.debug("Deleting temporary file {}", tmpFile);
                tmpFile.delete();
            }
        }
    }

    protected BufferedImage processImage(final ImageReader reader, final int originalWidth, final int originalHeight)
            throws IOException {

        final ScalingParameters params = getParameters();
        final BufferedImage imageToScale = reader.read(0);
        final double resizeRatio = calculateResizeRatio(originalWidth, originalHeight);

        final int targetWidth;
        final int targetHeight;
        if (resizeRatio >= 1.0d && !params.isUpscaling()) {
            targetWidth = originalWidth;
            targetHeight = originalHeight;
        } else {
            // scale the image
            targetWidth = (int) Math.max(originalWidth * resizeRatio, 1);
            targetHeight = (int) Math.max(originalHeight * resizeRatio, 1);
        }

        if (log.isDebugEnabled()) {
            log.debug("Resizing image of {}x{} to {}x{}", originalWidth, originalHeight, targetWidth, targetHeight);
        }

        return ImageUtils.scaleImage(imageToScale, targetWidth, targetHeight, params.getStrategy());
    }
}
