/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.jaxrs.services.content;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
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

import org.apache.commons.io.IOUtils;

class HippoGalleryImageThumbnailCreator {
    
    private static final int FIRST_IMAGE_IN_FILE = 0;
    private static final String MIME_IMAGE_PJPEG = "image/pjpeg";
    private static final String MIME_IMAGE_JPEG = "image/jpeg";
    
    private HippoGalleryImageThumbnailCreator() {
        
    }

    static InputStream createThumbnail(InputStream imageData, int maxSize, String mimeType) throws Exception {
        if (maxSize < 1) {
            throw new IllegalArgumentException("A maximum size smaller than 1 is not supported");
        }
        if (mimeType == null || "".equals(mimeType.trim())) {
            throw new IllegalArgumentException("A mime type should be provided");
        }
        if (imageData == null) {
            throw new IllegalArgumentException("We cannot create a thumbnail for a NULL input stream");
        }

        //IE uploads jpeg files with the non-standard mimetype image/pjpeg for which ImageIO
        //doesn't have an ImageReader. Simply replacing the mimetype with image/jpeg solves this.
        //For more info see http://www.iana.org/assignments/media-types/image/ and
        //http://groups.google.com/group/comp.infosystems.www.authoring.images/msg/7706603e4bd1d9d4?hl=en
        if (mimeType.equals(MIME_IMAGE_PJPEG)) {
            mimeType = MIME_IMAGE_JPEG;
        }

        ImageReader reader = null;
        ImageWriter writer = null;
        try {
            reader = getImageReader(mimeType);
            writer = getImageWriter(mimeType);

            MemoryCacheImageInputStream mciis = new MemoryCacheImageInputStream(imageData);
            reader.setInput(mciis);

            double originalWidth = reader.getWidth(FIRST_IMAGE_IN_FILE);
            double originalHeight = reader.getHeight(FIRST_IMAGE_IN_FILE);
            double resizeRatio = calcResizeRatio(maxSize, originalWidth, originalHeight);

            int resizeWidth = (int) (originalWidth * resizeRatio);
            int resizeHeight = (int) (originalHeight * resizeRatio);

            BufferedImage originalImage = reader.read(FIRST_IMAGE_IN_FILE);
            BufferedImage scaledImage = null;
            if (resizeRatio < 1.0d) {
                scaledImage = getScaledInstance(originalImage, resizeWidth, resizeHeight,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
            } else {
                scaledImage = getScaledInstance(originalImage, resizeWidth, resizeHeight,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR, false);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(out);
            writer.setOutput(ios);
            writer.write(scaledImage);
            ios.flush();
            ios.close();
            mciis.close();

            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Could not resize image", e);
        } catch (Exception e) {
            throw new RuntimeException("Could not resize image", e);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
            if (writer != null) {
                writer.dispose();
            }
            IOUtils.closeQuietly(imageData);
        }
    }

    private static double calcResizeRatio(int maxSize, double originalWidth, double originalHeight) {
        double resizeRatio;
        if (originalHeight > originalWidth) {
            resizeRatio = maxSize / originalHeight;
        } else {
            resizeRatio = maxSize / originalWidth;
        }
        return resizeRatio;
    }

    private static ImageReader getImageReader(String mimeType) throws IllegalArgumentException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType(mimeType);
        if (readers == null || !readers.hasNext()) {
            throw new IllegalArgumentException("Unsupported MIME Type: " + mimeType);
        }
        return readers.next();
    }

    private static ImageWriter getImageWriter(String mimeType) throws IllegalArgumentException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(mimeType);
        if (writers == null || !writers.hasNext()) {
            throw new IllegalArgumentException("Unsupported MIME Type: " + mimeType);
        }
        return writers.next();
    }

    private static BufferedImage getScaledInstance(BufferedImage img, int targetWidth, int targetHeight, Object hint,
            boolean higherQuality) {

        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;

        BufferedImage ret = img;

        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }
}
