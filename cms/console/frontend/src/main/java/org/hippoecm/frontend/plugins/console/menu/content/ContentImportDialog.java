/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.menu.t9ids.GenerateNewTranslationIdsVisitor;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.LabelledBooleanFieldWidget;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.onehippo.repository.api.ContentResourceLoader;
import org.onehippo.repository.util.ZipFileContentResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentImportDialog  extends AbstractDialog<Node> {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ContentImportDialog.class);

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
    private String xmlInput;
    private Boolean saveBehavior = false;
    private Boolean generate = false;

    // hard coded defaults
    private String uuidBehavior = "Create new uuids on import";
    private String mergeBehavior = "Disable merging";
    private String derefBehavior = "Throw error when not found";

    private void InitMaps() {
        uuidOpts.put(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING, "Remove existing node with same uuid");
        uuidOpts.put(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING, "Replace existing node with same uuid");
        uuidOpts.put(ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW, "Throw error on uuid collision");
        uuidOpts.put(ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, "Create new uuids on import");

        mergeOpts.put(ImportMergeBehavior.IMPORT_MERGE_DISABLE, "Disable merging");
        mergeOpts.put(ImportMergeBehavior.IMPORT_MERGE_ADD_OR_OVERWRITE, "Try to add, else overwrite same name nodes");
        mergeOpts.put(ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP, "Try to add, else skip same name nodes");
        mergeOpts.put(ImportMergeBehavior.IMPORT_MERGE_OVERWRITE, "Overwrite same name nodes");
        mergeOpts.put(ImportMergeBehavior.IMPORT_MERGE_SKIP, "Skip same name nodes");
        mergeOpts.put(ImportMergeBehavior.IMPORT_MERGE_THROW, "Throw error on naming conflict");

        derefOpts.put(ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE, "Remove reference when not found");
        derefOpts.put(ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_THROW, "Throw error when not found");
        derefOpts.put(ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_TO_ROOT, "Add reference to root node when not found");

    }

    public ContentImportDialog(IModelReference<Node> modelReference) {
        InitMaps();
        this.nodeModel = (JcrNodeModel) modelReference.getModel();

        DropDownChoice<String> uuid = new DropDownChoice<>("uuidBehaviors", new PropertyModel<String>(this, "uuidBehavior"), new ArrayList<>(uuidOpts.values()));
        DropDownChoice<String> merge = new DropDownChoice<>("mergeBehaviors", new PropertyModel<String>(this, "mergeBehavior"), new ArrayList<>(mergeOpts.values()));
        DropDownChoice<String> reference = new DropDownChoice<>("derefBehaviors", new PropertyModel<String>(this, "derefBehavior"), new ArrayList<>(derefOpts.values()));
        LabelledBooleanFieldWidget save = new LabelledBooleanFieldWidget("saveBehavior",
                new PropertyModel<Boolean>(this, "saveBehavior"),
                Model.of("Immediate save after import"));

        add(uuid.setNullValid(false).setRequired(true));
        add(merge.setNullValid(false).setRequired(true));
        add(reference.setNullValid(false).setRequired(true));
        add(save);

        LabelledBooleanFieldWidget generate = new LabelledBooleanFieldWidget("generate",
                new PropertyModel<Boolean>(this, "generate"),
                Model.of("Generate new translation ids"));
        add(generate);

        // file upload
        setMultiPart(true);
        setNonAjaxSubmit();
        add(fileUploadField = new FileUploadField("fileInput"));

        //xml import
        add(new TextArea<String>("xmlInput", new PropertyModel<String>(this, "xmlInput")));

        setOkLabel("Import");
        setFocus(uuid);

        try {
            String path = this.nodeModel.getNode().getPath();
            add(new Label("message", new StringResourceModel("dialog.message", this, null, null, path)));

            //info("Import content from a file to node: " + nodeModel.getNode().getPath());
        } catch (RepositoryException e) {
            log.error("Error getting node from model for contant import",e);
            throw new RuntimeException("Error getting node from model for contant import: " + e.getMessage());
        }
    }

    public IModel<String> getTitle() {
        return new Model<>("XML Import");
    }

    @Override
    protected void onOk() {
        final FileUpload upload = fileUploadField.getFileUpload();

        int uuidOpt = uuidOpts.getFirstKey(uuidBehavior);
        int mergeOpt = mergeOpts.getFirstKey(mergeBehavior);
        int derefOpt = derefOpts.getFirstKey(derefBehavior);

        try {

            if (upload == null && StringUtils.isEmpty(xmlInput)) {
                warn("No file was uploaded and no xml input provided. Nothing to import");
                return;
            }

            String absPath = nodeModel.getNode().getPath();

            // If save-after-import is enabled and the import fails, we do a Session.refresh(false) to revert any
            // changes done by the import. However, any changes done *before* the import will then also be lost.
            // We therefore have to save before importing, so all changes before the import are persisted regardless
            // of what happens during the import.
            if (saveBehavior) {
                nodeModel.getNode().getSession().save();
            }

            File tempFile = null;
            ZipFile zipFile = null;
            InputStream in = null;
            InputStream esvIn = null;
            OutputStream out = null;
            try {
                final HippoSession session = (HippoSession) UserSession.get().getJcrSession();
                List<String> nodesBefore = new ArrayList<>();

                if(generate) {
                    for(NodeIterator nodeIterator = nodeModel.getNode().getNodes(); nodeIterator.hasNext();) {
                        final Node node = nodeIterator.nextNode();
                        nodesBefore.add(node.getPath());
                    }
                }

                if (upload != null) {
                    final String fileName = upload.getClientFileName();
                    if (fileName.endsWith(".zip")) {
                        tempFile = File.createTempFile("package", "zip");
                        out = new FileOutputStream(tempFile);
                        in = upload.getInputStream();
                        IOUtils.copy(in, out);
                        out.close();
                        out = null;
                        zipFile = new ZipFile(tempFile);
                        ContentResourceLoader contentResourceLoader = new ZipFileContentResourceLoader(zipFile);
                        esvIn = contentResourceLoader.getResourceAsStream("esv.xml");
                        session.importDereferencedXML(absPath, esvIn, contentResourceLoader, uuidOpt, derefOpt, mergeOpt);
                    }
                    else if (fileName.endsWith(".xml")) {
                        in = new BufferedInputStream(upload.getInputStream());
                        session.importDereferencedXML(absPath, in, uuidOpt, derefOpt, mergeOpt);
                    }
                    else {
                        warn("Unrecognized file: only .xml and .zip can be processed");
                        return;
                    }
                }
                else {
                    in = new ByteArrayInputStream(xmlInput.getBytes("UTF-8"));
                    session.importDereferencedXML(absPath, in, uuidOpt, derefOpt, mergeOpt);
                }

                if(generate) {
                    final Node newNode = findNewNode(nodesBefore, nodeModel.getNode());
                    if(newNode != null) {
                        log.debug("Applying new translation ids on node: " + newNode.getPath());
                        newNode.accept(new GenerateNewTranslationIdsVisitor());
                    }
                }

                if (saveBehavior) {
                    nodeModel.getNode().getSession().save();
                }
            } finally {
                if (saveBehavior) {
                    nodeModel.getNode().getSession().refresh(false);
                }
                IOUtils.closeQuietly(out);
                IOUtils.closeQuietly(esvIn);
                IOUtils.closeQuietly(in);
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (Exception ignore) {
                    }
                }
                FileUtils.deleteQuietly(tempFile);
            }

        } catch (RepositoryException ex) {
            log.error("Error initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
            error("Import failed: " + ex.getMessage());
        } catch (IOException ex) {
            log.error("IOException initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
            error("Import failed: " + ex.getMessage());
        }
    }

    /**
     * Check all childnodes after the import and find the new node
     * @param nodesBefore list of nodepaths of the childnodes before the import
     * @param node the node on which the import has been done
     * @return the new child node or null (e.g. in case a merge was done and no new node was created)
     * @throws RepositoryException if iterating child nodes goes wrong
     */
    private Node findNewNode(final List<String> nodesBefore, final Node node) throws RepositoryException {
        // iterate all childnodes after the import
        for(final NodeIterator nodesAfter = node.getNodes(); nodesAfter.hasNext();) {
            final Node afterNode = nodesAfter.nextNode();
            // if its path is new, it is the new node
            if(!nodesBefore.contains(afterNode.getPath())) {
                return afterNode;
            }
        }
        return null;
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

    public String getXmlInput() {
        return xmlInput;
    }

    public void setXmlInput(String xmlInput) {
        this.xmlInput = xmlInput;
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=855,height=460").makeImmutable();
    }

}
