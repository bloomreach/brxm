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
package org.hippoecm.frontend.plugins.console.menu.content;

import java.io.File;
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
import org.apache.wicket.util.file.Folder;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentImportDialog  extends AbstractDialog implements ITitleDecorator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ContentImportDialog.class);

    private final IServiceReference<MenuPlugin> pluginRef;
    private final JcrNodeModel nodeModel;
    
    Component message;
    Model msgText;

    public ContentImportDialog(MenuPlugin plugin, IPluginContext context, IDialogService dialogWindow) {
        super(context, dialogWindow);

        pluginRef = context.getReference(plugin);
        nodeModel = (JcrNodeModel) plugin.getModel();
        
        final FileUploadForm simpleUploadForm = new FileUploadForm("simpleUpload");
        add(simpleUploadForm);
        try {
            msgText = new Model("Import content from a file to node: " + nodeModel.getNode().getPath());
            message = new Label("message", msgText);
            cancel.setVisible(false);
            add(message);
            
        } catch (RepositoryException e) {
            log.error("Error getting node from model for contant import",e);
            throw new RuntimeException("Error getting node from model for contant import: " + e.getMessage());
        }
    }

    public String getTitle() {
        return "Import content from file";
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
                // create temporary file
                File newFile;
                try {
                    newFile = File.createTempFile(upload.getClientFileName() + ".", ".xml", getUploadFolder() );
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to create temp file");
                }
                msgText.setObject("created temp file: " + upload.getClientFileName());
                
                // upload to temp file
                try {
                    upload.writeTo(newFile);
                    log.info("saved file: " + newFile.getAbsolutePath());
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to write to temp file");
                }

                msgText.setObject("file uploaded.");
                
                // create initialize node
                try {
                    Node rootNode = ((UserSession) Session.get()).getJcrSession().getRootNode();
                    Node initNode = rootNode.getNode(HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.INITIALIZE_PATH);
                    
                    if (initNode.hasNode("import-content")) {
                        initNode.getNode("import-content").remove();
                    }
                    Node node = initNode.addNode("import-content", HippoNodeType.NT_INITIALIZEITEM);
                    node.setProperty(HippoNodeType.HIPPO_CONTENTRESOURCE, "file://" + newFile.getAbsolutePath());
                    node.setProperty(HippoNodeType.HIPPO_CONTENTROOT, nodeModel.getNode().getPath());
                    rootNode.getSession().save();
                    msgText.setObject("initialize node saved.");
                    
                } catch (RepositoryException e) {
                    log.error("Error while creating nodetypes initialization node: ", e);
                    throw new RuntimeException(e.getMessage());
                }
                
            }
        }

        private Folder getUploadFolder() {
            Folder uploadFolder = new Folder(System.getProperty("java.io.tmpdir"), "wicket-uploads");
            // Ensure folder exists
            if (!uploadFolder.mkdirs()) {
                if (!uploadFolder.exists()) {
                    throw new RuntimeException("Unable to create upload folder: " + uploadFolder.getPath());
                }
            }
            return uploadFolder;
        }
    }


    @Override
    protected void cancel() {
    }

    @Override
    protected void ok() throws Exception {
        MenuPlugin plugin = pluginRef.getService();
        plugin.setModel(nodeModel);
        plugin.flushNodeModel(nodeModel);
    }
}
