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
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugins.yui.upload.ajax.AjaxMultiFileUploadComponent;
import org.hippoecm.frontend.plugins.yui.upload.ajax.AjaxMultiFileUploadSettings;

import java.util.Collection;

public abstract class UploadDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private final IUploadComponent uploadComponent;

    protected UploadDialog(String[] fileExtensions) {
        setMultiPart(true);
        setOutputMarkupId(true);

        //TODO: detect if flash is enabled, otherwise fallback to javascript which should be enough for Hippo atm.
        //For now only use default MultiFileUpload from Wicket
        AjaxMultiFileUploadSettings settings = new AjaxMultiFileUploadSettings();
        if(fileExtensions != null & fileExtensions.length > 0) {
            settings.setFileExtensions(fileExtensions);
        }
        uploadComponent = new AjaxMultiFileUploadComponent("upload", settings) {

            @Override
            protected void processFileUpload(FileUpload fileUpload) {
                handleUploadItem(fileUpload);
            }

            @Override
            protected void onFinish(AjaxRequestTarget target) {
                UploadDialog.this.handleSubmit();
            }

            @Override
            protected void onUploadSuccess() {
            }

        };
        add(uploadComponent.getComponent());

        setOkEnabled(false);
        setOkVisible(false);

        addButton(new AjaxButton(getButtonId(), new Model<String>("Ok")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                target.appendJavascript("YAHOO.hippo.Upload.upload();");
            }
        });
    }

    @Override
    public IValueMap getProperties() {
        return MEDIUM;
    }

    protected abstract void handleUploadItem(FileUpload upload);

}
