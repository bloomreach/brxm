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

import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultTreeModel;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;

public class JcrTreeModel extends DefaultTreeModel implements IDetachable {
    private static final long serialVersionUID = 1L;

    private Map registry;

    public JcrTreeModel(AbstractTreeNode rootModel) {
        super(rootModel);
        rootModel.setTreeModel(this);

        registry = new HashMap();
        register(rootModel);
    }

    public void register(AbstractTreeNode treeNodeModel) {
        String key = treeNodeModel.getNodeModel().getItemModel().getPath();
        registry.put(key, treeNodeModel);
    }

    //TODO: Currently treeNodes are never unregistered.
    //
    //Although an unregister method is easy to implement it
    //is not clear when to call it. Maybe a WeakReference HashMap
    //should be used to keep the registry clean.
    //
    //With the current use cases never unregistering doesn't
    //seem to cause much trouble though.

    public AbstractTreeNode lookup(JcrNodeModel nodeModel) {
        String key = nodeModel.getItemModel().getPath();
        if((AbstractTreeNode) registry.get(key) == null) {
            AbstractTreeNode parentNode = lookup(nodeModel.getParentModel());
            if(parentNode!=null) {
                // load children which get registered
                parentNode.children();
            } 
        }
        
        return (AbstractTreeNode) registry.get(key);
    }

    public void detach() {
        for(Map.Entry<String, AbstractTreeNode> entry : ((Map<String, AbstractTreeNode>) registry).entrySet()) {
            entry.getValue().detach();
        }
    }
    
}
