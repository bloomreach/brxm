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
package org.hippocms.repository.webapp.tree;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.markup.html.tree.ITreeStateListener;
import org.hippocms.repository.webapp.Browser;
import org.hippocms.repository.webapp.editor.NodeEditor;
import org.hippocms.repository.webapp.model.JcrNodeModel;

public class TreeView extends Tree {
    private static final long serialVersionUID = 1L;

    public TreeView(String id, TreeModel model) {
        super(id, model);
    }

    protected String renderNode(TreeNode treeNode) {
        JcrNodeModel nodeModel = (JcrNodeModel) treeNode;
        Node node = nodeModel.getNode();
        String result = "null";
        if (node != null) {
            try {
                result = node.getName();
            } catch (RepositoryException e) {
                result = e.getMessage();
            }
        }
        return result;
    }

    public void addTreeStateListener(ITreeStateListener listener) {
        getTreeState().addTreeStateListener(listener);
    }
       
    protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode treeNode) {
        Browser browser = (Browser) findParent(Browser.class);
        NodeEditor editor = browser.getEditorPanel().getEditor();
        target.addComponent(editor);
    }

}
