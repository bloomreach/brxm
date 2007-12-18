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
package org.hippoecm.cmsprototype.frontend.model.tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.swing.tree.TreeNode;

import org.apache.wicket.Session;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * A folder tree. It shows all nodes in the JCR tree except for those
 * of that are either of type "hippo:handle" or descend from a node
 * of type "hippo:handle".
 *
 */
public class FolderTreeNode extends AbstractTreeNode {
    private static final long serialVersionUID = 1L;

    public FolderTreeNode(JcrNodeModel nodeModel) {
        super(nodeModel);
        // TODO Auto-generated constructor stub
    }

    public FolderTreeNode(JcrNodeModel nodeModel, JcrTreeModel treeModel) {
        super(nodeModel);
        setTreeModel(treeModel);
        treeModel.register(this);
    }
    
    @Override
    protected int loadChildcount() throws RepositoryException {
        return subFolders().size();
    }

    @Override
    protected List<AbstractTreeNode> loadChildren() throws RepositoryException {
        List<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>();
        Iterator<Node> subDocuments = subFolders().iterator();
        while (subDocuments.hasNext()) {
            JcrNodeModel subDocument = new JcrNodeModel(nodeModel, subDocuments.next());
            result.add(new FolderTreeNode(subDocument, getTreeModel()));
        }
        return result;
    }

    public TreeNode getParent() {
        JcrNodeModel parentModel = nodeModel.getParentModel();
        if (parentModel != null) {
            return getTreeModel().lookup(parentModel);
        }
        return null;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    private List<Node> subFolders() throws RepositoryException {
        Node node = nodeModel.getNode();
        List<Node> childNodes = new ArrayList<Node>();
        NodeIterator jcrChildren = node.getNodes();
        while (jcrChildren.hasNext()) {
            Node jcrChild = jcrChildren.nextNode();
            if (jcrChild != null ) {
                
                NodeType nodeType = jcrChild.getPrimaryNodeType();
                if (!(HippoNodeType.NT_HANDLE.equals(nodeType.getName()))) {
                    childNodes.add(jcrChild);
                }
                
            }
        }
        return childNodes;
    }
    
}
