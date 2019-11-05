/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.menu.t9ids.GenerateNewTranslationIdsVisitor;
import org.hippoecm.frontend.widgets.LabelledBooleanFieldWidget;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.onehippo.cm.ConfigurationService;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YamlImportDialog extends Dialog<Node> {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(YamlImportDialog.class);

    public class LookupHashMap<K, V> extends HashMap<K, V> {
        private static final long serialVersionUID = 9065806784464553409L;

        public K getFirstKey(Object value) {
            if (value == null) {
                return null;
            }
            for (Map.Entry<K, V> e : entrySet()) {
                if (value.equals(e.getValue())) {
                    return e.getKey();
                }
            }
            return null;
        }
    }

    private final LookupHashMap<Integer, String> uuidOpts = new LookupHashMap<>();
    private final LookupHashMap<Integer, String> derefOpts = new LookupHashMap<>();

    private final JcrNodeModel nodeModel;
    private FileUploadField fileUploadField;
    private String xmlInput;
    private Boolean saveBehavior = false;
    private Boolean generate = false;

    // hard coded defaults
    private String uuidBehavior = "Create new uuids on import";
    private String derefBehavior = "Throw error when not found";

    private void InitMaps() {
        uuidOpts.put(ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, "Create new uuids on import");
        derefOpts.put(ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_THROW, "Throw error when not found");
    }

    public YamlImportDialog(IModelReference<Node> modelReference) {
        setTitle(Model.of("YAML import"));
        setSize(DialogConstants.LARGE);

        InitMaps();
        this.nodeModel = (JcrNodeModel) modelReference.getModel();

        DropDownChoice<String> uuid = new DropDownChoice<>("uuidBehaviors", new PropertyModel<>(this, "uuidBehavior"), new ArrayList<>(uuidOpts.values()));
        DropDownChoice<String> reference = new DropDownChoice<>("derefBehaviors", new PropertyModel<>(this, "derefBehavior"), new ArrayList<>(derefOpts.values()));
        LabelledBooleanFieldWidget save = new LabelledBooleanFieldWidget("saveBehavior",
                new PropertyModel<>(this, "saveBehavior"),
                Model.of("Immediate save after import"));

        add(uuid.setNullValid(false).setRequired(true));
        add(reference.setNullValid(false).setRequired(true));
        add(save);

        LabelledBooleanFieldWidget generate = new LabelledBooleanFieldWidget("generate",
                new PropertyModel<>(this, "generate"),
                Model.of("Generate new translation ids (only when adding a node)"));
        generate.setEnabled(false);
        add(generate);

        // file upload
        setMultiPart(true);
        setNonAjaxSubmit();
        add(fileUploadField = new FileUploadField("fileInput"));

        //xml import
        add(new TextArea<String>("xmlInput", new PropertyModel<>(this, "xmlInput")));

        setOkLabel("Import");
        setFocus(uuid);

        try {
            String path = this.nodeModel.getNode().getPath();
            add(new Label("message", new StringResourceModel("dialog.message", this).setParameters(path)));
        } catch (RepositoryException e) {
            log.error("Error getting node from model for contant import", e);
            throw new RuntimeException("Error getting node from model for contant import: " + e.getMessage());
        }
    }

    @Override
    protected void onOk() {
        final FileUpload upload = fileUploadField.getFileUpload();

        try {

            if (upload == null && StringUtils.isEmpty(xmlInput)) {
                warn("No file was uploaded and no yaml input provided. Nothing to import");
                return;
            }

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
            OutputStream out = null;
            try {
                List<String> nodesBefore = new ArrayList<>();

                if (generate) {
                    for (NodeIterator nodeIterator = nodeModel.getNode().getNodes(); nodeIterator.hasNext(); ) {
                        final Node node = nodeIterator.nextNode();
                        nodesBefore.add(node.getPath());
                    }
                }

                final ConfigurationService configurationService = HippoServiceRegistry.getService(ConfigurationService.class);

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

                        configurationService.importZippedContent(tempFile, nodeModel.getNode());

                    } else if (fileName.endsWith(".yaml")) {
                        in = new BufferedInputStream(upload.getInputStream());
                        configurationService.importPlainYaml(in, nodeModel.getNode());
                    } else {
                        warn("Unrecognized file: only .yaml and .zip can be processed");
                        return;
                    }
                } else {
                    in = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));
                    configurationService.importPlainYaml(in, nodeModel.getNode());
                }

                if (generate) {
                    final Node newNode = findNewNode(nodesBefore, nodeModel.getNode());
                    if (newNode != null) {
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
            error("Import failed: " + ex.getCause().getMessage());
        } catch (Exception ex) {
            log.error("Error initializing content in '" + nodeModel.getItemModel().getPath() + "' : " + ex.getMessage(), ex);
            error("Import failed: " + ex.getMessage());
        }
    }

    /**
     * Check all childnodes after the import and find the new node
     *
     * @param nodesBefore list of nodepaths of the childnodes before the import
     * @param node        the node on which the import has been done
     * @return the new child node or null (e.g. in case a merge was done and no new node was created)
     * @throws RepositoryException if iterating child nodes goes wrong
     */
    private Node findNewNode(final List<String> nodesBefore, final Node node) throws RepositoryException {
        // iterate all childnodes after the import
        for (final NodeIterator nodesAfter = node.getNodes(); nodesAfter.hasNext(); ) {
            final Node afterNode = nodesAfter.nextNode();
            // if its path is new, it is the new node
            if (!nodesBefore.contains(afterNode.getPath())) {
                return afterNode;
            }
        }
        return null;
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
}
