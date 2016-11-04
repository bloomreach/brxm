/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content.documenttype.field.sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sorts fields of a content type in the order of the editor template nodes.
 */
public class NodeOrderFieldSorter implements FieldSorter {
    private static final Logger log = LoggerFactory.getLogger(NodeOrderFieldSorter.class);

    @Override
    public List<FieldTypeContext> sortFields(final ContentTypeContext context) {
        final List<FieldTypeContext> sortedFields = new ArrayList<>();
        final Node rootNode = context.getContentTypeRoot();

        try {
            final Node editorConfigNode = rootNode.getNode(NamespaceUtils.EDITOR_CONFIG_PATH);
            final Node nodeTypeNode = rootNode.getNode(NamespaceUtils.NODE_TYPE_PATH);

            for (final Node editorFieldNode : new NodeIterable(editorConfigNode.getNodes())) {
                createFieldContext(editorFieldNode, nodeTypeNode, context).ifPresent(sortedFields::add);
            }
        } catch (RepositoryException e) {
            log.warn("Failed to sort fields of content type {}", context.getContentType().getName(), e);
        }

        return sortedFields;
    }

    protected Optional<FieldTypeContext> createFieldContext(final Node editorFieldNode, final Node nodeTypeNode,
                                                            final ContentTypeContext context) {
        try {
            if (editorFieldNode.hasProperty("field")) {
                final String fieldName = editorFieldNode.getProperty("field").getString();
                if (nodeTypeNode.hasNode(fieldName)) {
                    final Node fieldTypeNode = nodeTypeNode.getNode(fieldName);
                    final String path = fieldTypeNode.getProperty(HippoNodeType.HIPPO_PATH).getString();
                    final ContentTypeItem item = context.getContentType().getItem(path);
                    if (item != null) {
                        return Optional.of(new FieldTypeContext(editorFieldNode, item));
                    }
                }
            }
        } catch (RepositoryException e) {
            log.warn("Failed to create field context for node {}", JcrUtils.getNodePathQuietly(editorFieldNode), e);
        }
        return Optional.empty();
    }
}
