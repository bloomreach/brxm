/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.list.comparators;

import java.util.Comparator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNode;

public abstract class NodeComparator implements Comparator<Node>, IClusterable {

    public int compare(Node o1, Node o2) {
        return compare(new JcrNodeModel(o1), new JcrNodeModel(o2));
    }

    protected Node getCanonicalNode(JcrNodeModel model) throws RepositoryException {
        Node node = model.getNode();
        if (node instanceof HippoNode) {
            return ((HippoNode) node).getCanonicalNode();
        } else {
            return node;
        }
    }

    public abstract int compare(JcrNodeModel node1, JcrNodeModel node2);

}
