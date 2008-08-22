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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class TypeComparator extends NodeComparator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(JcrNodeModel o1, JcrNodeModel o2) {
        try {
            HippoNode n1 = ((JcrNodeModel) o1).getNode();
            HippoNode n2 = ((JcrNodeModel) o2).getNode();
            if (n1 == null) {
                if (n2 == null) {
                    return 0;
                }
                return 1;
            } else if (o2 == null) {
                return -1;
            }
            String label1 = getTypeLabel(n1);
            String label2 = getTypeLabel(n2);
            return String.CASE_INSENSITIVE_ORDER.compare(label1, label2);
        } catch (RepositoryException e) {
            return 0;
        }
    }
    
    private String getTypeLabel(HippoNode node) throws RepositoryException {
        String type = "";
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            type = node.getPrimaryNodeType().getName();
            NodeIterator nodeIt = node.getNodes();
            while (nodeIt.hasNext()) {
                Node childNode = nodeIt.nextNode();
                if (childNode.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    type = childNode.getPrimaryNodeType().getName();
                    break;
                }
            }
            if (type.indexOf(":") > -1) {
                type = type.substring(type.indexOf(":") + 1);
            }
        } else {
            type = "folder";
        }
        return type;
    }

}
