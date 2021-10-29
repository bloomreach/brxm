/*
 * Copyright 2012-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.settings.ApplicationSettings;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.editor.plugins.resource.InvalidMimeTypeException;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.yui.upload.MagicMimeTypeFileItem;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.SvgValidationResult;
import org.hippoecm.frontend.validation.SvgValidator;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.ValidationResult;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trim;

public class DefaultUploadValidationService implements FileUploadValidationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultUploadValidationService.class);

    protected interface Validator extends IClusterable {
        void validate(FileUpload upload);
    }

    public static final String MAX_FILE_SIZE      = "max.file.size";
    public static final String EXTENSIONS_ALLOWED = "extensions.allowed";
    public static final String EXTENSION_MIMETYPE_ALLOWED_MAPPINGS = "extension.mimetype.allowed.mappings";
    public static final String MIME_TYPES_ALLOWED = "mimetypes.allowed";

    private static final String SVG_MIME_TYPE = "image/svg+xml";
    private final String SVG_SCRIPTS_ENABLED = "svg.scripts.enabled";

    private ValidationResult result;
    private List<Validator> validators;
    private List<String> allowedExtensions;
    private boolean svgScriptsEnabled;
    private Map<String, String> extensionMimeTypeAllowedMappings = new HashMap<>();

    private IValueMap values;

    public DefaultUploadValidationService() {
        this(ValueMap.EMPTY_MAP);
    }

    public DefaultUploadValidationService(IValueMap params) {
        validators = new LinkedList<>();
        allowedExtensions = new LinkedList<>();

        if (params.containsKey(EXTENSIONS_ALLOWED)) {
            setAllowedExtensions(params.getStringArray(EXTENSIONS_ALLOWED));
        } else {
            setAllowedExtensions(getDefaultExtensionsAllowed());
        }
        if (params.containsKey(MIME_TYPES_ALLOWED)) {
            log.warn("Allowed mimetypes which was used to skip content mimetype validation of certain mimetypes is not" +
                    "supported any more. All mimetypes are checked for content mimetype validation");
        }
        svgScriptsEnabled = params.getAsBoolean(SVG_SCRIPTS_ENABLED, false);

        // default correct mapping from .psd and .ps : some browsers send a mimeType which is not a registered
        // (sub/super) mimeType by tika. Hence we add these by default to the extensionMimeTypeAllowedMapping
        extensionMimeTypeAllowedMappings.put(".psd", "image/vnd.adobe.photoshop");
        extensionMimeTypeAllowedMappings.put(".ps", "application/x-font-type1");

        if (params.containsKey(EXTENSION_MIMETYPE_ALLOWED_MAPPINGS)) {
            addExtensionMimeTypeAllowedMapping(params.getStringArray(EXTENSION_MIMETYPE_ALLOWED_MAPPINGS));
        }

        values = params;

        addValidator(this::validateExtension);

        addValidator(this::validateMaxFileSize);

        addValidator(this::validateMimeType);

        addValidator(this::validateSvg);
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
        final ClassResourceModel messageModel = new ClassResourceModel(key, getClass(), params);
        result.getViolations().add(new Violation(Collections.emptySet(), messageModel));
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
        try {
            // this validates the browser provided mimetype (contentType) against the actual content its mimetype detected via
            // Tika, see MagicMimeTypeFileItem#getContentType : in case the browser provided mimetype does not match
            // the content detected mimetype, eg when a .exe is renamed to a .pdf, an InvalidMimeTypeException will be
            // thrown
            // Unfortunately, we only want the upload#getContentType() to throw potentially a InvalidMimeTypeException
            // during this check, but not otherwise. The only feasible way is unfortunately an thread local. Ideally,
            // we could use the 'isExtensionMimeTypeAllowedMappings' below in the MagicMimeTypeFileItem but this is
            // really not doable at the moment and would require a real service in the HippoServiceRegistry to have the
            // extensionMimeTypeAllowedMappings available in MagicMimeTypeFileItem
            MagicMimeTypeFileItem.mimetypeValidationContext.set(Boolean.TRUE);
            upload.getContentType();
        } catch (InvalidMimeTypeException e) {
            // check if there is an explicit hardcoded or configred configuration to allow the mapping nonetheless
            if (isExtensionMimeTypeAllowedMappings(upload.getClientFileName(), e.getTikaDetectedContentType())) {
                log.debug("Mimetype '{}' for extension file '{}' is explicitly allowed", e.getTikaDetectedContentType(), upload.getClientFileName());
            } else {
                addViolation("file.validation.mime.invalid", upload.getClientFileName(), e.getTikaDetectedContentType() == null ? "unknown" : e.getTikaDetectedContentType());
                log.debug("Invalid MIME type for {}", upload.getClientFileName(), e);
            }
        } finally {
            MagicMimeTypeFileItem.mimetypeValidationContext.remove();
        }

    }

    private void validateSvg(final FileUpload upload){
        final String mimeType = upload.getContentType();
        if (SVG_MIME_TYPE.equals(mimeType) && !svgScriptsEnabled){
            try (InputStream is = upload.getInputStream()){
                final SvgValidationResult svgValidationResult = SvgValidator.validate(is);
                if (!svgValidationResult.isValid()){
                    addViolation("file.validation.svg.disallowed", upload.getClientFileName()
                    , svgValidationResult.getOffendingElements()
                    , svgValidationResult.getOffendingAttributes());
                }
            } catch (ParserConfigurationException | SAXException e) {
                log.error("Something went wrong during the upload of the svg", e);
            } catch (IOException e){
                log.error("Failed to get input stream from the uploaded file:" + upload.getClientFileName(), e);
            }
        }
    }


    /**
     * @deprecated since 14.7.0 : setting allowed mime types does not do anything any more
     */
    @Deprecated
    public void setAllowedMimeTypes(final String[] mimeTypes) {
        log.warn("Allowed mimetypes which was used to skip content mimetype validation of certain mimetypes is not" +
                "support any more. All mimetypes are checked for content mimetype validation");
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


    private void addExtensionMimeTypeAllowedMapping(final String[] mappings) {
        if (mappings == null) {
            return;
        }

        Arrays.stream(mappings).forEach(mapping -> {
            if (mapping.indexOf(".") != 0) {
                logInvalidMapping(mapping);
            } else if (mapping.indexOf(",") == -1) {
                logInvalidMapping(mapping);
            } else {
                final String extension = trim(substringBefore(mapping,","));
                final String mimeType = trim(substringAfter(mapping,","));
                extensionMimeTypeAllowedMappings.put(extension, mimeType);
            }
        });
    }

    private void logInvalidMapping(final String mapping) {
        log.warn("Incorrect extensionMimeTypeAllowedMappings entry found '{}' : the extension should " +
                "start with a '.' and between the extension and mimeType there should be a ',', eg " +
                ".psd,image/vnd.adobe.photoshop", mapping);
    }

    /**
     * <p>
     *     Some extensions are send as a different mimetype by some browsers than the detected mimetype by Tika. For
     *     example firefox sends mimetype
     * </p>
     */
    private boolean isExtensionMimeTypeAllowedMappings(final String fileName, final String tikaDetectedContentType) {
        if (fileName == null || tikaDetectedContentType == null) {
            return false;
        }
        try {
            String lowercaseExtension = getLowercaseExtension(fileName);
            if (tikaDetectedContentType.equalsIgnoreCase(extensionMimeTypeAllowedMappings.get(lowercaseExtension))) {
                log.debug("Explicit matched mapping found for browser provided mimetype '{}' to Tika detected mimetype '{}'",
                        fileName, tikaDetectedContentType);
                return true;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return false;
    }


    /**
     * <p>
     *     Returns lowercase extension include the dot (.)
     * </p>
     */
    private String getLowercaseExtension(final String fileName) {
        if (fileName == null) {
            return null;
        }
        int extensionIndex = fileName.lastIndexOf(".");
        if (extensionIndex == -1 ) {
            String allowed = StringUtils.join(allowedExtensions.iterator(), ", ");
            addViolation("file.validation.extension.unknown", fileName, allowed);
            log.debug("File '{}' has no extension. Allowed extensions are {}.", fileName, allowed);
            throw new IllegalArgumentException();
        }

        String lowercaseExtension = fileName.substring(extensionIndex).toLowerCase();
        return lowercaseExtension;
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
