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
import java.util.List;

import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

import org.hippoecm.cmsprototype.frontend.model.content.Folder;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * A folder tree. It shows all nodes in the JCR tree except for those
 * of that are either of type "hippo:handle" or descend from a node
 * of type "hippo:handle".
 *
 */
public class FolderTreeNode extends AbstractTreeNode {
    private static final long serialVersionUID = 1L;
    
    private Folder folder;

    public FolderTreeNode(JcrNodeModel nodeModel) {
        super(nodeModel);
        folder = new Folder(nodeModel);
    }

    public FolderTreeNode(JcrNodeModel nodeModel, JcrTreeModel treeModel) {
        super(nodeModel);
        folder = new Folder(nodeModel);
        setTreeModel(treeModel);
        treeModel.register(this);
    }
    
    @Override
    protected int loadChildcount() throws RepositoryException {
        return folder.getSubFolders().size();
    }

    @Override
    protected List<AbstractTreeNode> loadChildren() throws RepositoryException {
        List<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>();
        List<Folder> subFolders = folder.getSubFolders();
        for (Folder subFolder : subFolders) {
            result.add(new FolderTreeNode(subFolder.getNodeModel(), getTreeModel()));
        }
        return result;
    }

    @Override
    public String renderNode() {
        HippoNode node = getNodeModel().getNode();
        String result = "null";
        if (node != null) {
            try {
                result = node.getDisplayName();
                if (node.hasProperty(HippoNodeType.HIPPO_COUNT)) {
                    result += " [" + node.getProperty(HippoNodeType.HIPPO_COUNT).getLong() + "]";
                }
            } catch (RepositoryException e) {
                result = e.getMessage();
            }
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

}
