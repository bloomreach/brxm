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
package org.hippoecm.frontend.plugins.console.menu.move;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeNodeComparator;
import org.hippoecm.frontend.plugins.console.dialog.LookupDialog;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.AutoFocusSelectTextFieldWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveDialog extends LookupDialog {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MoveDialog.class);

    private static final IValueMap SIZE = new ValueMap("width=515,height=540");

    private String name;
    @SuppressWarnings("unused")
    private String target;
    private Label targetLabel;
    private final IModelReference<Node> modelReference;

    public MoveDialog(IModelReference<Node> modelReference) {
        super(new JcrTreeNode(new JcrNodeModel("/"), null, new JcrTreeNodeComparator()), modelReference.getModel());
        setTitle(Model.of("Move node"));
        setSize(SIZE);
        
        this.modelReference = modelReference;
        JcrNodeModel model = (JcrNodeModel) modelReference.getModel();

        try {
            if (model.getParentModel() != null) {
                setSelectedNode(model.getParentModel());

                add(new Label("source", model.getNode().getPath()));

                target = StringUtils.substringBeforeLast(model.getNode().getPath(), "/") + "/";
                targetLabel = new Label("target", new PropertyModel(this, "target"));
                targetLabel.setOutputMarkupId(true);
                add(targetLabel);

                name = model.getNode().getName();
                TextFieldWidget nameField = new AutoFocusSelectTextFieldWidget("name", new PropertyModel<>(this, "name"));
                nameField.setSize(String.valueOf(name.length() + 5));
                add(nameField);
            } else {
                add(new Label("source", "Cannot move the root node"));
                add(new EmptyPanel("target"));
                add(new EmptyPanel("name"));
                setOkVisible(false);
                setFocusOnCancel();
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            add(new Label("source", e.getClass().getName()));
            add(new Label("target", e.getMessage()));
            add(new EmptyPanel("name"));
            setOkVisible(false);
            setFocusOnCancel();
        }
    }

    @Override
    public void onSelect(IModel<Node> model) {
        if (model != null) {
            try {
                target = model.getObject().getPath() + "/";
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        AjaxRequestTarget requestTarget = RequestCycle.get().find(AjaxRequestTarget.class);
        if (requestTarget != null) {
            requestTarget.add(targetLabel);
        }
    }

    @Override
    protected boolean isValidSelection(IJcrTreeNode targetModel) {
        return true;
    }

    @Override
    public void onOk() {
        Node sourceNode = getOriginalModel().getObject();
        Node parentNode = getParentDestNode();

        if (Strings.isEmpty(name) || parentNode == null || sourceNode == null) {
            return;
        }

        try {
            String targetPath = parentNode.getPath();
            if (!targetPath.endsWith("/")) {
                targetPath += "/";
            }
            targetPath += name;

            // The actual move
            Session jcrSession = UserSession.get().getJcrSession();
            jcrSession.move(sourceNode.getPath(), targetPath);

            Node targetNode = JcrUtils.getNodeIfExists(targetPath, jcrSession);
            if (targetNode != null) {
                modelReference.setModel(new JcrNodeModel(targetNode));
            }
        } catch (RepositoryException ex) {
            error(ex.getMessage());
        }
    }

    private Node getParentDestNode() {
        IJcrTreeNode selectedTreeNode = getSelectedNode();
        if (selectedTreeNode == null || selectedTreeNode.getNodeModel() == null) {
            return null;
        }
        return selectedTreeNode.getNodeModel().getObject();
    }
}
