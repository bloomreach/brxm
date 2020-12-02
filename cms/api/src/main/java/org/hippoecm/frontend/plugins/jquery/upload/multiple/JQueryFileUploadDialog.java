/*
 * Copyright 2014-2020 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.jquery.upload.multiple;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.buttons.ButtonStyle;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadViolationException;
import org.hippoecm.frontend.plugins.yui.upload.processor.DefaultFileUploadPreProcessorService;
import org.hippoecm.frontend.plugins.yui.upload.processor.FileUploadPreProcessorService;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.hippoecm.frontend.plugins.yui.upload.validation.ImageUploadValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

/**
 * The multi-files upload dialog using jQuery File Upload plugin
 */
public abstract class JQueryFileUploadDialog extends Dialog {
    private static final Logger log = LoggerFactory.getLogger(JQueryFileUploadDialog.class);

    public static final String FILEUPLOAD_WIDGET_ID = "uploadPanel";
    private final IPluginContext pluginContext;
    private final IPluginConfig pluginConfig;

    private FileUploadWidget fileUploadWidget;

    private final FileUploadValidationService validator;
    private final FileUploadPreProcessorService fileUploadPreProcessorService;
    private final Button uploadButton;
    private boolean isUploadButtonEnabled;

    protected JQueryFileUploadDialog(final IPluginContext pluginContext, final IPluginConfig pluginConfig){
        setOutputMarkupId(true);
        setMultiPart(true);

        setOkVisible(false);
        setOkEnabled(false);
        setCancelLabel(new ResourceModel("button-close-label"));

        uploadButton = new AjaxButton(DialogConstants.BUTTON, new ResourceModel("button-upload-label")){
            @Override
            protected String getOnClickScript(){
                return fileUploadWidget.getUploadScript();
            }

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                setCancelEnabled(false);
                isUploadButtonEnabled = false;
                target.add(this);
            }

            @Override
            public boolean isEnabled() {
                return isUploadButtonEnabled;
            }
        };
        uploadButton.add(new InputBehavior(new KeyType[]{KeyType.Enter}, EventType.click));
        uploadButton.setOutputMarkupId(true);
        uploadButton.add(ClassAttribute.append(ButtonStyle.PRIMARY.getCssClass()));
        this.addButton(uploadButton);

        this.pluginContext = pluginContext;
        this.pluginConfig = pluginConfig;
        this.validator = getValidator();
        this.fileUploadPreProcessorService = getPreProcessor();

        createComponents();
    }

     private void createComponents() {
        fileUploadWidget = new FileUploadWidget(FILEUPLOAD_WIDGET_ID, pluginConfig, validator,
                fileUploadPreProcessorService){

            @Override
            protected void onFileUpload(final FileUpload fileUpload) throws FileUploadViolationException {
                JQueryFileUploadDialog.this.handleFileUpload(fileUpload);
            }

            @Override
            protected void onFinished(final AjaxRequestTarget target, final int numberOfFiles, final boolean error) {
                JQueryFileUploadDialog.this.onFinished();

                if (!error) {
                    JQueryFileUploadDialog.this.closeDialog();
                }
                else {
                    JQueryFileUploadDialog.this.setCancelEnabled(true);
                }
            }

            @Override
            protected void onSelectionChange(final AjaxRequestTarget target) {
                final int numberOfValidFiles = this.getNumberOfValidFiles();
                final int numberOfSelectedFiles = this.getNumberOfSelectedFiles();

                isUploadButtonEnabled = (numberOfValidFiles > 0) && (numberOfSelectedFiles <= settings.getMaxNumberOfFiles());
                target.add(uploadButton);
            }
        };
        add(fileUploadWidget);
    }

    /**
     * Invoke file upload event and translate error messages.
     *
     * @param file
     * @throws FileUploadViolationException
     */
    private void handleFileUpload(final FileUpload file) throws FileUploadViolationException {
        try {
            onFileUpload(file);
        } catch (FileUploadException e) {
            List<String> errors = new ArrayList<>();
            Throwable t = e;
            while(t != null) {
                final String translatedMessage = (String) getExceptionTranslation(t, file.getClientFileName()).getObject();
                if (translatedMessage != null && !errors.contains(translatedMessage)) {
                    errors.add(translatedMessage);
                }
                t = t.getCause();
            }
            if (log.isDebugEnabled()) {
                log.debug("FileUploadException caught: {}", StringUtils.join(errors.toArray(), ";"), e);
            } else {
                log.info("FileUploadException caught: ", e);
            }
            throw new FileUploadViolationException(errors);
        }
    }

    /**
     * Called when uploading files has done.
     */
    protected void onFinished() {
        if (log.isDebugEnabled()) {
            log.debug("Finished uploading files");
        }
    }

    protected FileUploadValidationService getValidator() {
        return ImageUploadValidationService.getValidationService(pluginContext, pluginConfig);
    }

    protected FileUploadPreProcessorService getPreProcessor() {
        return DefaultFileUploadPreProcessorService.getPreProcessorService(pluginContext, pluginConfig);
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM;
    }

    protected abstract void onFileUpload(FileUpload file) throws FileUploadException;
}
