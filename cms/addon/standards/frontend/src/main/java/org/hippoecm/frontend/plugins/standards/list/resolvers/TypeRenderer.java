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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class TypeRenderer extends AbstractNodeRenderer {
    private static final long serialVersionUID = 1L;

    @Override
    protected Component getViewer(String id, HippoNode node) throws RepositoryException {
        String label = null;

        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            label = node.getPrimaryNodeType().getName();
            NodeIterator nodeIt = node.getNodes();
            while (nodeIt.hasNext()) {
                Node childNode = nodeIt.nextNode();
                if (childNode.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    label = childNode.getPrimaryNodeType().getName();
                    break;
                }
            }
            if (label.indexOf(":") > -1) {
                label = label.substring(label.indexOf(":") + 1);
            }
        } else {
            label = "folder";
        }
        return new Label(id, label);
    }

}
