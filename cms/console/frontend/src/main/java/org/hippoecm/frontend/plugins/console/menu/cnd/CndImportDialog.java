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
package org.hippoecm.frontend.plugins.console.menu.cnd;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.repository.util.NodeTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CndImportDialog extends AbstractDialog<Void> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(CndImportDialog.class);

    private final FileUploadField fileUploadField;
    private final Model<String> msgText;

    public CndImportDialog() {
        setMultiPart(true);
        setNonAjaxSubmit();
        add(fileUploadField = new FileUploadField("fileInput"));

        msgText = new Model<>("Import a CND file.");
        add(new Label("message", msgText));

        setOkLabel("import");
    }

    public IModel getTitle() {
        return new Model<>("Import node type definition file");
    }

    @Override
    protected void onOk() {
        final FileUpload upload = fileUploadField.getFileUpload();
        if (upload != null) {
            msgText.setObject("File uploaded.");

            try {
                final Session session = UserSession.get().getJcrSession();
                NodeTypeUtils.initializeNodeTypes(session, upload.getInputStream(), upload.getClientFileName());
            } catch (IOException e) {
                log.error("Failed to read upload file for importing cnd", e);
                error("Failed to read upload file");
            } catch (RepositoryException e) {
                log.error("Failed to import cnd", e);
                error("Failed to import cnd");
            }
        }
    }

}
