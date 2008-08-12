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
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CndImportDialog extends AbstractDialog implements ITitleDecorator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(CndImportDialog.class);
    
    Component message;
    Model msgText;

    public CndImportDialog(MenuPlugin plugin, IPluginContext context, IDialogService dialogWindow) {
        super(context, dialogWindow);
        
        final FileUploadForm simpleUploadForm = new FileUploadForm("simpleUpload");
        add(simpleUploadForm);
        msgText = new Model("Import a CND file.");
        message = new Label("message", msgText);
        cancel.setVisible(false);
        add(message);
    }

    public String getTitle() {
        return "Import node type definition file";
    }


    /**
     * Form the upload.
     */
    private class FileUploadForm extends Form {
        private static final long serialVersionUID = 1L;

        private FileUploadField fileUploadField;

        public FileUploadForm(String name) {
            super(name);
            setMultiPart(true);
            add(fileUploadField = new FileUploadField("fileInput"));
        }

        @Override
        protected void onSubmit() {
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
                        node.setProperty(HippoNodeType.HIPPO_CONTENT, new BufferedInputStream(upload.getInputStream()));
                        rootNode.getSession().save();
                        msgText.setObject("initialize node saved.");
                    } catch (IOException e) {
                        msgText.setObject("initialize node saved.");
                        
                    }
                } catch (RepositoryException e) {
                    log.error("Error while creating nodetypes initialization node: ", e);
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }


    @Override
    protected void cancel() {
    }

    @Override
    protected void ok() throws Exception {
    }
}
