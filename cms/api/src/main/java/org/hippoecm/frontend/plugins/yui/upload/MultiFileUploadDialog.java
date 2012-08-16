/*
 *  Copyright 2010 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.yui.upload;

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;

/**
 * A multi file upload dialog that can be configured by means of the {@link FileUploadWidgetSettings}.
 */
public abstract class MultiFileUploadDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;


    private FileUploadWidget widget;
    private AjaxButton ajaxButton;
    private AjaxButton closeButton;

    private List<String> errors = new LinkedList<String>();

    protected MultiFileUploadDialog(IPluginContext pluginContext, IPluginConfig pluginConfig) {
        setOutputMarkupId(true);

        setNonAjaxSubmit();
        setMultiPart(true);
        setOkEnabled(false);
        setOkVisible(false);

        ajaxButton = new AjaxButton(DialogConstants.BUTTON, new Model<String>("Ok")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                //TODO: add AjaxBusyIndicator
                target.appendJavascript(widget.getStartAjaxUploadScript());
                target.addComponent(this);
            }
        };
        ajaxButton.setEnabled(false);
        ajaxButton.setVisible(false);
        addButton(ajaxButton);

        closeButton = new AjaxButton(DialogConstants.BUTTON, new Model<String>("Close")) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                closeDialog();
            }
        };
        closeButton.setEnabled(false);
        closeButton.setVisible(false);
        addButton(closeButton);

        String serviceId = pluginConfig.getString(FileUploadValidationService.VALIDATE_ID);
        FileUploadValidationService validator = pluginContext.getService(serviceId, FileUploadValidationService.class);

        FileUploadWidgetSettings settings = new FileUploadWidgetSettings(pluginConfig);
        widget = new FileUploadWidget("uploadWidget", settings, validator) {

            @Override
            protected void onFileUpload(FileUpload file) {
                try {
                    MultiFileUploadDialog.this.handleUploadItem(file);
                } catch(FileUploadException e) {
                    Throwable t = e.getCause();
                    String message = t.getLocalizedMessage();
                    if(t.getCause() != null) {
                        message +=  "<br/>" + t.getCause();
                        log.error("FileUploadException caught", t);
                    }
                    errors.add(message);
                }
            }

            @Override
            public void onFinishHtmlUpload() {
                super.onFinishHtmlUpload();
                handleErrors();
            }

            @Override
            protected void onFinishAjaxUpload(AjaxRequestTarget target) {
                super.onFinishAjaxUpload(target);
                handleErrors();

                if (hasFeedbackMessage() || MultiFileUploadDialog.this.hasFeedbackMessage()) {
                    setCancelVisible(false);
                    ajaxButton.setVisible(false);
                    removeButton(ajaxButton);
                    closeButton.setVisible(true);
                    closeButton.setEnabled(true);
                    setVisible(false);

                    target.addComponent(MultiFileUploadDialog.this);
                    target.appendJavascript(getAjaxIndicatorStopScript());
                }
                MultiFileUploadDialog.super.handleSubmit();
            }

            @Override
            public void renderFlashUpload() {
                super.renderFlashUpload();

                ajaxButton.setEnabled(true);
                ajaxButton.setVisible(true);

                AjaxRequestTarget target = AjaxRequestTarget.get();
                if (target != null) {
                    target.addComponent(MultiFileUploadDialog.this);
                }
            }

            @Override
            public void renderJavascriptUpload() {
                super.renderJavascriptUpload();

                setOkEnabled(true);
                setOkVisible(true);

                AjaxRequestTarget target = AjaxRequestTarget.get();
                if (target != null) {
                    target.addComponent(MultiFileUploadDialog.this);
                }
            }
        };
        add(widget);

        //The feedbackPanel of the AbstractDialog does not render messages when using the flashUpload. To work around
        //this use a local feedbackPanel in the case of a flash upload.
        Panel fp = new FeedbackPanel("feedbackPanel") {
            @Override
            public boolean isEnabled() {
                return widget.isFlashUpload();
            }

            @Override
            public boolean isVisible() {
                return widget.isFlashUpload();
            }
        };
        fp.setEscapeModelStrings(false);
        fp.add(new AttributeAppender("class", true, new Model<String>("hippo-modal-feedback"), " "));
        fp.add(new AttributeAppender("class", true, new Model<String>("upload-feedback-panel"), " "));
        add(fp);
    }

    private void handleErrors() {
        if (errors.size() > 0) {
            for (String error : errors) {
                MultiFileUploadDialog.this.error(error);
            }
            errors.clear();
        }
    }

    @Override
    protected void onOk() {
        if (!widget.isFlashUpload()) {
            widget.onFinishHtmlUpload();
        }
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM;
    }

    protected abstract void handleUploadItem(FileUpload upload) throws FileUploadException;

}
