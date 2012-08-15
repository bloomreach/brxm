/*
 * Copyright (c) 2012 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.yui.upload.validation;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.ValidationResult;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultUploadValidationService implements FileUploadValidationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultUploadValidationService.class);

    public static final String DEFAULT_MAX_SIZE = "2mb";
    public static final String[] DEFAULT_EXTENSIONS_ALLOWED = new String[0];

    public static final String EXTENSIONS_ALLOWED = "extensions.allowed";
    public static final String MAX_FILE_SIZE = "max.file.size";

    private ValidationResult result;

    private List<String> allowedExtensions = new LinkedList<String>();
    private Bytes maxFileSize;

    public DefaultUploadValidationService(IValueMap params) {

        final String[] fileExtensions;
        if (params.containsKey(EXTENSIONS_ALLOWED)) {
            fileExtensions = params.getStringArray(EXTENSIONS_ALLOWED);
        } else {
            fileExtensions = getDefaultExtensionsAllowed();
        }

        for (String extension : fileExtensions) {
            int pIndex = extension.indexOf("*.");
            if (pIndex > -1) {
                extension = extension.substring(pIndex + 2);
            }
            allowedExtensions.add(extension.toLowerCase());
        }

        maxFileSize = Bytes.valueOf(params.getString(MAX_FILE_SIZE, getDefaultMaxFileSize()));
    }

    @Override
    public void validate(final FileUpload upload) throws ValidationException {
        result = new ValidationResult();

        validateExtension(upload);
        validateMaxFileSize(upload);
    }

    @Override
    public void validate() throws ValidationException {
        throw new UnsupportedOperationException("Use validate(FileUpload upload) instead");
    }

    @Override
    public IValidationResult getValidationResult() {
        return result;
    }

    @Override
    public void addViolation(final String key, final Object... params) {
        result.getViolations().add(new Violation(null, key, params));
    }


    private void validateExtension(FileUpload upload) {
        if (allowedExtensions.size() > 0) {
            String fileName = upload.getClientFileName();
            int lastPeriod = fileName.lastIndexOf('.');
            if (lastPeriod == -1) {
                if (allowedExtensions.size() > 0) {
                    addViolation("upload.validation.extension.not.found",
                            fileName, StringUtils.join(allowedExtensions.iterator(), ", "));

                    if (log.isDebugEnabled()) {
                        log.debug("File '{}' has no extension. Allowed extensions are {}.",
                                new Object[]{fileName, StringUtils.join(allowedExtensions.iterator(), ", ")});
                    }
                }
            } else {
                String extension = fileName.substring(lastPeriod + 1).toLowerCase();
                if (!allowedExtensions.contains(extension)) {
                    addViolation("upload.validation.extension.not.allowed",
                            fileName, extension, StringUtils.join(allowedExtensions.iterator(), ", "));

                    if (log.isDebugEnabled()) {
                        log.debug("File '{}' has extension {} which is not allowed. Allowed extensions are {}.",
                                new Object[]{fileName, extension, StringUtils.join(allowedExtensions.iterator(), ", ")});
                    }
                }
            }
        }
    }

    private void validateMaxFileSize(final FileUpload upload) {
        Bytes fileSize = Bytes.bytes(upload.getSize());

        if (maxFileSize.compareTo(fileSize) == -1) {
            addViolation("upload.validation.filesize",
                    upload.getClientFileName(), fileSize.toString(), maxFileSize.toString());

            if (log.isDebugEnabled()) {
                log.debug("File '{}' has size {} which is too big. The maximum size allowed is {}", new Object[]{upload.getClientFileName(), fileSize.toString(), maxFileSize.toString()});
            }
        }
    }

    @Override
    public String[] getAllowedExtensions() {
        return allowedExtensions.toArray(new String[allowedExtensions.size()]);
    }

    @Override
    public Bytes getMaxFileSize() {
        return maxFileSize;
    }

    protected String getDefaultMaxFileSize() {
        return DEFAULT_MAX_SIZE;
    }

    protected String[] getDefaultExtensionsAllowed() {
        return DEFAULT_EXTENSIONS_ALLOWED;
    }

}
