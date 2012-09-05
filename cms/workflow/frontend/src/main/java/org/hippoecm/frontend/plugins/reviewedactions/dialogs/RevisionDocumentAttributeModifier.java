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
package org.hippoecm.frontend.plugins.reviewedactions.dialogs;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugins.reviewedactions.model.Revision;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractListAttributeModifier;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RevisionDocumentAttributeModifier extends AbstractListAttributeModifier<Revision> {

    private static final long serialVersionUID = 1L;


    private static final Logger log = LoggerFactory.getLogger(RevisionDocumentAttributeModifier.class);

    @Override
    public AttributeModifier[] getCellAttributeModifiers(IModel<Revision> model) {
        Revision revision = model.getObject();
        if (revision != null) {
            Node node = revision.getDocument().getObject();
            if (node != null) {
                return getCellAttributeModifiers(node);
            } else {
                log.warn("Cannot render a null node");
            }
        } else {
            log.warn("Cannot render a null revision");
        }
        return null;
    }

    protected AttributeModifier[] getCellAttributeModifiers(Node node) {
        AttributeModifier modifier = getCellAttributeModifier(node);
        if (modifier != null) {
            return new AttributeModifier[] { modifier };
        }
        return null;
    }

    protected AttributeModifier getCellAttributeModifier(Node node) {
        IModel<String> documentType = null;
        try {
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                //isFolder = false;
                NodeIterator nodeIt = node.getNodes();
                while (nodeIt.hasNext()) {
                    Node childNode = nodeIt.nextNode();
                    if (childNode.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        documentType = new TypeTranslator(new JcrNodeTypeModel(childNode.getPrimaryNodeType()))
                                .getTypeName();
                        break;
                    }
                }
            } else {
                documentType = new TypeTranslator(new JcrNodeTypeModel(node.getPrimaryNodeType())).getTypeName();
            }
        } catch (RepositoryException ex) {
            log.error("Unable to determine type of document", ex);
            return null;
        }
        return new AttributeAppender("title", documentType, " ");
    }

}
