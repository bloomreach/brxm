/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.translation.components.folder.model;

import java.util.Iterator;
import java.util.List;

/**
 * Tree filter that provides a consistent data model to the client, during the
 * linking or unlinking of a folder.  The node is filtered out of the list
 * of siblings; all requests for the node will return the edited instance.
 */
public class EditedT9Tree extends T9Tree {

    private final T9Tree upstream;
    private final T9Node editedNode;

    public EditedT9Tree(T9Tree upstream, T9Node editedNode) {
        this.upstream = upstream;
        this.editedNode = editedNode;
    }

    @Override
    public List<T9Node> getChildren(String nodeId) {
        List<T9Node> children = upstream.getChildren(nodeId);
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).getId().equals(editedNode.getId())) {
                children.set(i, editedNode);
                break;
            }
        }
        return children;
    }

    @Override
    public T9Node getNode(String id) {
        if (id.equals(editedNode.getId())) {
            return editedNode;
        }
        return upstream.getNode(id);
    }

    @Override
    public T9Node getRoot() {
        return upstream.getRoot();
    }

    @Override
    public List<T9Node> getSiblings(String t9Id) {
        List<T9Node> siblings = upstream.getSiblings(t9Id);
        for (Iterator<T9Node> iter = siblings.iterator(); iter.hasNext();) {
            T9Node node = iter.next();
            if (node.getId().equals(editedNode.getId())) {
                iter.remove();
                break;
            }
        }
        return siblings;
    }

}
