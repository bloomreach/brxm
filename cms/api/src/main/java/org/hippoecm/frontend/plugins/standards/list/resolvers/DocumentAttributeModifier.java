/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
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

    private static final AttributeModifier FOLDER_CLASS_APPENDER = CssClass.append(FOLDER_CSS_CLASS);
    private static final AttributeModifier DOCUMENT_CLASS_APPENDER = CssClass.append(DOCUMENT_CSS_CLASS);

    private static final DocumentAttributeModifier INSTANCE = new DocumentAttributeModifier();

    private DocumentAttributeModifier() {
    }

    public static DocumentAttributeModifier getInstance() {
        return INSTANCE;
    }

    @Override
    public AttributeModifier[] getCellAttributeModifiers(IModel<Node> model) {
        final Node node = model.getObject();

        if (node != null) {
            return new AttributeModifier[] {
                    createTitleModifierOrNull(node),
                    createClassModifierOrNull(node)
            };
        }
        return null;
    }

    private AttributeModifier createTitleModifierOrNull(final Node node) {
        final NodeTranslator nodeTranslator = new NodeTranslator(new JcrNodeModel(node));
        return TitleAttribute.append(nodeTranslator.getNodeName());
    }

    private AttributeModifier createClassModifierOrNull(final Node node) {
        final NodeType primaryType = getPrimaryTypeOfDocument(node);
        if (primaryType != null) {
            if (isFolder(primaryType)) {
                return FOLDER_CLASS_APPENDER;
            } else if (isDocument(primaryType)) {
                return DOCUMENT_CLASS_APPENDER;
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

    private boolean isFolder(NodeType primaryType) {
        return primaryType.isNodeType(HippoStdNodeType.NT_FOLDER) || primaryType.isNodeType(HippoStdNodeType.NT_DIRECTORY);
    }

    private boolean isDocument(NodeType primaryType) {
        return primaryType.isNodeType(HippoNodeType.NT_DOCUMENT);
    }

}
