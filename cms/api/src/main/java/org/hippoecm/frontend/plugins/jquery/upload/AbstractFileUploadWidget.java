/*
 * Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.jquery.upload;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.jquery.upload.behaviors.FileUploadInfo;
import org.hippoecm.frontend.plugins.yui.upload.model.UploadedFile;
import org.hippoecm.frontend.plugins.yui.upload.processor.DefaultFileUploadPreProcessorService;
import org.hippoecm.frontend.plugins.yui.upload.processor.FileUploadPreProcessorService;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The abstract file upload widget.
 *
 * When uploading a file, the widget should provide the following events in sequence:
 * <ul>
 *     <li>{@link #onBeforeUpload(FileUploadInfo)}</li>
 *     <li>{@link #onFileUpload(FileUpload fileUpload)}</li>
 *     <li>{@link #onUploadError(FileUploadInfo)}</li>
 *     <li>{@link #onAfterUpload(FileItem, FileUploadInfo)}</li>
 * </ul>
 */
public abstract class AbstractFileUploadWidget extends Panel {
    private static final Logger log = LoggerFactory.getLogger(AbstractFileUploadWidget.class);

    private final FileUploadValidationService validator;

    private final FileUploadPreProcessorService preProcessorService;

    protected final FileUploadWidgetSettings settings;

    @Deprecated
    public AbstractFileUploadWidget(String id, final IPluginConfig pluginConfig,
                                    final FileUploadValidationService validator){
        this(id, pluginConfig, validator, new DefaultFileUploadPreProcessorService());
    }

    public AbstractFileUploadWidget(String id, final IPluginConfig pluginConfig,
                                    final FileUploadValidationService validator,
                                    final FileUploadPreProcessorService preProcessorService){
        super(id);
        this.settings = new FileUploadWidgetSettings(pluginConfig, validator);
        this.validator = validator;
        this.preProcessorService = preProcessorService;
    }

    public String getUploadScript(){
        return StringUtils.EMPTY;
    }

    /**
     * Validate file upload item against the file upload validation service defined in
     * {@link #AbstractFileUploadWidget(String, IPluginConfig, FileUploadValidationService)}
     * @param fileUpload
     * @throws FileUploadViolationException
     */
    protected void validate(final FileUpload fileUpload) throws FileUploadViolationException {
        try {
            validator.validate(fileUpload);
        } catch (ValidationException e) {
            log.error("Error while validating upload", e);
            throw new FileUploadViolationException("Error while validating upload " + e.getMessage());
        }

        IValidationResult result = validator.getValidationResult();
        if (!result.isValid()){
            List<String> errors = new ArrayList<>();
            result.getViolations().forEach(violation -> errors.add(violation.getMessage().getObject()));
            throw new FileUploadViolationException(errors);
        }
    }

    /**
     * Executes custom preProcessors, a new FileUpload object will be returned
     * @param fileItem
     * @param originalFileUpload
     */
    protected FileUpload preProcess(FileItem fileItem, final FileUpload originalFileUpload) throws Exception {
        return preProcessorService.process(fileItem, originalFileUpload);
    }
    /**
     * It calls {@link #onFileUpload(FileUpload)}
     *
     * @param fileUpload
     * @throws FileUploadViolationException
     */
    protected void process(FileUpload fileUpload) throws FileUploadViolationException {
        onFileUpload(fileUpload);
    }

    public FileUploadWidgetSettings getSettings(){
        return settings;
    }

    /**
     * The event is fired before processing the uploaded file.
     */
    protected void onBeforeUpload(final FileUploadInfo fileUploadInfo) {
    }

    /**
     * Override this method to handle uploading files
     *
     * @param fileUpload
     * @throws FileUploadViolationException
     */
    protected abstract void onFileUpload(FileUpload fileUpload) throws FileUploadViolationException;

    /**
     * The event is fired when there is an error during processing uploaded file.
     * @param fileUploadInfo
     */
    protected void onUploadError(final FileUploadInfo fileUploadInfo) {}

    /**
     * The event is fired after the selecting files has been processed and uploaded.
     */
    protected void onAfterUpload(final FileItem file, final FileUploadInfo fileUploadInfo) {}

    /**
     * The event is fired after all files has been uploaded.
     *
     * @param target
     * @param numberOfFiles number of uploaded files
     * @param error <code>true</code> if there is any error in uploading files
     */
    protected void onFinished(final AjaxRequestTarget target, final int numberOfFiles, final boolean error) {
    }

}
