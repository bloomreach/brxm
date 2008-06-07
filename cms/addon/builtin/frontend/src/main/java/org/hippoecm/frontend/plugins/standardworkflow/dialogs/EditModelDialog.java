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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.hippoecm.frontend.legacy.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.legacy.dialog.DialogWindow;
import org.hippoecm.frontend.legacy.plugin.channel.Channel;
import org.hippoecm.frontend.legacy.plugin.channel.Request;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.standardworkflow.EditmodelWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditModelDialog extends AbstractWorkflowDialog {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(EditModelDialog.class);

    public EditModelDialog(DialogWindow dialogWindow) {
        super(dialogWindow, "Edit model");
        if (dialogWindow.getNodeModel().getNode() == null) {
            ok.setVisible(false);
        }
    }

    @Override
    protected void execute() throws Exception {
        EditmodelWorkflow workflow = (EditmodelWorkflow) getWorkflow();
        if (workflow != null) {
            String path = workflow.edit();
            try {
                Node node = ((UserSession) Session.get()).getJcrSession().getRootNode().getNode(path.substring(1));
                JcrItemModel itemModel = new JcrItemModel(node);
                if (path != null) {
                    Channel channel = getChannel();
                    if (channel != null) {
                        Request request = channel.createRequest("edit", new JcrNodeModel(itemModel));
                        channel.send(request);
                    } else {
                        log.error("could not send edit message");
                    }
                } else {
                    log.error("no model found to edit");
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.error("no workflow defined on model for selected node");
        }
    }

    @Override
    public void cancel() {
    }

    private Node getVersion(Node node, String name, String version) throws RepositoryException {
        Node template = node.getNode(name);
        NodeIterator iter = template.getNodes(name);
        while (iter.hasNext()) {
            Node versionNode = iter.nextNode();
            if (versionNode.isNodeType(HippoNodeType.NT_REMODEL)) {
                if (version.equals(versionNode.getProperty(HippoNodeType.HIPPO_REMODEL).getString())) {
                    return versionNode;
                }
            } else if (version.equals("current")) {
                return versionNode;
            }
        }
        return null;
    }
}
