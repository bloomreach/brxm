/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageReader;

public class CropAndScaleImageOperation extends ScaleImageOperation {

    public CropAndScaleImageOperation(final ScalingParameters parameters) {
        super(parameters);
    }

    @Override
    protected BufferedImage processImage(final ImageReader reader, final int originalWidth, final int originalHeight) throws IOException {
        final ScalingParameters params = getParameters();
        final Rectangle cropArea = calculateCropArea(originalWidth, originalHeight, params.getWidth(), params.getHeight());
        final Dimension variantDimension = new Dimension(params.getWidth(), params.getHeight());
        final Dimension targetDimension = ImageUtils.normalizeDimension(cropArea.getSize(), variantDimension);

        final BufferedImage original = reader.read(0);
        final BufferedImage croppedImage = ImageUtils.cropImage(original, cropArea);
        return ImageUtils.scaleImage(croppedImage, targetDimension.width, targetDimension.height, params.getStrategy());
    }

    private Rectangle calculateCropArea(final double originalWidth, final double originalHeight,
                                        final double variantWidth, final double variantHeight) {

        final double originalAspectRatio = originalWidth / originalHeight;
        final double variantAspectRatio = variantWidth / variantHeight;

        if (originalAspectRatio > variantAspectRatio) {
            // cut off left and right
            final int adjustedOriginalWidth = (int) ((originalHeight / variantHeight) * variantWidth);
            final int cutOffWidth = (int) ((originalWidth - adjustedOriginalWidth) / 2);
            return new Rectangle(cutOffWidth, 0, adjustedOriginalWidth, (int) originalHeight);
        } else {
            // cut off top and bottom
            final int adjustedOriginalHeight = (int) ((originalWidth / variantWidth) * variantHeight);
            final int cutOffHeight = (int) ((originalHeight - adjustedOriginalHeight) / 2);
            return new Rectangle(0, cutOffHeight, (int) originalWidth, adjustedOriginalHeight);
        }

    }
}
