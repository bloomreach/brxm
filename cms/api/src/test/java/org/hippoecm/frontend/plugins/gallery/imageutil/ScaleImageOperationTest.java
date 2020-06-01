/*
 * Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.gallery.imageutil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.io.IOUtils;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.junit.Test;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link ScaleImageOperation}.
 */
public class ScaleImageOperationTest {

    @Test
    public void scaleLandscapeImageInLandscapeBoundingBox() throws GalleryException, IOException {
        final ImageOperationResult result = scale("/test-688x292.jpg", "image/jpeg", 200, 100);

        checkImageDimensions(result, "image/jpeg", 200, 84);
    }

    @Test
    public void scaleLandscapeImageInPortraitBoundingBox() throws GalleryException, IOException {
        final ImageOperationResult result = scale("/test-688x292.jpg", "image/jpeg", 100, 200);

        checkImageDimensions(result, "image/jpeg", 100, 42);
    }

    @Test
    public void scalePortraitImageInLandscapeBoundingBox() throws GalleryException, IOException {
        final ImageOperationResult result = scale("/test-380x428.jpg", "image/jpeg", 200, 100);

        checkImageDimensions(result, "image/jpeg", 88, 100);
    }

    @Test
    public void scalePortraitImageInPortraitBoundingBox() throws GalleryException, IOException {
        final ImageOperationResult result = scale("/test-380x428.jpg", "image/jpeg", 100, 200);

        checkImageDimensions(result, "image/jpeg", 100, 112);
    }

    @Test
    public void scaleUp() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(500, 500).upscaling().build();
        final ImageOperationResult result = scale("/test-380x428.jpg", "image/jpeg", parameters);

        checkImageDimensions(result, "image/jpeg", 443, 500);
    }

    @Test
    public void scaleToOriginalDimensionsWhenBoundingBoxMatchesOriginalDimensions() throws GalleryException, IOException {
        final ImageOperationResult result = scale("/test-380x428.jpg", "image/jpeg", 380, 428);

        checkImageDimensions(result, "image/jpeg", 380, 428);
    }

    @Test
    public void unboundedBoundingBoxPreservesOriginalData() throws GalleryException, IOException {
        final ImageOperationResult result = scale("/test-380x428.jpg", "image/jpeg", 0, 0);

        final InputStream original = readFile("/test-380x428.jpg");
        assertTrue("Original image data should be stored as-is", IOUtils.contentEquals(original, result.getData()));
    }

    @Test
    public void unboundedBoundingBoxSetsOriginalWidthAndHeight() throws GalleryException, IOException {
        final ImageOperationResult result = scale("/test-380x428.jpg", "image/jpeg", 0, 0);

        checkImageDimensions(result, "image/jpeg", 380, 428);
    }

    @Test
    public void scaleToOriginalDimensionsWhenScalingUpAndUpscalingIsDisabled() throws GalleryException, IOException {
        final ImageOperationResult result = scale("/test-380x428.jpg", "image/jpeg", 500, 500);

        checkImageDimensions(result, "image/jpeg", 380, 428);
    }

    @Test
    public void ensureMinimumWidthOfOne() throws GalleryException, IOException {
        final ImageOperationResult result = scale("/test-1x5000.png", "image/png", 60, 60);

        checkImageDimensions(result, "image/png", 1, 60);
    }

    @Test
    public void ensureMinimumHeightOfOne() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(60, 60).upscaling().build();
        final ImageOperationResult result = scale("/test-5000x1.png", "image/png", parameters);

        checkImageDimensions(result, "image/png", 60, 1);
    }

    @Test
    public void scaleGif() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(200, 100).upscaling().build();
        final ImageOperationResult result = scale("/test-380x428.gif", "image/gif", parameters);

        checkImageDimensions(result, "image/gif", 88, 100);
    }

    /**
     * @deprecated This should be removed in v14
     */
    @Test
    @Deprecated
    public void scaleSvgWarnsAboutDeprecation() throws GalleryException {
        final InputStream data = readFile("/test-SVG.svg");
        final ScaleImageOperation scaleOp = createOperation(200, 100);

        try (final Log4jInterceptor listener = Log4jInterceptor.onWarn().trap(ScaleImageOperation.class).build()) {
            try {
                final ImageOperationResult result = scaleOp.run(data, "image/svg+xml");
                assertEquals(122, result.getWidth());
                assertEquals(100, result.getHeight());

            } finally {
                assertThat(listener.messages().count(), equalTo(1L));
                final Optional<String> first = listener.messages().findFirst();
                assertTrue(first.isPresent());
                assertTrue(first.get().contains("Deprecation:"));
            }
        }
    }

    @Test
    public void scaleJpgWithCompression() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(200, 100).compressionQuality(0.8f).build();
        final ImageOperationResult result = scale("/test-380x428.jpg", "image/jpeg", parameters);

        checkImageDimensions(result, "image/jpeg", 88, 100);
    }

    @Test
    public void compressedJpgIsSmaller() throws GalleryException, IOException {
        final ImageOperationResult result = scale("/test-380x428.jpg", "image/jpeg", 200, 100);
        final byte[] normalData = IOUtils.toByteArray(result.getData());

        final ScalingParameters compressedParameters = new ScalingParameters.Builder(200, 100).compressionQuality(0.5f).build();
        final ImageOperationResult compressedResult = scale("/test-380x428.jpg", "image/jpeg", compressedParameters);
        final byte[] compressedData = IOUtils.toByteArray(compressedResult.getData());

        assertTrue("The compressed scaled image (" + compressedData.length + " bytes) "
                + "should be smaller than the normal scaled image (" + normalData.length + " bytes)",
                compressedData.length < normalData.length);
    }

    @Test
    public void compressionQualityHigherThanOne() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(200, 100).build();
        final ImageOperationResult result = scale("/test-380x428.jpg", "image/jpeg", parameters);

        final ScalingParameters compressedParameters = new ScalingParameters.Builder(200, 100).compressionQuality(100).build();
        final ImageOperationResult compressedResult = scale("/test-380x428.jpg", "image/jpeg", compressedParameters);

        assertTrue("Compression quality higher than 1 should be interpreted as 1",
                IOUtils.contentEquals(result.getData(), compressedResult.getData()));
    }

    @Test
    public void compressionQualityLowerThanZero() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(200, 100).compressionQuality(0).build();
        final ImageOperationResult result = scale("/test-380x428.jpg", "image/jpeg", parameters);

        final ScalingParameters compressedParameters = new ScalingParameters.Builder(200, 100).compressionQuality(-42).build();
        final ImageOperationResult compressedResult = scale("/test-380x428.jpg", "image/jpeg", compressedParameters);

        assertTrue("Compression quality lower than 0 should be interpreted as 0",
                IOUtils.contentEquals(result.getData(), compressedResult.getData()));
    }

    @Test
    public void upscalingBounded() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(800, 600).upscaling().build();
        final ImageOperationResult result = scale("/test-380x428.gif", "image/gif", parameters);

        checkImageDimensions(result, "image/gif", 532, 600);
    }

    @Test
    public void upscalingUnboundedHeight() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(760, 0).upscaling().build();
        final ImageOperationResult result = scale("/test-380x428.gif", "image/gif", parameters);

        checkImageDimensions(result, "image/gif", 760, 856);
    }

    @Test
    public void upscalingUnboundedWidth() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(0, 856 ).upscaling().build();
        final ImageOperationResult result = scale("/test-380x428.gif", "image/gif", parameters);

        checkImageDimensions(result, "image/gif", 760, 856);
    }

    @Test
    public void upscalingUnboundedBothEdges() throws GalleryException, IOException {
        final ImageOperationResult result = scale("/test-380x428.gif", "image/gif", 0, 0);

        checkImageDimensions(result, "image/gif", 380, 428);
    }

    @Test
    public void calculateResizeRatio() {
        double ratio = createOperation(400, 500).calculateResizeRatio(800, 600);
        assertEquals("Resize ratio calculated by bounding-box limited width.", 0.5, ratio, 0.0);

        ratio = createOperation(700, 300).calculateResizeRatio(800, 600);
        assertEquals("Resize ratio calculated by bounding-box limited height.", 0.5, ratio, 0.0);

        ratio = createOperation(400, 0).calculateResizeRatio(800, 600);
        assertEquals("Resize ratio calculated by bounding-box width.", 0.5, ratio, 0.0);

        ratio = createOperation(0, 300).calculateResizeRatio(800, 600);
        assertEquals("Resize ratio calculated by bounding-box height.", 0.5, ratio, 0.0);

        ratio = createOperation(1700, 1200).calculateResizeRatio(800, 600);
        assertEquals("Resize ratio calculated by bounding-box height.", 2.0, ratio, 0.0);

        ratio = createOperation(1600, 1400).calculateResizeRatio(800, 600);
        assertEquals("Resize ratio calculated by bounding-box width.", 2.0, ratio, 0.0);
    }

    @Test
    public void scaledImageDataWeightIsNotBiggerWhenScalingToOriginalDimensions() throws GalleryException, IOException {
        final ImageOperationResult result = scale("/test-380x428.jpg", "image/jpeg", 380, 428);

        final InputStream inputStream = readFile("/test-380x428.jpg");
        assertTrue("Scaled image weight is not higher when scaling to original dimensions",
                IOUtils.toByteArray(result.getData()).length <= IOUtils.toByteArray(inputStream).length);
    }

    @Test
    public void scaledImageDataWeightIsNotBiggerWhenScalingToLargerDimensionsAndUpscalingIsDisabled() throws GalleryException, IOException {
        final ImageOperationResult result = scale("/test-380x428.jpg", "image/jpeg", 500, 500);

        final InputStream inputStream = readFile("/test-380x428.jpg");
        assertTrue("Scaled image weight is not higher when scaling to larger dimensions and upscaling is disabled",
                IOUtils.toByteArray(result.getData()).length <= IOUtils.toByteArray(inputStream).length);
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

    private static ImageOperationResult scale(final String inputFile, final String mimeType,
                                              final int width, final int height) throws GalleryException {
        return scale(inputFile, mimeType, new ScalingParameters.Builder(width, height).build());
    }

    private static ImageOperationResult scale(final String inputFile, final String mimeType,
                                              final ScalingParameters parameters) throws GalleryException {
        final InputStream data = readFile(inputFile);
        final ImageOperation operation = new ScaleImageOperation(parameters);
        return operation.run(data, mimeType);
    }

    private static InputStream readFile(final String filePath) {
        return ScaleImageOperationTest.class.getResourceAsStream(filePath);
    }

    private static ScaleImageOperation createOperation(final int width, final int height) {
        return new ScaleImageOperation(new ScalingParameters.Builder(width, height).build());
    }
}
