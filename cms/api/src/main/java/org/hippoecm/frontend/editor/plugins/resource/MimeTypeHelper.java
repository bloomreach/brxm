/*
 * Copyright 2015-2021 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.editor.plugins.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang3.StringUtils;

/**
 * @author cngo
 * @version $Id$
 * @since 2015-01-28
 */
public class MimeTypeHelper {

    public static final String MIME_TYPE_JPEG               = "image/jpeg";
    public static final String MIME_TYPE_PJPEG              = "image/pjpeg";
    public static final String MIME_TYPE_CITRIX_JPEG        = "image/x-citrix-pjpeg";
    public static final String MIME_TYPE_GIF                = "image/gif";
    public static final String MIME_TYPE_CITRIX_GIF         = "image/x-citrix-gif";
    public static final String MIME_TYPE_PNG                = "image/png";
    public static final String MIME_TYPE_X_PNG              = "image/x-png";
    public static final String MIME_TYPE_PDF                = "application/pdf";
    public static final String MIME_TYPE_SVG                = "image/svg+xml";
    public static final String MIME_TYPE_XML                = "text/xml";
    public static final String MIME_APPLICATION_POSTSCRIPT  = "application/postscript";
    public static final String MIMETYPE_IMAGE_PREFIX        = "image/";

    private MimeTypeHelper(){
        // constructor for helper class
    }

    /**
     * Mimetypes can be a tricky thing as browsers and/or environments tend to alter them without good reason. This
     * method will try to fix any of the quirks concerning mimetypes that are found out there.
     *
     * @param mimeType  The mimetype that needs to be sanitized.
     * @return A standard compliant mimetype in lowercase
     */
    public static String sanitizeMimeType(final String mimeType) {
        // Edge uploads JPEG files with the non-standard MIME type image/pjpeg for which ImageIO
        // doesn't have an ImageReader. Simply replacing the MIME type with image/jpeg solves this.
        // For more info see http://www.iana.org/assignments/media-types/image/ and
        // http://groups.google.com/group/comp.infosystems.www.authoring.images/msg/7706603e4bd1d9d4?hl=en
        if (MIME_TYPE_PJPEG.equalsIgnoreCase(mimeType)) {
            return MIME_TYPE_JPEG;
        }

        // Citrix environments change the mimetype of jpeg and gif files.
        if (MIME_TYPE_CITRIX_JPEG.equalsIgnoreCase(mimeType)) {
            return MIME_TYPE_JPEG;
        } else if (MIME_TYPE_CITRIX_GIF.equalsIgnoreCase(mimeType)) {
            return MIME_TYPE_GIF;
        }

        // Before it was accepted as a standard mimetype, PNG images where marked as image/x-png which is still the
        // case for IE8
        if (MIME_TYPE_X_PNG.equalsIgnoreCase(mimeType)) {
            return MIME_TYPE_PNG;
        }

        return mimeType.toLowerCase();
    }

    /**
     * Validate if the given data <code>inputStream</code> has the expected mime type identified by <code>mimeType</code>.
     * Exception will throw if the MimeType is invalid
     *
     * @param inputStream
     * @param mimeType
     * @throws org.hippoecm.frontend.editor.plugins.resource.ResourceException
     * @deprecated Not used any more, deprecated since 14.7.0, will be dropped in next major
     */
    @Deprecated
    public static void validateMimeType(final InputStream inputStream, final String mimeType) throws InvalidMimeTypeException {
        try {
            String sanitizedMimeType = sanitizeMimeType(mimeType);
            if (isSvgMimeType(sanitizedMimeType)) {
                // ignore SVG images and text/xml mime types
                return;
            }
            if (isImageMimeType(sanitizedMimeType)) {
                ImageInfo imageInfo = new ImageInfo();
                imageInfo.setInput(inputStream);
                if (imageInfo.check()) {
                    String imageInfoMimeType = imageInfo.getMimeType();
                    if (imageInfoMimeType == null) {
                        throw new InvalidMimeTypeException("impermissible image type content");
                    } else {
                        imageInfoMimeType = sanitizeMimeType(imageInfoMimeType);
                        if (!imageInfoMimeType.equals(sanitizedMimeType)) {
                            throw new InvalidMimeTypeException("mismatch image mime type");
                        }
                    }
                } else {
                    throw new InvalidMimeTypeException("impermissible image type content");
                }
            } else if (sanitizedMimeType.equals(MIME_TYPE_PDF)) {
                String line;
                line = new BufferedReader(new InputStreamReader(inputStream)).readLine().toUpperCase();
                if (!line.startsWith("%PDF-")) {
                    throw new InvalidMimeTypeException("impermissible pdf type content");
                }
            } else if (mimeType.equals(MIME_APPLICATION_POSTSCRIPT)) {
                String line;
                line = new BufferedReader(new InputStreamReader(inputStream)).readLine().toUpperCase();
                if (!line.startsWith("%!")) {
                    throw new InvalidMimeTypeException("impermissible postscript type content");
                }
            } else {
                // This method can be overridden to allow more such checks on content type.  if such an override
                // wants to be really strict and not allow unknown content, the following thrown exception is to be included
                // throw new InvalidMimeTypeException("impermissible unrecognized type content");
            }
        } catch (IOException ex) {
            throw new InvalidMimeTypeException("impermissible unknown type content");
        }
    }

    /**
     * Checks whether the given MIME type indicates an image.
     *
     * @param mimeType the MIME type to check
     * @return true if the given MIME type indicates an image, false otherwise.
     */
    public static boolean isImageMimeType(String mimeType) {
        return StringUtils.startsWithIgnoreCase(mimeType, MIMETYPE_IMAGE_PREFIX);
    }

    public static boolean isSvgMimeType(final String mimeType) {
        // Uploaded SVG images are stored in a file on disk. For some SVG files the MIME type
        // is then incorrectly read as 'text/xml'. We assume those files are OK too.
        return MIME_TYPE_SVG.equalsIgnoreCase(mimeType) || MIME_TYPE_XML.equalsIgnoreCase(mimeType);
    }

    public static boolean isPdfMimeType(final String mimeType){
        return MIME_TYPE_PDF.equalsIgnoreCase(mimeType);
    }

    public static boolean isJpegMimeType(final String mimeType) {
        return MIME_TYPE_JPEG.equalsIgnoreCase(mimeType);
    }
}
