/*
 *  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.yui.upload.validation.ImageUploadValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * <p>
 * Creates a scaled version of an image. The given scaling parameters define a bounding box with
 * a certain width and height. Images that do not fit in this box (i.e. are too large) are always scaled
 * down such that they do fit. If the aspect ratio of the original image differs from that of the
 * bounding box, either the width or the height of scaled image will be less than that of the box.</p>
 * <p>
 * Smaller images are scaled up in the same way as large images are scaled down, but only if upscaling is
 * true. When upscaling is false and the image is smaller than the bounding box, the scaled image
 * will be equal to the original.</p>
 * <p>
 * If the width or height of the scaling parameters is 0 or less, that side of the bounding box does not
 * exist (i.e. is unbounded). If both sides of the bounding box are unbounded, the scaled image will be
 * equal to the original.</p>
 */
public class ScaleImageOperation extends AbstractImageOperation {

    private static final Logger log = LoggerFactory.getLogger(ScaleImageOperation.class);
    private static final Object scalingLock = new Object();

    private final int width;
    private final int height;
    private final boolean upscaling;
    private final ImageUtils.ScalingStrategy strategy;
    private InputStream scaledData;
    private int scaledWidth;
    private int scaledHeight;
    private float compressionQuality = 1f;

    /**
     * Creates a image scaling operation, defined by the bounding box of a certain width and height.
     * The strategy will be set to {@link org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils.ScalingStrategy#QUALITY},
     * and the maximum memory usage to zero (i.e. undefined).
     *
     * @param width the width of the bounding box in pixels
     * @param height the height of the bounding box in pixels
     * @param upscaling whether to enlarge images that are smaller than the bounding box
     */
    public ScaleImageOperation(int width, int height, boolean upscaling) {
        this(width, height, upscaling, ImageUtils.ScalingStrategy.QUALITY);
    }

    /**
     * Creates a image scaling operation, defined by the bounding box of a certain width and height.
     *
     * @param width the width of the bounding box in pixels
     * @param height the height of the bounding box in pixels
     * @param upscaling whether to enlarge images that are smaller than the bounding box
     * @param strategy the strategy to use for scaling the image (e.g. optimize for speed, quality, a trade-off between
     *                 these two, etc.)
     */
    public ScaleImageOperation(int width, int height, boolean upscaling, ImageUtils.ScalingStrategy strategy) {
        this(width, height, upscaling, strategy, 1f);
    }

    /**
     * Creates a image scaling operation, defined by the bounding box of a certain width and height.
     *
     * @param width     the width of the bounding box in pixels
     * @param height    the height of the bounding box in pixels
     * @param upscaling whether to enlarge images that are smaller than the bounding box
     * @param strategy  the strategy to use for scaling the image (e.g. optimize for speed, quality, a trade-off between
     *                  these two, etc.)
     * @param compressionQuality a float between 0 and 1 indicating the compression quality to use for writing the
     *                           scaled image data.
     */
    public ScaleImageOperation(int width, int height, boolean upscaling, ImageUtils.ScalingStrategy strategy, float compressionQuality) {
        this.width = width;
        this.height = height;
        this.upscaling = upscaling;
        this.strategy = strategy;
        this.compressionQuality = compressionQuality;
    }

    @Override
    public void execute(final InputStream data, final String mimeType) throws GalleryException {
        if (ImageUploadValidationService.isSvgMimeType(mimeType)) {
            try {
                processSvg(data);
            } catch (IOException e) {
                throw new GalleryException("Error processing SVG file", e);
            }
        } else {
            super.execute(data, mimeType);
        }
    }

    private void processSvg(final InputStream data) throws IOException {
        // Save the image data in a temporary file so we can reuse the original data as-is
        // without putting it all into memory
        final File tmpFile = writeToTmpFile(data);
        log.debug("Stored uploaded image in temporary file {}", tmpFile);

        // by default, store SVG data as-is for all variants: the browser will do the real scaling
        scaledData = new AutoDeletingTmpFileInputStream(tmpFile);

        // by default, use the bounding box as scaled width and height
        scaledWidth = width;
        scaledHeight = height;

        if (!isOriginalVariant()) {
            try {
                scaleSvg(tmpFile);
            } catch (ParserConfigurationException | SAXException e) {
                log.info("Could not read dimensions of SVG image, using the bounding box dimensions instead", e);
            }
        }
    }

    private boolean isOriginalVariant() {
        return width <= 0 && height <= 0;
    }

    private void scaleSvg(final File tmpFile) throws ParserConfigurationException, SAXException, IOException {
        final Document svgDocument = readSvgDocument(tmpFile);
        final Element svg = svgDocument.getDocumentElement();

        if (svg.hasAttribute("width") && svg.hasAttribute("height")) {
            final String svgWidth = svg.getAttribute("width");
            final String svgHeight = svg.getAttribute("height");

            log.info("SVG size: {} x {}", svgWidth, svgHeight);

            final double originalWidth = readDoubleFromStart(svgWidth);
            final double originalHeight = readDoubleFromStart(svgHeight);

            final double resizeRatio = calculateResizeRatio(originalWidth, originalHeight, width, height);

            scaledWidth = (int) Math.max(originalWidth * resizeRatio, 1);
            scaledHeight = (int) Math.max(originalHeight * resizeRatio, 1);

            // save variant with scaled dimensions
            svg.setAttribute("width", Integer.toString(scaledWidth));
            svg.setAttribute("height", Integer.toString(scaledHeight));

            // add a viewbox when not present, so scaled variants still show the full image
            if (!svg.hasAttribute("viewBox")) {
                svg.setAttribute("viewBox", "0 0 " + originalWidth + " " + originalHeight);
            }

            writeSvgDocument(tmpFile, svgDocument);
        }
    }

    private Document readSvgDocument(final File tmpFile) throws ParserConfigurationException, SAXException, IOException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // disable validation to speed up SVG parsing (without it parsing a tiny SVG file can take up to 15 seconds)
        disableValidation(factory);

        final DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(tmpFile);
    }

    private void writeSvgDocument(final File file, final Document svgDocument) {
        try {
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, svgDocument.getInputEncoding());
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));
            Result output = new StreamResult(file);
            Source input = new DOMSource(svgDocument);
            transformer.transform(input, output);
        } catch (TransformerConfigurationException e) {
            log.info("Writing SVG file " + file.getName() + " failed, using original instead", e);
        } catch (TransformerException e) {
            log.info("Writing SVG file " + file.getName() + " failed, using original instead", e);
        }
    }

    private void disableValidation(final DocumentBuilderFactory factory) throws ParserConfigurationException {
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        factory.setFeature("http://xml.org/sax/features/namespaces", false);
        factory.setFeature("http://xml.org/sax/features/validation", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    }

    private double readDoubleFromStart(String s) {
        int i = 0;
        while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
            i++;
        }
        if (i == 0) {
            return 0;
        }
        return Double.parseDouble(s.substring(0, i));
    }

    /**
     * Creates a scaled version of an image. The given scaling parameters define a bounding box with
     * a certain width and height. Images that do not fit in this box (i.e. are too large) are always scaled
     * down such that they do fit. If the aspect ratio of the original image differs from that of the
     * bounding box, either the width or the height of scaled image will be less than that of the box.</p>
     * <p>
     * Smaller images are scaled up in the same way as large images are scaled down, but only if upscaling is
     * true. When upscaling is false and the image is smaller than the bounding box, the scaled image
     * will be equal to the original.</p>
     * <p>
     * If the width or height of the scaling parameters is 0 or less, that side of the bounding box does not
     * exist (i.e. is unbounded). If both sides of the bounding box are unbounded, the scaled image will be
     * equal to the original.</p>
     *
     * @param data the original image data
     * @param reader reader for the image data
     * @param writer writer for the image data
     */
    public void execute(InputStream data, ImageReader reader, ImageWriter writer) throws IOException {
        // save the image data in a temporary file so we can reuse the original data as-is if needed without
        // putting all the data into memory
        final File tmpFile = writeToTmpFile(data);
        boolean deleteTmpFile = true;
        log.debug("Stored uploaded image in temporary file {}", tmpFile);
        
        InputStream dataInputStream = null;
        ImageInputStream imageInputStream = null;
        
        try {
            dataInputStream = new FileInputStream(tmpFile);
            imageInputStream = new MemoryCacheImageInputStream(dataInputStream);
            reader.setInput(imageInputStream);

            final int originalWidth = reader.getWidth(0);
            final int originalHeight = reader.getHeight(0);
            final double resizeRatio = calculateResizeRatio(originalWidth, originalHeight, width, height);

            if (resizeRatio == 1.0d || (resizeRatio >= 1.0d && !upscaling)) {
                // return the original image data as-is by reading the temporary file, which is deleted when the
                // stream is closed
                log.debug("Using the original image of {}x{} as-is", originalWidth, originalHeight);
                deleteTmpFile = false;
                scaledData = new AutoDeletingTmpFileInputStream(tmpFile);
                scaledWidth = originalWidth;
                scaledHeight = originalHeight;
            } else {
                // scale the image
                int targetWidth = (int)Math.max(originalWidth * resizeRatio, 1);
                int targetHeight = (int)Math.max(originalHeight * resizeRatio, 1);

                if (log.isDebugEnabled()) {
                    log.debug("Resizing image of {}x{} to {}x{}", new Object[]{originalWidth, originalHeight, targetWidth, targetHeight});
                }

                BufferedImage scaledImage;

                synchronized(scalingLock) {
                    BufferedImage originalImage = reader.read(0);
                    scaledImage = ImageUtils.scaleImage(originalImage, targetWidth, targetHeight, strategy);
                }                

                scaledWidth = scaledImage.getWidth();
                scaledHeight = scaledImage.getHeight();

                ByteArrayOutputStream scaledOutputStream = ImageUtils.writeImage(writer, scaledImage, compressionQuality);
                scaledData = new ByteArrayInputStream(scaledOutputStream.toByteArray());
            }
        } finally {
            if (imageInputStream != null) {
                imageInputStream.close();
            }
            IOUtils.closeQuietly(dataInputStream);
            if (deleteTmpFile) {
                log.debug("Deleting temporary file {}", tmpFile);
                tmpFile.delete();
            }
        }
    }
    
    private File writeToTmpFile(InputStream data) throws IOException {
        File tmpFile = File.createTempFile("hippo-image", ".tmp");
        tmpFile.deleteOnExit();
        OutputStream tmpStream = null;
        try {
            tmpStream = new BufferedOutputStream(new FileOutputStream(tmpFile));
            IOUtils.copy(data, tmpStream);
        } finally {
            IOUtils.closeQuietly(tmpStream);
        }
        return tmpFile;
    }

    protected double calculateResizeRatio(double originalWidth, double originalHeight, int targetWidth,
                                          int targetHeight) {
        double widthRatio = 1;
        double heightRatio = 1;

        if (targetWidth >= 1) {
            widthRatio = targetWidth / originalWidth;
        }
        if (targetHeight >= 1) {
            heightRatio = targetHeight / originalHeight;
        }

        if (widthRatio == 1) {
            return heightRatio;
        } else if (heightRatio == 1) {
            return widthRatio;
        }

        // If the image has to be scaled down we should return the largest negative ratio.
        // If the image has to be scaled up, and we should take the smallest positive ratio.
        // If it is unbounded upscaling, return the largest positive ratio.
        if (!(targetWidth == 0 && targetHeight == 0) && (targetWidth == 0 || targetHeight == 0)) {
            return Math.max(widthRatio, heightRatio);
        } else {
            return Math.min(widthRatio, heightRatio);
        }
    }

    /**
     * @return the scaled image data
     */
    public InputStream getScaledData() {
        return scaledData;
    }

    /**
     * @return the width of this scaled image
     */
    public int getScaledWidth() {
        return scaledWidth;
    }

    /**
     * @return the height of this scaled image
     */
    public int getScaledHeight() {
        return scaledHeight;
    }

    public float getCompressionQuality() {
        return compressionQuality;
    }


    private static class AutoDeletingTmpFileInputStream extends FileInputStream {

        private final File tmpFile;

        AutoDeletingTmpFileInputStream(File tmpFile) throws FileNotFoundException {
            super(tmpFile);
            this.tmpFile = tmpFile;
        }

        @Override
        public void close() throws IOException {
            super.close();
            log.debug("Deleting temporary file {}", tmpFile);
            tmpFile.delete();
        }

    }

}
