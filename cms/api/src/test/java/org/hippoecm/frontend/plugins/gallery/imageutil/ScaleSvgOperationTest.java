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
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class ScaleSvgOperationTest {

    @Test
    public void scaleSvg() throws GalleryException {
        final ScalingParameters parameters = new ScalingParameters.Builder(200, 100).build();
        final ImageOperationResult result = scale("/test-SVG.svg", parameters);

        assertEquals(122, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    public void scaleSvgWithoutDimensionsInBoundingBox() throws GalleryException {
        final ScalingParameters parameters = new ScalingParameters.Builder(200, 100).build();
        final ImageOperationResult result = scale("/test-SVG-without-dimensions.svg", parameters);

        assertEquals(200, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    public void scaleSvgWithoutDimensionsAsOriginal() throws GalleryException {
        final ScalingParameters parameters = new ScalingParameters.Builder(0, 0).build();
        final ImageOperationResult result = scale("/test-SVG-without-dimensions.svg", parameters);

        assertEquals(0, result.getWidth());
        assertEquals(0, result.getHeight());
    }

    @Test
    public void scaleSvgAddsViewboxWhenMissing() throws GalleryException, IOException, ParserConfigurationException, SAXException {
        final ScalingParameters parameters = new ScalingParameters.Builder(200, 100).build();
        final ImageOperationResult result = scale("/test-SVG-without-viewbox.svg", parameters);

        // read svg
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        factory.setFeature("http://xml.org/sax/features/namespaces", false);
        factory.setFeature("http://xml.org/sax/features/validation", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document svgDocument = builder.parse(result.getData());
        final Element svgElement = svgDocument.getDocumentElement();

        assertEquals("SVG without a 'viewBox' attribute should have gotten one set to the original image size",
                "0 0 178.0 145.0", svgElement.getAttribute("viewBox"));
    }

    @Test
    public void scaleSvgRemovesDoctypeFromScaledImage() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(200, 100).build();
        final ImageOperationResult result = scale("/test-SVG.svg", parameters);

        final String scaledSvg = IOUtils.toString(result.getData(), StandardCharsets.UTF_8);
        assertThat(scaledSvg, not(containsString("<!DOCTYPE")));
    }

    @Test
    public void scaleSvgRemovesDoctypeFromOriginalImage() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(0, 0).build();
        final ImageOperationResult result = scale("/test-SVG.svg", parameters);

        final String scaledSvg = IOUtils.toString(result.getData(), StandardCharsets.UTF_8);
        assertThat(scaledSvg, not(containsString("<!DOCTYPE")));
    }

    @Test
    public void scaleSvgRemovesDoctypeFromInvalidSvg() throws GalleryException, IOException {
        final ScalingParameters parameters = new ScalingParameters.Builder(0, 0).build();
        final ImageOperationResult result = scale("/test-SVG-invalid.svg", parameters);

        final String scaledSvg = IOUtils.toString(result.getData(), StandardCharsets.UTF_8);
        assertThat(scaledSvg, not(containsString("<!DOCTYPE")));
    }

    @Test(expected = GalleryException.class)
    public void scaleSvgRefusedInvalidDoctype() throws GalleryException {
        final ScalingParameters parameters = new ScalingParameters.Builder(0, 0).build();
        scale("/test-SVG-invalid-doctype.svg", parameters);
    }

    private static ImageOperationResult scale(final String inputFile, final ScalingParameters parameters)
            throws GalleryException {
        final InputStream data = readFile(inputFile);
        final ImageOperation operation = new ScaleSvgOperation(parameters);
        return operation.run(data, "image/svg+xml");
    }

    private static InputStream readFile(final String filePath) {
        return ScaleSvgOperationTest.class.getResourceAsStream(filePath);
    }
}
