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

import java.io.InputStream;

public abstract class AbstractScaleImageOperation extends AbstractImageOperation {

    private final ScalingParameters parameters;

    protected AbstractScaleImageOperation(final ScalingParameters parameters) {
        this.parameters = parameters;
    }

    protected ScalingParameters getParameters() {
        return parameters;
    }

    /**
     * @deprecated Use {@link ScalingParameters#getCompressionQuality()} ()} instead
     */
    @Deprecated
    public float getCompressionQuality() {
        return parameters.getCompressionQuality();
    }

    /**
     * @return the scaled image data
     *
     * @deprecated use {@link ImageOperationResult#getData()} instead
     */
    @Deprecated
    public InputStream getScaledData() {
        return getResult().getData();
    }

    /**
     * @return the width of this scaled image
     *
     * @deprecated use {@link ImageOperationResult#getWidth()} instead
     */
    @Deprecated
    public int getScaledWidth() {
        return getResult().getWidth();
    }

    /**
     * @return the height of this scaled image
     * @deprecated Use {@link ImageOperationResult#getHeight()} instead
     */
    @Deprecated
    public int getScaledHeight() {
        return getResult().getHeight();
    }

    protected double calculateResizeRatio(final double originalWidth, final double originalHeight) {
        final ScalingParameters params = getParameters();
        return ImageUtils.determineResizeRatio(originalWidth, originalHeight, params.getWidth(), params.getHeight());
    }

    /**
     * @deprecated Use {@link ImageUtils#determineResizeRatio(double, double, int, int)} instead
     */
    @Deprecated
    protected double calculateResizeRatio(final double originalWidth, final double originalHeight,
                                          final int targetWidth, final int targetHeight) {
        return ImageUtils.determineResizeRatio(originalWidth, originalHeight, targetWidth, targetHeight);
    }

}
