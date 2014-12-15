/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ImageCropSettingsTest {

    // unbounded width and height at the same time is not supported

    @Test
    public void testFixedDimensionUpscaling() {
        ImageCropSettings imageCropSettings = new ImageCropSettings("", "", new Dimension(1000, 2000), new Dimension(100, 200), new Dimension(10, 20), true);
        assertEquals(ImageCropSettings.FIXED_BOTH, imageCropSettings.getFixedDimension());
        assertEquals(16,imageCropSettings.getMinimumWidth());
        assertEquals(16,imageCropSettings.getMinimumHeight());
    }

    @Test
    public void testFixedDimensionNoUpscaling() {
        ImageCropSettings imageCropSettings = new ImageCropSettings("", "", new Dimension(1000, 2000), new Dimension(100, 200), new Dimension(10, 20), false);
        assertEquals(ImageCropSettings.FIXED_BOTH, imageCropSettings.getFixedDimension());
        assertEquals(100,imageCropSettings.getMinimumWidth());
        assertEquals(200,imageCropSettings.getMinimumHeight());
    }

    @Test
    public void testFixedWidthUpscaling() {
        ImageCropSettings imageCropSettings = new ImageCropSettings("", "", new Dimension(1000, 2000), new Dimension(100, 0), new Dimension(10, 20), true);
        assertEquals(ImageCropSettings.FIXED_WIDTH, imageCropSettings.getFixedDimension());
        assertEquals(16,imageCropSettings.getMinimumWidth());
        assertEquals(16,imageCropSettings.getMinimumHeight());
    }

    @Test
    public void testFixedWidthNoUpscaling() {
        ImageCropSettings imageCropSettings = new ImageCropSettings("", "", new Dimension(1000, 2000), new Dimension(100, 0), new Dimension(10, 20), false);
        assertEquals(ImageCropSettings.FIXED_WIDTH, imageCropSettings.getFixedDimension());
        assertEquals(100,imageCropSettings.getMinimumWidth());
        assertEquals(16,imageCropSettings.getMinimumHeight());
    }

    @Test
    public void testFixedHeightUpscaling() {
        ImageCropSettings imageCropSettings = new ImageCropSettings("", "", new Dimension(1000, 2000), new Dimension(0, 200), new Dimension(10, 20), true);
        assertEquals(ImageCropSettings.FIXED_HEIGHT, imageCropSettings.getFixedDimension());
        assertEquals(16,imageCropSettings.getMinimumWidth());
        assertEquals(16,imageCropSettings.getMinimumHeight());
    }

    @Test
    public void testFixedHeightNoUpscaling() {
        ImageCropSettings imageCropSettings = new ImageCropSettings("", "", new Dimension(1000, 2000), new Dimension(0, 200), new Dimension(10, 20), false);
        assertEquals(ImageCropSettings.FIXED_HEIGHT, imageCropSettings.getFixedDimension());
        assertEquals(16,imageCropSettings.getMinimumWidth());
        assertEquals(200,imageCropSettings.getMinimumHeight());
    }

}
