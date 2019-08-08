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

import java.io.Serializable;

/**
 * Parameters for a scaling operation: the width and height of the bounding box, and whether to do
 * upscaling or not.
 */
public class ScalingParameters implements Serializable {

    public static class Builder {
        // required parameters
        private final int width;
        private final int height;

        // optional parameters, set to default values
        private boolean upscaling = false;
        private boolean cropping = false;
        private ImageUtils.ScalingStrategy strategy = ImageUtils.ScalingStrategy.QUALITY;
        private float compressionQuality = 1f;

        /**
         * Creates a scaled version of an image. The given scaling parameters define a bounding box with a certain width
         * and height.
         *
         * @param width  the width of the bounding box
         * @param height the height of the bounding box
         */
        public Builder(final int width, final int height) {
            this.width = width;
            this.height = height;
        }

        /**
         * Sets upscaling to true. Images that are smaller than the variant configuration are scaled up.
         */
        public ScalingParameters.Builder upscaling() {
            this.upscaling = true;
            return this;
        }

        /**
         * Sets cropping to true. The original is cropped to fill the variant dimensions. The upscaling setting is
         * ignored, since cropping implies upscaling.
         */
        public ScalingParameters.Builder cropping() {
            this.cropping = true;
            return this;
        }

        /**
         * Sets a scaling strategy. When not set the default {@code ImageUtils.ScalingStrategy.QUALITY} is used.
         */
        public ScalingParameters.Builder strategy(ImageUtils.ScalingStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        /**
         * Sets the compression quality. When not set the default 1 is used.
         */
        public ScalingParameters.Builder compressionQuality(final float compressionQuality) {
            this.compressionQuality = compressionQuality;
            return this;
        }

        public ScalingParameters build() {
            return new ScalingParameters(width, height, upscaling, cropping, strategy, compressionQuality);
        }
    }

    private final int width;
    private final int height;
    private final boolean upscaling;
    private final boolean cropping;
    private final ImageUtils.ScalingStrategy strategy;
    private final float compressionQuality;

    /**
     * Creates a set of scaling parameters: the width and height of the bounding box, and whether to
     * do upscaling. A width or height of 0 or less means 'unspecified'.
     *
     * @param width the width of the bounding box
     * @param height the height of the bounding box
     * @param upscaling whether to do upscaling of images that are smaller than the bounding box
     *
     * @deprecated Use {@link Builder} instead
     */
    @Deprecated
    public ScalingParameters(int width, int height, boolean upscaling) {
        this(width, height, upscaling, ImageUtils.ScalingStrategy.QUALITY, 1f);
    }

    /**
     * Creates a set of scaling parameters: the width and height of the bounding box, and whether to
     * do upscaling. A width or height of 0 or less means 'unspecified'.
     *
     * @param width the width of the bounding box
     * @param height the height of the bounding box
     * @param upscaling whether to do upscaling of images that are smaller than the bounding box
     * @param strategy the scaling strategy to use
     *
     * @deprecated Use {@link Builder} instead
     */
    @Deprecated
    public ScalingParameters(int width, int height, boolean upscaling, ImageUtils.ScalingStrategy strategy) {
        this(width, height, upscaling, strategy, 1f);
    }

    /**
     * Creates a set of scaling parameters: the width and height of the bounding box, and whether to do upscaling. A
     * width or height of 0 or less means 'unspecified'.
     *
     * @param width     the width of the bounding box
     * @param height    the height of the bounding box
     * @param upscaling whether to do upscaling of images that are smaller than the bounding box
     * @param strategy  the scaling strategy to use
     * @param compressionQuality compression quality
     *
     * @deprecated Use {@link Builder} instead
     */
    @Deprecated
    public ScalingParameters(int width, int height, boolean upscaling, ImageUtils.ScalingStrategy strategy, float compressionQuality) {
        this(width, height, upscaling, false, strategy, compressionQuality);
    }

    /**
     * Creates a set of scaling parameters: the width and height of the bounding box, and whether to do upscaling. A
     * width or height of 0 or less means 'unspecified'.
     *
     * @param width     the width of the bounding box
     * @param height    the height of the bounding box
     * @param upscaling whether to do upscaling of images that are smaller than the bounding box
     * @param cropping  whether to do cropping of images to fill the whole bounding box
     * @param strategy  the scaling strategy to use
     * @param compressionQuality compression quality
     */
    public ScalingParameters(final int width, final int height, final boolean upscaling, final boolean cropping,
                             final ImageUtils.ScalingStrategy strategy, final float compressionQuality) {
        this.width = width;
        this.height = height;
        this.upscaling = upscaling;
        this.cropping = cropping;
        this.strategy = strategy;
        this.compressionQuality = compressionQuality;
    }

    /**
     * @return the width of the bounding box
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the height of the bounding box
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return whether images that are smaller than the specified bounding box should be scaled up or not.
     *
     * @deprecated Use {@link #isUpscaling()} instead
     */
    @Deprecated
    public boolean getUpscaling() {
        return upscaling;
    }

    /**
     * @return whether images that are smaller than the specified bounding box should be scaled up or not.
     */
    public boolean isUpscaling() {
        return upscaling;
    }

    /**
     * @return the scaling strategy to use
     */
    public ImageUtils.ScalingStrategy getStrategy() {
        return strategy;
    }

    /**
     * @return whether cropping should be used to fill the bounding box
     */
    public boolean isCropping() {
        return cropping;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ScalingParameters other = (ScalingParameters) o;

        return width == other.width && height == other.height && upscaling == other.upscaling
                && cropping == other.cropping && strategy == other.strategy;
    }

    public float getCompressionQuality() {
        return compressionQuality;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return width + "x" + height + ",upscaling=" + upscaling + ",cropping=" + cropping
                + ",strategy=" + strategy.name() + ",compression=" + compressionQuality;
    }

}
