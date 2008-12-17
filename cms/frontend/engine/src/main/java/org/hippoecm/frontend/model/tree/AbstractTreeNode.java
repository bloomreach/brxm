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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTreeNode extends NodeModelWrapper implements TreeNode {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(AbstractTreeNode.class);

    private List<AbstractTreeNode> children = new ArrayList<AbstractTreeNode>();

    private boolean reloadChildren = true;
    private boolean reloadChildcount = true;
    private int childcount = 0;

    public AbstractTreeNode(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    public Enumeration children() {
        ensureChildrenLoaded();
        return Collections.enumeration(children);
    }

    public AbstractTreeNode getChild(String name) throws RepositoryException {
        if (getNodeModel().getNode().hasNode(name)) {
            JcrNodeModel childModel = new JcrNodeModel(getNodeModel().getNode().getNode(name));
            ensureChildrenLoaded();
            for (AbstractTreeNode child : children) {
                if (child.getNodeModel().equals(childModel)) {
                    return child;
                }
            }
        }
        return null;
    }

    public TreeNode getChildAt(int i) {
        ensureChildrenLoaded();
        return children.get(i);
    }

    public int getChildCount() {
        ensureChildcountLoaded();
        return childcount;
    }

    public int getIndex(TreeNode node) {
        ensureChildrenLoaded();
        return children.indexOf(node);
    }

    public boolean isLeaf() {
        ensureChildcountLoaded();
        return childcount == 0;
    }

    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public void detach() {
        reloadChildren = true;
        reloadChildcount = true;
        children = null;
        super.detach();
    }

    protected abstract int loadChildcount() throws RepositoryException;

    protected abstract List<AbstractTreeNode> loadChildren() throws RepositoryException;

    public abstract String renderNode();

    private void ensureChildcountLoaded() {
        if (nodeModel == null || nodeModel.getNode() == null) {
            reloadChildren = false;
            reloadChildcount = false;
            childcount = 0;
        } else if (reloadChildcount) {
            try {
                childcount = loadChildcount();
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
            reloadChildcount = false;
        }
    }

    private void ensureChildrenLoaded() {
        if (nodeModel.getNode() == null) {
            reloadChildren = false;
            reloadChildcount = false;
            children = new ArrayList();
        } else if (reloadChildren) {
            try {
                children = loadChildren();
                childcount = children.size();
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
            reloadChildren = false;
            reloadChildcount = false;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof AbstractTreeNode == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        AbstractTreeNode treeNode = (AbstractTreeNode) object;
        return new EqualsBuilder().append(nodeModel, treeNode.nodeModel).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(467, 17).append(nodeModel).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("nodeModel", nodeModel.toString())
                .toString();
    }

}
