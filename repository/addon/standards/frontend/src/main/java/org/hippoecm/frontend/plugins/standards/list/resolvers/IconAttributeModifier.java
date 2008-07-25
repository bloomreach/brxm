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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.Model;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class IconAttributeModifier extends AbstractNodeAttributeModifier {
    private static final long serialVersionUID = 1L;

    @Override
    protected AttributeModifier getCellAttributeModifier(HippoNode node) throws RepositoryException {
        String cssClass = null;
        String type = null;
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
                cssClass = "document-16";
            }
        } else {
            cssClass = "folder-16";
        }       
        return new AttributeModifier("class", true, new Model(cssClass));
    }

    @Override
    protected AttributeModifier getColumnAttributeModifier(HippoNode node) throws RepositoryException {
        return new AttributeModifier("class", true, new Model("icon-16"));
    }
    
}
