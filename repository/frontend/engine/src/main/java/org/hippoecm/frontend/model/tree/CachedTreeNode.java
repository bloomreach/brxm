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
package org.hippoecm.frontend.model.tree;

import java.util.Enumeration;

import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;

public class CachedTreeNode implements IJcrTreeNode, IClusterable {
    private static final long serialVersionUID = 1L;

    IJcrTreeNode node;
    
    CachedTreeNode(IJcrTreeNode node) {
        this.node = node;
    }

    public Enumeration children() {
        final Enumeration upstream = node.children();
        return new Enumeration() {

            public boolean hasMoreElements() {
                return upstream.hasMoreElements();
            }

            public Object nextElement() {
                return CachedTreeModel.wrapNode((TreeNode) upstream.nextElement());
            }
            
        };
    }

    public boolean getAllowsChildren() {
        return node.getAllowsChildren();
    }

    public TreeNode getChildAt(int childIndex) {
        return CachedTreeModel.wrapNode((TreeNode) node.getChildAt(childIndex));
    }

    public int getChildCount() {
        return node.getChildCount();
    }

    public int getIndex(TreeNode node) {
        return node.getIndex(CachedTreeModel.unwrapNode(node));
    }

    public TreeNode getParent() {
        return CachedTreeModel.wrapNode(node.getParent());
    }

    public boolean isLeaf() {
        return node.isLeaf();
    }

    IJcrTreeNode get() {
        return node;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CachedTreeNode) {
            return node.equals(((CachedTreeNode) obj).node);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 51 ^ node.hashCode();
    }

    public IJcrTreeNode getChild(String name) throws RepositoryException {
        return (IJcrTreeNode) CachedTreeModel.wrapNode((TreeNode) node.getChild(name));
    }

    public JcrNodeModel getNodeModel() {
        return node.getNodeModel();
    }

    public void detach() {
        // nothing, cache children
    }

}