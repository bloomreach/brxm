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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link ScaleImageOperation}.
 */
public class ScaleImageOperationTest {

    @Test
    public void scaleLandscapeImageInLandscapeBoundingBox() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-688x292.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        checkImageDimensions(scaleOp, "image/jpeg", 200, 84);
    }

    @Test
    public void scaleLandscapeImageInPortraitBoundingBox() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-688x292.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(100, 200, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        checkImageDimensions(scaleOp, "image/jpeg", 100, 42);
    }

    @Test
    public void scalePortraitImageInLandscapeBoundingBox() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        checkImageDimensions(scaleOp, "image/jpeg", 88, 100);
    }

    @Test
    public void scalePortraitImageInPortraitBoundingBox() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(100, 200, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        checkImageDimensions(scaleOp, "image/jpeg", 100, 112);
    }

    @Test
    public void scaleUp() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(500, 500, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        checkImageDimensions(scaleOp, "image/jpeg", 443, 500);
    }

    @Test
    public void scaleToOriginalDimensionsWhenBoundingBoxMatchesOriginalDimensions() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(380, 428, false, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        checkImageDimensions(scaleOp, "image/jpeg", 380, 428);
    }

    @Test
    public void unboundedBoundingBoxPreservesOriginalData() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(0, 0, false, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        InputStream original = getClass().getResourceAsStream("/test-380x428.jpg");
        assertTrue("Original image data should be stored as-is", IOUtils.contentEquals(original, scaleOp.getScaledData()));
    }

    @Test
    public void unboundedBoundingBoxSetsOriginalWidthAndHeight() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(0, 0, false, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        checkImageDimensions(scaleOp, "image/jpeg", 380, 428);
    }

    @Test
    public void scaleToOriginalDimensionsWhenScalingUpAndUpscalingIsDisabled() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(500, 500, false, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/jpeg");
        checkImageDimensions(scaleOp, "image/jpeg", 380, 428);
    }

    @Test
    public void ensureMinimumWidthOfOne() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-1x5000.png");
        ScaleImageOperation scaleOp = new ScaleImageOperation(60, 60, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/png");
        checkImageDimensions(scaleOp, "image/png", 1, 60);
    }

    @Test
    public void ensureMinimumHeightOfOne() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-5000x1.png");
        ScaleImageOperation scaleOp = new ScaleImageOperation(60, 60, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/png");
        checkImageDimensions(scaleOp, "image/png", 60, 1);
    }

    @Test
    public void scaleGif() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.gif");
        ScaleImageOperation scaleOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/gif");
        checkImageDimensions(scaleOp, "image/gif", 88, 100);
    }

    @Test
    public void scaleSvg() throws GalleryException {
        InputStream data = getClass().getResourceAsStream("/test-SVG.svg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/svg+xml");

        assertEquals(122, scaleOp.getScaledWidth());
        assertEquals(100, scaleOp.getScaledHeight());
    }

    @Test
    public void scaleSvgWithoutDimensionsInBoundingBox() throws GalleryException {
        InputStream data = getClass().getResourceAsStream("/test-SVG-without-dimensions.svg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/svg+xml");

        assertEquals(200, scaleOp.getScaledWidth());
        assertEquals(100, scaleOp.getScaledHeight());
    }

    @Test
    public void scaleSvgWithoutDimensionsAsOriginal() throws GalleryException {
        InputStream data = getClass().getResourceAsStream("/test-SVG-without-dimensions.svg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(0, 0, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/svg+xml");

        assertEquals(0, scaleOp.getScaledWidth());
        assertEquals(0, scaleOp.getScaledHeight());
    }

    @Test
    public void scaleSvgAddsViewboxWhenMissing() throws GalleryException, IOException, ParserConfigurationException, SAXException {
        InputStream data = getClass().getResourceAsStream("/test-SVG-without-viewbox.svg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/svg+xml");

        InputStream scaledData = scaleOp.getScaledData();

        // read svg
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        factory.setFeature("http://xml.org/sax/features/namespaces", false);
        factory.setFeature("http://xml.org/sax/features/validation", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document svgDocument = builder.parse(scaledData);
        final Element svgElement = svgDocument.getDocumentElement();

        assertEquals("SVG without a 'viewBox' attribute should have gotten one set to the original image size",
                "0 0 178.0 145.0", svgElement.getAttribute("viewBox"));
    }

    @Test
    public void scaleSvgRemovesDoctypeFromScaledImage() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-SVG.svg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED);

        scaleOp.execute(data, "image/svg+xml");

        final String scaledSvg = IOUtils.toString(scaleOp.getScaledData(), CharEncoding.UTF_8);
        assertThat(scaledSvg, not(containsString("<!DOCTYPE")));
    }

    @Test
    public void scaleSvgRemovesDoctypeFromOriginalImage() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-SVG.svg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(0, 0, true, ImageUtils.ScalingStrategy.SPEED);

        scaleOp.execute(data, "image/svg+xml");

        final String scaledSvg = IOUtils.toString(scaleOp.getScaledData(), CharEncoding.UTF_8);
        assertThat(scaledSvg, not(containsString("<!DOCTYPE")));
    }

    @Test
    public void scaleSvgRemovesDoctypeFromInvalidSvg() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-SVG-invalid.svg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(0, 0, true, ImageUtils.ScalingStrategy.SPEED);

        scaleOp.execute(data, "image/svg+xml");

        final String scaledSvg = IOUtils.toString(scaleOp.getScaledData(), CharEncoding.UTF_8);
        assertThat(scaledSvg, not(containsString("<!DOCTYPE")));
    }

    @Test(expected = GalleryException.class)
    public void scaleSvgRefusedInvalidDoctype() throws GalleryException {
        InputStream data = getClass().getResourceAsStream("/test-SVG-invalid-doctype.svg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(0, 0, true, ImageUtils.ScalingStrategy.SPEED);

        scaleOp.execute(data, "image/svg+xml");
    }

    @Test
    public void scaleJpgWithCompression() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation normalOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED, 1f);
        normalOp.execute(data, "image/jpeg");
        byte[] normalData = IOUtils.toByteArray(normalOp.getScaledData());

        data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation compressedOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED, 0.8f);
        compressedOp.execute(data, "image/jpeg");
        checkImageDimensions(compressedOp, "image/jpeg", 88, 100);
    }

    @Test
    public void compressedJpgIsSmaller() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation normalOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED, 1f);
        normalOp.execute(data, "image/jpeg");
        byte[] normalData = IOUtils.toByteArray(normalOp.getScaledData());

        data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation compressedOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED, 0.5f);
        compressedOp.execute(data, "image/jpeg");
        byte[] compressedData = IOUtils.toByteArray(compressedOp.getScaledData());

        assertTrue("The compressed scaled image (" + compressedData.length + " bytes) "
                + "should be smaller than the normal scaled image (" + normalData.length + " bytes)",
                compressedData.length < normalData.length);
    }

    @Test
    public void compressionQualityHigherThanOne() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation normalOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED, 1f);
        normalOp.execute(data, "image/jpeg");

        data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation compressedOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED, 100);
        compressedOp.execute(data, "image/jpeg");

        assertTrue("Compression quality higher than 1 should be interpreted as 1",
                IOUtils.contentEquals(normalOp.getScaledData(), compressedOp.getScaledData()));
    }

    @Test
    public void compressionQualityLowerThanZero() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation normalOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED, 0);
        normalOp.execute(data, "image/jpeg");

        data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation compressedOp = new ScaleImageOperation(200, 100, true, ImageUtils.ScalingStrategy.SPEED, -42);
        compressedOp.execute(data, "image/jpeg");

        assertTrue("Compression quality lower than 0 should be interpreted as 0",
                IOUtils.contentEquals(normalOp.getScaledData(), compressedOp.getScaledData()));
    }

    @Test
    public void upscalingBounded() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.gif");
        ScaleImageOperation scaleOp = new ScaleImageOperation(800, 600, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/gif");
        checkImageDimensions(scaleOp, "image/gif", 532, 600);
    }

    @Test
    public void upscalingUnboundedHeight() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.gif");
        ScaleImageOperation scaleOp = new ScaleImageOperation(760, 0, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/gif");
        checkImageDimensions(scaleOp, "image/gif", 760, 856);
    }

    @Test
    public void upscalingUnboundedWidth() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.gif");
        ScaleImageOperation scaleOp = new ScaleImageOperation(0, 856 , true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/gif");
        checkImageDimensions(scaleOp, "image/gif", 760, 856);
    }

    @Test
    public void upscalingUnboundedBothEdges() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.gif");
        ScaleImageOperation scaleOp = new ScaleImageOperation(0, 0, true, ImageUtils.ScalingStrategy.SPEED);
        scaleOp.execute(data, "image/gif");
        checkImageDimensions(scaleOp, "image/gif", 380, 428);
    }

    @Test
    public void calculateResizeRatio() throws GalleryException, IOException {
        final ScaleImageOperation scaleOp = new ScaleImageOperation(0, 0, true, ImageUtils.ScalingStrategy.SPEED);

        double ratio = scaleOp.calculateResizeRatio(800, 600, 400, 500);
        assertTrue("Resize ratio calculated by bounding-box limited width.", ratio == 0.5);

        ratio = scaleOp.calculateResizeRatio(800, 600, 700, 300);
        assertTrue("Resize ratio calculated by bounding-box limited height.", ratio == 0.5);

        ratio = scaleOp.calculateResizeRatio(800, 600, 400, 0);
        assertTrue("Resize ratio calculated by bounding-box width.", ratio == 0.5);

        ratio = scaleOp.calculateResizeRatio(800, 600, 0, 300);
        assertTrue("Resize ratio calculated by bounding-box height.", ratio == 0.5);

        ratio = scaleOp.calculateResizeRatio(800, 600, 1700, 1200);
        assertTrue("Resize ratio calculated by bounding-box height.", ratio == 2.0);

        ratio = scaleOp.calculateResizeRatio(800, 600, 1600, 1400);
        assertTrue("Resize ratio calculated by bounding-box width.", ratio == 2.0);
    }

    @Test
    public void scaledImageDataWeightIsNotBiggerWhenScalingToOriginalDimensions() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(380, 428, false, ImageUtils.ScalingStrategy.QUALITY);
        scaleOp.execute(data, "image/jpeg");
        data = getClass().getResourceAsStream("/test-380x428.jpg");
        assertTrue("Scaled image data weight is not higher when scaling to original dimensions",
                IOUtils.toByteArray(scaleOp.getScaledData()).length <= IOUtils.toByteArray(data).length);
    }

    @Test
    public void scaledImageDataWeightIsNotBiggerWhenScalingToLargerDimensionsAndUpscalingIsDisabled() throws GalleryException, IOException {
        InputStream data = getClass().getResourceAsStream("/test-380x428.jpg");
        ScaleImageOperation scaleOp = new ScaleImageOperation(500, 500, false, ImageUtils.ScalingStrategy.QUALITY);
        scaleOp.execute(data, "image/jpeg");
        data = getClass().getResourceAsStream("/test-380x428.jpg");
        assertTrue("Scaled image data weight is not higher when scaling to larger dimensions and upscaling is disabled",
                IOUtils.toByteArray(scaleOp.getScaledData()).length <= IOUtils.toByteArray(data).length);
    }

    private void checkImageDimensions(ScaleImageOperation scaleOp, String mimeType, int expectedWidth, int expectedHeight) throws IOException {
        assertEquals(expectedWidth, scaleOp.getScaledWidth());
        assertEquals(expectedHeight, scaleOp.getScaledHeight());

        ImageReader reader = ImageIO.getImageReadersByMIMEType(mimeType).next();
        ImageInputStream iis = null;
        try {
            iis = ImageIO.createImageInputStream(scaleOp.getScaledData());
            reader.setInput(iis);
            assertEquals(scaleOp.getScaledWidth(), reader.getWidth(0));
            assertEquals(scaleOp.getScaledHeight(), reader.getHeight(0));
        } finally {
            if (iis != null) {
                iis.close();
            }
        }
    }

}
