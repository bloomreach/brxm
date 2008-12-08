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
import java.util.HashMap;
import java.util.Map;

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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
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

    private final JcrNodeModel nodeModel;
    MenuPlugin plugin;

    Component message;
    Model msgText;

    public class LookupHashMap<K,V> extends HashMap<K,V> {
        private static final long serialVersionUID = 9065806784464553409L;

        public K getFirstKey(Object value) {
            if (value == null) {
                return null;
            }
            for (Map.Entry<K, V> e: entrySet()) {
                if (value.equals(e.getValue())) {
                    return e.getKey();
                }
            }
            return null;
        }
    }
    
    private final LookupHashMap<Integer, String> uuidOpts = new LookupHashMap<Integer, String>();
    private final LookupHashMap<Integer, String> mergeOpts = new LookupHashMap<Integer, String>();
    private final LookupHashMap<Integer, String> derefOpts = new LookupHashMap<Integer, String>();
    
    private final void InitMaps() {
        uuidOpts.put(new Integer(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING), "Remove existing node with same uuid");
        uuidOpts.put(new Integer(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING), "Replace existing node with same uuid");
        uuidOpts.put(new Integer(ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW), "Throw error on uuid collision");
        uuidOpts.put(new Integer(ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW), "Create new uuids on import");

        mergeOpts.put(new Integer(ImportMergeBehavior.IMPORT_MERGE_ADD_OR_OVERWRITE), "Try to add, else overwrite same name nodes");
        mergeOpts.put(new Integer(ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP), "Try to add, else skip same name nodes");
        mergeOpts.put(new Integer(ImportMergeBehavior.IMPORT_MERGE_OVERWRITE), "Overwrite same name nodes");
        mergeOpts.put(new Integer(ImportMergeBehavior.IMPORT_MERGE_SKIP), "Skip same name nodes");
        mergeOpts.put(new Integer(ImportMergeBehavior.IMPORT_MERGE_THROW), "Throw error on naming conflict");

        derefOpts.put(new Integer(ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE), "Remove reference when not found");
        derefOpts.put(new Integer(ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_THROW), "Throw error when not found");
        derefOpts.put(new Integer(ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_TO_ROOT), "Add reference to root node when not found");
        
    }
    

    public ContentImportDialog(MenuPlugin plugin, IDialogService dialogWindow) {
        super(dialogWindow);
        InitMaps();
        this.plugin = plugin;
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

    public IModel getTitle() {
        return new Model("Import content from file");
    }

    @Override
    protected void ok() throws Exception {
        plugin.setModel(nodeModel);
        plugin.flushNodeModel(nodeModel);
    }

    /**
     * Form the upload.
     */
    private class FileUploadForm extends Form {
        private static final long serialVersionUID = 1L;

        private FileUploadField fileUploadField;

        
        private String uuidBehavior = uuidOpts.get(ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        private String mergeBehavior = mergeOpts.get(ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP);
        private String derefBehavior = derefOpts.get(ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE);
        
        public FileUploadForm(String name) {
            
            super(name);

            DropDownChoice uuid = new DropDownChoice("uuidBehaviors", new PropertyModel(this, "uuidBehavior"), new ArrayList<String>(uuidOpts.values()));
            DropDownChoice merge = new DropDownChoice("mergeBehaviors", new PropertyModel(this, "mergeBehavior"), new ArrayList<String>(mergeOpts.values()));
            DropDownChoice reference = new DropDownChoice("derefBehaviors", new PropertyModel(this, "derefBehavior"), new ArrayList<String>(derefOpts.values()));

            add(uuid.setNullValid(false).setRequired(true));
            add(merge.setNullValid(false).setRequired(true));
            add(reference.setNullValid(false).setRequired(true));
            
            // file upload
            setMultiPart(true);
            add(fileUploadField = new FileUploadField("fileInput"));
            
        }

        @Override
        protected void onSubmit() {
            final FileUpload upload = fileUploadField.getFileUpload();

            int uuidOpt = uuidOpts.getFirstKey(uuidBehavior).intValue();
            int mergeOpt = mergeOpts.getFirstKey(mergeBehavior).intValue();
            int derefOpt = derefOpts.getFirstKey(derefBehavior).intValue();
            
            if (upload != null) {
                msgText.setObject("File uploaded. Start import..");

                // do import
                try {
                    InputStream contentStream = new BufferedInputStream(upload.getInputStream());
                    String absPath = nodeModel.getNode().getPath();
                    log.info("Starting import: importDereferencedXML(" + absPath + "," + upload.getClientFileName() + "," + uuidBehavior + "," + mergeBehavior + "," +derefBehavior);
                    
                    ((HippoSession)((UserSession) Session.get()).getJcrSession()).importDereferencedXML(absPath, contentStream, uuidOpt, derefOpt, mergeOpt);
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

        public void setMergeBehavior(String mergeBehavior) {
            this.mergeBehavior = mergeBehavior;
        }
        public String getMergeBehavior() {
            return mergeBehavior;
        }


        public void setDerefBehavior(String derefBehavior) {
            this.derefBehavior = derefBehavior;
        }
        public String getDerefBehavior() {
            return derefBehavior;
        }
        

        public void setUuidBehavior(String uuidBehavior) {
            this.uuidBehavior = uuidBehavior;
        }
        public String getUuidBehavior() {
            return uuidBehavior;
        }
    }

}
