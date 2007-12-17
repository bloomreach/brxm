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
package org.hippoecm.frontend.model.tree;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.Session;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNodeType;

public class DocumentTreeNode extends AbstractTreeNode {
    private static final long serialVersionUID = 1L;

    public DocumentTreeNode(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    public DocumentTreeNode(JcrNodeModel nodeModel, JcrTreeModel treeModel) {
        super(nodeModel);
        setTreeModel(treeModel);
        treeModel.register(this);
    }

    public TreeNode getParent() {
        try {
            JcrNodeModel parentModel = nodeModel.getParentModel();
            while (parentModel != null && !parentModel.getNode().getPrimaryNodeType().getName().equals(HippoNodeType.NT_DOCUMENT)) {
                parentModel = parentModel.getParentModel();
            }
            if (parentModel == null) {
                TreeNode root = (TreeNode)getTreeModel().getRoot();
                if (this.equals(root)) {
                    return null;
                }
                return root;
            }
            return getTreeModel().lookup(parentModel);
        } catch (RepositoryException e) {
            return (TreeNode)getTreeModel().getRoot();
        }    
    }

    @Override
    protected int loadChildcount() throws RepositoryException {
        return (int) subdocuments().getSize();
    }

    @Override
    protected List<AbstractTreeNode> loadChildren() throws RepositoryException {
        List<AbstractTreeNode> result = new ArrayList();
        NodeIterator subDocuments = subdocuments();
        while (subDocuments.hasNext()) {
            JcrNodeModel subDocument = new JcrNodeModel(nodeModel, subDocuments.nextNode());
            result.add(new DocumentTreeNode(subDocument, getTreeModel()));
        }
        return result;
    }

    private NodeIterator subdocuments() throws RepositoryException {
        String path = nodeModel.getNode().getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length()-1);
        }
        //
        String expression = path + "//*[@jcr:primaryType='" + HippoNodeType.NT_HANDLE + "']";

        UserSession session = (UserSession) Session.get();
        QueryManager queryManager = session.getJcrSession().getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(  expression, Query.XPATH);
        QueryResult result = query.execute();
        
        return result.getNodes();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("nodeModel", nodeModel.toString())
            .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof DocumentTreeNode == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        DocumentTreeNode treeNode = (DocumentTreeNode) object;
        return new EqualsBuilder().append(nodeModel, treeNode.nodeModel).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(87, 335).append(nodeModel).toHashCode();
    }

}
