/**
 * Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.admin.updater;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteUpdaterDialog extends Dialog<Node> {

    private static final Logger log = LoggerFactory.getLogger(DeleteUpdaterDialog.class);

    private final Panel container;

    public DeleteUpdaterDialog(final IModel<Node> defaultModel, Panel container) {
        super(defaultModel);
        setTitle(Model.of("Delete Updater"));
        setSize(DialogConstants.SMALL);
        
        this.container = container;
        add(new Label("message", "Are you sure you want to delete updater '" + getUpdaterName() + "'?"));
    }

    @Override
    protected void onOk() {
        final Node node = (Node) getDefaultModelObject();
        final Session session = UserSession.get().getJcrSession();
        if (node != null) {
            try {
                final Node siblingOrParent = getSiblingOrParent(node);
                node.remove();
                session.save();
                container.setDefaultModel(new JcrNodeModel(siblingOrParent));
            } catch (RepositoryException e) {
                log.error("Failed to remove updater", e);
            }
        }
    }

    private Node getSiblingOrParent(Node node) throws RepositoryException {
        final Node parent = node.getParent();
        final NodeIterator nodes = parent.getNodes();
        Node sibling = null;
        while (nodes.hasNext()) {
            final Node nextNode = nodes.nextNode();
            if (node.isSame(nextNode)) {
                if (nodes.hasNext()) {
                    return nodes.nextNode();
                } else if (sibling != null) {
                    return sibling;
                }
            }
            sibling = nextNode;
        }
        return parent;
    }

    private String getUpdaterName() {
        Node node = (Node) getDefaultModelObject();
        if (node != null) {
            try {
                return node.getName();
            } catch (RepositoryException e) {
                log.error("Failed to get name of updater", e);
            }
        }
        return "";
    }
}
