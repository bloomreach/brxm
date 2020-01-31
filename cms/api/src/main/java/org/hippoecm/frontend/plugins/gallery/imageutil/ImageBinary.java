/*
 * Copyright 2013-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.editor.plugins.resource.MimeTypeHelper;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.imaging.jpeg.JpegSegmentData;
import com.drew.imaging.jpeg.JpegSegmentReader;
import com.drew.imaging.jpeg.JpegSegmentType;
import com.drew.lang.StreamReader;

import static org.apache.commons.imaging.Imaging.getImageInfo;
import static org.apache.commons.imaging.ImagingConstants.PARAM_KEY_READ_THUMBNAILS;

/**
 * This class extends a {@link Binary} class with extra information regarding images: filename, mimetype and
 * color model. It uses the commons-imaging (formerly Sanselan) library to figure out the mimetype if none is provided. For SVG images
 * (which Sanselan does not recognize) the mimetype auto-detection is skipped and the color model is set to UNKNOWN.
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
    private final String fileName;

    public ImageBinary(final Node node, final InputStream stream, final String fileName) throws GalleryException {
        this(node, stream, fileName, null);
    }

    public ImageBinary(final Node parent, final InputStream stream, final String fileName, String mimeType) throws GalleryException {

        try {
            binary = ResourceHelper.getValueFactory(parent).createBinary(stream);
        } catch (RepositoryException e) {
            die("Failed to create binary", e);
        }

        this.fileName = fileName;

        if (MimeTypeHelper.isSvgMimeType(mimeType)) {
            // Sanselan does not recognize SVG files, so do not auto-detect the MIME type and color model
            this.mimeType = MimeTypeHelper.MIME_TYPE_SVG;
            this.colorModel = ColorModel.UNKNOWN;
        } else {
            final ImageInfo info = createImageInfo();

            this.mimeType = MimeTypeHelper.sanitizeMimeType(Strings.isEmpty(mimeType) ? info.getMimeType() : mimeType);
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
                    synchronized (conversionLock) {
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
    }

    private ImageInfo createImageInfo() throws GalleryException {
        InputStream stream = null;
        try {
            Map<String, Object> params = new HashMap<>();

            //If an image contains a corrupt thumbnail it will throw an error reading metadata, so skip it.
            //See https://issues.apache.org/jira/browse/IMAGING-50?focusedCommentId=13162306&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-13162306
            params.put(PARAM_KEY_READ_THUMBNAILS, Boolean.FALSE);
            stream = getStream();
            return getImageInfo(stream, fileName, params);
        } catch (ImageReadException | IOException | RepositoryException e) {
            die("Failed to create commons-imaging image info", e);
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
        if (MimeTypeHelper.isJpegMimeType(mimeType)) {
            switch(info.getColorType()) {
                case RGB:
                    return ColorModel.RGB;
                case BW:
                    return ColorModel.RGB;
                case GRAYSCALE:
                    return ColorModel.RGB;
                case YCbCr:
                    return ColorModel.RGB;
                case CMYK:
                    return ColorModel.CMYK;
                case YCCK:
                    return ColorModel.YCCK;
                default:
                    return ColorModel.UNKNOWN;
            }
        } else {
            return ColorModel.RGB;
        }
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
