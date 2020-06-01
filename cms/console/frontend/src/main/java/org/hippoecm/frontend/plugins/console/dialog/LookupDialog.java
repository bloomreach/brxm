/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.IJcrTreeModel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;

public abstract class LookupDialog extends AbstractDialog<Node> {

    private static final long serialVersionUID = 1L;

    private static final IValueMap SIZE = new ValueMap("width=515,height=470");

    private LookupTargetTreeView tree;
    private IJcrTreeModel treeModel;
    private IModel<Node> originalModel;

    protected LookupDialog(JcrTreeNode rootNode, IModel<Node> nodeModel) {
        treeModel = new JcrTreeModel(rootNode);
        this.tree = new LookupTargetTreeView("tree", treeModel, this);
        tree.getTreeState().expandNode(rootNode);
        originalModel = nodeModel;
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

    protected void onSelect(IModel<Node> nodeModel) {
    }

    protected IModel<Node> getOriginalModel() {
      return originalModel;
    }

    @Override
    public IValueMap getProperties() {
        return SIZE;
    }

    protected abstract boolean isValidSelection(IJcrTreeNode targetModel);
}
