/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.upload.FileItem;
import org.apache.wicket.util.upload.FileUploadException;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.upload.validation.DefaultUploadValidationService;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

public abstract class JQueryFileUploadDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JQueryFileUploadDialog.class);

    public static final String FILEUPLOAD_WIDGET_ID = "uploadPanel";
    public static final String UPLOADING_SCRIPT = "jqueryFileUploadImpl.uploadFiles()";
    private final IPluginContext pluginContext;
    private final IPluginConfig pluginConfig;

    private FileUploadWidget fileUploadWidget;

    private final List<Violation> violations;
    private final FileUploadValidationService validator;
    private final List<String> errors = new ArrayList<>();
    private final Button ajaxOkButton;

    protected JQueryFileUploadDialog(final IPluginContext pluginContext, final IPluginConfig pluginConfig){
        setOutputMarkupId(true);
        setMultiPart(true);

        setOkVisible(false);
        setOkEnabled(false);

        // create custom OK button to call javascript uploading
        ajaxOkButton = new AjaxButton(DialogConstants.BUTTON, new StringResourceModel("ok", this, null)){
            private boolean isUploading = false;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                log.debug("Submitting files");
                isUploading = true;
            }

            @Override
            protected String getOnClickScript(){
                return UPLOADING_SCRIPT;
            }
            @Override
            public boolean isEnabled(){
                return !isUploading;
            }
        };
        ajaxOkButton.setEnabled(true);
        ajaxOkButton.setVisible(true);
        ajaxOkButton.add(new InputBehavior(new KeyType[]{KeyType.Enter}, EventType.click));
        this.addButton(ajaxOkButton);

        this.pluginContext = pluginContext;
        this.pluginConfig = pluginConfig;

        createComponents();
        validator = getValidator();
        violations = new ArrayList<>();
    }

     private void createComponents() {
        fileUploadWidget = new FileUploadWidget(FILEUPLOAD_WIDGET_ID, pluginConfig){
            @Override
            protected void onFileUpload(FileItem fileItem) throws FileUploadViolationException {
                // handle for validation
                JQueryFileUploadDialog.this.process(new FileUpload(fileItem));
            }

            @Override
            protected void onFileUploadFinished() {
                JQueryFileUploadDialog.this.onFinished();
            }

        };
        add(fileUploadWidget);
    }

    /**
     * process uploading file
     * @param fileUpload
     */
    protected void process(FileUpload fileUpload) throws FileUploadViolationException {
        try {
            validator.validate(fileUpload);
        } catch (ValidationException e) {
            log.error("Error while validating upload", e);
            throw new FileUploadViolationException("Error while validating upload" + e.getMessage());
        }

        IValidationResult result = validator.getValidationResult();
        try{
            if (result.isValid()){
                handleFileUpload(fileUpload);
            } else {
                violations.addAll(result.getViolations());                // will be removed
                List<String> errors = new ArrayList<>();
                for(Violation violation : result.getViolations()){
                    errors.add(violation.getMessage().getObject());
                }
                throw new FileUploadViolationException(errors);
            }
        }finally {
            // remove from cache
            fileUpload.delete();
        }
    }

    private void handleFileUpload(final FileUpload file) throws FileUploadViolationException {
        try {
            onFileUpload(file);
        } catch (FileUploadException e) {
            if (log.isDebugEnabled()) {
                log.debug("FileUploadException caught", e);
            } else {
                log.info("FileUploadException caught: " + e);
            }
            List<String> errorMsgs = new ArrayList<>();
            Throwable t = e;
            while(t != null) {
                final String translatedMessage = (String) getExceptionTranslation(t, file.getClientFileName()).getObject();
                if (translatedMessage != null && !errors.contains(translatedMessage)) {
                    this.errors.add(translatedMessage);
                    errorMsgs.add(translatedMessage);
                }
                t = t.getCause();
            }

            throw new FileUploadViolationException(errorMsgs);
        }
    }

    /**
     * Called after uploading a file
     * @deprecated do not use feedback panel to display error messages anymore.
     */
    @Deprecated
    protected void onFinished(){
        boolean hasError = false;
        if (violations.size() > 0) {
            for (Violation violation : violations) {
                error(violation.getMessage().getObject());
            }
            violations.clear();
            hasError = true;
        }

        if (!errors.isEmpty()){
            for (String errMsg : errors) {
                error(errMsg);
            }
            errors.clear();
            hasError = true;
        }
//        if (hasError) {
//            // force to display feedback panel
//            onSubmit();
//        }
    }

    protected FileUploadValidationService getValidator() {
        String serviceId = pluginConfig.getString(FileUploadValidationService.VALIDATE_ID);
        FileUploadValidationService validator = pluginContext.getService(serviceId, FileUploadValidationService.class);

        if (validator == null) {
            validator = new DefaultUploadValidationService();
        }
        return validator;
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM;
    }

    protected abstract void onFileUpload(FileUpload file) throws FileUploadException;
}
