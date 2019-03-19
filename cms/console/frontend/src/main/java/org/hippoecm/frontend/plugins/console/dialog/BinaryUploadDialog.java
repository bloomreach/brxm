/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.dialog;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.value.BinaryImpl;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryUploadDialog extends Dialog<Void> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BinaryUploadDialog.class);

    private final FileUploadField fileUploadField;
    private final Model<String> msgText;
    private final JcrPropertyModel model;

    public BinaryUploadDialog(final JcrPropertyModel model) {
        setTitle(Model.of("Update binary property"));

        this.model = model;
        setMultiPart(true);
        setNonAjaxSubmit();
        add(fileUploadField = new FileUploadField("fileInput"));

        msgText = new Model<>("Upload new binary file");
        add(new Label("message", msgText));

        setOkLabel("Import");
    }

    @Override
    protected void onOk() {
        final FileUpload upload = fileUploadField.getFileUpload();
        if (upload != null) {
            msgText.setObject("File uploaded.");

            try {
                final InputStream inputStream = upload.getInputStream();
                final Property property = model.getProperty();
                final Binary binary = new BinaryImpl(inputStream);
                property.setValue(binary);
            } catch (RepositoryException e) {
                log.error("Failed to upload binary", e);
                error("Failed to upload binary");
            } catch (IOException e) {
                log.error("Error creating binary", e);
                error("Error updating binary");
            }
        }
    }
}
