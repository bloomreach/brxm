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
package org.hippoecm.frontend.legacy.dialog.lookup;

import javax.jcr.RepositoryException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.legacy.dialog.AbstractDialog;
import org.hippoecm.frontend.legacy.dialog.DialogWindow;
import org.hippoecm.frontend.legacy.plugin.channel.Channel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated use org.hippoecm.frontend.sa.* instead
 */
@Deprecated
public abstract class LookupDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LookupDialog.class);

    private LookupTargetTreeView tree;
    private InfoPanel infoPanel;
 
    protected LookupDialog(String title, AbstractTreeNode root, DialogWindow dialogWindow) {
        super(dialogWindow);
        
        dialogWindow.setTitle(title);
        
        JcrTreeModel treeModel = new JcrTreeModel(root);
        tree = new LookupTargetTreeView("tree", treeModel, this);
        tree.getTreeState().expandNode(root);
        add(tree);
        setInfoPanel(getInfoPanel(dialogWindow));
    }

    /**
     * Override this method to have a custom InfoPanel. 
     * Make sure when you override this method, you end with 
     * super.setInfoPanel(infoPanel);
     * @param dialogWindow
     */
    protected InfoPanel getInfoPanel(DialogWindow dialogWindow) {
        JcrNodeModel nodeModel = dialogWindow.getNodeModel();
        infoPanel = new LookupDialogDefaultInfoPanel("info", nodeModel);
        add(infoPanel);
        if (nodeModel.getNode() == null) {
            ok.setVisible(false);
        }
        return infoPanel;
    }
    
    protected void setInfoPanel(InfoPanel infoPanel) {
        this.infoPanel = infoPanel;
    }
    
    protected InfoPanel getInfoPanel() {
        return infoPanel;
    }

    protected AbstractTreeNode getSelectedNode() {
        return (AbstractTreeNode)tree.getSelectedNode();
    }

    public void update(AjaxRequestTarget target, JcrNodeModel model) {
        getInfoPanel().update(target, model);
        ok.setEnabled(isValidType(model));
        target.addComponent(ok);
    }
    
    protected boolean isValidType(JcrNodeModel targetNodeModel){
        return true;
    }

}
