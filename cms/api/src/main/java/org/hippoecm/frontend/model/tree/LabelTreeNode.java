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

import java.io.Serializable;
import java.util.Enumeration;

import javax.swing.tree.TreeNode;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class LabelTreeNode implements TreeNode, Serializable {

    private static final long serialVersionUID = 1L;

    private TreeNode parentNode;
    private IModel<String> label;

    public LabelTreeNode(TreeNode parentNode, String label) {
        this(parentNode, new Model<String>(label));
    }

    public LabelTreeNode(TreeNode parentNode, IModel<String> label) {
        this.parentNode = parentNode;
        this.label = label;
    }
    
    public String getLabel() {
        return (String) label.getObject();
    }

    public Enumeration<TreeNode> children() {
        return new Enumeration<TreeNode>() {

            public boolean hasMoreElements() {
                return false;
            }

            public TreeNode nextElement() {
                return null;
            }
            
        };
    }

    public boolean getAllowsChildren() {
        return false;
    }

    public TreeNode getChildAt(int childIndex) {
        return null;
    }

    public int getChildCount() {
        return 0;
    }

    public int getIndex(TreeNode node) {
        return -1;
    }

    public TreeNode getParent() {
        return parentNode;
    }

    public boolean isLeaf() {
        return true;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if(!(object instanceof LabelTreeNode)) {
            return false;
        } else {
            LabelTreeNode treeNode = (LabelTreeNode) object;
            return new EqualsBuilder().append(parentNode, treeNode.parentNode).isEquals();
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(87, 335).append(parentNode).toHashCode();
    }
}
