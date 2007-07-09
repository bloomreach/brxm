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
package org.hippocms.repository.webapp.editor;

import javax.jcr.Node;
import javax.swing.tree.TreeNode;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.tree.ITreeStateListener;
import org.hippocms.repository.webapp.menu.Menu;
import org.hippocms.repository.webapp.model.JcrNodeModel;
import org.hippocms.repository.webapp.model.JcrPropertiesDataProvider;

public class NodeEditor extends Form implements ITreeStateListener {
    private static final long serialVersionUID = 1L;

    public NodeEditor(String id, final JcrPropertiesDataProvider model) {
        super(id, model);
        add(new PropertiesEditor("properties", model));
        add(new Menu("menu", this, model));
 
    }

    public Node getNode() {
        return (Node) getModelObject();
    }

    // ITreeStateListener

    public void nodeSelected(TreeNode treeNode) {
        if (treeNode != null) {
            JcrNodeModel treeNodeModel = (JcrNodeModel) treeNode;
            Node jcrNode = treeNodeModel.getNode();

            JcrNodeModel editorNodeModel = (JcrNodeModel) getModel();
            editorNodeModel.setNode(jcrNode);
        }
    }

    public void nodeUnselected(TreeNode treeNode) {
    }

    public void nodeCollapsed(TreeNode treeNode) {
    }

    public void nodeExpanded(TreeNode treeNode) {
    }

    public void allNodesCollapsed() {
    }

    public void allNodesExpanded() {
    }

}
