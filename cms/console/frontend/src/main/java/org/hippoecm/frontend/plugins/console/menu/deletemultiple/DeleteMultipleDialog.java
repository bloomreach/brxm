/*
 * Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.console.menu.deletemultiple;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.tree.DefaultNestedTree;
import org.apache.wicket.extensions.markup.html.repeater.util.ProviderSubset;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNodeProvider;
import org.hippoecm.frontend.plugins.console.NodeModelReference;
import org.hippoecm.frontend.plugins.console.tree.StyledTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Opens a dialog with subtree of the node that's been selected and allows the user to select multiple nodes to delete
 * those.
 */
public class DeleteMultipleDialog extends AbstractDialog<Node> {

    private static final Logger log = LoggerFactory.getLogger(DeleteMultipleDialog.class);
    private static final long serialVersionUID = 1L;
    
    private NodeModelReference modelReference;
    private DefaultNestedTree<Node> tree;
    private IModel<Node> selectedModel;
    private IModel<Boolean> checkboxModel;
    private ProviderSubset<Node> selectedNodes;

    public DeleteMultipleDialog(final NodeModelReference modelReference) {
        this.modelReference = modelReference;
        selectedModel = modelReference.getModel();

        final JcrTreeNodeProvider provider = new JcrTreeNodeProvider(new JcrNodeModel(selectedModel.getObject()));
        selectedNodes = new ProviderSubset<>(provider, false);

        tree = new DefaultNestedTree<Node>("jcrtree", provider) {
            @Override
            protected Component newContentComponent(final String id, final IModel<Node> node) {
                return DeleteMultipleDialog.this.newContentComponent(id, node);
            }
        };
        tree.expand(selectedModel.getObject());
        add(tree);

        checkboxModel = Model.of(Boolean.FALSE);
        add(new CheckBox("deleteFolders", checkboxModel));
    }

    private Component newContentComponent(final String id, final IModel<Node> model) {
        return new StyledTreeNode(id, tree, model) {
            @Override
            protected boolean isClickable() {
                return !model.equals(selectedModel);
            }

            @Override
            protected void onClick(AjaxRequestTarget target) {
                final Node node = getModelObject();
                if (isSelected()) {
                    selectedNodes.remove(node);
                } else {
                    selectedNodes.add(node);
                }
                tree.updateNode(node, target);
            }

            @Override
            protected boolean isSelected() {
                return selectedNodes.contains(getModelObject());
            }

            @Override
            protected IModel<?> newLabelModel(final IModel<Node> model) {
                try {
                    return Model.of((model.getObject().getName()));
                } catch (RepositoryException e) {
                    log.error("Error retrieving node name", e);
                }
                return super.newLabelModel(model);
            }
        };
    }

    @Override
    protected void onOk() {
        boolean deleteFolders = checkboxModel.getObject() == null ? false : checkboxModel.getObject();

        for (final Node node : selectedNodes) {
            if (node != null) {
                try {
                    // check if node has subnodes
                    if (node.getNodes().hasNext()) {
                        // delete only when allowed
                        if (deleteFolders) {
                            node.remove();
                        }
                    } else {
                        node.remove();
                    }
                } catch (RepositoryException e) {
                    if (log.isDebugEnabled()) {
                        log.error("Error removing node:", e);
                    }
                }
            }
        }

        modelReference.setModel(selectedModel);
    }

    @Override
    public IModel<String> getTitle() {
        return Model.of("Delete multiple nodes");
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=640,height=650").makeImmutable();
    }

}
