/*
 * Copyright 2012-2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.yui.upload.validation;

import java.awt.Dimension;
import java.io.IOException;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.util.value.IValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageUploadValidationService extends DefaultUploadValidationService {

    private static final Logger log = LoggerFactory.getLogger(ImageUploadValidationService.class);

    public static final int DEFAULT_MAX_WIDTH = 1920;
    public static final int DEFAULT_MAX_HEIGHT = 1280;
    public static final String DEFAULT_MAX_FILE_SIZE = "4mb";
    public static final String[] DEFAULT_EXTENSIONS_ALLOWED = new String[] {"*.jpg", "*.jpeg", "*.gif", "*.png", "*.svg"};

    private static final String MIME_TYPE_SVG = "image/svg+xml";
    private static final String MIME_TYPE_XML = "text/xml";

    private static final String MAX_WIDTH = "max.width";
    private static final String MAX_HEIGHT = "max.height";

    private int maxWidth;
    private int maxHeight;

    public ImageUploadValidationService(IValueMap params) {
        super(params);

        maxWidth = params.getInt(MAX_WIDTH, DEFAULT_MAX_WIDTH);
        maxHeight = params.getInt(MAX_HEIGHT, DEFAULT_MAX_HEIGHT);

        addValidator(new Validator() {

            @Override
            public void validate(FileUpload upload) {
                // SVG files do not have a fixed size so they are always OK
                if (!isSvgMimeType(upload.getContentType())) {
                    validateSizes(upload);
                }
            }
        });
    }

    public static boolean isSvgMimeType(final String mimeType) {
        // Uploaded SVG images are stored in a file on disk. For some SVG files the MIME type
        // is then incorrectly read as 'text/xml'. We assume those files are OK too.
        return MIME_TYPE_SVG.equals(mimeType) || MIME_TYPE_XML.equals(mimeType);
    }

    public static String getSvgMimeType() {
        return MIME_TYPE_SVG;
    }

    private void validateSizes(final FileUpload upload) {
        String fileName = upload.getClientFileName();

        Dimension dim;
        try {
            dim = Sanselan.getImageSize(upload.getInputStream(), fileName);
        } catch (IOException e) {
            addViolation("image.validation.metadata.error", fileName);
            log.error("Error validating dimensions for file " + fileName, e);
            return;
        } catch (ImageReadException e) {
            addViolation("image.validation.metadata.error", fileName);
            log.error("Error validating dimensions for file " + fileName, e);
            return;
        }

        if (dim == null) {
            addViolation("image.validation.metadata.error", fileName);
            return;
        }

        //check image dimensions
        boolean tooWide = maxWidth > 0 && dim.width > maxWidth;
        boolean tooHigh = maxHeight > 0 && dim.height > maxHeight;
        Object[] params = new Object[] {fileName, dim.width, dim.height, maxWidth, maxHeight};

        if (tooWide && tooHigh) {
            addViolation("image.validation.width-height", params);
            log.debug("Image '{}' resolution is too high ({},{}). The max allowed width is ({}, {})", params);
        } else if (tooWide) {
            addViolation("image.validation.width", params);
            log.debug("Image '{}' is {} pixels wide while the max allowed width is {}", params);
        } else if (tooHigh) {
            addViolation("image.validation.height", params);
            log.debug("Image '{}' is {} pixels high while the max allowed height is {}", params);
        }
    }

    @Override
    protected String[] getDefaultExtensionsAllowed() {
        return DEFAULT_EXTENSIONS_ALLOWED;
    }

    @Override
    protected String getDefaultMaxFileSize() {
        return DEFAULT_MAX_FILE_SIZE;

    }
}
