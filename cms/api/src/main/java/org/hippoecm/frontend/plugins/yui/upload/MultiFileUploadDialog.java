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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

/**
 * A multi file upload dialog that can be configured by means of the {@link FileUploadWidgetSettings}.
 */
public abstract class MultiFileUploadDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private FileUploadWidget widget;
    private AjaxButton ajaxButton;

    protected MultiFileUploadDialog(IPluginConfig pluginConfig) {
        setOutputMarkupId(true);

        setNonAjaxSubmit();
        setMultiPart(true);
        setOkEnabled(false);
        setOkVisible(false);

        ajaxButton = new AjaxButton(getButtonId(), new Model<String>("Ok")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                //TODO: add AjaxBusyIndicator
                target.appendJavascript(widget.getStartAjaxUploadScript());
                //setEnabled(false);
                target.addComponent(this);
            }
        };
        ajaxButton.setEnabled(false);
        ajaxButton.setVisible(false);
        addButton(ajaxButton);

        FileUploadWidgetSettings settings = new FileUploadWidgetSettings(pluginConfig);
        widget = new FileUploadWidget("uploadWidget", settings) {

            @Override
            protected void onFileUpload(FileUpload file) {
                MultiFileUploadDialog.this.handleUploadItem(file);
            }

            @Override
            protected void onFinishAjaxUpload(AjaxRequestTarget target) {
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

            @Override
            protected String getAjaxIndicatorId() {
                return super.getAjaxIndicatorId();
            }
        };
        add(widget);
    }

    @Override
    protected void onOk() {
        if (widget.isFlashUpload()) {
            AjaxRequestTarget target = AjaxRequestTarget.get();
            if (target != null) {
                ajaxButton.setEnabled(false);
                target.addComponent(ajaxButton);
            }
        } else {
            widget.handleNonFlashSubmit();
        }
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM;
    }

    protected abstract void handleUploadItem(FileUpload upload);

}
