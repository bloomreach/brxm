/*
 *  Copyright 2019-2021 Hippo B.V. (http://www.onehippo.com)
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.hippoecm.frontend.editor.plugins.resource.MimeTypeHelper;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class ScaleSvgOperation extends AbstractScaleImageOperation {

    public static final Logger log = LoggerFactory.getLogger(ScaleSvgOperation.class);

    public ScaleSvgOperation(final ScalingParameters parameters) {
        super(parameters);
    }

    @Override
    public ImageOperationResult run(final InputStream data, final String mimeType) throws GalleryException {
        if (!MimeTypeHelper.isSvgMimeType(mimeType)) {
            throw new GalleryException("Can not process images with mime-type '" + mimeType + "'");
        }

        try {
            processSvg(data);
        } catch (IOException e) {
            throw new GalleryException("Error processing SVG file", e);
        }

        return getResult();
    }

    private void processSvg(final InputStream data) throws IOException, GalleryException {
        // Save the image data in a temporary file so we can reuse the original data as-is
        // without putting it all into memory
        final File tmpFile = writeToTmpFile(data);
        log.debug("Stored uploaded image in temporary file {}", tmpFile);

        // by default, store SVG data as-is for all variants: the browser will do the real scaling
        // and use the bounding box as scaled width and height
        setResult(new AutoDeletingTmpFileInputStream(tmpFile), getParameters().getWidth(), getParameters().getHeight());

        try {
            final Document svgDocument = readSvgDocument(tmpFile);
            final Element svg = svgDocument.getDocumentElement();
            if (svg.hasAttribute("width") && svg.hasAttribute("height")) {
                scaleSvg(svg);
            }
            writeSvgDocument(tmpFile, svgDocument);
        } catch (ParserConfigurationException | SAXException e) {
            setResult(null, 0, 0);
            throw new GalleryException("Cannot parse SVG", e);
        }
    }

    @Override
    public void execute(final InputStream data, final ImageReader reader, final ImageWriter writer)
            throws IOException {
        throw new UnsupportedOperationException("Use execute(final InputStream data, final String mimeType) instead");
    }

    private void scaleSvg(final Element svg) {
        final String svgWidth = svg.getAttribute("width");
        final String svgHeight = svg.getAttribute("height");

        log.info("SVG size: {} x {}", svgWidth, svgHeight);

        final double originalWidth = readDoubleFromStart(svgWidth);
        final double originalHeight = readDoubleFromStart(svgHeight);
        final double resizeRatio = calculateResizeRatio(originalWidth, originalHeight);

        final int scaledWidth = (int) Math.max(originalWidth * resizeRatio, 1);
        final int scaledHeight = (int) Math.max(originalHeight * resizeRatio, 1);

        getResult().setWidth(scaledWidth);
        getResult().setHeight(scaledHeight);

        // save variant with scaled dimensions
        svg.setAttribute("width", Integer.toString(scaledWidth));
        svg.setAttribute("height", Integer.toString(scaledHeight));

        // add a viewbox when not present, so scaled variants still show the full image
        if (!svg.hasAttribute("viewBox")) {
            svg.setAttribute("viewBox", "0 0 " + originalWidth + " " + originalHeight);
        }
    }

    private Document readSvgDocument(final File tmpFile) throws ParserConfigurationException, SAXException, IOException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        // disable validation to speed up SVG parsing (without it parsing a tiny SVG file can take up to 15 seconds)
        disableValidation(factory);

        final DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new Log4jErrorHandler());
        return builder.parse(tmpFile);
    }

    private void writeSvgDocument(final File file, final Document svgDocument) {
        try {
            final TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            final Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, svgDocument.getInputEncoding());
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));
            final Result output = new StreamResult(file);
            final Source input = new DOMSource(svgDocument);
            transformer.transform(input, output);
        } catch (TransformerException e) {
            log.info("Writing SVG file " + file.getName() + " failed, using original instead", e);
        }
    }

    private void disableValidation(final DocumentBuilderFactory factory) throws ParserConfigurationException {
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        factory.setFeature("http://xml.org/sax/features/namespaces", false);
        factory.setFeature("http://xml.org/sax/features/validation", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    }

    private double readDoubleFromStart(final String s) {
        int i = 0;
        while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
            i++;
        }
        if (i == 0) {
            return 0;
        }
        return Double.parseDouble(s.substring(0, i));
    }
}
