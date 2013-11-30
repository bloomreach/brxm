/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.imaging.jpeg.JpegSegmentReader;

/**
 * This class extends a {@link Binary} class with extra information regarding images: filename, mimetype and
 * color model. It uses the Sanselan library to figure out the mimetype if none is provided.
 *
 * Furthermore it converts YCCK and CMYK input into the RGB color model so it can be used by the JPEGImageReader. See
 * CMS7-5074 for more info.
 */
public class ImageBinary implements Binary {

    public static final Logger log = LoggerFactory.getLogger(ImageBinary.class);

    private static final Object conversionLock = new Object();

    private Binary binary;

    private ColorModel colorModel = ColorModel.UNKNOWN;
    private String mimeType;
    private String fileName;

    public ImageBinary(final Node node, final InputStream stream, final String fileName) throws GalleryException {
        this(node, stream, fileName, null);
    }

    public ImageBinary(final Node parent, final InputStream stream, final String fileName, final String mimeType) throws GalleryException {

        try {
            binary = ResourceHelper.getValueFactory(parent).createBinary(stream);
        } catch (RepositoryException e) {
            die("Failed to create binary", e);
        }

        ImageInfo info = createImageInfo();

        this.fileName = fileName;
        this.mimeType = ResourceHelper.sanitizeMimeType(Strings.isEmpty(mimeType) ? info.getMimeType() : mimeType);
        try {
            colorModel = parseColorModel(info);
        } catch (RepositoryException e) {
            die("Failed to parse color model", e);
        }

        if (colorModel == ColorModel.UNKNOWN) {
            throw new GalleryException("Unknown color profile for " + toString());
        }

        if (colorModel != ColorModel.RGB) {
            try {
                synchronized(conversionLock) {
                    InputStream converted = ImageUtils.convertToRGB(getStream(), colorModel);
                    binary.dispose();
                    binary = ResourceHelper.getValueFactory(parent).createBinary(converted);
                    colorModel = ColorModel.RGB;
                }
            } catch (IOException e) {
                die("Error during conversion to RGB", e);
            } catch (UnsupportedImageException e) {
                die("Image can't be converted to RGB", e);
            } catch (RepositoryException e) {
                die("Repository error after conversion", e);
            }
        }
    }

    private ImageInfo createImageInfo() throws GalleryException {
        InputStream stream = null;
        try {
            Map<String, Object> params = new HashMap<>();

            //If an image contains a corrupt thumbnail it will throw an error reading metadata, so skip it.
            //See https://issues.apache.org/jira/browse/IMAGING-50?focusedCommentId=13162306&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-13162306
            params.put(Sanselan.PARAM_KEY_READ_THUMBNAILS, Boolean.FALSE);
            stream = getStream();
            return Sanselan.getImageInfo(stream, fileName, params);
        } catch (ImageReadException | IOException | RepositoryException e) {
            die("Failed to create Sanselan image info", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }

        throw new IllegalStateException("No way");
    }

    /**
     * Currently only JPEG metadata is detected, al others formats are expected to be in the RGB color profile.
     * @param info Sanselan image metadata
     */
    private ColorModel parseColorModel(final ImageInfo info) throws RepositoryException {
        if (ResourceHelper.MIME_TYPE_JPEG.equals(mimeType)) {
            switch(info.getColorType()) {
                case ImageInfo.COLOR_TYPE_RGB:
                    return ColorModel.RGB;
                case ImageInfo.COLOR_TYPE_BW:
                    return ColorModel.RGB;
                case ImageInfo.COLOR_TYPE_GRAYSCALE:
                    return ColorModel.RGB;
                case ImageInfo.COLOR_TYPE_CMYK:
                    //Sanselan detects YCCK as CMYK so do a custom check
                    return isYCCK() ? ColorModel.YCCK : ColorModel.CMYK;
                default:
                    return ColorModel.UNKNOWN;
            }
        } else {
            return ColorModel.RGB;
        }
    }

    //TODO: When Sanselan 1.0 is released we can remove this custom check. See https://issues.apache.org/jira/browse/IMAGING-89
    private boolean isYCCK() throws RepositoryException {
        InputStream stream = getStream();
        try {
            JpegSegmentReader reader = new JpegSegmentReader(stream);
            byte[] appe = reader.readSegment(JpegSegmentReader.SEGMENT_APPE);
            return appe != null && appe[11] == 2;
        } catch (JpegProcessingException e1) {
            log.warn("Unable to read color space", e1);
        } finally {
            IOUtils.closeQuietly(stream);
        }
        return false;
    }

    private void die(String message, Exception e) throws GalleryException {
        throw new GalleryException(message + " - " + e.getMessage() + " - " + toString());
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public ColorModel getColorModel() {
        return colorModel;
    }

    @Override
    public InputStream getStream() throws RepositoryException {
        return binary.getStream();
    }

    @Override
    public int read(final byte[] b, final long position) throws IOException, RepositoryException {
        return binary.read(b, position);
    }

    @Override
    public long getSize() throws RepositoryException {
        return binary.getSize();
    }

    @Override
    public void dispose() {
        binary.dispose();
    }

    @Override
    public String toString() {
        return "[file name: " + fileName + ", MIME type=" + mimeType + ", color model=" + colorModel.name() + "]";
    }
}
