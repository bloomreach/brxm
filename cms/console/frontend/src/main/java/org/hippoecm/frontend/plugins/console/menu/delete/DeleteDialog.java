/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.menu.delete;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteDialog extends Dialog<Node> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DeleteDialog.class);

    private final IModelReference modelReference;
    private boolean immediateSave;

    public DeleteDialog(IModelReference<Node> modelReference) {
        setTitle(Model.of(getString("dialog.title")));
        setSize(DialogConstants.SMALL);

        this.modelReference = modelReference;
        IModel<Node> model = modelReference.getModel();
        setModel(model);

        String path;
        try {
            final Node node = model.getObject();
            if (node != null) {
                path = node.getPath();
            } else {
                path = "No node selected";
                setOkEnabled(false);
            }
        } catch (RepositoryException e) {
            path = e.getMessage();
        }
        add(new Label("message", new StringResourceModel("delete.message", this).setParameters(path)));

        add(new CheckBox("immediateSave", new PropertyModel<>(this, "immediateSave")));

        setFocusOnOk();
    }

    @Override
    public void onOk() {
        try {
            Node node = getModel().getObject();
            Node siblingOrParent = getSiblingOrParent(node);

            node.remove();

            if (immediateSave) {
                node.getSession().save();
            }

            modelReference.setModel(new JcrNodeModel(siblingOrParent));
        } catch (RepositoryException ex) {
            log.error("Error while deleting document", ex);
            error("Error while deleting document " + ex.getMessage());
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
}
