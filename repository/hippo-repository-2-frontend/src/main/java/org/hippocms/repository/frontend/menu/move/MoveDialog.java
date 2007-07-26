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
package org.hippocms.repository.frontend.menu.move;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.DefaultTreeModel;

import org.hippocms.repository.frontend.BrowserSession;
import org.hippocms.repository.frontend.dialog.AbstractDialog;
import org.hippocms.repository.frontend.dialog.DialogWindow;
import org.hippocms.repository.frontend.model.JcrNodeModel;
import org.hippocms.repository.frontend.tree.JcrTreeStateListener;
import org.hippocms.repository.frontend.tree.TreeView;

public class MoveDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;
    
	private MoveTargetTreeView tree;

	public MoveDialog(final DialogWindow dialogWindow, JcrNodeModel model) {
        super(dialogWindow, model);
        dialogWindow.setTitle("Move selected node");
        
        BrowserSession sessionProvider = (BrowserSession)getSession();
        Node root;
		try {
			root = sessionProvider.getJcrSession().getRootNode();
	        JcrNodeModel rootModel = new JcrNodeModel(root);
	        DefaultTreeModel treeModel = new DefaultTreeModel(rootModel);
	        tree = new MoveTargetTreeView("tree", treeModel);
			tree.getTreeState().addTreeStateListener(new JcrTreeStateListener());
	        tree.getTreeState().expandNode(rootModel);
	        add(tree);
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        if (model.getNode() == null) {
            ok.setVisible(false);
        }
    }

    public void ok() throws RepositoryException {
        if (model.getNode() != null) {
        	JcrNodeModel targetNodeModel = (JcrNodeModel) tree.getSelectedNode();
        	String target = targetNodeModel.getNode().getPath() + "/" + model.getNode().getName();
        	System.out.println(target);
            BrowserSession sessionProvider = (BrowserSession)getSession();
            sessionProvider.getJcrSession().move(model.getNode().getPath(), target);
        }
    }

    public void cancel() {
    }

    public String getMessage()  {
        try {
            return "Move " + model.getNode().getPath();
        } catch (RepositoryException e) {
            return "";
        }
    }

    public void setMessage(String message) {
    }

}
