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
package org.hippoecm.frontend.plugins.console.dialog;

import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;

public abstract class LookupDialog extends AbstractDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private LookupTargetTreeView tree;
    private JcrTreeModel treeModel;

    protected LookupDialog(JcrTreeNode rootNode) {
        treeModel = new JcrTreeModel(rootNode);
        this.tree = new LookupTargetTreeView("tree", treeModel, this);
        tree.getTreeState().expandNode(rootNode);
        add(tree);
    }

    @Override
    public final void onModelChanged() {
        setOkEnabled(isValidSelection(getSelectedNode()));
        onSelect(getSelectedNode().getNodeModel());
    }

    // The selected node
    public IJcrTreeNode getSelectedNode() {
        return tree.getSelectedNode();
    }

    public void setSelectedNode(JcrNodeModel selectedNode) {
        tree.setSelectedNode(selectedNode, treeModel);
    }

    protected void onSelect(JcrNodeModel nodeModel) {
    }

    protected abstract boolean isValidSelection(IJcrTreeNode targetModel);
}
