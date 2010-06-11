/*
 * Copyright 2010 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
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

public abstract class MultiFileUploadDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private MultiFileUploadWidget widget;
    private AjaxButton ajaxButton;

    @Override
    protected void onOk() {
        if (!widget.isFlash()) {
            widget.handleNonFlashSubmit();
        }
    }

    protected MultiFileUploadDialog(String[] fileExtensions) {
        setOutputMarkupId(true);

        setNonAjaxSubmit();
        setMultiPart(true);
        setOkEnabled(false);
        setOkVisible(false);

//        ajaxButton = new AjaxButton(getButtonId(), new Model<String>("Ok")) {
//
//            @Override
//            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
//                //TODO: add AjaxBusyIndicator
//                target.appendJavascript("YAHOO.hippo.Upload.upload();");
//            }
//        };
//        ajaxButton.setEnabled(false);
//        ajaxButton.setVisible(false);
//        addButton(ajaxButton);


        widget = new MultiFileUploadWidget("uploadWidget", fileExtensions) {

            @Override
            protected void onFileUpload(FileUpload file) {
                MultiFileUploadDialog.this.handleUploadItem(file);
            }

            @Override
            protected void onFinishAjaxUpload(AjaxRequestTarget target) {
                MultiFileUploadDialog.this.handleSubmit();
            }

            @Override
            public void handleFlash(AjaxRequestTarget target) {
                super.handleFlash(target);

                ajaxButton = new AjaxButton(getButtonId(), new Model<String>("Ok")) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        //TODO: add AjaxBusyIndicator
                        target.appendJavascript("YAHOO.hippo.Upload.upload();");
                    }
                };
                ajaxButton.setEnabled(false);
                ajaxButton.setVisible(false);
                addButton(ajaxButton);

//                ajaxButton.setEnabled(true);
//                ajaxButton.setVisible(true);

                target.addComponent(MultiFileUploadDialog.this);
            }

            @Override
            public void handleJavascript(AjaxRequestTarget target) {
                super.handleJavascript(target);

                setOkEnabled(true);
                setOkVisible(true);

                target.addComponent(MultiFileUploadDialog.this);
            }
        };
        add(widget);
    }

    @Override
    public IValueMap getProperties() {
        return MEDIUM;
    }

    protected abstract void handleUploadItem(FileUpload upload);

}
