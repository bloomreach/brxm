/*
 *  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.IAjaxCallListener;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmitter;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.behaviors.EventStoppingDecorator;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

/**
 * A multi file upload dialog that can be configured by means of the {@link FileUploadWidgetSettings}.
 */
public abstract class MultiFileUploadDialog<T> extends AbstractDialog<T> {
    private static final long serialVersionUID = 1L;

    private FileUploadWidget widget;
    private AjaxButton ajaxButton;
    private AjaxButton closeButton;

    private List<String> errors = new LinkedList<>();

    protected MultiFileUploadDialog(IPluginContext pluginContext, IPluginConfig pluginConfig) {
        setOutputMarkupId(true);

        setMultiPart(true);
        setOkEnabled(false);
        setOkVisible(false);

        ajaxButton = new AjaxButton(DialogConstants.BUTTON, new Model<String>("Ok")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                if (widget.isFlashUpload()) {
                    target.appendJavaScript(widget.getStartAjaxUploadScript());
                } else {
                    handleSubmit();
                    target.add(MultiFileUploadDialog.this);
                }
            }

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                final List<IAjaxCallListener> ajaxCallListeners = attributes.getAjaxCallListeners();
                ajaxCallListeners.add(new EventStoppingDecorator());
                ajaxCallListeners.add(new AjaxCallListener() {

                    @Override
                    public CharSequence getBeforeHandler(final Component component) {
                        return "if (" + widget.hasFileSelectedScript() + ") { this.disabled = true; }";
                    }

                    @Override
                    public CharSequence getPrecondition(Component component) {
                        return widget.hasFileSelectedScript();
                    }

                    @Override
                    public CharSequence getSuccessHandler(Component component) {
                        return "if (Wicket.$('" + getMarkupId() + "') != null) { Wicket.$('" + getMarkupId() + "').disabled = true; }";
                    }

                    @Override
                    public CharSequence getFailureHandler(Component component) {
                        return "if (Wicket.$('" + getMarkupId() + "') != null) { Wicket.$('" + getMarkupId() + "').disabled = false; }";
                    }
                });
            }

        };
        ajaxButton.setEnabled(true);
        ajaxButton.setVisible(true);
        ajaxButton.add(new InputBehavior(new KeyType[]{KeyType.Enter}, EventType.click));
        addButton(ajaxButton);

        closeButton = new AjaxButton(DialogConstants.BUTTON, new Model<String>("Close")) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                closeDialog();
            }
        };
        closeButton.setEnabled(false);
        closeButton.setVisible(false);
        closeButton.add(new InputBehavior(new KeyType[]{KeyType.Escape}, EventType.click));
        addButton(closeButton);

        String serviceId = pluginConfig.getString(FileUploadValidationService.VALIDATE_ID);
        FileUploadValidationService validator = pluginContext.getService(serviceId, FileUploadValidationService.class);

        FileUploadWidgetSettings settings = new FileUploadWidgetSettings(pluginConfig);
        widget = new FileUploadWidget("uploadWidget", settings, validator) {

            @Override
            protected void onFileUpload(FileUpload file) {
                try {
                    MultiFileUploadDialog.this.handleUploadItem(file);
                } catch (FileUploadException e) {
                    if (log.isDebugEnabled()) {
                        log.info("FileUploadException caught", e);
                    } else {
                        log.info("FileUploadException caught: " + e);
                    }
                    Throwable t = e;
                    while(t != null) {
                        final String translatedMessage = getExceptionTranslation(t, file.getClientFileName()).getObject();
                        if (translatedMessage != null && !errors.contains(translatedMessage)) {
                            errors.add(translatedMessage);
                        }
                        t = t.getCause();
                    }
                }
            }

            @Override
            public void onFinishHtmlUpload() {
                super.onFinishHtmlUpload();
                handleErrors();

                if (hasFeedbackMessage() || MultiFileUploadDialog.this.hasFeedbackMessage()) {
                    transformIntoErrorDialog(RequestCycle.get().find(AjaxRequestTarget.class));
                }
            }

            @Override
            protected void onFinishAjaxUpload(AjaxRequestTarget target) {
                super.onFinishAjaxUpload(target);
                handleErrors();

                if (hasFeedbackMessage() || MultiFileUploadDialog.this.hasFeedbackMessage()) {
                    transformIntoErrorDialog(target);
                } else {
                    MultiFileUploadDialog.super.handleSubmit();
                }
                target.appendJavaScript(widget.getAjaxIndicatorStopScript());
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

    private void transformIntoErrorDialog(final AjaxRequestTarget target) {
        setCancelVisible(false);
        setOkVisible(false);
        ajaxButton.setVisible(false);
        ajaxButton.setEnabled(false);
        closeButton.setVisible(true);
        closeButton.setEnabled(true);
        widget.setVisible(false);

        if (target != null) {
            target.add(MultiFileUploadDialog.this);
        }
    }

    private void handleErrors() {
        if (errors.size() > 0) {
            for (String error : errors) {
                error(error);
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
    public void process(IFormSubmitter component) {
        final WebRequest request = (WebRequest) RequestCycle.get().getRequest();
        if (request.isAjax() && RequestCycle.get().find(AjaxRequestTarget.class) == null) {
            WebApplication app = (WebApplication) getComponent().getApplication();
            AjaxRequestTarget target = app.newAjaxRequestTarget(getComponent().getPage());
            RequestCycle.get().scheduleRequestHandlerAfterCurrent(target);
        }
        super.process(component);
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM;
    }

    protected abstract void handleUploadItem(FileUpload upload) throws FileUploadException;

}
