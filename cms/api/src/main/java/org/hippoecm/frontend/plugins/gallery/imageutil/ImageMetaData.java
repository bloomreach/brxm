/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.imaging.jpeg.JpegSegmentReader;

/**
 * Provides meta-data about an image: its color model, MIME type, and file name.
 *
 * @deprecated The behavior of this class has been moved into the {@link ImageBinary}.
 */
@Deprecated
public class ImageMetaData implements IClusterable {

    final Logger log = LoggerFactory.getLogger(ImageMetaData.class);

    public enum ColorModel { UNKNOWN, RGB, CMYK, YCCK }

    private ColorModel colorModel = ColorModel.UNKNOWN;
    private String mimeType;
    private String fileName;

    public ImageMetaData(String mimeType, String fileName) {
        this.mimeType = ResourceHelper.sanitizeMimeType(mimeType);
        this.fileName = fileName;
    }

    /**
     * Currently only JPEG metadata is detected, al others formats are expected to be in the RGB color profile.
     */
    public void parse(InputStream is) throws ImageMetadataException {
        if (isJpeg()) {
            try {
                parse(new ReusableInputStream(is));
            } catch (IOException e) {
                log.error("Error parsing image metadata", e);
                throw new ImageMetadataException("Error parsing image metadata for " + toString(), e);
            } catch (ImageReadException e) {
                log.error("Error parsing image metadata", e);
                throw new ImageMetadataException("Error parsing image metadata for " + toString(), e);
            }
        } else {
            colorModel = ColorModel.RGB;
        }
    }

    private void parse(ReusableInputStream ris) throws IOException, ImageReadException {
        try {
            //If an image contains a corrupt thumbnail it will throw an error reading metadata, so skip it.
            //See https://issues.apache.org/jira/browse/IMAGING-50?focusedCommentId=13162306&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-13162306
            Map params = new HashMap();
            params.put(Sanselan.PARAM_KEY_READ_THUMBNAILS, Boolean.FALSE);

            ImageInfo info = Sanselan.getImageInfo(ris, fileName, params);
            ris.reset();

            switch(info.getColorType()) {
                case ImageInfo.COLOR_TYPE_RGB:
                    colorModel = ColorModel.RGB;
                    break;
                case ImageInfo.COLOR_TYPE_BW:
                    colorModel = ColorModel.RGB;
                    break;
                case ImageInfo.COLOR_TYPE_GRAYSCALE:
                    colorModel = ColorModel.RGB;
                    break;
                case ImageInfo.COLOR_TYPE_CMYK:
                    //Sanselan detects YCCK as CMYK so do a custom check
                    colorModel = isYCCK(ris) ? ColorModel.YCCK : ColorModel.CMYK;
                    break;
                default:
                    colorModel = ColorModel.UNKNOWN;
                    break;
            }
        } finally {
            ris.canBeClosed();
            IOUtils.closeQuietly(ris);
        }
    }

    private boolean isYCCK(final ReusableInputStream ris) throws IOException {
        try {
            JpegSegmentReader reader = new JpegSegmentReader(ris);
            byte[] appe = reader.readSegment(JpegSegmentReader.SEGMENT_APPE);
            return appe != null && appe[11] == 2;
        } catch (JpegProcessingException e1) {
            log.warn("Unable to read color space", e1);
        } finally {
            ris.reset();
        }
        return false;
    }

    public boolean isJpeg() {
        return ResourceHelper.MIME_TYPE_JPEG.equals(mimeType);
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
