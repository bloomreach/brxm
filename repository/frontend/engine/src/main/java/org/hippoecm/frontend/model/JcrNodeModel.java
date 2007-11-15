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
package org.hippoecm.frontend.model;

import javax.jcr.Node;
import javax.swing.tree.TreeNode;

import org.hippoecm.repository.api.HippoNode;

public abstract class JcrNodeModel extends ItemModelWrapper implements TreeNode {
    private static final long serialVersionUID = 1L;

    // Constructor

    public JcrNodeModel(Node node) {
        super(node);
    }

    // The wrapped repository node

    public HippoNode getNode() {
        return (HippoNode) itemModel.getObject();
    }
    
    // Convenience methods, not part of an api

    public void impersonate(JcrNodeModel model) {
        if (model != null) {
            itemModel = new JcrItemModel(model.getNode());
        }
    }

    
}
