/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;

public interface IJcrTreeNode extends TreeNode, IDetachable {

    IModel<Node> getNodeModel();
    
    IJcrTreeNode getChild(String name) throws RepositoryException;
}
