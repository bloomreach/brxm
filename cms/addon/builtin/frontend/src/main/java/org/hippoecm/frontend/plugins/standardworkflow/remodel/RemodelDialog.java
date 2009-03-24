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
package org.hippoecm.frontend.plugins.standardworkflow.remodel;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugins.standardworkflow.export.CndSerializer;
import org.hippoecm.frontend.plugins.standardworkflow.export.NamespaceUpdater;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.JcrTypeStore;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.TemplateEditorWorkflow;
import org.hippoecm.repository.standardworkflow.TemplateEditorWorkflow.TypeUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemodelDialog extends CompatibilityWorkflowPlugin.WorkflowAction.WorkflowDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RemodelDialog.class);

    private RemodelWizard wizard;

    WorkflowDescriptorModel model;

    public RemodelDialog(CompatibilityWorkflowPlugin.WorkflowAction action, WorkflowDescriptorModel model) {
        action . super();
        this.model = model;

        try {
            if (model == null || model.getNode() == null) {
                add(new Label("wizard"));
            } else {
                wizard = new RemodelWizard("wizard");
                add(wizard);
            }
        } catch(RepositoryException ex) {
            add(new Label("wizard"));
        }

        ok.setVisible(false);
        cancel.setVisible(false);
    }

    @Override
    public void setDialogService(IDialogService dialogService) {
        super.setDialogService(dialogService);
        wizard.setDialogService(dialogService);
    }
    
    public IModel getTitle() {
        return new StringResourceModel("update-content", this, null);
    }

    void remodel() throws Exception {
        onOk();
    }

    protected String execute2() {
        try {
        JcrSessionModel sessionModel = ((UserSession) Session.get()).getJcrSessionModel();

        Node node = model.getNode();
        String namespace = node.getName();

        CndSerializer serializer = new CndSerializer(sessionModel, namespace);
        serializer.versionNamespace(namespace);
        String cnd = serializer.getOutput();

        NamespaceUpdater updater = new NamespaceUpdater(new JcrTypeStore(), new JcrTypeStore(namespace));
        Map<String, TypeUpdate> update = updater.getUpdate(namespace);
        for (Map.Entry<String, TypeUpdate> entry : update.entrySet()) {
            String typeName = entry.getKey();
            Node typeNode = node.getNode(NodeNameCodec.encode(typeName.substring(typeName.indexOf(':') + 1)));
            if (typeNode.hasNode(HippoNodeType.HIPPO_PROTOTYPE)) {
                Node handle = typeNode.getNode(HippoNodeType.HIPPO_PROTOTYPE);
                NodeIterator children = handle.getNodes(HippoNodeType.HIPPO_PROTOTYPE);
                while (children.hasNext()) {
                    Node child = children.nextNode();
                    if (child.isNodeType("nt:unstructured")) {
                        entry.getValue().prototype = child.getUUID();
                    }
                }
            }
        }

        sessionModel.getSession().save();

        WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
        TemplateEditorWorkflow workflow = (TemplateEditorWorkflow) manager.getWorkflow((WorkflowDescriptor)model.getObject());
        if (workflow != null) {
            log.info("remodelling namespace " + namespace);
            try {
                /* String[] nodes = */
                workflow.updateModel(namespace, cnd, update);
                sessionModel.getSession().save();
            } catch (RepositoryException ex) {
                // log out; the session model will log in again.
                // Sessions cache path resolver information, which is incorrect after remapping the prefix.
                sessionModel.flush();

                throw ex;
            }
        } else {
            log.warn("no remodeling workflow available on selected node");
        }
        return null;
        } catch(Exception ex) {
            return ex.getClass().getName()+": "+ex.getMessage();
        }
    }
}
