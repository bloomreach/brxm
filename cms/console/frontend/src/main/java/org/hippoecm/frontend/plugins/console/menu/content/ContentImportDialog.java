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
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentImportDialog  extends AbstractDialog<Node> {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ContentImportDialog.class);
    private final IModelReference modelReference;

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

    private final JcrNodeModel nodeModel;
    private FileUploadField fileUploadField;

    // hard coded defaults
    private String uuidBehavior = "Create new uuids on import";
    private String mergeBehavior = "Disable merging";
    private String derefBehavior = "Throw error when not found";
    private boolean saveBehavior = false;

    private final void InitMaps() {
        uuidOpts.put(Integer.valueOf(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING), "Remove existing node with same uuid");
        uuidOpts.put(Integer.valueOf(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING), "Replace existing node with same uuid");
        uuidOpts.put(Integer.valueOf(ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW), "Throw error on uuid collision");
        uuidOpts.put(Integer.valueOf(ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW), "Create new uuids on import");

        mergeOpts.put(Integer.valueOf(ImportMergeBehavior.IMPORT_MERGE_DISABLE), "Disable merging");
        mergeOpts.put(Integer.valueOf(ImportMergeBehavior.IMPORT_MERGE_ADD_OR_OVERWRITE), "Try to add, else overwrite same name nodes");
        mergeOpts.put(Integer.valueOf(ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP), "Try to add, else skip same name nodes");
        mergeOpts.put(Integer.valueOf(ImportMergeBehavior.IMPORT_MERGE_OVERWRITE), "Overwrite same name nodes");
        mergeOpts.put(Integer.valueOf(ImportMergeBehavior.IMPORT_MERGE_SKIP), "Skip same name nodes");
        mergeOpts.put(Integer.valueOf(ImportMergeBehavior.IMPORT_MERGE_THROW), "Throw error on naming conflict");

        derefOpts.put(Integer.valueOf(ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE), "Remove reference when not found");
        derefOpts.put(Integer.valueOf(ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_THROW), "Throw error when not found");
        derefOpts.put(Integer.valueOf(ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_TO_ROOT), "Add reference to root node when not found");

    }

    public ContentImportDialog(IModelReference modelReference) {
        this.modelReference = modelReference;
        InitMaps();
        this.nodeModel = (JcrNodeModel) modelReference.getModel();
        
        DropDownChoice uuid = new DropDownChoice("uuidBehaviors", new PropertyModel(this, "uuidBehavior"), new ArrayList<String>(uuidOpts.values()));
        DropDownChoice merge = new DropDownChoice("mergeBehaviors", new PropertyModel(this, "mergeBehavior"), new ArrayList<String>(mergeOpts.values()));
        DropDownChoice reference = new DropDownChoice("derefBehaviors", new PropertyModel(this, "derefBehavior"), new ArrayList<String>(derefOpts.values()));
        CheckBox save = new CheckBox("saveBehavior", new PropertyModel(this, "saveBehavior"));

        add(uuid.setNullValid(false).setRequired(true));
        add(merge.setNullValid(false).setRequired(true));
        add(reference.setNullValid(false).setRequired(true));
        add(save);

        // file upload
        setMultiPart(true);
        setNonAjaxSubmit();
        add(fileUploadField = new FileUploadField("fileInput"));

        setOkLabel("import");
        setFocus(uuid);

        try {
            String path = this.nodeModel.getNode().getPath();
            add(new Label("message", new StringResourceModel("dialog.message", this, null, new Object[] {path})));

            //info("Import content from a file to node: " + nodeModel.getNode().getPath());
        } catch (RepositoryException e) {
            log.error("Error getting node from model for contant import",e);
            throw new RuntimeException("Error getting node from model for contant import: " + e.getMessage());
        }
    }

    public IModel getTitle() {
        return new Model("Import content from file");
    }

    @Override
    protected void onOk() {
        final FileUpload upload = fileUploadField.getFileUpload();

        int uuidOpt = uuidOpts.getFirstKey(uuidBehavior).intValue();
        int mergeOpt = mergeOpts.getFirstKey(mergeBehavior).intValue();
        int derefOpt = derefOpts.getFirstKey(derefBehavior).intValue();

        if (upload != null) {
            info("File uploaded. Start import..");

            // do import
            try {
                InputStream contentStream = new BufferedInputStream(upload.getInputStream());
                String absPath = nodeModel.getNode().getPath();
                log.info("Starting import: importDereferencedXML(" + absPath + "," + upload.getClientFileName() + "," + uuidBehavior + "," + mergeBehavior + "," + derefBehavior);

                if (saveBehavior) {
                    nodeModel.getNode().getSession().save();
                }

                try {
                    ((HippoSession)((UserSession) Session.get()).getJcrSession()).importDereferencedXML(absPath, contentStream, uuidOpt, derefOpt, mergeOpt);

                    // TODO if we want the imported node to be selected in the browser tree, we need to get to the new imported (top) node
                    // modelReference.setModel(newNodeModel);
                    info("Import done.");
                
                    if (saveBehavior) {
                        nodeModel.getNode().getSession().save();
                    }
                } finally {
                    if (saveBehavior) {
                        nodeModel.getNode().getSession().refresh(false);
                    }
                }

            } catch (PathNotFoundException ex) {
                log.error("Error initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
                error("Import failed: " + ex.getMessage());
            } catch (ItemExistsException ex) {
                log.error("Error initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
                error("Import failed: " + ex.getMessage());
            } catch (ConstraintViolationException ex) {
                log.error("Error initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
                error("Import failed: " + ex.getMessage());
            } catch (VersionException ex) {
                log.error("Error initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
                error("Import failed: " + ex.getMessage());
            } catch (InvalidSerializedDataException ex) {
                log.error("Error initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
                error("Import failed: " + ex.getMessage());
            } catch (LockException ex) {
                log.error("Error initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
                error("Import failed: " + ex.getMessage());
            } catch (RepositoryException ex) {
                log.error("Error initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
                error("Import failed: " + ex.getMessage());
            } catch (IOException ex) {
                log.error("IOException initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
                error("Import failed: " + ex.getMessage());
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

    public void setSaveBehavior(boolean saveBehavior) {
        this.saveBehavior = saveBehavior;
    }
    public boolean getSaveBehavior() {
        return saveBehavior;
    }

    @Override
    public IValueMap getProperties() {
        return MEDIUM;
    }

}
