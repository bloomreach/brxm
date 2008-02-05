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

import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LookupDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LookupDialog.class);

    private LookupTargetTreeView tree;
    private LookupDialogInfoPanel infoPanel;

    protected LookupDialog(String title, AbstractTreeNode root, DialogWindow dialogWindow, Channel channel) {
        super(dialogWindow, channel);

        dialogWindow.setTitle(title);

        JcrTreeModel treeModel = new JcrTreeModel(root);
        tree = new LookupTargetTreeView("tree", treeModel, this);
        tree.getTreeState().expandNode(root);
        add(tree);

        JcrNodeModel nodeModel = dialogWindow.getNodeModel();
        infoPanel = new LookupDialogInfoPanel("info", nodeModel);
        add(infoPanel);

        if (nodeModel.getNode() == null) {
            ok.setVisible(false);
        }
    }
    
    protected AbstractTreeNode getSelectedNode() {
        return (AbstractTreeNode)tree.getSelectedNode();
    }

    void update(AjaxRequestTarget target, JcrNodeModel model) {
        if (model != null) {
            try {
                infoPanel.setTarget(model.getNode().getPath());
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        if (target != null) {
            target.addComponent(infoPanel);
        }
    }
    


}
