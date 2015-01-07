/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadViolationException;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadWidgetSettings;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author cngo
 * @version $Id$
 * @since 2015-01-05
 */
public abstract class AbstractFileUploadWidget extends Panel {
    private static final Logger log = LoggerFactory.getLogger(AbstractFileUploadWidget.class);

    private final FileUploadValidationService validator;

    protected final FileUploadWidgetSettings settings;

    public AbstractFileUploadWidget(String id, final IPluginConfig pluginConfig, final FileUploadValidationService validator){
        super(id);
        this.settings = new FileUploadWidgetSettings(pluginConfig, validator);
        this.validator = validator;
    }

    /**
     * Validate file upload item against the file upload validation service defined in
     * {@link #AbstractFileUploadWidget(String, IPluginConfig, FileUploadValidationService)} then call {@link #onFileUpload(FileUpload)}
     *
     * @param fileUpload
     * @throws FileUploadViolationException
     */
    protected void process(FileUpload fileUpload) throws FileUploadViolationException {
        try {
            validator.validate(fileUpload);
        } catch (ValidationException e) {
            log.error("Error while validating upload", e);
            throw new FileUploadViolationException("Error while validating upload " + e.getMessage());
        }

        IValidationResult result = validator.getValidationResult();
        if (result.isValid()){
            onFileUpload(fileUpload);
        } else {
            List<String> errors = new ArrayList<>();
            result.getViolations().forEach(violation -> errors.add(violation.getMessage().getObject()));
            throw new FileUploadViolationException(errors);
        }
    }

    public FileUploadWidgetSettings getSettings(){
        return settings;
    }

    /**
     * Override this method to handle uploading files
     *
     * @param fileUpload
     * @throws FileUploadViolationException
     */
    protected abstract void onFileUpload(FileUpload fileUpload) throws FileUploadViolationException;
}
