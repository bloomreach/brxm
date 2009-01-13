/*
 *  Copyright 2009 Hippo.
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
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class DocumentAttributeModifier extends AbstractNodeAttributeModifier {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    @Override
    public AttributeModifier getCellAttributeModifier(HippoNode node) throws RepositoryException {
        IModel documentType = null;
        try {
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                //isFolder = false;
                NodeIterator nodeIt = node.getNodes();
                while (nodeIt.hasNext()) {
                    Node childNode = nodeIt.nextNode();
                    if (childNode.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        documentType = new TypeTranslator(new JcrNodeTypeModel(childNode.getPrimaryNodeType())).getTypeName();
                        break;
                    }
                }
            } else {
                documentType = new TypeTranslator(new JcrNodeTypeModel(node.getPrimaryNodeType())).getTypeName();
            }
        } catch (RepositoryException ex) {
        }
        return new AttributeAppender("title", documentType, " ");
    }

    @Override
    public AttributeModifier getColumnAttributeModifier(HippoNode node) throws RepositoryException {
        return null;
    }
}
