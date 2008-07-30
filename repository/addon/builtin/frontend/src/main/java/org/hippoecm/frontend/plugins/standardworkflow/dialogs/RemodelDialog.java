/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.standardworkflow.dialogs;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugins.standardworkflow.RemodelWorkflowPlugin;
import org.hippoecm.frontend.plugins.standardworkflow.export.CndSerializer;
import org.hippoecm.frontend.plugins.standardworkflow.export.NamespaceUpdater;
import org.hippoecm.frontend.plugins.standardworkflow.types.JcrTypeStore;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow.TypeUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemodelDialog extends AbstractWorkflowDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RemodelDialog.class);

    private IServiceReference<IJcrService> jcrServiceRef;

    public RemodelDialog(RemodelWorkflowPlugin plugin, IDialogService dialogWindow,
            IServiceReference<IJcrService> jcrService) {
        super(plugin, dialogWindow, "Update content");

        this.jcrServiceRef = jcrService;
        add(new Label("message", "Updating all content to match the updated types might take a long time and is irreversible. Are you sure you want to proceed?"));

        if (plugin.getModel() == null || ((WorkflowsModel) plugin.getModel()).getNodeModel().getNode() == null) {
            ok.setVisible(false);
        }
    }

    @Override
    protected void execute() throws Exception {
        JcrSessionModel sessionModel = ((UserSession) Session.get()).getJcrSessionModel();

        WorkflowsModel wflModel = (WorkflowsModel) getPlugin().getModel();
        Node node = wflModel.getNodeModel().getNode();
        String namespace = node.getName();

        CndSerializer serializer = new CndSerializer(sessionModel, namespace);
        serializer.versionNamespace(namespace);
        String cnd = serializer.getOutput();

        NamespaceUpdater updater = new NamespaceUpdater(new JcrTypeStore(), new JcrTypeStore(namespace));
        Map<String, TypeUpdate> update = updater.getUpdate(namespace);
        for (Map.Entry<String, TypeUpdate> entry : update.entrySet()) {
            String typeName = entry.getKey();
            Node typeNode = node.getNode(typeName.substring(typeName.indexOf(':') + 1));
            if (typeNode.hasNode(HippoNodeType.HIPPO_PROTOTYPE)) {
                Node handle = typeNode.getNode(HippoNodeType.HIPPO_PROTOTYPE);
                NodeIterator children = handle.getNodes(HippoNodeType.HIPPO_PROTOTYPE);
                while (children.hasNext()) {
                    Node child = children.nextNode();
                    if (child.isNodeType(JcrConstants.NT_UNSTRUCTURED)) {
                        entry.getValue().prototype = child.getUUID();
                    }
                }
            }
        }

        sessionModel.getSession().save();

        RemodelWorkflow workflow = (RemodelWorkflow) getWorkflow();
        if (workflow != null) {
            log.info("remodelling namespace " + namespace);
            try {
                /* String[] nodes = */
                workflow.remodel(cnd, update);
                sessionModel.getSession().save();

                // log out; the session model will log in again.
                // Sessions cache path resolver information, which is incorrect after remapping the prefix.
                sessionModel.flush();

                jcrServiceRef.getService().flush(new JcrNodeModel("/"));
            } catch (RepositoryException ex) {
                // log out; the session model will log in again.
                // Sessions cache path resolver information, which is incorrect after remapping the prefix.
                sessionModel.flush();

                throw ex;
            }
        } else {
            log.warn("no remodeling workflow available on selected node");
        }
    }
}
