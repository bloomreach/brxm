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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
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

        private final List<String> mergeOpts = new ArrayList<String>();
        private final List<String> referenceOpts = new ArrayList<String>();
        
        private String mergeBehavior = ImportMergeBehavior.STRINGS[ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP];
        private String referenceBehavior = ImportReferenceBehavior.STRINGS[ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE];
        
        public FileUploadForm(String name) {
            
            super(name);
            setMergeBehaviors();
            setReferenceBehaviors();

            DropDownChoice merge = new DropDownChoice("mergeBehaviors", new PropertyModel(this, "mergeBehavior"), mergeOpts);
            DropDownChoice reference = new DropDownChoice("referenceBehaviors", new PropertyModel(this, "referenceBehavior"),
                    referenceOpts);
            
            add(merge.setNullValid(false).setRequired(true));
            add(reference.setNullValid(false).setRequired(true));
            
            // file upload
            setMultiPart(true);
            add(fileUploadField = new FileUploadField("fileInput"));
            
        }

        @Override
        protected void onSubmit() {
            final FileUpload upload = fileUploadField.getFileUpload();

            int mergeOpt = mergeOpts.indexOf(mergeBehavior);
            int referenceOpt = referenceOpts.indexOf(referenceBehavior);
            
            if (upload != null) {
                msgText.setObject("File uploaded. Start import..");

                // do import
                try {
                    InputStream contentStream = new BufferedInputStream(upload.getInputStream());
                    String absPath = nodeModel.getNode().getPath();
                    log.info("Starting import: importDereferencedXML(" + absPath + "," + upload.getClientFileName() + "," + ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW + "," + mergeBehavior + "," +referenceBehavior);
                    
                    ((HippoSession)((UserSession) Session.get()).getJcrSession()).importDereferencedXML(absPath, contentStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, mergeOpt , referenceOpt);
                    //((HippoSession)((UserSession) Session.get()).getJcrSession()).importXML(absPath, contentStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
                    msgText.setObject("Import done.");
                } catch (PathNotFoundException ex) {
                    log.error("Error initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
                    msgText.setObject("Import failed: " + ex.getMessage());
                } catch (ItemExistsException ex) {
                    log.error("Error initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
                    msgText.setObject("Import failed: " + ex.getMessage());
                } catch (ConstraintViolationException ex) {
                    log.error("Error initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
                    msgText.setObject("Import failed: " + ex.getMessage());
                } catch (VersionException ex) {
                    log.error("Error initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
                    msgText.setObject("Import failed: " + ex.getMessage());
                } catch (InvalidSerializedDataException ex) {
                    log.error("Error initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
                    msgText.setObject("Import failed: " + ex.getMessage());
                } catch (LockException ex) {
                    log.error("Error initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
                    msgText.setObject("Import failed: " + ex.getMessage());
                } catch (RepositoryException ex) {
                    log.error("Error initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
                    msgText.setObject("Import failed: " + ex.getMessage());
                } catch (IOException ex) {
                    log.error("IOException initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
                    msgText.setObject("Import failed: " + ex.getMessage());
                }
            }
        }

        private void setMergeBehaviors() {
            for (String s : ImportMergeBehavior.STRINGS) {
                mergeOpts.add(s);
            }
        }

        private void setReferenceBehaviors() {
            for (String s : ImportReferenceBehavior.STRINGS) {
                referenceOpts.add(s);
            }
        }

        public String getMergeBehavior() {
            return mergeBehavior;
        }

        public void setMergeBehavior(String mergeBehavior) {
            this.mergeBehavior = mergeBehavior;
        }

        public String getReferenceBehavrior() {
            return referenceBehavior;
        }

        public void setgetReferenceBehavriorBehavior(String referenceBehavior) {
            this.referenceBehavior = referenceBehavior;
        }
    }

    @Override
    protected void ok() throws Exception {
        MenuPlugin plugin = pluginRef.getService();
        plugin.setModel(nodeModel);
        plugin.flushNodeModel(nodeModel);
    }
}
