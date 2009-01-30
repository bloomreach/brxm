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
package org.hippoecm.frontend.plugins.standards.list.comparators;

import java.util.Comparator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNode;

public abstract class NodeComparator implements Comparator<IModel>, IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    public int compare(IModel o1, IModel o2) {
        if (!(o1 instanceof JcrNodeModel) || !(o2 instanceof JcrNodeModel)) {
            return 0;
        }
        return compare((JcrNodeModel)o1, (JcrNodeModel)o2);
    }

    protected Node getCanonicalNode(JcrNodeModel model) throws RepositoryException {
        Node node1 = model.getNode();
        Node n1;
        if (node1 instanceof HippoNode) {
            n1 = ((HippoNode) node1).getCanonicalNode();
        } else {
            n1 = node1;
        }
        return n1;
    }

    public abstract int compare(JcrNodeModel node1, JcrNodeModel node2);

}
