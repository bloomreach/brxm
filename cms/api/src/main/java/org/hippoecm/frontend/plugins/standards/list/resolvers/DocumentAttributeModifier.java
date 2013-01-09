/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentAttributeModifier extends AbstractNodeAttributeModifier {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DocumentAttributeModifier.class);

    @Override
    public AttributeModifier[] getCellAttributeModifiers(IModel<Node> model) {
        if (model.getObject() == null) {
            return new AttributeModifier[] { null };
        }
        return super.getCellAttributeModifiers(model);
    }

    @Override
    public AttributeModifier getCellAttributeModifier(Node node) {
        IModel<String> documentType = null;
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
            log.error("Unable to determine type of document", ex);
        }
        return new AttributeAppender("title", documentType, " ");
    }

}
