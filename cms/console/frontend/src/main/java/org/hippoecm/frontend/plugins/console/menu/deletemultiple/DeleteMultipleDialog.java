/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.DefaultTreeModel;

import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation.Alignment;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation.Unit;
import org.apache.wicket.extensions.markup.html.tree.table.IColumn;
import org.apache.wicket.extensions.markup.html.tree.table.PropertyTreeColumn;
import org.apache.wicket.extensions.markup.html.tree.table.TreeTable;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugins.console.NodeModelReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens a dialog with subtree of node that's been selected
 * and allows user to select multiple nodes to delete those.
 */
public class DeleteMultipleDialog extends AbstractDialog<Node> {

    private static final Logger log = LoggerFactory.getLogger(DeleteMultipleDialog.class);
    private static final long serialVersionUID = 1L;

    private NodeModelReference modelReference;
    private final TreeTable tree;
    private IModel<Node> selectedModel;
    private IModel<Boolean> checkboxModel;

    public DeleteMultipleDialog(final NodeModelReference modelReference) {
        this.modelReference = modelReference;

        DefaultTreeModel model = null;
        try {

            selectedModel = modelReference.getModel();
            final JcrTreeNode root = new JcrTreeNode(new JcrNodeModel(selectedModel.getObject().getPath()), null);
            model = new DefaultTreeModel(root);
        } catch (RepositoryException e) {
            log.error("Error initializing tree", e);
        }
        IColumn columns[] = new IColumn[]{new PropertyTreeColumn(new ColumnLocation(Alignment.MIDDLE, 8,
                Unit.PROPORTIONAL), "Name", "nodeModel.node.name")

        };
        tree = new TreeTable("multitree", model, columns);
        tree.getTreeState().setAllowSelectMultiple(true);
        add(tree);
        if (model != null) {
            tree.getTreeState().expandNode(model.getRoot());
        }
        checkboxModel = new Model<Boolean>(Boolean.FALSE);
        add(new CheckBox("deleteFolders", checkboxModel));

    }


    @Override
    protected void onOk() {
        final Collection<Object> selectedNodes = tree.getTreeState().getSelectedNodes();
        // do not delete root (first selected node):
        if (rootSelected(selectedNodes)) {
            error("You've selected root node for deletion");
            return;
        }
        boolean deleteFolders = checkboxModel.getObject() == null ? false : checkboxModel.getObject();

        for (Object selectedNode : selectedNodes) {
            JcrTreeNode deleteNode = (JcrTreeNode) selectedNode;
            IModel<Node> nodeModel = deleteNode.getChainedModel();
            final Node node = nodeModel.getObject();
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


    private boolean rootSelected(Iterable<Object> selectedNodes) {
        for (Object selectedNode : selectedNodes) {
            JcrTreeNode deleteNode = (JcrTreeNode) selectedNode;
            IModel<Node> nodeModel = deleteNode.getChainedModel();
            if (nodeModel.equals(selectedModel)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IModel getTitle() {
        return new Model<String>("Delete multiple nodes");
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=640,height=650").makeImmutable();
    }

}
