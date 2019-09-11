/*
 * Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.gallery.editor.crop;

import java.awt.Dimension;

import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;
import org.hippoecm.frontend.plugins.yui.widget.WidgetSettings;

public class ImageCropSettings extends WidgetSettings {

    private static final int MAX_PREVIEW_RESOLUTION = 1600;
    static final String FIXED_NONE = "";
    static final String FIXED_BOTH = "both";
    static final String FIXED_WIDTH = "width";
    static final String FIXED_HEIGHT = "height";
    private String regionInputId;
    private String imagePreviewContainerId;

    private int initialX = 10;
    private int initialY = 10;
    private int initialWidth = 100;
    private int initialHeight = 100;
    private int minimumWidth = 16;
    private int minimumHeight = 16;

    private int originalWidth;
    private int originalHeight;
    private int thumbnailWidth;
    private int thumbnailHeight;

    private boolean upscalingEnabled;
    private boolean previewVisible;
    private boolean status;
    /**
     * fit image into view when dialog is in full screen mode
     */
    private boolean fitView;

    private String fixedDimension = FIXED_NONE;
    private String thumbnailSizeLabelId = "";

    public ImageCropSettings(final String regionInputId, final String imagePreviewContainerId,
                             final Dimension originalImageDimension, final Dimension configuredDimension,
                             final Dimension thumbnailDimension, final boolean upscalingEnabled,
                             final boolean fitView) {
        this.regionInputId = regionInputId;
        this.imagePreviewContainerId = imagePreviewContainerId;

        this.originalWidth = (int) originalImageDimension.getWidth();
        this.originalHeight = (int) originalImageDimension.getHeight();
        this.thumbnailWidth = (int) thumbnailDimension.getWidth();
        this.thumbnailHeight = (int) thumbnailDimension.getHeight();

        this.initialWidth = this.thumbnailWidth;
        this.initialHeight = this.thumbnailHeight;

        this.upscalingEnabled = upscalingEnabled;
        this.fitView = fitView;
        previewVisible = thumbnailWidth <= MAX_PREVIEW_RESOLUTION;

        if (initialX + configuredDimension.getWidth() >= originalWidth) {
            initialX = 0;
        }
        if (initialY + configuredDimension.getHeight() >= originalHeight) {
            initialY = 0;
        }

        if (configuredDimension.getHeight() > 0 && configuredDimension.getWidth() > 0) {
            fixedDimension = FIXED_BOTH;
            if (!upscalingEnabled) {
                this.minimumHeight = (int) configuredDimension.getHeight();
                this.minimumWidth = (int) configuredDimension.getWidth();
            }
        } else if(configuredDimension.getWidth() > 0 && configuredDimension.getHeight() == 0) {
            fixedDimension = FIXED_WIDTH;
            if (!upscalingEnabled) {
                this.minimumWidth = (int) configuredDimension.getWidth();
            }
        } else if (configuredDimension.getHeight() > 0 && configuredDimension.getWidth() == 0) {
            fixedDimension = FIXED_HEIGHT;
            if (!upscalingEnabled) {
                this.minimumHeight = (int) configuredDimension.getHeight();
            }
        }

        if (configuredDimension.width > originalImageDimension.width || configuredDimension.height > originalImageDimension.height) {
            final double cropFactor = ImageUtils.determineScalingFactor(
                    configuredDimension.width, configuredDimension.height,
                    originalImageDimension.width, originalImageDimension.height
            );

            this.initialWidth = (int) Math.floor(cropFactor * configuredDimension.getWidth());
            this.initialHeight = (int) Math.floor(cropFactor * configuredDimension.getHeight());
        }
    }

    public ImageCropSettings(final String regionInputId, final String imagePreviewContainerId, final Dimension originalImageDimension,
                             final Dimension configuredDimension, final Dimension thumbnailDimension, final boolean upscalingEnabled, final boolean fitView,
                             final String thumbnailSizeLabelId) {
    	this(regionInputId, imagePreviewContainerId, originalImageDimension, configuredDimension, thumbnailDimension,
                upscalingEnabled, fitView);

        this.thumbnailSizeLabelId = thumbnailSizeLabelId;
    }

    public String getRegionInputId() {
        return regionInputId;
    }

    public void setRegionInputId(final String regionInputId) {
        this.regionInputId = regionInputId;
    }

    public String getImagePreviewContainerId() {
        return imagePreviewContainerId;
    }

    public void setImagePreviewContainerId(final String imagePreviewContainerId) {
        this.imagePreviewContainerId = imagePreviewContainerId;
    }

    public int getInitialX() {
        return initialX;
    }

    public void setInitialX(final int initialX) {
        this.initialX = initialX;
    }

    public int getInitialY() {
        return initialY;
    }

    public void setInitialY(final int initialY) {
        this.initialY = initialY;
    }

    public int getInitialWidth() {
        return initialWidth;
    }

    public void setInitialWidth(final int initialWidth) {
        this.initialWidth = initialWidth;
    }

    public int getInitialHeight() {
        return initialHeight;
    }

    public void setInitialHeight(final int initialHeight) {
        this.initialHeight = initialHeight;
    }

    public int getMinimumWidth() {
        return minimumWidth;
    }

    public void setMinimumWidth(final int minimumWidth) {
        this.minimumWidth = minimumWidth;
    }

    public int getMinimumHeight() {
        return minimumHeight;
    }

    public void setMinimumHeight(final int minimumHeight) {
        this.minimumHeight = minimumHeight;
    }

    public int getOriginalWidth() {
        return originalWidth;
    }

    public void setOriginalWidth(final int originalWidth) {
        this.originalWidth = originalWidth;
    }

    public int getOriginalHeight() {
        return originalHeight;
    }

    public void setOriginalHeight(final int originalHeight) {
        this.originalHeight = originalHeight;
    }

    public int getThumbnailWidth() {
        return thumbnailWidth;
    }

    public void setThumbnailWidth(final int thumbnailWidth) {
        this.thumbnailWidth = thumbnailWidth;
    }

    public int getThumbnailHeight() {
        return thumbnailHeight;
    }

    public void setThumbnailHeight(final int thumbnailHeight) {
        this.thumbnailHeight = thumbnailHeight;
    }

    public boolean isUpscalingEnabled() {
        return upscalingEnabled;
    }

    public void setUpscalingEnabled(final boolean upscalingEnabled) {
        this.upscalingEnabled = upscalingEnabled;
    }

    public boolean isPreviewVisible() {
        return previewVisible;
    }

    public void setPreviewVisible(final boolean previewVisible) {
        this.previewVisible = previewVisible;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(final boolean status) {
        this.status = status;
    }

    public String getFixedDimension() {
    	return fixedDimension;
    }

    public void setFixedDimension(final String fixedDimension) {
    	this.fixedDimension = fixedDimension;
    }

    public String getThumbnailSizeLabelId() {
    	return thumbnailSizeLabelId;
    }

    public void setThumbnailSizeLabelId(final String thumbnailSizeLabelId){
    	this.thumbnailSizeLabelId = thumbnailSizeLabelId;
    }

    public boolean isFitView() {
        return fitView;
    }

    public void setFitView(final boolean fitView) {
        this.fitView = fitView;
    }
}
