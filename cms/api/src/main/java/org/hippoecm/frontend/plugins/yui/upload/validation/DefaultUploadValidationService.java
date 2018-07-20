/*
 * Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Strings;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.settings.ApplicationSettings;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.editor.plugins.resource.InvalidMimeTypeException;
import org.hippoecm.frontend.editor.plugins.resource.MimeTypeHelper;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.ValidationResult;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultUploadValidationService implements FileUploadValidationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultUploadValidationService.class);

    protected interface Validator extends IClusterable {
        void validate(FileUpload upload);
    }

    public static final String MAX_FILE_SIZE      = "max.file.size";
    public static final String EXTENSIONS_ALLOWED = "extensions.allowed";
    public static final String MIME_TYPES_ALLOWED = "mimetypes.allowed";

    private ValidationResult result;
    private List<Validator> validators;
    private List<String> allowedExtensions;
    private List<String> allowedMimeTypes;
    private IValueMap values;

    public DefaultUploadValidationService() {
        this(ValueMap.EMPTY_MAP);
    }

    public DefaultUploadValidationService(IValueMap params) {
        validators = new LinkedList<>();
        allowedExtensions = new LinkedList<>();
        allowedMimeTypes = new LinkedList<>();

        if (params.containsKey(EXTENSIONS_ALLOWED)) {
            setAllowedExtensions(params.getStringArray(EXTENSIONS_ALLOWED));
        } else {
            setAllowedExtensions(getDefaultExtensionsAllowed());
        }
        if (params.containsKey(MIME_TYPES_ALLOWED)) {
            setAllowedMimeTypes(params.getStringArray(MIME_TYPES_ALLOWED));
        }

        values = params;

        addValidator(this::validateExtension);

        addValidator(this::validateMaxFileSize);

        addValidator(this::validateMimeType);
    }

    protected final void addValidator(Validator validator) {
        validators.add(validator);
    }

    @Override
    public final void validate(final FileUpload upload) throws ValidationException {
        result = new ValidationResult();

        for(Validator validator : validators) {
            if (!result.isValid()) {
                return;
            }
            validator.validate(upload);
        }
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
        result.getViolations().add(new Violation(this.getClass(), key, params, null));
    }

    private void validateExtension(FileUpload upload) {
        if (allowedExtensions.size() > 0) {

            String fileName = upload.getClientFileName();
            int lastPeriod = fileName.lastIndexOf('.');
            if (lastPeriod == -1 || lastPeriod == fileName.length() - 1) {
                String allowed = StringUtils.join(allowedExtensions.iterator(), ", ");
                addViolation("file.validation.extension.unknown", fileName, allowed);
                log.debug("File '{}' has no extension. Allowed extensions are {}.", fileName, allowed);
            } else {
                String extension = fileName.substring(lastPeriod + 1).toLowerCase();
                if (!allowedExtensions.contains(extension)) {
                    String allowed = StringUtils.join(allowedExtensions.iterator(), ", ");
                    addViolation("file.validation.extension.disallowed", fileName, extension, allowed);
                    if (log.isDebugEnabled()) {
                        log.debug("File '{}' has extension {} which is not allowed. Allowed extensions are {}.",
                                  fileName, extension, allowed);
                    }
                }
            }
        }
    }

    private void validateMaxFileSize(final FileUpload upload) {
        Bytes fileSize = Bytes.bytes(upload.getSize());

        final Bytes maxFileSize = Bytes.valueOf(values.getString(MAX_FILE_SIZE, getDefaultMaxFileSize()));
        if (maxFileSize.compareTo(fileSize) == -1) {
            addViolation("file.validation.size",
                    upload.getClientFileName(), fileSize.toString(), maxFileSize.toString());

            if (log.isDebugEnabled()) {
                log.debug("File '{}' has size {} which is too big. The maximum size allowed is {}",
                          upload.getClientFileName(), fileSize.toString(), maxFileSize.toString());
            }
        }
    }

    private void validateMimeType(final FileUpload upload) {
        String mimeType = upload.getContentType();
        if (allowedMimeTypes.contains(mimeType)) {
            return;
        }
        try (InputStream is = upload.getInputStream()){
            MimeTypeHelper.validateMimeType(is, mimeType);
        } catch (InvalidMimeTypeException e) {
            addViolation("file.validation.mime.invalid", upload.getClientFileName(), mimeType);
            if (log.isDebugEnabled()) {
                log.debug("Invalid MIME type for " + upload.getClientFileName(), e);
            }
        } catch (IOException e) {
            log.error("Failed to get input stream from the uploaded file:" + upload.getClientFileName(), e);
        }
    }


    public void setAllowedMimeTypes(final String[] mimeTypes) {
        allowedMimeTypes.clear();
        for (String type : mimeTypes) {
            if (!Strings.isNullOrEmpty(type)) {
                allowedMimeTypes.add(type);
            }
        }
    }

    @Override
    public String[] getAllowedExtensions() {
        return allowedExtensions.toArray(new String[allowedExtensions.size()]);
    }

    @Override
    public void setAllowedExtensions(final String[] extensions) {
        allowedExtensions.clear();
        for (String extension : extensions) {
            int pIndex = extension.indexOf("*.");
            if (pIndex > -1) {
                extension = extension.substring(pIndex + 2);
            }
            allowedExtensions.add(extension.toLowerCase());
        }
    }

    @Override
    public Bytes getMaxFileSize() {
        return Bytes.valueOf(values.getString(MAX_FILE_SIZE, getDefaultMaxFileSize()));
    }

    /**
     * Check if the defaultMaximumUploadSize stored in the ApplicationSettings is set explicitly and only
     * then used it, otherwise use DEFAULT_MAX_FILE_SIZE. This is because it is set to Bytes.MAX
     * by default which is a bit overkill (8388608T).
     *
     * @return The String value of the default maximum file size for an upload
     */
    protected String getDefaultMaxFileSize() {
        ApplicationSettings settings = Application.get().getApplicationSettings();
        Bytes defaultSize = settings.getDefaultMaximumUploadSize();
        return Bytes.MAX.equals(defaultSize) ? DEFAULT_MAX_FILE_SIZE : defaultSize.toString();
    }

    protected String[] getDefaultExtensionsAllowed() {
        return DEFAULT_EXTENSIONS_ALLOWED;
    }

    /**
     * Get the validation service specified by the parameter {@link IValidationService#VALIDATE_ID} in the plugin config.
     * If no service id configuration is found, the service with id <code>defaultValidationServiceId</code> is used.
     * If it cannot find this service, a new default service is returned.
     */
    public static FileUploadValidationService getValidationService(final IPluginContext pluginContext,
                                                                   final IPluginConfig pluginConfig,
                                                                   final String defaultValidationServiceId) {

        String serviceId = pluginConfig.getString(FileUploadValidationService.VALIDATE_ID, defaultValidationServiceId);
        FileUploadValidationService validator = pluginContext.getService(serviceId, FileUploadValidationService.class);

        if (validator == null) {
            validator = new DefaultUploadValidationService();
            log.warn("Cannot load validation service with id '{}', using the default service '{}'",
                    serviceId, validator.getClass().getName());
        }
        return validator;
    }
}
