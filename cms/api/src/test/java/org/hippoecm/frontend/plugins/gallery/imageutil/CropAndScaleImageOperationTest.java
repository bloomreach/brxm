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

import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CropAndScaleImageOperationTest {

    @Test
    public void cropLandscapeImageInLandscapeBoundingBoxCutOffLeftRight() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(200, 100).cropping().build();
        final ImageOperationResult result = crop("/test-688x292.jpg", "image/jpeg", parameters);

        checkImageDimensions(result, "image/jpeg", 200, 100);
    }

    @Test
    public void cropUpscaleLandscapeImageInLandscapeBoundingBoxCutOffLeftRight() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(580, 300).cropping().build();
        final ImageOperationResult result = crop("/test-688x292.jpg", "image/jpeg", parameters);

        checkImageDimensions(result, "image/jpeg", 580, 300);
    }

    @Test
    public void cropLandscapeImageInLandscapeBoundingBoxCutOffTopBottom() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(500, 100).cropping().build();
        final ImageOperationResult result = crop("/test-688x292.jpg", "image/jpeg", parameters);

        checkImageDimensions(result, "image/jpeg", 500, 100);
    }
    @Test

    public void cropUpscaleLandscapeImageInLandscapeBoundingBoxCutOffTopBottom() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(900, 300).cropping().build();
        final ImageOperationResult result = crop("/test-688x292.jpg", "image/jpeg", parameters);

        checkImageDimensions(result, "image/jpeg", 900, 300);
    }

    @Test
    public void cropPortraitImageInPortraitBoundingBoxCutOffLeftRight() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(100, 200).cropping().build();
        final ImageOperationResult result = crop("/test-380x428.jpg", "image/jpeg", parameters);

        checkImageDimensions(result, "image/jpeg", 100, 200);
    }

    @Test
    public void cropUpscalePortraitImageInPortraitBoundingBoxCutOffLeftRight() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(400, 600).cropping().build();
        final ImageOperationResult result = crop("/test-380x428.jpg", "image/jpeg", parameters);

        checkImageDimensions(result, "image/jpeg", 400, 600);
    }

    @Test
    public void cropPortraitImageInPortraitBoundingBoxCutOffTopBottom() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(200, 200).cropping().build();
        final ImageOperationResult result = crop("/test-380x428.jpg", "image/jpeg", parameters);

        checkImageDimensions(result, "image/jpeg", 200, 200);
    }

    @Test
    public void cropUpscalePortraitImageInPortraitBoundingBoxCutOffTopBottom() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(400, 400).cropping().build();
        final ImageOperationResult result = crop("/test-380x428.jpg", "image/jpeg", parameters);

        checkImageDimensions(result, "image/jpeg", 400, 400);
    }

    @Test
    public void cropSquareImageInSquareBoundingBox() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(50, 50).cropping().build();
        final ImageOperationResult result = crop("/test-50x50.jpg", "image/jpeg", parameters);

        checkImageDimensions(result, "image/jpeg", 50, 50);
    }

    private static void checkImageDimensions(final ImageOperationResult result, final String mimeType,
                                             final int expectedWidth, final int expectedHeight) throws IOException {
        assertEquals(expectedWidth, result.getWidth());
        assertEquals(expectedHeight, result.getHeight());

        final ImageReader reader = ImageIO.getImageReadersByMIMEType(mimeType).next();
        try (final ImageInputStream iis = ImageIO.createImageInputStream(result.getData())) {
            reader.setInput(iis);
            assertEquals(result.getWidth(), reader.getWidth(0));
            assertEquals(result.getHeight(), reader.getHeight(0));
        }
    }

    private static ImageOperationResult crop(final String inputFile, final String mimeType,
                                             final ScalingParameters parameters) throws GalleryException {
        final InputStream data = readFile(inputFile);
        final ImageOperation operation = new CropAndScaleImageOperation(parameters);
        return operation.run(data, mimeType);
    }

    private static InputStream readFile(final String filePath) {
        return CropAndScaleImageOperationTest.class.getResourceAsStream(filePath);
    }

}
