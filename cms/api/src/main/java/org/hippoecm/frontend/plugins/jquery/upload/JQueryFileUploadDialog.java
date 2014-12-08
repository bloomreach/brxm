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

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.upload.FileItem;
import org.apache.wicket.util.upload.FileUploadException;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.upload.validation.DefaultUploadValidationService;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author cngo
 * @version $Id$
 * @since 2014-11-26
 */
public abstract class JQueryFileUploadDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JQueryFileUploadDialog.class);

    public static final String FILEUPLOAD_WIDGET_ID = "uploadPanel";
    private final IPluginContext pluginContext;
    private final IPluginConfig pluginConfig;

    private FileUploadWidget fileUploadWidget;

    private final List<Violation> violations;
    private final FileUploadValidationService validator;
    private final List<String> errors = new ArrayList<>();

    protected JQueryFileUploadDialog(final IPluginContext pluginContext, final IPluginConfig pluginConfig){
        setOutputMarkupId(true);
        setMultiPart(true);

        setOkEnabled(false);
        setOkVisible(false);

        setCancelLabel(new StringResourceModel("button-close-label", JQueryFileUploadDialog.this, null, "Close"));

        this.pluginContext = pluginContext;
        this.pluginConfig = pluginConfig;

        createComponents();
        validator = getValidator();
        violations = new ArrayList<>();
    }

     private void createComponents() {
        fileUploadWidget = new FileUploadWidget(FILEUPLOAD_WIDGET_ID, pluginConfig, pluginContext){
            @Override
            protected void onFileUpload(FileItem fileItem) throws FileUploadException{
                // handle for validation
                JQueryFileUploadDialog.this.process(fileItem);
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
     * @param fileItem
     */
    public void process(FileItem fileItem) throws FileUploadException{
        try {
            validator.validate(new FileUpload(fileItem));
            IValidationResult result = validator.getValidationResult();

            if (result.isValid()){
                handleFileUpload(new FileUpload(fileItem));
            } else {
                violations.addAll(result.getViolations());
            }
            fileItem.delete();
        } catch (ValidationException e) {
            log.error("Error while validating upload", e);
        }
    }

    private void handleFileUpload(final FileUpload file) {
        try {
            onFileUpload(file);
        } catch (FileUploadException e) {
            if (log.isDebugEnabled()) {
                log.info("FileUploadException caught", e);
            } else {
                log.info("FileUploadException caught: " + e);
            }
            Throwable t = e;
            while(t != null) {
                final String translatedMessage = (String) getExceptionTranslation(t, file.getClientFileName()).getObject();
                if (translatedMessage != null && !errors.contains(translatedMessage)) {
                    errors.add(translatedMessage);
                }
                t = t.getCause();
            }
        }
    }

    /**
     * Called after uploading a file
     */
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
        if (hasError) {
            // force to display feedback panel
            onSubmit();
        }
    }

    public FileUploadValidationService getValidator() {
        String serviceId = pluginConfig.getString(FileUploadValidationService.VALIDATE_ID);
        FileUploadValidationService validator = pluginContext.getService(serviceId, FileUploadValidationService.class);

        if (validator == null) {
            validator = new DefaultUploadValidationService();
        }
        return validator;
    }

    protected abstract void onFileUpload(FileUpload file) throws FileUploadException;
}
