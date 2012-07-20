/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.frontend.plugins.console.menu.nodereset;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.ReferenceWorkspace;

public class NodeResetDialog extends AbstractDialog<Node> {

    private static final long serialVersionUID = 1L;

    private static final String[] ILLEGAL_PATHS = new String[] {
            "/hippo:log",
            "/formdata",
            "/jcr:system"
    };

    private String path;
    private ReferenceWorkspace referenceWorkspace;

    public NodeResetDialog(IModel<Node> model) {
        super(model);

        add(new Label("message", "Resetting a node means that you undo all changes that were made to it since the system was bootstrapped." +
                " Changes will not be automatically saved so you can inspect the result of resetting first."));

        try {
            referenceWorkspace = UserSession.get().getHippoRepository().getOrCreateReferenceWorkspace();
            if (referenceWorkspace == null) {
                error("This functionality is not available in your environment");
                setOkEnabled(false);
                return;
            }
            final Node node = getModelObject();
            path = node.getPath();
            if (!isValidPath(path)) {
                error("Reset in this context is not supported");
                setOkEnabled(false);
                return;
            }
            if (isSameNameSibling(node)) {
                error("Resetting same name siblings is not supported");
                setOkEnabled(false);
                return;
            }
        } catch (RepositoryException e) {
            error("An unexpected error occurred: " + e.getMessage());
            setOkEnabled(false);
        }

    }

    private boolean isValidPath(final String path) {
        if (path.equals("/")) {
            return false;
        }
        if (path.equals("/hippo:configuration")) {
            return false;
        }
        if (path.equals("/hippo:namespaces")) {
            return false;
        }
        for (String illegalPath : ILLEGAL_PATHS) {
            if (path.startsWith(illegalPath)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onOk() {
        reload();
    }

    private void reload() {
        Session session = null;
        try {

            session = referenceWorkspace.login();

            if (!session.nodeExists("/hippo:configuration")) {
                referenceWorkspace.bootstrap();
            }

            // check if reference node exists
            if (!session.nodeExists(path)) {
                error("No node at " + path + " in reference repository");
                return;
            }

            Node reference = session.getNode(path);

            Node parent = getModelObject().getParent();
            getModelObject().remove();
            copy(reference, parent);

            if (parent.getPrimaryNodeType().hasOrderableChildNodes()) {
                order(reference, parent);
            }

        } catch (RepositoryException e) {
            error("An unexpected error occurred: " + e.getMessage());
            try { session.refresh(false); } catch (RepositoryException ignore) {}
        } catch (IOException e) {
            error("An unexpected error occurred: " + e.getMessage());
            try { session.refresh(false); } catch (RepositoryException ignore) {}
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    private void copy(final Node reference, Node parent) throws RepositoryException {
        if (JcrHelper.isVirtualNode(reference)) {
            return;
        }
        final Node node = parent.addNode(reference.getName(), reference.getPrimaryNodeType().getName());
        for (NodeType nodeType : reference.getMixinNodeTypes()) {
            node.addMixin(nodeType.getName());
        }
        final PropertyIterator properties = reference.getProperties();
        while (properties.hasNext()) {
            final Property property = properties.nextProperty();
            if (!property.getDefinition().isProtected()) {
                if (property.isMultiple()) {
                    node.setProperty(property.getName(), property.getValues());
                } else {
                    node.setProperty(property.getName(), property.getValue());
                }
            }
        }
        final NodeIterator children = reference.getNodes();
        while (children.hasNext()) {
            final Node child = children.nextNode();
            copy(child, node);
        }
    }

    private void order(final Node reference, final Node parent) throws RepositoryException {
        final String srcChildRelPath = reference.getName();
        String destChildRelPath = null;
        final NodeIterator siblings = reference.getParent().getNodes();
        while (siblings.hasNext()) {
            if (siblings.nextNode().isSame(reference) && siblings.hasNext()) {
                destChildRelPath = siblings.nextNode().getName();
            }
        }
        if (destChildRelPath != null && parent.hasNode(destChildRelPath)) {
            parent.orderBefore(srcChildRelPath, destChildRelPath);
        }
    }

    private boolean isSameNameSibling(Node node) throws RepositoryException {
        return node.getParent().getNodes(node.getName()).getSize() > 1;
    }

    @Override
    public IModel getTitle() {
        return new Model<String>("Reset " + path);
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.SMALL;
    }

}
