/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.documenttype.field;

import java.util.Objects;
import java.util.Optional;

import javax.jcr.Node;

import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.FieldScanningContext;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;

/**
 * FieldTypeContext groups and wraps sources of information about a document type field.
 */
public class FieldTypeContext {
    private final ContentTypeItem contentTypeItem;
    private final Node editorConfigNode;
    private final ContentTypeContext parentContext;

    /**
     * Create a FieldTypeContext given an editor configuration field node and a content type context.
     *
     * We only consider nodes that have a "field" property, and try to find the corresponding child node of the
     * content type's nodeType node (or of a supertype of the content type, to support inheritance).
     *
     * @param editorFieldConfigNode JCR node representing the field's editor configuration
     * @param context               content type context within which to create the field type context
     * @return                      derived FieldTypeContext or nothing, wrapped in an Optional
     */
    public static Optional<FieldTypeContext> create(final Node editorFieldConfigNode, final ContentTypeContext context) {
        return NamespaceUtils.getFieldProperty(editorFieldConfigNode)
                .flatMap(fieldName -> createForFieldName(fieldName, context, editorFieldConfigNode));
    }

    private static Optional<FieldTypeContext> createForFieldName(final String fieldName,
                                                                 final ContentTypeContext context,
                                                                 final Node editorFieldConfigNode) {
        return context.getFieldScanningContexts()
                .stream()
                .map(scanningContext -> createForParentType(scanningContext, fieldName, context, editorFieldConfigNode))
                .filter(Objects::nonNull)
                .findFirst();
    }

    private static FieldTypeContext createForParentType(final FieldScanningContext scanningContext,
                                                        final String fieldName,
                                                        final ContentTypeContext context,
                                                        final Node editorFieldConfigNode) {
        return NamespaceUtils.getPathForNodeTypeField(scanningContext.getNodeTypeNode(), fieldName)
                .map(path -> createForItem(path, scanningContext.getContentType(), context, editorFieldConfigNode))
                .orElse(null);
    }


    private static FieldTypeContext createForItem(final String itemName,
                                                  final ContentType parentType,
                                                  final ContentTypeContext context,
                                                  final Node editorFieldConfigNode) {
        final ContentTypeItem item = parentType.getItem(itemName);
        return item != null ? new FieldTypeContext(item, context, editorFieldConfigNode) : null;
    }

    public FieldTypeContext(final ContentTypeItem contentTypeItem, final ContentTypeContext parentContext,
                            final Node editorConfigNode) {
        this.contentTypeItem = contentTypeItem;
        this.parentContext = parentContext;
        this.editorConfigNode = editorConfigNode;
    }

    public FieldTypeContext(final ContentTypeItem contentTypeItem, final ContentTypeContext parentContext) {
        this(contentTypeItem, parentContext, null);
    }

    public ContentTypeItem getContentTypeItem() {
        return contentTypeItem;
    }

    public ContentTypeContext getParentContext() {
        return parentContext;
    }

    public Optional<Node> getEditorConfigNode() {
        return Optional.ofNullable(editorConfigNode);
    }

    public Optional<ContentTypeContext> createContextForCompound() {
        final String id = getContentTypeItem().getItemType();
        return ContentTypeContext.createFromParent(id, getParentContext());
    }
}
