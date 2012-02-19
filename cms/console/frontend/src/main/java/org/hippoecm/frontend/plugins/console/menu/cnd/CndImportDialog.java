/*
 *  Copyright 2008 Hippo.
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

import java.io.BufferedInputStream;
import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CndImportDialog extends AbstractDialog<Void> {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(CndImportDialog.class);

    private FileUploadField fileUploadField;

    Component message;
    Model msgText;

    public CndImportDialog(CndImportPlugin menuPlugin) {
        setMultiPart(true);
        setNonAjaxSubmit();
        add(fileUploadField = new FileUploadField("fileInput"));

        msgText = new Model("Import a CND file.");
        message = new Label("message", msgText);
        add(message);

        setOkLabel("import");
    }

    public IModel getTitle() {
        return new Model("Import node type definition file");
    }

    @Override
    protected void onOk() {
        final FileUpload upload = fileUploadField.getFileUpload();
        if (upload != null) {
            msgText.setObject("File uploaded.");

            // create initialize node
            try {
                Node rootNode = ((UserSession) Session.get()).getJcrSession().getRootNode();
                Node initNode = rootNode.getNode(HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.INITIALIZE_PATH);

                if (initNode.hasNode("import-cnd")) {
                    initNode.getNode("import-cnd").remove();
                }
                Node node = initNode.addNode("import-cnd", HippoNodeType.NT_INITIALIZEITEM);

                try {
                    node.setProperty(HippoNodeType.HIPPO_NODETYPES, new BufferedInputStream(upload.getInputStream()));
                    rootNode.getSession().save();
                    msgText.setObject("initialize node saved.");
                } catch (IOException e) {
                    msgText.setObject("initialize node saved.");

                }
            } catch (RepositoryException e) {
                log.error("Error while creating nodetypes initialization node: ", e);
                error("Error while creating nodetypes initialization node.");
            }
        }
    }

}
