/*
 * Copyright 2012 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.
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

import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.imaging.jpeg.JpegSegmentReader;

import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides meta-data about a image: its color model, MIME type, and file name.
 */
public class ImageMetaData implements IClusterable {

    final Logger log = LoggerFactory.getLogger(ImageMetaData.class);

    public enum ColorModel { UNKNOWN, RGB, CMYK, YCCK }

    private ColorModel colorModel = ColorModel.UNKNOWN;
    private String mimeType;
    private String fileName;

    public ImageMetaData(String mimeType, String fileName) {
        if (mimeType.equals(ResourceHelper.MIME_IMAGE_PJPEG)) {
            // IE uploads JPEG files with the non-standard MIME type image/pjpeg for which ImageIO
            // doesn't have an ImageReader. Simply replacing the MIME type with image/jpeg solves this.
            // For more info see http://www.iana.org/assignments/media-types/image/ and
            // http://groups.google.com/group/comp.infosystems.www.authoring.images/msg/7706603e4bd1d9d4?hl=en
            mimeType = ResourceHelper.MIME_IMAGE_JPEG;
        }
        this.mimeType = mimeType;
        this.fileName = fileName;
    }

    /**
     * Currently only JPEG metadata is detected, al others formats are expected to be in the RGB color profile.
     */
    public InputStream parse(InputStream is) throws ImageMetadataException {
        if (isJpeg()) {
            ReusableInputStream ris = new ReusableInputStream(is);
            try {
                parse(ris);
            } catch (IOException e) {
                log.error("Error parsing image metadata", e);
                throw new ImageMetadataException("Error parsing image metadata for " + toString(), e);
            } catch (ImageReadException e) {
                log.error("Error parsing image metadata", e);
                throw new ImageMetadataException("Error parsing image metadata for " + toString(), e);
            }
            return ris;
        } else {
            colorModel = ColorModel.RGB;
            return is;
        }
    }

    private void parse(ReusableInputStream ris) throws IOException, ImageReadException {
        try {
            ImageInfo info = Sanselan.getImageInfo(ris, fileName);
            ris.reset();

            if (info.getColorType() == ImageInfo.COLOR_TYPE_RGB ||
                info.getColorType() == ImageInfo.COLOR_TYPE_BW ||
                info.getColorType() == ImageInfo.COLOR_TYPE_GRAYSCALE) {

                colorModel = ColorModel.RGB;
            } else if (info.getColorType() == ImageInfo.COLOR_TYPE_CMYK) {
                colorModel = ColorModel.CMYK;
                try {
                    JpegSegmentReader segmentReader = new JpegSegmentReader(ris);
                    byte[] exifSegment = segmentReader.readSegment(JpegSegmentReader.SEGMENT_APPE);

                    if (exifSegment != null && exifSegment[11] == 2) {
                        colorModel = ColorModel.YCCK;
                    }
                } catch (JpegProcessingException e1) {
                    log.warn("Unable to read color space", e1);
                } finally {
                    ris.reset();
                }
            }
        } finally {
            ris.canBeClosed();
        }
    }

    public boolean isJpeg() {
        return ResourceHelper.MIME_IMAGE_JPEG.equalsIgnoreCase(mimeType);
    }

    public ColorModel getColorModel() {
        return colorModel;
    }

    public String getFilename() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String toString() {
        return "[file name: " + fileName + ", MIME type=" + mimeType + ", color model=" + colorModel.name() + "]";
    }

}
