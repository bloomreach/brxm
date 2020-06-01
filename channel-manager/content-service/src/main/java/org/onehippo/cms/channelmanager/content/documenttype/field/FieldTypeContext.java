/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.jcr.Node;

import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.FieldScanningContext;
import org.onehippo.cms.channelmanager.content.documenttype.util.JcrBooleanReader;
import org.onehippo.cms.channelmanager.content.documenttype.util.JcrMultipleStringReader;
import org.onehippo.cms.channelmanager.content.documenttype.util.JcrStringReader;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;

/**
 * FieldTypeContext groups and wraps sources of information about a document type field.
 */
public class FieldTypeContext {

    private final String name;
    private final String type;
    private final boolean isProperty;
    private final boolean isMultiple;
    private final List<String> validators;
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

    public FieldTypeContext(final ContentTypeItem contentTypeItem,
                            final ContentTypeContext parentContext,
                            final Node editorConfigNode) {
        this(contentTypeItem.getName(), contentTypeItem.getItemType(), contentTypeItem.isProperty(),
                contentTypeItem.isMultiple(), contentTypeItem.getValidators(), parentContext, editorConfigNode);
    }

    public FieldTypeContext(final ContentTypeItem contentTypeItem, final ContentTypeContext parentContext) {
        this(contentTypeItem, parentContext, null);
    }

    public FieldTypeContext(final String name,
                            final String type,
                            final boolean isProperty,
                            final boolean isMultiple,
                            final List<String> validators,
                            final ContentTypeContext parentContext,
                            final Node editorConfigNode) {
        this.name = name;
        this.type = type;
        this.isProperty = isProperty;
        this.isMultiple = isMultiple;
        this.validators = validators;
        this.parentContext = parentContext;
        this.editorConfigNode = editorConfigNode;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isProperty() {
        return isProperty;
    }

    public boolean isMultiple() {
        return isMultiple;
    }

    public List<String> getValidators() {
        return validators;
    }

    public ContentTypeContext getParentContext() {
        return parentContext;
    }

    public Optional<Node> getEditorConfigNode() {
        return Optional.ofNullable(editorConfigNode);
    }

    public Optional<ContentTypeContext> createContextForCompound() {
        return ContentTypeContext.createFromParent(this.type, getParentContext());
    }

    public Optional<Boolean> getBooleanConfig(final String propertyName) {
        return NamespaceUtils.getConfigProperty(this, propertyName, JcrBooleanReader.get());
    }

    public Optional<String> getStringConfig(final String propertyName) {
        return NamespaceUtils.getConfigProperty(this, propertyName, JcrStringReader.get());
    }

    public Optional<String[]> getMultipleStringConfig(final String propertyName) {
        return NamespaceUtils.getConfigProperty(this, propertyName, JcrMultipleStringReader.get());
    }
}
