/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.dialog.lookup;

import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.service.render.RenderPlugin;

public abstract class LookupDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;
    
    protected IServiceReference<RenderPlugin> pluginRef;
    protected LookupTargetTreeView tree;
    protected Panel infoPanel;
    
    protected LookupDialog(RenderPlugin plugin, IPluginContext context, IDialogService dialogWindow) {
        super(context, dialogWindow);
        this.pluginRef = context.getReference(plugin);
        
        AbstractTreeNode rootNode = getRootNode();
        JcrTreeModel treeModel = new JcrTreeModel(rootNode);
        this.tree = new LookupTargetTreeView("tree", treeModel, this);
        tree.getTreeState().expandNode(rootNode);
        add(tree);

        this.infoPanel = getInfoPanel();
        add(infoPanel);
    }
    
    @Override
    public void onModelChanged() {
        infoPanel.setModel(getModel());
        ok.setEnabled(isValidSelection(getSelectedNode()));
    }
    
    // The selected node
    public AbstractTreeNode getSelectedNode() {
        return (AbstractTreeNode) tree.getSelectedNode();
    }
       
    protected abstract Panel getInfoPanel();
    
    protected abstract AbstractTreeNode getRootNode();

    protected abstract boolean isValidSelection(AbstractTreeNode targetModel);
}
