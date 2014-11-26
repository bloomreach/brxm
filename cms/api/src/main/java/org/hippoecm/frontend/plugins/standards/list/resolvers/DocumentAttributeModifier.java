/*
 *  Copyright 2009-2014 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentAttributeModifier extends AbstractNodeAttributeModifier {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DocumentAttributeModifier.class);

    private static final String FOLDER_CSS_CLASS = "hippo-folder";
    private static final String DOCUMENT_CSS_CLASS = "hippo-document";

    public static final AttributeAppender FOLDER_CLASS_APPENDER = new AttributeAppender("class", Model.of(FOLDER_CSS_CLASS), " ");
    public static final AttributeAppender DOCUMENT_CLASS_APPENDER = new AttributeAppender("class", Model.of(DOCUMENT_CSS_CLASS), " ");

    @Override
    public AttributeModifier[] getCellAttributeModifiers(IModel<Node> model) {
        final Node node = model.getObject();

        if (node != null) {
            final NodeType primaryType = getPrimaryTypeOfDocument(node);
            if (primaryType != null) {
                return new AttributeModifier[] {
                        createTitleModifierOrNull(primaryType),
                        createClassModifierOrNull(primaryType)
                };
            }
        }
        return null;
    }

    private NodeType getPrimaryTypeOfDocument(final Node handleOrDocument) {
        try {
            if (handleOrDocument.isNodeType(HippoNodeType.NT_HANDLE)) {
                NodeIterator nodeIt = handleOrDocument.getNodes();
                while (nodeIt.hasNext()) {
                    Node childNode = nodeIt.nextNode();
                    if (childNode.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        return childNode.getPrimaryNodeType();
                    }
                }
            } else {
                return handleOrDocument.getPrimaryNodeType();
            }
        } catch (RepositoryException e) {
            log.warn("Unable to determine type of node '{}'", JcrUtils.getNodePathQuietly(handleOrDocument), e);
        }
        return null;
    }

    private AttributeModifier createTitleModifierOrNull(NodeType primaryType) {
        final JcrNodeTypeModel nodeType = new JcrNodeTypeModel(primaryType);
        IModel<String> typeName = new TypeTranslator(nodeType).getTypeName();
        return new AttributeAppender("title", typeName, " ");
    }

    private AttributeModifier createClassModifierOrNull(final NodeType primaryType) {
        if (isFolder(primaryType)) {
            return FOLDER_CLASS_APPENDER;
        } else if (isDocument(primaryType)) {
            return DOCUMENT_CLASS_APPENDER;
        }
        return null;
    }

    private boolean isFolder(NodeType primaryType) {
        return primaryType.isNodeType(HippoStdNodeType.NT_FOLDER) || primaryType.isNodeType(HippoStdNodeType.NT_DIRECTORY);
    }

    private boolean isDocument(NodeType primaryType) {
        return primaryType.isNodeType(HippoNodeType.NT_DOCUMENT);
    }

}
