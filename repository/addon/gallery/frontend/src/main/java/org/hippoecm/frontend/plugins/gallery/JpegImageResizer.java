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
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

// FIXME TODO replace this class with a image resizer not using com.sun.image.codec.jpeg and 
// capable of resizing gif and others: see http://svn.apache.org/repos/asf/incubator/sanselan/
public class JpegImageResizer {

    static final Logger log = LoggerFactory.getLogger(JpegImageResizer.class);

    public static InputStream resizeImage(InputStream imageData, int maxWidth) throws IOException {
        byte[] b = resizeImage(readBytes(imageData), maxWidth);
        imageData.close();
        return new ByteArrayInputStream(b);
    }
    
    public static byte[] resizeImage(byte[] imageData, int maxWidth) throws IOException {
        ImageIcon imageIcon = new ImageIcon(imageData);
        int width = imageIcon.getIconWidth();
        int height = imageIcon.getIconHeight();
        log.info("imageIcon width: #0  height: #1", width, height);
        if (maxWidth > 0 && width > maxWidth) {
            double ratio = (double) maxWidth / imageIcon.getIconWidth();
            log.info("resize ratio: #0", ratio);
            height = (int) (imageIcon.getIconHeight() * ratio);
            width = maxWidth;
            log.info("imageIcon post scale width: #0  height: #1", width, height);
        }
        BufferedImage bufferedResizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedResizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.drawImage(imageIcon.getImage(), 0, 0, width, height, null);
        g2d.dispose();
        ByteArrayOutputStream encoderOutputStream = new ByteArrayOutputStream();
        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(encoderOutputStream);
        encoder.encode(bufferedResizedImage);
        byte[] resizedImageByteArray = encoderOutputStream.toByteArray();
        return resizedImageByteArray;
    }

    public static byte[] readBytes(InputStream inputStream) throws IOException {
        final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        copyStream(inputStream, byteOut);
        return byteOut.toByteArray();
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        final byte[] buffer = new byte[4096];
        while (true) {
            final int bytesRead = in.read(buffer);
            if (bytesRead == -1) {
                break;
            }
            out.write(buffer, 0, bytesRead);
        }
    }
}
