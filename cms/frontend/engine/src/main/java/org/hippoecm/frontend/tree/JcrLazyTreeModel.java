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
package org.hippoecm.frontend.tree;

import java.util.Enumeration;

import org.hippoecm.frontend.model.JcrNodeModel;

/**
 * A LazyTreeModel containing JcrLazyTreeNodes.
 *
 */
public class JcrLazyTreeModel extends LazyTreeModel {
    private static final long serialVersionUID = 1L;

    public JcrLazyTreeModel(JcrLazyTreeNode root) {
        super(root);
    }
    
    /**
     * Searches the tree for a JcrLazyTreeNode representing the JcrNodeModel model.
     * @param model The JcrNodeModel to search for
     * @return The JcrLazyTreeNode representing model, or null if not found
     */
    public JcrLazyTreeNode getJcrLazyTreeNode(JcrNodeModel model) {
        JcrLazyTreeNode root = (JcrLazyTreeNode) getRoot();
        return getJcrLazyTreeNode(root, model);
    }
    
    private JcrLazyTreeNode getJcrLazyTreeNode(JcrLazyTreeNode root, JcrNodeModel model) {
        // FIXME this is very inefficient, using the path to find the node would be better
        JcrLazyTreeNode treeNode = null;
        if (model.equals((JcrNodeModel)root.getUserObject())) {
            treeNode = root;
        }
        else {
            Enumeration children = root.children();
            while (children.hasMoreElements() && treeNode == null) {
                JcrLazyTreeNode child = (JcrLazyTreeNode) children.nextElement();
                if (model.equals((JcrNodeModel)child.getUserObject())) {
                    treeNode = child;
                }
                else {
                    treeNode = getJcrLazyTreeNode(child, model);
                }
            }
        }
        return treeNode;
    }
    
}
