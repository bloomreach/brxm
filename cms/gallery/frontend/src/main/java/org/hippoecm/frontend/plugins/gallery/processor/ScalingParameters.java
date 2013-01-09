/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;

import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;

/**
 * Parameters for a scaling operation: the width and height of the bounding box, and whether to do
 * upscaling or not.
 */
public class ScalingParameters implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int width;
    private final int height;
    private final boolean upscaling;
    private final ImageUtils.ScalingStrategy strategy;
    private final float compressionQuality;

    /**
     * Creates a set of scaling parameters: the width and height of the bounding box, and whether to
     * do upscaling. A width or height of 0 or less means 'unspecified'.
     *
     * @param width the width of the bounding box
     * @param height the height of the bounding box
     * @param upscaling whether to do upscaling of images that are smaller than the bounding box
     */
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
     */
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
     */
    public ScalingParameters(int width, int height, boolean upscaling, ImageUtils.ScalingStrategy strategy, float compressionQuality) {
        this.width = width;
        this.height = height;
        this.upscaling = upscaling;
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
     */
    public boolean getUpscaling() {
        return upscaling;
    }

    /**
     * @return the scaling strategy to use
     */
    public ImageUtils.ScalingStrategy getStrategy() {
        return strategy;
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

        return width == other.width && height == other.height && upscaling == other.upscaling && strategy == other.strategy;
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
        return width + "x" + height + ",upscaling=" + upscaling + ",strategy=" + strategy.name() + ",compression=" + compressionQuality;
    }

}
