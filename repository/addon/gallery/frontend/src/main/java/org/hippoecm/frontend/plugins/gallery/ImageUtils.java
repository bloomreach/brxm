/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.gallery;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageUtils {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    static final Logger log = LoggerFactory.getLogger(ImageUtils.class);

    public static InputStream createThumbnail(InputStream imageData, int maxWidth, String mimeType) {
        if (maxWidth <= 0) {
            return new ByteArrayInputStream(new byte[] {});
        }
        try {
            Iterator readers = ImageIO.getImageReadersByMIMEType(mimeType);
            if (readers == null || !readers.hasNext()) {
                log.warn("Unsupported mimetype, cannot read: " + mimeType);
                return new ByteArrayInputStream(new byte[] {});
            }
            ImageReader reader = (ImageReader) readers.next();

            Iterator writers = ImageIO.getImageWritersByMIMEType(mimeType);
            if (writers == null || !writers.hasNext()) {
                log.warn("Unsupported mimetype, cannot write: " + mimeType);
                return new ByteArrayInputStream(new byte[] {});
            }
            ImageWriter writer = (ImageWriter) writers.next();

            reader.setInput(new MemoryCacheImageInputStream(imageData));
            double originalWidth = reader.getWidth(0);
            double originalHeight = reader.getHeight(0);
            BufferedImage originalImage = reader.read(0);

            AffineTransform transformation;
            BufferedImage scaledImage;
            double ratio = maxWidth / originalWidth;
            if (ratio < 1.0d) {
                transformation = AffineTransform.getScaleInstance(ratio, ratio);
                double scaledWidth = originalWidth * ratio;
                double scaledHeight = originalHeight * ratio;
                scaledImage = new BufferedImage((int) scaledWidth, (int) scaledHeight, BufferedImage.TYPE_INT_RGB);
            } else {
                transformation = new AffineTransform();
                scaledImage = new BufferedImage((int) originalWidth, (int) originalHeight, BufferedImage.TYPE_INT_RGB);
            }

            Graphics2D g2d = scaledImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.drawImage(originalImage, transformation, null);
            g2d.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(out);
            writer.setOutput(ios);
            writer.write(scaledImage);
            ios.flush();
            ios.close();

            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return new ByteArrayInputStream(new byte[] {});
        }
    }
}
